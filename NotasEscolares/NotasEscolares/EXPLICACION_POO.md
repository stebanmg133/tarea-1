# Explicación del proyecto para presentar en clase

**Proyecto:** Notas Escolares  
**Estudiante:** ____________________  
**Curso:** ____________________  
**Docente:** ____________________

## Objetivo

Desarrollar una aplicación web en Java que permita registrar profesores y estudiantes, guardar notas por materia y consultar el promedio y el estado académico, aplicando programación orientada a objetos.

El problema se divide en tres partes: representar la información del colegio, procesar las acciones de los usuarios y conservar los datos en archivos. El navegador muestra formularios; Java valida los datos, comprueba los permisos y calcula los resultados.

## Herencia: clase padre y clases hijas

```text
Persona (abstracta)
|-- Profesor
|-- Estudiante
`-- Administrador
```

`Persona` contiene los datos comunes: nombre, identificación y hash de contraseña. Las hijas reutilizan esas características. `Profesor` agrega su aula; `Estudiante` agrega el tipo de documento y su aula. `Administrador` representa la cuenta principal.

El uso de `extends` se puede observar directamente en el código:

```java
public final class Estudiante extends Persona {
    private final String tipoDocumento;
    private final String aula;

    public Estudiante(String nombre, String identificacion,
                      String claveHash, String tipoDocumento, String aula) {
        super(nombre, Validacion.documento(identificacion), claveHash);
        if (!"CC".equals(tipoDocumento) && !"TI".equals(tipoDocumento)) {
            throw new IllegalArgumentException("Selecciona cédula o tarjeta de identidad.");
        }
        this.tipoDocumento = tipoDocumento;
        this.aula = Validacion.aula(aula);
    }

    public String getTipoDocumento() { return tipoDocumento; }
    public String getAula() { return aula; }
    @Override public String getRol() { return "Estudiante"; }
}
```

`super(...)` llama al constructor de `Persona` para inicializar los datos heredados. Este fragmento pertenece al paquete `colegio.modelo`; el archivo completo contiene su declaración de paquete e importación.

## Encapsulamiento, abstracción y polimorfismo

**Encapsulamiento.** Los atributos son privados. Otras clases consultan los datos mediante métodos como `getNombre()` y `getAula()`. La lista de notas se copia como inmutable para impedir cambios directos que no pasen por un guardado.

**Abstracción.** `Persona` es abstracta: define lo común sin representar un rol concreto. Para crear un usuario se elige una hija, no una persona genérica. Por su parte, `BaseDatos` oculta los detalles de lectura y escritura detrás de métodos como `registrar()` y `guardarNotas()`.

**Polimorfismo.** Cada hija implementa `getRol()`. La interfaz trabaja con una referencia de tipo `Persona` y obtiene un resultado distinto según el objeto real.

```java
Persona usuario = new Profesor("Laura", "1000000001", "hashEjemplo", "10A");
System.out.println(usuario.getRol()); // Profesor
```

El texto `hashEjemplo` solo ilustra el constructor; no es una credencial válida para iniciar sesión. El registro real crea el hash con `Seguridad.hash()`.

## Responsabilidad de cada clase

| Clase | Responsabilidad |
|---|---|
| `Main` | Inicia la base de datos y el servidor web. |
| `Validacion` | Valida nombre, documento, aula, contraseña y notas. |
| `Persona` | Define los atributos y métodos compartidos. |
| `Profesor` | Representa al profesor y su aula. |
| `Estudiante` | Representa al estudiante, documento y aula. |
| `Administrador` | Representa la cuenta principal de consulta. |
| `RegistroNotas` | Agrupa las notas de una materia y calcula promedio y estado. |
| `BaseDatos` | Lee y escribe los archivos de personas y notas. |
| `Seguridad` | Genera y verifica hashes de contraseñas y tokens aleatorios. |
| `ServidorWeb` | Recibe peticiones, gestiona sesiones y comprueba permisos. |
| `Vistas` | Construye las páginas HTML, el CSS y el JavaScript de los campos dinámicos. |

Las notas son parte de un `RegistroNotas`: se usa composición para agrupar datos relacionados. Los registros se asocian al estudiante por su identificación; no se intenta hacer que una nota herede de una persona.

## Cálculo explicado

```text
Notas: 4,0; 3,5; 5,0
Suma: 12,5
Cantidad: 3
Promedio real: 12,5 / 3 = 4,1666...
Promedio mostrado: 4,17
Resultado: Ganó
```

Cada nota tiene el mismo peso y no se utilizan porcentajes. Para el promedio general se reúnen todas las notas de todas las materias del estudiante.

En lugar de decidir la aprobación usando un promedio ya redondeado, el programa compara:

```text
suma de notas >= 3 * cantidad de notas
```

Con una cantidad positiva de notas, esto equivale a `promedio real >= 3`. Evita aprobar por redondeo a quien obtuvo menos de 3. Si no hay notas, el estado es **Sin calificar**, no **Perdió**.

## Recorrido de una operación

```text
Profesor escribe notas en el navegador
                |
                v
ServidorWeb recibe el formulario
                |
                v
Comprueba sesión, rol y aula del estudiante
                |
                v
Validacion revisa materia y valores entre 0 y 5
                |
                v
RegistroNotas representa la materia y sus notas
                |
                v
BaseDatos escribe notas.properties
                |
                v
Vistas muestra la ficha con el nuevo promedio
```

La contraseña no se almacena como texto legible. El programa compara un hash derivado de la clave ingresada. El almacenamiento de nombres y notas sigue siendo un archivo local de texto estructurado, no una base de datos SQL.

## Relación con los requisitos

| Requisito | Implementación |
|---|---|
| Página web en Java | `ServidorWeb` y `Vistas`, ejecutados desde `Main`. |
| Inicio de sesión | Usuario, contraseña y sesión por cuenta. |
| Administrador por aula | Panel de aulas con profesores y estudiantes. |
| Registro de personas | Formularios separados por rol, con identificación y aula. |
| Buscar estudiante | Nombre o identificación; solo en el aula del profesor. |
| Agregar y modificar notas | Editor de materia dentro de la ficha del estudiante. |
| Promedios sin porcentajes | Media aritmética por materia y de todas las notas. |
| Ganó o perdió | Umbral de 3, comparado sin redondear. |
| Conservar registros | Archivos `.properties` dentro de `datos`. |
| POO con padre e hijas | `Persona`, `Profesor`, `Estudiante` y `Administrador`. |

## Conclusión

La herencia permite reutilizar los datos de las personas; el encapsulamiento protege la representación de los objetos; el polimorfismo distingue los roles, y las clases separadas organizan la interfaz, el cálculo y el almacenamiento. El proyecto cubre un flujo escolar básico de registro, calificación y consulta. Su alcance es una demostración local, no la administración real de una institución.
