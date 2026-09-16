# Pruebas de Notas Escolares

## Resultado de la validación

El código se compiló con OpenJDK 21.0.11 utilizando `--release 17` y codificación UTF-8. Las **101 comprobaciones automáticas** incluidas en `pruebas/colegio/Pruebas.java` finalizaron correctamente. La salida está en `pruebas/resultado_validacion.txt`.

Se comprobó por código el cálculo de promedios, notas decimales, casillas vacías, la frontera exacta de 3, rechazo de valores inválidos, herencia, contraseñas, archivos y conservación de cuentas y notas tras cerrar y reabrir la base de datos.

Las pruebas HTTP iniciaron un servidor local temporal y enviaron peticiones reales de registro, autenticación, búsqueda, guardado, edición, consulta y cambio de contraseña. Comprobaron que profesores y estudiantes no acceden a las fichas de otras aulas o personas sin permiso, y que un estudiante no puede cambiar notas enviando un formulario manual. También probaron formularios sin token, escape de HTML y conservación de registros al reiniciar el servidor.

Adicionalmente se realizaron **12 comprobaciones de interfaz**: se obtuvo el HTML real del servidor y se renderizó en Chromium con el CSS y JavaScript reales, sin navegación de red en ese navegador porque el entorno la restringía. Se verificaron cinco pantallas, los botones para agregar y quitar notas y vistas de 390 y 1440 píxeles. Las capturas están en `capturas/`.

El JAR superó 14 comprobaciones adicionales: arranque independiente, acceso del administrador, cambio de clave, reinicio, recursos web y archivos de datos. El resultado está en `pruebas/resultado_jar.json`. No se ejecutó la interfaz de IntelliJ ni los scripts `.bat` de Windows en este entorno Linux; se incluye su configuración para abrir el proyecto y ejecutar la misma clase `Main`.

## Ejecutar las pruebas incluidas

En IntelliJ, abre `pruebas/colegio/Pruebas.java` y ejecuta `Pruebas.main()`, o selecciona la configuración **Pruebas del proyecto**.

En Windows, con un JDK 17+ en el PATH, ejecuta `probar.bat`. En Linux o macOS:

```sh
sh probar.sh
```

Las pruebas crean carpetas temporales y servidores en puertos libres. Utilizan exclusivamente datos ficticios y **no modifican la carpeta `datos` del proyecto**. No necesitan JUnit, descargas ni servicios externos.

La última línea esperada es:

```text
RESULTADO: 101 comprobaciones correctas. Ningun dato real fue modificado.
```

Una prueba fallida lanza un error con su nombre. Si el entorno impide abrir puertos locales, las comprobaciones HTTP requieren habilitar un entorno de ejecución que permita servidores locales.

## Casos manuales sugeridos para la exposición

| Caso | Resultado esperado |
|---|---|
| Guardar 4, 3,5 y 5 | Promedio 4,17; Ganó. |
| Guardar una sola nota 3 | Promedio 3,00; Ganó. |
| Guardar 2 y 3 | Promedio 2,50; Perdió. |
| Guardar 0 y 5 | Promedio 2,50; el cero sí cuenta. |
| Estudiante sin notas | Sin calificar. |
| Nota 5,1, negativa o texto | Error; no reemplaza las notas anteriores. |
| Dejar todas las casillas vacías | Solicita al menos una nota. |
| Guardar una identificación repetida | Rechaza el registro duplicado. |
| Buscar por nombre sin tildes | Encuentra la coincidencia del aula. |
| Editar una materia ya registrada | Actualiza su lista; no crea otra por cambios de tildes. |
| Entrar como estudiante | Muestra solo su propia ficha sin editor. |
| Reiniciar Java e iniciar sesión | Conserva cuentas y notas; solicita entrar de nuevo. |

Estas pruebas cubren el alcance escolar definido. No constituyen una auditoría de seguridad, una prueba de carga ni una validación para uso institucional real.
