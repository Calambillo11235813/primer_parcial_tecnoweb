package uagrm.feder.Negocio;

import uagrm.feder.Datos.DGaleriaMultimedia;
import uagrm.feder.Datos.DNoticia;
import uagrm.feder.Datos.DTestimonio;
import uagrm.feder.Datos.DUsuario;
import uagrm.feder.Entity.GaleriaMultimedia;
import uagrm.feder.Entity.Noticia;
import uagrm.feder.Entity.Testimonio;
import uagrm.feder.Entity.Usuario;

import java.util.List;

/**
 * Capa de Negocio: NPublicacion.
 * Controla la lógica del CU-03: Gestionar Publicaciones y Medios
 * (RF-01: Noticias y Comunicados, RF-02: Galería Multimedia, RF-08: Testimonios).
 */
public class NPublicacion {

    private final DNoticia dNoticia;
    private final DGaleriaMultimedia dGaleria;
    private final DTestimonio dTestimonio;
    private final DUsuario dUsuario;

    public NPublicacion() {
        this.dNoticia = new DNoticia();
        this.dGaleria = new DGaleriaMultimedia();
        this.dTestimonio = new DTestimonio();
        this.dUsuario = new DUsuario();
    }

    /**
     * Verifica si el correo del remitente pertenece a un usuario activo con rol
     * 'Director' o 'Instructor' (roles autorizados para gestionar publicaciones y medios).
     *
     * @param senderEmail Correo del remitente.
     * @return true si tiene permisos para publicar/editar medios.
     */
    public boolean esAutorizadoPublicacion(String senderEmail) {
        if (senderEmail == null || senderEmail.trim().isEmpty()) {
            return false;
        }
        String rol = dUsuario.getRolByEmail(senderEmail.trim());
        return rol != null
                && (rol.equalsIgnoreCase("Director") || rol.equalsIgnoreCase("Instructor"));
    }

    // =========================================================================
    // RF-01: GESTIÓN DE NOTICIAS Y COMUNICADOS (NOTICIA)
    // =========================================================================

    /**
     * Registra y publica una nueva noticia o comunicado institucional.
     * Comando: INSNOT [titulo; contenido; imagen_principal; etiquetas]
     *
     * @param params      Lista con [titulo, contenido, imagen_principal, etiquetas].
     * @param senderEmail Correo del remitente (requiere rol 'Director' o 'Instructor').
     * @return Mensaje de confirmación o descripción del error.
     */
    public String registrarNoticia(List<String> params, String senderEmail) {
        if (!esAutorizadoPublicacion(senderEmail)) {
            return "ERROR [403]: Acceso denegado. Solo los roles 'Director' e 'Instructor' "
                    + "pueden registrar noticias y comunicados.";
        }

        if (params == null || params.size() != 4) {
            return "ERROR [400]: Cantidad de parámetros inválida para INSNOT. "
                    + "Se esperan 4: [titulo; contenido; imagen_principal; etiquetas]";
        }

        String titulo = params.get(0) != null ? params.get(0).trim() : "";
        String contenido = params.get(1) != null ? params.get(1).trim() : "";
        String imagenPrincipal = params.get(2) != null ? params.get(2).trim() : "";
        String etiquetas = params.get(3) != null ? params.get(3).trim() : "";

        if (titulo.isEmpty() || titulo.length() > 200) {
            return "ERROR [422]: El título de la noticia no puede estar vacío "
                    + "ni exceder 200 caracteres.";
        }
        if (contenido.isEmpty()) {
            return "ERROR [422]: El contenido de la noticia no puede estar vacío.";
        }
        if (imagenPrincipal.isEmpty() || imagenPrincipal.length() > 255) {
            return "ERROR [422]: La ruta o URL de la imagen principal es obligatoria "
                    + "(máximo 255 caracteres).";
        }
        if (etiquetas.length() > 200) {
            return "ERROR [422]: Las etiquetas no pueden exceder 200 caracteres.";
        }

        Usuario operador = dUsuario.getByEmail(senderEmail);
        if (operador == null) {
            return "ERROR [401]: No se pudo identificar al operador '" + senderEmail + "'.";
        }

        boolean ok = dNoticia.insert(
                titulo, contenido, imagenPrincipal, etiquetas, operador.getId_usuario()
        );
        if (ok) {
            return "SUCCESS: Noticia '" + titulo + "' registrada y publicada exitosamente.";
        }
        return "ERROR [500]: Fallo al registrar la noticia en la base de datos.";
    }

    /**
     * Modifica una noticia o cambia su estado ('Publicado', 'Borrador', 'Archivado').
     * Comando: MODNOT [id_noticia; titulo; contenido; imagen_principal; etiquetas; estado]
     *
     * @param params      Lista con [id, titulo, contenido, imagen, etiquetas, estado].
     * @param senderEmail Correo del remitente (requiere rol 'Director' o 'Instructor').
     * @return Mensaje de confirmación o descripción del error.
     */
    public String modificarNoticia(List<String> params, String senderEmail) {
        if (!esAutorizadoPublicacion(senderEmail)) {
            return "ERROR [403]: Acceso denegado. Solo los roles 'Director' e 'Instructor' "
                    + "pueden modificar noticias.";
        }

        if (params == null || params.size() != 6) {
            return "ERROR [400]: Cantidad de parámetros inválida para MODNOT. Se esperan 6: "
                    + "[id_noticia; titulo; contenido; imagen_principal; etiquetas; estado]";
        }

        int id;
        try {
            id = Integer.parseInt(params.get(0).trim());
        } catch (NumberFormatException e) {
            return "ERROR [422]: El ID de la noticia debe ser un número entero válido.";
        }

        String titulo = params.get(1) != null ? params.get(1).trim() : "";
        String contenido = params.get(2) != null ? params.get(2).trim() : "";
        String imagenPrincipal = params.get(3) != null ? params.get(3).trim() : "";
        String etiquetas = params.get(4) != null ? params.get(4).trim() : "";
        String estadoRaw = params.get(5) != null ? params.get(5).trim() : "";

        if (titulo.isEmpty() || titulo.length() > 200) {
            return "ERROR [422]: El título no puede estar vacío ni exceder 200 caracteres.";
        }
        if (contenido.isEmpty()) {
            return "ERROR [422]: El contenido de la noticia no puede estar vacío.";
        }
        if (imagenPrincipal.isEmpty() || imagenPrincipal.length() > 255) {
            return "ERROR [422]: La imagen principal es obligatoria (máximo 255 caracteres).";
        }

        String estadoNormalizado = normalizarEstadoNoticia(estadoRaw);
        if (estadoNormalizado == null) {
            return "ERROR [422]: Estado inválido ('" + estadoRaw
                    + "'). Valores permitidos: 'Publicado', 'Borrador' o 'Archivado'.";
        }

        if (dNoticia.getById(id) == null) {
            return "ERROR [404]: No existe ninguna noticia registrada con ID " + id + ".";
        }

        Usuario operador = dUsuario.getByEmail(senderEmail);
        if (operador == null) {
            return "ERROR [401]: No se pudo identificar al operador '" + senderEmail + "'.";
        }

        boolean ok = dNoticia.update(
                id, titulo, contenido, imagenPrincipal, etiquetas,
                estadoNormalizado, operador.getId_usuario()
        );
        if (ok) {
            return "SUCCESS: Noticia ID " + id + " ('" + titulo + "') actualizada "
                    + "correctamente (Estado: " + estadoNormalizado + ").";
        }
        return "ERROR [500]: Fallo al actualizar la noticia en la base de datos.";
    }

    /**
     * Archiva una noticia cambiando su estado a 'Archivado'.
     * Comando: DELNOT [id_noticia]
     *
     * @param params      Lista con [id_noticia].
     * @param senderEmail Correo del remitente (requiere rol 'Director' o 'Instructor').
     * @return Mensaje de confirmación o descripción del error.
     */
    public String eliminarNoticia(List<String> params, String senderEmail) {
        if (!esAutorizadoPublicacion(senderEmail)) {
            return "ERROR [403]: Acceso denegado. Solo los roles 'Director' e 'Instructor' "
                    + "pueden archivar noticias.";
        }

        if (params == null || params.size() != 1) {
            return "ERROR [400]: Cantidad de parámetros inválida para DELNOT. "
                    + "Se espera 1: [id_noticia]";
        }

        int id;
        try {
            id = Integer.parseInt(params.get(0).trim());
        } catch (NumberFormatException e) {
            return "ERROR [422]: El ID de la noticia debe ser un número entero válido.";
        }

        Noticia noticia = dNoticia.getById(id);
        if (noticia == null) {
            return "ERROR [404]: No existe ninguna noticia con ID " + id + ".";
        }

        Usuario operador = dUsuario.getByEmail(senderEmail);
        if (operador == null) {
            return "ERROR [401]: No se pudo identificar al operador '" + senderEmail + "'.";
        }

        boolean ok = dNoticia.delete(id, operador.getId_usuario());
        if (ok) {
            return "SUCCESS: Noticia ID " + id + " ('" + noticia.getTitulo()
                    + "') archivada exitosamente (Estado: Archivado).";
        }
        return "ERROR [500]: Fallo al archivar la noticia.";
    }

    /**
     * Lista todas las noticias registradas.
     * Comando: LISNOT []
     *
     * @return Lista de filas String[] con las noticias.
     */
    public List<String[]> listarNoticias() {
        return dNoticia.list();
    }

    // =========================================================================
    // RF-02: GESTIÓN DE GALERÍA MULTIMEDIA (GALERIA_MULTIMEDIA)
    // =========================================================================

    /**
     * Registra un nuevo archivo en la galería multimedia vinculado a una noticia.
     * Comando: INSMUL [titulo; tipo; url_archivo; descripcion; noticia_id]
     *
     * @param params      Lista con [titulo, tipo, url_archivo, descripcion, noticia_id].
     * @param senderEmail Correo del remitente (requiere rol 'Director' o 'Instructor').
     * @return Mensaje de confirmación o descripción del error.
     */
    public String registrarMultimedia(List<String> params, String senderEmail) {
        if (!esAutorizadoPublicacion(senderEmail)) {
            return "ERROR [403]: Acceso denegado. Solo los roles 'Director' e 'Instructor' "
                    + "pueden gestionar la galería multimedia.";
        }

        if (params == null || params.size() != 5) {
            return "ERROR [400]: Cantidad de parámetros inválida para INSMUL. Se esperan 5: "
                    + "[titulo; tipo; url_archivo; descripcion; noticia_id]";
        }

        String titulo = params.get(0) != null ? params.get(0).trim() : "";
        String tipoRaw = params.get(1) != null ? params.get(1).trim() : "";
        String urlArchivo = params.get(2) != null ? params.get(2).trim() : "";
        String descripcion = params.get(3) != null ? params.get(3).trim() : "";

        int noticiaId;
        try {
            noticiaId = Integer.parseInt(params.get(4).trim());
        } catch (NumberFormatException e) {
            return "ERROR [422]: El ID de la noticia asociada debe ser un número entero.";
        }

        if (titulo.isEmpty() || titulo.length() > 150) {
            return "ERROR [422]: El título multimedia no puede estar vacío "
                    + "ni exceder 150 caracteres.";
        }

        String tipoNormalizado = normalizarTipoMultimedia(tipoRaw);
        if (tipoNormalizado == null) {
            return "ERROR [422]: Tipo multimedia inválido ('" + tipoRaw
                    + "'). Solo se admite 'Imagen' o 'Video'.";
        }

        if (urlArchivo.isEmpty() || urlArchivo.length() > 255) {
            return "ERROR [422]: La URL o ruta del archivo multimedia es obligatoria "
                    + "(máximo 255 caracteres).";
        }
        if (descripcion.length() > 255) {
            return "ERROR [422]: La descripción no puede exceder 255 caracteres.";
        }

        if (dNoticia.getById(noticiaId) == null) {
            return "ERROR [404]: No existe la noticia asociada con ID " + noticiaId + ".";
        }

        Usuario operador = dUsuario.getByEmail(senderEmail);
        if (operador == null) {
            return "ERROR [401]: No se pudo identificar al operador '" + senderEmail + "'.";
        }

        boolean ok = dGaleria.insert(
                titulo, tipoNormalizado, urlArchivo, descripcion,
                noticiaId, operador.getId_usuario()
        );
        if (ok) {
            return "SUCCESS: Recurso multimedia '" + titulo + "' (" + tipoNormalizado
                    + ") vinculado a la noticia ID " + noticiaId + " exitosamente.";
        }
        return "ERROR [500]: Fallo al registrar el recurso multimedia.";
    }

    /**
     * Modifica un elemento existente de la galería multimedia.
     * Comando: MODMUL [id_multimedia; titulo; tipo; url_archivo; descripcion; noticia_id]
     *
     * @param params      Lista con [id, titulo, tipo, url_archivo, descripcion, noticia_id].
     * @param senderEmail Correo del remitente (requiere rol 'Director' o 'Instructor').
     * @return Mensaje de confirmación o descripción del error.
     */
    public String modificarMultimedia(List<String> params, String senderEmail) {
        if (!esAutorizadoPublicacion(senderEmail)) {
            return "ERROR [403]: Acceso denegado. Solo los roles 'Director' e 'Instructor' "
                    + "pueden modificar la galería multimedia.";
        }

        if (params == null || params.size() != 6) {
            return "ERROR [400]: Cantidad de parámetros inválida para MODMUL. Se esperan 6: "
                    + "[id_multimedia; titulo; tipo; url_archivo; descripcion; noticia_id]";
        }

        int id;
        int noticiaId;
        try {
            id = Integer.parseInt(params.get(0).trim());
            noticiaId = Integer.parseInt(params.get(5).trim());
        } catch (NumberFormatException e) {
            return "ERROR [422]: Los identificadores (id_multimedia y noticia_id) "
                    + "deben ser números enteros válidos.";
        }

        String titulo = params.get(1) != null ? params.get(1).trim() : "";
        String tipoRaw = params.get(2) != null ? params.get(2).trim() : "";
        String urlArchivo = params.get(3) != null ? params.get(3).trim() : "";
        String descripcion = params.get(4) != null ? params.get(4).trim() : "";

        if (titulo.isEmpty() || titulo.length() > 150) {
            return "ERROR [422]: El título no puede estar vacío ni exceder 150 caracteres.";
        }

        String tipoNormalizado = normalizarTipoMultimedia(tipoRaw);
        if (tipoNormalizado == null) {
            return "ERROR [422]: Tipo multimedia inválido ('" + tipoRaw
                    + "'). Solo se admite 'Imagen' o 'Video'.";
        }

        if (urlArchivo.isEmpty() || urlArchivo.length() > 255) {
            return "ERROR [422]: La URL del archivo es obligatoria (máximo 255 caracteres).";
        }

        if (dGaleria.getById(id) == null) {
            return "ERROR [404]: No existe ningún elemento multimedia con ID " + id + ".";
        }
        if (dNoticia.getById(noticiaId) == null) {
            return "ERROR [404]: No existe la noticia asociada con ID " + noticiaId + ".";
        }

        Usuario operador = dUsuario.getByEmail(senderEmail);
        if (operador == null) {
            return "ERROR [401]: No se pudo identificar al operador '" + senderEmail + "'.";
        }

        boolean ok = dGaleria.update(
                id, titulo, tipoNormalizado, urlArchivo, descripcion,
                noticiaId, operador.getId_usuario()
        );
        if (ok) {
            return "SUCCESS: Elemento multimedia ID " + id + " ('" + titulo
                    + "') actualizado exitosamente.";
        }
        return "ERROR [500]: Fallo al actualizar el elemento multimedia.";
    }

    /**
     * Elimina un elemento de la galería multimedia.
     * Comando: DELMUL [id_multimedia]
     *
     * @param params      Lista con [id_multimedia].
     * @param senderEmail Correo del remitente (requiere rol 'Director' o 'Instructor').
     * @return Mensaje de confirmación o descripción del error.
     */
    public String eliminarMultimedia(List<String> params, String senderEmail) {
        if (!esAutorizadoPublicacion(senderEmail)) {
            return "ERROR [403]: Acceso denegado. Solo los roles 'Director' e 'Instructor' "
                    + "pueden eliminar recursos multimedia.";
        }

        if (params == null || params.size() != 1) {
            return "ERROR [400]: Cantidad de parámetros inválida para DELMUL. "
                    + "Se espera 1: [id_multimedia]";
        }

        int id;
        try {
            id = Integer.parseInt(params.get(0).trim());
        } catch (NumberFormatException e) {
            return "ERROR [422]: El ID multimedia debe ser un número entero válido.";
        }

        GaleriaMultimedia media = dGaleria.getById(id);
        if (media == null) {
            return "ERROR [404]: No existe ningún recurso multimedia con ID " + id + ".";
        }

        Usuario operador = dUsuario.getByEmail(senderEmail);
        if (operador == null) {
            return "ERROR [401]: No se pudo identificar al operador '" + senderEmail + "'.";
        }

        boolean ok = dGaleria.delete(id, operador.getId_usuario());
        if (ok) {
            return "SUCCESS: Recurso multimedia ID " + id + " ('" + media.getTitulo()
                    + "') eliminado exitosamente.";
        }
        return "ERROR [500]: Fallo al eliminar el recurso multimedia.";
    }

    /**
     * Lista todos los elementos de la galería multimedia.
     * Comando: LISMUL []
     *
     * @return Lista de filas String[] con los archivos multimedia.
     */
    public List<String[]> listarMultimedia() {
        return dGaleria.list();
    }

    // =========================================================================
    // RF-08: GESTIÓN DE TESTIMONIOS (TESTIMONIO)
    // =========================================================================

    /**
     * Registra y publica un nuevo testimonio de microempresario o comunidad.
     * Comando: INSTES [titulo; texto; fotografia; enlace_audiovisual; comunidad_id]
     *
     * @param params      Lista con [titulo, texto, fotografia, enlace, comunidad_id].
     * @param senderEmail Correo del remitente (requiere rol 'Director' o 'Instructor').
     * @return Mensaje de confirmación o descripción del error.
     */
    public String registrarTestimonio(List<String> params, String senderEmail) {
        if (!esAutorizadoPublicacion(senderEmail)) {
            return "ERROR [403]: Acceso denegado. Solo los roles 'Director' e 'Instructor' "
                    + "pueden registrar testimonios.";
        }

        if (params == null || params.size() != 5) {
            return "ERROR [400]: Cantidad de parámetros inválida para INSTES. Se esperan 5: "
                    + "[titulo; texto; fotografia; enlace_audiovisual; comunidad_id]";
        }

        String titulo = params.get(0) != null ? params.get(0).trim() : "";
        String texto = params.get(1) != null ? params.get(1).trim() : "";
        String fotografia = params.get(2) != null ? params.get(2).trim() : "";
        String enlaceAudio = params.get(3) != null ? params.get(3).trim() : "";

        int comunidadId;
        try {
            comunidadId = Integer.parseInt(params.get(4).trim());
        } catch (NumberFormatException e) {
            return "ERROR [422]: El ID de la comunidad debe ser un número entero válido.";
        }

        if (titulo.isEmpty() || titulo.length() > 150) {
            return "ERROR [422]: El título del testimonio no puede estar vacío "
                    + "ni exceder 150 caracteres.";
        }
        if (texto.isEmpty()) {
            return "ERROR [422]: El relato del testimonio no puede estar vacío.";
        }
        if (!dTestimonio.existeComunidad(comunidadId)) {
            return "ERROR [404]: No existe ninguna comunidad registrada con ID "
                    + comunidadId + ".";
        }

        Usuario operador = dUsuario.getByEmail(senderEmail);
        if (operador == null) {
            return "ERROR [401]: No se pudo identificar al operador '" + senderEmail + "'.";
        }

        boolean ok = dTestimonio.insert(
                titulo, texto, fotografia, enlaceAudio, comunidadId, operador.getId_usuario()
        );
        if (ok) {
            return "SUCCESS: Testimonio '" + titulo + "' registrado y publicado exitosamente.";
        }
        return "ERROR [500]: Fallo al registrar el testimonio en la base de datos.";
    }

    /**
     * Modifica los datos o el estado ('Publicado', 'Archivado') de un testimonio.
     * Comando: MODTES [id_testimonio; titulo; texto; fotografia; enlace_audiovisual; estado; comunidad_id]
     *
     * @param params      Lista con los 7 parámetros de modificación.
     * @param senderEmail Correo del remitente (requiere rol 'Director' o 'Instructor').
     * @return Mensaje de confirmación o descripción del error.
     */
    public String modificarTestimonio(List<String> params, String senderEmail) {
        if (!esAutorizadoPublicacion(senderEmail)) {
            return "ERROR [403]: Acceso denegado. Solo los roles 'Director' e 'Instructor' "
                    + "pueden modificar testimonios.";
        }

        if (params == null || params.size() != 7) {
            return "ERROR [400]: Cantidad de parámetros inválida para MODTES. Se esperan 7: "
                    + "[id_testimonio; titulo; texto; fotografia; enlace_audiovisual; "
                    + "estado; comunidad_id]";
        }

        int id;
        int comunidadId;
        try {
            id = Integer.parseInt(params.get(0).trim());
            comunidadId = Integer.parseInt(params.get(6).trim());
        } catch (NumberFormatException e) {
            return "ERROR [422]: Los identificadores (id_testimonio y comunidad_id) "
                    + "deben ser números enteros válidos.";
        }

        String titulo = params.get(1) != null ? params.get(1).trim() : "";
        String texto = params.get(2) != null ? params.get(2).trim() : "";
        String fotografia = params.get(3) != null ? params.get(3).trim() : "";
        String enlaceAudio = params.get(4) != null ? params.get(4).trim() : "";
        String estadoRaw = params.get(5) != null ? params.get(5).trim() : "";

        if (titulo.isEmpty() || titulo.length() > 150) {
            return "ERROR [422]: El título no puede estar vacío ni exceder 150 caracteres.";
        }
        if (texto.isEmpty()) {
            return "ERROR [422]: El relato del testimonio no puede estar vacío.";
        }

        String estadoNormalizado;
        if (estadoRaw.equalsIgnoreCase("Publicado")) {
            estadoNormalizado = "Publicado";
        } else if (estadoRaw.equalsIgnoreCase("Archivado")) {
            estadoNormalizado = "Archivado";
        } else {
            return "ERROR [422]: Estado inválido ('" + estadoRaw
                    + "'). Valores permitidos: 'Publicado' o 'Archivado'.";
        }

        if (dTestimonio.getById(id) == null) {
            return "ERROR [404]: No existe ningún testimonio registrado con ID " + id + ".";
        }
        if (!dTestimonio.existeComunidad(comunidadId)) {
            return "ERROR [404]: No existe ninguna comunidad registrada con ID "
                    + comunidadId + ".";
        }

        Usuario operador = dUsuario.getByEmail(senderEmail);
        if (operador == null) {
            return "ERROR [401]: No se pudo identificar al operador '" + senderEmail + "'.";
        }

        boolean ok = dTestimonio.update(
                id, titulo, texto, fotografia, enlaceAudio,
                estadoNormalizado, comunidadId, operador.getId_usuario()
        );
        if (ok) {
            return "SUCCESS: Testimonio ID " + id + " ('" + titulo + "') actualizado "
                    + "exitosamente (Estado: " + estadoNormalizado + ").";
        }
        return "ERROR [500]: Fallo al actualizar el testimonio.";
    }

    /**
     * Archiva un testimonio cambiando su estado a 'Archivado'.
     * Comando: DELTES [id_testimonio]
     *
     * @param params      Lista con [id_testimonio].
     * @param senderEmail Correo del remitente (requiere rol 'Director' o 'Instructor').
     * @return Mensaje de confirmación o descripción del error.
     */
    public String eliminarTestimonio(List<String> params, String senderEmail) {
        if (!esAutorizadoPublicacion(senderEmail)) {
            return "ERROR [403]: Acceso denegado. Solo los roles 'Director' e 'Instructor' "
                    + "pueden archivar testimonios.";
        }

        if (params == null || params.size() != 1) {
            return "ERROR [400]: Cantidad de parámetros inválida para DELTES. "
                    + "Se espera 1: [id_testimonio]";
        }

        int id;
        try {
            id = Integer.parseInt(params.get(0).trim());
        } catch (NumberFormatException e) {
            return "ERROR [422]: El ID del testimonio debe ser un número entero válido.";
        }

        Testimonio testimonio = dTestimonio.getById(id);
        if (testimonio == null) {
            return "ERROR [404]: No existe ningún testimonio con ID " + id + ".";
        }

        Usuario operador = dUsuario.getByEmail(senderEmail);
        if (operador == null) {
            return "ERROR [401]: No se pudo identificar al operador '" + senderEmail + "'.";
        }

        boolean ok = dTestimonio.delete(id, operador.getId_usuario());
        if (ok) {
            return "SUCCESS: Testimonio ID " + id + " ('" + testimonio.getTitulo()
                    + "') archivado exitosamente (Estado: Archivado).";
        }
        return "ERROR [500]: Fallo al archivar el testimonio.";
    }

    /**
     * Lista todos los testimonios registrados.
     * Comando: LISTES []
     *
     * @return Lista de filas String[] con los testimonios.
     */
    public List<String[]> listarTestimonios() {
        return dTestimonio.list();
    }

    private String normalizarEstadoNoticia(String estado) {
        if (estado == null) {
            return null;
        }
        if (estado.equalsIgnoreCase("Publicado")) {
            return "Publicado";
        }
        if (estado.equalsIgnoreCase("Borrador")) {
            return "Borrador";
        }
        if (estado.equalsIgnoreCase("Archivado")) {
            return "Archivado";
        }
        return null;
    }

    private String normalizarTipoMultimedia(String tipo) {
        if (tipo == null) {
            return null;
        }
        if (tipo.equalsIgnoreCase("Imagen")) {
            return "Imagen";
        }
        if (tipo.equalsIgnoreCase("Video")) {
            return "Video";
        }
        return null;
    }
}
