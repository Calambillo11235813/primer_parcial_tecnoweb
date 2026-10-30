package uagrm.feder.Negocio;

import uagrm.feder.Datos.DUsuario;
import uagrm.feder.Entity.Usuario;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Capa de Negocio: NUsuario
 * Controla la lógica del CU-01 (Gestionar Usuarios y Roles).
 * Aplica reglas de autorización por remitente, validación de integridad y cifrado de credenciales.
 */
public class NUsuario {

    private final DUsuario dUsuario;

    // Expresión regular estándar RFC 5322 simplificada para validación de emails
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    public NUsuario() {
        this.dUsuario = new DUsuario();
    }

    public NUsuario(DUsuario dUsuario) {
        this.dUsuario = dUsuario;
    }

    /**
     * Valida si el remitente posee el rol requerido para ejecutar la operación.
     */
    public boolean verificarRemitente(String senderEmail, String rolRequerido) {
        if (senderEmail == null || senderEmail.trim().isEmpty()) {
            return false;
        }
        String rolActual = dUsuario.getRolByEmail(senderEmail.trim());
        return rolActual != null && rolActual.equalsIgnoreCase(rolRequerido);
    }

    /**
     * CU-01 / RF-14, RF-15: Registrar nuevo usuario en el sistema.
     * Comando: INSUSR [nombre_completo; correo_electronico; contrasena; rol_id]
     * Restricción: Exclusivo para el rol 'Director'.
     */
    public String registrar(List<String> params, String senderEmail) {
        // 1. Control de autorización
        if (!verificarRemitente(senderEmail, "Director")) {
            return "ERROR [403]: Acceso denegado. Solo el rol 'Director' tiene autorización para registrar usuarios.";
        }

        // 2. Validación de número de parámetros
        if (params == null || params.size() != 4) {
            return "ERROR [400]: Cantidad de parámetros inválida para INSUSR. Se esperan 4 parámetros: " +
                   "[nombre_completo; correo_electronico; contrasena; rol_id]";
        }

        String nombre = params.get(0) != null ? params.get(0).trim() : "";
        String email = params.get(1) != null ? params.get(1).trim().toLowerCase() : "";
        String passPlano = params.get(2) != null ? params.get(2).trim() : "";
        String strRolId = params.get(3) != null ? params.get(3).trim() : "";

        // 3. Validaciones de negocio en campos
        if (nombre.isEmpty()) {
            return "ERROR [422]: El nombre completo no puede estar vacío.";
        }
        if (!isValidEmail(email)) {
            return "ERROR [422]: El formato del correo electrónico '" + email + "' no es válido.";
        }
        if (passPlano.length() < 6) {
            return "ERROR [422]: La contraseña debe tener al menos 6 caracteres.";
        }

        int rolId;
        try {
            rolId = Integer.parseInt(strRolId);
        } catch (NumberFormatException e) {
            return "ERROR [422]: El identificador del rol debe ser un número entero válido (recibido: '" + strRolId + "').";
        }

        if (!dUsuario.existeRol(rolId)) {
            return "ERROR [404]: El rol especificado (ID " + rolId + ") no existe en el sistema.";
        }

        // 4. Validación de unicidad de correo
        Usuario usuarioExistente = dUsuario.getByEmail(email);
        if (usuarioExistente != null) {
            return "ERROR [409]: Ya existe un usuario registrado con el correo '" + email + "'.";
        }

        // 5. Obtención del usuario operador para auditoría
        Usuario operador = dUsuario.getByEmail(senderEmail);
        if (operador == null) {
            return "ERROR [401]: No se pudo identificar al usuario operador con correo '" + senderEmail + "'.";
        }

        // 6. Cifrado criptográfico con SHA-256
        String passHash = calcularHashSHA256(passPlano);

        // 7. Persistencia en capa de datos
        boolean insertado = dUsuario.insert(nombre, email, passHash, rolId, operador.getId_usuario());
        if (insertado) {
            return "SUCCESS: Usuario '" + nombre + "' (" + email + ") registrado exitosamente con Rol ID " + rolId + ".";
        } else {
            return "ERROR [500]: No se pudo registrar el usuario en la base de datos debido a un error interno.";
        }
    }

    /**
     * CU-01 / RF-15: Modificar datos de un usuario existente.
     * Comando: MODUSR [id_usuario; nombre_completo; correo_electronico; estado; rol_id]
     * Restricción: Exclusivo para el rol 'Director'.
     */
    public String modificar(List<String> params, String senderEmail) {
        // 1. Control de autorización
        if (!verificarRemitente(senderEmail, "Director")) {
            return "ERROR [403]: Acceso denegado. Solo el rol 'Director' tiene autorización para modificar usuarios.";
        }

        // 2. Validación de parámetros
        if (params == null || params.size() != 5) {
            return "ERROR [400]: Cantidad de parámetros inválida para MODUSR. Se esperan 5 parámetros: " +
                   "[id_usuario; nombre_completo; correo_electronico; estado; rol_id]";
        }

        String strId = params.get(0) != null ? params.get(0).trim() : "";
        String nombre = params.get(1) != null ? params.get(1).trim() : "";
        String email = params.get(2) != null ? params.get(2).trim().toLowerCase() : "";
        String estado = params.get(3) != null ? params.get(3).trim() : "";
        String strRolId = params.get(4) != null ? params.get(4).trim() : "";

        int id;
        try {
            id = Integer.parseInt(strId);
        } catch (NumberFormatException e) {
            return "ERROR [422]: El ID de usuario debe ser un número entero válido (recibido: '" + strId + "').";
        }

        int rolId;
        try {
            rolId = Integer.parseInt(strRolId);
        } catch (NumberFormatException e) {
            return "ERROR [422]: El ID del rol debe ser un número entero válido (recibido: '" + strRolId + "').";
        }

        if (nombre.isEmpty()) {
            return "ERROR [422]: El nombre completo no puede estar vacío.";
        }
        if (!isValidEmail(email)) {
            return "ERROR [422]: El formato del correo electrónico '" + email + "' no es válido.";
        }
        if (!estado.equalsIgnoreCase("Activo") && !estado.equalsIgnoreCase("Inactivo")) {
            return "ERROR [422]: Estado no permitido ('" + estado + "'). Solo se admite 'Activo' o 'Inactivo'.";
        }
        // Normalización capitalizada
        estado = estado.equalsIgnoreCase("Activo") ? "Activo" : "Inactivo";

        if (!dUsuario.existeRol(rolId)) {
            return "ERROR [404]: El rol especificado (ID " + rolId + ") no existe en el sistema.";
        }

        // 3. Validar existencia del usuario a modificar
        Usuario usuarioActual = dUsuario.getById(id);
        if (usuarioActual == null) {
            return "ERROR [404]: No se encontró ningún usuario con ID " + id + ".";
        }

        // 4. Validar que el correo no esté ocupado por OTRO usuario
        Usuario usuarioConMismoEmail = dUsuario.getByEmail(email);
        if (usuarioConMismoEmail != null && usuarioConMismoEmail.getId_usuario() != id) {
            return "ERROR [409]: El correo '" + email + "' ya se encuentra asignado a otro usuario (ID " +
                   usuarioConMismoEmail.getId_usuario() + ").";
        }

        // 5. Usuario operador
        Usuario operador = dUsuario.getByEmail(senderEmail);
        if (operador == null) {
            return "ERROR [401]: No se pudo identificar al usuario operador con correo '" + senderEmail + "'.";
        }

        // 6. Actualización
        boolean actualizado = dUsuario.update(id, nombre, email, estado, rolId, operador.getId_usuario());
        if (actualizado) {
            return "SUCCESS: Usuario con ID " + id + " modificado exitosamente (Nombre: '" + nombre +
                   "', Estado: '" + estado + "').";
        } else {
            return "ERROR [500]: Error interno al actualizar el usuario en la base de datos.";
        }
    }

    /**
     * CU-01 / RF-15: Dar de baja lógica a un usuario.
     * Comando: DELUSR [id_usuario]
     * Restricción: Exclusivo para el rol 'Director'.
     */
    public String eliminar(List<String> params, String senderEmail) {
        // 1. Control de autorización
        if (!verificarRemitente(senderEmail, "Director")) {
            return "ERROR [403]: Acceso denegado. Solo el rol 'Director' tiene autorización para eliminar usuarios.";
        }

        // 2. Validación de parámetros
        if (params == null || params.size() != 1) {
            return "ERROR [400]: Cantidad de parámetros inválida para DELUSR. Se espera 1 parámetro: [id_usuario]";
        }

        String strId = params.get(0) != null ? params.get(0).trim() : "";
        int id;
        try {
            id = Integer.parseInt(strId);
        } catch (NumberFormatException e) {
            return "ERROR [422]: El ID de usuario debe ser un número entero válido (recibido: '" + strId + "').";
        }

        // 3. Validar existencia del usuario
        Usuario usuarioObjetivo = dUsuario.getById(id);
        if (usuarioObjetivo == null) {
            return "ERROR [404]: No se encontró ningún usuario con ID " + id + ".";
        }

        // 4. Operador y regla de no auto-eliminación
        Usuario operador = dUsuario.getByEmail(senderEmail);
        if (operador == null) {
            return "ERROR [401]: No se pudo identificar al usuario operador con correo '" + senderEmail + "'.";
        }

        if (operador.getId_usuario() == id) {
            return "ERROR [409]: Acción rechazada: El usuario operador no puede darse de baja a sí mismo.";
        }

        // 5. Baja lógica en DUsuario
        boolean eliminado = dUsuario.delete(id, operador.getId_usuario());
        if (eliminado) {
            return "SUCCESS: Usuario con ID " + id + " ('" + usuarioObjetivo.getNombre_completo() +
                   "') dado de baja exitosamente (Estado: Inactivo).";
        } else {
            return "ERROR [500]: Error interno al procesar la baja del usuario.";
        }
    }

    /**
     * CU-01: Listar todos los usuarios del sistema junto a su rol.
     * Comando: LISUSR []
     * Requiere que el remitente sea un usuario registrado y activo con cualquier rol.
     */
    public List<String[]> listar(String senderEmail) {
        String rol = dUsuario.getRolByEmail(senderEmail);
        if (rol == null) {
            throw new SecurityException("ERROR [403]: Acceso denegado. El remitente '" + senderEmail +
                                       "' no es un usuario activo autorizado en el sistema.");
        }
        return dUsuario.list();
    }

    /**
     * Obtiene los encabezados de tabla para formateo de respuestas de lista.
     */
    public String[] getHeaders() {
        return DUsuario.HEADERS;
    }

    /**
     * Valida el formato de una dirección de correo electrónico.
     */
    public boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    /**
     * Cifra un texto plano aplicando el algoritmo criptográfico SHA-256.
     * Retorna la representación hexadecimal en minúsculas (64 caracteres).
     */
    public String calcularHashSHA256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Algoritmo de cifrado SHA-256 no disponible en la JVM.", e);
        }
    }

    public DUsuario getDUsuario() {
        return dUsuario;
    }
}

