package uagrm.feder.Entity;

/**
 * Entidad del Dominio: ProgramaProductivo.
 * Representa la tabla PROGRAMA_PRODUCTIVO en la base de datos (RF-06).
 */
public class ProgramaProductivo {

    private int id_programa;
    private String nombre;
    private String descripcion;
    private String area_productiva;
    private String poblacion_beneficiaria;
    private int comunidad_id;

    /**
     * Constructor por defecto requerido para instanciación vacía.
     */
    public ProgramaProductivo() {
    }

    /**
     * Constructor parametrizado completo para mapear registros de PROGRAMA_PRODUCTIVO.
     *
     * @param id_programa            Identificador primario del programa productivo.
     * @param nombre                 Nombre del programa o taller de asistencia técnica.
     * @param descripcion            Descripción detallada del programa.
     * @param area_productiva        Rubro o área productiva (aceites, jabonería, harinas, etc.).
     * @param poblacion_beneficiaria Descripción o grupo de beneficiarios directos.
     * @param comunidad_id           ID de la comunidad beneficiaria asociada.
     */
    public ProgramaProductivo(int id_programa, String nombre, String descripcion,
            String area_productiva, String poblacion_beneficiaria, int comunidad_id) {
        this.id_programa = id_programa;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.area_productiva = area_productiva;
        this.poblacion_beneficiaria = poblacion_beneficiaria;
        this.comunidad_id = comunidad_id;
    }

    public int getId_programa() {
        return id_programa;
    }

    public void setId_programa(int id_programa) {
        this.id_programa = id_programa;
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

    public String getArea_productiva() {
        return area_productiva;
    }

    public void setArea_productiva(String area_productiva) {
        this.area_productiva = area_productiva;
    }

    public String getPoblacion_beneficiaria() {
        return poblacion_beneficiaria;
    }

    public void setPoblacion_beneficiaria(String poblacion_beneficiaria) {
        this.poblacion_beneficiaria = poblacion_beneficiaria;
    }

    public int getComunidad_id() {
        return comunidad_id;
    }

    public void setComunidad_id(int comunidad_id) {
        this.comunidad_id = comunidad_id;
    }

    @Override
    public String toString() {
        return "ProgramaProductivo{"
                + "id_programa=" + id_programa
                + ", nombre='" + nombre + '\''
                + ", area_productiva='" + area_productiva + '\''
                + ", poblacion_beneficiaria='" + poblacion_beneficiaria + '\''
                + ", comunidad_id=" + comunidad_id
                + '}';
    }
}
