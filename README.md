# Gestor de Incidencias — v2.0-OpenSource

> **⚠️ Aviso sobre esta edición pública**
>
> Tanto el código de esta edición pública como este README fueron **convertidos por completo por Claude** (IA de Anthropic) a partir de la versión privada del proyecto. **No han sido revisados en profundidad** por una persona: pueden contener errores, comportamientos distintos a los de la versión privada o fallos de seguridad. Además, la compilación con Maven y el funcionamiento con un almacén de credenciales real no se han comprobado tras la conversión. Úsalo bajo tu propia responsabilidad y revisa el código antes de usarlo con cuentas o datos reales.

Aplicación de escritorio en Java (Swing) que rellena la plantilla oficial de incidencias (`Rexistro de incidencia.odt`), la convierte a PDF con LibreOffice y la envía por correo. Esta es la **edición OpenSource del código fuente**: es una copia funcional de la versión completa, sin los secretos reales ni la parte privada de ofuscación.

## Estructura

```text
src/main/java/com/incidencias/
├── Main.java
├── Version.java
├── controller/
│   └── IncidenciaController.java
├── model/
│   ├── Configuracion.java
│   └── ConfiguracionPublica.java
├── service/
│   ├── CryptoService.java        (solo integración con el keyring)
│   ├── GoogleAuthService.java
│   ├── HistorialService.java
│   ├── MailService.java
│   ├── OdtService.java
│   ├── SonidoService.java        (reproduce el MP3 del tema URSS)
│   └── UpdateService.java
├── utils/
│   ├── FailureSimulator.java
│   └── LoggerUtil.java
└── view/
    ├── ConfigDialogResult.java
    ├── IncidenciaView.java
    ├── SmtpConfigResult.java
    └── TemaUI.java               (temas de la interfaz)
src/main/resources/  (Rexistro de incidencia.odt, icon.png, y opcionalmente URSS.png y URSS.mp3)
icon.ico
pom.xml
.github/workflows/compilar.yml
```

## Credenciales y `java-keyring`

La aplicación guarda las credenciales del usuario en el **almacén de credenciales del sistema operativo** mediante [`java-keyring`](https://github.com/javakeyring/java-keyring).

- `CryptoService` contiene **solamente la integración mínima con el keyring**: guardar, recuperar y eliminar una credencial. Usa el servicio `IncidenciasApp` y las cuentas técnicas `SMTP_User` (contraseña SMTP) y `Google_RefreshToken` (refresh token de Google OAuth). No incluye ningún secreto.
- `Config.json` **no contiene la contraseña ni el token**. Solo guarda una referencia:

```json
{
  "email": "usuario@ejemplo.com",
  "password_os": "KEYRING:saved",
  "auth_method": "PASSWORD",
  "google_refresh_token": ""
}
```

  Con inicio de sesión de Google, `google_refresh_token` vale `"KEYRING:saved"` y `password_os` queda vacío.
- El secreto real permanece únicamente en el almacén de credenciales del sistema (Windows Credential Manager, Keychain de macOS, Secret Service en Linux).
- Al introducir una contraseña SMTP se guarda en el keyring; al enviar un correo se recupera de él; al borrar las credenciales desde la aplicación se elimina también del keyring.
- Las credenciales reales de producción **no están publicadas**: ni contraseñas, ni client secrets, ni refresh tokens.
- La implementación privada de cifrado (AES/DPAPI/ofuscación) **deliberadamente no está incluida** en esta edición.

### Cuenta oficial y cuenta remitente

`informatica.ies.teis@edu.xunta.gal` es la cuenta que recibe las incidencias. 

La edición pública **no tiene cuenta remitente por defecto**. Para enviar hay que pulsar **«Iniciar Sesión»** y entrar con una cuenta propia (SMTP con contraseña de aplicación, o Google). Sin sesión iniciada, la aplicación lo indica y no envía nada.

### Variables de entorno

Ver `.env.example`.

| Variable | Uso |
| --- | --- |
| `GOOGLE_CLIENT_ID` | Client ID de tu proyecto OAuth (para «Iniciar sesión con Google») |
| `GOOGLE_CLIENT_SECRET` | Client secret de tu proyecto OAuth |

## Temas de la interfaz

En **Ajustes → Tema de la interfaz** se puede elegir entre:

| Tema | Descripción |
| --- | --- |
| Automático | Sigue el modo claro/oscuro del sistema (Windows y macOS; en Linux queda en claro) |
| Claro | Siempre claro |
| Oscuro | Siempre oscuro |
| URSS | Rojo bandera con letras y acentos dorados; el título pasa a «Nuestro gestor de incidencias», cambia el logo y puede sonar una música |

El cambio se aplica al pulsar **Guardar**, sin reiniciar. La elección se guarda en `Config.json`:

```json
{
  "tema": "URSS",
  "sonido_urss": true
}
```

`tema` admite `AUTO`, `CLARO`, `OSCURO` o `URSS` (si falta o no es válido, se usa Automático).

### Tema URSS: logo y sonido

Junto a la opción URSS hay una casilla **Sonido**, marcada por defecto (solo se puede cambiar cuando URSS es el tema elegido). El sonido suena una vez al cambiar a URSS, al marcar la casilla y al arrancar la aplicación con ese tema; se corta al cambiar de tema o desmarcarla.

Los archivos se colocan en `src/main/resources/`:

| Archivo | Uso |
| --- | --- |
| `URSS.png` | Logo de la ventana y de la barra de tareas con el tema URSS (Swing no lee `.ico`) |
| `URSS.mp3` | Sonido del tema URSS (se reproduce con [JLayer](https://github.com/umjammer/jlayer), LGPL) |

Ambos son opcionales: si no existen, la aplicación conserva el logo normal y no reproduce nada, sin dar error. El icono del `.exe` (`icon.ico`) lo fija Launch4j al compilar y no cambia con el tema.

Las paletas están en `TemaUI` y se aplican con `FlatLaf.setGlobalExtraDefaults(...)`, así que para retocar colores o añadir un tema nuevo basta con tocar esa clase.

## Historial de incidencias

Cada incidencia enviada se registra en `historial_incidencias.sql` (carpeta de datos de la aplicación: `%APPDATA%\Incidencias_OS` en Windows, `~/.config/Incidencias_OS` en Linux). Es un script SQL con una tabla `incidencias` y un `INSERT` por registro, con los campos: `fecha`, `hora`, `curso`, `taller`, `equipo`, `alumno`, `profesor`, `problema` y `destinatario`.

## Actualizaciones

`UpdateService` comprueba al arrancar si hay una versión más nueva en la ruta de red interna `Z:\incidencias` (ficheros `Incidencia_<versión>-OpenSource.jar|.exe|.AppImage`). Si la ruta no existe, la comprobación se ignora sin error.

## Compilación

Con Java 25 y Maven:

```bash
mvn clean package
```

El workflow `.github/workflows/compilar.yml` construye el AppImage de Linux. Para generar el PDF hace falta tener LibreOffice instalado.
