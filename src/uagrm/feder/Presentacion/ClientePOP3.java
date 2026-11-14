package uagrm.feder.Presentacion;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Cliente POP3 puro mediante Sockets TCP para el protocolo RFC 1939.
 * Incluye soporte completo UTF-8 y decodificación MIME (RFC 2047 Base64 y Quoted-Printable)
 * para soportar eñes, tildes y caracteres especiales enviados desde Gmail/Outlook/Thunderbird.
 */
public class ClientePOP3 {

    private static final String DEFAULT_HOST = "mail.tecnoweb.org.bo";
    private static final int DEFAULT_PORT = 110;

    // Patrón RFC 2047: =?charset?encoding?encoded-text?=
    private static final Pattern RFC2047_PATTERN = Pattern.compile(
            "=\\?([^?]+)\\?([BbQq])\\?([^?]*)\\?="
    );

    private final String host;
    private final int port;

    private Socket socket;
    private BufferedReader reader;
    private DataOutputStream writer;

    /**
     * Estructura que encapsula los datos esenciales extraídos de un correo.
     */
    public static class Correo {
        private final int numero;
        private final String remitente;
        private final String asunto;
        private final String cuerpo;

        public Correo(int numero, String remitente, String asunto, String cuerpo) {
            this.numero = numero;
            this.remitente = remitente;
            this.asunto = asunto;
            this.cuerpo = cuerpo;
        }

        public int getNumero() {
            return numero;
        }

        public String getRemitente() {
            return remitente;
        }

        public String getAsunto() {
            return asunto;
        }

        public String getCuerpo() {
            return cuerpo;
        }

        @Override
        public String toString() {
            return "Correo [#" + numero + ", De: " + remitente + ", Asunto: " + asunto + "]";
        }
    }

    public ClientePOP3() {
        this(DEFAULT_HOST, DEFAULT_PORT);
    }

    public ClientePOP3(String host, int port) {
        this.host = host;
        this.port = port;
    }

    /**
     * Establece la conexión TCP con el servidor POP3 utilizando codificación UTF-8.
     *
     * @return true si el servidor respondió con +OK.
     * @throws IOException Si ocurre un error de red.
     */
    public boolean conectar() throws IOException {
        this.socket = new Socket(host, port);
        this.socket.setSoTimeout(15000);
        this.reader = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8)
        );
        this.writer = new DataOutputStream(socket.getOutputStream());

        String saludo = reader.readLine();
        return saludo != null && saludo.startsWith("+OK");
    }

    /**
     * Autentica la sesión POP3 enviando USER y PASS.
     *
     * @param usuario    Nombre de usuario del buzón.
     * @param contrasena Contraseña del buzón.
     * @return true si la autenticación fue exitosa.
     * @throws IOException Si falla la lectura/escritura en el socket.
     */
    public boolean autenticar(String usuario, String contrasena) throws IOException {
        enviarComando("USER " + usuario);
        String respUser = reader.readLine();
        if (respUser == null || !respUser.startsWith("+OK")) {
            System.err.println("[ERROR] POP3 USER: " + respUser);
            return false;
        }

        enviarComando("PASS " + contrasena);
        String respPass = reader.readLine();
        if (respPass == null || !respPass.startsWith("+OK")) {
            System.err.println("[ERROR] POP3 PASS: " + respPass);
            return false;
        }
        return true;
    }

    /**
     * Consulta el estado del buzón con STAT y retorna la cantidad de mensajes.
     *
     * @return Número de mensajes disponibles en el buzón.
     * @throws IOException Si ocurre un error de comunicación.
     */
    public int obtenerCantidadMensajes() throws IOException {
        enviarComando("STAT");
        String respuesta = reader.readLine();
        if (respuesta != null && respuesta.startsWith("+OK")) {
            String[] partes = respuesta.split("\\s+");
            if (partes.length >= 2) {
                try {
                    return Integer.parseInt(partes[1]);
                } catch (NumberFormatException e) {
                    System.err.println("[ERROR] Parseando STAT: " + respuesta);
                }
            }
        }
        return 0;
    }

    /**
     * Descarga el correo correspondiente al índice dado mediante RETR y decodifica
     * sus cabeceras y cuerpo en UTF-8.
     *
     * @param nro Número de mensaje en el buzón POP3.
     * @return Instancia de Correo con remitente, asunto y cuerpo limpios.
     * @throws IOException Si ocurre un error de lectura en el socket.
     */
    public Correo descargarCorreo(int nro) throws IOException {
        enviarComando("RETR " + nro);
        String linea = reader.readLine();
        if (linea == null || !linea.startsWith("+OK")) {
            System.err.println("[ERROR] Al recuperar correo #" + nro + ": " + linea);
            return null;
        }

        StringBuilder rawEmail = new StringBuilder();
        while ((linea = reader.readLine()) != null) {
            if (linea.equals(".")) {
                break;
            }
            if (linea.startsWith("..")) {
                linea = linea.substring(1);
            }
            rawEmail.append(linea).append("\r\n");
        }

        return parsearMensaje(nro, rawEmail.toString());
    }

    /**
     * Marca un mensaje para su eliminación en el servidor mediante DELE.
     *
     * @param nro Número del mensaje a eliminar.
     * @return true si el servidor confirmó con +OK.
     * @throws IOException Si falla el envío del comando.
     */
    public boolean borrarCorreo(int nro) throws IOException {
        enviarComando("DELE " + nro);
        String respuesta = reader.readLine();
        return respuesta != null && respuesta.startsWith("+OK");
    }

    /**
     * Envía el comando QUIT y cierra ordenadamente los flujos y el Socket TCP.
     */
    public void cerrar() {
        try {
            if (writer != null && socket != null && !socket.isClosed()) {
                enviarComando("QUIT");
                if (reader != null) {
                    reader.readLine();
                }
            }
        } catch (IOException ignored) {
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
                if (writer != null) {
                    writer.close();
                }
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException e) {
                System.err.println("[ERROR] Cerrando socket POP3: " + e.getMessage());
            }
        }
    }

    private void enviarComando(String comando) throws IOException {
        byte[] bytes = (comando + "\r\n").getBytes(StandardCharsets.UTF_8);
        writer.write(bytes);
        writer.flush();
    }

    /**
     * Parsea el correo crudo aplicando header unfolding (RFC 5322), decodificación
     * RFC 2047 para el asunto y Quoted-Printable para el cuerpo.
     */
    private Correo parsearMensaje(int nro, String rawContent) {
        int sepIndex = rawContent.indexOf("\r\n\r\n");
        String bloqueCabeceras = sepIndex >= 0
                ? rawContent.substring(0, sepIndex)
                : rawContent;
        String bloqueCuerpo = sepIndex >= 0
                ? rawContent.substring(sepIndex + 4)
                : "";

        // Desplegar cabeceras multilínea (unfolding: CRLF seguido de espacio o tab)
        String cabecerasUnfolded = bloqueCabeceras.replaceAll("\r\n[ \t]+", " ");
        String[] lineasCabecera = cabecerasUnfolded.split("\r\n");

        String remitente = "";
        String asunto = "";
        boolean esQuotedPrintable = false;

        for (String linea : lineasCabecera) {
            String lower = linea.toLowerCase();
            if (lower.startsWith("from:")) {
                String fromDecodificado = decodificarRFC2047(linea.substring(5).trim());
                remitente = extraerEmail(fromDecodificado);
            } else if (lower.startsWith("subject:")) {
                asunto = decodificarRFC2047(linea.substring(8).trim());
            } else if (lower.startsWith("content-transfer-encoding:")
                    && lower.contains("quoted-printable")) {
                esQuotedPrintable = true;
            }
        }

        String cuerpoTexto = procesarCuerpoMime(bloqueCuerpo, esQuotedPrintable);
        return new Correo(nro, remitente, asunto, cuerpoTexto);
    }

    /**
     * Decodifica palabras codificadas bajo el estándar RFC 2047 (=?UTF-8?Q?...?= y =?UTF-8?B?...?=).
     *
     * @param texto Cadena de cabecera posiblemente codificada.
     * @return Texto limpio en UTF-8 con eñes y tildes restauradas.
     */
    public static String decodificarRFC2047(String texto) {
        if (texto == null || !texto.contains("=?")) {
            return texto != null ? texto : "";
        }

        // Unir bloques encoded-word adyacentes separados solo por espacios
        String preparado = texto.replaceAll("\\?=\\s+=\\?", "?==?");
        Matcher matcher = RFC2047_PATTERN.matcher(preparado);
        StringBuffer resultado = new StringBuffer();

        while (matcher.find()) {
            String nombreCharset = matcher.group(1);
            String tipoCodificacion = matcher.group(2).toUpperCase();
            String contenido = matcher.group(3);

            Charset charset;
            try {
                charset = Charset.forName(nombreCharset);
            } catch (Exception e) {
                charset = StandardCharsets.UTF_8;
            }

            String decodificado;
            try {
                if ("B".equals(tipoCodificacion)) {
                    byte[] bytes = Base64.getDecoder().decode(contenido.replaceAll("\\s+", ""));
                    decodificado = new String(bytes, charset);
                } else {
                    // En Q-encoding de cabeceras, '_' representa espacio (0x20)
                    String qContenido = contenido.replace('_', ' ');
                    decodificado = decodificarQuotedPrintable(qContenido, charset);
                }
            } catch (Exception e) {
                decodificado = contenido;
            }

            matcher.appendReplacement(resultado, Matcher.quoteReplacement(decodificado));
        }
        matcher.appendTail(resultado);
        return resultado.toString().trim();
    }

    /**
     * Decodifica una cadena en formato Quoted-Printable (ej. =C3=B1 -> ñ) usando el Charset indicado.
     *
     * @param input   Texto en Quoted-Printable.
     * @param charset Juego de caracteres destino (por defecto UTF-8).
     * @return Cadena decodificada.
     */
    public static String decodificarQuotedPrintable(String input, Charset charset) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        // Eliminar saltos de línea suaves (soft line breaks: '=' al final de línea)
        String sinSaltosSuaves = input.replaceAll("=\r?\n", "");
        byte[] bytesEntrada = sinSaltosSuaves.getBytes(StandardCharsets.ISO_8859_1);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream(bytesEntrada.length);

        for (int i = 0; i < bytesEntrada.length; i++) {
            int b = bytesEntrada[i];
            if (b == '=' && i + 2 < bytesEntrada.length) {
                int u = Character.digit((char) bytesEntrada[i + 1], 16);
                int l = Character.digit((char) bytesEntrada[i + 2], 16);
                if (u != -1 && l != -1) {
                    buffer.write((char) ((u << 4) + l));
                    i += 2;
                    continue;
                }
            }
            buffer.write(b);
        }
        return new String(buffer.toByteArray(), charset);
    }

    /**
     * Limpia el cuerpo MIME extrayendo texto plano y decodificando Quoted-Printable si aplica.
     */
    private String procesarCuerpoMime(String rawBody, boolean qpEnCabeceraPrincipal) {
        if (rawBody == null || rawBody.isEmpty()) {
            return "";
        }

        boolean usarQP = qpEnCabeceraPrincipal
                || rawBody.toLowerCase().contains("content-transfer-encoding: quoted-printable")
                || rawBody.matches("(?s).*=C3=[89AB][0-9A-F].*");

        String cuerpo = usarQP
                ? decodificarQuotedPrintable(rawBody, StandardCharsets.UTF_8)
                : rawBody;

        // Filtrar líneas de fronteras MIME y sub-cabeceras para dejar limpio el comando
        StringBuilder limpio = new StringBuilder();
        for (String linea : cuerpo.split("\r?\n")) {
            String trim = linea.trim();
            String lower = trim.toLowerCase();
            if (trim.startsWith("--")
                    || lower.startsWith("content-type:")
                    || lower.startsWith("content-transfer-encoding:")) {
                continue;
            }
            limpio.append(linea).append("\n");
        }
        return limpio.toString().trim();
    }

    private String extraerEmail(String fromRaw) {
        if (fromRaw == null) {
            return "";
        }
        Matcher matcher = Pattern.compile("<([^>]+)>").matcher(fromRaw);
        if (matcher.find()) {
            return matcher.group(1).trim().toLowerCase();
        }
        Matcher emailMatcher = Pattern.compile(
                "[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\\.[a-zA-Z0-9-.]+"
        ).matcher(fromRaw);
        if (emailMatcher.find()) {
            return emailMatcher.group().trim().toLowerCase();
        }
        return fromRaw.trim().toLowerCase();
    }
}
