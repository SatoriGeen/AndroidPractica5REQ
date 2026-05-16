**Práctica 5:** Consulta de Base de Datos vía APIs y Funcionalidades Específicas en Aplicaciones Nativas  
**Alumno:** José Julio Pérez Olivares  

## Introducción y Arquitectura General
Esta aplicación móvil nativa (`ApiRestClient`) implementa una solución robusta cliente-servidor tolerante a fallos de red y desconexiones (Modo Offline). El sistema consume asíncronamente las APIs públicas internacionales **TVMaze** (para series) y **Open Library** (para libros y autores) mediante respuestas JSON estructuradas, persistiendo los datos de manera local y relacional bajo el motor interno de SQLite.

### Componentes de la Arquitectura Nativa:
* **Capa de Red Asíncrona (Retrofit + GSON):** Las llamadas a microservicios corren en hilos de fondo independientes (Background Worker Threads) evitando el congelamiento de la aplicación.
* **Sincronización en el Main UI Thread:** Utiliza el método nativo `runOnUiThread` para mutar la interfaz gráfica de forma segura y coordinada tras la persistencia en disco duro.
* **Persistencia Relacional Local:** Base de datos estructurada con esquemas definidos para `usuarios`, `historial` y `favoritos` mediante `SQLiteOpenHelper`.

---

## 2. Manual de Operación y Funcionalidades Específicas

La aplicación cuenta con una interfaz gráfica moderna integrada mediante contenedores tridimensionales de elevación (`CardView`) y espaciados dinámicos que se adaptan al flujo:

### A. Flujo de Búsqueda Interactiva y Persistencia (Ejercicio 2)
1. Al iniciar, se despliega el asistente obligatorio de autenticación federada mediante el SDK de **Google Play Services**.
2. Una vez guardado el usuario, escribe el nombre de un título en el campo de texto.
3. Presiona **"Series"** o **"Libros"** para disparar la llamada HTTP asíncrona. 
4. Los resultados se procesan, se guardan automáticamente en la tabla `favoritos` y la consulta en la tabla `historial` de SQLite, y se renderizan de inmediato en la pantalla.

### B. Sistema de Recomendaciones Inteligente (Ejercicio 3)
* Cada vez que se procesa una acción en la base de datos, la tarjeta azul de **Recomendación Personalizada** ejecuta de forma reactiva un algoritmo en la clase `SistemaRecomendaciones`. 
* Este motor calcula las frecuencias de los términos buscados y las fuentes más consultadas del usuario activo para sugerirle nuevo contenido afín en tiempo real, operando de manera 100% autónoma incluso en **Modo Avión (Offline)**.

### C. Mecanismo de Sincronización Periódica
* El botón verde **"Sincronizar Base Local"** simula de forma estructurada un proceso periódico de sincronización de fondo, barriendo la base local y actualizando las llaves relacionales con los servidores remotos para garantizar la consistencia de datos en todo el ciclo de vida.

---

## 3. Guía de Acceso: Cómo Ingresar al Modo Administrador (Auditoría Global)
De acuerdo con los requerimientos del PDF, el sistema discrimina accesos exclusivos mediante roles de usuario. 

Para activar el **Panel de Auditoría del Administrador** en el entorno de desarrollo:
1. Abre el controlador principal `MainActivity.kt`.
2. Modifica la variable del entorno en el interceptor del login (Línea **24** o **101**) para forzar el correo corporativo del administrador:
   ```kotlin
   correoUsuarioActivo = "thebestonegift1@gmail.com"
