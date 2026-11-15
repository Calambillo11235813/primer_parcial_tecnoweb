package uagrm.feder.Presentacion;

import uagrm.feder.Datos.DAporte;
import uagrm.feder.Datos.DCampana;
import uagrm.feder.Datos.DComunidad;
import uagrm.feder.Datos.DConsulta;
import uagrm.feder.Datos.DGaleriaMultimedia;
import uagrm.feder.Datos.DNoticia;
import uagrm.feder.Datos.DPaginaInstitucional;
import uagrm.feder.Datos.DProgramaProductivo;
import uagrm.feder.Datos.DRedSocial;
import uagrm.feder.Datos.DReporte;
import uagrm.feder.Datos.DTestimonio;
import uagrm.feder.Datos.DUsuario;
import uagrm.feder.Entity.Aporte;
import uagrm.feder.Entity.Consulta;
import uagrm.feder.Negocio.NAporte;
import uagrm.feder.Negocio.NCampana;
import uagrm.feder.Negocio.NComunidad;
import uagrm.feder.Negocio.NConsulta;
import uagrm.feder.Negocio.NInstitucional;
import uagrm.feder.Negocio.NPublicacion;
import uagrm.feder.Negocio.NReporte;
import uagrm.feder.Negocio.NUsuario;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Analizador Sintáctico determinista de comandos para el sistema FEDER.
 * Procesa las órdenes enviadas por correo electrónico (Asunto o Cuerpo)
 * y las despacha a la Capa de Negocio.
 */
public class CommandParser {

    private static final List<String> COMANDOS_VALIDOS = Arrays.asList(
            // CU-01: Usuarios y Roles
            "INSUSR", "MODUSR", "DELUSR", "LISUSR",
            // CU-02: Información Institucional y Redes Sociales
            "INSPAG", "MODPAG", "DELPAG", "LISPAG",
            "INSRED", "MODRED", "DELRED", "LISRED",
            // CU-03: Publicaciones y Medios (Noticias, Multimedia, Testimonios)
            "INSNOT", "MODNOT", "DELNOT", "LISNOT",
            "INSMUL", "MODMUL", "DELMUL", "LISMUL",
            "INSTES", "MODTES", "DELTES", "LISTES",
            // CU-04: Comunidades, Programas Productivos y Búsqueda/Filtrado
            "INSCOM", "MODCOM", "DELCOM", "LISCOM",
            "INSPRO", "MODPRO", "DELPRO", "LISPRO",
            "BUSCON",
            // CU-05: Campañas de Recaudación y Seguimiento Financiero
            "INSCMP", "MODCMP", "DELCMP", "LISCMP", "SEGCMP",
            // CU-06: Donaciones, Aportes y Fichas Técnicas de Proyectos
            "INSAPO", "MODAPO", "DELAPO", "LISAPO", "FICPRO",
            // CU-07: Bandeja de Consultas Ciudadanas
            "INSCON", "MODCON", "RESCON", "DELCON", "LISCON",
            // CU-08: Reportes y Estadísticas Ejecutivas
            "REPGEN", "REPCMP", "REPAPO", "REPCON", "REPAUD",
            // Ayuda general
            "HELP"
    );

    private final NUsuario nUsuario;
    private final NInstitucional nInstitucional;
    private final NPublicacion nPublicacion;
    private final NComunidad nComunidad;
    private final NCampana nCampana;
    private final NAporte nAporte;
    private final NConsulta nConsulta;
    private final NReporte nReporte;

    public CommandParser() {
        this.nUsuario = new NUsuario();
        this.nInstitucional = new NInstitucional();
        this.nPublicacion = new NPublicacion();
        this.nComunidad = new NComunidad();
        this.nCampana = new NCampana();
        this.nAporte = new NAporte();
        this.nConsulta = new NConsulta();
        this.nReporte = new NReporte();
    }

    public CommandParser(NUsuario nUsuario, NInstitucional nInstitucional,
            NPublicacion nPublicacion, NComunidad nComunidad,
            NCampana nCampana, NAporte nAporte, NConsulta nConsulta,
            NReporte nReporte) {
        this.nUsuario = nUsuario;
        this.nInstitucional = nInstitucional;
        this.nPublicacion = nPublicacion;
        this.nComunidad = nComunidad;
        this.nCampana = nCampana;
        this.nAporte = nAporte;
        this.nConsulta = nConsulta;
        this.nReporte = nReporte;
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

                // --- CU-03: Gestionar Noticias y Comunicados (RF-01) ---
                case "INSNOT":
                    return nPublicacion.registrarNoticia(parametros, remitente);
                case "MODNOT":
                    return nPublicacion.modificarNoticia(parametros, remitente);
                case "DELNOT":
                    return nPublicacion.eliminarNoticia(parametros, remitente);
                case "LISNOT":
                    return construirTablaAscii(
                            DNoticia.HEADERS, nPublicacion.listarNoticias()
                    );

                // --- CU-03: Gestionar Galería Multimedia (RF-02) ---
                case "INSMUL":
                    return nPublicacion.registrarMultimedia(parametros, remitente);
                case "MODMUL":
                    return nPublicacion.modificarMultimedia(parametros, remitente);
                case "DELMUL":
                    return nPublicacion.eliminarMultimedia(parametros, remitente);
                case "LISMUL":
                    return construirTablaAscii(
                            DGaleriaMultimedia.HEADERS, nPublicacion.listarMultimedia()
                    );

                // --- CU-03: Gestionar Testimonios (RF-08) ---
                case "INSTES":
                    return nPublicacion.registrarTestimonio(parametros, remitente);
                case "MODTES":
                    return nPublicacion.modificarTestimonio(parametros, remitente);
                case "DELTES":
                    return nPublicacion.eliminarTestimonio(parametros, remitente);
                case "LISTES":
                    return construirTablaAscii(
                            DTestimonio.HEADERS, nPublicacion.listarTestimonios()
                    );

                // --- CU-04: Gestionar Comunidades Beneficiarias (RF-05) ---
                case "INSCOM":
                    return nComunidad.registrarComunidad(parametros, remitente);
                case "MODCOM":
                    return nComunidad.modificarComunidad(parametros, remitente);
                case "DELCOM":
                    return nComunidad.eliminarComunidad(parametros, remitente);
                case "LISCOM":
                    return construirTablaAscii(
                            DComunidad.HEADERS, nComunidad.listarComunidades()
                    );

                // --- CU-04: Gestionar Programas Productivos (RF-06) ---
                case "INSPRO":
                    return nComunidad.registrarPrograma(parametros, remitente);
                case "MODPRO":
                    return nComunidad.modificarPrograma(parametros, remitente);
                case "DELPRO":
                    return nComunidad.eliminarPrograma(parametros, remitente);
                case "LISPRO":
                    return construirTablaAscii(
                            DProgramaProductivo.HEADERS, nComunidad.listarProgramas()
                    );

                // --- CU-04: Búsqueda y Filtrado de Contenidos (RF-07) ---
                case "BUSCON":
                    return construirTablaAscii(
                            DProgramaProductivo.HEADERS_BUSQUEDA,
                            nComunidad.buscarContenidos(parametros)
                    );

                // --- CU-05: Gestionar Campañas de Recaudación (RF-09 y RF-10) ---
                case "INSCMP":
                    return nCampana.registrarCampana(parametros, remitente);
                case "MODCMP":
                    return nCampana.modificarCampana(parametros, remitente);
                case "DELCMP":
                    return nCampana.cerrarCampana(parametros, remitente);
                case "LISCMP":
                    return construirTablaAscii(
                            DCampana.HEADERS,
                            nCampana.listarCampanas()
                    );
                case "SEGCMP":
                    return construirTablaAscii(
                            DCampana.HEADERS_SEGUIMIENTO,
                            nCampana.seguimientoRecaudacion(parametros)
                    );

                // --- CU-06: Gestionar Donaciones y Aportes (RF-11, RF-12 y RF-13) ---
                case "INSAPO":
                    return nAporte.registrarAporte(parametros, remitente);
                case "MODAPO":
                    return procesarVerificacionAporte(parametros, remitente);
                case "DELAPO":
                    return nAporte.anularAporte(parametros, remitente);
                case "LISAPO":
                    return construirTablaAscii(
                            DAporte.HEADERS,
                            nAporte.listarAportes(parametros, remitente)
                    );
                case "FICPRO":
                    return nAporte.descargarFichaProyecto(parametros);

                // --- CU-07: Gestionar Bandeja de Consultas (RF-16) ---
                case "INSCON":
                    return nConsulta.registrarConsulta(parametros, remitente);
                case "MODCON":
                    return nConsulta.clasificarEstado(parametros, remitente);
                case "RESCON":
                    return procesarRespuestaConsulta(parametros, remitente);
                case "DELCON":
                    return nConsulta.eliminarConsulta(parametros, remitente);
                case "LISCON":
                    return construirTablaAscii(
                            DConsulta.HEADERS,
                            nConsulta.listarConsultas(parametros, remitente)
                    );

                // --- CU-08: Generar Reportes y Estadísticas (RF-17) ---
                case "REPGEN":
                    return construirReporteEjecutivoIntegral(remitente);
                case "REPCMP":
                    return construirTablaAscii(
                            DReporte.HEADERS_CAMPANAS,
                            nReporte.reporteCampanas(remitente)
                    );
                case "REPAPO":
                    return construirTablaAscii(
                            DReporte.HEADERS_APORTES_MODALIDAD,
                            nReporte.reporteAportes(remitente)
                    );
                case "REPCON":
                    return construirTablaAscii(
                            DReporte.HEADERS_CONSULTAS_VOLUMEN,
                            nReporte.reporteConsultas(remitente)
                    );
                case "REPAUD":
                    return construirTablaAscii(
                            DReporte.HEADERS_AUDITORIA,
                            nReporte.reporteAuditoria(remitente)
                    );

                // --- Manual de Ayuda ---
                case "HELP":
                    return obtenerManualAyuda();

                default:
                    return "ERROR [404]: Comando desconocido '" + comando + "'.\n"
                            + "Envíe 'HELP' para consultar el catálogo de comandos y su sintaxis.";
            }
        } catch (IllegalArgumentException | SecurityException e) {
            return e.getMessage();
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

    /**
     * Construye el reporte ejecutivo integral (REPGEN - RF-17) combinando KPIs globales
     * con las tablas consolidadas de campañas, aportes por modalidad y volumen de consultas.
     */
    private String construirReporteEjecutivoIntegral(String remitente) {
        String kpis = nReporte.obtenerResumenEjecutivo(remitente);
        if (kpis.startsWith("ERROR")) {
            return kpis;
        }
        StringBuilder sb = new StringBuilder(kpis);
        sb.append("\n[4] AVANCE FINANCIERO DE METAS POR CAMPANA\n");
        sb.append(construirTablaAscii(
                DReporte.HEADERS_CAMPANAS, nReporte.reporteCampanas(remitente)
        ));
        sb.append("\n[5] DISTRIBUCION DE APORTES POR MODALIDAD DE PAGO\n");
        sb.append(construirTablaAscii(
                DReporte.HEADERS_APORTES_MODALIDAD, nReporte.reporteAportes(remitente)
        ));
        sb.append("\n[6] VOLUMEN DE CONSULTAS CIUDADANAS POR ESTADO\n");
        sb.append(construirTablaAscii(
                DReporte.HEADERS_CONSULTAS_VOLUMEN, nReporte.reporteConsultas(remitente)
        ));
        return sb.toString();
    }

    /**
     * Ejecuta MODAPO en la capa de negocio y, si el correo del donante difiere del operador,
     * despacha el comprobante digital de confirmación vía SMTP directamente al donante (RF-12).
     */
    private String procesarVerificacionAporte(List<String> parametros, String remitente) {
        String resultado = nAporte.verificarAporte(parametros, remitente);
        if (resultado.startsWith("SUCCESS:") && parametros.size() == 2) {
            try {
                int idAporte = Integer.parseInt(parametros.get(0).trim());
                Aporte aporte = nAporte.obtenerPorId(idAporte);
                if (aporte != null
                        && !aporte.getCorreo_donante().equalsIgnoreCase(remitente)) {
                    ClienteSMTP smtp = new ClienteSMTP();
                    String asuntoDonante = "Comprobante Digital de Aporte FEDER #APO-"
                            + String.format("%05d", idAporte);
                    smtp.enviarCorreo(aporte.getCorreo_donante(), asuntoDonante, resultado);
                }
            } catch (Exception ignored) {
                // Si el correo externo no puede enrutarse por ACL, se mantiene el registro en BD
            }
        }
        return resultado;
    }

    /**
     * Ejecuta RESCON en la capa de negocio y, si el correo del ciudadano difiere del operador,
     * despacha una notificación SMTP adicional directamente al ciudadano.
     */
    private String procesarRespuestaConsulta(List<String> parametros, String remitente) {
        String resultado = nConsulta.responderConsulta(parametros, remitente);
        if (resultado.startsWith("SUCCESS:") && parametros.size() == 2) {
            try {
                int idConsulta = Integer.parseInt(parametros.get(0).trim());
                Consulta consulta = nConsulta.obtenerPorId(idConsulta);
                if (consulta != null
                        && !consulta.getCorreo_remitente().equalsIgnoreCase(remitente)) {
                    ClienteSMTP smtp = new ClienteSMTP();
                    String asuntoCiudadano = "RE: Consulta FEDER - " + consulta.getAsunto();
                    smtp.enviarCorreo(
                            consulta.getCorreo_remitente(), asuntoCiudadano, resultado
                    );
                }
            } catch (Exception ignored) {
                // Si el correo externo no puede enrutarse por ACL, se mantiene el registro en BD
            }
        }
        return resultado;
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
                + "CU-03: GESTIONAR PUBLICACIONES Y MEDIOS (RF-01, RF-02, RF-08)\n"
                + "------------------------------------------------------------------------\n"
                + "  INSNOT [titulo; contenido; imagen_principal; etiquetas]\n"
                + "  MODNOT [id_noticia; titulo; contenido; imagen_principal; etiquetas; estado]\n"
                + "  DELNOT [id_noticia]\n"
                + "  LISNOT []\n\n"
                + "  INSMUL [titulo; tipo; url_archivo; descripcion; noticia_id]\n"
                + "  MODMUL [id_multimedia; titulo; tipo; url_archivo; descripcion; noticia_id]\n"
                + "  DELMUL [id_multimedia]\n"
                + "  LISMUL []\n\n"
                + "  INSTES [titulo; texto; fotografia; enlace_audiovisual; comunidad_id]\n"
                + "  MODTES [id_testimonio; titulo; texto; fotografia; enlace; estado; comunidad_id]\n"
                + "  DELTES [id_testimonio]\n"
                + "  LISTES []\n\n"
                + "------------------------------------------------------------------------\n"
                + "CU-04: GESTIONAR COMUNIDADES Y PROGRAMAS (RF-05, RF-06, RF-07)\n"
                + "------------------------------------------------------------------------\n"
                + "  INSCOM [nombre; descripcion; ubicacion]\n"
                + "  MODCOM [id_comunidad; nombre; descripcion; ubicacion; estado]\n"
                + "  DELCOM [id_comunidad]\n"
                + "  LISCOM []\n\n"
                + "  INSPRO [nombre; descripcion; area_productiva; poblacion_beneficiaria; comunidad_id]\n"
                + "  MODPRO [id_programa; nombre; descripcion; area; poblacion; comunidad_id]\n"
                + "  DELPRO [id_programa]\n"
                + "  LISPRO []\n\n"
                + "  BUSCON [criterio_comunidad_o_rubro]\n\n"
                + "------------------------------------------------------------------------\n"
                + "CU-05: GESTIONAR CAMPAÑAS DE RECAUDACIÓN (RF-09, RF-10)\n"
                + "------------------------------------------------------------------------\n"
                + "  INSCMP [titulo; descripcion; meta_economica; fecha_limite; comunidad_id]\n"
                + "  MODCMP [id_campana; titulo; descripcion; meta; fecha_limite; estado; comunidad_id]\n"
                + "  DELCMP [id_campana]  (Cierra la campana)\n"
                + "  LISCMP []\n"
                + "  SEGCMP []  o  SEGCMP [id_campana]\n\n"
                + "------------------------------------------------------------------------\n"
                + "CU-06: GESTIONAR DONACIONES Y APORTES (RF-11, RF-12, RF-13)\n"
                + "------------------------------------------------------------------------\n"
                + "  INSAPO [nombre_donante; monto; modalidad; campana_id]\n"
                + "  INSAPO [nombre_donante; correo_donante; monto; modalidad; campana_id]\n"
                + "  MODAPO [id_aporte; estado]\n"
                + "  DELAPO [id_aporte]\n"
                + "  LISAPO []  o  LISAPO [campana_id]\n"
                + "  FICPRO []  o  FICPRO [id_campana]\n"
                + "  * Modalidades: Transferencia bancaria, QR, Efectivo\n"
                + "  * Estados Aporte: Pendiente de Confirmacion, Confirmado, Rechazado\n\n"
                + "------------------------------------------------------------------------\n"
                + "CU-07: GESTIONAR BANDEJA DE CONSULTAS CIUDADANAS (RF-16)\n"
                + "------------------------------------------------------------------------\n"
                + "  INSCON [nombre_remitente; asunto; mensaje]\n"
                + "  INSCON [nombre_remitente; correo_remitente; asunto; mensaje]\n"
                + "  MODCON [id_consulta; estado]\n"
                + "  RESCON [id_consulta; respuesta_institucional]\n"
                + "  DELCON [id_consulta]\n"
                + "  LISCON []  o  LISCON [estado]\n"
                + "  * Estados: Pendiente, En Proceso, Atendido\n\n"
                + "------------------------------------------------------------------------\n"
                + "CU-08: GENERAR REPORTES Y ESTADISTICAS EJECUTIVAS (RF-17)\n"
                + "------------------------------------------------------------------------\n"
                + "  REPGEN []  (Reporte Ejecutivo Consolidado Integral)\n"
                + "  REPCMP []  (Reporte Estadistico de Avance Financiero de Campanas)\n"
                + "  REPAPO []  (Reporte de Aportes Agrupados por Modalidad y Estado)\n"
                + "  REPCON []  (Reporte de Volumen de Consultas por Estado de Atencion)\n"
                + "  REPAUD []  (Reporte de Bitacora y Auditoria de Acciones)\n\n"
                + "------------------------------------------------------------------------\n"
                + "AYUDA GENERAL:\n"
                + "  HELP\n"
                + "========================================================================";
    }
}
