package uagrm.feder.Datos;

import uagrm.feder.Entity.Consulta;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Capa de Datos: DConsulta.
 * Gestiona la persistencia en PostgreSQL para la tabla CONSULTA (RF-16)
 * con registro transaccional en AUDITORIA para operaciones administrativas.
 */
public class DConsulta {

    public static final String[] HEADERS = {
        "ID", "REMITENTE", "CORREO", "ASUNTO", "MENSAJE",
        "FECHA ENVIO", "ESTADO", "ATENDIDO POR"
    };

    /**
     * Registra una nueva consulta ciudadana en la bandeja interna con estado 'Pendiente'.
     *
     * @param nombreRemitente Nombre completo del ciudadano o visitante.
     * @param correoRemitente Correo electrónico de contacto del remitente.
     * @param asunto          Asunto o tema de la consulta.
     * @param mensaje         Cuerpo del mensaje de consulta.
     * @return true si el registro fue exitoso.
     */
    public boolean insert(String nombreRemitente, String correoRemitente,
            String asunto, String mensaje) {
        String sql = "INSERT INTO feder.CONSULTA "
                + "(nombre_remitente, correo_remitente, asunto, mensaje, "
                + "fecha_envio, estado, usuario_id) "
                + "VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, 'Pendiente', NULL)";

        try (Connection conn = SqlConnetion.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, nombreRemitente.trim());
            ps.setString(2, correoRemitente.trim().toLowerCase());
            ps.setString(3, asunto.trim());
            ps.setString(4, mensaje.trim());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[ERROR] DConsulta.insert: " + e.getMessage());
            return false;
        }
    }

    /**
     * Actualiza el estado de una consulta ('Pendiente', 'En Proceso', 'Atendido'),
     * asigna el funcionario responsable y registra la acción en AUDITORIA.
     *
     * @param id                ID de la consulta.
     * @param estado            Nuevo estado de clasificación.
     * @param usuarioOperadorId ID del usuario (Secretaria/Director) que atiende.
     * @return true si la transacción fue exitosa.
     */
    public boolean updateEstado(int id, String estado, int usuarioOperadorId) {
        String sqlConsulta = "UPDATE feder.CONSULTA SET estado = ?, usuario_id = ? "
                + "WHERE id_consulta = ?";
        String sqlAuditoria = "INSERT INTO feder.AUDITORIA "
                + "(accion_realizada, tabla_afectada, usuario_id) VALUES (?, ?, ?)";

        try (Connection conn = SqlConnetion.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement psCon = conn.prepareStatement(sqlConsulta);
                 PreparedStatement psAuditoria = conn.prepareStatement(sqlAuditoria)) {

                psCon.setString(1, estado.trim());
                psCon.setInt(2, usuarioOperadorId);
                psCon.setInt(3, id);

                if (psCon.executeUpdate() == 0) {
                    conn.rollback();
                    return false;
                }

                psAuditoria.setString(1, "UPDATE");
                psAuditoria.setString(2, "CONSULTA");
                psAuditoria.setInt(3, usuarioOperadorId);
                psAuditoria.executeUpdate();

                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                System.err.println("[ERROR] DConsulta.updateEstado: " + e.getMessage());
                return false;
            }
        } catch (SQLException e) {
            System.err.println(
                    "[ERROR] Conexión en DConsulta.updateEstado: " + e.getMessage()
            );
            return false;
        }
    }

    /**
     * Elimina una consulta de la bandeja y registra la operación en AUDITORIA.
     *
     * @param id                ID de la consulta a eliminar.
     * @param usuarioOperadorId ID del usuario responsable.
     * @return true si se eliminó correctamente.
     */
    public boolean delete(int id, int usuarioOperadorId) {
        String sqlConsulta = "DELETE FROM feder.CONSULTA WHERE id_consulta = ?";
        String sqlAuditoria = "INSERT INTO feder.AUDITORIA "
                + "(accion_realizada, tabla_afectada, usuario_id) VALUES (?, ?, ?)";

        try (Connection conn = SqlConnetion.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement psCon = conn.prepareStatement(sqlConsulta);
                 PreparedStatement psAuditoria = conn.prepareStatement(sqlAuditoria)) {

                psCon.setInt(1, id);
                if (psCon.executeUpdate() == 0) {
                    conn.rollback();
                    return false;
                }

                psAuditoria.setString(1, "DELETE");
                psAuditoria.setString(2, "CONSULTA");
                psAuditoria.setInt(3, usuarioOperadorId);
                psAuditoria.executeUpdate();

                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                System.err.println("[ERROR] DConsulta.delete: " + e.getMessage());
                return false;
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] Conexión en DConsulta.delete: " + e.getMessage());
            return false;
        }
    }

    /**
     * Lista las consultas de la bandeja interna, opcionalmente filtradas por estado.
     *
     * @param filtroEstado Estado opcional ('Pendiente', 'En Proceso', 'Atendido') o null/vacío.
     * @return Lista de filas String[] alineadas con HEADERS.
     */
    public List<String[]> list(String filtroEstado) {
        List<String[]> resultado = new ArrayList<>();
        boolean tieneFiltro = filtroEstado != null && !filtroEstado.trim().isEmpty();

        String sql = "SELECT c.id_consulta, c.nombre_remitente, c.correo_remitente, "
                + "c.asunto, c.mensaje, "
                + "TO_CHAR(c.fecha_envio, 'YYYY-MM-DD HH24:MI'), c.estado, "
                + "COALESCE(u.nombre_completo, 'Sin asignar') "
                + "FROM feder.CONSULTA c "
                + "LEFT JOIN feder.USUARIO u ON c.usuario_id = u.id_usuario "
                + (tieneFiltro ? "WHERE LOWER(c.estado) = LOWER(?) " : "")
                + "ORDER BY c.id_consulta ASC";

        try (Connection conn = SqlConnetion.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            if (tieneFiltro) {
                ps.setString(1, filtroEstado.trim());
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    resultado.add(new String[]{
                        String.valueOf(rs.getInt(1)),
                        rs.getString(2),
                        rs.getString(3),
                        rs.getString(4),
                        rs.getString(5),
                        rs.getString(6),
                        rs.getString(7),
                        rs.getString(8)
                    });
                }
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] DConsulta.list: " + e.getMessage());
        }
        return resultado;
    }

    /**
     * Obtiene una consulta por su identificador primario.
     *
     * @param id Identificador de la consulta.
     * @return Instancia de Consulta o null si no existe.
     */
    public Consulta getById(int id) {
        String sql = "SELECT id_consulta, nombre_remitente, correo_remitente, "
                + "asunto, mensaje, fecha_envio, estado, usuario_id "
                + "FROM feder.CONSULTA WHERE id_consulta = ?";

        try (Connection conn = SqlConnetion.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int usrId = rs.getInt("usuario_id");
                    Integer usuarioIdObj = rs.wasNull() ? null : usrId;
                    return new Consulta(
                            rs.getInt("id_consulta"),
                            rs.getString("nombre_remitente"),
                            rs.getString("correo_remitente"),
                            rs.getString("asunto"),
                            rs.getString("mensaje"),
                            rs.getTimestamp("fecha_envio"),
                            rs.getString("estado"),
                            usuarioIdObj
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] DConsulta.getById: " + e.getMessage());
        }
        return null;
    }
}
