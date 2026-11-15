package uagrm.feder.Negocio;

import uagrm.feder.Datos.DCampana;
import uagrm.feder.Datos.DComunidad;
import uagrm.feder.Datos.DUsuario;
import uagrm.feder.Entity.Campana;
import uagrm.feder.Entity.Comunidad;
import uagrm.feder.Entity.Usuario;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Capa de Negocio: NCampana.
 * Controla la lógica del CU-05: Gestionar Campañas de Recaudación
 * (RF-09: Creación, edición y cierre de campañas; RF-10: Seguimiento de recaudación
 * calculado exclusivamente sobre aportes confirmados).
 */
public class NCampana {

    private final DCampana dCampana;
    private final DComunidad dComunidad;
    private final DUsuario dUsuario;

    public NCampana() {
        this.dCampana = new DCampana();
        this.dComunidad = new DComunidad();
        this.dUsuario = new DUsuario();
    }

    /**
     * Verifica si el correo del remitente pertenece a un usuario activo con rol 'Director'.
     *
     * @param senderEmail Correo electrónico del remitente.
     * @return true si el remitente posee rol 'Director'.
     */
    public boolean esDirector(String senderEmail) {
        if (senderEmail == null || senderEmail.trim().isEmpty()) {
            return false;
        }
        String rol = dUsuario.getRolByEmail(senderEmail.trim());
        return rol != null && rol.equalsIgnoreCase("Director");
    }

    // =========================================================================
    // RF-09: GESTIÓN DE CAMPAÑAS DE FINANCIAMIENTO (CAMPANA)
    // =========================================================================

    /**
     * Crea una nueva campaña de financiamiento con meta económica y plazo delimitado.
     * Soporta 5 parámetros:
     *   INSCMP [titulo; descripcion; meta_economica; fecha_limite; comunidad_id]
     * o 6 parámetros:
     *   INSCMP [titulo; descripcion; meta_economica; fecha_inicio; fecha_limite; comunidad_id]
     *
     * @param params      Lista con 5 o 6 parámetros.
     * @param senderEmail Correo del remitente (requiere rol 'Director').
     * @return Mensaje de confirmación o descripción del error.
     */
    public String registrarCampana(List<String> params, String senderEmail) {
        if (!esDirector(senderEmail)) {
            return "ERROR [403]: Acceso denegado. Solo el rol 'Director' "
                    + "puede crear campañas de recaudación.";
        }

        if (params == null || (params.size() != 5 && params.size() != 6)) {
            return "ERROR [400]: Cantidad de parámetros inválida para INSCMP. "
                    + "Se esperan 5: [titulo; descripcion; meta_economica; "
                    + "fecha_limite(YYYY-MM-DD); comunidad_id] o 6 incluyendo fecha_inicio.";
        }

        String titulo = params.get(0) != null ? params.get(0).trim() : "";
        String descripcion = params.get(1) != null ? params.get(1).trim() : "";
        String metaStr = params.get(2) != null ? params.get(2).trim() : "";

        if (titulo.isEmpty() || titulo.length() > 200) {
            return "ERROR [422]: El título de la campaña no puede estar vacío "
                    + "ni exceder 200 caracteres.";
        }
        if (descripcion.isEmpty()) {
            return "ERROR [422]: La descripción de la campaña no puede estar vacía.";
        }

        BigDecimal metaEconomica;
        try {
            metaEconomica = new BigDecimal(metaStr);
            if (metaEconomica.compareTo(BigDecimal.ZERO) <= 0) {
                return "ERROR [422]: La meta económica debe ser mayor a 0.00 Bs.";
            }
        } catch (NumberFormatException e) {
            return "ERROR [422]: La meta económica '" + metaStr
                    + "' debe ser un valor numérico válido (ej. 15000.00).";
        }

        LocalDate fechaInicioLocal;
        LocalDate fechaLimiteLocal;
        int comunidadId;

        if (params.size() == 5) {
            fechaInicioLocal = LocalDate.now();
            try {
                fechaLimiteLocal = LocalDate.parse(params.get(3).trim());
            } catch (DateTimeParseException e) {
                return "ERROR [422]: La fecha límite '" + params.get(3).trim()
                        + "' debe tener formato YYYY-MM-DD (ej. 2026-12-31).";
            }
            try {
                comunidadId = Integer.parseInt(params.get(4).trim());
            } catch (NumberFormatException e) {
                return "ERROR [422]: El ID de la comunidad debe ser un número entero.";
            }
        } else {
            try {
                fechaInicioLocal = LocalDate.parse(params.get(3).trim());
                fechaLimiteLocal = LocalDate.parse(params.get(4).trim());
            } catch (DateTimeParseException e) {
                return "ERROR [422]: Las fechas (fecha_inicio y fecha_limite) "
                        + "deben tener formato YYYY-MM-DD.";
            }
            try {
                comunidadId = Integer.parseInt(params.get(5).trim());
            } catch (NumberFormatException e) {
                return "ERROR [422]: El ID de la comunidad debe ser un número entero.";
            }
        }

        if (fechaLimiteLocal.isBefore(fechaInicioLocal)) {
            return "ERROR [422]: La fecha límite (" + fechaLimiteLocal
                    + ") no puede ser anterior a la fecha de inicio ("
                    + fechaInicioLocal + ").";
        }

        Comunidad comunidad = dComunidad.getById(comunidadId);
        if (comunidad == null) {
            return "ERROR [404]: No existe ninguna comunidad registrada con ID "
                    + comunidadId + ".";
        }

        Usuario operador = dUsuario.getByEmail(senderEmail);
        if (operador == null) {
            return "ERROR [401]: No se pudo identificar al operador '" + senderEmail + "'.";
        }

        boolean ok = dCampana.insert(
                titulo, descripcion, metaEconomica,
                Date.valueOf(fechaInicioLocal), Date.valueOf(fechaLimiteLocal),
                comunidadId, operador.getId_usuario()
        );
        if (ok) {
            return "SUCCESS: Campaña de recaudación '" + titulo + "' creada para '"
                    + comunidad.getNombre() + "' (Meta: Bs. " + metaEconomica
                    + " | Fecha límite: " + fechaLimiteLocal + ").";
        }
        return "ERROR [500]: Fallo al registrar la campaña en la base de datos.";
    }

    /**
     * Modifica los datos, meta económica, fecha límite o estado de una campaña.
     * Comando: MODCMP [id_campana; titulo; descripcion; meta_economica; fecha_limite; estado; comunidad_id]
     *
     * @param params      Lista con los 7 parámetros de modificación.
     * @param senderEmail Correo del remitente (requiere rol 'Director').
     * @return Mensaje de confirmación o descripción del error.
     */
    public String modificarCampana(List<String> params, String senderEmail) {
        if (!esDirector(senderEmail)) {
            return "ERROR [403]: Acceso denegado. Solo el rol 'Director' "
                    + "puede editar campañas de recaudación.";
        }

        if (params == null || params.size() != 7) {
            return "ERROR [400]: Cantidad de parámetros inválida para MODCMP. Se esperan 7: "
                    + "[id_campana; titulo; descripcion; meta_economica; "
                    + "fecha_limite(YYYY-MM-DD); estado; comunidad_id]";
        }

        int id;
        int comunidadId;
        try {
            id = Integer.parseInt(params.get(0).trim());
            comunidadId = Integer.parseInt(params.get(6).trim());
        } catch (NumberFormatException e) {
            return "ERROR [422]: Los identificadores (id_campana y comunidad_id) "
                    + "deben ser números enteros válidos.";
        }

        String titulo = params.get(1) != null ? params.get(1).trim() : "";
        String descripcion = params.get(2) != null ? params.get(2).trim() : "";
        String metaStr = params.get(3) != null ? params.get(3).trim() : "";
        String fechaLimStr = params.get(4) != null ? params.get(4).trim() : "";
        String estadoRaw = params.get(5) != null ? params.get(5).trim() : "";

        if (titulo.isEmpty() || titulo.length() > 200) {
            return "ERROR [422]: El título no puede estar vacío ni exceder 200 caracteres.";
        }
        if (descripcion.isEmpty()) {
            return "ERROR [422]: La descripción no puede estar vacía.";
        }

        BigDecimal metaEconomica;
        try {
            metaEconomica = new BigDecimal(metaStr);
            if (metaEconomica.compareTo(BigDecimal.ZERO) <= 0) {
                return "ERROR [422]: La meta económica debe ser mayor a 0.00 Bs.";
            }
        } catch (NumberFormatException e) {
            return "ERROR [422]: La meta económica '" + metaStr
                    + "' debe ser un número válido.";
        }

        LocalDate fechaLimiteLocal;
        try {
            fechaLimiteLocal = LocalDate.parse(fechaLimStr);
        } catch (DateTimeParseException e) {
            return "ERROR [422]: La fecha límite '" + fechaLimStr
                    + "' debe tener formato YYYY-MM-DD.";
        }

        String estadoNormalizado = normalizarEstadoCampana(estadoRaw);
        if (estadoNormalizado == null) {
            return "ERROR [422]: Estado inválido ('" + estadoRaw
                    + "'). Valores permitidos: 'Activa', 'Cerrada', 'Finalizada' o 'Cancelada'.";
        }

        Campana actual = dCampana.getById(id);
        if (actual == null) {
            return "ERROR [404]: No existe ninguna campaña registrada con ID " + id + ".";
        }

        LocalDate fechaInicioOriginal = actual.getFecha_inicio().toLocalDate();
        if (fechaLimiteLocal.isBefore(fechaInicioOriginal)) {
            return "ERROR [422]: La fecha límite (" + fechaLimiteLocal
                    + ") no puede ser anterior a la fecha de inicio de la campaña ("
                    + fechaInicioOriginal + ").";
        }

        if (dComunidad.getById(comunidadId) == null) {
            return "ERROR [404]: No existe ninguna comunidad registrada con ID "
                    + comunidadId + ".";
        }

        Usuario operador = dUsuario.getByEmail(senderEmail);
        if (operador == null) {
            return "ERROR [401]: No se pudo identificar al operador '" + senderEmail + "'.";
        }

        boolean ok = dCampana.update(
                id, titulo, descripcion, metaEconomica, Date.valueOf(fechaLimiteLocal),
                estadoNormalizado, comunidadId, operador.getId_usuario()
        );
        if (ok) {
            return "SUCCESS: Campaña ID " + id + " ('" + titulo
                    + "') actualizada exitosamente (Estado: " + estadoNormalizado + ").";
        }
        return "ERROR [500]: Fallo al actualizar la campaña.";
    }

    /**
     * Cierra formalmente una campaña de financiamiento cambiando su estado a 'Cerrada'.
     * Comando: DELCMP [id_campana]
     *
     * @param params      Lista con [id_campana].
     * @param senderEmail Correo del remitente (requiere rol 'Director').
     * @return Mensaje de confirmación o descripción del error.
     */
    public String cerrarCampana(List<String> params, String senderEmail) {
        if (!esDirector(senderEmail)) {
            return "ERROR [403]: Acceso denegado. Solo el rol 'Director' "
                    + "puede cerrar campañas de recaudación.";
        }

        if (params == null || params.size() != 1) {
            return "ERROR [400]: Cantidad de parámetros inválida para DELCMP. "
                    + "Se espera 1: [id_campana]";
        }

        int id;
        try {
            id = Integer.parseInt(params.get(0).trim());
        } catch (NumberFormatException e) {
            return "ERROR [422]: El ID de la campaña debe ser un número entero válido.";
        }

        Campana campana = dCampana.getById(id);
        if (campana == null) {
            return "ERROR [404]: No existe ninguna campaña registrada con ID " + id + ".";
        }

        Usuario operador = dUsuario.getByEmail(senderEmail);
        if (operador == null) {
            return "ERROR [401]: No se pudo identificar al operador '" + senderEmail + "'.";
        }

        boolean ok = dCampana.close(id, operador.getId_usuario());
        if (ok) {
            return "SUCCESS: Campaña ID " + id + " ('" + campana.getTitulo()
                    + "') cerrada oficialmente (Estado: Cerrada).";
        }
        return "ERROR [500]: Fallo al cerrar la campaña.";
    }

    /**
     * Lista todas las campañas registradas con su avance de recaudación confirmada.
     * Comando: LISCMP []
     *
     * @return Lista de filas String[] con las campañas.
     */
    public List<String[]> listarCampanas() {
        return dCampana.list();
    }

    // =========================================================================
    // RF-10: SEGUIMIENTO DE RECAUDACIÓN SOBRE APORTES CONFIRMADOS
    // =========================================================================

    /**
     * Calcula y retorna el cuadro de seguimiento de recaudación y porcentaje de avance
     * tomando como base exclusivamente los aportes confirmados.
     * Comando: SEGCMP [] o SEGCMP [id_campana]
     *
     * @param params Lista vacía o con 1 parámetro [id_campana].
     * @return Lista de filas String[] alineadas con DCampana.HEADERS_SEGUIMIENTO.
     */
    public List<String[]> seguimientoRecaudacion(List<String> params) {
        Integer idCampanaFiltro = null;
        if (params != null && !params.isEmpty() && !params.get(0).trim().isEmpty()) {
            try {
                idCampanaFiltro = Integer.parseInt(params.get(0).trim());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(
                        "ERROR [422]: El ID de campaña para SEGCMP debe ser un número entero."
                );
            }
            if (dCampana.getById(idCampanaFiltro) == null) {
                throw new IllegalArgumentException(
                        "ERROR [404]: No existe ninguna campaña con ID "
                        + idCampanaFiltro + "."
                );
            }
        }
        return dCampana.obtenerSeguimientoRecaudacion(idCampanaFiltro);
    }

    private String normalizarEstadoCampana(String estado) {
        if (estado == null) {
            return null;
        }
        if (estado.equalsIgnoreCase("Activa") || estado.equalsIgnoreCase("Activo")) {
            return "Activa";
        }
        if (estado.equalsIgnoreCase("Cerrada") || estado.equalsIgnoreCase("Cerrado")) {
            return "Cerrada";
        }
        if (estado.equalsIgnoreCase("Finalizada") || estado.equalsIgnoreCase("Finalizado")) {
            return "Finalizada";
        }
        if (estado.equalsIgnoreCase("Cancelada") || estado.equalsIgnoreCase("Cancelado")) {
            return "Cancelada";
        }
        return null;
    }
}
