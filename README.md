# tarea-1

integrantes: 
-Steban Muñoz gallego
-Manuel Antonio Mora Giraldo

en esta tarea hicimos una pagina web que consiste en un administrador, registro de profesores y registro de estudiantes. 
los profesores pueden registrarse o iniciar sesion, al igual que los estudiantes; esteo se aplica a traves de una clase padre llamada persona, y sus hijas que son admin, profesor, y estudiante, que todos ellos tienen las mismas caracteristicas; pero tambien en cada archivo .java de estos hay especificaciones, como que el admin tiene privilegios, los profesores ponen notas y los estudiantes y profesores pertenecen a aulas.
tambien hay una base de datos donde se  guardan los datos registrados y al iniciar el programa el archivo BaseDatos.java lee los datos que tiene y los muestra al ingresar en la sesion.
un usuario inicia sesión, el Servidor crea sesión, el Navegador recibe la clave, cada nueva petición usa esa sesión.
al final los profesores pueden poner las notas y el programa se encarga de promediarlas sumandolas y dividiendolas por el numero de notas puestas para luego poder decidir si en mayor o igual a 3 ganó, y sino perdió.
