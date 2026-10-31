# Music Player

Reproductor de música para Android desarrollado con Kotlin y Jetpack Compose. Explora las canciones de audio disponibles en el dispositivo y permite organizarlas y reproducirlas desde una interfaz con controles compactos y un reproductor ampliado.

## Funcionalidades

- Explorar canciones locales y agruparlas por carpeta, artista, álbum o género.
- Buscar canciones por título o artista.
- Ordenar la biblioteca por título, artista, álbum o fecha de incorporación, en orden ascendente o descendente.
- Reproducir, pausar, avanzar, retroceder y buscar una posición dentro de la canción.
- Activar reproducción aleatoria y repetición.
- Consultar listas de favoritas, más escuchadas y música reciente.
- Ocultar carpetas de la biblioteca y volver a mostrarlas desde los ajustes.
- Eliminar canciones usando el flujo de confirmación de Android cuando está disponible.
- Mantener la reproducción en segundo plano mediante un servicio multimedia y mostrar los controles del sistema.
- Guardar preferencias, como favoritos, carpetas ocultas y la última canción seleccionada.

## Requisitos

- Android Studio con soporte para proyectos Android y Kotlin.
- JDK 17.
- Android SDK 34 para compilar.
- Dispositivo o emulador con Android 8.0 (API 26) o posterior.

La aplicación tiene `minSdk 26` y `targetSdk 34`. La primera vez que se inicia, solicita los permisos necesarios para consultar audio local; en Android 13 o posterior también solicita permiso para notificaciones.

## Ejecutar la aplicación

1. Clona el repositorio y abre la carpeta del proyecto en Android Studio.
2. Deja que Gradle sincronice las dependencias.
3. Conecta un dispositivo Android (API 26+) o inicia un emulador.
4. Ejecuta la configuración `app` desde Android Studio y concede los permisos solicitados.

También puedes compilar desde PowerShell usando el Gradle Wrapper:

```powershell
.\gradlew.bat assembleDebug
```

Para instalar la versión de depuración en un dispositivo conectado:

```powershell
.\gradlew.bat installDebug
```

El APK de depuración se genera en `app/build/outputs/apk/debug/`.

## Tecnologías

- Kotlin 1.9.24 y Android Gradle Plugin 8.5.2.
- Jetpack Compose y Material 3 para la interfaz.
- AndroidX Lifecycle y `ViewModel` para el estado de la aplicación.
- AndroidX Media3 ExoPlayer y MediaSession para reproducción y controles multimedia.
- MediaStore para consultar la biblioteca de audio del dispositivo.
- SharedPreferences para preferencias y datos sencillos de la biblioteca.

## Estructura del proyecto

```text
app/src/main/
├── AndroidManifest.xml
└── java/com/musicplayer/app/
    ├── data/
    │   ├── local/       # Consulta de música y almacenamiento local
    │   └── model/       # Modelos de canciones y opciones de orden
    ├── service/         # Reproducción, sesión multimedia y audio
    ├── ui/              # Pantallas, componentes y tema
    ├── viewmodel/       # Estado y lógica de la biblioteca y reproducción
    └── MainActivity.kt  # Actividad de entrada y solicitud de permisos
```

## Permisos

La aplicación solicita permisos para leer audio del dispositivo. En versiones anteriores a Android 13 utiliza `READ_EXTERNAL_STORAGE`; en Android 13 y posteriores utiliza `READ_MEDIA_AUDIO`. También declara permisos de notificaciones y de servicio en primer plano para integrar la reproducción con las notificaciones y los controles multimedia del sistema.
