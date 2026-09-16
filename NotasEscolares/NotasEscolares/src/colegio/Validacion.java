package colegio;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Reglas compartidas: se validan también en Java, no solo en el navegador. */
public final class Validacion {
    private Validacion() { }

    public static String texto(String valor, String campo, int maximo) {
        String limpio = valor == null ? "" : valor.strip().replaceAll("\\s+", " ");
        if (limpio.isEmpty() || limpio.length() > maximo || limpio.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException(campo + ": escribe entre 1 y " + maximo + " caracteres.");
        }
        return limpio;
    }

    public static String documento(String valor) {
        String limpio = valor == null ? "" : valor.strip();
        if (!limpio.matches("[0-9]{5,20}")) {
            throw new IllegalArgumentException("La identificación debe tener entre 5 y 20 dígitos, sin puntos.");
        }
        return limpio;
    }

    public static String aula(String valor) {
        return texto(valor, "Aula", 30).toUpperCase(Locale.ROOT).replace(" ", "");
    }

    public static void clave(String valor) {
        if (valor == null || valor.length() < 8 || valor.length() > 128 || valor.isBlank()) {
            throw new IllegalArgumentException("La contraseña debe tener entre 8 y 128 caracteres.");
        }
    }

    /** Permite buscar sin distinguir mayúsculas ni tildes. */
    public static String normalizar(String valor) {
        return Normalizer.normalize(valor == null ? "" : valor.strip(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    public static List<BigDecimal> notas(List<String> valores) {
        List<BigDecimal> resultado = new ArrayList<>();
        for (String valor : valores) {
            String limpio = valor.strip();
            if (limpio.isEmpty()) continue; // Una casilla vacía NO cuenta como cero.
            if (limpio.length() > 20 || !limpio.matches("[0-9]+(?:[.,][0-9]+)?")) {
                throw new IllegalArgumentException("Nota no válida: usa un número entre 0 y 5, por ejemplo 3,5.");
            }
            BigDecimal nota = new BigDecimal(limpio.replace(',', '.'));
            if (nota.compareTo(BigDecimal.ZERO) < 0 || nota.compareTo(new BigDecimal("5")) > 0) {
                throw new IllegalArgumentException("Todas las notas deben estar entre 0 y 5.");
            }
            resultado.add(nota);
        }
        if (resultado.isEmpty()) throw new IllegalArgumentException("Escribe al menos una nota.");
        return resultado;
    }
}
