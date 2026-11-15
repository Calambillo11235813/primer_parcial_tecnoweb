package uagrm.feder.Datos;

import uagrm.feder.Entity.Campana;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Capa de Datos: DCampana.
 * Gestiona la persistencia en PostgreSQL para la tabla CAMPANA (RF-09)
 * y el cálculo de avance financiero sobre aportes confirmados (RF-10),
 * registrando cada acción administrativa en AUDITORIA.
 */
public class DCampana {

    public static final String[] HEADERS = {
        "ID", "TITULO CAMPANA", "COMUNIDAD", "META (BS)", "RECAUDADO CONFIRMADO (BS)",
        "AVANCE (%)", "FECHA INICIO", "FECHA LIMITE", "ESTADO"
    };

    public static final String[] HEADERS_SEGUIMIENTO = {
        "ID", "CAMPANA", "COMUNIDAD", "META (BS)", "CONFIRMADO (BS)",
        "FALTANTE (BS)", "AVANCE (%)", "APORTES CONF.", "APORTES PEND.",
        "PLAZO LIMITE", "ESTADO"
    };

    /**
     * Registra una nueva campaña de recaudación con estado 'Activa' y audita la acción.
     *
     * @param titulo            Título de la campaña.
     * @param descripcion       Descripción de la campaña.
     * @param metaEconomica     Meta económica en Bolivianos (> 0).
     * @param fechaInicio       Fecha de inicio de la campaña.
     * @param fechaLimite       Fecha límite (>= fechaInicio).
     * @param comunidadId       ID de la comunidad beneficiaria.
     * @param usuarioOperadorId ID del usuario Director responsable.
     * @return true si la transacción se completó correctamente.
     */
    public boolean insert(String titulo, String descripcion, BigDecimal metaEconomica,
            Date fechaInicio, Date fechaLimite, int comunidadId, int usuarioOperadorId) {
        String sqlCampana = "INSERT INTO feder.CAMPANA "
                + "(titulo, descripcion, meta_economica, monto_recaudado, "
                + "fecha_inicio, fecha_limite, estado, comunidad_id) "
                + "VALUES (?, ?, ?, 0.00, ?, ?, 'Activa', ?)";
        String sqlAuditoria = "INSERT INTO feder.AUDITORIA "
                + "(accion_realizada, tabla_afectada, usuario_id) VALUES (?, ?, ?)";

        try (Connection conn = SqlConnetion.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement psCmp = conn.prepareStatement(sqlCampana);
                 PreparedStatement psAuditoria = conn.prepareStatement(sqlAuditoria)) {

                psCmp.setString(1, titulo.trim());
                psCmp.setString(2, descripcion.trim());
                psCmp.setBigDecimal(3, metaEconomica);
                psCmp.setDate(4, fechaInicio);
                psCmp.setDate(5, fechaLimite);
                psCmp.setInt(6, comunidadId);

                if (psCmp.executeUpdate() == 0) {
                    conn.rollback();
                    return false;
                }

                psAuditoria.setString(1, "INSERT");
                psAuditoria.setString(2, "CAMPANA");
                psAuditoria.setInt(3, usuarioOperadorId);
                psAuditoria.executeUpdate();

                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                System.err.println("[ERROR] DCampana.insert: " + e.getMessage());
                return false;
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] Conexión en DCampana.insert: " + e.getMessage());
            return false;
        }
    }

    /**
     * Actualiza los datos, plazos o estado de una campaña existente y audita la acción.
     *
     * @param id                ID de la campaña a modificar.
     * @param titulo            Nuevo título.
     * @param descripcion       Nueva descripción.
     * @param metaEconomica     Nueva meta económica (> 0).
     * @param fechaLimite       Nueva fecha límite (>= fecha_inicio).
     * @param estado            Estado ('Activa', 'Cerrada', 'Finalizada', 'Cancelada').
     * @param comunidadId       ID de la comunidad beneficiaria.
     * @param usuarioOperadorId ID del usuario Director responsable.
     * @return true si la actualización fue exitosa.
     */
    public boolean update(int id, String titulo, String descripcion,
            BigDecimal metaEconomica, Date fechaLimite, String estado,
            int comunidadId, int usuarioOperadorId) {
        String sqlCampana = "UPDATE feder.CAMPANA SET titulo = ?, descripcion = ?, "
                + "meta_economica = ?, fecha_limite = ?, estado = ?, comunidad_id = ? "
                + "WHERE id_campana = ?";
        String sqlAuditoria = "INSERT INTO feder.AUDITORIA "
                + "(accion_realizada, tabla_afectada, usuario_id) VALUES (?, ?, ?)";

        try (Connection conn = SqlConnetion.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement psCmp = conn.prepareStatement(sqlCampana);
                 PreparedStatement psAuditoria = conn.prepareStatement(sqlAuditoria)) {

                psCmp.setString(1, titulo.trim());
                psCmp.setString(2, descripcion.trim());
                psCmp.setBigDecimal(3, metaEconomica);
                psCmp.setDate(4, fechaLimite);
                psCmp.setString(5, estado.trim());
                psCmp.setInt(6, comunidadId);
                psCmp.setInt(7, id);

                if (psCmp.executeUpdate() == 0) {
                    conn.rollback();
                    return false;
                }

                psAuditoria.setString(1, "UPDATE");
                psAuditoria.setString(2, "CAMPANA");
                psAuditoria.setInt(3, usuarioOperadorId);
                psAuditoria.executeUpdate();

                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                System.err.println("[ERROR] DCampana.update: " + e.getMessage());
                return false;
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] Conexión en DCampana.update: " + e.getMessage());
            return false;
        }
    }

    /**
     * Cierra formalmente una campaña cambiando su estado a 'Cerrada' y audita la operación.
     *
     * @param id                ID de la campaña a cerrar.
     * @param usuarioOperadorId ID del usuario Director responsable.
     * @return true si se cerró correctamente.
     */
    public boolean close(int id, int usuarioOperadorId) {
        String sqlCampana = "UPDATE feder.CAMPANA SET estado = 'Cerrada' "
                + "WHERE id_campana = ?";
        String sqlAuditoria = "INSERT INTO feder.AUDITORIA "
                + "(accion_realizada, tabla_afectada, usuario_id) VALUES (?, ?, ?)";

        try (Connection conn = SqlConnetion.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement psCmp = conn.prepareStatement(sqlCampana);
                 PreparedStatement psAuditoria = conn.prepareStatement(sqlAuditoria)) {

                psCmp.setInt(1, id);
                if (psCmp.executeUpdate() == 0) {
                    conn.rollback();
                    return false;
                }

                psAuditoria.setString(1, "CLOSE");
                psAuditoria.setString(2, "CAMPANA");
                psAuditoria.setInt(3, usuarioOperadorId);
                psAuditoria.executeUpdate();

                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                System.err.println("[ERROR] DCampana.close: " + e.getMessage());
                return false;
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] Conexión en DCampana.close: " + e.getMessage());
            return false;
        }
    }

    /**
     * Lista todas las campañas calculando su monto recaudado y porcentaje de avance
     * exclusivamente sobre los aportes confirmados (RF-10).
     *
     * @return Lista de filas String[] alineadas con HEADERS.
     */
    public List<String[]> list() {
        List<String[]> resultado = new ArrayList<>();
        String sql = "SELECT c.id_campana, c.titulo, com.nombre, "
                + "TO_CHAR(c.meta_economica, 'FM999999990.00'), "
                + "TO_CHAR(COALESCE(SUM(CASE WHEN LOWER(a.estado) = 'confirmado' "
                + "   THEN a.monto ELSE 0 END), 0.00), 'FM999999990.00') AS confirmado, "
                + "TO_CHAR(ROUND((COALESCE(SUM(CASE WHEN LOWER(a.estado) = 'confirmado' "
                + "   THEN a.monto ELSE 0 END), 0.00) / c.meta_economica) * 100, 2), "
                + "   'FM999990.00') || '%' AS avance, "
                + "TO_CHAR(c.fecha_inicio, 'YYYY-MM-DD'), "
                + "TO_CHAR(c.fecha_limite, 'YYYY-MM-DD'), "
                + "c.estado "
                + "FROM feder.CAMPANA c "
                + "INNER JOIN feder.COMUNIDAD com ON c.comunidad_id = com.id_comunidad "
                + "LEFT JOIN feder.APORTE a ON a.campana_id = c.id_campana "
                + "GROUP BY c.id_campana, c.titulo, com.nombre, c.meta_economica, "
                + "c.fecha_inicio, c.fecha_limite, c.estado "
                + "ORDER BY c.id_campana ASC";

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
                    rs.getString(6),
                    rs.getString(7),
                    rs.getString(8),
                    rs.getString(9)
                });
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] DCampana.list: " + e.getMessage());
        }
        return resultado;
    }

    /**
     * RF-10: Obtiene el seguimiento detallado de recaudación (todas las campañas
     * o una campaña específica) tomando como base exclusivamente los aportes confirmados.
     *
     * @param idCampanaFiltro ID de campaña opcional (null para consultar todas).
     * @return Lista de filas String[] alineadas con HEADERS_SEGUIMIENTO.
     */
    public List<String[]> obtenerSeguimientoRecaudacion(Integer idCampanaFiltro) {
        List<String[]> resultado = new ArrayList<>();
        boolean filtrarPorId = idCampanaFiltro != null;

        String sql = "SELECT c.id_campana, c.titulo, com.nombre, "
                + "TO_CHAR(c.meta_economica, 'FM999999990.00') AS meta, "
                + "TO_CHAR(COALESCE(SUM(CASE WHEN LOWER(a.estado) = 'confirmado' "
                + "   THEN a.monto ELSE 0 END), 0.00), 'FM999999990.00') AS confirmado, "
                + "TO_CHAR(GREATEST(0.00, c.meta_economica - COALESCE(SUM(CASE "
                + "   WHEN LOWER(a.estado) = 'confirmado' THEN a.monto ELSE 0 END), 0.00)), "
                + "   'FM999999990.00') AS faltante, "
                + "TO_CHAR(ROUND((COALESCE(SUM(CASE WHEN LOWER(a.estado) = 'confirmado' "
                + "   THEN a.monto ELSE 0 END), 0.00) / c.meta_economica) * 100, 2), "
                + "   'FM999990.00') || '%' AS porcentaje, "
                + "COUNT(CASE WHEN LOWER(a.estado) = 'confirmado' THEN 1 END) AS cant_conf, "
                + "COUNT(CASE WHEN a.id_aporte IS NOT NULL "
                + "   AND LOWER(a.estado) <> 'confirmado' THEN 1 END) AS cant_pend, "
                + "TO_CHAR(c.fecha_limite, 'YYYY-MM-DD') AS plazo, "
                + "c.estado "
                + "FROM feder.CAMPANA c "
                + "INNER JOIN feder.COMUNIDAD com ON c.comunidad_id = com.id_comunidad "
                + "LEFT JOIN feder.APORTE a ON a.campana_id = c.id_campana "
                + (filtrarPorId ? "WHERE c.id_campana = ? " : "")
                + "GROUP BY c.id_campana, c.titulo, com.nombre, c.meta_economica, "
                + "c.fecha_limite, c.estado "
                + "ORDER BY c.id_campana ASC";

        try (Connection conn = SqlConnetion.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            if (filtrarPorId) {
                ps.setInt(1, idCampanaFiltro);
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
                        String.valueOf(rs.getInt(8)),
                        String.valueOf(rs.getInt(9)),
                        rs.getString(10),
                        rs.getString(11)
                    });
                }
            }
        } catch (SQLException e) {
            System.err.println(
                    "[ERROR] DCampana.obtenerSeguimientoRecaudacion: " + e.getMessage()
            );
        }
        return resultado;
    }

    /**
     * Obtiene una campaña por su identificador primario.
     *
     * @param id Identificador de la campaña.
     * @return Instancia de Campana o null si no existe.
     */
    public Campana getById(int id) {
        String sql = "SELECT id_campana, titulo, descripcion, meta_economica, "
                + "monto_recaudado, fecha_inicio, fecha_limite, estado, comunidad_id "
                + "FROM feder.CAMPANA WHERE id_campana = ?";

        try (Connection conn = SqlConnetion.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Campana(
                            rs.getInt("id_campana"),
                            rs.getString("titulo"),
                            rs.getString("descripcion"),
                            rs.getBigDecimal("meta_economica"),
                            rs.getBigDecimal("monto_recaudado"),
                            rs.getDate("fecha_inicio"),
                            rs.getDate("fecha_limite"),
                            rs.getString("estado"),
                            rs.getInt("comunidad_id")
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] DCampana.getById: " + e.getMessage());
        }
        return null;
    }
}
