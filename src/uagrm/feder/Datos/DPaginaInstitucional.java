package uagrm.feder.Datos;

import uagrm.feder.Entity.PaginaInstitucional;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Capa de Datos: DPaginaInstitucional.
 * Gestiona la persistencia en PostgreSQL para la tabla PAGINA_INSTITUCIONAL (RF-03)
 * con registro transaccional en AUDITORIA.
 */
public class DPaginaInstitucional {

    public static final String[] HEADERS = {
        "ID", "TITULO", "CONTENIDO", "ULTIMA ACTUALIZACION", "ACTUALIZADO POR"
    };

    /**
     * Registra una nueva sección institucional y audita la acción.
     *
     * @param titulo            Título de la sección (ej. Mision, Vision, Resena, Contacto).
     * @param contenido         Texto descriptivo de la página institucional.
     * @param usuarioOperadorId ID del usuario responsable.
     * @return true si la transacción se completó correctamente.
     */
    public boolean insert(String titulo, String contenido, int usuarioOperadorId) {
        String sqlPagina = "INSERT INTO feder.PAGINA_INSTITUCIONAL "
                + "(titulo, contenido, ultima_actualizacion, usuario_id) "
                + "VALUES (?, ?, CURRENT_TIMESTAMP, ?)";
        String sqlAuditoria = "INSERT INTO feder.AUDITORIA "
                + "(accion_realizada, tabla_afectada, usuario_id) VALUES (?, ?, ?)";

        try (Connection conn = SqlConnetion.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement psPagina = conn.prepareStatement(sqlPagina);
                 PreparedStatement psAuditoria = conn.prepareStatement(sqlAuditoria)) {

                psPagina.setString(1, titulo.trim());
                psPagina.setString(2, contenido.trim());
                psPagina.setInt(3, usuarioOperadorId);

                if (psPagina.executeUpdate() == 0) {
                    conn.rollback();
                    return false;
                }

                psAuditoria.setString(1, "INSERT");
                psAuditoria.setString(2, "PAGINA_INSTITUCIONAL");
                psAuditoria.setInt(3, usuarioOperadorId);
                psAuditoria.executeUpdate();

                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                System.err.println("[ERROR] DPaginaInstitucional.insert: " + e.getMessage());
                return false;
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] Conexión en DPaginaInstitucional.insert: " + e.getMessage());
            return false;
        }
    }

    /**
     * Actualiza el título y contenido de una sección institucional existente y audita el cambio.
     *
     * @param id                Identificador de la página institucional.
     * @param titulo            Nuevo título de la sección.
     * @param contenido         Nuevo contenido descriptivo.
     * @param usuarioOperadorId ID del usuario responsable.
     * @return true si la actualización fue exitosa.
     */
    public boolean update(int id, String titulo, String contenido, int usuarioOperadorId) {
        String sqlPagina = "UPDATE feder.PAGINA_INSTITUCIONAL "
                + "SET titulo = ?, contenido = ?, ultima_actualizacion = CURRENT_TIMESTAMP, "
                + "usuario_id = ? WHERE id_pagina = ?";
        String sqlAuditoria = "INSERT INTO feder.AUDITORIA "
                + "(accion_realizada, tabla_afectada, usuario_id) VALUES (?, ?, ?)";

        try (Connection conn = SqlConnetion.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement psPagina = conn.prepareStatement(sqlPagina);
                 PreparedStatement psAuditoria = conn.prepareStatement(sqlAuditoria)) {

                psPagina.setString(1, titulo.trim());
                psPagina.setString(2, contenido.trim());
                psPagina.setInt(3, usuarioOperadorId);
                psPagina.setInt(4, id);

                if (psPagina.executeUpdate() == 0) {
                    conn.rollback();
                    return false;
                }

                psAuditoria.setString(1, "UPDATE");
                psAuditoria.setString(2, "PAGINA_INSTITUCIONAL");
                psAuditoria.setInt(3, usuarioOperadorId);
                psAuditoria.executeUpdate();

                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                System.err.println("[ERROR] DPaginaInstitucional.update: " + e.getMessage());
                return false;
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] Conexión en DPaginaInstitucional.update: " + e.getMessage());
            return false;
        }
    }

    /**
     * Elimina una sección institucional y registra la operación en AUDITORIA.
     *
     * @param id                ID de la página a eliminar.
     * @param usuarioOperadorId ID del usuario responsable.
     * @return true si se eliminó correctamente.
     */
    public boolean delete(int id, int usuarioOperadorId) {
        String sqlPagina = "DELETE FROM feder.PAGINA_INSTITUCIONAL WHERE id_pagina = ?";
        String sqlAuditoria = "INSERT INTO feder.AUDITORIA "
                + "(accion_realizada, tabla_afectada, usuario_id) VALUES (?, ?, ?)";

        try (Connection conn = SqlConnetion.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement psPagina = conn.prepareStatement(sqlPagina);
                 PreparedStatement psAuditoria = conn.prepareStatement(sqlAuditoria)) {

                psPagina.setInt(1, id);
                if (psPagina.executeUpdate() == 0) {
                    conn.rollback();
                    return false;
                }

                psAuditoria.setString(1, "DELETE");
                psAuditoria.setString(2, "PAGINA_INSTITUCIONAL");
                psAuditoria.setInt(3, usuarioOperadorId);
                psAuditoria.executeUpdate();

                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                System.err.println("[ERROR] DPaginaInstitucional.delete: " + e.getMessage());
                return false;
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] Conexión en DPaginaInstitucional.delete: " + e.getMessage());
            return false;
        }
    }

    /**
     * Lista todas las secciones institucionales junto con el nombre del usuario que las actualizó.
     *
     * @return Lista de filas String[] alineadas con HEADERS.
     */
    public List<String[]> list() {
        List<String[]> resultado = new ArrayList<>();
        String sql = "SELECT p.id_pagina, p.titulo, p.contenido, "
                + "TO_CHAR(p.ultima_actualizacion, 'YYYY-MM-DD HH24:MI'), u.nombre_completo "
                + "FROM feder.PAGINA_INSTITUCIONAL p "
                + "INNER JOIN feder.USUARIO u ON p.usuario_id = u.id_usuario "
                + "ORDER BY p.id_pagina ASC";

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
            System.err.println("[ERROR] DPaginaInstitucional.list: " + e.getMessage());
        }
        return resultado;
    }

    /**
     * Obtiene una página institucional por su identificador primario.
     *
     * @param id Identificador de la página.
     * @return Instancia de PaginaInstitucional o null si no existe.
     */
    public PaginaInstitucional getById(int id) {
        String sql = "SELECT id_pagina, titulo, contenido, ultima_actualizacion, usuario_id "
                + "FROM feder.PAGINA_INSTITUCIONAL WHERE id_pagina = ?";

        try (Connection conn = SqlConnetion.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearPagina(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] DPaginaInstitucional.getById: " + e.getMessage());
        }
        return null;
    }

    /**
     * Obtiene una página institucional buscando por su título (case-insensitive).
     *
     * @param titulo Título de la sección a buscar.
     * @return Instancia de PaginaInstitucional o null si no existe.
     */
    public PaginaInstitucional getByTitulo(String titulo) {
        String sql = "SELECT id_pagina, titulo, contenido, ultima_actualizacion, usuario_id "
                + "FROM feder.PAGINA_INSTITUCIONAL WHERE LOWER(titulo) = LOWER(?)";

        try (Connection conn = SqlConnetion.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, titulo.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearPagina(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] DPaginaInstitucional.getByTitulo: " + e.getMessage());
        }
        return null;
    }

    private PaginaInstitucional mapearPagina(ResultSet rs) throws SQLException {
        return new PaginaInstitucional(
                rs.getInt("id_pagina"),
                rs.getString("titulo"),
                rs.getString("contenido"),
                rs.getTimestamp("ultima_actualizacion"),
                rs.getInt("usuario_id")
        );
    }
}
