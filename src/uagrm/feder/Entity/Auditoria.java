package uagrm.feder.Entity;

import java.sql.Timestamp;

/**
 * Entidad del Dominio: Auditoria
 * Registra cada operación sensible ejecutada en el sistema (INSERT, UPDATE, DELETE).
 */
public class Auditoria {

    private int id_auditoria;
    private String accion_realizada;
    private String tabla_afectada;
    private Timestamp fecha_hora;
    private int usuario_id;

    public Auditoria() {
    }

    public Auditoria(int id_auditoria, String accion_realizada, String tabla_afectada, Timestamp fecha_hora, int usuario_id) {
        this.id_auditoria = id_auditoria;
        this.accion_realizada = accion_realizada;
        this.tabla_afectada = tabla_afectada;
        this.fecha_hora = fecha_hora;
        this.usuario_id = usuario_id;
    }

    public Auditoria(String accion_realizada, String tabla_afectada, int usuario_id) {
        this.accion_realizada = accion_realizada;
        this.tabla_afectada = tabla_afectada;
        this.usuario_id = usuario_id;
    }

    public int getId_auditoria() {
        return id_auditoria;
    }

    public void setId_auditoria(int id_auditoria) {
        this.id_auditoria = id_auditoria;
    }

    public String getAccion_realizada() {
        return accion_realizada;
    }

    public void setAccion_realizada(String accion_realizada) {
        this.accion_realizada = accion_realizada;
    }

    public String getTabla_afectada() {
        return tabla_afectada;
    }

    public void setTabla_afectada(String tabla_afectada) {
        this.tabla_afectada = tabla_afectada;
    }

    public Timestamp getFecha_hora() {
        return fecha_hora;
    }

    public void setFecha_hora(Timestamp fecha_hora) {
        this.fecha_hora = fecha_hora;
    }

    public int getUsuario_id() {
        return usuario_id;
    }

    public void setUsuario_id(int usuario_id) {
        this.usuario_id = usuario_id;
    }

    @Override
    public String toString() {
        return "Auditoria{" +
                "id_auditoria=" + id_auditoria +
                ", accion_realizada='" + accion_realizada + '\'' +
                ", tabla_afectada='" + tabla_afectada + '\'' +
                ", fecha_hora=" + fecha_hora +
                ", usuario_id=" + usuario_id +
                '}';
    }
}

