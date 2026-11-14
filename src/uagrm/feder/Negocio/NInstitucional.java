package uagrm.feder.Negocio;

import uagrm.feder.Datos.DPaginaInstitucional;
import uagrm.feder.Datos.DRedSocial;
import uagrm.feder.Datos.DUsuario;
import uagrm.feder.Entity.PaginaInstitucional;
import uagrm.feder.Entity.RedSocial;
import uagrm.feder.Entity.Usuario;

import java.util.List;

/**
 * Capa de Negocio: NInstitucional.
 * Controla la lógica del CU-02: Gestionar Información Institucional
 * (RF-03: Páginas Institucionales y RF-04: Redes Sociales).
 */
public class NInstitucional {

    private final DPaginaInstitucional dPagina;
    private final DRedSocial dRedSocial;
    private final DUsuario dUsuario;

    public NInstitucional() {
        this.dPagina = new DPaginaInstitucional();
        this.dRedSocial = new DRedSocial();
        this.dUsuario = new DUsuario();
    }

    /**
     * Verifica si el correo remitente corresponde a un usuario activo con el rol requerido.
     *
     * @param senderEmail  Correo del remitente.
     * @param rolRequerido Rol exigido (ej. 'Director').
     * @return true si cumple con el rol requerido.
     */
    public boolean verificarRemitente(String senderEmail, String rolRequerido) {
        if (senderEmail == null || senderEmail.trim().isEmpty()) {
            return false;
        }
        String rolActual = dUsuario.getRolByEmail(senderEmail.trim());
        return rolActual != null && rolActual.equalsIgnoreCase(rolRequerido);
    }

    // =========================================================================
    // RF-03: GESTIÓN DE INFORMACIÓN INSTITUCIONAL (PAGINA_INSTITUCIONAL)
    // =========================================================================

    /**
     * Registra una nueva sección institucional (Misión, Visión, Reseña, Objetivos, Contacto).
     * Comando: INSPAG [titulo; contenido]
     *
     * @param params      Lista con [titulo, contenido].
     * @param senderEmail Correo del remitente (requiere rol 'Director').
     * @return Mensaje de éxito o descripción del error.
     */
    public String registrarPagina(List<String> params, String senderEmail) {
        if (!verificarRemitente(senderEmail, "Director")) {
            return "ERROR [403]: Acceso denegado. Solo el rol 'Director' "
                    + "puede registrar información institucional.";
        }

        if (params == null || params.size() != 2) {
            return "ERROR [400]: Cantidad de parámetros inválida para INSPAG. "
                    + "Se esperan 2: [titulo; contenido]";
        }

        String titulo = params.get(0) != null ? params.get(0).trim() : "";
        String contenido = params.get(1) != null ? params.get(1).trim() : "";

        if (titulo.isEmpty() || titulo.length() > 100) {
            return "ERROR [422]: El título no puede estar vacío ni exceder 100 caracteres.";
        }
        if (contenido.isEmpty()) {
            return "ERROR [422]: El contenido institucional no puede estar vacío.";
        }

        if (dPagina.getByTitulo(titulo) != null) {
            return "ERROR [409]: Ya existe una página institucional con el título '"
                    + titulo + "'. Use MODPAG para actualizarla.";
        }

        Usuario operador = dUsuario.getByEmail(senderEmail);
        if (operador == null) {
            return "ERROR [401]: No se pudo identificar al operador '" + senderEmail + "'.";
        }

        boolean ok = dPagina.insert(titulo, contenido, operador.getId_usuario());
        if (ok) {
            return "SUCCESS: Sección institucional '" + titulo + "' registrada exitosamente.";
        }
        return "ERROR [500]: Fallo al registrar la página institucional en la base de datos.";
    }

    /**
     * Actualiza una sección de información institucional existente.
     * Comando: MODPAG [id_pagina; titulo; contenido]
     *
     * @param params      Lista con [id_pagina, titulo, contenido].
     * @param senderEmail Correo del remitente (requiere rol 'Director').
     * @return Mensaje de éxito o descripción del error.
     */
    public String modificarPagina(List<String> params, String senderEmail) {
        if (!verificarRemitente(senderEmail, "Director")) {
            return "ERROR [403]: Acceso denegado. Solo el rol 'Director' "
                    + "puede modificar información institucional.";
        }

        if (params == null || params.size() != 3) {
            return "ERROR [400]: Cantidad de parámetros inválida para MODPAG. "
                    + "Se esperan 3: [id_pagina; titulo; contenido]";
        }

        int id;
        try {
            id = Integer.parseInt(params.get(0).trim());
        } catch (NumberFormatException e) {
            return "ERROR [422]: El ID de página debe ser un número entero válido.";
        }

        String titulo = params.get(1) != null ? params.get(1).trim() : "";
        String contenido = params.get(2) != null ? params.get(2).trim() : "";

        if (titulo.isEmpty() || titulo.length() > 100) {
            return "ERROR [422]: El título no puede estar vacío ni exceder 100 caracteres.";
        }
        if (contenido.isEmpty()) {
            return "ERROR [422]: El contenido institucional no puede estar vacío.";
        }

        if (dPagina.getById(id) == null) {
            return "ERROR [404]: No existe ninguna página institucional con ID " + id + ".";
        }

        PaginaInstitucional existenteTitulo = dPagina.getByTitulo(titulo);
        if (existenteTitulo != null && existenteTitulo.getId_pagina() != id) {
            return "ERROR [409]: El título '" + titulo + "' ya está asignado a otra página (ID "
                    + existenteTitulo.getId_pagina() + ").";
        }

        Usuario operador = dUsuario.getByEmail(senderEmail);
        if (operador == null) {
            return "ERROR [401]: No se pudo identificar al operador '" + senderEmail + "'.";
        }

        boolean ok = dPagina.update(id, titulo, contenido, operador.getId_usuario());
        if (ok) {
            return "SUCCESS: Página institucional ID " + id
                    + " ('" + titulo + "') actualizada exitosamente.";
        }
        return "ERROR [500]: Fallo al actualizar la página institucional.";
    }

    /**
     * Elimina una sección institucional.
     * Comando: DELPAG [id_pagina]
     *
     * @param params      Lista con [id_pagina].
     * @param senderEmail Correo del remitente (requiere rol 'Director').
     * @return Mensaje de éxito o descripción del error.
     */
    public String eliminarPagina(List<String> params, String senderEmail) {
        if (!verificarRemitente(senderEmail, "Director")) {
            return "ERROR [403]: Acceso denegado. Solo el rol 'Director' "
                    + "puede eliminar secciones institucionales.";
        }

        if (params == null || params.size() != 1) {
            return "ERROR [400]: Cantidad de parámetros inválida para DELPAG. "
                    + "Se espera 1: [id_pagina]";
        }

        int id;
        try {
            id = Integer.parseInt(params.get(0).trim());
        } catch (NumberFormatException e) {
            return "ERROR [422]: El ID de página debe ser un número entero válido.";
        }

        PaginaInstitucional pagina = dPagina.getById(id);
        if (pagina == null) {
            return "ERROR [404]: No existe ninguna página institucional con ID " + id + ".";
        }

        Usuario operador = dUsuario.getByEmail(senderEmail);
        if (operador == null) {
            return "ERROR [401]: No se pudo identificar al operador '" + senderEmail + "'.";
        }

        boolean ok = dPagina.delete(id, operador.getId_usuario());
        if (ok) {
            return "SUCCESS: Página institucional ID " + id
                    + " ('" + pagina.getTitulo() + "') eliminada exitosamente.";
        }
        return "ERROR [500]: Fallo al eliminar la página institucional.";
    }

    /**
     * Lista todas las secciones de información institucional (consulta pública/general).
     * Comando: LISPAG []
     *
     * @return Lista de filas String[] con las páginas institucionales.
     */
    public List<String[]> listarPaginas() {
        return dPagina.list();
    }

    // =========================================================================
    // RF-04: GESTIÓN DE REDES SOCIALES (RED_SOCIAL)
    // =========================================================================

    /**
     * Registra un nuevo enlace oficial de red social.
     * Comando: INSRED [plataforma; url_enlace]
     *
     * @param params      Lista con [plataforma, url_enlace].
     * @param senderEmail Correo del remitente (requiere rol 'Director').
     * @return Mensaje de éxito o descripción del error.
     */
    public String registrarRedSocial(List<String> params, String senderEmail) {
        if (!verificarRemitente(senderEmail, "Director")) {
            return "ERROR [403]: Acceso denegado. Solo el rol 'Director' "
                    + "puede registrar enlaces de redes sociales.";
        }

        if (params == null || params.size() != 2) {
            return "ERROR [400]: Cantidad de parámetros inválida para INSRED. "
                    + "Se esperan 2: [plataforma; url_enlace]";
        }

        String plataforma = params.get(0) != null ? params.get(0).trim() : "";
        String url = params.get(1) != null ? params.get(1).trim() : "";

        if (plataforma.isEmpty() || plataforma.length() > 50) {
            return "ERROR [422]: El nombre de la plataforma no puede estar vacío.";
        }
        if (!esUrlValida(url)) {
            return "ERROR [422]: El enlace URL '" + url
                    + "' no es válido. Debe iniciar con http:// o https://";
        }

        Usuario operador = dUsuario.getByEmail(senderEmail);
        if (operador == null) {
            return "ERROR [401]: No se pudo identificar al operador '" + senderEmail + "'.";
        }

        boolean ok = dRedSocial.insert(plataforma, url, operador.getId_usuario());
        if (ok) {
            return "SUCCESS: Red social '" + plataforma + "' (" + url
                    + ") registrada exitosamente.";
        }
        return "ERROR [500]: Fallo al registrar la red social en la base de datos.";
    }

    /**
     * Modifica un enlace oficial de red social existente.
     * Comando: MODRED [id_red_social; plataforma; url_enlace; estado]
     *
     * @param params      Lista con [id_red_social, plataforma, url_enlace, estado].
     * @param senderEmail Correo del remitente (requiere rol 'Director').
     * @return Mensaje de éxito o descripción del error.
     */
    public String modificarRedSocial(List<String> params, String senderEmail) {
        if (!verificarRemitente(senderEmail, "Director")) {
            return "ERROR [403]: Acceso denegado. Solo el rol 'Director' "
                    + "puede modificar enlaces de redes sociales.";
        }

        if (params == null || params.size() != 4) {
            return "ERROR [400]: Cantidad de parámetros inválida para MODRED. "
                    + "Se esperan 4: [id_red_social; plataforma; url_enlace; estado]";
        }

        int id;
        try {
            id = Integer.parseInt(params.get(0).trim());
        } catch (NumberFormatException e) {
            return "ERROR [422]: El ID de red social debe ser un número entero válido.";
        }

        String plataforma = params.get(1) != null ? params.get(1).trim() : "";
        String url = params.get(2) != null ? params.get(2).trim() : "";
        String estado = params.get(3) != null ? params.get(3).trim() : "";

        if (plataforma.isEmpty()) {
            return "ERROR [422]: El nombre de la plataforma no puede estar vacío.";
        }
        if (!esUrlValida(url)) {
            return "ERROR [422]: El enlace URL '" + url
                    + "' no es válido. Debe iniciar con http:// o https://";
        }
        if (!estado.equalsIgnoreCase("Activo") && !estado.equalsIgnoreCase("Inactivo")) {
            return "ERROR [422]: Estado inválido ('" + estado
                    + "'). Solo se admite 'Activo' o 'Inactivo'.";
        }
        estado = estado.equalsIgnoreCase("Activo") ? "Activo" : "Inactivo";

        if (dRedSocial.getById(id) == null) {
            return "ERROR [404]: No existe ninguna red social con ID " + id + ".";
        }

        Usuario operador = dUsuario.getByEmail(senderEmail);
        if (operador == null) {
            return "ERROR [401]: No se pudo identificar al operador '" + senderEmail + "'.";
        }

        boolean ok = dRedSocial.update(
                id, plataforma, url, estado, operador.getId_usuario()
        );
        if (ok) {
            return "SUCCESS: Red social ID " + id + " ('" + plataforma
                    + "') actualizada exitosamente.";
        }
        return "ERROR [500]: Fallo al actualizar la red social.";
    }

    /**
     * Realiza la baja lógica de una red social.
     * Comando: DELRED [id_red_social]
     *
     * @param params      Lista con [id_red_social].
     * @param senderEmail Correo del remitente (requiere rol 'Director').
     * @return Mensaje de éxito o descripción del error.
     */
    public String eliminarRedSocial(List<String> params, String senderEmail) {
        if (!verificarRemitente(senderEmail, "Director")) {
            return "ERROR [403]: Acceso denegado. Solo el rol 'Director' "
                    + "puede dar de baja redes sociales.";
        }

        if (params == null || params.size() != 1) {
            return "ERROR [400]: Cantidad de parámetros inválida para DELRED. "
                    + "Se espera 1: [id_red_social]";
        }

        int id;
        try {
            id = Integer.parseInt(params.get(0).trim());
        } catch (NumberFormatException e) {
            return "ERROR [422]: El ID de red social debe ser un número entero válido.";
        }

        RedSocial red = dRedSocial.getById(id);
        if (red == null) {
            return "ERROR [404]: No existe ninguna red social con ID " + id + ".";
        }

        Usuario operador = dUsuario.getByEmail(senderEmail);
        if (operador == null) {
            return "ERROR [401]: No se pudo identificar al operador '" + senderEmail + "'.";
        }

        boolean ok = dRedSocial.delete(id, operador.getId_usuario());
        if (ok) {
            return "SUCCESS: Red social ID " + id + " ('" + red.getPlataforma()
                    + "') dada de baja exitosamente (Estado: Inactivo).";
        }
        return "ERROR [500]: Fallo al dar de baja la red social.";
    }

    /**
     * Lista todos los enlaces de redes sociales registrados.
     * Comando: LISRED []
     *
     * @return Lista de filas String[] con las redes sociales.
     */
    public List<String[]> listarRedesSociales() {
        return dRedSocial.list();
    }

    private boolean esUrlValida(String url) {
        if (url == null || url.isEmpty() || url.length() > 255) {
            return false;
        }
        String lower = url.toLowerCase();
        return lower.startsWith("http://") || lower.startsWith("https://");
    }
}
