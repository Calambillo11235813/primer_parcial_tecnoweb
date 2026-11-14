package uagrm.feder.Entity;

import java.sql.Timestamp;

/**
 * Entidad del Dominio: PaginaInstitucional.
 * Representa secciones informativas de FEDER (Misión, Visión, Reseña, Objetivos, Contacto).
 */
public class PaginaInstitucional {

    private int id_pagina;
    private String titulo;
    private String contenido;
    private Timestamp ultima_actualizacion;
    private int usuario_id;

    public PaginaInstitucional() {
    }

    public PaginaInstitucional(int id_pagina, String titulo, String contenido,
            Timestamp ultima_actualizacion, int usuario_id) {
        this.id_pagina = id_pagina;
        this.titulo = titulo;
        this.contenido = contenido;
        this.ultima_actualizacion = ultima_actualizacion;
        this.usuario_id = usuario_id;
    }

    public PaginaInstitucional(String titulo, String contenido, int usuario_id) {
        this.titulo = titulo;
        this.contenido = contenido;
        this.usuario_id = usuario_id;
    }

    public int getId_pagina() {
        return id_pagina;
    }

    public void setId_pagina(int id_pagina) {
        this.id_pagina = id_pagina;
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

    public Timestamp getUltima_actualizacion() {
        return ultima_actualizacion;
    }

    public void setUltima_actualizacion(Timestamp ultima_actualizacion) {
        this.ultima_actualizacion = ultima_actualizacion;
    }

    public int getUsuario_id() {
        return usuario_id;
    }

    public void setUsuario_id(int usuario_id) {
        this.usuario_id = usuario_id;
    }

    @Override
    public String toString() {
        return "PaginaInstitucional{"
                + "id_pagina=" + id_pagina
                + ", titulo='" + titulo + '\''
                + ", contenido='" + contenido + '\''
                + ", ultima_actualizacion=" + ultima_actualizacion
                + ", usuario_id=" + usuario_id
                + '}';
    }
}
