package uagrm.feder.Negocio;

import uagrm.feder.Datos.DReporte;
import uagrm.feder.Datos.DUsuario;

import java.util.List;

/**
 * Capa de Negocio: NReporte.
 * Controla la lógica del CU-08: Generar Reportes y Estadísticas
 * (RF-17: Reportes estadísticos e indicadores consolidados sobre montos recaudados,
 * avance financiero de metas, registro de aportes y volumen de consultas atendidas).
 */
public class NReporte {

    private final DReporte dReporte;
    private final DUsuario dUsuario;

    public NReporte() {
        this.dReporte = new DReporte();
        this.dUsuario = new DUsuario();
    }

    /**
     * Verifica si el correo del remitente pertenece a un usuario activo con rol 'Director'.
     *
     * @param senderEmail Correo electrónico del remitente.
     * @return true si posee rol 'Director'.
     */
    public boolean esDirector(String senderEmail) {
        if (senderEmail == null || senderEmail.trim().isEmpty()) {
            return false;
        }
        String rol = dUsuario.getRolByEmail(senderEmail.trim());
        return rol != null && rol.equalsIgnoreCase("Director");
    }

    /**
     * Retorna los indicadores ejecutivos consolidados de todo el sistema (RF-17).
     * Comando: REPGEN []
     *
     * @param senderEmail Correo del operador (requiere rol 'Director').
     * @return Bloque de texto con KPIs ejecutivos o mensaje de error.
     */
    public String obtenerResumenEjecutivo(String senderEmail) {
        if (!esDirector(senderEmail)) {
            return "ERROR [403]: Acceso denegado. Solo el rol 'Director' "
                    + "puede generar y visualizar reportes ejecutivos y estadísticas.";
        }
        return dReporte.obtenerResumenIndicadoresEjecutivos();
    }

    /**
     * Retorna la tabla analítica de avance financiero de metas por campaña (RF-17).
     * Comando: REPCMP []
     *
     * @param senderEmail Correo del operador (requiere rol 'Director').
     * @return Lista de filas String[] alineadas con DReporte.HEADERS_CAMPANAS.
     */
    public List<String[]> reporteCampanas(String senderEmail) {
        if (!esDirector(senderEmail)) {
            throw new SecurityException(
                    "ERROR [403]: Acceso denegado. Solo el rol 'Director' "
                    + "puede consultar el reporte estadístico de campañas."
            );
        }
        return dReporte.reporteAvanceCampanas();
    }

    /**
     * Retorna la tabla estadística de aportes agrupados por modalidad y estado (RF-17).
     * Comando: REPAPO []
     *
     * @param senderEmail Correo del operador (requiere rol 'Director').
     * @return Lista de filas String[] alineadas con DReporte.HEADERS_APORTES_MODALIDAD.
     */
    public List<String[]> reporteAportes(String senderEmail) {
        if (!esDirector(senderEmail)) {
            throw new SecurityException(
                    "ERROR [403]: Acceso denegado. Solo el rol 'Director' "
                    + "puede consultar el reporte estadístico de aportes."
            );
        }
        return dReporte.reporteAportesPorModalidad();
    }

    /**
     * Retorna la tabla estadística de volumen de consultas ciudadanas por estado (RF-17).
     * Comando: REPCON []
     *
     * @param senderEmail Correo del operador (requiere rol 'Director').
     * @return Lista de filas String[] alineadas con DReporte.HEADERS_CONSULTAS_VOLUMEN.
     */
    public List<String[]> reporteConsultas(String senderEmail) {
        if (!esDirector(senderEmail)) {
            throw new SecurityException(
                    "ERROR [403]: Acceso denegado. Solo el rol 'Director' "
                    + "puede consultar el reporte estadístico de consultas."
            );
        }
        return dReporte.reporteVolumenConsultas();
    }

    /**
     * Retorna el reporte de las últimas operaciones registradas en AUDITORIA.
     * Comando: REPAUD []
     *
     * @param senderEmail Correo del operador (requiere rol 'Director').
     * @return Lista de filas String[] alineadas con DReporte.HEADERS_AUDITORIA.
     */
    public List<String[]> reporteAuditoria(String senderEmail) {
        if (!esDirector(senderEmail)) {
            throw new SecurityException(
                    "ERROR [403]: Acceso denegado. Solo el rol 'Director' "
                    + "puede consultar la bitácora de auditoría del sistema."
            );
        }
        return dReporte.reporteAuditoria();
    }
}
