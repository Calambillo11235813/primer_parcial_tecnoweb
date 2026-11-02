package uagrm.feder.Presentacion;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Cliente SMTP puro mediante Sockets TCP para el protocolo RFC 5321.
 * Se conecta a mail.tecnoweb.org.bo:25 para el despacho de correos electrónicos
 * de respuesta con tablas y acuses estructurados.
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
     * @param asunto       Título del correo.
     * @param cuerpo       Contenido en texto plano / tabla ASCII.
     * @return true si el correo fue aceptado por el servidor SMTP.
     */
    public boolean enviarCorreo(String destinatario, String asunto, String cuerpo) {
        Socket socket = null;
        BufferedReader reader = null;
        DataOutputStream writer = null;

        try {
            socket = new Socket(host, port);
            socket.setSoTimeout(15000); // 15s de timeout de lectura
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            writer = new DataOutputStream(socket.getOutputStream());

            // 1. Saludo del servidor (espera código 220)
            String respBanner = leerRespuesta(reader);
            if (!respBanner.startsWith("220")) {
                System.err.println("SMTP Banner Error: " + respBanner);
                return false;
            }

            // 2. HELO tecnoweb.org.bo (espera código 250)
            enviarComando(writer, "HELO " + DEFAULT_HELO);
            String respHelo = leerRespuesta(reader);
            if (!respHelo.startsWith("250")) {
                System.err.println("SMTP HELO Error: " + respHelo);
                return false;
            }

            // 3. MAIL FROM:<origen> (espera código 250)
            enviarComando(writer, "MAIL FROM:<" + remitenteSistema + ">");
            String respMailFrom = leerRespuesta(reader);
            if (!respMailFrom.startsWith("250")) {
                System.err.println("SMTP MAIL FROM Error: " + respMailFrom);
                return false;
            }

            // 4. RCPT TO:<destinatario> (espera código 250)
            enviarComando(writer, "RCPT TO:<" + destinatario.trim() + ">");
            String respRcpt = leerRespuesta(reader);
            if (!respRcpt.startsWith("250")) {
                System.err.println("SMTP RCPT TO Error: " + respRcpt);
                return false;
            }

            // 5. DATA (espera código 354)
            enviarComando(writer, "DATA");
            String respData = leerRespuesta(reader);
            if (!respData.startsWith("354")) {
                System.err.println("SMTP DATA Error: " + respData);
                return false;
            }

            // 6. Envío de cabeceras RFC 822 y cuerpo
            StringBuilder mensaje = new StringBuilder();
            mensaje.append("From: \"Sistema FEDER\" <").append(remitenteSistema).append(">\r\n");
            mensaje.append("To: <").append(destinatario.trim()).append(">\r\n");
            mensaje.append("Subject: ").append(asunto).append("\r\n");
            mensaje.append("MIME-Version: 1.0\r\n");
            mensaje.append("Content-Type: text/plain; charset=UTF-8\r\n");
            mensaje.append("Content-Transfer-Encoding: 8bit\r\n");
            mensaje.append("\r\n"); // Línea en blanco obligatoria que separa cabeceras del cuerpo

            // Cuerpo del mensaje
            mensaje.append(cuerpo).append("\r\n");

            // Finalizador de transmisión SMTP: CRLF . CRLF
            mensaje.append(".\r\n");

            byte[] bytesMensaje = mensaje.toString().getBytes(StandardCharsets.UTF_8);
            writer.write(bytesMensaje);
            writer.flush();

            String respFinData = leerRespuesta(reader);
            if (!respFinData.startsWith("250")) {
                System.err.println("SMTP Envio Datos Error: " + respFinData);
                return false;
            }

            // 7. QUIT (espera código 221)
            enviarComando(writer, "QUIT");
            leerRespuesta(reader);

            return true;
        } catch (IOException e) {
            System.err.println("Excepción I/O en ClienteSMTP: " + e.getMessage());
            return false;
        } finally {
            cerrarRecursos(reader, writer, socket);
        }
    }

    /**
     * Escribe un comando en el canal TCP asegurando el terminador de línea CRLF (\r\n).
     */
    private void enviarComando(DataOutputStream writer, String comando) throws IOException {
        writer.writeBytes(comando + "\r\n");
        writer.flush();
    }

    /**
     * Lee la respuesta del servidor SMTP, gestionando posibles respuestas multilínea (ej. 250-...).
     */
    private String leerRespuesta(BufferedReader reader) throws IOException {
        String linea = reader.readLine();
        if (linea == null) return "";

        // Si la respuesta es multilínea (código seguido de guión '-'), leer hasta el final
        while (linea.length() >= 4 && linea.charAt(3) == '-') {
            linea = reader.readLine();
            if (linea == null) break;
        }
        return linea;
    }

    /**
     * Cierra de forma ordenada los descriptores de entrada/salida y el Socket TCP.
     */
    private void cerrarRecursos(BufferedReader reader, DataOutputStream writer, Socket socket) {
        try {
            if (reader != null) reader.close();
            if (writer != null) writer.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            System.err.println("Error cerrando socket SMTP: " + e.getMessage());
        }
    }
}

