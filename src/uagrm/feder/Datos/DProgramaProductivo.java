package uagrm.feder.Datos;

import uagrm.feder.Entity.ProgramaProductivo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Capa de Datos: DProgramaProductivo.
 * Gestiona la persistencia en PostgreSQL para la tabla PROGRAMA_PRODUCTIVO (RF-06)
 * y las consultas transversales de búsqueda y filtrado por comunidad o rubro (RF-07).
 */
public class DProgramaProductivo {

    public static final String[] HEADERS = {
        "ID", "PROGRAMA / TALLER", "AREA PRODUCTIVA", "POBLACION BENEFICIARIA",
        "COMUNIDAD", "DESCRIPCION"
    };

    public static final String[] HEADERS_BUSQUEDA = {
        "TIPO CONTENIDO", "ID", "TITULO / PROGRAMA", "COMUNIDAD / ETIQUETAS",
        "RUBRO / ESTADO", "DETALLE"
    };

    /**
     * Registra un nuevo programa productivo o capacitación técnica y audita la acción.
     *
     * @param nombre                Nombre del programa productivo.
     * @param descripcion           Descripción del programa.
     * @param areaProductiva        Área productiva o rubro (ej. Aceites, Jabonería, Harinas).
     * @param poblacionBeneficiaria Grupo o población beneficiaria.
     * @param comunidadId           ID de la comunidad beneficiaria.
     * @param usuarioOperadorId     ID del usuario responsable.
     * @return true si la inserción fue exitosa.
     */
    public boolean insert(String nombre, String descripcion, String areaProductiva,
            String poblacionBeneficiaria, int comunidadId, int usuarioOperadorId) {
        String sqlProg = "INSERT INTO feder.PROGRAMA_PRODUCTIVO "
                + "(nombre, descripcion, area_productiva, poblacion_beneficiaria, comunidad_id) "
                + "VALUES (?, ?, ?, ?, ?)";
        String sqlAuditoria = "INSERT INTO feder.AUDITORIA "
                + "(accion_realizada, tabla_afectada, usuario_id) VALUES (?, ?, ?)";

        try (Connection conn = SqlConnetion.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement psProg = conn.prepareStatement(sqlProg);
                 PreparedStatement psAuditoria = conn.prepareStatement(sqlAuditoria)) {

                psProg.setString(1, nombre.trim());
                psProg.setString(2, descripcion.trim());
                psProg.setString(3, areaProductiva.trim());
                psProg.setString(4,
                        poblacionBeneficiaria != null ? poblacionBeneficiaria.trim() : "");
                psProg.setInt(5, comunidadId);

                if (psProg.executeUpdate() == 0) {
                    conn.rollback();
                    return false;
                }

                psAuditoria.setString(1, "INSERT");
                psAuditoria.setString(2, "PROGRAMA_PRODUCTIVO");
                psAuditoria.setInt(3, usuarioOperadorId);
                psAuditoria.executeUpdate();

                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                System.err.println("[ERROR] DProgramaProductivo.insert: " + e.getMessage());
                return false;
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] Conexión en DProgramaProductivo.insert: " + e.getMessage());
            return false;
        }
    }

    /**
     * Actualiza un programa productivo existente y audita la acción.
     *
     * @param id                    ID del programa a modificar.
     * @param nombre                Nuevo nombre del programa.
     * @param descripcion           Nueva descripción.
     * @param areaProductiva        Nueva área productiva o rubro.
     * @param poblacionBeneficiaria Nueva población beneficiaria.
     * @param comunidadId           ID de la comunidad asociada.
     * @param usuarioOperadorId     ID del usuario responsable.
     * @return true si la actualización fue exitosa.
     */
    public boolean update(int id, String nombre, String descripcion, String areaProductiva,
            String poblacionBeneficiaria, int comunidadId, int usuarioOperadorId) {
        String sqlProg = "UPDATE feder.PROGRAMA_PRODUCTIVO SET nombre = ?, descripcion = ?, "
                + "area_productiva = ?, poblacion_beneficiaria = ?, comunidad_id = ? "
                + "WHERE id_programa = ?";
        String sqlAuditoria = "INSERT INTO feder.AUDITORIA "
                + "(accion_realizada, tabla_afectada, usuario_id) VALUES (?, ?, ?)";

        try (Connection conn = SqlConnetion.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement psProg = conn.prepareStatement(sqlProg);
                 PreparedStatement psAuditoria = conn.prepareStatement(sqlAuditoria)) {

                psProg.setString(1, nombre.trim());
                psProg.setString(2, descripcion.trim());
                psProg.setString(3, areaProductiva.trim());
                psProg.setString(4,
                        poblacionBeneficiaria != null ? poblacionBeneficiaria.trim() : "");
                psProg.setInt(5, comunidadId);
                psProg.setInt(6, id);

                if (psProg.executeUpdate() == 0) {
                    conn.rollback();
                    return false;
                }

                psAuditoria.setString(1, "UPDATE");
                psAuditoria.setString(2, "PROGRAMA_PRODUCTIVO");
                psAuditoria.setInt(3, usuarioOperadorId);
                psAuditoria.executeUpdate();

                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                System.err.println("[ERROR] DProgramaProductivo.update: " + e.getMessage());
                return false;
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] Conexión en DProgramaProductivo.update: " + e.getMessage());
            return false;
        }
    }

    /**
     * Elimina un programa productivo y registra la acción en AUDITORIA.
     *
     * @param id                ID del programa productivo a eliminar.
     * @param usuarioOperadorId ID del usuario responsable.
     * @return true si se eliminó correctamente.
     */
    public boolean delete(int id, int usuarioOperadorId) {
        String sqlProg = "DELETE FROM feder.PROGRAMA_PRODUCTIVO WHERE id_programa = ?";
        String sqlAuditoria = "INSERT INTO feder.AUDITORIA "
                + "(accion_realizada, tabla_afectada, usuario_id) VALUES (?, ?, ?)";

        try (Connection conn = SqlConnetion.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement psProg = conn.prepareStatement(sqlProg);
                 PreparedStatement psAuditoria = conn.prepareStatement(sqlAuditoria)) {

                psProg.setInt(1, id);
                if (psProg.executeUpdate() == 0) {
                    conn.rollback();
                    return false;
                }

                psAuditoria.setString(1, "DELETE");
                psAuditoria.setString(2, "PROGRAMA_PRODUCTIVO");
                psAuditoria.setInt(3, usuarioOperadorId);
                psAuditoria.executeUpdate();

                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                System.err.println("[ERROR] DProgramaProductivo.delete: " + e.getMessage());
                return false;
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] Conexión en DProgramaProductivo.delete: " + e.getMessage());
            return false;
        }
    }

    /**
     * Lista todos los programas productivos junto con el nombre de su comunidad.
     *
     * @return Lista de filas String[] alineadas con HEADERS.
     */
    public List<String[]> list() {
        List<String[]> resultado = new ArrayList<>();
        String sql = "SELECT p.id_programa, p.nombre, p.area_productiva, "
                + "COALESCE(p.poblacion_beneficiaria, ''), c.nombre, p.descripcion "
                + "FROM feder.PROGRAMA_PRODUCTIVO p "
                + "INNER JOIN feder.COMUNIDAD c ON p.comunidad_id = c.id_comunidad "
                + "ORDER BY p.id_programa ASC";

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
            System.err.println("[ERROR] DProgramaProductivo.list: " + e.getMessage());
        }
        return resultado;
    }

    /**
     * Obtiene un programa productivo por su identificador primario.
     *
     * @param id Identificador del programa productivo.
     * @return Instancia de ProgramaProductivo o null si no existe.
     */
    public ProgramaProductivo getById(int id) {
        String sql = "SELECT id_programa, nombre, descripcion, area_productiva, "
                + "poblacion_beneficiaria, comunidad_id "
                + "FROM feder.PROGRAMA_PRODUCTIVO WHERE id_programa = ?";

        try (Connection conn = SqlConnetion.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new ProgramaProductivo(
                            rs.getInt("id_programa"),
                            rs.getString("nombre"),
                            rs.getString("descripcion"),
                            rs.getString("area_productiva"),
                            rs.getString("poblacion_beneficiaria"),
                            rs.getInt("comunidad_id")
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] DProgramaProductivo.getById: " + e.getMessage());
        }
        return null;
    }

    /**
     * RF-07: Busca y filtra programas productivos, noticias y testimonios
     * por nombre de comunidad, rubro/área productiva, etiquetas o título.
     *
     * @param criterio Texto o palabra clave de búsqueda.
     * @return Lista de filas String[] alineadas con HEADERS_BUSQUEDA.
     */
    public List<String[]> buscarContenidos(String criterio) {
        List<String[]> resultado = new ArrayList<>();
        String patron = "%" + criterio.trim() + "%";

        String sql = "SELECT 'PROGRAMA' AS tipo, p.id_programa AS id, p.nombre AS titulo, "
                + "c.nombre AS comunidad_etiq, p.area_productiva AS rubro_estado, "
                + "p.descripcion AS detalle "
                + "FROM feder.PROGRAMA_PRODUCTIVO p "
                + "INNER JOIN feder.COMUNIDAD c ON p.comunidad_id = c.id_comunidad "
                + "WHERE c.nombre ILIKE ? OR p.area_productiva ILIKE ? "
                + "   OR p.nombre ILIKE ? OR p.descripcion ILIKE ? "
                + "UNION ALL "
                + "SELECT 'NOTICIA' AS tipo, n.id_noticia AS id, n.titulo AS titulo, "
                + "COALESCE(n.etiquetas, '') AS comunidad_etiq, n.estado AS rubro_estado, "
                + "n.contenido AS detalle "
                + "FROM feder.NOTICIA n "
                + "WHERE n.estado = 'Publicado' AND (n.titulo ILIKE ? "
                + "   OR COALESCE(n.etiquetas, '') ILIKE ? OR n.contenido ILIKE ?) "
                + "UNION ALL "
                + "SELECT 'TESTIMONIO' AS tipo, t.id_testimonio AS id, t.titulo AS titulo, "
                + "c.nombre AS comunidad_etiq, t.estado AS rubro_estado, "
                + "t.texto AS detalle "
                + "FROM feder.TESTIMONIO t "
                + "INNER JOIN feder.COMUNIDAD c ON t.comunidad_id = c.id_comunidad "
                + "WHERE t.estado = 'Publicado' AND (c.nombre ILIKE ? "
                + "   OR t.titulo ILIKE ? OR t.texto ILIKE ?) "
                + "ORDER BY 1 ASC, 2 ASC";

        try (Connection conn = SqlConnetion.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            for (int i = 1; i <= 10; i++) {
                ps.setString(i, patron);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    resultado.add(new String[]{
                        rs.getString(1),
                        String.valueOf(rs.getInt(2)),
                        rs.getString(3),
                        rs.getString(4),
                        rs.getString(5),
                        rs.getString(6)
                    });
                }
            }
        } catch (SQLException e) {
            System.err.println(
                    "[ERROR] DProgramaProductivo.buscarContenidos: " + e.getMessage()
            );
        }
        return resultado;
    }
}
