# Verificación de Personas — 23/10/2026

Se probó la aplicación real en **Pixel 7, Android 17**, con emulador visible, teclado virtual, cámara del sistema, selector de fotos, API Spring Boot y PostgreSQL 18.

Para este recorrido se usó una base temporal con datos sintéticos, separada de `personas_app`. El registro fue Alexis Sandoval, DNI `00601024`, con correo `alexis@example.com`. La base temporal se eliminó después de comprobar la fila inactiva. La versión habitual se volvió a compilar e instalar al terminar y quedó conectada a su servidor automático, sin una pantalla de IP.

## Flujo comprobado desde la interfaz

| Prueba | Resultado y evidencia |
| --- | --- |
| Nuevo | Abre el formulario vacío, sin copiar datos de una ficha consultada. |
| Campos obligatorios | Siguiente muestra aviso y señala los campos incompletos. [Captura](docs/capturas/datos-obligatorios.png). |
| Profesión nueva | Añadir profesión crea `Diseno QA` en la base de prueba y la selecciona. |
| Celular inválido | `887654321` se rechaza; exige nueve dígitos y que el primero sea 9. [Captura](docs/capturas/celular-invalido.png). |
| Foto obligatoria | Guardar sin fotografía muestra validación. [Captura](docs/capturas/foto-obligatoria.png). |
| Cámara | Se capturó una fotografía desde la cámara del emulador y se guardó el registro. [Captura](docs/capturas/foto-camara.png). |
| Búsqueda | Se vació la búsqueda, se introdujo el DNI y se abrió la ficha. Se contrastaron los datos guardados con la API. [Captura](docs/capturas/consulta.png). |
| DNI activo duplicado | Un segundo formulario con el mismo DNI fue rechazado. [Captura](docs/capturas/dni-duplicado-activo.png). |
| Editar | Se cambió el nombre a `Alexis Actualizado` y el celular a `999888777`; los valores se comprobaron en la API y mediante SQL. |
| Reemplazar foto | Se seleccionó una imagen de prueba distinta desde el selector de Android y se confirmó el cambio. [Captura](docs/capturas/reemplazo-foto.png). |
| Persistencia del reemplazo | Cambiaron la URL/ruta y los bytes de la foto. El archivo anterior se retiró después de guardar; quedó un JPEG en la carpeta temporal de fotos. [Ficha editada](docs/capturas/ficha-editada.png). |
| Confirmación de eliminación | Cancelar mantuvo `ACTIVO`; confirmar realizó la eliminación lógica. [Captura](docs/capturas/confirmacion-eliminar.png). |
| Eliminación lógica | SQL confirmó una fila conservada con `estado = 'INACTIVO'`. Buscar de nuevo no encontró un registro activo. [Captura](docs/capturas/sin-registro-activo.png). |
| Acceso al eliminado | La consulta de persona y la descarga de foto devolvieron 404; el endpoint `existe` continuó devolviendo `true`. |
| DNI inactivo duplicado | Intentar un alta nueva con el DNI eliminado también fue rechazado. [Captura](docs/capturas/dni-duplicado-inactivo.png). |
| Sin ajuste de IP | No existe el botón ni el diálogo del servidor. La versión final cargó las cinco profesiones habituales por conexión automática. |

La consulta SQL usada para comprobar la eliminación fue:

```sql
SELECT dni, estado FROM personas.persona WHERE dni = '00601024';
-- 00601024 | INACTIVO
SELECT count(*) FROM personas.persona WHERE dni = '00601024';
-- 1
```

El resultado estructurado está en [pruebas-emulador.json](docs/pruebas-emulador.json). La imagen de cuadros azules es una imagen sintética para distinguir claramente la fotografía sustituta de la captura inicial.

## Comprobaciones automáticas

- **31 comprobaciones HTTP/SQL correctas**, ejecutadas con PostgreSQL en otra base desechable. Incluyen migración de la columna antigua, repetición del arranque sin reactivar filas, DNI con cero inicial, duplicados simultáneos, celular, profesión inexistente, imágenes inválidas, sustitución y limpieza de fotos, eliminación lógica y restricciones SQL de estado y clave foránea. [Resultados](docs/pruebas-api.json).
- **6 pruebas instrumentales correctas** en Android: recorrido de pestañas, validación, ausencia del ajuste de IP, ficha en vivo, conservación del texto y foco usando Siguiente del teclado, iconos accesibles y etiquetas al 200 %. [Salida](docs/instrumentacion.txt).
- **16 pruebas JVM Android y 8 pruebas del backend correctas**.
- Compilación debug y release correctas. El APK release compilado es unsigned y necesita configurar una API HTTPS para un despliegue fuera del entorno local.
- Lint debug: cero errores y dieciséis avisos de metadatos, estilo y dependencias.

## Arranque automático y estado final

La configuración Run ejecuta `:app:startLocalServices`, y `preDebugBuild` también depende de esa tarea. En la prueba de arranque se verificó la propiedad de los procesos, se apagaron la API y PostgreSQL del proyecto sin borrar sus datos y se ejecutó la instalación desde Gradle: los servicios volvieron a iniciarse automáticamente. Esta verificación ejecutó las tareas usadas por Run; no automatizó el botón del IDE.

En la compilación final se ejecutó `:app:installDebug` sin `PERSONAS_API_URL` de prueba: PostgreSQL quedó aceptando conexiones en 55440 y `/api/health` confirmó `estado: ok` y `baseDatos: PostgreSQL` en 8081. La app se abrió y cargó las profesiones habituales. No quedó apuntando al puerto temporal de verificación. No se detectaron errores AndroidRuntime del proceso durante el recorrido.

Las credenciales, el cluster, archivos de fotos de usuarios y logs locales permanecen excluidos de Git. Las capturas publicadas corresponden únicamente a datos de prueba.


## Modelos, dependencias y carpetas de fotografías

La organización utiliza modelos separados y contratos `PersonasRepository` y `FotosRepository`. `AppContainer` proporciona las implementaciones mediante una fábrica con `SavedStateHandle`. El ViewModel utiliza `ViewModel` y no depende de `Application`, `Context`, `Uri` ni `FileProvider`.

- JVM Android: 16 casos correctos, incluidos siete del ViewModel con repositorios sustitutos. [Resultados](docs/pruebas-unitarias.json).
- Backend: ocho casos correctos de validación y almacenamiento, incluidos rollback, lectura de rutas antiguas y protección de directorios.
- API/PostgreSQL: 31 comprobaciones correctas. La ruta contiene el DNI y la restricción SQL impide asignar la carpeta de otra persona.
- Android: seis pruebas instrumentales correctas.
- Recorrido desde el emulador: alta, búsqueda, edición, galería, duplicados y eliminación lógica con las dependencias inyectadas. La carpeta del DNI y el archivo permanecen después de cambiar el estado a INACTIVO.
- Recuperación: cierre intencionado del proceso mientras la cámara está abierta; tras confirmar la foto, la aplicación recupera nombre, DNI, profesión y captura. [Captura restaurada](docs/capturas/camara-restaurada.png).

La foto de la ejecución utiliza una ruta relativa dentro de `00601024/`. El resultado estructurado contiene el nombre del archivo en [pruebas-emulador.json](docs/pruebas-emulador.json). Las fotografías y la base del recorrido son temporales; los datos habituales permanecen separados.
