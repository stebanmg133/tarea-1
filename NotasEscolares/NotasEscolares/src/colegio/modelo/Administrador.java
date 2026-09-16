package colegio.modelo;

/** Cuenta principal. No se puede crear desde el formulario de registro. */
public final class Administrador extends Persona {
    public Administrador(String claveHash) { super("Administrador", "admin", claveHash); }
    @Override public String getRol() { return "Administrador"; }
}
