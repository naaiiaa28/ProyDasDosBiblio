package com.example.biblioteca;

public class MediaItem {
    //las pelis/series que se van añadiendo son objetos con sus set y get (mitika clase de java)
    private int id;
    private String titulo;
    private String tipo;
    private String genero;
    private String comentario;
    private String resumen;
    private String estado;
    private String fechaAdicion;
    private float puntuacion;
    private int temporadasTotales;
    private int temporadaActual;
    private int capituloActual;
    private String rutaImagen;

    public MediaItem() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getGenero() { return genero; }
    public void setGenero(String genero) { this.genero = genero; }

    public String getComentario() { return comentario; }
    public void setComentario(String comentario) { this.comentario = comentario; }

    public String getResumen() { return resumen; }
    public void setResumen(String resumen) { this.resumen = resumen; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getFechaAdicion() { return fechaAdicion; }
    public void setFechaAdicion(String fechaAdicion) { this.fechaAdicion = fechaAdicion; }

    public float getPuntuacion() { return puntuacion; }
    public void setPuntuacion(float puntuacion) { this.puntuacion = puntuacion; }

    public int getTemporadasTotales() { return temporadasTotales; }
    public void setTemporadasTotales(int temporadasTotales) { this.temporadasTotales = temporadasTotales; }

    public int getTemporadaActual() { return temporadaActual; }
    public void setTemporadaActual(int temporadaActual) { this.temporadaActual = temporadaActual; }

    public int getCapituloActual() { return capituloActual; }
    public void setCapituloActual(int capituloActual) { this.capituloActual = capituloActual; }

    public String getRutaImagen() { return rutaImagen; }
    public void setRutaImagen(String rutaImagen) { this.rutaImagen = rutaImagen; }

}