package uagrm.feder.Entity;

/**
 * Entidad del Dominio: RedSocial.
 * Representa un canal oficial de difusión de FEDER (Facebook, Instagram, TikTok, WhatsApp).
 */
public class RedSocial {

    private int id_red_social;
    private String plataforma;
    private String url_enlace;
    private String estado;
    private int usuario_id;

    public RedSocial() {
    }

    public RedSocial(int id_red_social, String plataforma, String url_enlace,
            String estado, int usuario_id) {
        this.id_red_social = id_red_social;
        this.plataforma = plataforma;
        this.url_enlace = url_enlace;
        this.estado = estado;
        this.usuario_id = usuario_id;
    }

    public RedSocial(String plataforma, String url_enlace, String estado, int usuario_id) {
        this.plataforma = plataforma;
        this.url_enlace = url_enlace;
        this.estado = estado;
        this.usuario_id = usuario_id;
    }

    public int getId_red_social() {
        return id_red_social;
    }

    public void setId_red_social(int id_red_social) {
        this.id_red_social = id_red_social;
    }

    public String getPlataforma() {
        return plataforma;
    }

    public void setPlataforma(String plataforma) {
        this.plataforma = plataforma;
    }

    public String getUrl_enlace() {
        return url_enlace;
    }

    public void setUrl_enlace(String url_enlace) {
        this.url_enlace = url_enlace;
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
        return "RedSocial{"
                + "id_red_social=" + id_red_social
                + ", plataforma='" + plataforma + '\''
                + ", url_enlace='" + url_enlace + '\''
                + ", estado='" + estado + '\''
                + ", usuario_id=" + usuario_id
                + '}';
    }
}
