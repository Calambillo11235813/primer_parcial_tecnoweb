package uagrm.feder.Datos;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Gestor de conexión JDBC a la base de datos PostgreSQL.
 * Soporta alternancia rápida entre la base de datos LOCAL y la REMOTA del docente.
 * Esquema: feder | Base de datos: db_grupo16sc
 */
public class SqlConnetion {

    private static final String DRIVER = "org.postgresql.Driver";
    private static final String SCHEMA = "feder";

    // =========================================================================
    // INTERRUPTOR: true = BD Local en tu PC | false = Servidor del Docente
    // =========================================================================
    public static final boolean USAR_BD_LOCAL = true;

    // Configuración Base de Datos LOCAL
    private static final String LOCAL_HOST = "127.0.0.1";
    private static final int LOCAL_PORT = 5432;
    private static final String LOCAL_DATABASE = "db_grupo16sc";
    private static final String LOCAL_USER = "postgres";
    private static final String LOCAL_PASSWORD = "nicolas123";

    // Configuración Base de Datos REMOTA (Servidor Institucional)
    private static final String REMOTE_HOST = "mail.tecnoweb.org.bo";
    private static final int REMOTE_PORT = 5432;
    private static final String REMOTE_DATABASE = "db_grupo16sc";
    private static final String REMOTE_USER = "grupo16sc";
    private static final String REMOTE_PASSWORD = "grup016grup016*";

    // Parámetros activos según el interruptor
    private static final String HOST = USAR_BD_LOCAL ? LOCAL_HOST : REMOTE_HOST;
    private static final int PORT = USAR_BD_LOCAL ? LOCAL_PORT : REMOTE_PORT;
    private static final String DATABASE = USAR_BD_LOCAL ? LOCAL_DATABASE : REMOTE_DATABASE;
    private static final String USER = USAR_BD_LOCAL ? LOCAL_USER : REMOTE_USER;
    private static final String PASSWORD = USAR_BD_LOCAL ? LOCAL_PASSWORD : REMOTE_PASSWORD;

    private static final String URL = "jdbc:postgresql://" + HOST + ":" + PORT
            + "/" + DATABASE + "?currentSchema=" + SCHEMA;

    static {
        try {
            Class.forName(DRIVER);
        } catch (ClassNotFoundException e) {
            System.err.println("[ERROR] Driver PostgreSQL no encontrado: " + e.getMessage());
        }
    }

    /**
     * Obtiene una nueva conexión a la base de datos PostgreSQL activa.
     *
     * @return Instancia de Connection activa.
     * @throws SQLException Si ocurre un error de autenticación o de conexión JDBC.
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    /**
     * Cierra de forma segura una conexión activa.
     *
     * @param conn Conexión JDBC a cerrar.
     */
    public static void closeConnection(Connection conn) {
        if (conn != null) {
            try {
                if (!conn.isClosed()) {
                    conn.close();
                }
            } catch (SQLException e) {
                System.err.println("[ERROR] Fallo al cerrar conexión JDBC: " + e.getMessage());
            }
        }
    }
}
