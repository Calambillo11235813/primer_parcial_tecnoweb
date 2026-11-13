package uagrm.feder.Negocio;

import uagrm.feder.Datos.DUsuario;
import uagrm.feder.Entity.Usuario;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Capa de Negocio: NUsuario.
 * Controla la lógica del CU-01 (Gestionar Usuarios y Roles), aplicando RBAC,
 * validaciones de integridad y cifrado SHA-256.
 */
public class NUsuario {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    private final DUsuario dUsuario;

    public NUsuario() {
        this.dUsuario = new DUsuario();
    }

    public NUsuario(DUsuario dUsuario) {
        this.dUsuario = dUsuario;
    }

    /**
     * Valida si el remitente posee el rol requerido para ejecutar la operación.
     *
     * @param senderEmail  Correo electrónico de quien envía el comando.
     * @param rolRequerido Nombre del rol requerido (ej. 'Director').
     * @return true si el usuario está activo y posee el rol requerido.
     */
    public boolean verificarRemitente(String senderEmail, String rolRequerido) {
        if (senderEmail == null || senderEmail.trim().isEmpty()) {
            return false;
        }
        String rolActual = dUsuario.getRolByEmail(senderEmail.trim());
        return rolActual != null && rolActual.equalsIgnoreCase(rolRequerido);
    }

    /**
     * CU-01 / RF-14, RF-15: Registra un nuevo usuario en el sistema.
     * Comando: INSUSR [nombre_completo; correo_electronico; contrasena; rol_id]
     *
     * @param params      Lista con 4 parámetros: nombre, correo, contraseña y rol_id.
     * @param senderEmail Correo del remitente (debe tener rol 'Director').
     * @return Mensaje con el resultado de la operación.
     */
    public String registrar(List<String> params, String senderEmail) {
        if (!verificarRemitente(senderEmail, "Director")) {
            return "ERROR [403]: Acceso denegado. Solo el rol 'Director' "
                    + "tiene autorización para registrar usuarios.";
        }

        if (params == null || params.size() != 4) {
            return "ERROR [400]: Cantidad de parámetros inválida para INSUSR. "
                    + "Se esperan 4: [nombre_completo; correo_electronico; contrasena; rol_id]";
        }

        String nombre = params.get(0) != null ? params.get(0).trim() : "";
        String email = params.get(1) != null ? params.get(1).trim().toLowerCase() : "";
        String passPlano = params.get(2) != null ? params.get(2).trim() : "";
        String strRolId = params.get(3) != null ? params.get(3).trim() : "";

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
            return "ERROR [422]: El ID del rol debe ser un número entero válido "
                    + "(recibido: '" + strRolId + "').";
        }

        if (!dUsuario.existeRol(rolId)) {
            return "ERROR [404]: El rol especificado (ID " + rolId + ") no existe en el sistema.";
        }

        if (dUsuario.getByEmail(email) != null) {
            return "ERROR [409]: Ya existe un usuario registrado con el correo '" + email + "'.";
        }

        Usuario operador = dUsuario.getByEmail(senderEmail);
        if (operador == null) {
            return "ERROR [401]: No se pudo identificar al operador '" + senderEmail + "'.";
        }

        String passHash = calcularHashSHA256(passPlano);
        boolean insertado = dUsuario.insert(
                nombre, email, passHash, rolId, operador.getId_usuario()
        );

        if (insertado) {
            return "SUCCESS: Usuario '" + nombre + "' (" + email
                    + ") registrado exitosamente con Rol ID " + rolId + ".";
        }
        return "ERROR [500]: No se pudo registrar el usuario en la base de datos.";
    }

    /**
     * CU-01 / RF-15: Modifica los datos de un usuario existente.
     * Comando: MODUSR [id_usuario; nombre_completo; correo_electronico; estado; rol_id]
     *
     * @param params      Lista con 5 parámetros: id, nombre, correo, estado y rol_id.
     * @param senderEmail Correo del remitente (debe tener rol 'Director').
     * @return Mensaje con el resultado de la operación.
     */
    public String modificar(List<String> params, String senderEmail) {
        if (!verificarRemitente(senderEmail, "Director")) {
            return "ERROR [403]: Acceso denegado. Solo el rol 'Director' "
                    + "tiene autorización para modificar usuarios.";
        }

        if (params == null || params.size() != 5) {
            return "ERROR [400]: Cantidad de parámetros inválida para MODUSR. Se esperan 5: "
                    + "[id_usuario; nombre_completo; correo_electronico; estado; rol_id]";
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
            return "ERROR [422]: El ID de usuario debe ser un entero válido "
                    + "(recibido: '" + strId + "').";
        }

        int rolId;
        try {
            rolId = Integer.parseInt(strRolId);
        } catch (NumberFormatException e) {
            return "ERROR [422]: El ID del rol debe ser un entero válido "
                    + "(recibido: '" + strRolId + "').";
        }

        if (nombre.isEmpty()) {
            return "ERROR [422]: El nombre completo no puede estar vacío.";
        }
        if (!isValidEmail(email)) {
            return "ERROR [422]: El formato del correo electrónico '" + email + "' no es válido.";
        }
        if (!estado.equalsIgnoreCase("Activo") && !estado.equalsIgnoreCase("Inactivo")) {
            return "ERROR [422]: Estado no permitido ('" + estado
                    + "'). Solo se admite 'Activo' o 'Inactivo'.";
        }
        estado = estado.equalsIgnoreCase("Activo") ? "Activo" : "Inactivo";

        if (!dUsuario.existeRol(rolId)) {
            return "ERROR [404]: El rol especificado (ID " + rolId + ") no existe en el sistema.";
        }

        if (dUsuario.getById(id) == null) {
            return "ERROR [404]: No se encontró ningún usuario con ID " + id + ".";
        }

        Usuario usuarioConMismoEmail = dUsuario.getByEmail(email);
        if (usuarioConMismoEmail != null && usuarioConMismoEmail.getId_usuario() != id) {
            return "ERROR [409]: El correo '" + email + "' ya pertenece a otro usuario (ID "
                    + usuarioConMismoEmail.getId_usuario() + ").";
        }

        Usuario operador = dUsuario.getByEmail(senderEmail);
        if (operador == null) {
            return "ERROR [401]: No se pudo identificar al operador '" + senderEmail + "'.";
        }

        boolean actualizado = dUsuario.update(
                id, nombre, email, estado, rolId, operador.getId_usuario()
        );
        if (actualizado) {
            return "SUCCESS: Usuario con ID " + id + " modificado exitosamente "
                    + "(Nombre: '" + nombre + "', Estado: '" + estado + "').";
        }
        return "ERROR [500]: Error interno al actualizar el usuario en la base de datos.";
    }

    /**
     * CU-01 / RF-15: Realiza la baja lógica de un usuario.
     * Comando: DELUSR [id_usuario]
     *
     * @param params      Lista con 1 parámetro: id_usuario.
     * @param senderEmail Correo del remitente (debe tener rol 'Director').
     * @return Mensaje con el resultado de la operación.
     */
    public String eliminar(List<String> params, String senderEmail) {
        if (!verificarRemitente(senderEmail, "Director")) {
            return "ERROR [403]: Acceso denegado. Solo el rol 'Director' "
                    + "tiene autorización para eliminar usuarios.";
        }

        if (params == null || params.size() != 1) {
            return "ERROR [400]: Cantidad de parámetros inválida para DELUSR. "
                    + "Se espera 1 parámetro: [id_usuario]";
        }

        String strId = params.get(0) != null ? params.get(0).trim() : "";
        int id;
        try {
            id = Integer.parseInt(strId);
        } catch (NumberFormatException e) {
            return "ERROR [422]: El ID de usuario debe ser un entero válido "
                    + "(recibido: '" + strId + "').";
        }

        Usuario usuarioObjetivo = dUsuario.getById(id);
        if (usuarioObjetivo == null) {
            return "ERROR [404]: No se encontró ningún usuario con ID " + id + ".";
        }

        Usuario operador = dUsuario.getByEmail(senderEmail);
        if (operador == null) {
            return "ERROR [401]: No se pudo identificar al operador '" + senderEmail + "'.";
        }

        if (operador.getId_usuario() == id) {
            return "ERROR [409]: El usuario operador no puede darse de baja a sí mismo.";
        }

        boolean eliminado = dUsuario.delete(id, operador.getId_usuario());
        if (eliminado) {
            return "SUCCESS: Usuario con ID " + id + " ('"
                    + usuarioObjetivo.getNombre_completo()
                    + "') dado de baja exitosamente (Estado: Inactivo).";
        }
        return "ERROR [500]: Error interno al procesar la baja del usuario.";
    }

    /**
     * CU-01: Lista todos los usuarios registrados junto a su rol.
     * Comando: LISUSR []
     *
     * @param senderEmail Correo del usuario solicitante.
     * @return Lista de filas String[] con los datos de los usuarios.
     * @throws SecurityException Si el remitente no es un usuario activo.
     */
    public List<String[]> listar(String senderEmail) {
        String rol = dUsuario.getRolByEmail(senderEmail);
        if (rol == null) {
            throw new SecurityException(
                    "ERROR [403]: Acceso denegado. El remitente '" + senderEmail
                    + "' no es un usuario activo autorizado en el sistema."
            );
        }
        return dUsuario.list();
    }

    /**
     * Obtiene los encabezados de tabla para el listado de usuarios.
     *
     * @return Arreglo de encabezados de columna.
     */
    public String[] getHeaders() {
        return DUsuario.HEADERS;
    }

    /**
     * Valida el formato de una dirección de correo electrónico.
     *
     * @param email Dirección de correo a verificar.
     * @return true si cumple el formato estándar; false en caso contrario.
     */
    public boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    /**
     * Cifra un texto plano aplicando el algoritmo criptográfico SHA-256.
     *
     * @param input Cadena en texto claro.
     * @return Resumen hexadecimal en minúsculas de 64 caracteres.
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
            throw new RuntimeException("Algoritmo SHA-256 no disponible en la JVM.", e);
        }
    }

    public DUsuario getDUsuario() {
        return dUsuario;
    }
}
