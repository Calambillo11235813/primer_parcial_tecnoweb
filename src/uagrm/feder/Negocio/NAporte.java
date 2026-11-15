package uagrm.feder.Negocio;

import uagrm.feder.Datos.DAporte;
import uagrm.feder.Datos.DCampana;
import uagrm.feder.Datos.DUsuario;
import uagrm.feder.Entity.Aporte;
import uagrm.feder.Entity.Campana;
import uagrm.feder.Entity.Usuario;

import java.math.BigDecimal;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Capa de Negocio: NAporte.
 * Controla la lógica del CU-06: Gestionar Donaciones y Aportes
 * (RF-11: Registro de compromisos de aporte; RF-12: Confirmación de aporte y
 * emisión de comprobante digital con agradecimiento; RF-13: Descarga de fichas
 * técnicas de proyectos comunitarios).
 */
public class NAporte {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    private final DAporte dAporte;
    private final DCampana dCampana;
    private final DUsuario dUsuario;

    public NAporte() {
        this.dAporte = new DAporte();
        this.dCampana = new DCampana();
        this.dUsuario = new DUsuario();
    }

    /**
     * Verifica si el correo del remitente pertenece a un usuario activo con rol
     * 'Director' o 'Secretaria' (roles autorizados para verificar compromisos de aporte).
     *
     * @param senderEmail Correo electrónico del operador.
     * @return true si tiene permisos para verificar y listar aportes.
     */
    public boolean esAutorizadoAportes(String senderEmail) {
        if (senderEmail == null || senderEmail.trim().isEmpty()) {
            return false;
        }
        String rol = dUsuario.getRolByEmail(senderEmail.trim());
        return rol != null
                && (rol.equalsIgnoreCase("Director") || rol.equalsIgnoreCase("Secretaria"));
    }

    // =========================================================================
    // RF-11 y RF-12: REGISTRO DE COMPROMISO Y COMPROBANTE DIGITAL DE APORTE
    // =========================================================================

    /**
     * Registra un nuevo compromiso de donación y emite un comprobante digital
     * con mensaje de agradecimiento institucional (RF-11 y RF-12).
     * Soporta 4 parámetros:
     *   INSAPO [nombre_donante; monto; modalidad; campana_id]
     * o 5 parámetros:
     *   INSAPO [nombre_donante; correo_donante; monto; modalidad; campana_id]
     *
     * @param params      Lista con 4 o 5 parámetros.
     * @param senderEmail Correo electrónico del remitente.
     * @return Comprobante digital de registro o descripción del error.
     */
    public String registrarAporte(List<String> params, String senderEmail) {
        if (params == null || (params.size() != 4 && params.size() != 5)) {
            return "ERROR [400]: Cantidad de parámetros inválida para INSAPO. "
                    + "Uso: INSAPO [nombre_donante; monto; modalidad; campana_id] o "
                    + "INSAPO [nombre_donante; correo_donante; monto; modalidad; campana_id]";
        }

        String nombreDonante;
        String correoDonante;
        String montoStr;
        String modalidadRaw;
        String campanaIdStr;

        if (params.size() == 4) {
            nombreDonante = params.get(0) != null ? params.get(0).trim() : "";
            correoDonante = senderEmail != null ? senderEmail.trim() : "";
            montoStr = params.get(1) != null ? params.get(1).trim() : "";
            modalidadRaw = params.get(2) != null ? params.get(2).trim() : "";
            campanaIdStr = params.get(3) != null ? params.get(3).trim() : "";
        } else {
            nombreDonante = params.get(0) != null ? params.get(0).trim() : "";
            correoDonante = params.get(1) != null ? params.get(1).trim() : "";
            montoStr = params.get(2) != null ? params.get(2).trim() : "";
            modalidadRaw = params.get(3) != null ? params.get(3).trim() : "";
            campanaIdStr = params.get(4) != null ? params.get(4).trim() : "";
        }

        if (nombreDonante.isEmpty() || nombreDonante.length() > 150) {
            return "ERROR [422]: El nombre del donante no puede estar vacío "
                    + "ni exceder 150 caracteres.";
        }
        if (correoDonante.isEmpty() || correoDonante.length() > 150
                || !EMAIL_PATTERN.matcher(correoDonante).matches()) {
            return "ERROR [422]: El correo del donante '" + correoDonante
                    + "' no tiene un formato válido.";
        }

        BigDecimal monto;
        try {
            monto = new BigDecimal(montoStr);
            if (monto.compareTo(BigDecimal.ZERO) <= 0) {
                return "ERROR [422]: El monto del aporte debe ser mayor a 0.00 Bs.";
            }
        } catch (NumberFormatException e) {
            return "ERROR [422]: El monto '" + montoStr
                    + "' debe ser un valor numérico válido (ej. 250.00).";
        }

        String modalidadNormalizada = normalizarModalidad(modalidadRaw);
        if (modalidadNormalizada == null) {
            return "ERROR [422]: Modalidad de pago inválida ('" + modalidadRaw
                    + "'). Valores permitidos: 'Transferencia bancaria', 'QR' o 'Efectivo'.";
        }

        int campanaId;
        try {
            campanaId = Integer.parseInt(campanaIdStr);
        } catch (NumberFormatException e) {
            return "ERROR [422]: El ID de la campaña debe ser un número entero válido.";
        }

        Campana campana = dCampana.getById(campanaId);
        if (campana == null) {
            return "ERROR [404]: No existe ninguna campaña registrada con ID "
                    + campanaId + ".";
        }
        if (!campana.getEstado().equalsIgnoreCase("Activa")) {
            return "ERROR [409]: La campaña '" + campana.getTitulo()
                    + "' no se encuentra activa actualmente (Estado: "
                    + campana.getEstado() + ").";
        }

        int idGenerado = dAporte.insert(
                nombreDonante, correoDonante, monto, modalidadNormalizada, campanaId
        );
        if (idGenerado > 0) {
            return construirComprobanteDigital(
                    idGenerado, nombreDonante, correoDonante, monto,
                    modalidadNormalizada, "Pendiente de Confirmación",
                    campana.getTitulo(), null
            );
        }
        return "ERROR [500]: Fallo al registrar el compromiso de aporte en la base de datos.";
    }

    /**
     * Verifica y actualiza el estado de un compromiso de aporte ('Confirmado',
     * 'Pendiente de Confirmación' o 'Rechazado'), recalculando el avance financiero
     * de la campaña y emitiendo el comprobante oficial de confirmación (RF-12).
     * Comando: MODAPO [id_aporte; estado]
     *
     * @param params      Lista con [id_aporte, estado].
     * @param senderEmail Correo del operador (requiere rol 'Director' o 'Secretaria').
     * @return Comprobante oficial de confirmación o descripción del error.
     */
    public String verificarAporte(List<String> params, String senderEmail) {
        if (!esAutorizadoAportes(senderEmail)) {
            return "ERROR [403]: Acceso denegado. Solo los roles 'Director' y 'Secretaria' "
                    + "pueden verificar y confirmar aportes.";
        }

        if (params == null || params.size() != 2) {
            return "ERROR [400]: Cantidad de parámetros inválida para MODAPO. "
                    + "Se esperan 2: [id_aporte; estado]";
        }

        int idAporte;
        try {
            idAporte = Integer.parseInt(params.get(0).trim());
        } catch (NumberFormatException e) {
            return "ERROR [422]: El ID del aporte debe ser un número entero válido.";
        }

        String estadoRaw = params.get(1) != null ? params.get(1).trim() : "";
        String estadoNormalizado = normalizarEstadoAporte(estadoRaw);
        if (estadoNormalizado == null) {
            return "ERROR [422]: Estado inválido ('" + estadoRaw
                    + "'). Valores permitidos: 'Confirmado', "
                    + "'Pendiente de Confirmación' o 'Rechazado'.";
        }

        Aporte aporte = dAporte.getById(idAporte);
        if (aporte == null) {
            return "ERROR [404]: No existe ningún aporte registrado con ID " + idAporte + ".";
        }

        Campana campana = dCampana.getById(aporte.getCampana_id());
        String tituloCampana = campana != null
                ? campana.getTitulo() : "Campaña ID " + aporte.getCampana_id();

        Usuario operador = dUsuario.getByEmail(senderEmail);
        if (operador == null) {
            return "ERROR [401]: No se pudo identificar al operador '" + senderEmail + "'.";
        }

        boolean ok = dAporte.updateEstadoYSincronizarCampana(
                idAporte, aporte.getCampana_id(), estadoNormalizado,
                operador.getId_usuario()
        );
        if (ok) {
            return construirComprobanteDigital(
                    idAporte, aporte.getNombre_donante(), aporte.getCorreo_donante(),
                    aporte.getMonto(), aporte.getModalidad(), estadoNormalizado,
                    tituloCampana, operador.getNombre_completo()
            );
        }
        return "ERROR [500]: Fallo al actualizar y sincronizar el estado del aporte.";
    }

    /**
     * Anula o rechaza un aporte cambiando su estado a 'Rechazado' y recalculando
     * el monto recaudado de la campaña.
     * Comando: DELAPO [id_aporte]
     *
     * @param params      Lista con [id_aporte].
     * @param senderEmail Correo del operador (requiere rol 'Director' o 'Secretaria').
     * @return Mensaje de confirmación o descripción del error.
     */
    public String anularAporte(List<String> params, String senderEmail) {
        if (!esAutorizadoAportes(senderEmail)) {
            return "ERROR [403]: Acceso denegado. Solo los roles 'Director' y 'Secretaria' "
                    + "pueden anular registros de aportes.";
        }

        if (params == null || params.size() != 1) {
            return "ERROR [400]: Cantidad de parámetros inválida para DELAPO. "
                    + "Se espera 1: [id_aporte]";
        }

        int idAporte;
        try {
            idAporte = Integer.parseInt(params.get(0).trim());
        } catch (NumberFormatException e) {
            return "ERROR [422]: El ID del aporte debe ser un número entero válido.";
        }

        Aporte aporte = dAporte.getById(idAporte);
        if (aporte == null) {
            return "ERROR [404]: No existe ningún aporte registrado con ID " + idAporte + ".";
        }

        Usuario operador = dUsuario.getByEmail(senderEmail);
        if (operador == null) {
            return "ERROR [401]: No se pudo identificar al operador '" + senderEmail + "'.";
        }

        boolean ok = dAporte.updateEstadoYSincronizarCampana(
                idAporte, aporte.getCampana_id(), "Rechazado",
                operador.getId_usuario()
        );
        if (ok) {
            return "SUCCESS: Aporte ID " + idAporte + " del donante '"
                    + aporte.getNombre_donante()
                    + "' anulado exitosamente (Estado: Rechazado).";
        }
        return "ERROR [500]: Fallo al anular el aporte.";
    }

    /**
     * Lista todos los aportes registrados o los de una campaña específica.
     * Comando: LISAPO [] o LISAPO [campana_id]
     *
     * @param params      Lista vacía o con 1 parámetro [campana_id].
     * @param senderEmail Correo del operador (requiere rol 'Director' o 'Secretaria').
     * @return Lista de filas String[] con los aportes.
     */
    public List<String[]> listarAportes(List<String> params, String senderEmail) {
        if (!esAutorizadoAportes(senderEmail)) {
            throw new SecurityException(
                    "ERROR [403]: Acceso denegado. Solo los roles 'Director' y 'Secretaria' "
                    + "pueden consultar el listado administrativo de aportes."
            );
        }

        Integer campanaIdFiltro = null;
        if (params != null && !params.isEmpty() && !params.get(0).trim().isEmpty()) {
            try {
                campanaIdFiltro = Integer.parseInt(params.get(0).trim());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(
                        "ERROR [422]: El ID de campaña para LISAPO debe ser un número entero."
                );
            }
        }
        return dAporte.list(campanaIdFiltro);
    }

    // =========================================================================
    // RF-13: DESCARGA DE INFORMACIÓN Y FICHAS TÉCNICAS DE PROYECTOS
    // =========================================================================

    /**
     * Genera y retorna la ficha técnica digital de un proyecto/campaña comunitaria
     * o de todos los proyectos si no se envía parámetro.
     * Comando: FICPRO [] o FICPRO [id_campana]
     *
     * @param params Lista vacía o con 1 parámetro [id_campana].
     * @return Texto estructurado con la ficha técnica digital del proyecto.
     */
    public String descargarFichaProyecto(List<String> params) {
        Integer idCampana = null;
        if (params != null && !params.isEmpty() && !params.get(0).trim().isEmpty()) {
            try {
                idCampana = Integer.parseInt(params.get(0).trim());
            } catch (NumberFormatException e) {
                return "ERROR [422]: El ID de proyecto/campaña para FICPRO "
                        + "debe ser un número entero válido.";
            }
        }
        return dAporte.generarFichaTecnicaProyecto(idCampana);
    }

    /**
     * Obtiene un aporte por su ID para permitir notificación SMTP directa al donante.
     *
     * @param id Identificador del aporte.
     * @return Instancia de Aporte o null si no existe.
     */
    public Aporte obtenerPorId(int id) {
        return dAporte.getById(id);
    }

    private String construirComprobanteDigital(int idAporte, String donante,
            String correo, BigDecimal monto, String modalidad, String estado,
            String campana, String verificadoPor) {
        StringBuilder sb = new StringBuilder();
        sb.append("SUCCESS: Aporte procesado correctamente (Estado: ").append(estado).append(").\n");
        sb.append("====================================================================\n");
        sb.append("            COMPROBANTE DIGITAL DE APORTE - FUNDACION FEDER         \n");
        sb.append("====================================================================\n");
        sb.append("  Nro. Comprobante : APO-").append(String.format("%05d", idAporte)).append("\n");
        sb.append("  Donante          : ").append(donante).append("\n");
        sb.append("  Correo Donante   : ").append(correo).append("\n");
        sb.append("  Campana Destino  : ").append(campana).append("\n");
        sb.append("  Monto Aportado   : Bs. ").append(monto.setScale(2)).append("\n");
        sb.append("  Modalidad Pago   : ").append(modalidad).append("\n");
        sb.append("  Estado Aporte    : ").append(estado).append("\n");
        if (verificadoPor != null) {
            sb.append("  Verificado por   : ").append(verificadoPor).append("\n");
        }
        sb.append("--------------------------------------------------------------------\n");
        sb.append("  MENSAJE DE AGRADECIMIENTO INSTITUCIONAL:\n");
        sb.append("  La Fundacion Empresarial para el Desarrollo Rural (FEDER) y las\n");
        sb.append("  familias artesanas de la Chiquitania agradecen profundamente su\n");
        sb.append("  solidaridad. Su aporte impulsa el desarrollo productivo rural.\n");
        sb.append("====================================================================");
        return sb.toString();
    }

    private String normalizarModalidad(String modalidad) {
        if (modalidad == null) {
            return null;
        }
        if (modalidad.equalsIgnoreCase("Transferencia bancaria")
                || modalidad.equalsIgnoreCase("Transferencia")) {
            return "Transferencia bancaria";
        }
        if (modalidad.equalsIgnoreCase("QR") || modalidad.equalsIgnoreCase("Pago QR")) {
            return "QR";
        }
        if (modalidad.equalsIgnoreCase("Efectivo")) {
            return "Efectivo";
        }
        return null;
    }

    private String normalizarEstadoAporte(String estado) {
        if (estado == null) {
            return null;
        }
        if (estado.equalsIgnoreCase("Confirmado")
                || estado.equalsIgnoreCase("Confirmada")) {
            return "Confirmado";
        }
        if (estado.equalsIgnoreCase("Pendiente de Confirmación")
                || estado.equalsIgnoreCase("Pendiente de Confirmacion")
                || estado.equalsIgnoreCase("Pendiente")) {
            return "Pendiente de Confirmación";
        }
        if (estado.equalsIgnoreCase("Rechazado")
                || estado.equalsIgnoreCase("Rechazada")) {
            return "Rechazado";
        }
        return null;
    }
}
