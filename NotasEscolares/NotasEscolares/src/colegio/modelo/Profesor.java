package colegio.modelo;

import colegio.Validacion;

/** Clase HIJA: un profesor pertenece a un aula y puede calificarla. */
public final class Profesor extends Persona {
    private final String aula;

    public Profesor(String nombre, String cedula, String claveHash, String aula) {
        super(nombre, Validacion.documento(cedula), claveHash);
        this.aula = Validacion.aula(aula);
    }

    public String getAula() { return aula; }
    @Override public String getRol() { return "Profesor"; }
}
