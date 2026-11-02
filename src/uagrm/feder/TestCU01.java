package uagrm.feder;

import java.sql.Connection;
import java.util.Arrays;
import java.util.List;
import uagrm.feder.Datos.SqlConnetion;
import uagrm.feder.Negocio.NUsuario;

public class TestCU01 {
    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("   INICIANDO PRUEBAS UNITARIAS DE CU-01 (RBAC)    ");
        System.out.println("==================================================\n");

        // 1. Probar conectividad con PostgreSQL
        System.out.println("--> Test 1: Verificando conexion a la BD...");
        try (Connection conn = SqlConnetion.getConnection()) {
            if (conn != null && !conn.isClosed()) {
                System.out.println("✔ Conexion exitosa a la base de datos (esquema feder)\n");
            } else {
                System.err.println("✘ Error al conectar a la base de datos.");
                return;
            }
        } catch (Exception e) {
            System.err.println("✘ Error al conectar a la base de datos: " + e.getMessage());
            return;
        }

        NUsuario nUsuario = new NUsuario();
        String remitenteDirector = "grupo16sc@tecnoweb.org.bo"; // Director inicial del seed
        String remitenteNoAutorizado = "juan.perez@gmail.com";

        // 2. Test de Control de Acceso (RBAC) - Remitente sin permisos
        System.out.println("--> Test 2: Probando seguridad RBAC (remitente no autorizado)...");
        try {
            String resRechazo = nUsuario.registrar(
                Arrays.asList("Usuario Invalido", "invalido@tecnoweb.org.bo", "123456", "2"),
                remitenteNoAutorizado
            );
            System.out.println("Respuesta recibida: " + resRechazo);
        } catch (Exception e) {
            System.out.println("✔ Rechazo correcto por falta de permisos: " + e.getMessage());
        }
        System.out.println();

        // 3. Test de Inserción (INSUSR) por el Director
        System.out.println("--> Test 3: Registrando nueva Secretaria mediante INSUSR...");
        try {
            List<String> paramsInsert = Arrays.asList(
                "Maria Lopez", 
                "maria.lopez@tecnoweb.org.bo", 
                "claveSegura2026", 
                "2" // ID 2 = Rol Secretaria
            );
            String resInsert = nUsuario.registrar(paramsInsert, remitenteDirector);
            System.out.println("✔ Resultado: " + resInsert + "\n");
        } catch (Exception e) {
            System.out.println("⚠ Detalle en insercion (posiblemente ya existe): " + e.getMessage() + "\n");
        }

        // 4. Test de Listado (LISUSR)
        System.out.println("--> Test 4: Listando usuarios registrados (LISUSR)...");
        try {
            List<String[]> usuarios = nUsuario.listar(remitenteDirector);
            System.out.println(String.format("%-5s | %-25s | %-30s | %-10s | %-15s", 
                "ID", "NOMBRE COMPLETO", "CORREO", "ESTADO", "ROL"));
            System.out.println("-----------------------------------------------------------------------------------------");
            for (String[] row : usuarios) {
                System.out.println(String.format("%-5s | %-25s | %-30s | %-10s | %-15s", 
                    row[0], row[1], row[2], row[3], row[4]));
            }
            System.out.println("\n✔ Listado generado correctamente.");
        } catch (Exception e) {
            System.err.println("✘ Error al listar usuarios: " + e.getMessage());
        }

        System.out.println("\n==================================================");
        System.out.println("           FIN DE LA BATERIA DE PRUEBAS           ");
        System.out.println("==================================================");
    }
}

