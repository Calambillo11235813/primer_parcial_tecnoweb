package uagrm.feder.Datos;

import uagrm.feder.Entity.Aporte;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Capa de Datos: DAporte.
 * Gestiona la persistencia en PostgreSQL para la tabla APORTE (RF-11, RF-12)
 * y la obtención de fichas técnicas digitales de proyectos comunitarios (RF-13).
 */
public class DAporte {

    public static final String[] HEADERS = {
        "ID", "DONANTE", "CORREO", "MONTO (BS)", "MODALIDAD",
        "FECHA REGISTRO", "ESTADO", "CAMPANA"
    };

    /**
     * Registra un nuevo compromiso de aporte con estado 'Pendiente de Confirmación'
     * y retorna el ID autogenerado para el comprobante digital.
     *
     * @param nombreDonante Nombre completo del donante.
     * @param correoDonante Correo electrónico del donante.
     * @param monto         Monto comprometido en Bolivianos (> 0).
     * @param modalidad     Modalidad de pago ('Transferencia bancaria', 'QR', 'Efectivo').
     * @param campanaId     ID de la campaña de recaudación asociada.
     * @return ID del aporte generado (> 0) o -1 si ocurrió un error.
     */
    public int insert(String nombreDonante, String correoDonante, BigDecimal monto,
            String modalidad, int campanaId) {
        String sql = "INSERT INTO feder.APORTE "
                + "(nombre_donante, correo_donante, monto, modalidad, "
                + "fecha_registro, estado, campana_id) "
                + "VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, "
                + "'Pendiente de Confirmación', ?)";

        try (Connection conn = SqlConnetion.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, nombreDonante.trim());
            ps.setString(2, correoDonante.trim().toLowerCase());
            ps.setBigDecimal(3, monto);
            ps.setString(4, modalidad.trim());
            ps.setInt(5, campanaId);

            if (ps.executeUpdate() > 0) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        return keys.getInt(1);
                    }
                }
                return 1;
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] DAporte.insert: " + e.getMessage());
        }
        return -1;
    }

    /**
     * Actualiza el estado de un aporte ('Confirmado', 'Pendiente de Confirmación', 'Rechazado'),
     * sincroniza atómicamente el monto_recaudado en CAMPANA sumando solo los aportes
     * con estado 'Confirmado' (RF-10 / RF-12) y registra la acción en AUDITORIA.
     *
     * @param idAporte          ID del aporte a actualizar.
     * @param campanaId         ID de la campaña vinculada.
     * @param nuevoEstado       Nuevo estado del aporte.
     * @param usuarioOperadorId ID del funcionario (Secretaria/Director) que verifica.
     * @return true si la transacción completa fue exitosa.
     */
    public boolean updateEstadoYSincronizarCampana(int idAporte, int campanaId,
            String nuevoEstado, int usuarioOperadorId) {
        String sqlUpdateAporte = "UPDATE feder.APORTE SET estado = ? WHERE id_aporte = ?";
        String sqlSyncCampana = "UPDATE feder.CAMPANA SET monto_recaudado = ("
                + "SELECT COALESCE(SUM(monto), 0.00) FROM feder.APORTE "
                + "WHERE campana_id = ? AND LOWER(estado) = 'confirmado'"
                + ") WHERE id_campana = ?";
        String sqlAuditoria = "INSERT INTO feder.AUDITORIA "
                + "(accion_realizada, tabla_afectada, usuario_id) VALUES (?, ?, ?)";

        try (Connection conn = SqlConnetion.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement psApo = conn.prepareStatement(sqlUpdateAporte);
                 PreparedStatement psSync = conn.prepareStatement(sqlSyncCampana);
                 PreparedStatement psAud = conn.prepareStatement(sqlAuditoria)) {

                psApo.setString(1, nuevoEstado.trim());
                psApo.setInt(2, idAporte);
                if (psApo.executeUpdate() == 0) {
                    conn.rollback();
                    return false;
                }

                psSync.setInt(1, campanaId);
                psSync.setInt(2, campanaId);
                psSync.executeUpdate();

                psAud.setString(1, "UPDATE (" + nuevoEstado.trim() + ")");
                psAud.setString(2, "APORTE");
                psAud.setInt(3, usuarioOperadorId);
                psAud.executeUpdate();

                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                System.err.println(
                        "[ERROR] DAporte.updateEstadoYSincronizarCampana: " + e.getMessage()
                );
                return false;
            }
        } catch (SQLException e) {
            System.err.println(
                    "[ERROR] Conexión en DAporte.updateEstadoYSincronizarCampana: "
                    + e.getMessage()
            );
            return false;
        }
    }

    /**
     * Lista los aportes registrados, opcionalmente filtrados por campaña.
     *
     * @param campanaIdFiltro ID de campaña opcional (null para listar todos).
     * @return Lista de filas String[] alineadas con HEADERS.
     */
    public List<String[]> list(Integer campanaIdFiltro) {
        List<String[]> resultado = new ArrayList<>();
        boolean filtrar = campanaIdFiltro != null;

        String sql = "SELECT a.id_aporte, a.nombre_donante, a.correo_donante, "
                + "TO_CHAR(a.monto, 'FM999999990.00'), a.modalidad, "
                + "TO_CHAR(a.fecha_registro, 'YYYY-MM-DD HH24:MI'), "
                + "a.estado, c.titulo "
                + "FROM feder.APORTE a "
                + "INNER JOIN feder.CAMPANA c ON a.campana_id = c.id_campana "
                + (filtrar ? "WHERE a.campana_id = ? " : "")
                + "ORDER BY a.id_aporte ASC";

        try (Connection conn = SqlConnetion.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            if (filtrar) {
                ps.setInt(1, campanaIdFiltro);
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
            System.err.println("[ERROR] DAporte.list: " + e.getMessage());
        }
        return resultado;
    }

    /**
     * Obtiene un aporte por su identificador primario.
     *
     * @param id Identificador del aporte.
     * @return Instancia de Aporte o null si no existe.
     */
    public Aporte getById(int id) {
        String sql = "SELECT id_aporte, nombre_donante, correo_donante, monto, "
                + "modalidad, fecha_registro, estado, campana_id "
                + "FROM feder.APORTE WHERE id_aporte = ?";

        try (Connection conn = SqlConnetion.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Aporte(
                            rs.getInt("id_aporte"),
                            rs.getString("nombre_donante"),
                            rs.getString("correo_donante"),
                            rs.getBigDecimal("monto"),
                            rs.getString("modalidad"),
                            rs.getTimestamp("fecha_registro"),
                            rs.getString("estado"),
                            rs.getInt("campana_id")
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] DAporte.getById: " + e.getMessage());
        }
        return null;
    }

    /**
     * RF-13: Obtiene la información consolidada de una campaña/proyecto comunitario
     * (o de todos los proyectos si idCampana es null) junto con sus programas productivos
     * para generar la ficha técnica digital descargable por correo.
     *
     * @param idCampana ID de campaña opcional (null para exportar todas).
     * @return Texto formateado con la(s) ficha(s) técnica(s) del proyecto.
     */
    public String generarFichaTecnicaProyecto(Integer idCampana) {
        boolean filtrar = idCampana != null;
        String sqlCampana = "SELECT c.id_campana, c.titulo, c.descripcion, "
                + "TO_CHAR(c.meta_economica, 'FM999999990.00') AS meta, "
                + "TO_CHAR(COALESCE(SUM(CASE WHEN LOWER(a.estado) = 'confirmado' "
                + "   THEN a.monto ELSE 0 END), 0.00), 'FM999999990.00') AS confirmado, "
                + "TO_CHAR(GREATEST(0.00, c.meta_economica - COALESCE(SUM(CASE "
                + "   WHEN LOWER(a.estado) = 'confirmado' THEN a.monto ELSE 0 END), 0.00)), "
                + "   'FM999999990.00') AS faltante, "
                + "TO_CHAR(ROUND((COALESCE(SUM(CASE WHEN LOWER(a.estado) = 'confirmado' "
                + "   THEN a.monto ELSE 0 END), 0.00) / c.meta_economica) * 100, 2), "
                + "   'FM999990.00') || '%' AS porcentaje, "
                + "TO_CHAR(c.fecha_inicio, 'YYYY-MM-DD') AS f_inicio, "
                + "TO_CHAR(c.fecha_limite, 'YYYY-MM-DD') AS f_limite, "
                + "c.estado, com.id_comunidad, com.nombre AS com_nombre, "
                + "com.ubicacion AS com_ubicacion, COALESCE(com.descripcion, '') AS com_desc "
                + "FROM feder.CAMPANA c "
                + "INNER JOIN feder.COMUNIDAD com ON c.comunidad_id = com.id_comunidad "
                + "LEFT JOIN feder.APORTE a ON a.campana_id = c.id_campana "
                + (filtrar ? "WHERE c.id_campana = ? " : "")
                + "GROUP BY c.id_campana, c.titulo, c.descripcion, c.meta_economica, "
                + "c.fecha_inicio, c.fecha_limite, c.estado, com.id_comunidad, "
                + "com.nombre, com.ubicacion, com.descripcion "
                + "ORDER BY c.id_campana ASC";

        String sqlProgramas = "SELECT nombre, area_productiva, "
                + "COALESCE(poblacion_beneficiaria, 'Comunidad en general') "
                + "FROM feder.PROGRAMA_PRODUCTIVO WHERE comunidad_id = ? "
                + "ORDER BY id_programa ASC";

        StringBuilder sb = new StringBuilder();
        int contador = 0;

        try (Connection conn = SqlConnetion.getConnection();
             PreparedStatement psCmp = conn.prepareStatement(sqlCampana);
             PreparedStatement psProg = conn.prepareStatement(sqlProgramas)) {

            if (filtrar) {
                psCmp.setInt(1, idCampana);
            }

            try (ResultSet rs = psCmp.executeQuery()) {
                while (rs.next()) {
                    contador++;
                    int idCmp = rs.getInt("id_campana");
                    int idCom = rs.getInt("id_comunidad");

                    sb.append("========================================================================\n");
                    sb.append("        FICHA TECNICA DIGITAL DE PROYECTO COMUNITARIO - FEDER           \n");
                    sb.append("========================================================================\n");
                    sb.append("1. IDENTIFICACION DEL PROYECTO / CAMPANA\n");
                    sb.append("   Codigo de Proyecto : CMP-").append(idCmp).append("\n");
                    sb.append("   Titulo             : ").append(rs.getString("titulo")).append("\n");
                    sb.append("   Estado Actual      : ").append(rs.getString("estado")).append("\n");
                    sb.append("   Vigencia           : ").append(rs.getString("f_inicio"))
                      .append(" al ").append(rs.getString("f_limite")).append("\n");
                    sb.append("   Resumen Ejecutivo  : ").append(rs.getString("descripcion")).append("\n\n");

                    sb.append("2. COMUNIDAD RURAL BENEFICIARIA\n");
                    sb.append("   Comunidad          : ").append(rs.getString("com_nombre")).append("\n");
                    sb.append("   Ubicacion          : ").append(rs.getString("com_ubicacion")).append("\n");
                    sb.append("   Perfil Productivo  : ").append(rs.getString("com_desc")).append("\n\n");

                    sb.append("3. PROGRAMAS DE ASISTENCIA TECNICA VINCULADOS\n");
                    psProg.setInt(1, idCom);
                    try (ResultSet rsProg = psProg.executeQuery()) {
                        boolean tieneProg = false;
                        while (rsProg.next()) {
                            tieneProg = true;
                            sb.append("   - ").append(rsProg.getString(1))
                              .append(" [Rubro: ").append(rsProg.getString(2))
                              .append(" | Beneficiarios: ").append(rsProg.getString(3))
                              .append("]\n");
                        }
                        if (!tieneProg) {
                            sb.append("   - Asistencia tecnica integral comunitaria.\n");
                        }
                    }
                    sb.append("\n");

                    sb.append("4. ESQUEMA FINANCIERO Y AVANCE DE RECAUDACION\n");
                    sb.append("   Meta Economica     : Bs. ").append(rs.getString("meta")).append("\n");
                    sb.append("   Aportes Confirmados: Bs. ").append(rs.getString("confirmado")).append("\n");
                    sb.append("   Saldo por Cubrir   : Bs. ").append(rs.getString("faltante")).append("\n");
                    sb.append("   Porcentaje Avance  : ").append(rs.getString("porcentaje")).append("\n");
                    sb.append("   Modalidades Aporte : Transferencia bancaria, QR, Efectivo\n");
                    sb.append("   Comando para Donar : INSAPO [Tu Nombre; Monto; Modalidad; ")
                      .append(idCmp).append("]\n");
                    sb.append("========================================================================\n\n");
                }
            }
        } catch (SQLException e) {
            System.err.println(
                    "[ERROR] DAporte.generarFichaTecnicaProyecto: " + e.getMessage()
            );
            return "ERROR [500]: Fallo al generar la ficha técnica del proyecto.";
        }

        if (contador == 0) {
            return filtrar
                    ? "ERROR [404]: No existe ningún proyecto o campaña con ID "
                      + idCampana + "."
                    : "La consulta no retornó proyectos comunitarios registrados.";
        }
        return sb.toString().trim();
    }
}
