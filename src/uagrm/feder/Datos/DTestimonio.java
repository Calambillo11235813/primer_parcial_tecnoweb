package uagrm.feder.Datos;

import uagrm.feder.Entity.Testimonio;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Capa de Datos: DTestimonio.
 * Gestiona la persistencia en PostgreSQL para la tabla TESTIMONIO (RF-08)
 * con registro transaccional en AUDITORIA.
 */
public class DTestimonio {

    public static final String[] HEADERS = {
        "ID", "TITULO", "RELATO / TESTIMONIO", "FOTOGRAFIA", "ENLACE VIDEO", "ESTADO", "COMUNIDAD"
    };

    /**
     * Registra un nuevo testimonio vinculado a una comunidad y audita la operación.
     *
     * @param titulo            Título de la historia de éxito.
     * @param texto             Relato completo del testimonio.
     * @param fotografia        URL de la fotografía.
     * @param enlaceAudiovisual URL del video o audio (opcional).
     * @param comunidadId       ID de la comunidad beneficiaria.
     * @param usuarioOperadorId ID del usuario responsable.
     * @return true si la transacción fue exitosa.
     */
    public boolean insert(String titulo, String texto, String fotografia,
            String enlaceAudiovisual, int comunidadId, int usuarioOperadorId) {
        String sqlTestimonio = "INSERT INTO feder.TESTIMONIO "
                + "(titulo, texto, fotografia, enlace_audiovisual, estado, comunidad_id) "
                + "VALUES (?, ?, ?, ?, 'Publicado', ?)";
        String sqlAuditoria = "INSERT INTO feder.AUDITORIA "
                + "(accion_realizada, tabla_afectada, usuario_id) VALUES (?, ?, ?)";

        try (Connection conn = SqlConnetion.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement psTest = conn.prepareStatement(sqlTestimonio);
                 PreparedStatement psAuditoria = conn.prepareStatement(sqlAuditoria)) {

                psTest.setString(1, titulo.trim());
                psTest.setString(2, texto.trim());
                psTest.setString(3, fotografia != null ? fotografia.trim() : "");
                psTest.setString(4, enlaceAudiovisual != null ? enlaceAudiovisual.trim() : "");
                psTest.setInt(5, comunidadId);

                if (psTest.executeUpdate() == 0) {
                    conn.rollback();
                    return false;
                }

                psAuditoria.setString(1, "INSERT");
                psAuditoria.setString(2, "TESTIMONIO");
                psAuditoria.setInt(3, usuarioOperadorId);
                psAuditoria.executeUpdate();

                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                System.err.println("[ERROR] DTestimonio.insert: " + e.getMessage());
                return false;
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] Conexión en DTestimonio.insert: " + e.getMessage());
            return false;
        }
    }

    /**
     * Actualiza un testimonio existente y registra la operación en AUDITORIA.
     *
     * @param id                ID del testimonio.
     * @param titulo            Nuevo título.
     * @param texto             Nuevo relato.
     * @param fotografia        Nueva fotografía.
     * @param enlaceAudiovisual Nuevo enlace audiovisual.
     * @param estado            Estado ('Publicado' o 'Archivado').
     * @param comunidadId       ID de la comunidad asociada.
     * @param usuarioOperadorId ID del usuario operador.
     * @return true si la actualización fue exitosa.
     */
    public boolean update(int id, String titulo, String texto, String fotografia,
            String enlaceAudiovisual, String estado, int comunidadId, int usuarioOperadorId) {
        String sqlTestimonio = "UPDATE feder.TESTIMONIO SET titulo = ?, texto = ?, "
                + "fotografia = ?, enlace_audiovisual = ?, estado = ?, comunidad_id = ? "
                + "WHERE id_testimonio = ?";
        String sqlAuditoria = "INSERT INTO feder.AUDITORIA "
                + "(accion_realizada, tabla_afectada, usuario_id) VALUES (?, ?, ?)";

        try (Connection conn = SqlConnetion.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement psTest = conn.prepareStatement(sqlTestimonio);
                 PreparedStatement psAuditoria = conn.prepareStatement(sqlAuditoria)) {

                psTest.setString(1, titulo.trim());
                psTest.setString(2, texto.trim());
                psTest.setString(3, fotografia != null ? fotografia.trim() : "");
                psTest.setString(4, enlaceAudiovisual != null ? enlaceAudiovisual.trim() : "");
                psTest.setString(5, estado.trim());
                psTest.setInt(6, comunidadId);
                psTest.setInt(7, id);

                if (psTest.executeUpdate() == 0) {
                    conn.rollback();
                    return false;
                }

                psAuditoria.setString(1, "UPDATE");
                psAuditoria.setString(2, "TESTIMONIO");
                psAuditoria.setInt(3, usuarioOperadorId);
                psAuditoria.executeUpdate();

                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                System.err.println("[ERROR] DTestimonio.update: " + e.getMessage());
                return false;
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] Conexión en DTestimonio.update: " + e.getMessage());
            return false;
        }
    }

    /**
     * Archiva un testimonio cambiando su estado a 'Archivado' y audita la acción.
     *
     * @param id                ID del testimonio.
     * @param usuarioOperadorId ID del usuario responsable.
     * @return true si se archivó correctamente.
     */
    public boolean delete(int id, int usuarioOperadorId) {
        String sqlTestimonio = "UPDATE feder.TESTIMONIO SET estado = 'Archivado' "
                + "WHERE id_testimonio = ?";
        String sqlAuditoria = "INSERT INTO feder.AUDITORIA "
                + "(accion_realizada, tabla_afectada, usuario_id) VALUES (?, ?, ?)";

        try (Connection conn = SqlConnetion.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement psTest = conn.prepareStatement(sqlTestimonio);
                 PreparedStatement psAuditoria = conn.prepareStatement(sqlAuditoria)) {

                psTest.setInt(1, id);
                if (psTest.executeUpdate() == 0) {
                    conn.rollback();
                    return false;
                }

                psAuditoria.setString(1, "DELETE");
                psAuditoria.setString(2, "TESTIMONIO");
                psAuditoria.setInt(3, usuarioOperadorId);
                psAuditoria.executeUpdate();

                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                System.err.println("[ERROR] DTestimonio.delete: " + e.getMessage());
                return false;
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] Conexión en DTestimonio.delete: " + e.getMessage());
            return false;
        }
    }

    /**
     * Lista todos los testimonios junto con el nombre de su comunidad beneficiaria.
     *
     * @return Lista de filas String[] alineadas con HEADERS.
     */
    public List<String[]> list() {
        List<String[]> resultado = new ArrayList<>();
        String sql = "SELECT t.id_testimonio, t.titulo, t.texto, "
                + "COALESCE(t.fotografia, ''), COALESCE(t.enlace_audiovisual, ''), "
                + "t.estado, c.nombre "
                + "FROM feder.TESTIMONIO t "
                + "INNER JOIN feder.COMUNIDAD c ON t.comunidad_id = c.id_comunidad "
                + "ORDER BY t.id_testimonio ASC";

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
                    rs.getString(7)
                });
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] DTestimonio.list: " + e.getMessage());
        }
        return resultado;
    }

    /**
     * Obtiene un testimonio por su identificador primario.
     *
     * @param id Identificador del testimonio.
     * @return Instancia de Testimonio o null si no existe.
     */
    public Testimonio getById(int id) {
        String sql = "SELECT id_testimonio, titulo, texto, fotografia, "
                + "enlace_audiovisual, estado, comunidad_id "
                + "FROM feder.TESTIMONIO WHERE id_testimonio = ?";

        try (Connection conn = SqlConnetion.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Testimonio(
                            rs.getInt("id_testimonio"),
                            rs.getString("titulo"),
                            rs.getString("texto"),
                            rs.getString("fotografia"),
                            rs.getString("enlace_audiovisual"),
                            rs.getString("estado"),
                            rs.getInt("comunidad_id")
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] DTestimonio.getById: " + e.getMessage());
        }
        return null;
    }

    /**
     * Verifica si existe una comunidad registrada con el ID proporcionado.
     *
     * @param comunidadId ID de la comunidad.
     * @return true si existe en feder.COMUNIDAD.
     */
    public boolean existeComunidad(int comunidadId) {
        String sql = "SELECT 1 FROM feder.COMUNIDAD WHERE id_comunidad = ?";
        try (Connection conn = SqlConnetion.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, comunidadId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] DTestimonio.existeComunidad: " + e.getMessage());
            return false;
        }
    }
}
