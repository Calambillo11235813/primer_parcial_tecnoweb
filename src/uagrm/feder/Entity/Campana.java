package uagrm.feder.Entity;

import java.math.BigDecimal;
import java.sql.Date;

/**
 * Entidad del Dominio: Campana.
 * Representa la tabla CAMPANA en la base de datos (RF-09 y RF-10).
 */
public class Campana {

    private int id_campana;
    private String titulo;
    private String descripcion;
    private BigDecimal meta_economica;
    private BigDecimal monto_recaudado;
    private Date fecha_inicio;
    private Date fecha_limite;
    private String estado;
    private int comunidad_id;

    /**
     * Constructor por defecto requerido para instanciación vacía.
     */
    public Campana() {
    }

    /**
     * Constructor parametrizado completo para mapear registros de CAMPANA.
     *
     * @param id_campana      Identificador primario de la campaña.
     * @param titulo          Título de la campaña de recaudación.
     * @param descripcion     Descripción y objetivo del proyecto comunitario.
     * @param meta_economica  Meta económica objetivo en Bolivianos (mayor a 0).
     * @param monto_recaudado Monto acumulado recaudado en Bolivianos.
     * @param fecha_inicio    Fecha de inicio de la campaña.
     * @param fecha_limite    Fecha límite de cierre (mayor o igual a fecha_inicio).
     * @param estado          Estado de la campaña ('Activa', 'Cerrada', 'Finalizada', 'Cancelada').
     * @param comunidad_id    ID de la comunidad beneficiaria asociada.
     */
    public Campana(int id_campana, String titulo, String descripcion,
            BigDecimal meta_economica, BigDecimal monto_recaudado,
            Date fecha_inicio, Date fecha_limite, String estado, int comunidad_id) {
        this.id_campana = id_campana;
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.meta_economica = meta_economica;
        this.monto_recaudado = monto_recaudado;
        this.fecha_inicio = fecha_inicio;
        this.fecha_limite = fecha_limite;
        this.estado = estado;
        this.comunidad_id = comunidad_id;
    }

    public int getId_campana() {
        return id_campana;
    }

    public void setId_campana(int id_campana) {
        this.id_campana = id_campana;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public BigDecimal getMeta_economica() {
        return meta_economica;
    }

    public void setMeta_economica(BigDecimal meta_economica) {
        this.meta_economica = meta_economica;
    }

    public BigDecimal getMonto_recaudado() {
        return monto_recaudado;
    }

    public void setMonto_recaudado(BigDecimal monto_recaudado) {
        this.monto_recaudado = monto_recaudado;
    }

    public Date getFecha_inicio() {
        return fecha_inicio;
    }

    public void setFecha_inicio(Date fecha_inicio) {
        this.fecha_inicio = fecha_inicio;
    }

    public Date getFecha_limite() {
        return fecha_limite;
    }

    public void setFecha_limite(Date fecha_limite) {
        this.fecha_limite = fecha_limite;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public int getComunidad_id() {
        return comunidad_id;
    }

    public void setComunidad_id(int comunidad_id) {
        this.comunidad_id = comunidad_id;
    }

    @Override
    public String toString() {
        return "Campana{"
                + "id_campana=" + id_campana
                + ", titulo='" + titulo + '\''
                + ", meta_economica=" + meta_economica
                + ", monto_recaudado=" + monto_recaudado
                + ", fecha_inicio=" + fecha_inicio
                + ", fecha_limite=" + fecha_limite
                + ", estado='" + estado + '\''
                + ", comunidad_id=" + comunidad_id
                + '}';
    }
}
