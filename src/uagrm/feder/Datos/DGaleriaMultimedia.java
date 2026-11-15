package uagrm.feder.Datos;

import uagrm.feder.Entity.GaleriaMultimedia;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Capa de Datos: DGaleriaMultimedia.
 * Gestiona la persistencia en PostgreSQL para la tabla GALERIA_MULTIMEDIA (RF-02)
 * con registro transaccional en AUDITORIA.
 */
public class DGaleriaMultimedia {

    public static final String[] HEADERS = {
        "ID", "TITULO", "TIPO", "URL ARCHIVO", "FECHA CARGA", "NOTICIA ASOCIADA"
    };

    /**
     * Registra un nuevo elemento multimedia vinculado a una noticia y audita la acción.
     *
     * @param titulo            Título del recurso multimedia.
     * @param tipo              Tipo de archivo ('Imagen' o 'Video').
     * @param urlArchivo        URL o enlace del archivo.
     * @param descripcion       Descripción breve del contenido.
     * @param noticiaId         ID de la noticia a la que pertenece.
     * @param usuarioOperadorId ID del usuario responsable.
     * @return true si la inserción fue exitosa.
     */
    public boolean insert(String titulo, String tipo, String urlArchivo,
            String descripcion, int noticiaId, int usuarioOperadorId) {
        String sqlMulti = "INSERT INTO feder.GALERIA_MULTIMEDIA "
                + "(titulo, tipo, url_archivo, descripcion, fecha_carga, noticia_id) "
                + "VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, ?)";
        String sqlAuditoria = "INSERT INTO feder.AUDITORIA "
                + "(accion_realizada, tabla_afectada, usuario_id) VALUES (?, ?, ?)";

        try (Connection conn = SqlConnetion.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement psMulti = conn.prepareStatement(sqlMulti);
                 PreparedStatement psAuditoria = conn.prepareStatement(sqlAuditoria)) {

                psMulti.setString(1, titulo.trim());
                psMulti.setString(2, tipo.trim());
                psMulti.setString(3, urlArchivo.trim());
                psMulti.setString(4, descripcion != null ? descripcion.trim() : "");
                psMulti.setInt(5, noticiaId);

                if (psMulti.executeUpdate() == 0) {
                    conn.rollback();
                    return false;
                }

                psAuditoria.setString(1, "INSERT");
                psAuditoria.setString(2, "GALERIA_MULTIMEDIA");
                psAuditoria.setInt(3, usuarioOperadorId);
                psAuditoria.executeUpdate();

                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                System.err.println("[ERROR] DGaleriaMultimedia.insert: " + e.getMessage());
                return false;
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] Conexión en DGaleriaMultimedia.insert: " + e.getMessage());
            return false;
        }
    }

    /**
     * Actualiza un registro de galería multimedia existente y audita la operación.
     *
     * @param id                ID del elemento multimedia.
     * @param titulo            Nuevo título.
     * @param tipo              Tipo ('Imagen' o 'Video').
     * @param urlArchivo        Nueva URL del recurso.
     * @param descripcion       Nueva descripción.
     * @param noticiaId         ID de la noticia asociada.
     * @param usuarioOperadorId ID del usuario operador.
     * @return true si la actualización fue exitosa.
     */
    public boolean update(int id, String titulo, String tipo, String urlArchivo,
            String descripcion, int noticiaId, int usuarioOperadorId) {
        String sqlMulti = "UPDATE feder.GALERIA_MULTIMEDIA SET titulo = ?, tipo = ?, "
                + "url_archivo = ?, descripcion = ?, noticia_id = ? WHERE id_multimedia = ?";
        String sqlAuditoria = "INSERT INTO feder.AUDITORIA "
                + "(accion_realizada, tabla_afectada, usuario_id) VALUES (?, ?, ?)";

        try (Connection conn = SqlConnetion.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement psMulti = conn.prepareStatement(sqlMulti);
                 PreparedStatement psAuditoria = conn.prepareStatement(sqlAuditoria)) {

                psMulti.setString(1, titulo.trim());
                psMulti.setString(2, tipo.trim());
                psMulti.setString(3, urlArchivo.trim());
                psMulti.setString(4, descripcion != null ? descripcion.trim() : "");
                psMulti.setInt(5, noticiaId);
                psMulti.setInt(6, id);

                if (psMulti.executeUpdate() == 0) {
                    conn.rollback();
                    return false;
                }

                psAuditoria.setString(1, "UPDATE");
                psAuditoria.setString(2, "GALERIA_MULTIMEDIA");
                psAuditoria.setInt(3, usuarioOperadorId);
                psAuditoria.executeUpdate();

                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                System.err.println("[ERROR] DGaleriaMultimedia.update: " + e.getMessage());
                return false;
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] Conexión en DGaleriaMultimedia.update: " + e.getMessage());
            return false;
        }
    }

    /**
     * Elimina un recurso de la galería multimedia y registra la acción en AUDITORIA.
     *
     * @param id                ID del elemento multimedia a eliminar.
     * @param usuarioOperadorId ID del usuario operador.
     * @return true si se eliminó correctamente.
     */
    public boolean delete(int id, int usuarioOperadorId) {
        String sqlMulti = "DELETE FROM feder.GALERIA_MULTIMEDIA WHERE id_multimedia = ?";
        String sqlAuditoria = "INSERT INTO feder.AUDITORIA "
                + "(accion_realizada, tabla_afectada, usuario_id) VALUES (?, ?, ?)";

        try (Connection conn = SqlConnetion.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement psMulti = conn.prepareStatement(sqlMulti);
                 PreparedStatement psAuditoria = conn.prepareStatement(sqlAuditoria)) {

                psMulti.setInt(1, id);
                if (psMulti.executeUpdate() == 0) {
                    conn.rollback();
                    return false;
                }

                psAuditoria.setString(1, "DELETE");
                psAuditoria.setString(2, "GALERIA_MULTIMEDIA");
                psAuditoria.setInt(3, usuarioOperadorId);
                psAuditoria.executeUpdate();

                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                System.err.println("[ERROR] DGaleriaMultimedia.delete: " + e.getMessage());
                return false;
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] Conexión en DGaleriaMultimedia.delete: " + e.getMessage());
            return false;
        }
    }

    /**
     * Lista todos los elementos multimedia junto al título de la noticia asociada.
     *
     * @return Lista de filas String[] alineadas con HEADERS.
     */
    public List<String[]> list() {
        List<String[]> resultado = new ArrayList<>();
        String sql = "SELECT g.id_multimedia, g.titulo, g.tipo, g.url_archivo, "
                + "TO_CHAR(g.fecha_carga, 'YYYY-MM-DD HH24:MI'), n.titulo "
                + "FROM feder.GALERIA_MULTIMEDIA g "
                + "INNER JOIN feder.NOTICIA n ON g.noticia_id = n.id_noticia "
                + "ORDER BY g.id_multimedia ASC";

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
            System.err.println("[ERROR] DGaleriaMultimedia.list: " + e.getMessage());
        }
        return resultado;
    }

    /**
     * Obtiene un registro multimedia por su identificador primario.
     *
     * @param id Identificador del elemento multimedia.
     * @return Instancia de GaleriaMultimedia o null si no existe.
     */
    public GaleriaMultimedia getById(int id) {
        String sql = "SELECT id_multimedia, titulo, tipo, url_archivo, descripcion, "
                + "fecha_carga, noticia_id FROM feder.GALERIA_MULTIMEDIA WHERE id_multimedia = ?";

        try (Connection conn = SqlConnetion.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new GaleriaMultimedia(
                            rs.getInt("id_multimedia"),
                            rs.getString("titulo"),
                            rs.getString("tipo"),
                            rs.getString("url_archivo"),
                            rs.getString("descripcion"),
                            rs.getTimestamp("fecha_carga"),
                            rs.getInt("noticia_id")
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] DGaleriaMultimedia.getById: " + e.getMessage());
        }
        return null;
    }
}
