package uagrm.feder.Entity;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * Entidad del Dominio: Aporte.
 * Representa la tabla APORTE en la base de datos (RF-11 y RF-12).
 */
public class Aporte {

    private int id_aporte;
    private String nombre_donante;
    private String correo_donante;
    private BigDecimal monto;
    private String modalidad;
    private Timestamp fecha_registro;
    private String estado;
    private int campana_id;

    /**
     * Constructor por defecto requerido para instanciación vacía.
     */
    public Aporte() {
    }

    /**
     * Constructor parametrizado completo para mapear registros de APORTE.
     *
     * @param id_aporte      Identificador primario del aporte.
     * @param nombre_donante Nombre completo del donante o entidad aportante.
     * @param correo_donante Correo electrónico del donante.
     * @param monto          Monto comprometido o donado en Bolivianos (> 0).
     * @param modalidad      Modalidad de pago ('Transferencia bancaria', 'QR', 'Efectivo').
     * @param fecha_registro Fecha y hora de registro del compromiso.
     * @param estado         Estado ('Pendiente de Confirmación', 'Confirmado', 'Rechazado').
     * @param campana_id     ID de la campaña de recaudación asociada.
     */
    public Aporte(int id_aporte, String nombre_donante, String correo_donante,
            BigDecimal monto, String modalidad, Timestamp fecha_registro,
            String estado, int campana_id) {
        this.id_aporte = id_aporte;
        this.nombre_donante = nombre_donante;
        this.correo_donante = correo_donante;
        this.monto = monto;
        this.modalidad = modalidad;
        this.fecha_registro = fecha_registro;
        this.estado = estado;
        this.campana_id = campana_id;
    }

    public int getId_aporte() {
        return id_aporte;
    }

    public void setId_aporte(int id_aporte) {
        this.id_aporte = id_aporte;
    }

    public String getNombre_donante() {
        return nombre_donante;
    }

    public void setNombre_donante(String nombre_donante) {
        this.nombre_donante = nombre_donante;
    }

    public String getCorreo_donante() {
        return correo_donante;
    }

    public void setCorreo_donante(String correo_donante) {
        this.correo_donante = correo_donante;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public void setMonto(BigDecimal monto) {
        this.monto = monto;
    }

    public String getModalidad() {
        return modalidad;
    }

    public void setModalidad(String modalidad) {
        this.modalidad = modalidad;
    }

    public Timestamp getFecha_registro() {
        return fecha_registro;
    }

    public void setFecha_registro(Timestamp fecha_registro) {
        this.fecha_registro = fecha_registro;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public int getCampana_id() {
        return campana_id;
    }

    public void setCampana_id(int campana_id) {
        this.campana_id = campana_id;
    }

    @Override
    public String toString() {
        return "Aporte{"
                + "id_aporte=" + id_aporte
                + ", nombre_donante='" + nombre_donante + '\''
                + ", correo_donante='" + correo_donante + '\''
                + ", monto=" + monto
                + ", modalidad='" + modalidad + '\''
                + ", fecha_registro=" + fecha_registro
                + ", estado='" + estado + '\''
                + ", campana_id=" + campana_id
                + '}';
    }
}
