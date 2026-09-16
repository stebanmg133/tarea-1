package colegio.web;

import colegio.Validacion;
import colegio.datos.BaseDatos;
import colegio.modelo.*;
import colegio.seguridad.Seguridad;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

/** Controlador: recibe peticiones, comprueba permisos y conecta modelos con vistas. */
public final class ServidorWeb implements AutoCloseable {
    private static final int MAX_FORMULARIO = 256 * 1024;
    private static final long DURACION_SESION = 30 * 60 * 1000L;
    private final BaseDatos db;
    private final HttpServer servidor;
    private final ExecutorService hilos = Executors.newFixedThreadPool(8);
    private final Map<String, Sesion> sesiones = new ConcurrentHashMap<>();
    private final int puerto;

    private static final class Sesion {
        final String token = Seguridad.token();
        final String csrf = Seguridad.token();
        final String usuario;
        volatile long ultimoAcceso = System.currentTimeMillis();
        Sesion(String usuario) { this.usuario = usuario; }
    }

    private static final class ErrorHttp extends RuntimeException {
        private static final long serialVersionUID = 1L;
        final int codigo;
        ErrorHttp(int codigo, String mensaje) { super(mensaje); this.codigo = codigo; }
    }

    public ServidorWeb(BaseDatos db, int puerto) throws IOException {
        this.db = db;
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", puerto), 0);
        this.puerto = servidor.getAddress().getPort();
        servidor.setExecutor(hilos);
        servidor.createContext("/", this::atender);
    }

    public void iniciar() { servidor.start(); }
    public int getPuerto() { return puerto; }

    private void atender(HttpExchange peticion) throws IOException {
        Sesion sesion = null;
        Persona persona = null;
        try {
            String host = peticion.getRequestHeaders().getFirst("Host");
            if (!("localhost:" + puerto).equalsIgnoreCase(host) && !("127.0.0.1:" + puerto).equals(host)) {
                throw new ErrorHttp(403, "Abre la aplicación desde localhost o 127.0.0.1.");
            }
            String ruta = peticion.getRequestURI().getPath();
            String metodo = peticion.getRequestMethod();
            if (!metodo.equals("GET") && !metodo.equals("POST")) {
                peticion.getResponseHeaders().set("Allow", "GET, POST");
                throw new ErrorHttp(405, "Método no permitido.");
            }
            if (metodo.equals("GET") && ruta.equals("/estilos.css")) {
                responder(peticion, 200, "text/css", Vistas.CSS); return;
            }
            if (metodo.equals("GET") && ruta.equals("/app.js")) {
                responder(peticion, 200, "text/javascript", Vistas.JS); return;
            }
            if (ruta.equals("/favicon.ico")) { peticion.sendResponseHeaders(204, -1); return; }
            sesion = obtenerSesion(peticion);
            persona = sesion.usuario == null ? null : db.persona(sesion.usuario);
            String rawQuery = peticion.getRequestURI().getRawQuery();
            if (rawQuery != null && rawQuery.length() > 8192) throw new ErrorHttp(414, "La consulta es demasiado larga.");
            Map<String, List<String>> consulta = parametros(rawQuery);
            Map<String, List<String>> formulario = Map.of();
            if (metodo.equals("POST")) {
                String tipo = peticion.getRequestHeaders().getFirst("Content-Type");
                if (tipo == null || !tipo.toLowerCase(Locale.ROOT).startsWith("application/x-www-form-urlencoded")) {
                    throw new ErrorHttp(415, "Tipo de formulario no admitido.");
                }
                byte[] cuerpo = peticion.getRequestBody().readNBytes(MAX_FORMULARIO + 1);
                if (cuerpo.length > MAX_FORMULARIO) throw new ErrorHttp(413, "El formulario es demasiado grande.");
                formulario = parametros(new String(cuerpo, StandardCharsets.UTF_8));
                if (!sesion.csrf.equals(valor(formulario, "csrf"))) {
                    throw new ErrorHttp(403, "El formulario venció. Vuelve al inicio e inténtalo de nuevo.");
                }
                String origen = peticion.getRequestHeaders().getFirst("Origin");
                if (origen != null && !origen.equals("http://localhost:" + puerto) && !origen.equals("http://127.0.0.1:" + puerto)) {
                    throw new ErrorHttp(403, "Origen de formulario no permitido.");
                }
            }
            if (persona == null && !Set.of("/", "/login", "/registro").contains(ruta)) {
                redirigir(peticion, "/login"); return;
            }
            switch (metodo + " " + ruta) {
                case "GET /" -> redirigir(peticion, inicio(persona));
                case "GET /login" -> {
                    if (persona != null) redirigir(peticion, inicio(persona));
                    else html(peticion, 200, Vistas.login(sesion.csrf, "", consulta.containsKey("registro")
                            ? "Cuenta creada. Inicia sesión con tu identificación y contraseña." : "", ""));
                }
                case "POST /login" -> ingresar(peticion, formulario, sesion);
                case "GET /registro" -> {
                    if (persona != null) redirigir(peticion, inicio(persona));
                    else html(peticion, 200, Vistas.registro(sesion.csrf, "profesor".equals(valor(consulta, "rol"))
                            ? "profesor" : "estudiante", Map.of(), ""));
                }
                case "POST /registro" -> {
                    if (persona != null) redirigir(peticion, inicio(persona));
                    else registrar(peticion, formulario, sesion);
                }
                case "GET /admin" -> {
                    if (!(persona instanceof Administrador administrador)) throw new ErrorHttp(403, "Esta sección es del administrador.");
                    html(peticion, 200, Vistas.admin(administrador, db, valor(consulta, "aula"), sesion.csrf));
                }
                case "GET /profesor" -> {
                    if (!(persona instanceof Profesor profesor)) throw new ErrorHttp(403, "Esta sección es de profesores.");
                    html(peticion, 200, Vistas.profesor(profesor, db, valor(consulta, "q"), sesion.csrf));
                }
                case "GET /estudiante" -> {
                    String id = valor(consulta, "id");
                    if (id.isBlank() && persona instanceof Estudiante) id = persona.getIdentificacion();
                    Estudiante estudiante = comprobarFicha(persona, id);
                    html(peticion, 200, Vistas.estudiante(persona, estudiante, db.materias(id), valor(consulta, "materia"),
                            null, "", "", consulta.containsKey("guardado") ? "Notas guardadas. Los promedios están actualizados." : "", sesion.csrf));
                }
                case "POST /notas" -> guardarNotas(peticion, persona, formulario, sesion);
                case "GET /clave" -> html(peticion, 200, Vistas.clave(persona, sesion.csrf, "",
                        consulta.containsKey("cambiada") ? "Contraseña actualizada correctamente." : ""));
                case "POST /clave" -> cambiarClave(peticion, persona, formulario, sesion);
                case "POST /salir" -> {
                    sesiones.remove(sesion.token);
                    peticion.getResponseHeaders().set("Set-Cookie", "SESION=; Path=/; HttpOnly; SameSite=Strict; Max-Age=0");
                    redirigir(peticion, "/login");
                }
                default -> throw new ErrorHttp(404, "La página solicitada no existe.");
            }
        } catch (ErrorHttp e) {
            html(peticion, e.codigo, Vistas.error(e.codigo, e.getMessage(), persona, sesion == null ? "" : sesion.csrf));
        } catch (IllegalArgumentException e) {
            html(peticion, 400, Vistas.error(400, "Los datos enviados no son válidos.", persona, sesion == null ? "" : sesion.csrf));
        } catch (Exception e) {
            System.err.println("Error al atender la peticion: " + e.getMessage());
            e.printStackTrace();
            html(peticion, 500, Vistas.error(500, "No se pudo completar la operación. Revisa la consola y los permisos de la carpeta datos.",
                    persona, sesion == null ? "" : sesion.csrf));
        } finally { peticion.close(); }
    }

    private void ingresar(HttpExchange peticion, Map<String, List<String>> form, Sesion anterior) throws IOException {
        String usuario = valor(form, "usuario").strip();
        Persona persona = db.persona(usuario);
        if (persona == null || !Seguridad.verificar(valor(form, "clave"), persona.getClaveHash())) {
            html(peticion, 401, Vistas.login(anterior.csrf, "Usuario o contraseña incorrectos.", "", usuario));
            return;
        }
        sesiones.remove(anterior.token);
        nuevaSesion(peticion, usuario);
        redirigir(peticion, inicio(persona));
    }

    private void registrar(HttpExchange peticion, Map<String, List<String>> form, Sesion sesion) throws IOException {
        String rol = valor(form, "rol");
        try {
            if (!rol.equals("profesor") && !rol.equals("estudiante")) throw new IllegalArgumentException("Selecciona profesor o estudiante.");
            String clave = valor(form, "clave");
            Validacion.clave(clave);
            if (!clave.equals(valor(form, "confirmacion"))) throw new IllegalArgumentException("Las contraseñas no coinciden.");
            String id = Validacion.documento(valor(form, "identificacion"));
            if (db.persona(id) != null) throw new IllegalArgumentException("Ya existe una cuenta con esa identificación.");
            String hash = Seguridad.hash(clave);
            Persona persona = rol.equals("profesor")
                    ? new Profesor(valor(form, "nombre"), id, hash, valor(form, "aula"))
                    : new Estudiante(valor(form, "nombre"), id, hash, valor(form, "tipoDocumento"), valor(form, "aula"));
            db.registrar(persona);
            redirigir(peticion, "/login?registro=1");
        } catch (IllegalArgumentException e) {
            Map<String, String> datos = new HashMap<>();
            for (String campo : List.of("nombre", "identificacion", "aula", "tipoDocumento")) datos.put(campo, valor(form, campo));
            html(peticion, 400, Vistas.registro(sesion.csrf, rol, datos, e.getMessage()));
        }
    }

    private Estudiante comprobarFicha(Persona usuario, String id) {
        Persona buscado = db.persona(id);
        if (!(buscado instanceof Estudiante estudiante)) throw new ErrorHttp(404, "El estudiante no existe.");
        boolean autorizado = usuario instanceof Administrador
                || usuario instanceof Estudiante && usuario.getIdentificacion().equals(id)
                || usuario instanceof Profesor profesor && profesor.getAula().equals(estudiante.getAula());
        if (!autorizado) throw new ErrorHttp(403, "No tienes permiso para consultar este estudiante.");
        return estudiante;
    }

    private void guardarNotas(HttpExchange peticion, Persona usuario, Map<String, List<String>> form, Sesion sesion) throws IOException {
        if (!(usuario instanceof Profesor)) throw new ErrorHttp(403, "Solo los profesores pueden guardar notas.");
        String id = valor(form, "id");
        Estudiante estudiante = comprobarFicha(usuario, id);
        String materia = valor(form, "materia");
        List<String> borrador = form.getOrDefault("nota", List.of());
        try {
            materia = Validacion.texto(materia, "Materia", 60);
            if (db.materia(id, materia) != null && !valor(form, "modo").equals("editar")) {
                throw new IllegalArgumentException("Esta materia ya existe. Pulsa Editar en su fila para cargar y modificar sus notas.");
            }
            RegistroNotas registro = new RegistroNotas(id, materia, Validacion.notas(borrador));
            db.guardarNotas(registro);
            redirigir(peticion, "/estudiante?id=" + Vistas.url(id) + "&materia=" + Vistas.url(materia) + "&guardado=1#editor");
        } catch (IllegalArgumentException e) {
            html(peticion, 400, Vistas.estudiante(usuario, estudiante, db.materias(id), materia, borrador,
                    valor(form, "modo"), e.getMessage(), "", sesion.csrf));
        }
    }

    private void cambiarClave(HttpExchange peticion, Persona persona, Map<String, List<String>> form, Sesion sesion) throws IOException {
        try {
            if (!Seguridad.verificar(valor(form, "actual"), persona.getClaveHash())) throw new IllegalArgumentException("La contraseña actual es incorrecta.");
            String nueva = valor(form, "nueva");
            Validacion.clave(nueva);
            if (!nueva.equals(valor(form, "confirmacion"))) throw new IllegalArgumentException("Las contraseñas nuevas no coinciden.");
            db.cambiarClave(persona.getIdentificacion(), Seguridad.hash(nueva));
            sesiones.entrySet().removeIf(e -> persona.getIdentificacion().equals(e.getValue().usuario));
            nuevaSesion(peticion, persona.getIdentificacion());
            redirigir(peticion, "/clave?cambiada=1");
        } catch (IllegalArgumentException e) {
            html(peticion, 400, Vistas.clave(persona, sesion.csrf, e.getMessage(), ""));
        }
    }

    private static String inicio(Persona persona) {
        if (persona == null) return "/login";
        if (persona instanceof Administrador) return "/admin";
        if (persona instanceof Profesor) return "/profesor";
        return "/estudiante?id=" + Vistas.url(persona.getIdentificacion());
    }

    private Sesion obtenerSesion(HttpExchange peticion) {
        long ahora = System.currentTimeMillis();
        sesiones.entrySet().removeIf(e -> ahora - e.getValue().ultimoAcceso > DURACION_SESION);
        String cookie = peticion.getRequestHeaders().getFirst("Cookie");
        if (cookie != null) {
            for (String parte : cookie.split(";")) {
                String[] par = parte.strip().split("=", 2);
                if (par.length == 2 && par[0].equals("SESION")) {
                    String token = par[1];
                    // Algunos clientes HTTP envian el valor de la cookie entre comillas.
                    if (token.length() >= 2 && token.startsWith("\"") && token.endsWith("\"")) {
                        token = token.substring(1, token.length() - 1);
                    }
                    Sesion encontrada = sesiones.get(token);
                    if (encontrada != null) {
                        encontrada.ultimoAcceso = ahora;
                        enviarCookie(peticion, encontrada);
                        return encontrada;
                    }
                }
            }
        }
        return nuevaSesion(peticion, null);
    }

    private Sesion nuevaSesion(HttpExchange peticion, String usuario) {
        Sesion nueva = new Sesion(usuario);
        sesiones.put(nueva.token, nueva);
        enviarCookie(peticion, nueva);
        return nueva;
    }

    private static void enviarCookie(HttpExchange peticion, Sesion sesion) {
        peticion.getResponseHeaders().set("Set-Cookie", "SESION=" + sesion.token + "; Path=/; HttpOnly; SameSite=Strict; Max-Age=1800");
    }

    private static Map<String, List<String>> parametros(String contenido) {
        Map<String, List<String>> resultado = new LinkedHashMap<>();
        if (contenido == null || contenido.isEmpty()) return resultado;
        for (String parte : contenido.split("&")) {
            String[] par = parte.split("=", 2);
            String nombre = URLDecoder.decode(par[0], StandardCharsets.UTF_8);
            String valor = par.length == 2 ? URLDecoder.decode(par[1], StandardCharsets.UTF_8) : "";
            resultado.computeIfAbsent(nombre, k -> new ArrayList<>()).add(valor);
        }
        return resultado;
    }

    private static String valor(Map<String, List<String>> datos, String campo) {
        List<String> valores = datos.get(campo);
        return valores == null || valores.isEmpty() ? "" : valores.get(0);
    }

    private static void cabeceras(HttpExchange peticion) {
        peticion.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        peticion.getResponseHeaders().set("X-Frame-Options", "DENY");
        peticion.getResponseHeaders().set("Referrer-Policy", "same-origin");
        peticion.getResponseHeaders().set("Cache-Control", "no-store");
        peticion.getResponseHeaders().set("Content-Security-Policy",
                "default-src 'none'; style-src 'self'; script-src 'self'; img-src 'self'; base-uri 'none'; form-action 'self'; frame-ancestors 'none'");
    }

    private static void html(HttpExchange peticion, int codigo, String contenido) throws IOException {
        responder(peticion, codigo, "text/html", contenido);
    }

    private static void responder(HttpExchange peticion, int codigo, String tipo, String contenido) throws IOException {
        cabeceras(peticion);
        byte[] bytes = contenido.getBytes(StandardCharsets.UTF_8);
        peticion.getResponseHeaders().set("Content-Type", tipo + "; charset=UTF-8");
        peticion.sendResponseHeaders(codigo, bytes.length);
        peticion.getResponseBody().write(bytes);
    }

    private static void redirigir(HttpExchange peticion, String ruta) throws IOException {
        cabeceras(peticion);
        peticion.getResponseHeaders().set("Location", ruta);
        peticion.sendResponseHeaders(303, -1);
    }

    @Override public void close() {
        servidor.stop(1);
        hilos.shutdownNow();
        sesiones.clear();
    }
}
