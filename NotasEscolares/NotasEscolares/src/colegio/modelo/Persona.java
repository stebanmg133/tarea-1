package colegio.modelo;

import colegio.Validacion;

/** Clase PADRE: los tres tipos de usuario heredan sus datos comunes. */
public abstract class Persona {
    private final String nombre;
    private final String identificacion;
    private String claveHash;

    protected Persona(String nombre, String identificacion, String claveHash) {
        this.nombre = Validacion.texto(nombre, "Nombre", 80);
        this.identificacion = identificacion;
        this.claveHash = claveHash;
    }

    public String getNombre() { return nombre; }
    public String getIdentificacion() { return identificacion; }
    public String getClaveHash() { return claveHash; }
    public void setClaveHash(String claveHash) { this.claveHash = claveHash; }

    // Polimorfismo: cada clase hija devuelve un rol diferente.
    public abstract String getRol();
}
