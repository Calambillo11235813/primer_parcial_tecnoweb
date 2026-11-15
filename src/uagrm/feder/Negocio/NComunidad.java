package uagrm.feder.Negocio;

import uagrm.feder.Datos.DComunidad;
import uagrm.feder.Datos.DProgramaProductivo;
import uagrm.feder.Datos.DUsuario;
import uagrm.feder.Entity.Comunidad;
import uagrm.feder.Entity.ProgramaProductivo;
import uagrm.feder.Entity.Usuario;

import java.util.List;

/**
 * Capa de Negocio: NComunidad.
 * Controla la lógica del CU-04: Gestionar Comunidades y Programas
 * (RF-05: Comunidades Beneficiarias, RF-06: Programas de Asistencia,
 * RF-07: Búsqueda y Filtrado de Contenidos).
 */
public class NComunidad {

    private final DComunidad dComunidad;
    private final DProgramaProductivo dPrograma;
    private final DUsuario dUsuario;

    public NComunidad() {
        this.dComunidad = new DComunidad();
        this.dPrograma = new DProgramaProductivo();
        this.dUsuario = new DUsuario();
    }

    /**
     * Verifica si el correo del remitente pertenece a un usuario activo con rol
     * 'Director' o 'Instructor'.
     *
     * @param senderEmail Correo electrónico del remitente.
     * @return true si tiene permisos para gestionar comunidades y programas.
     */
    public boolean esAutorizadoComunidad(String senderEmail) {
        if (senderEmail == null || senderEmail.trim().isEmpty()) {
            return false;
        }
        String rol = dUsuario.getRolByEmail(senderEmail.trim());
        return rol != null
                && (rol.equalsIgnoreCase("Director") || rol.equalsIgnoreCase("Instructor"));
    }

    // =========================================================================
    // RF-05: GESTIÓN DE COMUNIDADES BENEFICIARIAS (COMUNIDAD)
    // =========================================================================

    /**
     * Registra una nueva comunidad beneficiaria de la Chiquitanía.
     * Comando: INSCOM [nombre; descripcion; ubicacion]
     *
     * @param params      Lista con [nombre, descripcion, ubicacion].
     * @param senderEmail Correo del remitente (requiere rol 'Director' o 'Instructor').
     * @return Mensaje de éxito o descripción del error.
     */
    public String registrarComunidad(List<String> params, String senderEmail) {
        if (!esAutorizadoComunidad(senderEmail)) {
            return "ERROR [403]: Acceso denegado. Solo los roles 'Director' e 'Instructor' "
                    + "pueden registrar comunidades beneficiarias.";
        }

        if (params == null || params.size() != 3) {
            return "ERROR [400]: Cantidad de parámetros inválida para INSCOM. "
                    + "Se esperan 3: [nombre; descripcion; ubicacion]";
        }

        String nombre = params.get(0) != null ? params.get(0).trim() : "";
        String descripcion = params.get(1) != null ? params.get(1).trim() : "";
        String ubicacion = params.get(2) != null ? params.get(2).trim() : "";

        if (nombre.isEmpty() || nombre.length() > 100) {
            return "ERROR [422]: El nombre de la comunidad no puede estar vacío "
                    + "ni exceder 100 caracteres.";
        }
        if (ubicacion.isEmpty() || ubicacion.length() > 255) {
            return "ERROR [422]: La ubicación geográfica es obligatoria "
                    + "(máximo 255 caracteres).";
        }

        if (dComunidad.getByNombre(nombre) != null) {
            return "ERROR [409]: Ya existe una comunidad registrada con el nombre '"
                    + nombre + "'.";
        }

        Usuario operador = dUsuario.getByEmail(senderEmail);
        if (operador == null) {
            return "ERROR [401]: No se pudo identificar al operador '" + senderEmail + "'.";
        }

        boolean ok = dComunidad.insert(
                nombre, descripcion, ubicacion, operador.getId_usuario()
        );
        if (ok) {
            return "SUCCESS: Comunidad beneficiaria '" + nombre
                    + "' registrada exitosamente.";
        }
        return "ERROR [500]: Fallo al registrar la comunidad en la base de datos.";
    }

    /**
     * Modifica los datos o el estado ('Activa', 'Inactiva') de una comunidad.
     * Comando: MODCOM [id_comunidad; nombre; descripcion; ubicacion; estado]
     *
     * @param params      Lista con [id_comunidad, nombre, descripcion, ubicacion, estado].
     * @param senderEmail Correo del remitente (requiere rol 'Director' o 'Instructor').
     * @return Mensaje de éxito o descripción del error.
     */
    public String modificarComunidad(List<String> params, String senderEmail) {
        if (!esAutorizadoComunidad(senderEmail)) {
            return "ERROR [403]: Acceso denegado. Solo los roles 'Director' e 'Instructor' "
                    + "pueden modificar comunidades beneficiarias.";
        }

        if (params == null || params.size() != 5) {
            return "ERROR [400]: Cantidad de parámetros inválida para MODCOM. "
                    + "Se esperan 5: [id_comunidad; nombre; descripcion; ubicacion; estado]";
        }

        int id;
        try {
            id = Integer.parseInt(params.get(0).trim());
        } catch (NumberFormatException e) {
            return "ERROR [422]: El ID de la comunidad debe ser un número entero válido.";
        }

        String nombre = params.get(1) != null ? params.get(1).trim() : "";
        String descripcion = params.get(2) != null ? params.get(2).trim() : "";
        String ubicacion = params.get(3) != null ? params.get(3).trim() : "";
        String estadoRaw = params.get(4) != null ? params.get(4).trim() : "";

        if (nombre.isEmpty() || nombre.length() > 100) {
            return "ERROR [422]: El nombre de la comunidad no puede estar vacío "
                    + "ni exceder 100 caracteres.";
        }
        if (ubicacion.isEmpty() || ubicacion.length() > 255) {
            return "ERROR [422]: La ubicación geográfica es obligatoria.";
        }

        String estadoNormalizado;
        if (estadoRaw.equalsIgnoreCase("Activa") || estadoRaw.equalsIgnoreCase("Activo")) {
            estadoNormalizado = "Activa";
        } else if (estadoRaw.equalsIgnoreCase("Inactiva")
                || estadoRaw.equalsIgnoreCase("Inactivo")) {
            estadoNormalizado = "Inactiva";
        } else {
            return "ERROR [422]: Estado inválido ('" + estadoRaw
                    + "'). Valores permitidos: 'Activa' o 'Inactiva'.";
        }

        if (dComunidad.getById(id) == null) {
            return "ERROR [404]: No existe ninguna comunidad registrada con ID " + id + ".";
        }

        Comunidad existenteNombre = dComunidad.getByNombre(nombre);
        if (existenteNombre != null && existenteNombre.getId_comunidad() != id) {
            return "ERROR [409]: El nombre '" + nombre
                    + "' ya pertenece a otra comunidad (ID "
                    + existenteNombre.getId_comunidad() + ").";
        }

        Usuario operador = dUsuario.getByEmail(senderEmail);
        if (operador == null) {
            return "ERROR [401]: No se pudo identificar al operador '" + senderEmail + "'.";
        }

        boolean ok = dComunidad.update(
                id, nombre, descripcion, ubicacion, estadoNormalizado,
                operador.getId_usuario()
        );
        if (ok) {
            return "SUCCESS: Comunidad ID " + id + " ('" + nombre
                    + "') actualizada exitosamente (Estado: " + estadoNormalizado + ").";
        }
        return "ERROR [500]: Fallo al actualizar la comunidad.";
    }

    /**
     * Realiza la baja lógica de una comunidad cambiando su estado a 'Inactiva'.
     * Comando: DELCOM [id_comunidad]
     *
     * @param params      Lista con [id_comunidad].
     * @param senderEmail Correo del remitente (requiere rol 'Director' o 'Instructor').
     * @return Mensaje de éxito o descripción del error.
     */
    public String eliminarComunidad(List<String> params, String senderEmail) {
        if (!esAutorizadoComunidad(senderEmail)) {
            return "ERROR [403]: Acceso denegado. Solo los roles 'Director' e 'Instructor' "
                    + "pueden dar de baja comunidades.";
        }

        if (params == null || params.size() != 1) {
            return "ERROR [400]: Cantidad de parámetros inválida para DELCOM. "
                    + "Se espera 1: [id_comunidad]";
        }

        int id;
        try {
            id = Integer.parseInt(params.get(0).trim());
        } catch (NumberFormatException e) {
            return "ERROR [422]: El ID de la comunidad debe ser un número entero válido.";
        }

        Comunidad comunidad = dComunidad.getById(id);
        if (comunidad == null) {
            return "ERROR [404]: No existe ninguna comunidad con ID " + id + ".";
        }

        Usuario operador = dUsuario.getByEmail(senderEmail);
        if (operador == null) {
            return "ERROR [401]: No se pudo identificar al operador '" + senderEmail + "'.";
        }

        boolean ok = dComunidad.delete(id, operador.getId_usuario());
        if (ok) {
            return "SUCCESS: Comunidad ID " + id + " ('" + comunidad.getNombre()
                    + "') dada de baja exitosamente (Estado: Inactiva).";
        }
        return "ERROR [500]: Fallo al dar de baja la comunidad.";
    }

    /**
     * Lista todas las comunidades beneficiarias registradas.
     * Comando: LISCOM []
     *
     * @return Lista de filas String[] con las comunidades.
     */
    public List<String[]> listarComunidades() {
        return dComunidad.list();
    }

    // =========================================================================
    // RF-06: GESTIÓN DE PROGRAMAS DE ASISTENCIA (PROGRAMA_PRODUCTIVO)
    // =========================================================================

    /**
     * Registra un nuevo programa productivo o capacitación técnica.
     * Comando: INSPRO [nombre; descripcion; area_productiva; poblacion_beneficiaria; comunidad_id]
     *
     * @param params      Lista con [nombre, descripcion, area, poblacion, comunidad_id].
     * @param senderEmail Correo del remitente (requiere rol 'Director' o 'Instructor').
     * @return Mensaje de éxito o descripción del error.
     */
    public String registrarPrograma(List<String> params, String senderEmail) {
        if (!esAutorizadoComunidad(senderEmail)) {
            return "ERROR [403]: Acceso denegado. Solo los roles 'Director' e 'Instructor' "
                    + "pueden registrar programas productivos.";
        }

        if (params == null || params.size() != 5) {
            return "ERROR [400]: Cantidad de parámetros inválida para INSPRO. Se esperan 5: "
                    + "[nombre; descripcion; area_productiva; poblacion_beneficiaria; "
                    + "comunidad_id]";
        }

        String nombre = params.get(0) != null ? params.get(0).trim() : "";
        String descripcion = params.get(1) != null ? params.get(1).trim() : "";
        String areaProductiva = params.get(2) != null ? params.get(2).trim() : "";
        String poblacion = params.get(3) != null ? params.get(3).trim() : "";

        int comunidadId;
        try {
            comunidadId = Integer.parseInt(params.get(4).trim());
        } catch (NumberFormatException e) {
            return "ERROR [422]: El ID de la comunidad debe ser un número entero válido.";
        }

        if (nombre.isEmpty() || nombre.length() > 150) {
            return "ERROR [422]: El nombre del programa no puede estar vacío "
                    + "ni exceder 150 caracteres.";
        }
        if (descripcion.isEmpty()) {
            return "ERROR [422]: La descripción del programa no puede estar vacía.";
        }
        if (areaProductiva.isEmpty() || areaProductiva.length() > 100) {
            return "ERROR [422]: El área productiva o rubro es obligatoria "
                    + "(máximo 100 caracteres).";
        }
        if (poblacion.length() > 150) {
            return "ERROR [422]: La población beneficiaria no puede exceder 150 caracteres.";
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

        boolean ok = dPrograma.insert(
                nombre, descripcion, areaProductiva, poblacion,
                comunidadId, operador.getId_usuario()
        );
        if (ok) {
            return "SUCCESS: Programa productivo '" + nombre + "' (Rubro: "
                    + areaProductiva + ") registrado para la comunidad '"
                    + comunidad.getNombre() + "'.";
        }
        return "ERROR [500]: Fallo al registrar el programa productivo.";
    }

    /**
     * Modifica un programa productivo existente.
     * Comando: MODPRO [id_programa; nombre; descripcion; area_productiva; poblacion_beneficiaria; comunidad_id]
     *
     * @param params      Lista con los 6 parámetros de modificación.
     * @param senderEmail Correo del remitente (requiere rol 'Director' o 'Instructor').
     * @return Mensaje de éxito o descripción del error.
     */
    public String modificarPrograma(List<String> params, String senderEmail) {
        if (!esAutorizadoComunidad(senderEmail)) {
            return "ERROR [403]: Acceso denegado. Solo los roles 'Director' e 'Instructor' "
                    + "pueden modificar programas productivos.";
        }

        if (params == null || params.size() != 6) {
            return "ERROR [400]: Cantidad de parámetros inválida para MODPRO. Se esperan 6: "
                    + "[id_programa; nombre; descripcion; area_productiva; "
                    + "poblacion_beneficiaria; comunidad_id]";
        }

        int id;
        int comunidadId;
        try {
            id = Integer.parseInt(params.get(0).trim());
            comunidadId = Integer.parseInt(params.get(5).trim());
        } catch (NumberFormatException e) {
            return "ERROR [422]: Los identificadores (id_programa y comunidad_id) "
                    + "deben ser números enteros válidos.";
        }

        String nombre = params.get(1) != null ? params.get(1).trim() : "";
        String descripcion = params.get(2) != null ? params.get(2).trim() : "";
        String areaProductiva = params.get(3) != null ? params.get(3).trim() : "";
        String poblacion = params.get(4) != null ? params.get(4).trim() : "";

        if (nombre.isEmpty() || nombre.length() > 150) {
            return "ERROR [422]: El nombre del programa no puede estar vacío "
                    + "ni exceder 150 caracteres.";
        }
        if (descripcion.isEmpty()) {
            return "ERROR [422]: La descripción del programa no puede estar vacía.";
        }
        if (areaProductiva.isEmpty() || areaProductiva.length() > 100) {
            return "ERROR [422]: El área productiva es obligatoria (máximo 100 caracteres).";
        }

        if (dPrograma.getById(id) == null) {
            return "ERROR [404]: No existe ningún programa productivo con ID " + id + ".";
        }
        if (dComunidad.getById(comunidadId) == null) {
            return "ERROR [404]: No existe ninguna comunidad registrada con ID "
                    + comunidadId + ".";
        }

        Usuario operador = dUsuario.getByEmail(senderEmail);
        if (operador == null) {
            return "ERROR [401]: No se pudo identificar al operador '" + senderEmail + "'.";
        }

        boolean ok = dPrograma.update(
                id, nombre, descripcion, areaProductiva, poblacion,
                comunidadId, operador.getId_usuario()
        );
        if (ok) {
            return "SUCCESS: Programa productivo ID " + id + " ('" + nombre
                    + "') actualizado exitosamente.";
        }
        return "ERROR [500]: Fallo al actualizar el programa productivo.";
    }

    /**
     * Elimina un programa productivo existente.
     * Comando: DELPRO [id_programa]
     *
     * @param params      Lista con [id_programa].
     * @param senderEmail Correo del remitente (requiere rol 'Director' o 'Instructor').
     * @return Mensaje de éxito o descripción del error.
     */
    public String eliminarPrograma(List<String> params, String senderEmail) {
        if (!esAutorizadoComunidad(senderEmail)) {
            return "ERROR [403]: Acceso denegado. Solo los roles 'Director' e 'Instructor' "
                    + "pueden eliminar programas productivos.";
        }

        if (params == null || params.size() != 1) {
            return "ERROR [400]: Cantidad de parámetros inválida para DELPRO. "
                    + "Se espera 1: [id_programa]";
        }

        int id;
        try {
            id = Integer.parseInt(params.get(0).trim());
        } catch (NumberFormatException e) {
            return "ERROR [422]: El ID del programa debe ser un número entero válido.";
        }

        ProgramaProductivo prog = dPrograma.getById(id);
        if (prog == null) {
            return "ERROR [404]: No existe ningún programa productivo con ID " + id + ".";
        }

        Usuario operador = dUsuario.getByEmail(senderEmail);
        if (operador == null) {
            return "ERROR [401]: No se pudo identificar al operador '" + senderEmail + "'.";
        }

        boolean ok = dPrograma.delete(id, operador.getId_usuario());
        if (ok) {
            return "SUCCESS: Programa productivo ID " + id + " ('" + prog.getNombre()
                    + "') eliminado exitosamente.";
        }
        return "ERROR [500]: Fallo al eliminar el programa productivo.";
    }

    /**
     * Lista todos los programas productivos registrados.
     * Comando: LISPRO []
     *
     * @return Lista de filas String[] con los programas productivos.
     */
    public List<String[]> listarProgramas() {
        return dPrograma.list();
    }

    // =========================================================================
    // RF-07: BÚSQUEDA Y FILTRADO DE CONTENIDOS POR COMUNIDAD O RUBRO
    // =========================================================================

    /**
     * Busca y filtra programas, noticias y testimonios por comunidad, rubro o palabra clave.
     * Comando: BUSCON [criterio_busqueda]
     *
     * @param params Lista con [criterio_busqueda].
     * @return Lista de filas String[] con las coincidencias encontradas.
     */
    public List<String[]> buscarContenidos(List<String> params) {
        if (params == null || params.size() != 1 || params.get(0).trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "ERROR [400]: Debe especificar un criterio de búsqueda. "
                    + "Uso: BUSCON [comunidad_o_rubro]"
            );
        }
        return dPrograma.buscarContenidos(params.get(0).trim());
    }
}
