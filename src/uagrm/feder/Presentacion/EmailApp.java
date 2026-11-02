package uagrm.feder.Presentacion;

import uagrm.feder.Presentacion.ClientePOP3.Correo;

/**
 * Aplicación Frontera / Capa de Presentación: EmailApp
 * Hilo continuo de sondeo asíncrono sobre el buzón POP3 institucional.
 * Escucha comandos entrantes enviados por correo, los analiza con CommandParser,
 * ejecuta la lógica de negocio y despacha las respuestas automáticas vía SMTP.
 */
public class EmailApp implements Runnable {

    // Parámetros de conexión a los servidores de correo
    private static final String MAIL_HOST = "mail.tecnoweb.org.bo";
    private static final int POP3_PORT = 110;
    private static final int SMTP_PORT = 25;

    private static final String POP3_USER = "grupo16sc";
    private static final String POP3_PASS = "grupoxxsc"; // Contraseña del buzón institucional
    private static final String SMTP_SENDER = "grupo16sc@tecnoweb.org.bo";

    private static final long INTERVALO_SONDEO_MS = 10000; // 10 segundos entre cada revisión

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
        System.out.println("   SERVICIO DE ESCUCHA POR CORREO INICIADO (SISTEMA FEDER)          ");
        System.out.println("   Buzón: " + POP3_USER + "@" + MAIL_HOST + " (Puerto " + POP3_PORT + ")  ");
        System.out.println("   Despacho SMTP: " + SMTP_SENDER + " (Puerto " + SMTP_PORT + ")          ");
        System.out.println("   Intervalo de sondeo: " + (INTERVALO_SONDEO_MS / 1000) + " segundos");
        System.out.println("====================================================================\n");

        while (ejecutando) {
            ClientePOP3 pop3 = new ClientePOP3(MAIL_HOST, POP3_PORT);
            try {
                // 1. Conectar y autenticar en el servidor POP3
                if (pop3.conectar()) {
                    if (pop3.autenticar(POP3_USER, POP3_PASS)) {
                        // 2. Verificar cantidad de correos en el buzón con STAT
                        int totalCorreos = pop3.obtenerCantidadMensajes();

                        if (totalCorreos > 0) {
                            System.out.println("[POP3] Mensajes nuevos detectados en el buzón: " + totalCorreos);

                            // 3. Procesar cada correo (los índices POP3 van de 1 a totalCorreos)
                            for (int i = 1; i <= totalCorreos; i++) {
                                Correo correo = pop3.descargarCorreo(i);
                                if (correo != null) {
                                    procesarYResponder(correo);
                                    // 4. Marcar para eliminación en el servidor para evitar reprocesamiento
                                    pop3.borrarCorreo(i);
                                }
                            }
                        }
                    } else {
                        System.err.println("[POP3] Error de autenticación para usuario: " + POP3_USER);
                    }
                }
            } catch (Exception e) {
                System.err.println("[POP3] Excepción durante el sondeo del buzón: " + e.getMessage());
            } finally {
                // Cerrar sesión POP3 (al enviar QUIT se confirman los borrados con DELE)
                pop3.cerrar();
            }

            // Pausa antes de la siguiente inspección del buzón
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
     */
    private void procesarYResponder(Correo correo) {
        String remitente = correo.getRemitente();
        String asunto = correo.getAsunto();
        String cuerpo = correo.getCuerpo();

        System.out.println("--------------------------------------------------------------------");
        System.out.println("📥 [CORREO RECIBIDO #" + correo.getNumero() + "]");
        System.out.println("   De:      " + remitente);
        System.out.println("   Asunto:  " + asunto);
        System.out.println("--------------------------------------------------------------------");

        if (remitente == null || remitente.isEmpty()) {
            System.err.println("⚠ No se pudo identificar la dirección del remitente. Se omite respuesta.");
            return;
        }

        // Delegar análisis sintáctico y ejecución de lógica de negocio
        String respuesta = parser.procesar(remitente, asunto, cuerpo);

        System.out.println("⚙ [RESULTADO DE EJECUCIÓN]");
        System.out.println(respuesta);

        // Despachar acuse/respuesta por SMTP
        String asuntoRespuesta = asunto.toUpperCase().startsWith("RE:") ? asunto : "RE: " + asunto;
        if (asuntoRespuesta.trim().equals("RE:")) {
            asuntoRespuesta = "RE: Notificacion Sistema FEDER";
        }

        System.out.println("📤 [DESPACHANDO RESPUESTA SMTP A: " + remitente + "]...");
        boolean enviado = clienteSMTP.enviarCorreo(remitente, asuntoRespuesta, respuesta);

        if (enviado) {
            System.out.println("✔ Respuesta entregada satisfactoriamente al servidor SMTP.\n");
        } else {
            System.err.println("✘ Error al enviar la respuesta SMTP a: " + remitente + "\n");
        }
    }

    /**
     * Inicia el servicio en un hilo separado de fondo.
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
     */
    public static void main(String[] args) {
        EmailApp app = new EmailApp();
        app.iniciarServicio();
    }
}

