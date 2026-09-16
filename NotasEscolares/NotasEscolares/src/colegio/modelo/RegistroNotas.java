package colegio.modelo;

import colegio.Validacion;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** Composición: un registro agrupa las notas de una materia de un estudiante. */
public final class RegistroNotas {
    private final String estudianteId;
    private final String materia;
    private final List<BigDecimal> notas;

    public RegistroNotas(String estudianteId, String materia, List<BigDecimal> notas) {
        this.estudianteId = Validacion.documento(estudianteId);
        this.materia = Validacion.texto(materia, "Materia", 60);
        if (notas == null || notas.isEmpty()) throw new IllegalArgumentException("Escribe al menos una nota.");
        for (BigDecimal nota : notas) {
            if (nota == null || nota.compareTo(BigDecimal.ZERO) < 0 || nota.compareTo(new BigDecimal("5")) > 0) {
                throw new IllegalArgumentException("Las notas deben estar entre 0 y 5.");
            }
        }
        this.notas = List.copyOf(notas); // Evita que otra clase cambie la lista sin guardar.
    }

    public String getEstudianteId() { return estudianteId; }
    public String getMateria() { return materia; }
    public List<BigDecimal> getNotas() { return notas; }
    public BigDecimal getPromedio() { return promedio(notas); }
    public String getEstado() { return estado(notas); }

    private static BigDecimal suma(List<BigDecimal> notas) {
        BigDecimal total = BigDecimal.ZERO;
        for (BigDecimal nota : notas) total = total.add(nota);
        return total;
    }

    /** Media aritmética: todas las notas tienen el mismo peso. */
    public static BigDecimal promedio(List<BigDecimal> notas) {
        if (notas.isEmpty()) return null;
        return suma(notas).divide(BigDecimal.valueOf(notas.size()), 2, RoundingMode.HALF_UP);
    }

    /** Compara ANTES de redondear; así 2,999 no se convierte en aprobado. */
    public static String estado(List<BigDecimal> notas) {
        if (notas.isEmpty()) return "Sin calificar";
        BigDecimal minimo = new BigDecimal("3").multiply(BigDecimal.valueOf(notas.size()));
        return suma(notas).compareTo(minimo) >= 0 ? "Ganó" : "Perdió";
    }
}
