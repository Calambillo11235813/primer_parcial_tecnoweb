package uagrm.feder.Datos;

import uagrm.feder.Entity.Usuario;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Capa de Datos: DUsuario
 * Gestiona el acceso y persistencia en PostgreSQL para la tabla USUARIO y su auditoría.
 */
public class DUsuario {

    public static final String[] HEADERS = {"ID", "NOMBRE COMPLETO", "CORREO ELECTRONICO", "ESTADO", "ROL"};

    /**
     * Inserta un nuevo usuario y registra la operación en la tabla AUDITORIA.
     * Manejo transaccional (commit/rollback).
     */
    public boolean insert(String nombre, String email, String passHash, int rolId, int usuarioOperadorId) {
        String sqlUsuario = "INSERT INTO feder.USUARIO (nombre_completo, correo_electronico, hash_contrasena, estado, rol_id) " +
                            "VALUES (?, ?, ?, 'Activo', ?)";
        String sqlAuditoria = "INSERT INTO feder.AUDITORIA (accion_realizada, tabla_afectada, usuario_id) " +
                              "VALUES (?, ?, ?)";

        Connection conn = null;
        PreparedStatement psUsuario = null;
        PreparedStatement psAuditoria = null;

        try {
            conn = SqlConnetion.getConnection();
            conn.setAutoCommit(false);

            // 1. Inserción de usuario
            psUsuario = conn.prepareStatement(sqlUsuario);
            psUsuario.setString(1, nombre);
            psUsuario.setString(2, email.trim().toLowerCase());
            psUsuario.setString(3, passHash);
            psUsuario.setInt(4, rolId);
            int filasUsuario = psUsuario.executeUpdate();

            if (filasUsuario == 0) {
                conn.rollback();
                return false;
            }

            // 2. Inserción de auditoría
            psAuditoria = conn.prepareStatement(sqlAuditoria);
            psAuditoria.setString(1, "INSERT");
            psAuditoria.setString(2, "USUARIO");
            psAuditoria.setInt(3, usuarioOperadorId);
            psAuditoria.executeUpdate();

            conn.commit();
            return true;
        } catch (SQLException e) {
            System.err.println("Error en DUsuario.insert: " + e.getMessage());
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    System.err.println("Error en rollback: " + ex.getMessage());
                }
            }
            return false;
        } finally {
            closeResources(psUsuario, null);
            closeResources(psAuditoria, conn);
        }
    }

    /**
     * Actualiza los datos de un usuario existente y registra la operación en AUDITORIA.
     */
    public boolean update(int id, String nombre, String email, String estado, int rolId, int usuarioOperadorId) {
        String sqlUsuario = "UPDATE feder.USUARIO SET nombre_completo = ?, correo_electronico = ?, estado = ?, rol_id = ? " +
                            "WHERE id_usuario = ?";
        String sqlAuditoria = "INSERT INTO feder.AUDITORIA (accion_realizada, tabla_afectada, usuario_id) " +
                              "VALUES (?, ?, ?)";

        Connection conn = null;
        PreparedStatement psUsuario = null;
        PreparedStatement psAuditoria = null;

        try {
            conn = SqlConnetion.getConnection();
            conn.setAutoCommit(false);

            // 1. Actualización de usuario
            psUsuario = conn.prepareStatement(sqlUsuario);
            psUsuario.setString(1, nombre);
            psUsuario.setString(2, email.trim().toLowerCase());
            psUsuario.setString(3, estado);
            psUsuario.setInt(4, rolId);
            psUsuario.setInt(5, id);
            int filasActualizadas = psUsuario.executeUpdate();

            if (filasActualizadas == 0) {
                conn.rollback();
                return false;
            }

            // 2. Registro en auditoría
            psAuditoria = conn.prepareStatement(sqlAuditoria);
            psAuditoria.setString(1, "UPDATE");
            psAuditoria.setString(2, "USUARIO");
            psAuditoria.setInt(3, usuarioOperadorId);
            psAuditoria.executeUpdate();

            conn.commit();
            return true;
        } catch (SQLException e) {
            System.err.println("Error en DUsuario.update: " + e.getMessage());
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    System.err.println("Error en rollback: " + ex.getMessage());
                }
            }
            return false;
        } finally {
            closeResources(psUsuario, null);
            closeResources(psAuditoria, conn);
        }
    }

    /**
     * Realiza baja lógica cambiando el estado a 'Inactivo' y registra en AUDITORIA.
     */
    public boolean delete(int id, int usuarioOperadorId) {
        String sqlUsuario = "UPDATE feder.USUARIO SET estado = 'Inactivo' WHERE id_usuario = ?";
        String sqlAuditoria = "INSERT INTO feder.AUDITORIA (accion_realizada, tabla_afectada, usuario_id) " +
                              "VALUES (?, ?, ?)";

        Connection conn = null;
        PreparedStatement psUsuario = null;
        PreparedStatement psAuditoria = null;

        try {
            conn = SqlConnetion.getConnection();
            conn.setAutoCommit(false);

            // 1. Baja lógica
            psUsuario = conn.prepareStatement(sqlUsuario);
            psUsuario.setInt(1, id);
            int filasAfectadas = psUsuario.executeUpdate();

            if (filasAfectadas == 0) {
                conn.rollback();
                return false;
            }

            // 2. Auditoría
            psAuditoria = conn.prepareStatement(sqlAuditoria);
            psAuditoria.setString(1, "DELETE");
            psAuditoria.setString(2, "USUARIO");
            psAuditoria.setInt(3, usuarioOperadorId);
            psAuditoria.executeUpdate();

            conn.commit();
            return true;
        } catch (SQLException e) {
            System.err.println("Error en DUsuario.delete: " + e.getMessage());
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    System.err.println("Error en rollback: " + ex.getMessage());
                }
            }
            return false;
        } finally {
            closeResources(psUsuario, null);
            closeResources(psAuditoria, conn);
        }
    }

    /**
     * Retorna una lista con todos los usuarios y el nombre de su rol (JOIN feder.ROL).
     * Cada fila coincide con la cabecera HEADERS.
     */
    public List<String[]> list() {
        List<String[]> resultado = new ArrayList<>();
        String sql = "SELECT u.id_usuario, u.nombre_completo, u.correo_electronico, u.estado, r.nombre_rol " +
                     "FROM feder.USUARIO u " +
                     "INNER JOIN feder.ROL r ON u.rol_id = r.id_rol " +
                     "ORDER BY u.id_usuario ASC";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = SqlConnetion.getConnection();
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();

            while (rs.next()) {
                String[] fila = new String[]{
                        String.valueOf(rs.getInt("id_usuario")),
                        rs.getString("nombre_completo"),
                        rs.getString("correo_electronico"),
                        rs.getString("estado"),
                        rs.getString("nombre_rol")
                };
                resultado.add(fila);
            }
        } catch (SQLException e) {
            System.err.println("Error en DUsuario.list: " + e.getMessage());
        } finally {
            closeResources(ps, conn, rs);
        }
        return resultado;
    }

    /**
     * Obtiene un usuario por su identificador primario.
     */
    public Usuario getById(int id) {
        String sql = "SELECT id_usuario, nombre_completo, correo_electronico, hash_contrasena, estado, fecha_registro, rol_id " +
                     "FROM feder.USUARIO WHERE id_usuario = ?";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = SqlConnetion.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, id);
            rs = ps.executeQuery();

            if (rs.next()) {
                return new Usuario(
                        rs.getInt("id_usuario"),
                        rs.getString("nombre_completo"),
                        rs.getString("correo_electronico"),
                        rs.getString("hash_contrasena"),
                        rs.getString("estado"),
                        rs.getTimestamp("fecha_registro"),
                        rs.getInt("rol_id")
                );
            }
        } catch (SQLException e) {
            System.err.println("Error en DUsuario.getById: " + e.getMessage());
        } finally {
            closeResources(ps, conn, rs);
        }
        return null;
    }

    /**
     * Obtiene un usuario por su correo electrónico.
     */
    public Usuario getByEmail(String email) {
        String sql = "SELECT id_usuario, nombre_completo, correo_electronico, hash_contrasena, estado, fecha_registro, rol_id " +
                     "FROM feder.USUARIO WHERE LOWER(correo_electronico) = LOWER(?)";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = SqlConnetion.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, email.trim());
            rs = ps.executeQuery();

            if (rs.next()) {
                return new Usuario(
                        rs.getInt("id_usuario"),
                        rs.getString("nombre_completo"),
                        rs.getString("correo_electronico"),
                        rs.getString("hash_contrasena"),
                        rs.getString("estado"),
                        rs.getTimestamp("fecha_registro"),
                        rs.getInt("rol_id")
                );
            }
        } catch (SQLException e) {
            System.err.println("Error en DUsuario.getByEmail: " + e.getMessage());
        } finally {
            closeResources(ps, conn, rs);
        }
        return null;
    }

    /**
     * Retorna el nombre del rol asignado a un usuario activo mediante su correo electrónico.
     * Ejemplo: 'Director', 'Secretaria', 'Instructor'.
     */
    public String getRolByEmail(String email) {
        String sql = "SELECT r.nombre_rol " +
                     "FROM feder.USUARIO u " +
                     "INNER JOIN feder.ROL r ON u.rol_id = r.id_rol " +
                     "WHERE LOWER(u.correo_electronico) = LOWER(?) AND u.estado = 'Activo'";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = SqlConnetion.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, email.trim());
            rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getString("nombre_rol");
            }
        } catch (SQLException e) {
            System.err.println("Error en DUsuario.getRolByEmail: " + e.getMessage());
        } finally {
            closeResources(ps, conn, rs);
        }
        return null;
    }

    /**
     * Verifica si existe un rol por su id.
     */
    public boolean existeRol(int rolId) {
        String sql = "SELECT 1 FROM feder.ROL WHERE id_rol = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = SqlConnetion.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, rolId);
            rs = ps.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            System.err.println("Error en DUsuario.existeRol: " + e.getMessage());
            return false;
        } finally {
            closeResources(ps, conn, rs);
        }
    }

    private void closeResources(Statement stmt, Connection conn) {
        closeResources(stmt, conn, null);
    }

    private void closeResources(Statement stmt, Connection conn, ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                System.err.println("Error al cerrar ResultSet: " + e.getMessage());
            }
        }
        if (stmt != null) {
            try {
                stmt.close();
            } catch (SQLException e) {
                System.err.println("Error al cerrar Statement: " + e.getMessage());
            }
        }
        SqlConnetion.closeConnection(conn);
    }
}

