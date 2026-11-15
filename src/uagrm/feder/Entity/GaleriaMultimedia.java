package uagrm.feder.Entity;

import java.sql.Timestamp;

/**
 * Entidad del Dominio: GaleriaMultimedia.
 * Representa una fotografía o enlace audiovisual asociado a una noticia (RF-02).
 */
public class GaleriaMultimedia {

    private int id_multimedia;
    private String titulo;
    private String tipo;
    private String url_archivo;
    private String descripcion;
    private Timestamp fecha_carga;
    private int noticia_id;

    public GaleriaMultimedia() {
    }

    public GaleriaMultimedia(int id_multimedia, String titulo, String tipo,
            String url_archivo, String descripcion, Timestamp fecha_carga, int noticia_id) {
        this.id_multimedia = id_multimedia;
        this.titulo = titulo;
        this.tipo = tipo;
        this.url_archivo = url_archivo;
        this.descripcion = descripcion;
        this.fecha_carga = fecha_carga;
        this.noticia_id = noticia_id;
    }

    public int getId_multimedia() {
        return id_multimedia;
    }

    public void setId_multimedia(int id_multimedia) {
        this.id_multimedia = id_multimedia;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getUrl_archivo() {
        return url_archivo;
    }

    public void setUrl_archivo(String url_archivo) {
        this.url_archivo = url_archivo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Timestamp getFecha_carga() {
        return fecha_carga;
    }

    public void setFecha_carga(Timestamp fecha_carga) {
        this.fecha_carga = fecha_carga;
    }

    public int getNoticia_id() {
        return noticia_id;
    }

    public void setNoticia_id(int noticia_id) {
        this.noticia_id = noticia_id;
    }

    @Override
    public String toString() {
        return "GaleriaMultimedia{"
                + "id_multimedia=" + id_multimedia
                + ", titulo='" + titulo + '\''
                + ", tipo='" + tipo + '\''
                + ", url_archivo='" + url_archivo + '\''
                + ", noticia_id=" + noticia_id
                + '}';
    }
}
