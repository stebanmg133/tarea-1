package colegio;

import colegio.datos.BaseDatos;
import colegio.web.ServidorWeb;
import java.io.IOException;
import java.net.BindException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Ejecuta esta clase en IntelliJ IDEA y abre la dirección indicada en la consola. */
public final class Main {
    private Main() { }

    public static void main(String[] args) {
        BaseDatos db = null;
        try {
            int puerto = args.length == 0 ? 8080 : Integer.parseInt(args[0]);
            if (puerto < 1024 || puerto > 65535) throw new IllegalArgumentException("El puerto debe estar entre 1024 y 65535.");
            Path carpeta = Path.of(System.getProperty("colegio.datos", "datos"));
            boolean primeraVez = !Files.exists(carpeta.resolve("administrador.properties"));
            db = new BaseDatos(carpeta);
            ServidorWeb web = new ServidorWeb(db, puerto);
            BaseDatos datos = db;
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                web.close();
                try { datos.close(); } catch (IOException e) { System.err.println("No se pudo cerrar el archivo de datos."); }
            }));
            web.iniciar();
            System.out.println("\n=============================================");
            System.out.println("           NOTAS ESCOLARES - JAVA");
            System.out.println("=============================================");
            System.out.println("Abre: http://localhost:" + web.getPuerto());
            System.out.println("Datos: " + datos.getCarpeta());
            System.out.println("Administrador: usuario admin");
            if (primeraVez) {
                String personalizada = System.getenv("COLEGIO_ADMIN_CLAVE");
                System.out.println(personalizada == null || personalizada.isBlank()
                        ? "Clave inicial: Admin123! (cambiala al ingresar)"
                        : "Clave inicial: la definida en COLEGIO_ADMIN_CLAVE");
            }
            System.out.println("Deten con el boton Stop de IntelliJ o Ctrl+C.\n");
        } catch (Exception e) {
            if (db != null) try { db.close(); } catch (IOException ignorada) { }
            if (e instanceof BindException) System.err.println("El puerto esta ocupado. Deten la otra ejecucion o usa otro puerto, por ejemplo 8081.");
            else System.err.println("No se pudo iniciar: " + e.getMessage());
            System.exit(1);
        }
    }
}
