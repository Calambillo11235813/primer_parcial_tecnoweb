package uagrm.feder.Presentacion;

import uagrm.feder.Presentacion.ClientePOP3.Correo;

/**
 * Aplicación Frontera / Capa de Presentación: EmailApp.
 * Hilo continuo de sondeo asíncrono sobre el buzón POP3 institucional.
 * Escucha comandos entrantes por correo, delega a CommandParser y responde vía SMTP.
 */
public class EmailApp implements Runnable {

    private static final String MAIL_HOST = "mail.tecnoweb.org.bo";
    private static final int POP3_PORT = 110;
    private static final int SMTP_PORT = 25;

    private static final String POP3_USER = "grupo16sc";
    private static final String POP3_PASS = "grup016grup016*";
    private static final String SMTP_SENDER = "grupo16sc@tecnoweb.org.bo";

    private static final long INTERVALO_SONDEO_MS = 10000L;

    private final CommandParser parser;
    private final ClienteSMTP clienteSMTP;
    private volatile boolean ejecutando;

    public EmailApp() {
        this.parser = new CommandParser();
        this.clienteSMTP = new ClienteSMTP(MAIL_HOST, SMTP_PORT, SMTP_SENDER);
        this.ejecutando = false;
    }

    /**
     * Ciclo principal del hilo de escucha (Sondeo POP3 continuo).
     */
    @Override
    public void run() {
        ejecutando = true;
        System.out.println("====================================================================");
        System.out.println("   SERVICIO DE ESCUCHA POR CORREO INICIADO (SISTEMA FEDER)");
        System.out.println("   Buzón POP3:    " + POP3_USER + "@" + MAIL_HOST + ":" + POP3_PORT);
        System.out.println("   Despacho SMTP: " + SMTP_SENDER + " (Puerto " + SMTP_PORT + ")");
        System.out.println("   Intervalo:     " + (INTERVALO_SONDEO_MS / 1000) + " segundos");
        System.out.println("====================================================================\n");

        while (ejecutando) {
            ClientePOP3 pop3 = new ClientePOP3(MAIL_HOST, POP3_PORT);
            try {
                if (pop3.conectar()) {
                    if (pop3.autenticar(POP3_USER, POP3_PASS)) {
                        int totalCorreos = pop3.obtenerCantidadMensajes();
                        if (totalCorreos > 0) {
                            System.out.println("[POP3] Mensajes nuevos detectados: " + totalCorreos);
                            for (int i = 1; i <= totalCorreos; i++) {
                                Correo correo = pop3.descargarCorreo(i);
                                if (correo != null) {
                                    procesarYResponder(correo);
                                    pop3.borrarCorreo(i);
                                }
                            }
                        }
                    } else {
                        System.err.println("[POP3] Error de autenticación para: " + POP3_USER);
                    }
                }
            } catch (Exception e) {
                System.err.println("[POP3] Excepción durante el sondeo: " + e.getMessage());
            } finally {
                pop3.cerrar();
            }

            try {
                Thread.sleep(INTERVALO_SONDEO_MS);
            } catch (InterruptedException e) {
                System.out.println("[SERVICIO] Hilo de escucha interrumpido.");
                Thread.currentThread().interrupt();
                ejecutando = false;
            }
        }

        System.out.println("[SERVICIO] El servicio de escucha ha finalizado.");
    }

    /**
     * Procesa un correo recibido, invoca al analizador sintáctico y remite la respuesta por SMTP.
     *
     * @param correo Objeto Correo descargado desde el buzón POP3.
     */
    private void procesarYResponder(Correo correo) {
        String remitente = correo.getRemitente();
        String asunto = correo.getAsunto();
        String cuerpo = correo.getCuerpo();

        System.out.println("--------------------------------------------------------------------");
        System.out.println("[IN] CORREO RECIBIDO #" + correo.getNumero());
        System.out.println("     De:     " + remitente);
        System.out.println("     Asunto: " + asunto);
        System.out.println("--------------------------------------------------------------------");

        if (remitente == null || remitente.isEmpty()) {
            System.err.println("[WARN] Remitente vacío. Se omite respuesta.");
            return;
        }

        String respuesta = parser.procesar(remitente, asunto, cuerpo);

        System.out.println("[PROCESADO] Resultado de ejecución:");
        System.out.println(respuesta);

        String asuntoRespuesta = asunto.toUpperCase().startsWith("RE:")
                ? asunto
                : "RE: " + asunto;
        if (asuntoRespuesta.trim().equals("RE:")) {
            asuntoRespuesta = "RE: Notificacion Sistema FEDER";
        }

        System.out.println("[OUT] Despachando respuesta SMTP a: " + remitente + "...");
        boolean enviado = clienteSMTP.enviarCorreo(remitente, asuntoRespuesta, respuesta);

        if (enviado) {
            System.out.println("[OK] Respuesta entregada al servidor SMTP.\n");
        } else {
            System.err.println("[ERROR] Fallo al enviar respuesta SMTP a: " + remitente + "\n");
        }
    }

    /**
     * Inicia el servicio en un hilo separado.
     */
    public void iniciarServicio() {
        Thread workerThread = new Thread(this, "Hilo-EmailApp-FEDER");
        workerThread.setDaemon(false);
        workerThread.start();
    }

    /**
     * Detiene la ejecución del hilo de sondeo de manera limpia.
     */
    public void detenerServicio() {
        this.ejecutando = false;
    }

    /**
     * Punto de entrada principal para arrancar el listener de correo por consola.
     *
     * @param args Argumentos de línea de comandos.
     */
    public static void main(String[] args) {
        EmailApp app = new EmailApp();
        app.iniciarServicio();
    }
}
