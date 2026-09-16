package colegio.datos;

import colegio.Validacion;
import colegio.modelo.*;
import colegio.seguridad.Seguridad;
import java.io.*;
import java.math.BigDecimal;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

/** Persistencia sencilla en archivos .properties (no necesita instalar una base de datos). */
public final class BaseDatos implements AutoCloseable {
    private final Path carpeta;
    private final Map<String, Persona> personas = new LinkedHashMap<>();
    private final Map<String, RegistroNotas> registros = new LinkedHashMap<>();
    private final FileChannel canal;
    private final FileLock bloqueo;

    public BaseDatos(Path carpeta) throws IOException {
        this.carpeta = carpeta.toAbsolutePath().normalize();
        Files.createDirectories(this.carpeta);
        canal = FileChannel.open(this.carpeta.resolve(".bloqueo"), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        FileLock temporal;
        try { temporal = canal.tryLock(); }
        catch (OverlappingFileLockException e) { temporal = null; }
        if (temporal == null) {
            canal.close();
            throw new IOException("Estos archivos ya están abiertos. Detén la otra ejecución del programa.");
        }
        bloqueo = temporal;
        try { cargar(); }
        catch (IOException | RuntimeException e) {
            close();
            throw new IOException("No se pudieron leer los datos. No se han borrado ni reiniciado. " + e.getMessage(), e);
        }
    }

    private void cargar() throws IOException {
        Path archivoAdmin = carpeta.resolve("administrador.properties");
        if (!Files.exists(archivoAdmin)) {
            String clave = System.getenv("COLEGIO_ADMIN_CLAVE");
            if (clave == null || clave.isBlank()) clave = "Admin123!";
            Validacion.clave(clave);
            Properties p = new Properties();
            p.setProperty("clave", Seguridad.hash(clave));
            guardarArchivo("administrador.properties", p);
        }
        Properties admin = leer("administrador.properties");
        personas.put("admin", new Administrador(obligatorio(admin, "clave")));
        for (String archivo : List.of("profesores.properties", "estudiantes.properties", "notas.properties")) {
            if (!Files.exists(carpeta.resolve(archivo))) guardarArchivo(archivo, new Properties());
        }
        cargarPersonas("profesores.properties", true);
        cargarPersonas("estudiantes.properties", false);
        Properties p = leer("notas.properties");
        for (String llave : new TreeSet<>(p.stringPropertyNames())) {
            if (!llave.endsWith(".materia")) continue;
            String base = llave.substring(0, llave.length() - ".materia".length());
            String id = obligatorio(p, base + ".estudiante");
            if (!(personas.get(id) instanceof Estudiante)) throw new IOException("Notas sin estudiante: " + id);
            List<BigDecimal> notas = Validacion.notas(Arrays.asList(obligatorio(p, base + ".notas").split(";", -1)));
            RegistroNotas registro = new RegistroNotas(id, obligatorio(p, llave), notas);
            String clave = llaveNotas(id, registro.getMateria());
            if (registros.putIfAbsent(clave, registro) != null) throw new IOException("Materia repetida en los archivos.");
        }
    }

    private void cargarPersonas(String archivo, boolean profesor) throws IOException {
        Properties p = leer(archivo);
        for (String llave : new TreeSet<>(p.stringPropertyNames())) {
            if (!llave.endsWith(".nombre")) continue;
            String id = llave.substring(0, llave.length() - ".nombre".length());
            String nombre = obligatorio(p, llave);
            String clave = obligatorio(p, id + ".clave");
            String aula = obligatorio(p, id + ".aula");
            Persona persona = profesor ? new Profesor(nombre, id, clave, aula)
                    : new Estudiante(nombre, id, clave, obligatorio(p, id + ".tipoDocumento"), aula);
            if (personas.putIfAbsent(id, persona) != null) throw new IOException("Identificación repetida: " + id);
        }
    }

    private static String obligatorio(Properties p, String llave) throws IOException {
        String valor = p.getProperty(llave);
        if (valor == null || valor.isBlank()) throw new IOException("Falta el dato: " + llave);
        return valor;
    }

    private Properties leer(String nombre) throws IOException {
        Properties p = new Properties();
        try (Reader lector = Files.newBufferedReader(carpeta.resolve(nombre), StandardCharsets.UTF_8)) {
            p.load(lector);
        }
        return p;
    }

    /** Escribe primero un temporal y después reemplaza el archivo completo. */
    private void guardarArchivo(String nombre, Properties contenido) throws IOException {
        Path temporal = Files.createTempFile(carpeta, "guardado-", ".tmp");
        try {
            try (Writer escritor = Files.newBufferedWriter(temporal, StandardCharsets.UTF_8)) {
                contenido.store(escritor, "Notas Escolares - datos UTF-8. No editar mientras el programa este abierto.");
            }
            try {
                Files.move(temporal, carpeta.resolve(nombre), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporal, carpeta.resolve(nombre), StandardCopyOption.REPLACE_EXISTING);
            }
        } finally { Files.deleteIfExists(temporal); }
    }

    public synchronized Persona persona(String usuario) { return personas.get(usuario); }

    public synchronized List<Profesor> profesores() {
        return personas.values().stream().filter(p -> p instanceof Profesor).map(p -> (Profesor) p)
                .sorted(Comparator.comparing(Persona::getNombre, String.CASE_INSENSITIVE_ORDER)).toList();
    }

    public synchronized List<Estudiante> estudiantes() {
        return personas.values().stream().filter(p -> p instanceof Estudiante).map(p -> (Estudiante) p)
                .sorted(Comparator.comparing(Persona::getNombre, String.CASE_INSENSITIVE_ORDER)).toList();
    }

    private Properties serializarPersonas(Collection<Persona> lista, boolean profesores) {
        Properties p = new Properties();
        for (Persona persona : lista) {
            if (profesores != (persona instanceof Profesor) || persona instanceof Administrador) continue;
            String id = persona.getIdentificacion();
            p.setProperty(id + ".nombre", persona.getNombre());
            p.setProperty(id + ".clave", persona.getClaveHash());
            if (persona instanceof Profesor profesor) p.setProperty(id + ".aula", profesor.getAula());
            if (persona instanceof Estudiante estudiante) {
                p.setProperty(id + ".aula", estudiante.getAula());
                p.setProperty(id + ".tipoDocumento", estudiante.getTipoDocumento());
            }
        }
        return p;
    }

    public synchronized void registrar(Persona persona) throws IOException {
        if (persona instanceof Administrador) throw new IllegalArgumentException("La cuenta principal ya existe.");
        if (personas.containsKey(persona.getIdentificacion())) {
            throw new IllegalArgumentException("Ya existe una cuenta con esa identificación.");
        }
        Map<String, Persona> copia = new LinkedHashMap<>(personas);
        copia.put(persona.getIdentificacion(), persona);
        boolean profesor = persona instanceof Profesor;
        guardarArchivo(profesor ? "profesores.properties" : "estudiantes.properties", serializarPersonas(copia.values(), profesor));
        personas.put(persona.getIdentificacion(), persona); // Solo cambia la memoria si el guardado tuvo éxito.
    }

    public synchronized void cambiarClave(String usuario, String hash) throws IOException {
        Persona persona = personas.get(usuario);
        if (persona == null) throw new IllegalArgumentException("La cuenta no existe.");
        String archivo = persona instanceof Administrador ? "administrador.properties"
                : persona instanceof Profesor ? "profesores.properties" : "estudiantes.properties";
        Properties p = leer(archivo);
        p.setProperty(persona instanceof Administrador ? "clave" : usuario + ".clave", hash);
        guardarArchivo(archivo, p);
        persona.setClaveHash(hash);
    }

    private static String llaveNotas(String estudianteId, String materia) {
        String codificada = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(Validacion.normalizar(materia).getBytes(StandardCharsets.UTF_8));
        return estudianteId + "." + codificada;
    }

    public synchronized RegistroNotas materia(String id, String nombre) {
        return registros.get(llaveNotas(id, nombre));
    }

    public synchronized List<RegistroNotas> materias(String id) {
        return registros.values().stream().filter(r -> r.getEstudianteId().equals(id))
                .sorted(Comparator.comparing(RegistroNotas::getMateria, String.CASE_INSENSITIVE_ORDER)).toList();
    }

    public synchronized void guardarNotas(RegistroNotas registro) throws IOException {
        if (!(personas.get(registro.getEstudianteId()) instanceof Estudiante)) {
            throw new IllegalArgumentException("El estudiante no existe.");
        }
        String llave = llaveNotas(registro.getEstudianteId(), registro.getMateria());
        RegistroNotas anterior = registros.get(llave);
        if (anterior != null) registro = new RegistroNotas(registro.getEstudianteId(), anterior.getMateria(), registro.getNotas());
        Map<String, RegistroNotas> copia = new LinkedHashMap<>(registros);
        copia.put(llave, registro);
        Properties p = new Properties();
        copia.forEach((base, r) -> {
            p.setProperty(base + ".estudiante", r.getEstudianteId());
            p.setProperty(base + ".materia", r.getMateria());
            p.setProperty(base + ".notas", r.getNotas().stream().map(BigDecimal::toPlainString).collect(Collectors.joining(";")));
        });
        guardarArchivo("notas.properties", p);
        registros.put(llave, registro);
    }

    public Path getCarpeta() { return carpeta; }
    @Override public void close() throws IOException {
        if (bloqueo.isValid()) bloqueo.release();
        canal.close();
    }
}
