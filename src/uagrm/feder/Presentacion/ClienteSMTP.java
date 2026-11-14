package uagrm.feder.Presentacion;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Cliente SMTP puro mediante Sockets TCP para el protocolo RFC 5321.
 * Se conecta a mail.tecnoweb.org.bo:25 para el despacho de correos electrónicos
 * con soporte completo UTF-8 en asunto (RFC 2047) y cuerpo.
 */
public class ClienteSMTP {

    private static final String DEFAULT_HOST = "mail.tecnoweb.org.bo";
    private static final int DEFAULT_PORT = 25;
    private static final String DEFAULT_ORIGEN = "grupo16sc@tecnoweb.org.bo";
    private static final String DEFAULT_HELO = "tecnoweb.org.bo";

    private final String host;
    private final int port;
    private final String remitenteSistema;

    public ClienteSMTP() {
        this(DEFAULT_HOST, DEFAULT_PORT, DEFAULT_ORIGEN);
    }

    public ClienteSMTP(String host, int port, String remitenteSistema) {
        this.host = host;
        this.port = port;
        this.remitenteSistema = remitenteSistema;
    }

    /**
     * Envía un correo electrónico ejecutando la secuencia estándar de comandos SMTP.
     * Secuencia: HELO -> MAIL FROM -> RCPT TO -> DATA -> [Headers + Body] -> . -> QUIT
     *
     * @param destinatario Dirección de correo de destino.
     * @param asunto       Título del correo (se codifica en UTF-8 RFC 2047 si contiene acentos/ñ).
     * @param cuerpo       Contenido en texto plano / tabla ASCII.
     * @return true si el correo fue aceptado por el servidor SMTP.
     */
    public boolean enviarCorreo(String destinatario, String asunto, String cuerpo) {
        try (Socket socket = new Socket(host, port)) {
            socket.setSoTimeout(15000);
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                 DataOutputStream writer = new DataOutputStream(socket.getOutputStream())) {

                // 1. Saludo del servidor (espera código 220)
                String respBanner = leerRespuesta(reader);
                if (!respBanner.startsWith("220")) {
                    System.err.println("[ERROR] SMTP Banner: " + respBanner);
                    return false;
                }

                // 2. HELO tecnoweb.org.bo (espera código 250)
                enviarComando(writer, "HELO " + DEFAULT_HELO);
                String respHelo = leerRespuesta(reader);
                if (!respHelo.startsWith("250")) {
                    System.err.println("[ERROR] SMTP HELO: " + respHelo);
                    return false;
                }

                // 3. MAIL FROM:<origen> (espera código 250)
                enviarComando(writer, "MAIL FROM:<" + remitenteSistema + ">");
                String respMailFrom = leerRespuesta(reader);
                if (!respMailFrom.startsWith("250")) {
                    System.err.println("[ERROR] SMTP MAIL FROM: " + respMailFrom);
                    return false;
                }

                // 4. RCPT TO:<destinatario> (espera código 250)
                enviarComando(writer, "RCPT TO:<" + destinatario.trim() + ">");
                String respRcpt = leerRespuesta(reader);
                if (!respRcpt.startsWith("250")) {
                    System.err.println("[ERROR] SMTP RCPT TO: " + respRcpt);
                    return false;
                }

                // 5. DATA (espera código 354)
                enviarComando(writer, "DATA");
                String respData = leerRespuesta(reader);
                if (!respData.startsWith("354")) {
                    System.err.println("[ERROR] SMTP DATA: " + respData);
                    return false;
                }

                // 6. Envío de cabeceras RFC 822 y cuerpo en UTF-8
                String asuntoCodificado = codificarAsuntoUTF8(asunto);
                StringBuilder mensaje = new StringBuilder();
                mensaje.append("From: \"Sistema FEDER\" <").append(remitenteSistema).append(">\r\n");
                mensaje.append("To: <").append(destinatario.trim()).append(">\r\n");
                mensaje.append("Subject: ").append(asuntoCodificado).append("\r\n");
                mensaje.append("MIME-Version: 1.0\r\n");
                mensaje.append("Content-Type: text/plain; charset=UTF-8\r\n");
                mensaje.append("Content-Transfer-Encoding: 8bit\r\n");
                mensaje.append("\r\n");
                mensaje.append(cuerpo).append("\r\n");
                mensaje.append(".\r\n");

                writer.write(mensaje.toString().getBytes(StandardCharsets.UTF_8));
                writer.flush();

                String respFinData = leerRespuesta(reader);
                if (!respFinData.startsWith("250")) {
                    System.err.println("[ERROR] SMTP Envio Datos: " + respFinData);
                    return false;
                }

                // 7. QUIT (espera código 221)
                enviarComando(writer, "QUIT");
                leerRespuesta(reader);
                return true;
            }
        } catch (IOException e) {
            System.err.println("[ERROR] Excepción I/O en ClienteSMTP: " + e.getMessage());
            return false;
        }
    }

    /**
     * Codifica el asunto bajo RFC 2047 (=?UTF-8?B?...?=) si contiene caracteres fuera de ASCII.
     */
    private String codificarAsuntoUTF8(String asunto) {
        if (asunto == null || asunto.isEmpty()) {
            return "";
        }
        boolean soloAscii = asunto.chars().allMatch(c -> c < 128);
        if (soloAscii) {
            return asunto;
        }
        String base64 = Base64.getEncoder().encodeToString(
                asunto.getBytes(StandardCharsets.UTF_8)
        );
        return "=?UTF-8?B?" + base64 + "?=";
    }

    private void enviarComando(DataOutputStream writer, String comando) throws IOException {
        writer.write((comando + "\r\n").getBytes(StandardCharsets.UTF_8));
        writer.flush();
    }

    private String leerRespuesta(BufferedReader reader) throws IOException {
        String linea = reader.readLine();
        if (linea == null) {
            return "";
        }
        while (linea.length() >= 4 && linea.charAt(3) == '-') {
            linea = reader.readLine();
            if (linea == null) {
                break;
            }
        }
        return linea;
    }
}
