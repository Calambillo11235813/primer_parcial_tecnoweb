package uagrm.feder.Entity;

/**
 * Entidad del Dominio: Testimonio.
 * Representa una historia de éxito o testimonio de artesanos y comunidades (RF-08).
 */
public class Testimonio {

    private int id_testimonio;
    private String titulo;
    private String texto;
    private String fotografia;
    private String enlace_audiovisual;
    private String estado;
    private int comunidad_id;

    public Testimonio() {
    }

    public Testimonio(int id_testimonio, String titulo, String texto,
            String fotografia, String enlace_audiovisual, String estado, int comunidad_id) {
        this.id_testimonio = id_testimonio;
        this.titulo = titulo;
        this.texto = texto;
        this.fotografia = fotografia;
        this.enlace_audiovisual = enlace_audiovisual;
        this.estado = estado;
        this.comunidad_id = comunidad_id;
    }

    public int getId_testimonio() {
        return id_testimonio;
    }

    public void setId_testimonio(int id_testimonio) {
        this.id_testimonio = id_testimonio;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getTexto() {
        return texto;
    }

    public void setTexto(String texto) {
        this.texto = texto;
    }

    public String getFotografia() {
        return fotografia;
    }

    public void setFotografia(String fotografia) {
        this.fotografia = fotografia;
    }

    public String getEnlace_audiovisual() {
        return enlace_audiovisual;
    }

    public void setEnlace_audiovisual(String enlace_audiovisual) {
        this.enlace_audiovisual = enlace_audiovisual;
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
        return "Testimonio{"
                + "id_testimonio=" + id_testimonio
                + ", titulo='" + titulo + '\''
                + ", estado='" + estado + '\''
                + ", comunidad_id=" + comunidad_id
                + '}';
    }
}
