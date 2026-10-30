package uagrm.feder.Datos;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Gestor de conexión JDBC a la base de datos PostgreSQL institucional.
 * Esquema: feder
 * Base de datos: db_grupo16sc
 */
public class SqlConnetion {

    private static final String DRIVER = "org.postgresql.Driver";
    private static final String HOST = "mail.tecnoweb.org.bo";
    private static final int PORT = 5432;
    private static final String DATABASE = "db_grupo16sc";
    private static final String SCHEMA = "feder";
    private static final String USER = "grupo16sc";
    private static final String PASSWORD = "grup016grup016*"; // Contraseña estándar asignada al grupo

    private static final String URL = "jdbc:postgresql://" + HOST + ":" + PORT + "/" + DATABASE + "?currentSchema=" + SCHEMA;

    static {
        try {
            Class.forName(DRIVER);
        } catch (ClassNotFoundException e) {
            System.err.println("Error: Driver PostgreSQL no encontrado en el classpath: " + e.getMessage());
        }
    }

    /**
     * Obtiene una nueva conexión a la base de datos PostgreSQL.
     * @return Connection activa
     * @throws SQLException si ocurre un error de conexión
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    /**
     * Cierra de forma segura una conexión activa.
     * @param conn Conexión a cerrar
     */
    public static void closeConnection(Connection conn) {
        if (conn != null) {
            try {
                if (!conn.isClosed()) {
                    conn.close();
                }
            } catch (SQLException e) {
                System.err.println("Error al cerrar conexión JDBC: " + e.getMessage());
            }
        }
    }
}
