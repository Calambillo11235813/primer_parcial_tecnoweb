package uagrm.feder.Presentacion;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Cliente POP3 puro mediante Sockets TCP para el protocolo RFC 1939.
 * Se conecta a mail.tecnoweb.org.bo:110 para la descarga y gestión de mensajes entrantes.
 */
public class ClientePOP3 {

    private static final String DEFAULT_HOST = "mail.tecnoweb.org.bo";
    private static final int DEFAULT_PORT = 110;

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
     * Establece la conexión física TCP con el servidor POP3 y lee el saludo inicial (+OK).
     */
    public boolean conectar() throws IOException {
        this.socket = new Socket(host, port);
        this.socket.setSoTimeout(15000); // 15s de timeout de lectura
        this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.writer = new DataOutputStream(socket.getOutputStream());

        String saludo = reader.readLine();
        return saludo != null && saludo.startsWith("+OK");
    }

    /**
     * Autentica la sesión POP3 enviando USER y PASS.
     */
    public boolean autenticar(String usuario, String contrasena) throws IOException {
        // 1. Enviar comando USER
        enviarComando("USER " + usuario);
        String respUser = reader.readLine();
        if (respUser == null || !respUser.startsWith("+OK")) {
            System.err.println("Error POP3 en USER: " + respUser);
            return false;
        }

        // 2. Enviar comando PASS
        enviarComando("PASS " + contrasena);
        String respPass = reader.readLine();
        if (respPass == null || !respPass.startsWith("+OK")) {
            System.err.println("Error POP3 en PASS: " + respPass);
            return false;
        }

        return true;
    }

    /**
     * Consulta el estado del buzón con STAT y retorna la cantidad de mensajes disponibles.
     * Respuesta esperada: +OK <cantidad_mensajes> <tamano_octetos>
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
                    System.err.println("Error parseando respuesta STAT: " + respuesta);
                }
            }
        }
        return 0;
    }

    /**
     * Descarga el correo correspondiente al número dado mediante el comando RETR
     * y analiza sus cabeceras (From, Subject) y cuerpo.
     * Lectura multilínea hasta recibir una línea con un punto solitario '.'
     */
    public Correo descargarCorreo(int nro) throws IOException {
        enviarComando("RETR " + nro);
        String linea = reader.readLine();
        if (linea == null || !linea.startsWith("+OK")) {
            System.err.println("Error al recuperar correo #" + nro + ": " + linea);
            return null;
        }

        StringBuilder rawEmail = new StringBuilder();
        while ((linea = reader.readLine()) != null) {
            // El protocolo POP3 finaliza la transmisión multilínea con un punto '.'
            if (linea.equals(".")) {
                break;
            }
            // Byte-stuffing: Si una línea empieza con '..', el primer punto se descarta
            if (linea.startsWith("..")) {
                linea = linea.substring(1);
            }
            rawEmail.append(linea).append("\r\n");
        }

        return parsearMensaje(nro, rawEmail.toString());
    }

    /**
     * Marca un mensaje para su eliminación en el servidor mediante el comando DELE.
     * La eliminación efectiva ocurre al enviar QUIT.
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
                    reader.readLine(); // Leer acuse de QUIT (+OK Goodbye)
                }
            }
        } catch (IOException ignored) {
        } finally {
            try {
                if (reader != null) reader.close();
                if (writer != null) writer.close();
                if (socket != null && !socket.isClosed()) socket.close();
            } catch (IOException e) {
                System.err.println("Error cerrando socket POP3: " + e.getMessage());
            }
        }
    }

    /**
     * Envía una línea de comando terminada estrictamente en CRLF (\r\n).
     */
    private void enviarComando(String comando) throws IOException {
        writer.writeBytes(comando + "\r\n");
        writer.flush();
    }

    /**
     * Parsea el mensaje crudo separando cabeceras RFC 822 del cuerpo del correo.
     * Extrae: From (limpiando nombre y dejando solo el email), Subject y Body.
     */
    private Correo parsearMensaje(int nro, String rawContent) {
        String[] lineas = rawContent.split("\r\n");
        String remitente = "";
        String asunto = "";
        StringBuilder cuerpo = new StringBuilder();

        boolean enCabecera = true;

        for (String linea : lineas) {
            if (enCabecera) {
                // Una línea en blanco marca el fin de los headers y el inicio del cuerpo
                if (linea.trim().isEmpty()) {
                    enCabecera = false;
                    continue;
                }

                String lineaLower = linea.toLowerCase();
                if (lineaLower.startsWith("from:")) {
                    remitente = extraerEmail(linea.substring(5).trim());
                } else if (lineaLower.startsWith("subject:")) {
                    asunto = linea.substring(8).trim();
                }
            } else {
                cuerpo.append(linea).append("\n");
            }
        }

        // Limpieza básica del cuerpo (por si contiene texto plano o firmas)
        String cuerpoTexto = limpiarCuerpoTexto(cuerpo.toString());

        return new Correo(nro, remitente, asunto, cuerpoTexto);
    }

    /**
     * Extrae la dirección de correo limpia.
     * Ejemplo: "Juan Perez <juan@example.com>" -> "juan@example.com"
     */
    private String extraerEmail(String fromRaw) {
        if (fromRaw == null) return "";
        // Si contiene formato <email@dominio.com>
        Pattern pattern = Pattern.compile("<([^>]+)>");
        Matcher matcher = pattern.matcher(fromRaw);
        if (matcher.find()) {
            return matcher.group(1).trim().toLowerCase();
        }
        // Si viene directo como texto
        Pattern emailPattern = Pattern.compile("[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\\.[a-zA-Z0-9-.]+");
        Matcher emailMatcher = emailPattern.matcher(fromRaw);
        if (emailMatcher.find()) {
            return emailMatcher.group().trim().toLowerCase();
        }
        return fromRaw.trim().toLowerCase();
    }

    /**
     * Limpia secuencias MIME comunes o saltos de línea sobrantes del cuerpo.
     */
    private String limpiarCuerpoTexto(String cuerpo) {
        if (cuerpo == null) return "";
        // Eliminar saltos excesivos y espacios al borde
        return cuerpo.trim();
    }
}

