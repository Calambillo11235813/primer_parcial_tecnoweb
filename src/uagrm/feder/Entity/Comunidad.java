package uagrm.feder.Entity;

/**
 * Entidad del Dominio: Comunidad.
 * Representa la tabla COMUNIDAD en la base de datos (RF-05).
 */
public class Comunidad {

    private int id_comunidad;
    private String nombre;
    private String descripcion;
    private String ubicacion;
    private String estado;
    private int usuario_id;

    /**
     * Constructor por defecto requerido para instanciación vacía.
     */
    public Comunidad() {
    }

    /**
     * Constructor parametrizado completo para mapear registros de COMUNIDAD.
     *
     * @param id_comunidad Identificador primario de la comunidad.
     * @param nombre       Nombre de la comunidad o municipio beneficiario.
     * @param descripcion  Descripción socioeconómica o productiva de la comunidad.
     * @param ubicacion    Ubicación geográfica (provincia, región).
     * @param estado       Estado operativo ('Activa' o 'Inactiva').
     * @param usuario_id   ID del usuario que registró o actualizó la comunidad.
     */
    public Comunidad(int id_comunidad, String nombre, String descripcion,
            String ubicacion, String estado, int usuario_id) {
        this.id_comunidad = id_comunidad;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.ubicacion = ubicacion;
        this.estado = estado;
        this.usuario_id = usuario_id;
    }

    public int getId_comunidad() {
        return id_comunidad;
    }

    public void setId_comunidad(int id_comunidad) {
        this.id_comunidad = id_comunidad;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getUbicacion() {
        return ubicacion;
    }

    public void setUbicacion(String ubicacion) {
        this.ubicacion = ubicacion;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public int getUsuario_id() {
        return usuario_id;
    }

    public void setUsuario_id(int usuario_id) {
        this.usuario_id = usuario_id;
    }

    @Override
    public String toString() {
        return "Comunidad{"
                + "id_comunidad=" + id_comunidad
                + ", nombre='" + nombre + '\''
                + ", ubicacion='" + ubicacion + '\''
                + ", estado='" + estado + '\''
                + ", usuario_id=" + usuario_id
                + '}';
    }
}
