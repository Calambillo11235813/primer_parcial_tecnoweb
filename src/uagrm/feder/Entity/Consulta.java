package uagrm.feder.Entity;

import java.sql.Timestamp;

/**
 * Entidad del Dominio: Consulta.
 * Representa la tabla CONSULTA en la base de datos (RF-16).
 */
public class Consulta {

    private int id_consulta;
    private String nombre_remitente;
    private String correo_remitente;
    private String asunto;
    private String mensaje;
    private Timestamp fecha_envio;
    private String estado;
    private Integer usuario_id;

    /**
     * Constructor por defecto requerido para instanciación vacía.
     */
    public Consulta() {
    }

    /**
     * Constructor parametrizado completo para mapear registros de CONSULTA.
     *
     * @param id_consulta      Identificador primario de la consulta.
     * @param nombre_remitente Nombre completo del ciudadano o remitente.
     * @param correo_remitente Dirección de correo electrónico del remitente.
     * @param asunto           Asunto o motivo de la consulta.
     * @param mensaje          Contenido detallado del mensaje.
     * @param fecha_envio      Fecha y hora de recepción de la consulta.
     * @param estado           Estado de atención ('Pendiente', 'En Proceso', 'Atendido').
     * @param usuario_id       ID del funcionario que atendió/clasificó (puede ser null).
     */
    public Consulta(int id_consulta, String nombre_remitente, String correo_remitente,
            String asunto, String mensaje, Timestamp fecha_envio,
            String estado, Integer usuario_id) {
        this.id_consulta = id_consulta;
        this.nombre_remitente = nombre_remitente;
        this.correo_remitente = correo_remitente;
        this.asunto = asunto;
        this.mensaje = mensaje;
        this.fecha_envio = fecha_envio;
        this.estado = estado;
        this.usuario_id = usuario_id;
    }

    public int getId_consulta() {
        return id_consulta;
    }

    public void setId_consulta(int id_consulta) {
        this.id_consulta = id_consulta;
    }

    public String getNombre_remitente() {
        return nombre_remitente;
    }

    public void setNombre_remitente(String nombre_remitente) {
        this.nombre_remitente = nombre_remitente;
    }

    public String getCorreo_remitente() {
        return correo_remitente;
    }

    public void setCorreo_remitente(String correo_remitente) {
        this.correo_remitente = correo_remitente;
    }

    public String getAsunto() {
        return asunto;
    }

    public void setAsunto(String asunto) {
        this.asunto = asunto;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public Timestamp getFecha_envio() {
        return fecha_envio;
    }

    public void setFecha_envio(Timestamp fecha_envio) {
        this.fecha_envio = fecha_envio;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public Integer getUsuario_id() {
        return usuario_id;
    }

    public void setUsuario_id(Integer usuario_id) {
        this.usuario_id = usuario_id;
    }

    @Override
    public String toString() {
        return "Consulta{"
                + "id_consulta=" + id_consulta
                + ", nombre_remitente='" + nombre_remitente + '\''
                + ", correo_remitente='" + correo_remitente + '\''
                + ", asunto='" + asunto + '\''
                + ", fecha_envio=" + fecha_envio
                + ", estado='" + estado + '\''
                + ", usuario_id=" + usuario_id
                + '}';
    }
}
