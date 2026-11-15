package uagrm.feder.Negocio;

import uagrm.feder.Datos.DConsulta;
import uagrm.feder.Datos.DUsuario;
import uagrm.feder.Entity.Consulta;
import uagrm.feder.Entity.Usuario;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Capa de Negocio: NConsulta.
 * Controla la lógica del CU-07: Gestionar Bandeja de Consultas
 * (RF-16: Gestión de mensajes de contacto, clasificación de estado y respuesta).
 */
public class NConsulta {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    private final DConsulta dConsulta;
    private final DUsuario dUsuario;

    public NConsulta() {
        this.dConsulta = new DConsulta();
        this.dUsuario = new DUsuario();
    }

    /**
     * Verifica si el correo del remitente pertenece a un usuario activo con rol
     * 'Director' o 'Secretaria' (roles autorizados para administrar la bandeja de consultas).
     *
     * @param senderEmail Correo electrónico del remitente.
     * @return true si tiene permisos sobre la bandeja de consultas.
     */
    public boolean esAutorizadoBandeja(String senderEmail) {
        if (senderEmail == null || senderEmail.trim().isEmpty()) {
            return false;
        }
        String rol = dUsuario.getRolByEmail(senderEmail.trim());
        return rol != null
                && (rol.equalsIgnoreCase("Director") || rol.equalsIgnoreCase("Secretaria"));
    }

    /**
     * Registra una nueva consulta ciudadana en la bandeja interna con estado 'Pendiente'.
     * Soporta 3 parámetros: INSCON [nombre_remitente; asunto; mensaje] (toma correo del From)
     * o 4 parámetros: INSCON [nombre_remitente; correo_remitente; asunto; mensaje]
     *
     * @param params      Lista con 3 o 4 parámetros.
     * @param senderEmail Correo electrónico desde el cual se envió el mensaje.
     * @return Mensaje de acuse de recibo o descripción del error.
     */
    public String registrarConsulta(List<String> params, String senderEmail) {
        if (params == null || (params.size() != 3 && params.size() != 4)) {
            return "ERROR [400]: Cantidad de parámetros inválida para INSCON. "
                    + "Uso: INSCON [nombre_remitente; asunto; mensaje] o "
                    + "INSCON [nombre_remitente; correo_remitente; asunto; mensaje]";
        }

        String nombreRemitente;
        String correoRemitente;
        String asunto;
        String mensaje;

        if (params.size() == 3) {
            nombreRemitente = params.get(0) != null ? params.get(0).trim() : "";
            correoRemitente = senderEmail != null ? senderEmail.trim() : "";
            asunto = params.get(1) != null ? params.get(1).trim() : "";
            mensaje = params.get(2) != null ? params.get(2).trim() : "";
        } else {
            nombreRemitente = params.get(0) != null ? params.get(0).trim() : "";
            correoRemitente = params.get(1) != null ? params.get(1).trim() : "";
            asunto = params.get(2) != null ? params.get(2).trim() : "";
            mensaje = params.get(3) != null ? params.get(3).trim() : "";
        }

        if (nombreRemitente.isEmpty() || nombreRemitente.length() > 150) {
            return "ERROR [422]: El nombre del remitente no puede estar vacío "
                    + "ni exceder 150 caracteres.";
        }
        if (correoRemitente.isEmpty() || correoRemitente.length() > 150
                || !EMAIL_PATTERN.matcher(correoRemitente).matches()) {
            return "ERROR [422]: El correo del remitente '" + correoRemitente
                    + "' no tiene un formato válido.";
        }
        if (asunto.isEmpty() || asunto.length() > 150) {
            return "ERROR [422]: El asunto de la consulta no puede estar vacío "
                    + "ni exceder 150 caracteres.";
        }
        if (mensaje.isEmpty()) {
            return "ERROR [422]: El mensaje de la consulta no puede estar vacío.";
        }

        boolean ok = dConsulta.insert(nombreRemitente, correoRemitente, asunto, mensaje);
        if (ok) {
            return "SUCCESS: Su consulta ('" + asunto + "') ha sido registrada en la "
                    + "bandeja de atención ciudadana de FEDER con estado 'Pendiente'. "
                    + "Pronto recibirá una respuesta institucional.";
        }
        return "ERROR [500]: Fallo al registrar la consulta en la base de datos.";
    }

    /**
     * Clasifica el estado de una consulta ciudadana ('Pendiente', 'En Proceso', 'Atendido').
     * Comando: MODCON [id_consulta; estado]
     *
     * @param params      Lista con [id_consulta, estado].
     * @param senderEmail Correo del operador (requiere rol 'Director' o 'Secretaria').
     * @return Mensaje de confirmación o descripción del error.
     */
    public String clasificarEstado(List<String> params, String senderEmail) {
        if (!esAutorizadoBandeja(senderEmail)) {
            return "ERROR [403]: Acceso denegado. Solo los roles 'Director' y 'Secretaria' "
                    + "pueden clasificar el estado de la bandeja de consultas.";
        }

        if (params == null || params.size() != 2) {
            return "ERROR [400]: Cantidad de parámetros inválida para MODCON. "
                    + "Se esperan 2: [id_consulta; estado]";
        }

        int id;
        try {
            id = Integer.parseInt(params.get(0).trim());
        } catch (NumberFormatException e) {
            return "ERROR [422]: El ID de la consulta debe ser un número entero válido.";
        }

        String estadoRaw = params.get(1) != null ? params.get(1).trim() : "";
        String estadoNormalizado = normalizarEstadoConsulta(estadoRaw);
        if (estadoNormalizado == null) {
            return "ERROR [422]: Estado inválido ('" + estadoRaw
                    + "'). Valores permitidos: 'Pendiente', 'En Proceso' o 'Atendido'.";
        }

        Consulta consulta = dConsulta.getById(id);
        if (consulta == null) {
            return "ERROR [404]: No existe ninguna consulta registrada con ID " + id + ".";
        }

        Usuario operador = dUsuario.getByEmail(senderEmail);
        if (operador == null) {
            return "ERROR [401]: No se pudo identificar al operador '" + senderEmail + "'.";
        }

        boolean ok = dConsulta.updateEstado(
                id, estadoNormalizado, operador.getId_usuario()
        );
        if (ok) {
            return "SUCCESS: Consulta ID " + id + " ('" + consulta.getAsunto()
                    + "') clasificada con estado '" + estadoNormalizado
                    + "' por " + operador.getNombre_completo() + ".";
        }
        return "ERROR [500]: Fallo al actualizar el estado de la consulta.";
    }

    /**
     * Emite una respuesta institucional a una consulta ciudadana, actualiza su estado
     * a 'Atendido', registra al funcionario responsable y audita la operación.
     * Comando: RESCON [id_consulta; respuesta_institucional]
     *
     * @param params      Lista con [id_consulta, respuesta_institucional].
     * @param senderEmail Correo del operador (requiere rol 'Director' o 'Secretaria').
     * @return Comprobante de respuesta institucional o descripción del error.
     */
    public String responderConsulta(List<String> params, String senderEmail) {
        if (!esAutorizadoBandeja(senderEmail)) {
            return "ERROR [403]: Acceso denegado. Solo los roles 'Director' y 'Secretaria' "
                    + "pueden emitir respuestas institucionales a consultas.";
        }

        if (params == null || params.size() != 2) {
            return "ERROR [400]: Cantidad de parámetros inválida para RESCON. "
                    + "Se esperan 2: [id_consulta; respuesta_institucional]";
        }

        int id;
        try {
            id = Integer.parseInt(params.get(0).trim());
        } catch (NumberFormatException e) {
            return "ERROR [422]: El ID de la consulta debe ser un número entero válido.";
        }

        String respuesta = params.get(1) != null ? params.get(1).trim() : "";
        if (respuesta.isEmpty()) {
            return "ERROR [422]: El texto de la respuesta institucional no puede estar vacío.";
        }

        Consulta consulta = dConsulta.getById(id);
        if (consulta == null) {
            return "ERROR [404]: No existe ninguna consulta registrada con ID " + id + ".";
        }

        Usuario operador = dUsuario.getByEmail(senderEmail);
        if (operador == null) {
            return "ERROR [401]: No se pudo identificar al operador '" + senderEmail + "'.";
        }

        boolean ok = dConsulta.updateEstado(id, "Atendido", operador.getId_usuario());
        if (ok) {
            return "SUCCESS: Consulta ID " + id + " marcada como 'Atendido'.\n"
                    + "--------------------------------------------------------------------\n"
                    + "COMPROBANTE DE RESPUESTA INSTITUCIONAL FEDER\n"
                    + "--------------------------------------------------------------------\n"
                    + "Destinatario: " + consulta.getNombre_remitente()
                    + " <" + consulta.getCorreo_remitente() + ">\n"
                    + "Asunto Orig.: " + consulta.getAsunto() + "\n"
                    + "Consulta:     " + consulta.getMensaje() + "\n"
                    + "Atendido por: " + operador.getNombre_completo() + "\n"
                    + "Respuesta:    " + respuesta + "\n"
                    + "--------------------------------------------------------------------";
        }
        return "ERROR [500]: Fallo al registrar la atención de la consulta.";
    }

    /**
     * Elimina una consulta de la bandeja interna y audita la acción.
     * Comando: DELCON [id_consulta]
     *
     * @param params      Lista con [id_consulta].
     * @param senderEmail Correo del operador (requiere rol 'Director' o 'Secretaria').
     * @return Mensaje de confirmación o descripción del error.
     */
    public String eliminarConsulta(List<String> params, String senderEmail) {
        if (!esAutorizadoBandeja(senderEmail)) {
            return "ERROR [403]: Acceso denegado. Solo los roles 'Director' y 'Secretaria' "
                    + "pueden eliminar registros de la bandeja de consultas.";
        }

        if (params == null || params.size() != 1) {
            return "ERROR [400]: Cantidad de parámetros inválida para DELCON. "
                    + "Se espera 1: [id_consulta]";
        }

        int id;
        try {
            id = Integer.parseInt(params.get(0).trim());
        } catch (NumberFormatException e) {
            return "ERROR [422]: El ID de la consulta debe ser un número entero válido.";
        }

        Consulta consulta = dConsulta.getById(id);
        if (consulta == null) {
            return "ERROR [404]: No existe ninguna consulta registrada con ID " + id + ".";
        }

        Usuario operador = dUsuario.getByEmail(senderEmail);
        if (operador == null) {
            return "ERROR [401]: No se pudo identificar al operador '" + senderEmail + "'.";
        }

        boolean ok = dConsulta.delete(id, operador.getId_usuario());
        if (ok) {
            return "SUCCESS: Consulta ID " + id + " ('" + consulta.getAsunto()
                    + "') eliminada de la bandeja exitosamente.";
        }
        return "ERROR [500]: Fallo al eliminar la consulta.";
    }

    /**
     * Lista las consultas de la bandeja interna, opcionalmente filtradas por estado.
     * Comando: LISCON [] o LISCON [estado]
     *
     * @param params      Lista vacía o con 1 parámetro [estado].
     * @param senderEmail Correo del operador (requiere rol 'Director' o 'Secretaria').
     * @return Lista de filas String[] con las consultas.
     */
    public List<String[]> listarConsultas(List<String> params, String senderEmail) {
        if (!esAutorizadoBandeja(senderEmail)) {
            throw new SecurityException(
                    "ERROR [403]: Acceso denegado. Solo los roles 'Director' y 'Secretaria' "
                    + "pueden consultar la bandeja de atención ciudadana."
            );
        }

        String filtroEstado = null;
        if (params != null && !params.isEmpty() && !params.get(0).trim().isEmpty()) {
            String estadoNormalizado = normalizarEstadoConsulta(params.get(0).trim());
            if (estadoNormalizado == null) {
                throw new IllegalArgumentException(
                        "ERROR [422]: Filtro de estado inválido ('" + params.get(0).trim()
                        + "'). Use 'Pendiente', 'En Proceso', 'Atendido' o vacío []."
                );
            }
            filtroEstado = estadoNormalizado;
        }
        return dConsulta.list(filtroEstado);
    }

    /**
     * Obtiene una consulta por su ID para permitir el despacho SMTP al ciudadano.
     *
     * @param id Identificador de la consulta.
     * @return Instancia de Consulta o null si no existe.
     */
    public Consulta obtenerPorId(int id) {
        return dConsulta.getById(id);
    }

    private String normalizarEstadoConsulta(String estado) {
        if (estado == null) {
            return null;
        }
        if (estado.equalsIgnoreCase("Pendiente")) {
            return "Pendiente";
        }
        if (estado.equalsIgnoreCase("En Proceso")
                || estado.equalsIgnoreCase("En_Proceso")) {
            return "En Proceso";
        }
        if (estado.equalsIgnoreCase("Atendido") || estado.equalsIgnoreCase("Atendida")) {
            return "Atendido";
        }
        return null;
    }
}
