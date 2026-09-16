# Notas Escolares

Aplicación web local de registro y consulta de calificaciones, desarrollada en Java con programación orientada a objetos. Incluye código fuente, configuración de IntelliJ IDEA, pruebas y un archivo JAR compilado.

## Inicio en IntelliJ IDEA

1. Extrae el ZIP completo y abre la carpeta **NotasEscolares** mediante **File > Open**. Abre la carpeta que contiene `src`, no su carpeta superior ni un archivo Java aislado.
2. En **File > Project Structure > Project**, selecciona un **JDK 17 o superior**. Si falta, utiliza la opción para agregar o descargar un JDK. Un SDK llamado 21, por ejemplo, también sirve; cambia la referencia inicial al SDK 17 por el que tengas instalado.
3. Abre `src/colegio/Main.java`. Pulsa el botón verde junto a `main` y selecciona **Run 'Main.main()'**. También se incluye la configuración **Notas Escolares**.
4. Mantén Java ejecutándose y abre `http://localhost:8080` en el navegador.
5. Para detenerlo, pulsa **Stop** en IntelliJ. Cerrar solo el navegador no detiene Java.

La primera ejecución crea al administrador. No hay profesores ni estudiantes preinscritos.

```text
Usuario: admin
Contraseña inicial: Admin123!
```

Entra y utiliza **Cambiar contraseña**. Los demás usuarios eligen su clave al registrarse; su usuario es su número de identificación. La clave debe tener entre 8 y 128 caracteres.

## Qué permite hacer

**Administrador.** Consulta el total de profesores, estudiantes y aulas. Ve los integrantes agrupados por aula, filtra un grupo y abre las fichas de estudiantes. No modifica notas ni crea más administradores desde la página.

**Profesor.** Se registra con nombre, cédula, aula y contraseña. Busca por nombre o identificación a un estudiante de su aula. Desde la ficha escribe la materia, agrega las notas que necesite y las guarda. Para una materia existente utiliza **Editar**; puede cambiar valores, agregar casillas y quitar notas antes de guardar.

**Estudiante.** Se registra con nombre, tipo de documento (CC o TI), identificación, aula y contraseña. Al entrar consulta únicamente su propia ficha, sin controles para modificar notas.

## Reglas adoptadas para este proyecto escolar

Cada profesor y estudiante pertenece a **una sola aula**. Las aulas se forman automáticamente con el texto del registro; no existe una pantalla adicional para crearlas. `10a`, `10 A` y `10A` se consideran la misma aula. Usa nombres de grupo consistentes.

El profesor puede crear materias al calificar. Varios profesores de una misma aula pueden editar las materias de los estudiantes de esa aula; no hay asignación exclusiva de materias a docentes. El registro de profesores es inmediato, sin aprobación del administrador. No hay períodos, años escolares, recuperaciones, matrículas ni cierre definitivo del curso.

La identificación debe contener entre 5 y 20 dígitos, sin puntos. Se conserva como texto para no perder ceros iniciales. No puede repetirse entre cuentas, incluso de roles diferentes. La validación es de formato; no verifica documentos ante ninguna entidad.

## Cálculo de las calificaciones

```text
Promedio = suma de las notas / cantidad de notas
Ganó: promedio real >= 3
Perdió: promedio real < 3
```

La escala es de 0 a 5, incluidos ambos extremos. Se acepta coma o punto decimal: `3,5` y `3.5`. Todas las notas tienen el mismo peso. Una casilla vacía no se cuenta; un `0` escrito sí se cuenta. No se puede guardar una materia sin ninguna nota válida.

Se muestra un promedio por materia y un **promedio general de todas las notas registradas**. El estado del curso usa este promedio general, no la obligación de aprobar todas las materias. No se calcula el promedio de los promedios.

Ejemplo: Matemáticas tiene `4` y `3`; Ciencias tiene `4,5`. El general es `(4 + 3 + 4,5) / 3 = 3,8333...`, mostrado como **3,83**, y el curso aparece como **Ganó**. Todos los resultados son provisionales según las notas actualmente guardadas.

Se muestran dos decimales, pero el estado se decide **antes de redondear**. Por ejemplo, `2,995` se puede mostrar como `3,00` y sigue siendo **Perdió**. La interfaz informa esta regla. La clase `RegistroNotas` utiliza `BigDecimal` y compara la suma con `3 × cantidad` para decidir la aprobación sin redondear.

## Recorrido de demostración

Los siguientes datos son ficticios. No son cuentas preinstaladas.

1. En la pantalla de acceso, entra en **Soy profesor**. Registra `Laura Docente`, cédula `1000000001`, aula `10A`, clave `Prueba123!`.
2. Registra un estudiante: `Ana Estudiante`, TI `1000000002`, aula `10A`, clave `Prueba123!`.
3. Inicia sesión como profesor con `1000000001`. Busca `Ana` y pulsa **Ver / calificar**.
4. Escribe `Matemáticas`. Agrega `4`, `3,5` y `5` con **Agregar otra nota**. Guarda: el promedio será **4,17**, **Ganó**.
5. Desde **Editar**, cambia las notas a `2` y `3`. Guarda: el promedio será **2,50**, **Perdió**.
6. Cierra sesión e ingresa como estudiante. Verás sus notas, sin poder editarlas. Después entra como administrador para mostrar el aula y sus integrantes.
7. Detén y vuelve a ejecutar Java. Inicia sesión de nuevo y comprueba que las cuentas y notas siguen guardadas.

Para entrar con dos cuentas al mismo tiempo, usa navegadores o perfiles separados. Una misma sesión de navegador mantiene una sola cuenta activa.

## Dónde se conservan los datos

```text
datos/
  administrador.properties   # Se crea en la primera ejecución.
  profesores.properties
  estudiantes.properties
  notas.properties
```

Son archivos de texto estructurados con `java.util.Properties`, **no una base de datos SQL**. No requieren instalar MySQL, SQLite ni un gestor de bases de datos. Cada registro y cada guardado de notas se escribe al disco antes de confirmar el éxito.

El programa muestra en consola la ubicación exacta de los datos. Los scripts y la configuración incluida de IntelliJ usan la carpeta del proyecto como directorio de trabajo. No cambies ese directorio entre ejecuciones: podrías crear una carpeta `datos` distinta y creer que se perdió la información.

Para respaldar la información, detén el programa y copia toda la carpeta `datos`. No edites los archivos mientras Java esté abierto. Solo una instancia puede abrir la misma carpeta a la vez.

Las contraseñas se almacenan como hash con sal, no como texto legible. Los nombres, identificaciones y notas **no están cifrados**. La entrega y las pruebas usan datos ficticios. Las sesiones vencen tras 30 minutos de inactividad o al reiniciar Java; esto no borra las cuentas ni las notas.

## Ejecutar sin IntelliJ

Con un JDK 17 o superior disponible en el `PATH`, en Windows abre `ejecutar.bat`. En Linux o macOS ejecuta:

```sh
sh ejecutar.sh
```

También puedes ejecutar el JAR incluido. Desde la carpeta del proyecto:

```sh
java -jar bin/notas-escolares.jar
```

El JAR contiene el código compilado, no tus datos. Cambiar los archivos `.java` no cambia ese JAR: utiliza IntelliJ o `ejecutar.bat`/`ejecutar.sh` para ejecutar tus modificaciones, o vuelve a compilarlo (crea primero la carpeta `out/clases`):

```sh
javac --release 17 -encoding UTF-8 -d out/clases @fuentes.txt
jar --create --file bin/notas-escolares.jar --main-class colegio.Main -C out/clases .
```

## Problemas frecuentes

**No aparece el botón verde o todo está en rojo.** Selecciona un JDK en Project Structure. Comprueba que abriste el proyecto completo. En la configuración incluida, `src` es el directorio de fuentes y `pruebas` el de pruebas. Si el IDE no lo reconoció, marca `src` como **Sources Root**.

**`javac` no se reconoce.** El script no encuentra un JDK en el PATH. Seleccionarlo en IntelliJ permite trabajar desde el IDE aunque no esté en el PATH de Windows.

**Puerto ocupado.** Detén la ejecución anterior. También puedes poner `8081` en **Program arguments**, usar `ejecutar.bat 8081`, `sh ejecutar.sh 8081` o `java -jar bin/notas-escolares.jar 8081`. Abre el mismo puerto en el navegador.

**No abre la página.** Verifica que Java continúe ejecutándose y que la consola no muestre un error. Usa `http://localhost:8080`, no HTTPS. También sirve `http://127.0.0.1:8080`. Si el puerto es otro, sustitúyelo en la dirección.

**No encuentro al estudiante.** Comprueba el aula de ambas cuentas. Solo aparecen estudiantes del aula del profesor. La búsqueda no distingue mayúsculas ni tildes.

**Materia ya existente.** Pulsa **Editar** junto a esa materia antes de cambiar sus notas. `Matemáticas` y `matematicas` no crean dos materias diferentes para el mismo estudiante.

**Archivos ya abiertos.** Cierra la otra instancia que esté usando los mismos datos. Cambiar solo el puerto no permite compartir una misma carpeta entre dos servidores.

**Olvidé la contraseña de demostración del admin.** No hay recuperación por correo. En tu copia local, detén Java, respalda `datos` y elimina solo `datos/administrador.properties`; al iniciar se recreará con la clave inicial. No borres los demás archivos. Este procedimiento requiere acceso a los archivos del proyecto.

## Alcance y pruebas

Proyecto educativo para ejecutarse **en un solo computador**. El servidor escucha exclusivamente en `127.0.0.1`, por lo que no se publica en Internet ni admite conexiones directas de otros dispositivos. No necesita conexión a Internet durante su uso una vez instalado el JDK.

No es un sistema listo para operar un colegio real: el registro docente está abierto, la clave inicial es conocida y no hay auditoría, recuperación de cuentas, copias automáticas ni resolución de ediciones simultáneas de la misma materia. En ese último caso prevalece el último guardado. No lo expongas públicamente ni uses datos reales de estudiantes en una demostración.

Consulta `EXPLICACION_POO.md` para sustentar el código y `PRUEBAS.md` para ejecutar las 101 comprobaciones automáticas. Las capturas de `capturas/` utilizan cuentas ficticias que no se incluyen en `datos/`.

## Referencias técnicas

Documentación del servidor HTTP incluido en el JDK 17:
https://docs.oracle.com/en/java/javase/17/docs/api/jdk.httpserver/module-summary.html

Documentación de IntelliJ IDEA sobre creación y ejecución de aplicaciones Java:
https://www.jetbrains.com/help/idea/creating-and-running-your-first-java-application.html

La guía de JetBrains muestra ejemplos con versiones recientes de Java; este proyecto utiliza clases convencionales y se compila con destino Java 17.
