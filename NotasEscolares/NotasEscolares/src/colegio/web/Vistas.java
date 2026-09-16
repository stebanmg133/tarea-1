package colegio.web;

import colegio.Validacion;
import colegio.datos.BaseDatos;
import colegio.modelo.*;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/** Solo genera la interfaz HTML. Las cuentas y las notas se procesan en Java. */
public final class Vistas {
    private Vistas() { }

    public static String h(String texto) {
        if (texto == null) return "";
        return texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    public static String url(String texto) {
        return URLEncoder.encode(texto, StandardCharsets.UTF_8);
    }

    private static String oculto(String nombre, String valor) {
        return "<input type='hidden' name='" + h(nombre) + "' value='" + h(valor) + "'>";
    }

    private static String mensaje(String texto, boolean error) {
        if (texto == null || texto.isBlank()) return "";
        return "<div role='alert' class='aviso " + (error ? "error" : "exito") + "'>" + h(texto) + "</div>";
    }

    private static String numero(BigDecimal valor) {
        return valor == null ? "—" : valor.toPlainString().replace('.', ',');
    }

    private static String estado(String texto) {
        String clase = "Ganó".equals(texto) ? "ganado" : "Perdió".equals(texto) ? "perdido" : "pendiente";
        return "<span class='estado " + clase + "'>" + h(texto) + "</span>";
    }

    private static List<BigDecimal> todas(List<RegistroNotas> materias) {
        return materias.stream().flatMap(m -> m.getNotas().stream()).toList();
    }

    public static String pagina(String titulo, Persona persona, String csrf, String contenido) {
        String nav;
        if (persona == null) {
            nav = "<a href='/login'>Iniciar sesión</a><a class='btn secundario pequeno' href='/registro?rol=estudiante'>Registrarse</a>";
        } else {
            nav = "<a href='/'>Inicio</a><a href='/clave'>Cambiar contraseña</a><span class='rol'>"
                    + h(persona.getRol()) + "</span><form method='post' action='/salir'>" + oculto("csrf", csrf)
                    + "<button class='btn secundario pequeno' type='submit'>Salir</button></form>";
        }
        return "<!doctype html><html lang='es'><head><meta charset='UTF-8'>"
                + "<meta name='viewport' content='width=device-width, initial-scale=1'>"
                + "<title>" + h(titulo) + " | Notas Escolares</title>"
                + "<link rel='stylesheet' href='/estilos.css'><script defer src='/app.js'></script></head><body>"
                + "<header class='cabecera'><div class='barra'><a class='marca' href='/'><span class='logo'>NE</span>"
                + "<span>Notas Escolares<small>Registro académico</small></span></a><nav aria-label='Principal'>"
                + nav + "</nav></div></header><main>" + contenido + "</main>"
                + "<footer>Notas Escolares <span>·</span> Proyecto escolar en Java y POO <span>·</span> Uso local</footer></body></html>";
    }

    public static String login(String csrf, String error, String aviso, String usuario) {
        String cuerpo = """
                <section class='portada'>
                  <div class='presentacion'>
                    <p class='etiqueta'>APRENDER. REGISTRAR. AVANZAR.</p>
                    <h1>Cada nota<br>cuenta.</h1>
                    <p class='descripcion'>Un espacio sencillo para registrar estudiantes, organizar aulas y consultar sus resultados.</p>
                    <div class='reglas'><div><strong>0 a 5</strong><span>Escala de notas</span></div>
                    <div><strong>3,0</strong><span>Mínimo para ganar</span></div>
                    <div><strong>Sin %</strong><span>Promedio simple</span></div></div>
                    <p class='nota-portada'>Profesores, estudiantes y administrador.<br>Cada cuenta tiene su propio acceso.</p>
                  </div>
                  <div class='panel acceso'>
                    <p class='etiqueta'>BIENVENIDO</p><h2>Iniciar sesión</h2>
                    <p class='suave'>Ingresa tus datos para continuar.</p>
                """;
        cuerpo += mensaje(error, true) + mensaje(aviso, false)
                + "<form method='post' action='/login' class='formulario'>" + oculto("csrf", csrf)
                + "<label for='usuario'>Usuario</label><input id='usuario' name='usuario' value='" + h(usuario)
                + "' placeholder='Tu identificación o admin' autocomplete='username' maxlength='20' required>"
                + "<label for='clave'>Contraseña</label><input id='clave' name='clave' type='password' autocomplete='current-password' maxlength='128' required>"
                + "<button class='btn ancho' type='submit'>Ingresar</button></form>"
                + "<div class='registro-links'><p>¿Todavía no tienes cuenta?</p>"
                + "<a href='/registro?rol=profesor'>Soy profesor</a><span>·</span><a href='/registro?rol=estudiante'>Soy estudiante</a></div></div></section>";
        return pagina("Iniciar sesión", null, csrf, cuerpo);
    }

    public static String registro(String csrf, String rol, Map<String, String> datos, String error) {
        boolean profesor = "profesor".equals(rol);
        String cuerpo = "<section class='panel registro'><p class='etiqueta'>NUEVA CUENTA</p><h1>Registro de "
                + (profesor ? "profesor" : "estudiante") + "</h1><p class='suave'>Tu identificación será tu usuario para iniciar sesión.</p>"
                + "<div class='pestanas'><a class='" + (profesor ? "activa" : "") + "' href='/registro?rol=profesor'>Profesor</a>"
                + "<a class='" + (!profesor ? "activa" : "") + "' href='/registro?rol=estudiante'>Estudiante</a></div>"
                + mensaje(error, true) + "<form method='post' action='/registro' class='formulario rejilla-form'>"
                + oculto("csrf", csrf) + oculto("rol", rol)
                + campo("nombre", "Nombre completo", datos.get("nombre"), "text", "80", "class='completo'", "autocomplete='name'");
        if (!profesor) {
            cuerpo += "<div><label for='tipoDocumento'>Tipo de identificación</label><select id='tipoDocumento' name='tipoDocumento'>"
                    + "<option value='TI'" + ("CC".equals(datos.get("tipoDocumento")) ? "" : " selected") + ">Tarjeta de identidad</option>"
                    + "<option value='CC'" + ("CC".equals(datos.get("tipoDocumento")) ? " selected" : "") + ">Cédula de ciudadanía</option></select></div>";
        }
        cuerpo += campo("identificacion", profesor ? "Cédula" : "Número de identificación", datos.get("identificacion"), "text", "20", "", "inputmode='numeric' pattern='[0-9]{5,20}' autocomplete='username'")
                + campo("aula", "Aula", datos.get("aula"), "text", "30", "", "placeholder='Ejemplo: 10A'")
                + "<p class='ayuda completo'>Usa la misma aula que tu grupo. Ejemplo: 10A. Cada cuenta pertenece a una sola aula.</p>"
                + campo("clave", "Contraseña", "", "password", "128", "", "minlength='8' autocomplete='new-password'")
                + campo("confirmacion", "Repetir contraseña", "", "password", "128", "", "minlength='8' autocomplete='new-password'")
                + "<p class='ayuda completo'>Mínimo 8 caracteres. No uses tu contraseña de otros servicios.</p>"
                + "<button class='btn completo' type='submit'>Crear cuenta</button></form>"
                + "<p class='pie-form'>¿Ya tienes cuenta? <a href='/login'>Iniciar sesión</a></p></section>";
        return pagina("Registro", null, csrf, cuerpo);
    }

    private static String campo(String id, String titulo, String valor, String tipo, String maximo, String contenedor, String extra) {
        return "<div " + contenedor + "><label for='" + id + "'>" + titulo + "</label><input id='" + id + "' name='"
                + id + "' type='" + tipo + "' maxlength='" + maximo + "' value='" + h(valor) + "' " + extra + " required></div>";
    }

    private static String indicador(String titulo, String valor, String detalle) {
        return "<div class='indicador'><span>" + h(titulo) + "</span><strong>" + valor + "</strong><small>" + h(detalle) + "</small></div>";
    }

    public static String admin(Administrador admin, BaseDatos db, String filtro, String csrf) {
        List<Profesor> profesores = db.profesores();
        List<Estudiante> estudiantes = db.estudiantes();
        Set<String> aulas = new TreeSet<>();
        profesores.forEach(p -> aulas.add(p.getAula()));
        estudiantes.forEach(e -> aulas.add(e.getAula()));
        StringBuilder cuerpo = new StringBuilder("<p class='etiqueta'>VISTA GENERAL</p><h1>Panel del administrador</h1>"
                + "<p class='descripcion'>Consulta quiénes están registrados en cada aula.</p><div class='indicadores'>"
                + indicador("Profesores", Integer.toString(profesores.size()), "Cuentas registradas")
                + indicador("Estudiantes", Integer.toString(estudiantes.size()), "Cuentas registradas")
                + indicador("Aulas", Integer.toString(aulas.size()), "Grupos con integrantes") + "</div>");
        if (aulas.isEmpty()) {
            cuerpo.append("<div class='panel vacio'><h2>El colegio está listo para empezar</h2><p>Aún no hay profesores ni estudiantes. "
                    + "Cierra sesión y usa Registrarse para crear las primeras cuentas de prueba.</p></div>");
        } else {
            cuerpo.append("<form class='buscador panel' action='/admin' method='get'><div><label for='aula-filtro'>Ver un aula</label>"
                    + "<select name='aula' id='aula-filtro'><option value=''>Todas las aulas</option>");
            for (String aula : aulas) cuerpo.append("<option value='").append(h(aula)).append("'")
                    .append(aula.equals(filtro) ? " selected" : "").append(">").append(h(aula)).append("</option>");
            cuerpo.append("</select></div><button class='btn secundario' type='submit'>Filtrar</button></form>");
            for (String aula : aulas) {
                if (!filtro.isBlank() && !aula.equals(filtro)) continue;
                List<Profesor> ps = profesores.stream().filter(p -> p.getAula().equals(aula)).toList();
                List<Estudiante> es = estudiantes.stream().filter(e -> e.getAula().equals(aula)).toList();
                cuerpo.append("<section class='panel aula'><div class='titulo-fila'><h2>Aula ").append(h(aula))
                        .append("</h2><span class='suave'>").append(ps.size()).append(" profesores · ").append(es.size())
                        .append(" estudiantes</span></div><div class='dos-columnas'><div><h3>Profesores</h3>");
                if (ps.isEmpty()) cuerpo.append("<p class='suave'>Sin profesores registrados.</p>");
                else {
                    cuerpo.append("<div class='tabla-contenedor'><table><thead><tr><th>Nombre</th><th>Cédula</th></tr></thead><tbody>");
                    for (Profesor p : ps) cuerpo.append("<tr><td>").append(h(p.getNombre())).append("</td><td>")
                            .append(h(p.getIdentificacion())).append("</td></tr>");
                    cuerpo.append("</tbody></table></div>");
                }
                cuerpo.append("</div><div><h3>Estudiantes</h3>");
                if (es.isEmpty()) cuerpo.append("<p class='suave'>Sin estudiantes registrados.</p>");
                else {
                    cuerpo.append("<div class='tabla-contenedor'><table><thead><tr><th>Nombre</th><th>Identificación</th><th>Ficha</th></tr></thead><tbody>");
                    for (Estudiante e : es) cuerpo.append("<tr><td>").append(h(e.getNombre())).append("</td><td>")
                            .append(h(e.getTipoDocumento())).append(" ").append(h(e.getIdentificacion()))
                            .append("</td><td><a href='/estudiante?id=").append(url(e.getIdentificacion())).append("'>Consultar</a></td></tr>");
                    cuerpo.append("</tbody></table></div>");
                }
                cuerpo.append("</div></div></section>");
            }
        }
        return pagina("Administración", admin, csrf, cuerpo.toString());
    }

    public static String profesor(Profesor profesor, BaseDatos db, String consulta, String csrf) {
        String busqueda = Validacion.normalizar(consulta);
        List<Estudiante> aula = db.estudiantes().stream().filter(e -> e.getAula().equals(profesor.getAula())).toList();
        List<Estudiante> encontrados = aula.stream().filter(e -> Validacion.normalizar(e.getNombre()).contains(busqueda)
                || e.getIdentificacion().contains(busqueda)).toList();
        StringBuilder cuerpo = new StringBuilder("<p class='etiqueta'>ESPACIO DEL PROFESOR</p><h1>Hola, " + h(profesor.getNombre())
                + "</h1><p class='descripcion'>Aula <strong>" + h(profesor.getAula()) + "</strong> · " + aula.size()
                + " estudiantes registrados. Busca un estudiante y abre su ficha para calificar.</p>"
                + "<form class='buscador panel' method='get' action='/profesor'><div><label for='q'>Nombre o identificación del estudiante</label>"
                + "<input id='q' name='q' value='" + h(consulta) + "' maxlength='80' placeholder='Ejemplo: Ana o 1000000002'>"
                + "</div><button class='btn' type='submit'>Buscar</button><a class='btn secundario' href='/profesor'>Ver todos</a></form>"
                + "<section class='panel'><div class='titulo-fila'><h2>Estudiantes de tu aula</h2><span class='suave'>" + encontrados.size() + " resultados</span></div>");
        if (encontrados.isEmpty()) cuerpo.append("<div class='vacio'><p>No hay estudiantes que coincidan con la búsqueda.</p>"
                + "<p class='suave'>Comprueba que el estudiante se haya registrado en el aula ").append(h(profesor.getAula())).append(".</p></div>");
        else {
            cuerpo.append("<div class='tabla-contenedor'><table><thead><tr><th>Estudiante</th><th>Identificación</th><th>Promedio general</th><th>Estado del curso</th><th>Acción</th></tr></thead><tbody>");
            for (Estudiante e : encontrados) {
                List<BigDecimal> notas = todas(db.materias(e.getIdentificacion()));
                cuerpo.append("<tr><td><strong>").append(h(e.getNombre())).append("</strong></td><td>")
                        .append(h(e.getTipoDocumento())).append(" ").append(h(e.getIdentificacion())).append("</td><td>")
                        .append(numero(RegistroNotas.promedio(notas))).append("</td><td>").append(estado(RegistroNotas.estado(notas)))
                        .append("</td><td><a class='btn secundario pequeno' href='/estudiante?id=").append(url(e.getIdentificacion()))
                        .append("'>Ver / calificar</a></td></tr>");
            }
            cuerpo.append("</tbody></table></div>");
        }
        cuerpo.append("</section><p class='ayuda'>Solo puedes consultar y calificar estudiantes de tu aula.</p>");
        return pagina("Mis estudiantes", profesor, csrf, cuerpo.toString());
    }

    public static String estudiante(Persona usuario, Estudiante estudiante, List<RegistroNotas> materias,
                                     String materiaElegida, List<String> borrador, String modoBorrador,
                                     String error, String aviso, String csrf) {
        boolean profesor = usuario instanceof Profesor;
        String id = estudiante.getIdentificacion();
        List<BigDecimal> notas = todas(materias);
        StringBuilder cuerpo = new StringBuilder();
        if (!(usuario instanceof Estudiante)) cuerpo.append("<a class='volver' href='/'>← Volver al listado</a>");
        cuerpo.append("<p class='etiqueta'>FICHA DEL ESTUDIANTE</p><h1>").append(h(estudiante.getNombre()))
                .append("</h1><p class='descripcion'>").append(h(estudiante.getTipoDocumento())).append(" ").append(h(id))
                .append(" · Aula <strong>").append(h(estudiante.getAula())).append("</strong></p>")
                .append(mensaje(error, true)).append(mensaje(aviso, false)).append("<div class='indicadores'>")
                .append(indicador("Promedio general", numero(RegistroNotas.promedio(notas)), "Escala de 0 a 5"))
                .append(indicador("Estado del curso", estado(RegistroNotas.estado(notas)), "Gana con promedio real de 3 o más"))
                .append(indicador("Materias", Integer.toString(materias.size()), notas.size() + " notas registradas")).append("</div>")
                .append("<section class='panel'><div class='titulo-fila'><h2>Notas por materia</h2><span class='suave'>Todas las notas tienen el mismo peso</span></div>");
        if (materias.isEmpty()) cuerpo.append("<div class='vacio'><p>Todavía no hay notas registradas.</p><p class='suave'>El resultado aparecerá cuando un profesor guarde la primera nota.</p></div>");
        else {
            cuerpo.append("<div class='tabla-contenedor'><table><thead><tr><th>Materia</th><th>Notas</th><th>Promedio</th><th>Resultado</th>")
                    .append(profesor ? "<th>Acción</th>" : "").append("</tr></thead><tbody>");
            for (RegistroNotas materia : materias) {
                cuerpo.append("<tr><td><strong>").append(h(materia.getMateria())).append("</strong></td><td><div class='chips'>");
                for (BigDecimal nota : materia.getNotas()) cuerpo.append("<span class='chip'>").append(numero(nota.stripTrailingZeros())).append("</span>");
                cuerpo.append("</div></td><td>").append(numero(materia.getPromedio())).append("</td><td>").append(estado(materia.getEstado())).append("</td>");
                if (profesor) cuerpo.append("<td><a href='/estudiante?id=").append(url(id)).append("&amp;materia=")
                        .append(url(materia.getMateria())).append("#editor'>Editar</a></td>");
                cuerpo.append("</tr>");
            }
            cuerpo.append("</tbody></table></div>");
        }
        cuerpo.append("</section><p class='ayuda'>Resultado con las notas actuales. El promedio general usa todas las notas registradas, "
                + "no el promedio de los promedios. Se muestran 2 decimales; la aprobación se decide antes de redondear.</p>");
        if (profesor) {
            RegistroNotas elegida = materias.stream().filter(m -> Validacion.normalizar(m.getMateria()).equals(Validacion.normalizar(materiaElegida))).findFirst().orElse(null);
            boolean editando = borrador != null ? "editar".equals(modoBorrador) : elegida != null;
            String nombre = elegida != null && editando ? elegida.getMateria() : materiaElegida;
            List<String> valores = borrador != null ? borrador : elegida != null ? elegida.getNotas().stream()
                    .map(BigDecimal::toPlainString).toList() : List.of("");
            if (valores.isEmpty()) valores = List.of("");
            cuerpo.append("<section class='panel editor' id='editor'><div class='titulo-fila'><div><p class='etiqueta'>CALIFICAR</p><h2>")
                    .append(editando ? "Modificar notas" : "Registrar una materia").append("</h2></div>");
            if (editando) cuerpo.append("<a href='/estudiante?id=").append(url(id)).append("#editor'>Agregar otra materia</a>");
            cuerpo.append("</div><form method='post' action='/notas' class='formulario' id='form-notas'>")
                    .append(oculto("csrf", csrf)).append(oculto("id", id)).append(oculto("modo", editando ? "editar" : "crear"))
                    .append("<label for='materia'>Materia que vas a calificar</label><input id='materia' name='materia' value='")
                    .append(h(nombre)).append("' maxlength='60' placeholder='Ejemplo: Matemáticas' required ")
                    .append(editando ? "readonly" : "").append("><p class='ayuda'>Usa Editar para cargar una materia existente. "
                            + "Al guardar cambios se reemplaza su lista de notas.</p><div id='lista-notas' class='lista-notas'>");
            for (int i = 0; i < valores.size(); i++) cuerpo.append(filaNota(i + 1, valores.get(i)));
            cuerpo.append("</div><div class='acciones'><button class='btn secundario' id='agregar-nota' type='button'>+ Agregar otra nota</button>"
                    + "<button class='btn' type='submit'>Guardar notas</button></div><p class='ayuda'>Acepta coma o punto decimal: 3,5 o 3.5. "
                    + "Las casillas vacías no cuentan. El promedio se actualiza al guardar.</p></form>"
                    + "<template id='plantilla-nota'>").append(filaNota(0, "")).append("</template></section>");
        }
        return pagina("Ficha de " + estudiante.getNombre(), usuario, csrf, cuerpo.toString());
    }

    private static String filaNota(int posicion, String valor) {
        return "<div class='fila-nota'><label for='nota-" + posicion + "' class='nombre-nota'>Nota " + posicion
                + "</label><input id='nota-" + posicion + "' name='nota' inputmode='decimal' type='text' maxlength='20' value='" + h(valor)
                + "' placeholder='0 a 5' autocomplete='off'><button class='quitar' type='button' data-quitar>Quitar</button></div>";
    }

    public static String clave(Persona persona, String csrf, String error, String aviso) {
        String cuerpo = "<section class='panel registro estrecho'><p class='etiqueta'>MI CUENTA</p><h1>Cambiar contraseña</h1>"
                + "<p class='suave'>Usuario: " + h(persona.getIdentificacion()) + "</p>" + mensaje(error, true) + mensaje(aviso, false)
                + "<form method='post' action='/clave' class='formulario'>" + oculto("csrf", csrf)
                + campo("actual", "Contraseña actual", "", "password", "128", "", "autocomplete='current-password'")
                + campo("nueva", "Nueva contraseña", "", "password", "128", "", "minlength='8' autocomplete='new-password'")
                + campo("confirmacion", "Repetir nueva contraseña", "", "password", "128", "", "minlength='8' autocomplete='new-password'")
                + "<p class='ayuda'>Mínimo 8 caracteres. Las otras sesiones de esta cuenta se cerrarán.</p>"
                + "<button class='btn' type='submit'>Guardar contraseña</button></form></section>";
        return pagina("Cambiar contraseña", persona, csrf, cuerpo);
    }

    public static String error(int codigo, String detalle, Persona persona, String csrf) {
        return pagina("Aviso " + codigo, persona, csrf, "<section class='panel registro'><p class='etiqueta'>AVISO " + codigo
                + "</p><h1>No se pudo completar</h1><p>" + h(detalle) + "</p><a class='btn' href='/'>Volver al inicio</a></section>");
    }

    public static final String CSS = """
            :root { --tinta:#172a39; --suave:#5c707e; --fondo:#f4f7f8; --linea:#dce5e8; --acento:#086e74; --claro:#e9f5f3; }
            * { box-sizing:border-box; }
            body { margin:0; color:var(--tinta); background:var(--fondo); font:15px/1.55 system-ui,-apple-system,'Segoe UI',sans-serif; }
            a { color:var(--acento); text-decoration:none; font-weight:600; }
            a:hover { text-decoration:underline; }
            .cabecera { background:#fff; border-bottom:1px solid var(--linea); }
            .barra { max-width:1160px; margin:auto; padding:18px 28px; display:flex; justify-content:space-between; align-items:center; gap:20px; }
            .marca { display:flex; align-items:center; gap:12px; color:var(--tinta); font-size:17px; font-weight:750; }
            .marca:hover { text-decoration:none; }
            .marca small { display:block; color:var(--suave); font-size:11px; font-weight:500; letter-spacing:.06em; }
            .logo { display:grid; place-items:center; width:42px; height:42px; background:var(--acento); color:white; border-radius:12px; font-size:16px; }
            nav { display:flex; align-items:center; gap:22px; font-size:13px; }
            nav form { margin:0; }
            .rol { background:var(--claro); color:var(--acento); border-radius:20px; padding:5px 12px; font-size:12px; }
            main { max-width:1160px; margin:0 auto; padding:42px 28px 58px; min-height:calc(100vh - 156px); }
            h1,h2,h3,p { margin-top:0; }
            h1 { font-size:32px; line-height:1.2; letter-spacing:-.035em; margin-bottom:14px; overflow-wrap:anywhere; }
            h2 { font-size:21px; line-height:1.3; margin-bottom:14px; letter-spacing:-.02em; }
            h3 { font-size:15px; margin-bottom:10px; }
            .descripcion { color:var(--suave); font-size:16px; max-width:780px; margin-bottom:30px; }
            .suave { color:var(--suave); }
            .etiqueta { color:var(--acento); font-size:11px; font-weight:800; letter-spacing:.14em; margin-bottom:12px; }
            .panel { background:white; border:1px solid var(--linea); border-radius:18px; padding:28px; margin-bottom:24px; box-shadow:0 3px 12px #172a3903; }
            .btn { display:inline-flex; justify-content:center; align-items:center; gap:6px; background:var(--acento); color:white; border:1px solid var(--acento); border-radius:9px; padding:11px 18px; font:600 14px/1.4 system-ui,-apple-system,'Segoe UI',sans-serif; cursor:pointer; text-align:center; }
            .btn:hover { background:#07585d; color:white; text-decoration:none; }
            .btn.secundario { background:white; color:var(--acento); border-color:#b8cdce; }
            .btn.secundario:hover { background:var(--claro); }
            .btn.pequeno { padding:7px 12px; font-size:12px; white-space:nowrap; }
            .btn.ancho { width:100%; margin-top:12px; }
            input,select { width:100%; min-width:0; padding:11px 12px; border:1px solid #b9cbd1; border-radius:8px; color:var(--tinta); background:white; font:inherit; }
            input[readonly] { background:#f0f4f5; }
            input:focus,select:focus { border-color:var(--acento); outline:3px solid #086e741f; }
            :focus-visible { outline:3px solid #36898e; outline-offset:3px; }
            label { display:block; font-weight:600; font-size:13px; margin-bottom:7px; }
            .formulario { display:grid; gap:14px; }
            .formulario>label { margin-bottom:-8px; }
            .formulario>div>label { margin-bottom:7px; }
            .rejilla-form { grid-template-columns:1fr 1fr; gap:18px; }
            .completo { grid-column:1/-1; }
            .ayuda { color:var(--suave); font-size:12px; margin:0 0 22px; }
            .formulario .ayuda { margin:0; }
            .aviso { padding:13px 16px; border-radius:9px; margin-bottom:20px; border:1px solid; font-size:14px; overflow-wrap:anywhere; }
            .error { background:#fff1ef; color:#963a32; border-color:#eebfb9; }
            .exito { background:#eaf7ef; color:#236044; border-color:#bddbc9; }
            .portada { display:grid; grid-template-columns:1.08fr 1fr; gap:80px; align-items:center; min-height:620px; }
            .presentacion h1 { font-size:72px; line-height:1.03; letter-spacing:-.065em; margin:22px 0; }
            .presentacion .descripcion { max-width:420px; font-size:17px; line-height:1.7; }
            .reglas { display:flex; gap:38px; margin:34px 0; }
            .reglas strong { font-size:25px; display:block; letter-spacing:-.03em; }
            .reglas span { font-size:11px; color:var(--suave); display:block; margin-top:3px; }
            .nota-portada { font-size:12px; color:var(--suave); border-left:3px solid #b9d8d5; padding-left:15px; margin-top:40px; }
            .acceso { padding:40px; margin:0; }
            .acceso h2 { font-size:29px; }
            .registro-links { text-align:center; font-size:13px; border-top:1px solid var(--linea); margin-top:28px; padding-top:22px; }
            .registro-links p { color:var(--suave); margin-bottom:8px; }
            .registro-links span { margin:0 12px; color:var(--suave); }
            .registro { max-width:650px; margin:10px auto; padding:34px; }
            .registro.estrecho { max-width:530px; }
            .registro h1 { font-size:27px; }
            .pestanas { display:flex; padding:5px; background:var(--fondo); border-radius:10px; margin:25px 0; gap:6px; }
            .pestanas a { flex:1; text-align:center; color:var(--suave); padding:9px; border-radius:7px; font-size:14px; }
            .pestanas .activa { background:white; color:var(--acento); box-shadow:0 1px 4px #172a3915; }
            .pie-form { text-align:center; font-size:13px; margin:24px 0 0; color:var(--suave); }
            .indicadores { display:grid; grid-template-columns:repeat(3,1fr); gap:18px; margin-bottom:28px; }
            .indicador { background:white; border:1px solid var(--linea); border-radius:14px; padding:22px 24px; }
            .indicador>span { display:block; font-size:12px; color:var(--suave); }
            .indicador>strong { display:block; font-size:32px; line-height:1.4; margin:6px 0; letter-spacing:-.03em; }
            .indicador small { display:block; color:var(--suave); font-size:11px; }
            .indicador .estado { font-size:20px; line-height:1.5; vertical-align:middle; letter-spacing:0; }
            .buscador { display:flex; align-items:flex-end; gap:14px; }
            .buscador>div { flex:1; }
            .titulo-fila { display:flex; align-items:center; justify-content:space-between; gap:18px; margin-bottom:18px; }
            .titulo-fila h2 { margin:0; }
            .titulo-fila .suave { font-size:12px; }
            .titulo-fila .etiqueta { margin-bottom:6px; }
            .dos-columnas { display:grid; grid-template-columns:.9fr 1.1fr; gap:32px; }
            .dos-columnas>div { min-width:0; }
            .tabla-contenedor { width:100%; overflow-x:auto; }
            table { border-collapse:collapse; width:100%; text-align:left; font-size:13px; }
            th { font-size:11px; color:var(--suave); font-weight:600; background:#f7f9fa; padding:12px 14px; white-space:nowrap; }
            td { padding:16px 14px; border-bottom:1px solid #e8eef0; vertical-align:middle; overflow-wrap:anywhere; }
            tr:last-child td { border-bottom:0; }
            .tabla-contenedor td:first-child { min-width:130px; }
            .estado { display:inline-block; border-radius:7px; padding:4px 10px; font-size:12px; font-weight:650; white-space:nowrap; }
            .ganado { color:#1f6448; background:#e8f5ed; }
            .perdido { color:#9c4235; background:#fcece8; }
            .pendiente { color:#657680; background:#edf1f3; }
            .vacio { padding:28px 16px; text-align:center; }
            .vacio p { margin-bottom:8px; }
            .vacio p:last-child { margin:0; }
            .volver { display:inline-block; font-size:12px; margin-bottom:24px; }
            .chips { display:flex; flex-wrap:wrap; gap:6px; max-width:440px; min-width:120px; }
            .chip { display:inline-block; background:#eff4f5; border-radius:6px; padding:4px 9px; font-size:12px; }
            .editor { border-top:3px solid var(--acento); }
            .editor input[name=materia] { max-width:520px; }
            .lista-notas { display:flex; flex-wrap:wrap; gap:12px; margin:6px 0; }
            .fila-nota { border:1px solid var(--linea); background:#fafcfc; border-radius:10px; padding:14px; width:148px; }
            .fila-nota label { font-size:12px; }
            .fila-nota input { background:white; padding:9px 10px; }
            .quitar { display:block; margin-top:8px; padding:2px 0; border:0; color:#89534b; background:none; cursor:pointer; font:12px/1.4 system-ui,sans-serif; }
            .quitar:hover { text-decoration:underline; }
            .acciones { display:flex; gap:12px; margin:12px 0; }
            footer { color:var(--suave); text-align:center; padding:20px; font-size:11px; border-top:1px solid var(--linea); }
            footer span { margin:0 10px; color:#a7b7bf; }
            @media(max-width:850px) { .portada { gap:35px; min-height:560px; } .presentacion h1 { font-size:57px; } .acceso { padding:28px; } .reglas { gap:20px; } .dos-columnas { grid-template-columns:1fr; } nav { gap:12px; } }
            @media(max-width:640px) { .barra { padding:15px 18px; flex-wrap:wrap; } nav { width:100%; flex-wrap:wrap; justify-content:flex-start; } main { padding:28px 18px 40px; } .portada { grid-template-columns:1fr; gap:20px; } .presentacion h1 { font-size:52px; } .nota-portada { display:none; } .presentacion .descripcion { margin-bottom:20px; } .reglas { margin:22px 0; } .panel { padding:22px 18px; } .indicadores { grid-template-columns:1fr; gap:10px; } .indicador { padding:15px 20px; } .indicador>strong { font-size:29px; margin:2px 0; } .buscador { flex-wrap:wrap; } .buscador>div { flex-basis:100%; } .titulo-fila { flex-wrap:wrap; } .rejilla-form { grid-template-columns:1fr; } .acciones { flex-wrap:wrap; } h1 { font-size:27px; } .fila-nota { width:calc(50% - 6px); } .registro { margin:0; } footer span { margin:0 4px; } }
            """;

    public static final String JS = """
            // Solo maneja los campos dinámicos; los cálculos y validaciones finales son de Java.
            const lista = document.getElementById('lista-notas');
            if (lista) {
                const renumerar = () => {
                    lista.querySelectorAll('.fila-nota').forEach((fila, indice) => {
                        const id = 'nota-' + (indice + 1);
                        const etiqueta = fila.querySelector('label');
                        etiqueta.textContent = 'Nota ' + (indice + 1);
                        etiqueta.htmlFor = id;
                        fila.querySelector('input').id = id;
                    });
                };
                document.getElementById('agregar-nota').addEventListener('click', () => {
                    const plantilla = document.getElementById('plantilla-nota');
                    lista.appendChild(plantilla.content.cloneNode(true));
                    renumerar();
                    lista.lastElementChild.querySelector('input').focus();
                });
                lista.addEventListener('click', (evento) => {
                    const boton = evento.target.closest('[data-quitar]');
                    if (!boton) return;
                    if (lista.children.length === 1) {
                        lista.querySelector('input').value = '';
                        lista.querySelector('input').focus();
                    } else {
                        boton.closest('.fila-nota').remove();
                        renumerar();
                    }
                });
            }
            """;
}
