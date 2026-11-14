package uagrm.feder.Datos;

import uagrm.feder.Entity.RedSocial;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Capa de Datos: DRedSocial.
 * Gestiona la persistencia en PostgreSQL para la tabla RED_SOCIAL (RF-04)
 * con registro transaccional en AUDITORIA.
 */
public class DRedSocial {

    public static final String[] HEADERS = {
        "ID", "PLATAFORMA", "URL ENLACE", "ESTADO", "REGISTRADO POR"
    };

    /**
     * Inserta un nuevo enlace de red social y registra la acción en AUDITORIA.
     *
     * @param plataforma        Nombre de la red social (ej. Facebook, Instagram, TikTok, WhatsApp).
     * @param urlEnlace         Enlace oficial (URL).
     * @param usuarioOperadorId ID del usuario responsable.
     * @return true si se registró correctamente.
     */
    public boolean insert(String plataforma, String urlEnlace, int usuarioOperadorId) {
        String sqlRed = "INSERT INTO feder.RED_SOCIAL "
                + "(plataforma, url_enlace, estado, usuario_id) VALUES (?, ?, 'Activo', ?)";
        String sqlAuditoria = "INSERT INTO feder.AUDITORIA "
                + "(accion_realizada, tabla_afectada, usuario_id) VALUES (?, ?, ?)";

        try (Connection conn = SqlConnetion.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement psRed = conn.prepareStatement(sqlRed);
                 PreparedStatement psAuditoria = conn.prepareStatement(sqlAuditoria)) {

                psRed.setString(1, plataforma.trim());
                psRed.setString(2, urlEnlace.trim());
                psRed.setInt(3, usuarioOperadorId);

                if (psRed.executeUpdate() == 0) {
                    conn.rollback();
                    return false;
                }

                psAuditoria.setString(1, "INSERT");
                psAuditoria.setString(2, "RED_SOCIAL");
                psAuditoria.setInt(3, usuarioOperadorId);
                psAuditoria.executeUpdate();

                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                System.err.println("[ERROR] DRedSocial.insert: " + e.getMessage());
                return false;
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] Conexión en DRedSocial.insert: " + e.getMessage());
            return false;
        }
    }

    /**
     * Actualiza una red social existente y registra la operación en AUDITORIA.
     *
     * @param id                ID de la red social.
     * @param plataforma        Nombre de la plataforma.
     * @param urlEnlace         Nueva URL de enlace.
     * @param estado            Estado ('Activo' o 'Inactivo').
     * @param usuarioOperadorId ID del usuario responsable.
     * @return true si la actualización fue exitosa.
     */
    public boolean update(int id, String plataforma, String urlEnlace, String estado,
            int usuarioOperadorId) {
        String sqlRed = "UPDATE feder.RED_SOCIAL "
                + "SET plataforma = ?, url_enlace = ?, estado = ?, usuario_id = ? "
                + "WHERE id_red_social = ?";
        String sqlAuditoria = "INSERT INTO feder.AUDITORIA "
                + "(accion_realizada, tabla_afectada, usuario_id) VALUES (?, ?, ?)";

        try (Connection conn = SqlConnetion.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement psRed = conn.prepareStatement(sqlRed);
                 PreparedStatement psAuditoria = conn.prepareStatement(sqlAuditoria)) {

                psRed.setString(1, plataforma.trim());
                psRed.setString(2, urlEnlace.trim());
                psRed.setString(3, estado.trim());
                psRed.setInt(4, usuarioOperadorId);
                psRed.setInt(5, id);

                if (psRed.executeUpdate() == 0) {
                    conn.rollback();
                    return false;
                }

                psAuditoria.setString(1, "UPDATE");
                psAuditoria.setString(2, "RED_SOCIAL");
                psAuditoria.setInt(3, usuarioOperadorId);
                psAuditoria.executeUpdate();

                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                System.err.println("[ERROR] DRedSocial.update: " + e.getMessage());
                return false;
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] Conexión en DRedSocial.update: " + e.getMessage());
            return false;
        }
    }

    /**
     * Realiza la baja lógica (estado = 'Inactivo') de una red social y audita la operación.
     *
     * @param id                ID de la red social.
     * @param usuarioOperadorId ID del usuario responsable.
     * @return true si se dio de baja correctamente.
     */
    public boolean delete(int id, int usuarioOperadorId) {
        String sqlRed = "UPDATE feder.RED_SOCIAL SET estado = 'Inactivo' WHERE id_red_social = ?";
        String sqlAuditoria = "INSERT INTO feder.AUDITORIA "
                + "(accion_realizada, tabla_afectada, usuario_id) VALUES (?, ?, ?)";

        try (Connection conn = SqlConnetion.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement psRed = conn.prepareStatement(sqlRed);
                 PreparedStatement psAuditoria = conn.prepareStatement(sqlAuditoria)) {

                psRed.setInt(1, id);
                if (psRed.executeUpdate() == 0) {
                    conn.rollback();
                    return false;
                }

                psAuditoria.setString(1, "DELETE");
                psAuditoria.setString(2, "RED_SOCIAL");
                psAuditoria.setInt(3, usuarioOperadorId);
                psAuditoria.executeUpdate();

                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                System.err.println("[ERROR] DRedSocial.delete: " + e.getMessage());
                return false;
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] Conexión en DRedSocial.delete: " + e.getMessage());
            return false;
        }
    }

    /**
     * Lista todas las redes sociales registradas junto con el usuario responsable.
     *
     * @return Lista de filas String[] alineadas con HEADERS.
     */
    public List<String[]> list() {
        List<String[]> resultado = new ArrayList<>();
        String sql = "SELECT r.id_red_social, r.plataforma, r.url_enlace, r.estado, "
                + "u.nombre_completo "
                + "FROM feder.RED_SOCIAL r "
                + "INNER JOIN feder.USUARIO u ON r.usuario_id = u.id_usuario "
                + "ORDER BY r.id_red_social ASC";

        try (Connection conn = SqlConnetion.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                resultado.add(new String[]{
                    String.valueOf(rs.getInt(1)),
                    rs.getString(2),
                    rs.getString(3),
                    rs.getString(4),
                    rs.getString(5)
                });
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] DRedSocial.list: " + e.getMessage());
        }
        return resultado;
    }

    /**
     * Obtiene una red social por su identificador primario.
     *
     * @param id Identificador de la red social.
     * @return Instancia de RedSocial o null si no existe.
     */
    public RedSocial getById(int id) {
        String sql = "SELECT id_red_social, plataforma, url_enlace, estado, usuario_id "
                + "FROM feder.RED_SOCIAL WHERE id_red_social = ?";

        try (Connection conn = SqlConnetion.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new RedSocial(
                            rs.getInt("id_red_social"),
                            rs.getString("plataforma"),
                            rs.getString("url_enlace"),
                            rs.getString("estado"),
                            rs.getInt("usuario_id")
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] DRedSocial.getById: " + e.getMessage());
        }
        return null;
    }
}
