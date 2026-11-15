package uagrm.feder.Entity;

import java.sql.Date;

/**
 * Entidad del Dominio: Noticia.
 * Representa una nota de prensa, comunicado o artículo informativo (RF-01).
 */
public class Noticia {

    private int id_noticia;
    private String titulo;
    private String contenido;
    private Date fecha_publicacion;
    private String imagen_principal;
    private String etiquetas;
    private String estado;
    private int usuario_id;

    public Noticia() {
    }

    public Noticia(int id_noticia, String titulo, String contenido,
            Date fecha_publicacion, String imagen_principal, String etiquetas,
            String estado, int usuario_id) {
        this.id_noticia = id_noticia;
        this.titulo = titulo;
        this.contenido = contenido;
        this.fecha_publicacion = fecha_publicacion;
        this.imagen_principal = imagen_principal;
        this.etiquetas = etiquetas;
        this.estado = estado;
        this.usuario_id = usuario_id;
    }

    public int getId_noticia() {
        return id_noticia;
    }

    public void setId_noticia(int id_noticia) {
        this.id_noticia = id_noticia;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getContenido() {
        return contenido;
    }

    public void setContenido(String contenido) {
        this.contenido = contenido;
    }

    public Date getFecha_publicacion() {
        return fecha_publicacion;
    }

    public void setFecha_publicacion(Date fecha_publicacion) {
        this.fecha_publicacion = fecha_publicacion;
    }

    public String getImagen_principal() {
        return imagen_principal;
    }

    public void setImagen_principal(String imagen_principal) {
        this.imagen_principal = imagen_principal;
    }

    public String getEtiquetas() {
        return etiquetas;
    }

    public void setEtiquetas(String etiquetas) {
        this.etiquetas = etiquetas;
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
        return "Noticia{"
                + "id_noticia=" + id_noticia
                + ", titulo='" + titulo + '\''
                + ", fecha_publicacion=" + fecha_publicacion
                + ", estado='" + estado + '\''
                + ", usuario_id=" + usuario_id
                + '}';
    }
}
