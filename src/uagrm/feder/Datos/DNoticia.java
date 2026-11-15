package uagrm.feder.Datos;

import uagrm.feder.Entity.Noticia;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Capa de Datos: DNoticia.
 * Gestiona la persistencia en PostgreSQL para la tabla NOTICIA (RF-01)
 * con registro transaccional en AUDITORIA.
 */
public class DNoticia {

    public static final String[] HEADERS = {
        "ID", "TITULO", "FECHA PUBLICACION", "ETIQUETAS", "ESTADO", "AUTOR"
    };

    /**
     * Registra una nueva noticia o comunicado con estado 'Publicado' y audita la acción.
     *
     * @param titulo            Título de la noticia.
     * @param contenido         Cuerpo completo del artículo.
     * @param imagenPrincipal   URL o ruta de la imagen principal.
     * @param etiquetas         Etiquetas descriptivas separadas por coma.
     * @param usuarioOperadorId ID del usuario autor/operador.
     * @return true si la transacción se completó correctamente.
     */
    public boolean insert(String titulo, String contenido, String imagenPrincipal,
            String etiquetas, int usuarioOperadorId) {
        String sqlNoticia = "INSERT INTO feder.NOTICIA "
                + "(titulo, contenido, fecha_publicacion, imagen_principal, "
                + "etiquetas, estado, usuario_id) "
                + "VALUES (?, ?, CURRENT_DATE, ?, ?, 'Publicado', ?)";
        String sqlAuditoria = "INSERT INTO feder.AUDITORIA "
                + "(accion_realizada, tabla_afectada, usuario_id) VALUES (?, ?, ?)";

        try (Connection conn = SqlConnetion.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement psNoticia = conn.prepareStatement(sqlNoticia);
                 PreparedStatement psAuditoria = conn.prepareStatement(sqlAuditoria)) {

                psNoticia.setString(1, titulo.trim());
                psNoticia.setString(2, contenido.trim());
                psNoticia.setString(3, imagenPrincipal.trim());
                psNoticia.setString(4, etiquetas != null ? etiquetas.trim() : "");
                psNoticia.setInt(5, usuarioOperadorId);

                if (psNoticia.executeUpdate() == 0) {
                    conn.rollback();
                    return false;
                }

                psAuditoria.setString(1, "INSERT");
                psAuditoria.setString(2, "NOTICIA");
                psAuditoria.setInt(3, usuarioOperadorId);
                psAuditoria.executeUpdate();

                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                System.err.println("[ERROR] DNoticia.insert: " + e.getMessage());
                return false;
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] Conexión en DNoticia.insert: " + e.getMessage());
            return false;
        }
    }

    /**
     * Actualiza los datos o el estado de una noticia existente y audita la operación.
     *
     * @param id                ID de la noticia a modificar.
     * @param titulo            Nuevo título.
     * @param contenido         Nuevo contenido.
     * @param imagenPrincipal   Nueva imagen principal.
     * @param etiquetas         Nuevas etiquetas.
     * @param estado            Estado ('Publicado', 'Borrador', 'Archivado').
     * @param usuarioOperadorId ID del usuario operador.
     * @return true si la actualización fue exitosa.
     */
    public boolean update(int id, String titulo, String contenido, String imagenPrincipal,
            String etiquetas, String estado, int usuarioOperadorId) {
        String sqlNoticia = "UPDATE feder.NOTICIA SET titulo = ?, contenido = ?, "
                + "imagen_principal = ?, etiquetas = ?, estado = ? WHERE id_noticia = ?";
        String sqlAuditoria = "INSERT INTO feder.AUDITORIA "
                + "(accion_realizada, tabla_afectada, usuario_id) VALUES (?, ?, ?)";

        try (Connection conn = SqlConnetion.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement psNoticia = conn.prepareStatement(sqlNoticia);
                 PreparedStatement psAuditoria = conn.prepareStatement(sqlAuditoria)) {

                psNoticia.setString(1, titulo.trim());
                psNoticia.setString(2, contenido.trim());
                psNoticia.setString(3, imagenPrincipal.trim());
                psNoticia.setString(4, etiquetas != null ? etiquetas.trim() : "");
                psNoticia.setString(5, estado.trim());
                psNoticia.setInt(6, id);

                if (psNoticia.executeUpdate() == 0) {
                    conn.rollback();
                    return false;
                }

                psAuditoria.setString(1, "UPDATE");
                psAuditoria.setString(2, "NOTICIA");
                psAuditoria.setInt(3, usuarioOperadorId);
                psAuditoria.executeUpdate();

                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                System.err.println("[ERROR] DNoticia.update: " + e.getMessage());
                return false;
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] Conexión en DNoticia.update: " + e.getMessage());
            return false;
        }
    }

    /**
     * Archiva una noticia cambiando su estado a 'Archivado' (baja lógica) y audita la acción.
     *
     * @param id                ID de la noticia a archivar.
     * @param usuarioOperadorId ID del usuario operador.
     * @return true si se archivó correctamente.
     */
    public boolean delete(int id, int usuarioOperadorId) {
        String sqlNoticia = "UPDATE feder.NOTICIA SET estado = 'Archivado' WHERE id_noticia = ?";
        String sqlAuditoria = "INSERT INTO feder.AUDITORIA "
                + "(accion_realizada, tabla_afectada, usuario_id) VALUES (?, ?, ?)";

        try (Connection conn = SqlConnetion.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement psNoticia = conn.prepareStatement(sqlNoticia);
                 PreparedStatement psAuditoria = conn.prepareStatement(sqlAuditoria)) {

                psNoticia.setInt(1, id);
                if (psNoticia.executeUpdate() == 0) {
                    conn.rollback();
                    return false;
                }

                psAuditoria.setString(1, "DELETE");
                psAuditoria.setString(2, "NOTICIA");
                psAuditoria.setInt(3, usuarioOperadorId);
                psAuditoria.executeUpdate();

                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                System.err.println("[ERROR] DNoticia.delete: " + e.getMessage());
                return false;
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] Conexión en DNoticia.delete: " + e.getMessage());
            return false;
        }
    }

    /**
     * Lista todas las noticias registradas junto con el nombre de su autor.
     *
     * @return Lista de filas String[] alineadas con HEADERS.
     */
    public List<String[]> list() {
        List<String[]> resultado = new ArrayList<>();
        String sql = "SELECT n.id_noticia, n.titulo, TO_CHAR(n.fecha_publicacion, 'YYYY-MM-DD'), "
                + "COALESCE(n.etiquetas, ''), n.estado, u.nombre_completo "
                + "FROM feder.NOTICIA n "
                + "INNER JOIN feder.USUARIO u ON n.usuario_id = u.id_usuario "
                + "ORDER BY n.id_noticia ASC";

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
            System.err.println("[ERROR] DNoticia.list: " + e.getMessage());
        }
        return resultado;
    }

    /**
     * Obtiene una noticia por su identificador primario.
     *
     * @param id Identificador de la noticia.
     * @return Instancia de Noticia o null si no existe.
     */
    public Noticia getById(int id) {
        String sql = "SELECT id_noticia, titulo, contenido, fecha_publicacion, "
                + "imagen_principal, etiquetas, estado, usuario_id "
                + "FROM feder.NOTICIA WHERE id_noticia = ?";

        try (Connection conn = SqlConnetion.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Noticia(
                            rs.getInt("id_noticia"),
                            rs.getString("titulo"),
                            rs.getString("contenido"),
                            rs.getDate("fecha_publicacion"),
                            rs.getString("imagen_principal"),
                            rs.getString("etiquetas"),
                            rs.getString("estado"),
                            rs.getInt("usuario_id")
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] DNoticia.getById: " + e.getMessage());
        }
        return null;
    }
}
