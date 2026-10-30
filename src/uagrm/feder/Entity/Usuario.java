package uagrm.feder.Entity;

import java.sql.Timestamp;

/**
 * Entidad del Dominio: Usuario
 * Representa a un usuario del sistema (Director, Secretaria, Instructor).
 */
public class Usuario {

    private int id_usuario;
    private String nombre_completo;
    private String correo_electronico;
    private String hash_contrasena;
    private String estado;
    private Timestamp fecha_registro;
    private int rol_id;

    public Usuario() {
    }

    public Usuario(int id_usuario, String nombre_completo, String correo_electronico, String hash_contrasena, String estado, Timestamp fecha_registro, int rol_id) {
        this.id_usuario = id_usuario;
        this.nombre_completo = nombre_completo;
        this.correo_electronico = correo_electronico;
        this.hash_contrasena = hash_contrasena;
        this.estado = estado;
        this.fecha_registro = fecha_registro;
        this.rol_id = rol_id;
    }

    public Usuario(String nombre_completo, String correo_electronico, String hash_contrasena, String estado, int rol_id) {
        this.nombre_completo = nombre_completo;
        this.correo_electronico = correo_electronico;
        this.hash_contrasena = hash_contrasena;
        this.estado = estado;
        this.rol_id = rol_id;
    }

    public int getId_usuario() {
        return id_usuario;
    }

    public void setId_usuario(int id_usuario) {
        this.id_usuario = id_usuario;
    }

    public String getNombre_completo() {
        return nombre_completo;
    }

    public void setNombre_completo(String nombre_completo) {
        this.nombre_completo = nombre_completo;
    }

    public String getCorreo_electronico() {
        return correo_electronico;
    }

    public void setCorreo_electronico(String correo_electronico) {
        this.correo_electronico = correo_electronico;
    }

    public String getHash_contrasena() {
        return hash_contrasena;
    }

    public void setHash_contrasena(String hash_contrasena) {
        this.hash_contrasena = hash_contrasena;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public Timestamp getFecha_registro() {
        return fecha_registro;
    }

    public void setFecha_registro(Timestamp fecha_registro) {
        this.fecha_registro = fecha_registro;
    }

    public int getRol_id() {
        return rol_id;
    }

    public void setRol_id(int rol_id) {
        this.rol_id = rol_id;
    }

    @Override
    public String toString() {
        return "Usuario{" +
                "id_usuario=" + id_usuario +
                ", nombre_completo='" + nombre_completo + '\'' +
                ", correo_electronico='" + correo_electronico + '\'' +
                ", estado='" + estado + '\'' +
                ", fecha_registro=" + fecha_registro +
                ", rol_id=" + rol_id +
                '}';
    }
}

