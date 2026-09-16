package colegio;

import colegio.datos.BaseDatos;
import colegio.modelo.*;
import colegio.seguridad.Seguridad;
import colegio.web.ServidorWeb;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

/** Pruebas sin JUnit ni librerías externas. Solo utilizan datos ficticios y carpetas temporales. */
public final class Pruebas {
    private static int comprobaciones = 0;
    private static final String CLAVE = "Prueba123!";
    private static final String PROFESOR = "1000000001";
    private static final String ESTUDIANTE = "1000000002";
    private static final String OTRO = "1000000003";
    private static final String ADMIN_CLAVE = System.getenv("COLEGIO_ADMIN_CLAVE") == null || System.getenv("COLEGIO_ADMIN_CLAVE").isBlank()
            ? "Admin123!" : System.getenv("COLEGIO_ADMIN_CLAVE");

    private Pruebas() { }

    public static void main(String[] args) throws Exception {
        modelos();
        persistencia();
        web();
        System.out.println("\nRESULTADO: " + comprobaciones + " comprobaciones correctas. Ningun dato real fue modificado.");
    }

    private static void comprobar(boolean condicion, String nombre) {
        if (!condicion) throw new AssertionError("FALLO: " + nombre);
        comprobaciones++;
        System.out.println("OK - " + nombre);
    }

    private static void invalido(Runnable accion, String nombre) {
        boolean fallo = false;
        try { accion.run(); } catch (IllegalArgumentException e) { fallo = true; }
        comprobar(fallo, nombre);
    }

    private static void modelos() {
        Persona persona = new Estudiante("Ana Ejemplo", ESTUDIANTE, "hash", "TI", "10 a");
        comprobar(persona instanceof Estudiante, "Estudiante hereda de Persona");
        comprobar(persona.getNombre().equals("Ana Ejemplo"), "Datos heredados");
        comprobar(persona.getRol().equals("Estudiante"), "Polimorfismo en getRol");
        comprobar(((Estudiante) persona).getAula().equals("10A"), "Normalizacion del aula");
        comprobar(Validacion.normalizar(" Matemáticas ").equals("matematicas"), "Busqueda sin tildes ni mayusculas");
        var notas = Validacion.notas(List.of("4", "3,5", "5.0", " "));
        comprobar(notas.size() == 3, "Casillas vacias no cuentan");
        comprobar(RegistroNotas.promedio(notas).toPlainString().equals("4.17"), "Promedio simple con decimales");
        comprobar(RegistroNotas.estado(notas).equals("Ganó"), "Aprueba con promedio superior a 3");
        comprobar(RegistroNotas.estado(Validacion.notas(List.of("3"))).equals("Ganó"), "Aprueba con promedio exacto 3");
        comprobar(RegistroNotas.estado(Validacion.notas(List.of("2", "2.5"))).equals("Perdió"), "Pierde con promedio inferior a 3");
        comprobar(RegistroNotas.estado(List.of()).equals("Sin calificar"), "Sin notas no aparece como perdido");
        comprobar(RegistroNotas.promedio(List.of()) == null, "Sin notas no hay division por cero");
        comprobar(RegistroNotas.promedio(Validacion.notas(List.of("0", "5"))).toPlainString().equals("2.50"), "Cero si cuenta como nota");
        var limite = Validacion.notas(List.of("2.99", "3"));
        comprobar(RegistroNotas.promedio(limite).toPlainString().equals("3.00"), "Redondeo visual a dos decimales");
        comprobar(RegistroNotas.estado(limite).equals("Perdió"), "2.995 no aprueba por redondeo");
        comprobar(RegistroNotas.estado(Validacion.notas(List.of("2.9", "3.1"))).equals("Ganó"), "Comparacion decimal exacta");
        for (String valor : List.of("5.1", "-1", "NaN", "Infinity", "abc", "3e0", "3,4.5")) {
            invalido(() -> Validacion.notas(List.of(valor)), "Rechaza nota invalida: " + valor);
        }
        invalido(() -> Validacion.notas(List.of("", " ")), "Rechaza lista sin ninguna nota");
        invalido(() -> new Estudiante("Ana", ESTUDIANTE, "h", "OTRO", "10A"), "Rechaza tipo de documento desconocido");
        invalido(() -> new Profesor("", PROFESOR, "h", "10A"), "Rechaza nombre vacio");
        invalido(() -> Validacion.documento("abc123"), "Documento solo numerico");
        invalido(() -> Validacion.clave("123"), "Contrasena demasiado corta");
        String hash = Seguridad.hash(CLAVE);
        comprobar(!hash.contains(CLAVE), "El hash no contiene la contrasena en texto");
        comprobar(Seguridad.verificar(CLAVE, hash), "Autenticacion con contrasena correcta");
        comprobar(!Seguridad.verificar("Incorrecta!", hash), "Rechaza contrasena incorrecta");
        comprobar(!Seguridad.hash(CLAVE).equals(hash), "Cada hash utiliza una sal aleatoria");
        comprobar(!Seguridad.verificar(CLAVE, "dato-invalido"), "Hash invalido no autentica");
        RegistroNotas registro = new RegistroNotas(ESTUDIANTE, "Ciencias", notas);
        boolean inmutable = false;
        try { registro.getNotas().add(BigDecimal.ONE); } catch (UnsupportedOperationException e) { inmutable = true; }
        comprobar(inmutable, "Encapsulamiento de la lista de notas");
    }

    private static void persistencia() throws Exception {
        Path carpeta = Files.createTempDirectory("notas-pruebas-modelo-");
        try {
            try (BaseDatos db = new BaseDatos(carpeta)) {
                comprobar(db.persona("admin") instanceof Administrador, "Cuenta admin creada al iniciar");
                comprobar(Seguridad.verificar(ADMIN_CLAVE, db.persona("admin").getClaveHash()), "Clave inicial del administrador");
                db.registrar(new Profesor("Laura Ejemplo", PROFESOR, Seguridad.hash(CLAVE), "10A"));
                db.registrar(new Estudiante("Ana Ejemplo", ESTUDIANTE, Seguridad.hash(CLAVE), "TI", "10A"));
                comprobar(db.profesores().size() == 1 && db.estudiantes().size() == 1, "Registro de ambas clases de persona");
                boolean duplicado = false;
                try { db.registrar(new Estudiante("Repetido", PROFESOR, "hash", "CC", "10A")); }
                catch (IllegalArgumentException e) { duplicado = true; }
                comprobar(duplicado, "Identificacion unica incluso entre roles");
                db.guardarNotas(new RegistroNotas(ESTUDIANTE, "Matemáticas", Validacion.notas(List.of("4", "3", "5"))));
                db.guardarNotas(new RegistroNotas(ESTUDIANTE, "matematicas", Validacion.notas(List.of("2", "3"))));
                comprobar(db.materias(ESTUDIANTE).size() == 1, "Editar materia no crea duplicados por tildes");
                comprobar(db.materia(ESTUDIANTE, "MATEMATICAS").getPromedio().toPlainString().equals("2.50"), "Edicion reemplaza notas anteriores");
                db.cambiarClave("admin", Seguridad.hash("Nueva123!"));
                boolean bloqueada = false;
                try (BaseDatos segunda = new BaseDatos(carpeta)) { segunda.estudiantes(); }
                catch (IOException e) { bloqueada = true; }
                comprobar(bloqueada, "Evita dos procesos escribiendo los mismos archivos");
                for (String archivo : List.of("administrador.properties", "profesores.properties", "estudiantes.properties", "notas.properties")) {
                    comprobar(Files.isRegularFile(carpeta.resolve(archivo)), "Archivo persistente: " + archivo);
                }
            }
            try (BaseDatos reiniciada = new BaseDatos(carpeta)) {
                comprobar(reiniciada.profesores().size() == 1, "Profesor conservado despues de reiniciar");
                comprobar(reiniciada.estudiantes().size() == 1, "Estudiante conservado despues de reiniciar");
                comprobar(reiniciada.materia(ESTUDIANTE, "matematicas").getPromedio().toPlainString().equals("2.50"), "Notas conservadas despues de reiniciar");
                comprobar(Seguridad.verificar("Nueva123!", reiniciada.persona("admin").getClaveHash()), "Cambio de contrasena conservado");
            }
            Files.writeString(carpeta.resolve("estudiantes.properties"), ESTUDIANTE + ".nombre=Ana\n");
            boolean corrupto = false;
            try (BaseDatos invalida = new BaseDatos(carpeta)) { invalida.estudiantes(); }
            catch (IOException e) { corrupto = true; }
            comprobar(corrupto, "Archivo incompleto produce un error, no un reinicio silencioso");
            comprobar(Files.readString(carpeta.resolve("estudiantes.properties")).contains(".nombre=Ana"), "No destruye el archivo defectuoso");
        } finally { borrar(carpeta); }
    }

    private static void web() throws Exception {
        Path carpeta = Files.createTempDirectory("notas-pruebas-web-");
        try {
            try (BaseDatos db = new BaseDatos(carpeta); ServidorWeb servidor = new ServidorWeb(db, 0)) {
                servidor.iniciar();
                String base = "http://127.0.0.1:" + servidor.getPuerto();
                Navegador anonimo = new Navegador(base);
                var acceso = anonimo.get("/login");
                comprobar(acceso.statusCode() == 200 && acceso.body().contains("Iniciar sesión"), "Pagina de inicio de sesion");
                comprobar(acceso.headers().firstValue("set-cookie").orElse("").contains("HttpOnly"), "Cookie de sesion protegida");
                comprobar(acceso.headers().firstValue("content-security-policy").isPresent(), "Politica de seguridad de la pagina");
                comprobar(anonimo.get("/admin").statusCode() == 303, "Sin login no entra al administrador");
                comprobar(anonimo.post("/login", "usuario", "admin", "clave", ADMIN_CLAVE).statusCode() == 403, "POST sin token CSRF rechazado");
                comprobar(anonimo.enviar("/login", "usuario", "admin", "clave", "Incorrecta!").statusCode() == 401, "Login incorrecto muestra error");
                comprobar(anonimo.enviar("/registro", "rol", "profesor", "nombre", "Laura Docente", "identificacion", PROFESOR,
                        "aula", "10 a", "clave", CLAVE, "confirmacion", CLAVE).statusCode() == 303, "Registro web de profesor");
                comprobar(anonimo.enviar("/registro", "rol", "estudiante", "nombre", "Ana Álvarez", "identificacion", ESTUDIANTE,
                        "tipoDocumento", "TI", "aula", "10A", "clave", CLAVE, "confirmacion", CLAVE).statusCode() == 303, "Registro web de estudiante con TI");
                comprobar(anonimo.enviar("/registro", "rol", "estudiante", "nombre", "Carlos Ejemplo", "identificacion", OTRO,
                        "tipoDocumento", "CC", "aula", "11B", "clave", CLAVE, "confirmacion", CLAVE).statusCode() == 303, "Registro web de estudiante con CC");
                comprobar(anonimo.enviar("/registro", "rol", "profesor", "nombre", "Duplicado", "identificacion", ESTUDIANTE,
                        "aula", "10A", "clave", CLAVE, "confirmacion", CLAVE).statusCode() == 400, "Web rechaza cuenta duplicada");
                comprobar(anonimo.enviar("/registro", "rol", "administrador", "nombre", "Falso Admin", "identificacion", "1000000009",
                        "aula", "10A", "clave", CLAVE, "confirmacion", CLAVE).statusCode() == 400, "No permite crear administradores desde registro");
                Navegador profesor = new Navegador(base);
                comprobar(profesor.login(PROFESOR, CLAVE).statusCode() == 303, "Inicio de sesion de profesor");
                var busqueda = profesor.get("/profesor?q=alvarez");
                comprobar(busqueda.statusCode() == 200 && busqueda.body().contains("Ana Álvarez"), "Busca nombre ignorando tildes");
                comprobar(!busqueda.body().contains("Carlos Ejemplo"), "Busqueda no revela otras aulas");
                comprobar(profesor.get("/profesor?q=" + ESTUDIANTE).body().contains("Ana Álvarez"), "Busca por identificacion");
                comprobar(profesor.get("/admin").statusCode() == 403, "Profesor no accede a admin");
                comprobar(profesor.get("/estudiante?id=" + OTRO).statusCode() == 403, "Profesor no lee fichas de otra aula");
                comprobar(profesor.enviar("/notas", "id", OTRO, "materia", "Ciencias", "modo", "crear", "nota", "5").statusCode() == 403,
                        "Profesor no modifica notas de otra aula");
                comprobar(profesor.get("/estudiante?id=" + ESTUDIANTE).body().contains("Sin calificar"), "Ficha inicial sin calificar");
                comprobar(profesor.enviar("/notas", "id", ESTUDIANTE, "materia", "Matemáticas", "modo", "crear",
                        "nota", "4", "nota", "3,5", "nota", "5", "nota", "").statusCode() == 303, "Guardar varias notas con decimales y blancos");
                comprobar(profesor.get("/estudiante?id=" + ESTUDIANTE).body().contains("4,17"), "El promedio se muestra en la ficha");
                comprobar(profesor.enviar("/notas", "id", ESTUDIANTE, "materia", "matematicas", "modo", "crear", "nota", "1").statusCode() == 400,
                        "Materia existente exige cargarla con Editar");
                comprobar(profesor.enviar("/notas", "id", ESTUDIANTE, "materia", "matematicas", "modo", "editar",
                        "nota", "2", "nota", "3").statusCode() == 303, "Modificar materia existente");
                comprobar(db.materia(ESTUDIANTE, "Matemáticas").getPromedio().toPlainString().equals("2.50"), "Edicion web persistida");
                var invalida = profesor.enviar("/notas", "id", ESTUDIANTE, "materia", "Matemáticas", "modo", "editar", "nota", "6");
                comprobar(invalida.statusCode() == 400, "Valida notas en el servidor");
                comprobar(invalida.body().contains("value='6'"), "Formulario invalido conserva lo escrito para corregirlo");
                comprobar(db.materia(ESTUDIANTE, "Matemáticas").getPromedio().toPlainString().equals("2.50"), "Error no altera las notas guardadas");
                comprobar(profesor.enviar("/notas", "id", ESTUDIANTE, "materia", "Ciencias", "modo", "crear", "nota", "5").statusCode() == 303,
                        "Permite una segunda materia");
                comprobar(profesor.get("/estudiante?id=" + ESTUDIANTE).body().contains("3,33"), "General usa todas las notas, no promedio de promedios");
                String xss = "<script>alert(1)</script>";
                comprobar(profesor.enviar("/notas", "id", ESTUDIANTE, "materia", xss, "modo", "crear", "nota", "4").statusCode() == 303,
                        "Materia con caracteres especiales se procesa como texto");
                String ficha = profesor.get("/estudiante?id=" + ESTUDIANTE).body();
                comprobar(!ficha.contains(xss) && ficha.contains("&lt;script&gt;"), "Escapa HTML para impedir inyeccion de scripts");
                Navegador estudiante = new Navegador(base);
                comprobar(estudiante.login(ESTUDIANTE, CLAVE).statusCode() == 303, "Inicio de sesion de estudiante");
                comprobar(estudiante.get("/estudiante?id=" + ESTUDIANTE).statusCode() == 200, "Estudiante consulta su propia ficha");
                comprobar(!estudiante.get("/estudiante?id=" + ESTUDIANTE).body().contains("id='form-notas'"), "Estudiante no ve el editor");
                comprobar(estudiante.get("/estudiante?id=" + OTRO).statusCode() == 403, "Estudiante no lee ficha ajena");
                comprobar(estudiante.enviar("/notas", "id", ESTUDIANTE, "materia", "Ciencias", "modo", "editar", "nota", "0").statusCode() == 403,
                        "Estudiante no modifica sus propias notas");
                Navegador admin = new Navegador(base);
                comprobar(admin.login("admin", ADMIN_CLAVE).statusCode() == 303, "Inicio de sesion del administrador");
                String panel = admin.get("/admin").body();
                comprobar(panel.contains("Aula 10A") && panel.contains("Aula 11B"), "Admin agrupa por aula");
                comprobar(panel.contains("Laura Docente") && panel.contains("Ana Álvarez"), "Admin ve profesores y estudiantes");
                comprobar(!admin.get("/admin?aula=10A").body().contains("Carlos Ejemplo"), "Filtro de aula del administrador");
                comprobar(admin.get("/estudiante?id=" + OTRO).statusCode() == 200, "Admin consulta cualquier aula");
                comprobar(admin.enviar("/notas", "id", ESTUDIANTE, "materia", "Ciencias", "modo", "editar", "nota", "0").statusCode() == 403,
                        "Admin es de consulta, no califica");
                Navegador otraSesion = new Navegador(base);
                otraSesion.login("admin", ADMIN_CLAVE);
                comprobar(admin.enviar("/clave", "actual", ADMIN_CLAVE, "nueva", "Cambiada123!", "confirmacion", "Cambiada123!").statusCode() == 303,
                        "Cambio de contrasena por la web");
                comprobar(otraSesion.get("/admin").statusCode() == 303, "Cambio de contrasena cierra otras sesiones");
                comprobar(admin.enviar("/salir").statusCode() == 303 && admin.get("/admin").statusCode() == 303, "Cerrar sesion retira el acceso");
                comprobar(admin.login("admin", ADMIN_CLAVE).statusCode() == 401, "La clave anterior deja de funcionar");
                comprobar(admin.login("admin", "Cambiada123!").statusCode() == 303, "La nueva clave permite entrar");
                comprobar(admin.get("/no-existe").statusCode() == 404, "Pagina desconocida devuelve 404");
            }
            try (BaseDatos db = new BaseDatos(carpeta); ServidorWeb servidor = new ServidorWeb(db, 0)) {
                servidor.iniciar();
                Navegador profesor = new Navegador("http://127.0.0.1:" + servidor.getPuerto());
                comprobar(profesor.login(PROFESOR, CLAVE).statusCode() == 303, "Cuenta web conservada al reiniciar el servidor");
                comprobar(profesor.get("/estudiante?id=" + ESTUDIANTE).body().contains("2,50"), "Calificaciones web conservadas al reiniciar");
            }
        } finally { borrar(carpeta); }
    }

    private static final class Navegador {
        final String base;
        final HttpClient cliente;
        String csrf = "";
        Navegador(String base) {
            this.base = base;
            cliente = HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
                    .followRedirects(HttpClient.Redirect.NEVER).build();
        }
        HttpResponse<String> get(String ruta) throws Exception {
            return recibir(HttpRequest.newBuilder(URI.create(base + ruta)).GET().build());
        }
        HttpResponse<String> post(String ruta, String... pares) throws Exception {
            StringJoiner cuerpo = new StringJoiner("&");
            for (int i = 0; i < pares.length; i += 2) cuerpo.add(URLEncoder.encode(pares[i], StandardCharsets.UTF_8)
                    + "=" + URLEncoder.encode(pares[i + 1], StandardCharsets.UTF_8));
            return recibir(HttpRequest.newBuilder(URI.create(base + ruta)).header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(cuerpo.toString())).build());
        }
        HttpResponse<String> enviar(String ruta, String... pares) throws Exception {
            get(ruta.equals("/notas") ? "/profesor" : ruta.equals("/salir") ? "/clave" : ruta);
            List<String> campos = new ArrayList<>(List.of("csrf", csrf));
            campos.addAll(List.of(pares));
            return post(ruta, campos.toArray(String[]::new));
        }
        HttpResponse<String> login(String usuario, String clave) throws Exception {
            get("/login");
            return post("/login", "csrf", csrf, "usuario", usuario, "clave", clave);
        }
        HttpResponse<String> recibir(HttpRequest peticion) throws Exception {
            HttpResponse<String> respuesta = cliente.send(peticion, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            Matcher matcher = Pattern.compile("name='csrf' value='([^']+)'").matcher(respuesta.body());
            if (matcher.find()) csrf = matcher.group(1);
            return respuesta;
        }
    }

    private static void borrar(Path carpeta) throws IOException {
        try (var rutas = Files.walk(carpeta)) {
            for (Path ruta : rutas.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(ruta);
        }
    }
}
