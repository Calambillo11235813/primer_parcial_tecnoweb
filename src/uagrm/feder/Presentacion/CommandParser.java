package uagrm.feder.Presentacion;

import uagrm.feder.Datos.DPaginaInstitucional;
import uagrm.feder.Datos.DRedSocial;
import uagrm.feder.Datos.DUsuario;
import uagrm.feder.Negocio.NInstitucional;
import uagrm.feder.Negocio.NUsuario;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Analizador Sintáctico determinista de comandos para el sistema FEDER.
 * Procesa las órdenes enviadas por correo electrónico (Asunto o Cuerpo)
 * y las despacha a la Capa de Negocio (NUsuario, NInstitucional).
 */
public class CommandParser {

    private static final List<String> COMANDOS_VALIDOS = Arrays.asList(
            // CU-01: Usuarios y Roles
            "INSUSR", "MODUSR", "DELUSR", "LISUSR",
            // CU-02: Información Institucional y Redes Sociales
            "INSPAG", "MODPAG", "DELPAG", "LISPAG",
            "INSRED", "MODRED", "DELRED", "LISRED",
            // Ayuda general
            "HELP"
    );

    private final NUsuario nUsuario;
    private final NInstitucional nInstitucional;

    public CommandParser() {
        this.nUsuario = new NUsuario();
        this.nInstitucional = new NInstitucional();
    }

    public CommandParser(NUsuario nUsuario, NInstitucional nInstitucional) {
        this.nUsuario = nUsuario;
        this.nInstitucional = nInstitucional;
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

        // 1. Determinar cuál campo contiene el comando (Subject, Body o combinado)
        String textoOrden = buscarTextoComando(subject, body);
        if (textoOrden == null || textoOrden.trim().isEmpty()) {
            return "ERROR [400]: No se detectó ningún comando válido en el correo.\n"
                    + "Envíe 'HELP' para consultar el manual de comandos permitidos.";
        }

        textoOrden = textoOrden.trim();

        // 2. Extraer el nombre del comando y sus parámetros
        String comando;
        List<String> parametros = new ArrayList<>();

        Pattern patronCorchetes = Pattern.compile(
                "^([A-Za-z0-9_]+)\\s*\\[([^\\]]*)\\]", Pattern.DOTALL
        );
        Matcher matcher = patronCorchetes.matcher(textoOrden);

        if (matcher.find()) {
            comando = matcher.group(1).toUpperCase();
            String contenidoParams = matcher.group(2).trim();
            if (!contenidoParams.isEmpty()) {
                String[] partes = contenidoParams.split(";");
                for (String p : partes) {
                    parametros.add(limpiarParametro(p));
                }
            }
        } else {
            String textoLimpio = textoOrden.replaceAll("[.]+$", "").trim();
            String[] partesEspacio = textoLimpio.split("\\s+", 2);
            comando = partesEspacio[0].toUpperCase();

            if (partesEspacio.length > 1) {
                String resto = partesEspacio[1].trim().replaceAll("[\\[\\]]", "").trim();
                if (!resto.isEmpty()) {
                    String[] partes = resto.split(";");
                    for (String p : partes) {
                        parametros.add(limpiarParametro(p));
                    }
                }
            }
        }

        // 3. Despachar a la Capa de Negocio según el comando
        try {
            switch (comando) {
                // --- CU-01: Gestionar Usuarios y Roles ---
                case "INSUSR":
                    return nUsuario.registrar(parametros, remitente);
                case "MODUSR":
                    return nUsuario.modificar(parametros, remitente);
                case "DELUSR":
                    return nUsuario.eliminar(parametros, remitente);
                case "LISUSR":
                    return construirTablaAscii(DUsuario.HEADERS, nUsuario.listar(remitente));

                // --- CU-02: Gestionar Información Institucional (RF-03) ---
                case "INSPAG":
                    return nInstitucional.registrarPagina(parametros, remitente);
                case "MODPAG":
                    return nInstitucional.modificarPagina(parametros, remitente);
                case "DELPAG":
                    return nInstitucional.eliminarPagina(parametros, remitente);
                case "LISPAG":
                    return construirTablaAscii(
                            DPaginaInstitucional.HEADERS, nInstitucional.listarPaginas()
                    );

                // --- CU-02: Gestionar Redes Sociales (RF-04) ---
                case "INSRED":
                    return nInstitucional.registrarRedSocial(parametros, remitente);
                case "MODRED":
                    return nInstitucional.modificarRedSocial(parametros, remitente);
                case "DELRED":
                    return nInstitucional.eliminarRedSocial(parametros, remitente);
                case "LISRED":
                    return construirTablaAscii(
                            DRedSocial.HEADERS, nInstitucional.listarRedesSociales()
                    );

                // --- Manual de Ayuda ---
                case "HELP":
                    return obtenerManualAyuda();

                default:
                    return "ERROR [404]: Comando desconocido '" + comando + "'.\n"
                            + "Envíe 'HELP' para consultar el catálogo de comandos y su sintaxis.";
            }
        } catch (SecurityException se) {
            return se.getMessage();
        } catch (Exception ex) {
            return "ERROR [500]: Error inesperado al procesar el comando: " + ex.getMessage();
        }
    }

    private String limpiarParametro(String param) {
        if (param == null) {
            return "";
        }
        String p = param.trim();
        if ((p.startsWith("\"") && p.endsWith("\""))
                || (p.startsWith("'") && p.endsWith("'"))) {
            if (p.length() >= 2) {
                p = p.substring(1, p.length() - 1).trim();
            }
        }
        return p;
    }

    /**
     * Localiza la orden completa combinando Asunto y Cuerpo de forma flexible.
     */
    private String buscarTextoComando(String subject, String body) {
        String subTrim = subject != null ? subject.trim() : "";
        String bodyTrim = body != null ? body.trim() : "";

        for (String cmd : COMANDOS_VALIDOS) {
            if (subTrim.toUpperCase().startsWith(cmd)) {
                if (subTrim.equalsIgnoreCase(cmd) && !bodyTrim.isEmpty()
                        && !cmd.equals("HELP")) {
                    return cmd + " " + extraerBloqueDesde(bodyTrim);
                }
                return subTrim;
            }
        }

        if (!bodyTrim.isEmpty()) {
            String[] lineas = bodyTrim.split("\r?\n");
            for (int i = 0; i < lineas.length; i++) {
                String lineaTrim = lineas[i].trim();
                for (String cmd : COMANDOS_VALIDOS) {
                    if (lineaTrim.toUpperCase().startsWith(cmd)) {
                        StringBuilder bloque = new StringBuilder(lineaTrim);
                        if (lineaTrim.contains("[") && !lineaTrim.contains("]")) {
                            for (int j = i + 1; j < lineas.length; j++) {
                                bloque.append(" ").append(lineas[j].trim());
                                if (lineas[j].contains("]")) {
                                    break;
                                }
                            }
                        }
                        return bloque.toString();
                    }
                }
            }
        }

        if (!subTrim.isEmpty()) {
            return subTrim;
        }
        return !bodyTrim.isEmpty() ? bodyTrim.split("\r?\n")[0].trim() : null;
    }

    private String extraerBloqueDesde(String texto) {
        int inicio = texto.indexOf('[');
        int fin = texto.indexOf(']');
        if (inicio != -1 && fin > inicio) {
            return texto.substring(inicio, fin + 1).replaceAll("\\r?\\n", " ");
        }
        return texto.split("\r?\n")[0].trim();
    }

    /**
     * Construye una tabla ASCII legible y auto-dimensionada para respuestas tabulares.
     *
     * @param cabeceras Arreglo de nombres de columnas.
     * @param filas     Lista de filas con los datos.
     * @return Cadena con la tabla formateada en ASCII.
     */
    public static String construirTablaAscii(String[] cabeceras, List<String[]> filas) {
        if (filas == null || filas.isEmpty()) {
            return "La consulta no retornó registros.\n";
        }

        int columnas = cabeceras.length;
        int[] anchos = new int[columnas];

        for (int i = 0; i < columnas; i++) {
            anchos[i] = cabeceras[i].length();
        }

        for (String[] fila : filas) {
            for (int i = 0; i < columnas && i < fila.length; i++) {
                if (fila[i] != null && fila[i].length() > anchos[i]) {
                    anchos[i] = fila[i].length();
                }
            }
        }

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

        sb.append("|");
        for (int i = 0; i < columnas; i++) {
            sb.append(" ").append(padRight(cabeceras[i], anchos[i])).append(" |");
        }
        sb.append("\n");
        sb.append(separador);

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

    private String obtenerManualAyuda() {
        return "========================================================================\n"
                + "           MANUAL DE COMANDOS DEL SISTEMA INSTITUCIONAL FEDER           \n"
                + "========================================================================\n\n"
                + "FORMATO GENERAL:\n"
                + "  COMANDO [parametro1; parametro2; ...]\n\n"
                + "------------------------------------------------------------------------\n"
                + "CU-01: GESTIONAR USUARIOS Y ROLES (RF-14, RF-15)\n"
                + "------------------------------------------------------------------------\n"
                + "  INSUSR [nombre_completo; correo_electronico; contrasena; rol_id]\n"
                + "  MODUSR [id_usuario; nombre_completo; correo_electronico; estado; rol_id]\n"
                + "  DELUSR [id_usuario]\n"
                + "  LISUSR []\n"
                + "  * Roles: 1=Director, 2=Secretaria, 3=Instructor\n\n"
                + "------------------------------------------------------------------------\n"
                + "CU-02: GESTIONAR INFORMACIÓN INSTITUCIONAL Y REDES SOCIALES (RF-03, RF-04)\n"
                + "------------------------------------------------------------------------\n"
                + "  INSPAG [titulo; contenido]\n"
                + "  MODPAG [id_pagina; titulo; contenido]\n"
                + "  DELPAG [id_pagina]\n"
                + "  LISPAG []\n\n"
                + "  INSRED [plataforma; url_enlace]\n"
                + "  MODRED [id_red_social; plataforma; url_enlace; estado]\n"
                + "  DELRED [id_red_social]\n"
                + "  LISRED []\n\n"
                + "------------------------------------------------------------------------\n"
                + "AYUDA GENERAL:\n"
                + "  HELP\n"
                + "========================================================================";
    }
}
