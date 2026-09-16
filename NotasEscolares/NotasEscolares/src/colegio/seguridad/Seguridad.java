package colegio.seguridad;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** Las contraseñas no se guardan como texto legible. */
public final class Seguridad {
    private static final SecureRandom AZAR = new SecureRandom();
    private static final int ITERACIONES = 210_000;
    private Seguridad() { }

    public static String token() {
        byte[] bytes = new byte[32];
        AZAR.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String hash(String clave) {
        byte[] sal = new byte[16];
        AZAR.nextBytes(sal);
        byte[] resultado = derivar(clave, sal, ITERACIONES);
        return ITERACIONES + "$" + Base64.getEncoder().encodeToString(sal)
                + "$" + Base64.getEncoder().encodeToString(resultado);
    }

    public static boolean verificar(String clave, String guardado) {
        try {
            String[] partes = guardado.split("\\$");
            if (partes.length != 3 || clave == null || clave.length() > 128) return false;
            int rondas = Integer.parseInt(partes[0]);
            if (rondas < 10_000 || rondas > 1_000_000) return false;
            byte[] sal = Base64.getDecoder().decode(partes[1]);
            byte[] esperado = Base64.getDecoder().decode(partes[2]);
            return MessageDigest.isEqual(esperado, derivar(clave, sal, rondas));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static byte[] derivar(String clave, byte[] sal, int rondas) {
        PBEKeySpec datos = new PBEKeySpec(clave.toCharArray(), sal, rondas, 256);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(datos).getEncoded();
        } catch (java.security.GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo proteger la contraseña.", e);
        } finally {
            datos.clearPassword();
        }
    }
}
