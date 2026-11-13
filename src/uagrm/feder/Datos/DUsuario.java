package uagrm.feder.Datos;

import uagrm.feder.Entity.Usuario;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Capa de Datos: DUsuario.
 * Gestiona el acceso y persistencia en PostgreSQL para la tabla USUARIO y su auditoría.
 */
public class DUsuario {

    public static final String[] HEADERS = {
        "ID", "NOMBRE COMPLETO", "CORREO ELECTRONICO", "ESTADO", "ROL"
    };

    /**
     * Inserta un nuevo usuario y registra la operación en la tabla AUDITORIA.
     *
     * @param nombre            Nombre completo del usuario.
     * @param email             Correo electrónico único.
     * @param passHash          Hash SHA-256 de la contraseña.
     * @param rolId             Identificador del rol asignado.
     * @param usuarioOperadorId ID del usuario que ejecuta la operación.
     * @return true si la transacción se completó con éxito; false en caso contrario.
     */
    public boolean insert(String nombre, String email, String passHash,
            int rolId, int usuarioOperadorId) {
        String sqlUsuario = "INSERT INTO feder.USUARIO "
                + "(nombre_completo, correo_electronico, hash_contrasena, estado, rol_id) "
                + "VALUES (?, ?, ?, 'Activo', ?)";
        String sqlAuditoria = "INSERT INTO feder.AUDITORIA "
                + "(accion_realizada, tabla_afectada, usuario_id) VALUES (?, ?, ?)";

        try (Connection conn = SqlConnetion.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement psUsuario = conn.prepareStatement(sqlUsuario);
                 PreparedStatement psAuditoria = conn.prepareStatement(sqlAuditoria)) {

                psUsuario.setString(1, nombre);
                psUsuario.setString(2, email.trim().toLowerCase());
                psUsuario.setString(3, passHash);
                psUsuario.setInt(4, rolId);

                if (psUsuario.executeUpdate() == 0) {
                    conn.rollback();
                    return false;
                }

                psAuditoria.setString(1, "INSERT");
                psAuditoria.setString(2, "USUARIO");
                psAuditoria.setInt(3, usuarioOperadorId);
                psAuditoria.executeUpdate();

                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                System.err.println("[ERROR] DUsuario.insert: " + e.getMessage());
                return false;
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] Conexión en DUsuario.insert: " + e.getMessage());
            return false;
        }
    }

    /**
     * Actualiza los datos de un usuario existente y registra la operación en AUDITORIA.
     *
     * @param id                ID del usuario a modificar.
     * @param nombre            Nuevo nombre completo.
     * @param email             Nuevo correo electrónico.
     * @param estado            Nuevo estado ('Activo' o 'Inactivo').
     * @param rolId             Nuevo ID de rol.
     * @param usuarioOperadorId ID del usuario operador.
     * @return true si la actualización fue exitosa; false en caso contrario.
     */
    public boolean update(int id, String nombre, String email, String estado,
            int rolId, int usuarioOperadorId) {
        String sqlUsuario = "UPDATE feder.USUARIO SET nombre_completo = ?, "
                + "correo_electronico = ?, estado = ?, rol_id = ? WHERE id_usuario = ?";
        String sqlAuditoria = "INSERT INTO feder.AUDITORIA "
                + "(accion_realizada, tabla_afectada, usuario_id) VALUES (?, ?, ?)";

        try (Connection conn = SqlConnetion.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement psUsuario = conn.prepareStatement(sqlUsuario);
                 PreparedStatement psAuditoria = conn.prepareStatement(sqlAuditoria)) {

                psUsuario.setString(1, nombre);
                psUsuario.setString(2, email.trim().toLowerCase());
                psUsuario.setString(3, estado);
                psUsuario.setInt(4, rolId);
                psUsuario.setInt(5, id);

                if (psUsuario.executeUpdate() == 0) {
                    conn.rollback();
                    return false;
                }

                psAuditoria.setString(1, "UPDATE");
                psAuditoria.setString(2, "USUARIO");
                psAuditoria.setInt(3, usuarioOperadorId);
                psAuditoria.executeUpdate();

                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                System.err.println("[ERROR] DUsuario.update: " + e.getMessage());
                return false;
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] Conexión en DUsuario.update: " + e.getMessage());
            return false;
        }
    }

    /**
     * Realiza baja lógica cambiando el estado a 'Inactivo' y registra en AUDITORIA.
     *
     * @param id                ID del usuario a dar de baja.
     * @param usuarioOperadorId ID del usuario operador.
     * @return true si se dio de baja correctamente; false en caso contrario.
     */
    public boolean delete(int id, int usuarioOperadorId) {
        String sqlUsuario = "UPDATE feder.USUARIO SET estado = 'Inactivo' WHERE id_usuario = ?";
        String sqlAuditoria = "INSERT INTO feder.AUDITORIA "
                + "(accion_realizada, tabla_afectada, usuario_id) VALUES (?, ?, ?)";

        try (Connection conn = SqlConnetion.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement psUsuario = conn.prepareStatement(sqlUsuario);
                 PreparedStatement psAuditoria = conn.prepareStatement(sqlAuditoria)) {

                psUsuario.setInt(1, id);
                if (psUsuario.executeUpdate() == 0) {
                    conn.rollback();
                    return false;
                }

                psAuditoria.setString(1, "DELETE");
                psAuditoria.setString(2, "USUARIO");
                psAuditoria.setInt(3, usuarioOperadorId);
                psAuditoria.executeUpdate();

                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                System.err.println("[ERROR] DUsuario.delete: " + e.getMessage());
                return false;
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] Conexión en DUsuario.delete: " + e.getMessage());
            return false;
        }
    }

    /**
     * Retorna una lista con todos los usuarios y el nombre de su rol.
     *
     * @return Lista de arreglos String[] con las columnas de HEADERS.
     */
    public List<String[]> list() {
        List<String[]> resultado = new ArrayList<>();
        String sql = "SELECT u.id_usuario, u.nombre_completo, u.correo_electronico, "
                + "u.estado, r.nombre_rol "
                + "FROM feder.USUARIO u "
                + "INNER JOIN feder.ROL r ON u.rol_id = r.id_rol "
                + "ORDER BY u.id_usuario ASC";

        try (Connection conn = SqlConnetion.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                resultado.add(new String[]{
                    String.valueOf(rs.getInt("id_usuario")),
                    rs.getString("nombre_completo"),
                    rs.getString("correo_electronico"),
                    rs.getString("estado"),
                    rs.getString("nombre_rol")
                });
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] DUsuario.list: " + e.getMessage());
        }
        return resultado;
    }

    /**
     * Obtiene un usuario por su identificador primario.
     *
     * @param id Identificador del usuario.
     * @return Instancia de Usuario o null si no existe.
     */
    public Usuario getById(int id) {
        String sql = "SELECT id_usuario, nombre_completo, correo_electronico, "
                + "hash_contrasena, estado, fecha_registro, rol_id "
                + "FROM feder.USUARIO WHERE id_usuario = ?";

        try (Connection conn = SqlConnetion.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearUsuario(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] DUsuario.getById: " + e.getMessage());
        }
        return null;
    }

    /**
     * Obtiene un usuario por su correo electrónico.
     *
     * @param email Dirección de correo a buscar.
     * @return Instancia de Usuario o null si no existe.
     */
    public Usuario getByEmail(String email) {
        String sql = "SELECT id_usuario, nombre_completo, correo_electronico, "
                + "hash_contrasena, estado, fecha_registro, rol_id "
                + "FROM feder.USUARIO WHERE LOWER(correo_electronico) = LOWER(?)";

        try (Connection conn = SqlConnetion.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, email.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearUsuario(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] DUsuario.getByEmail: " + e.getMessage());
        }
        return null;
    }

    /**
     * Retorna el nombre del rol asignado a un usuario activo mediante su correo electrónico.
     *
     * @param email Correo electrónico del usuario.
     * @return Nombre del rol ('Director', 'Secretaria', 'Instructor') o null si no está activo.
     */
    public String getRolByEmail(String email) {
        String sql = "SELECT r.nombre_rol "
                + "FROM feder.USUARIO u "
                + "INNER JOIN feder.ROL r ON u.rol_id = r.id_rol "
                + "WHERE LOWER(u.correo_electronico) = LOWER(?) AND u.estado = 'Activo'";

        try (Connection conn = SqlConnetion.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, email.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("nombre_rol");
                }
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] DUsuario.getRolByEmail: " + e.getMessage());
        }
        return null;
    }

    /**
     * Verifica si existe un rol registrado con el ID proporcionado.
     *
     * @param rolId Identificador del rol.
     * @return true si el rol existe; false en caso contrario.
     */
    public boolean existeRol(int rolId) {
        String sql = "SELECT 1 FROM feder.ROL WHERE id_rol = ?";
        try (Connection conn = SqlConnetion.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, rolId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] DUsuario.existeRol: " + e.getMessage());
            return false;
        }
    }

    private Usuario mapearUsuario(ResultSet rs) throws SQLException {
        return new Usuario(
                rs.getInt("id_usuario"),
                rs.getString("nombre_completo"),
                rs.getString("correo_electronico"),
                rs.getString("hash_contrasena"),
                rs.getString("estado"),
                rs.getTimestamp("fecha_registro"),
                rs.getInt("rol_id")
        );
    }
}
