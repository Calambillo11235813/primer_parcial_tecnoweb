package uagrm.feder.Datos;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Capa de Datos: DReporte.
 * Ejecuta consultas analíticas y de agregación en PostgreSQL para el CU-08:
 * Generar Reportes y Estadísticas (RF-17: Montos recaudados, avance financiero
 * de metas, registro de aportes, volumen de consultas atendidas y auditoría).
 */
public class DReporte {

    public static final String[] HEADERS_CAMPANAS = {
        "ID", "CAMPANA", "COMUNIDAD", "META (BS)", "CONFIRMADO (BS)",
        "PENDIENTE (BS)", "FALTANTE (BS)", "AVANCE (%)", "NRO APORTES", "ESTADO"
    };

    public static final String[] HEADERS_APORTES_MODALIDAD = {
        "MODALIDAD PAGO", "TOTAL APORTES", "CONFIRMADOS", "MONTO CONFIRMADO (BS)",
        "MONTO PENDIENTE (BS)", "PARTICIPACION (%)"
    };

    public static final String[] HEADERS_CONSULTAS_VOLUMEN = {
        "ESTADO CONSULTA", "VOLUMEN (CANTIDAD)", "PORCENTAJE (%)", "ULTIMA ACTIVIDAD"
    };

    public static final String[] HEADERS_AUDITORIA = {
        "ID AUD.", "ACCION REALIZADA", "TABLA AFECTADA", "FECHA Y HORA",
        "USUARIO OPERADOR", "ROL"
    };

    /**
     * Genera la tabla estadística de avance financiero de metas por campaña (RF-17).
     *
     * @return Lista de filas String[] alineadas con HEADERS_CAMPANAS.
     */
    public List<String[]> reporteAvanceCampanas() {
        List<String[]> filas = new ArrayList<>();
        String sql = "SELECT c.id_campana, c.titulo, com.nombre, "
                + "TO_CHAR(c.meta_economica, 'FM999999990.00') AS meta, "
                + "TO_CHAR(COALESCE(SUM(CASE WHEN LOWER(a.estado) = 'confirmado' "
                + "   THEN a.monto ELSE 0 END), 0.00), 'FM999999990.00') AS confirmado, "
                + "TO_CHAR(COALESCE(SUM(CASE WHEN a.id_aporte IS NOT NULL "
                + "   AND LOWER(a.estado) LIKE 'pendiente%' THEN a.monto ELSE 0 END), 0.00), "
                + "   'FM999999990.00') AS pendiente, "
                + "TO_CHAR(GREATEST(0.00, c.meta_economica - COALESCE(SUM(CASE "
                + "   WHEN LOWER(a.estado) = 'confirmado' THEN a.monto ELSE 0 END), 0.00)), "
                + "   'FM999999990.00') AS faltante, "
                + "TO_CHAR(ROUND((COALESCE(SUM(CASE WHEN LOWER(a.estado) = 'confirmado' "
                + "   THEN a.monto ELSE 0 END), 0.00) / c.meta_economica) * 100, 2), "
                + "   'FM999990.00') || '%' AS avance, "
                + "COUNT(a.id_aporte) AS total_aportes, "
                + "c.estado "
                + "FROM feder.CAMPANA c "
                + "INNER JOIN feder.COMUNIDAD com ON c.comunidad_id = com.id_comunidad "
                + "LEFT JOIN feder.APORTE a ON a.campana_id = c.id_campana "
                + "GROUP BY c.id_campana, c.titulo, com.nombre, c.meta_economica, c.estado "
                + "ORDER BY c.id_campana ASC";

        try (Connection conn = SqlConnetion.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                filas.add(new String[]{
                    String.valueOf(rs.getInt(1)),
                    rs.getString(2),
                    rs.getString(3),
                    rs.getString(4),
                    rs.getString(5),
                    rs.getString(6),
                    rs.getString(7),
                    rs.getString(8),
                    String.valueOf(rs.getInt(9)),
                    rs.getString(10)
                });
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] DReporte.reporteAvanceCampanas: " + e.getMessage());
        }
        return filas;
    }

    /**
     * Genera la tabla estadística de aportes agrupados por modalidad de pago (RF-17).
     *
     * @return Lista de filas String[] alineadas con HEADERS_APORTES_MODALIDAD.
     */
    public List<String[]> reporteAportesPorModalidad() {
        List<String[]> filas = new ArrayList<>();
        String sql = "WITH total_conf AS ("
                + "  SELECT COALESCE(SUM(monto), 0.00) AS gran_total "
                + "  FROM feder.APORTE WHERE LOWER(estado) = 'confirmado'"
                + ") "
                + "SELECT a.modalidad, "
                + "COUNT(a.id_aporte) AS total_reg, "
                + "COUNT(CASE WHEN LOWER(a.estado) = 'confirmado' THEN 1 END) AS cant_conf, "
                + "TO_CHAR(COALESCE(SUM(CASE WHEN LOWER(a.estado) = 'confirmado' "
                + "   THEN a.monto ELSE 0 END), 0.00), 'FM999999990.00') AS m_conf, "
                + "TO_CHAR(COALESCE(SUM(CASE WHEN LOWER(a.estado) LIKE 'pendiente%' "
                + "   THEN a.monto ELSE 0 END), 0.00), 'FM999999990.00') AS m_pend, "
                + "CASE WHEN tc.gran_total > 0 THEN "
                + "   TO_CHAR(ROUND((COALESCE(SUM(CASE WHEN LOWER(a.estado) = 'confirmado' "
                + "   THEN a.monto ELSE 0 END), 0.00) / tc.gran_total) * 100, 2), "
                + "   'FM999990.00') || '%' ELSE '0.00%' END AS participacion "
                + "FROM feder.APORTE a CROSS JOIN total_conf tc "
                + "GROUP BY a.modalidad, tc.gran_total "
                + "ORDER BY a.modalidad ASC";

        try (Connection conn = SqlConnetion.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                filas.add(new String[]{
                    rs.getString(1),
                    String.valueOf(rs.getInt(2)),
                    String.valueOf(rs.getInt(3)),
                    rs.getString(4),
                    rs.getString(5),
                    rs.getString(6)
                });
            }
        } catch (SQLException e) {
            System.err.println(
                    "[ERROR] DReporte.reporteAportesPorModalidad: " + e.getMessage()
            );
        }
        return filas;
    }

    /**
     * Genera la tabla estadística de volumen de consultas ciudadanas por estado (RF-17).
     *
     * @return Lista de filas String[] alineadas con HEADERS_CONSULTAS_VOLUMEN.
     */
    public List<String[]> reporteVolumenConsultas() {
        List<String[]> filas = new ArrayList<>();
        String sql = "WITH total_con AS ("
                + "  SELECT COUNT(*) AS total FROM feder.CONSULTA"
                + ") "
                + "SELECT c.estado, "
                + "COUNT(c.id_consulta) AS volumen, "
                + "CASE WHEN tc.total > 0 THEN "
                + "   TO_CHAR(ROUND((COUNT(c.id_consulta)::numeric / tc.total) * 100, 2), "
                + "   'FM999990.00') || '%' ELSE '0.00%' END AS porcentaje, "
                + "COALESCE(TO_CHAR(MAX(c.fecha_envio), 'YYYY-MM-DD HH24:MI'), '-') "
                + "FROM feder.CONSULTA c CROSS JOIN total_con tc "
                + "GROUP BY c.estado, tc.total "
                + "ORDER BY c.estado ASC";

        try (Connection conn = SqlConnetion.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                filas.add(new String[]{
                    rs.getString(1),
                    String.valueOf(rs.getInt(2)),
                    rs.getString(3),
                    rs.getString(4)
                });
            }
        } catch (SQLException e) {
            System.err.println(
                    "[ERROR] DReporte.reporteVolumenConsultas: " + e.getMessage()
            );
        }
        return filas;
    }

    /**
     * Genera el reporte de bitácora de auditoría con las acciones registradas en el sistema.
     *
     * @return Lista de filas String[] alineadas con HEADERS_AUDITORIA.
     */
    public List<String[]> reporteAuditoria() {
        List<String[]> filas = new ArrayList<>();
        String sql = "SELECT a.id_auditoria, a.accion_realizada, a.tabla_afectada, "
                + "TO_CHAR(a.fecha_hora, 'YYYY-MM-DD HH24:MI:SS'), "
                + "u.nombre_completo, r.nombre_rol "
                + "FROM feder.AUDITORIA a "
                + "INNER JOIN feder.USUARIO u ON a.usuario_id = u.id_usuario "
                + "INNER JOIN feder.ROL r ON u.rol_id = r.id_rol "
                + "ORDER BY a.id_auditoria DESC LIMIT 50";

        try (Connection conn = SqlConnetion.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                filas.add(new String[]{
                    String.valueOf(rs.getInt(1)),
                    rs.getString(2),
                    rs.getString(3),
                    rs.getString(4),
                    rs.getString(5),
                    rs.getString(6)
                });
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] DReporte.reporteAuditoria: " + e.getMessage());
        }
        return filas;
    }

    /**
     * Obtiene los indicadores ejecutivos consolidados (KPIs globales) de todo el sistema
     * para el reporte gerencial integral (REPGEN - RF-17).
     *
     * @return Cadena con el bloque resumen de indicadores consolidados.
     */
    public String obtenerResumenIndicadoresEjecutivos() {
        String sqlKpi = "SELECT "
                + "(SELECT COUNT(*) FROM feder.CAMPANA) AS total_campanas, "
                + "(SELECT COUNT(*) FROM feder.CAMPANA "
                + "   WHERE LOWER(estado) = 'activa') AS campanas_activas, "
                + "(SELECT TO_CHAR(COALESCE(SUM(meta_economica), 0.00), 'FM999999990.00') "
                + "   FROM feder.CAMPANA) AS meta_global, "
                + "(SELECT TO_CHAR(COALESCE(SUM(monto), 0.00), 'FM999999990.00') "
                + "   FROM feder.APORTE WHERE LOWER(estado) = 'confirmado') AS rec_confirmada, "
                + "(SELECT TO_CHAR(COALESCE(SUM(monto), 0.00), 'FM999999990.00') "
                + "   FROM feder.APORTE WHERE LOWER(estado) LIKE 'pendiente%') AS rec_pendiente, "
                + "(SELECT COUNT(*) FROM feder.APORTE) AS total_aportes, "
                + "(SELECT COUNT(*) FROM feder.APORTE "
                + "   WHERE LOWER(estado) = 'confirmado') AS aportes_confirmados, "
                + "(SELECT COUNT(*) FROM feder.CONSULTA) AS total_consultas, "
                + "(SELECT COUNT(*) FROM feder.CONSULTA "
                + "   WHERE LOWER(estado) = 'atendido') AS consultas_atendidas, "
                + "(SELECT COUNT(*) FROM feder.CONSULTA "
                + "   WHERE LOWER(estado) = 'en proceso') AS consultas_proceso, "
                + "(SELECT COUNT(*) FROM feder.CONSULTA "
                + "   WHERE LOWER(estado) = 'pendiente') AS consultas_pendientes, "
                + "(SELECT COUNT(*) FROM feder.COMUNIDAD "
                + "   WHERE LOWER(estado) = 'activa') AS comunidades_activas, "
                + "(SELECT COUNT(*) FROM feder.PROGRAMA_PRODUCTIVO) AS total_programas, "
                + "(SELECT COUNT(*) FROM feder.NOTICIA "
                + "   WHERE LOWER(estado) = 'publicado') AS noticias_publicadas, "
                + "(SELECT COUNT(*) FROM feder.TESTIMONIO "
                + "   WHERE LOWER(estado) = 'publicado') AS testimonios_publicados";

        StringBuilder sb = new StringBuilder();
        try (Connection conn = SqlConnetion.getConnection();
             PreparedStatement ps = conn.prepareStatement(sqlKpi);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                int totalConsultas = rs.getInt("total_consultas");
                int atendidas = rs.getInt("consultas_atendidas");
                double tasaAtencion = totalConsultas > 0
                        ? (atendidas * 100.0) / totalConsultas : 0.0;

                sb.append("========================================================================\n");
                sb.append("       REPORTE EJECUTIVO Y ESTADISTICO CONSOLIDADO - PORTAL FEDER       \n");
                sb.append("========================================================================\n\n");
                sb.append("[1] INDICADORES FINANCIEROS Y DE RECAUDACION (CROWDFUNDING)\n");
                sb.append("    - Campanas Registradas (Totales / Activas) : ")
                  .append(rs.getInt("total_campanas")).append(" / ")
                  .append(rs.getInt("campanas_activas")).append("\n");
                sb.append("    - Meta Economica Global Acumulada          : Bs. ")
                  .append(rs.getString("meta_global")).append("\n");
                sb.append("    - Monto Total Recaudado (Confirmado)       : Bs. ")
                  .append(rs.getString("rec_confirmada")).append("\n");
                sb.append("    - Monto en Compromisos por Verificar       : Bs. ")
                  .append(rs.getString("rec_pendiente")).append("\n");
                sb.append("    - Volumen de Aportes (Confirmados / Total) : ")
                  .append(rs.getInt("aportes_confirmados")).append(" / ")
                  .append(rs.getInt("total_aportes")).append("\n\n");

                sb.append("[2] INDICADORES DE ATENCION CIUDADANA (BANDEJA DE CONSULTAS)\n");
                sb.append("    - Volumen Total de Consultas Recibidas     : ")
                  .append(totalConsultas).append("\n");
                sb.append("    - Consultas Atendidas                      : ")
                  .append(atendidas).append("\n");
                sb.append("    - Consultas En Proceso                     : ")
                  .append(rs.getInt("consultas_proceso")).append("\n");
                sb.append("    - Consultas Pendientes                     : ")
                  .append(rs.getInt("consultas_pendientes")).append("\n");
                sb.append("    - Tasa de Resolucion de Consultas          : ")
                  .append(String.format("%.2f%%", tasaAtencion)).append("\n\n");

                sb.append("[3] INDICADORES DE IMPACTO COMUNITARIO Y DIFUSION (CMS)\n");
                sb.append("    - Comunidades Beneficiarias Activas        : ")
                  .append(rs.getInt("comunidades_activas")).append("\n");
                sb.append("    - Programas Productivos en Ejecucion       : ")
                  .append(rs.getInt("total_programas")).append("\n");
                sb.append("    - Noticias y Comunicados Publicados        : ")
                  .append(rs.getInt("noticias_publicadas")).append("\n");
                sb.append("    - Testimonios de Exito Publicados          : ")
                  .append(rs.getInt("testimonios_publicados")).append("\n");
            }
        } catch (SQLException e) {
            System.err.println(
                    "[ERROR] DReporte.obtenerResumenIndicadoresEjecutivos: " + e.getMessage()
            );
            return "ERROR [500]: Fallo al calcular los indicadores ejecutivos.";
        }
        return sb.toString();
    }
}
