package colegio.modelo;

import colegio.Validacion;

/** Clase HIJA: hereda nombre, identificación y credenciales de Persona. */
public final class Estudiante extends Persona {
    private final String tipoDocumento;
    private final String aula;

    public Estudiante(String nombre, String identificacion, String claveHash, String tipoDocumento, String aula) {
        super(nombre, Validacion.documento(identificacion), claveHash);
        if (!"CC".equals(tipoDocumento) && !"TI".equals(tipoDocumento)) {
            throw new IllegalArgumentException("Selecciona cédula o tarjeta de identidad.");
        }
        this.tipoDocumento = tipoDocumento;
        this.aula = Validacion.aula(aula);
    }

    public String getTipoDocumento() { return tipoDocumento; }
    public String getAula() { return aula; }
    @Override public String getRol() { return "Estudiante"; }
}
