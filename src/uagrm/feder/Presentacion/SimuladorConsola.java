package uagrm.feder.Presentacion;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Simulador Interactivo de Terminal Telnet SMTP (Puerto 25) para el Sistema FEDER.
 * Emula la máquina de estados y las validaciones de sintaxis RFC 5321 de Postfix:
 * - Valida secuencia obligatoria: HELO/EHLO -> MAIL FROM:<email> -> RCPT TO:<email> -> DATA -> '.' -> QUIT.
 * - Valida formato sintáctico estricto de direcciones entre corchetes angulares <usuario@dominio>.
 * - Rechaza comandos no reconocidos o fuera de orden con códigos de error estándar (500, 501, 503).
 */
public class SimuladorConsola {

    private enum EstadoSMTP {
        INICIO, HELO_OK, MAIL_FROM_OK, RCPT_TO_OK, EN_DATA
    }

    // Expresión regular RFC 5321 para validar sintaxis de dirección: <usuario@dominio>
    private static final Pattern PATRON_MAIL_FROM = Pattern.compile(
            "^MAIL\\s+FROM:\\s*<([a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\\.[a-zA-Z0-9-.]+)>$",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern PATRON_RCPT_TO = Pattern.compile(
            "^RCPT\\s+TO:\\s*<([a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\\.[a-zA-Z0-9-.]+)>$",
            Pattern.CASE_INSENSITIVE
    );

    public static void main(String[] args) {
        // Asegurar que la salida de consola use UTF-8 estándar
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8);
        CommandParser parser = new CommandParser();

        System.out.println("========================================================================");
        System.out.println("         SIMULADOR DE TERMINAL TELNET SMTP (PUERTO 25) - FEDER          ");
        System.out.println("========================================================================");
        System.out.println("Conectado a mail.tecnoweb.org.bo (SIMULADO)...");
        System.out.println("220 mail.tecnoweb.org.bo ESMTP Postfix (FEDER Simulator)");
        System.out.println("Secuencia requerida: HELO -> MAIL FROM:<...> -> RCPT TO:<...> -> DATA\n");

        EstadoSMTP estado = EstadoSMTP.INICIO;
        String mailFrom = "";
        String rcptTo = "";
        StringBuilder bufferData = new StringBuilder();

        while (true) {
            if (!scanner.hasNextLine()) {
                break;
            }
            String linea = scanner.nextLine();

            // -------------------------------------------------------------
            // MODO DATA: Captura del mensaje hasta encontrar una línea con un punto '.'
            // -------------------------------------------------------------
            if (estado == EstadoSMTP.EN_DATA) {
                if (linea.trim().equals(".")) {
                    System.out.println("250 2.0.0 Ok: queued as FEDER-" + System.currentTimeMillis());

                    // Procesar a través de las capas reales (CommandParser -> N... -> D... -> PostgreSQL)
                    procesarMensajeTelnet(parser, mailFrom, rcptTo, bufferData.toString());

                    // Limpiar y resetear para la siguiente transacción de correo
                    bufferData.setLength(0);
                    estado = EstadoSMTP.HELO_OK;
                } else {
                    bufferData.append(linea).append("\n");
                }
                continue;
            }

            String trimLine = linea.trim();
            if (trimLine.isEmpty()) {
                continue;
            }

            String upperLine = trimLine.toUpperCase();

            // 1. Comando HELO / EHLO
            if (upperLine.startsWith("HELO") || upperLine.startsWith("EHLO")) {
                String[] partes = trimLine.split("\\s+");
                if (partes.length < 2) {
                    System.out.println("501 5.5.4 Syntax: HELO <hostname>");
                } else {
                    estado = EstadoSMTP.HELO_OK;
                    mailFrom = "";
                    rcptTo = "";
                    bufferData.setLength(0);
                    System.out.println("250 mail.tecnoweb.org.bo Hello " + partes[1] + ", pleased to meet you");
                }
            }
            // 2. Comando MAIL FROM:<...>
            else if (upperLine.startsWith("MAIL FROM")) {
                if (estado == EstadoSMTP.INICIO) {
                    System.out.println("503 5.5.1 Error: send HELO/EHLO first");
                    continue;
                }

                Matcher matcher = PATRON_MAIL_FROM.matcher(trimLine);
                if (!matcher.matches()) {
                    System.out.println("501 5.1.7 Bad sender address syntax. Formato esperado: MAIL FROM:<usuario@dominio>");
                } else {
                    mailFrom = matcher.group(1).toLowerCase();
                    estado = EstadoSMTP.MAIL_FROM_OK;
                    System.out.println("250 2.1.0 Ok");
                }
            }
            // 3. Comando RCPT TO:<...>
            else if (upperLine.startsWith("RCPT TO")) {
                if (estado != EstadoSMTP.MAIL_FROM_OK && estado != EstadoSMTP.RCPT_TO_OK) {
                    System.out.println("503 5.5.1 Error: need MAIL FROM command before RCPT TO");
                    continue;
                }

                Matcher matcher = PATRON_RCPT_TO.matcher(trimLine);
                if (!matcher.matches()) {
                    System.out.println("501 5.1.3 Bad recipient address syntax. Formato esperado: RCPT TO:<usuario@dominio>");
                } else {
                    rcptTo = matcher.group(1).toLowerCase();
                    estado = EstadoSMTP.RCPT_TO_OK;
                    System.out.println("250 2.1.5 Ok");
                }
            }
            // 4. Comando DATA
            else if (upperLine.equals("DATA")) {
                if (estado != EstadoSMTP.RCPT_TO_OK) {
                    System.out.println("503 5.5.1 Error: need valid RCPT TO command before DATA");
                    continue;
                }
                estado = EstadoSMTP.EN_DATA;
                bufferData.setLength(0);
                System.out.println("354 End data with <CR><LF>.<CR><LF>");
            }
            // 5. Comando RSET (Reset de transacción)
            else if (upperLine.equals("RSET")) {
                estado = (estado != EstadoSMTP.INICIO) ? EstadoSMTP.HELO_OK : EstadoSMTP.INICIO;
                mailFrom = "";
                rcptTo = "";
                bufferData.setLength(0);
                System.out.println("250 2.0.0 Ok (Transaction reset)");
            }
            // 6. Comando NOOP
            else if (upperLine.equals("NOOP")) {
                System.out.println("250 2.0.0 Ok");
            }
            // 7. Comando QUIT / EXIT
            else if (upperLine.equals("QUIT") || upperLine.equals("EXIT") || upperLine.equals(":SALIR")) {
                System.out.println("221 2.0.0 Bye (Cerrando conexion Telnet)");
                break;
            }
            // 8. Comando HELP de SMTP
            else if (upperLine.equals("HELP")) {
                System.out.println("214-Comandos SMTP RFC 5321 soportados:");
                System.out.println("214-  HELO <dominio>");
                System.out.println("214-  MAIL FROM:<remitente@dominio>");
                System.out.println("214-  RCPT TO:<destinatario@dominio>");
                System.out.println("214-  DATA");
                System.out.println("214-  RSET");
                System.out.println("214   QUIT");
            }
            // 9. Comando desconocido
            else {
                System.out.println("500 5.5.2 Error: command not recognized ('" + trimLine + "')");
            }
        }

        scanner.close();
    }

    /**
     * Parsea las cabeceras From, Subject y el Cuerpo enviados en DATA, y los procesa con CommandParser.
     */
    private static void procesarMensajeTelnet(CommandParser parser, String mailFrom,
            String rcptTo, String contenidoData) {
        String[] lineas = contenidoData.split("\n");
        String subject = "";
        String fromHeader = "";
        StringBuilder cuerpo = new StringBuilder();
        boolean enCabecera = true;

        for (String linea : lineas) {
            String trim = linea.trim();
            if (enCabecera) {
                if (trim.isEmpty()) {
                    enCabecera = false;
                    continue;
                }
                String lower = trim.toLowerCase();
                if (lower.startsWith("subject:")) {
                    subject = trim.substring(8).trim();
                } else if (lower.startsWith("from:")) {
                    fromHeader = trim.substring(5).trim();
                }
            } else {
                cuerpo.append(linea).append("\n");
            }
        }

        // Determinar remitente efectivo (prioriza From: en header, sino MAIL FROM)
        String remitenteEfectivo = !fromHeader.isEmpty() ? fromHeader : mailFrom;
        remitenteEfectivo = remitenteEfectivo.replaceAll("[<>]", "").trim().toLowerCase();

        System.out.println("\n------------------------------------------------------------------------");
        System.out.println("[IN] CORREO SIMULADO ENTREGADO AL SISTEMA");
        System.out.println("     De:      " + remitenteEfectivo);
        System.out.println("     Para:    " + rcptTo);
        System.out.println("     Asunto:  " + subject);
        System.out.println("     Cuerpo:  " + cuerpo.toString().trim());
        System.out.println("------------------------------------------------------------------------");

        long inicio = System.currentTimeMillis();
        // Ejecución en la lógica de negocio y persistencia en PostgreSQL
        String respuesta = parser.procesar(remitenteEfectivo, subject, cuerpo.toString());
        long tiempo = System.currentTimeMillis() - inicio;

        System.out.println("[OUT] RESPUESTA SMTP GENERADA (" + tiempo + " ms):");
        System.out.println("De: \"Sistema FEDER\" <grupo16sc@tecnoweb.org.bo>");
        System.out.println("Para: " + remitenteEfectivo);
        System.out.println("Asunto: RE: " + (!subject.isEmpty() ? subject : "Notificacion FEDER"));
        System.out.println();
        System.out.println(respuesta);
        System.out.println("------------------------------------------------------------------------\n");
    }
}
