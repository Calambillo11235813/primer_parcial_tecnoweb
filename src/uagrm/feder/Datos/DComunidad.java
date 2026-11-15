package uagrm.feder.Datos;

import uagrm.feder.Entity.Comunidad;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Capa de Datos: DComunidad.
 * Gestiona la persistencia en PostgreSQL para la tabla COMUNIDAD (RF-05)
 * con registro transaccional en AUDITORIA.
 */
public class DComunidad {

    public static final String[] HEADERS = {
        "ID", "NOMBRE COMUNIDAD", "DESCRIPCION", "UBICACION", "ESTADO", "REGISTRADO POR"
    };

    /**
     * Registra una nueva comunidad beneficiaria con estado 'Activa' y audita la acción.
     *
     * @param nombre            Nombre de la comunidad o municipio.
     * @param descripcion       Descripción de la comunidad.
     * @param ubicacion         Ubicación geográfica.
     * @param usuarioOperadorId ID del usuario responsable.
     * @return true si la inserción fue exitosa.
     */
    public boolean insert(String nombre, String descripcion, String ubicacion,
            int usuarioOperadorId) {
        String sqlComunidad = "INSERT INTO feder.COMUNIDAD "
                + "(nombre, descripcion, ubicacion, estado, usuario_id) "
                + "VALUES (?, ?, ?, 'Activa', ?)";
        String sqlAuditoria = "INSERT INTO feder.AUDITORIA "
                + "(accion_realizada, tabla_afectada, usuario_id) VALUES (?, ?, ?)";

        try (Connection conn = SqlConnetion.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement psCom = conn.prepareStatement(sqlComunidad);
                 PreparedStatement psAuditoria = conn.prepareStatement(sqlAuditoria)) {

                psCom.setString(1, nombre.trim());
                psCom.setString(2, descripcion != null ? descripcion.trim() : "");
                psCom.setString(3, ubicacion.trim());
                psCom.setInt(4, usuarioOperadorId);

                if (psCom.executeUpdate() == 0) {
                    conn.rollback();
                    return false;
                }

                psAuditoria.setString(1, "INSERT");
                psAuditoria.setString(2, "COMUNIDAD");
                psAuditoria.setInt(3, usuarioOperadorId);
                psAuditoria.executeUpdate();

                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                System.err.println("[ERROR] DComunidad.insert: " + e.getMessage());
                return false;
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] Conexión en DComunidad.insert: " + e.getMessage());
            return false;
        }
    }

    /**
     * Actualiza los datos o el estado de una comunidad existente y audita la acción.
     *
     * @param id                ID de la comunidad a modificar.
     * @param nombre            Nuevo nombre.
     * @param descripcion       Nueva descripción.
     * @param ubicacion         Nueva ubicación geográfica.
     * @param estado            Estado ('Activa' o 'Inactiva').
     * @param usuarioOperadorId ID del usuario operador.
     * @return true si la actualización fue exitosa.
     */
    public boolean update(int id, String nombre, String descripcion, String ubicacion,
            String estado, int usuarioOperadorId) {
        String sqlComunidad = "UPDATE feder.COMUNIDAD SET nombre = ?, descripcion = ?, "
                + "ubicacion = ?, estado = ?, usuario_id = ? WHERE id_comunidad = ?";
        String sqlAuditoria = "INSERT INTO feder.AUDITORIA "
                + "(accion_realizada, tabla_afectada, usuario_id) VALUES (?, ?, ?)";

        try (Connection conn = SqlConnetion.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement psCom = conn.prepareStatement(sqlComunidad);
                 PreparedStatement psAuditoria = conn.prepareStatement(sqlAuditoria)) {

                psCom.setString(1, nombre.trim());
                psCom.setString(2, descripcion != null ? descripcion.trim() : "");
                psCom.setString(3, ubicacion.trim());
                psCom.setString(4, estado.trim());
                psCom.setInt(5, usuarioOperadorId);
                psCom.setInt(6, id);

                if (psCom.executeUpdate() == 0) {
                    conn.rollback();
                    return false;
                }

                psAuditoria.setString(1, "UPDATE");
                psAuditoria.setString(2, "COMUNIDAD");
                psAuditoria.setInt(3, usuarioOperadorId);
                psAuditoria.executeUpdate();

                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                System.err.println("[ERROR] DComunidad.update: " + e.getMessage());
                return false;
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] Conexión en DComunidad.update: " + e.getMessage());
            return false;
        }
    }

    /**
     * Realiza la baja lógica de una comunidad cambiando su estado a 'Inactiva'.
     *
     * @param id                ID de la comunidad a desactivar.
     * @param usuarioOperadorId ID del usuario responsable.
     * @return true si la baja lógica se completó correctamente.
     */
    public boolean delete(int id, int usuarioOperadorId) {
        String sqlComunidad = "UPDATE feder.COMUNIDAD SET estado = 'Inactiva' "
                + "WHERE id_comunidad = ?";
        String sqlAuditoria = "INSERT INTO feder.AUDITORIA "
                + "(accion_realizada, tabla_afectada, usuario_id) VALUES (?, ?, ?)";

        try (Connection conn = SqlConnetion.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement psCom = conn.prepareStatement(sqlComunidad);
                 PreparedStatement psAuditoria = conn.prepareStatement(sqlAuditoria)) {

                psCom.setInt(1, id);
                if (psCom.executeUpdate() == 0) {
                    conn.rollback();
                    return false;
                }

                psAuditoria.setString(1, "DELETE");
                psAuditoria.setString(2, "COMUNIDAD");
                psAuditoria.setInt(3, usuarioOperadorId);
                psAuditoria.executeUpdate();

                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                System.err.println("[ERROR] DComunidad.delete: " + e.getMessage());
                return false;
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] Conexión en DComunidad.delete: " + e.getMessage());
            return false;
        }
    }

    /**
     * Lista todas las comunidades beneficiarias registradas.
     *
     * @return Lista de filas String[] alineadas con HEADERS.
     */
    public List<String[]> list() {
        List<String[]> resultado = new ArrayList<>();
        String sql = "SELECT c.id_comunidad, c.nombre, COALESCE(c.descripcion, ''), "
                + "c.ubicacion, c.estado, u.nombre_completo "
                + "FROM feder.COMUNIDAD c "
                + "INNER JOIN feder.USUARIO u ON c.usuario_id = u.id_usuario "
                + "ORDER BY c.id_comunidad ASC";

        try (Connection conn = SqlConnetion.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                resultado.add(new String[]{
                    String.valueOf(rs.getInt(1)),
                    rs.getString(2),
                    rs.getString(3),
                    rs.getString(4),
                    rs.getString(5),
                    rs.getString(6)
                });
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] DComunidad.list: " + e.getMessage());
        }
        return resultado;
    }

    /**
     * Obtiene una comunidad por su identificador primario.
     *
     * @param id Identificador de la comunidad.
     * @return Instancia de Comunidad o null si no existe.
     */
    public Comunidad getById(int id) {
        String sql = "SELECT id_comunidad, nombre, descripcion, ubicacion, estado, usuario_id "
                + "FROM feder.COMUNIDAD WHERE id_comunidad = ?";

        try (Connection conn = SqlConnetion.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Comunidad(
                            rs.getInt("id_comunidad"),
                            rs.getString("nombre"),
                            rs.getString("descripcion"),
                            rs.getString("ubicacion"),
                            rs.getString("estado"),
                            rs.getInt("usuario_id")
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] DComunidad.getById: " + e.getMessage());
        }
        return null;
    }

    /**
     * Busca una comunidad por su nombre exacto (sin distinción de mayúsculas/minúsculas).
     *
     * @param nombre Nombre de la comunidad.
     * @return Instancia de Comunidad o null si no existe.
     */
    public Comunidad getByNombre(String nombre) {
        String sql = "SELECT id_comunidad, nombre, descripcion, ubicacion, estado, usuario_id "
                + "FROM feder.COMUNIDAD WHERE LOWER(nombre) = LOWER(?)";

        try (Connection conn = SqlConnetion.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, nombre.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Comunidad(
                            rs.getInt("id_comunidad"),
                            rs.getString("nombre"),
                            rs.getString("descripcion"),
                            rs.getString("ubicacion"),
                            rs.getString("estado"),
                            rs.getInt("usuario_id")
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] DComunidad.getByNombre: " + e.getMessage());
        }
        return null;
    }
}
