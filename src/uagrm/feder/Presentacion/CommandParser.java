package uagrm.feder.Presentacion;

import uagrm.feder.Datos.DUsuario;
import uagrm.feder.Negocio.NUsuario;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Analizador Sintáctico determinista de comandos para el sistema FEDER.
 * Procesa las órdenes enviadas por correo electrónico (Asunto o Cuerpo)
 * y las despacha a la Capa de Negocio (NUsuario).
 */
public class CommandParser {

    private final NUsuario nUsuario;

    public CommandParser() {
        this.nUsuario = new NUsuario();
    }

    public CommandParser(NUsuario nUsuario) {
        this.nUsuario = nUsuario;
    }

    /**
     * Procesa una solicitud recibida por correo y retorna el texto de respuesta.
     *
     * @param remitente Dirección de correo de quien envía el comando.
     * @param subject   Asunto del correo electrónico.
     * @param body      Cuerpo del mensaje.
     * @return Cadena con el resultado de la operación o mensaje de error formateado.
     */
    public String procesar(String remitente, String subject, String body) {
        if (remitente == null || remitente.trim().isEmpty()) {
            return "ERROR [401]: Remitente no identificado o dirección de correo vacía.";
        }

        // 1. Determinar cuál campo contiene el comando (prioridad: Subject, luego Body)
        String textoOrden = buscarTextoComando(subject, body);
        if (textoOrden == null || textoOrden.trim().isEmpty()) {
            return "ERROR [400]: No se detectó ningún comando válido en el asunto ni en el cuerpo del correo.\n" +
                   "Envíe 'HELP' para consultar el manual de comandos permitidos.";
        }

        // 2. Extraer el nombre del comando y sus parámetros
        String comando;
        List<String> parametros = new ArrayList<>();

        textoOrden = textoOrden.trim();

        // Patrón estándar: COMANDO [param1; param2; ...] o COMANDO []
        Pattern patronCorchetes = Pattern.compile("^([A-Za-z0-9_]+)\\s*\\[(.*)\\]$", Pattern.DOTALL);
        Matcher matcher = patronCorchetes.matcher(textoOrden);

        if (matcher.find()) {
            comando = matcher.group(1).toUpperCase();
            String contenidoParams = matcher.group(2).trim();
            if (!contenidoParams.isEmpty()) {
                String[] partes = contenidoParams.split(";");
                for (String p : partes) {
                    parametros.add(p.trim());
                }
            }
        } else {
            // Sintaxis simple sin corchetes (ej. "HELP", "LISUSR", o "INSUSR param1; param2")
            String[] partesEspacio = textoOrden.split("\\s+", 2);
            comando = partesEspacio[0].toUpperCase();

            if (partesEspacio.length > 1) {
                String resto = partesEspacio[1].trim();
                if (!resto.isEmpty()) {
                    String[] partes = resto.split(";");
                    for (String p : partes) {
                        parametros.add(p.trim());
                    }
                }
            }
        }

        // 3. Despachar a la Capa de Negocio según el comando
        try {
            switch (comando) {
                case "INSUSR":
                    return nUsuario.registrar(parametros, remitente);

                case "MODUSR":
                    return nUsuario.modificar(parametros, remitente);

                case "DELUSR":
                    return nUsuario.eliminar(parametros, remitente);

                case "LISUSR":
                    List<String[]> usuarios = nUsuario.listar(remitente);
                    return construirTablaAscii(DUsuario.HEADERS, usuarios);

                case "HELP":
                    return obtenerManualAyuda();

                default:
                    return "ERROR [404]: Comando desconocido '" + comando + "'.\n" +
                           "Comandos disponibles: INSUSR, MODUSR, DELUSR, LISUSR, HELP.\n" +
                           "Envíe 'HELP' para mayor información de sintaxis.";
            }
        } catch (SecurityException se) {
            return se.getMessage();
        } catch (Exception ex) {
            return "ERROR [500]: Ocurrió un error inesperado al procesar el comando: " + ex.getMessage();
        }
    }

    /**
     * Localiza la línea que contiene el comando dentro del asunto o las primeras líneas del cuerpo.
     */
    private String buscarTextoComando(String subject, String body) {
        List<String> comandosValidos = Arrays.asList("INSUSR", "MODUSR", "DELUSR", "LISUSR", "HELP");

        // 1. Revisar asunto
        if (subject != null) {
            String subTrim = subject.trim();
            for (String cmd : comandosValidos) {
                if (subTrim.toUpperCase().startsWith(cmd)) {
                    return subTrim;
                }
            }
        }

        // 2. Revisar cuerpo línea por línea
        if (body != null) {
            String[] lineas = body.split("\n");
            for (String linea : lineas) {
                String lineaTrim = linea.trim();
                for (String cmd : comandosValidos) {
                    if (lineaTrim.toUpperCase().startsWith(cmd)) {
                        return lineaTrim;
                    }
                }
            }
        }

        // Si el asunto tiene texto pero no coincide con los tokens exactos, se retorna para que el parser intente evaluar
        if (subject != null && !subject.trim().isEmpty()) {
            return subject.trim();
        }

        return null;
    }

    /**
     * Construye una tabla ASCII legible y auto-dimensionada para respuestas tabulares.
     */
    public static String construirTablaAscii(String[] cabeceras, List<String[]> filas) {
        if (filas == null || filas.isEmpty()) {
            return "La consulta no retornó registros.\n";
        }

        int columnas = cabeceras.length;
        int[] anchos = new int[columnas];

        // Calcular ancho con base en las cabeceras
        for (int i = 0; i < columnas; i++) {
            anchos[i] = cabeceras[i].length();
        }

        // Ajustar ancho con base en los datos de las filas
        for (String[] fila : filas) {
            for (int i = 0; i < columnas && i < fila.length; i++) {
                if (fila[i] != null && fila[i].length() > anchos[i]) {
                    anchos[i] = fila[i].length();
                }
            }
        }

        // Construir separador (+-----+---------+)
        StringBuilder separador = new StringBuilder("+");
        for (int ancho : anchos) {
            for (int k = 0; k < ancho + 2; k++) {
                separador.append("-");
            }
            separador.append("+");
        }
        separador.append("\n");

        StringBuilder sb = new StringBuilder();
        sb.append("TOTAL REGISTROS: ").append(filas.size()).append("\n\n");
        sb.append(separador);

        // Línea de cabecera
        sb.append("|");
        for (int i = 0; i < columnas; i++) {
            sb.append(" ").append(padRight(cabeceras[i], anchos[i])).append(" |");
        }
        sb.append("\n");
        sb.append(separador);

        // Filas de datos
        for (String[] fila : filas) {
            sb.append("|");
            for (int i = 0; i < columnas; i++) {
                String valor = (i < fila.length && fila[i] != null) ? fila[i] : "";
                sb.append(" ").append(padRight(valor, anchos[i])).append(" |");
            }
            sb.append("\n");
        }
        sb.append(separador);

        return sb.toString();
    }

    private static String padRight(String s, int n) {
        return String.format("%-" + n + "s", s);
    }

    /**
     * Genera el manual del comando HELP con la sintaxis de todos los comandos soportados.
     */
    private String obtenerManualAyuda() {
        return "========================================================================\n" +
               "           MANUAL DE COMANDOS DEL SISTEMA INSTITUCIONAL FEDER           \n" +
               "========================================================================\n\n" +
               "FORMATO GENERAL:\n" +
               "  COMANDO [parametro1; parametro2; ...]\n\n" +
               "COMANDOS DISPONIBLES (CU-01: USUARIOS Y ROLES):\n" +
               "------------------------------------------------------------------------\n" +
               "1. INSUSR [nombre_completo; correo_electronico; contrasena; rol_id]\n" +
               "   - Permiso requerido: Solo rol 'Director'.\n" +
               "   - Descripción: Registra un nuevo usuario con contraseña cifrada (SHA-256).\n" +
               "   - Ejemplo: INSUSR [Maria Lopez; mlopez@feder.org.bo; Secr2026!; 2]\n\n" +
               "2. MODUSR [id_usuario; nombre_completo; correo_electronico; estado; rol_id]\n" +
               "   - Permiso requerido: Solo rol 'Director'.\n" +
               "   - Estados válidos: 'Activo' o 'Inactivo'.\n" +
               "   - Ejemplo: MODUSR [2; Maria Lopez Perez; mlopez@feder.org.bo; Activo; 2]\n\n" +
               "3. DELUSR [id_usuario]\n" +
               "   - Permiso requerido: Solo rol 'Director'.\n" +
               "   - Descripción: Realiza baja lógica (estado 'Inactivo') del usuario.\n" +
               "   - Ejemplo: DELUSR [2]\n\n" +
               "4. LISUSR []\n" +
               "   - Permiso requerido: Usuarios activos (Director, Secretaria, Instructor).\n" +
               "   - Descripción: Devuelve el listado tabulado de usuarios registrados.\n" +
               "   - Ejemplo: LISUSR []\n\n" +
               "5. HELP\n" +
               "   - Permiso requerido: Público / Cualquier usuario.\n" +
               "   - Descripción: Retorna esta guía de comandos.\n\n" +
               "CATÁLOGO DE ROLES:\n" +
               "  ID 1: Director\n" +
               "  ID 2: Secretaria\n" +
               "  ID 3: Instructor\n" +
               "========================================================================";
    }
}

