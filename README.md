# Gestor de Incidencias OpenSource — v2.0-OpenSource

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
│   └── UpdateService.java
├── utils/
│   ├── FailureSimulator.java
│   └── LoggerUtil.java
└── view/
    ├── ConfigDialogResult.java
    ├── IncidenciaView.java
    └── SmtpConfigResult.java
src/main/resources/  (Rexistro de incidencia.odt, icon.png)
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

`informatica.ies.teis@edu.xunta.gal` es la cuenta oficial del centro y **solo actúa como destinatario** de las incidencias: no se puede usar para enviar. Su contraseña real no se publica.

La edición pública **no tiene cuenta remitente por defecto**. Para enviar hay que pulsar **«Iniciar Sesión»** y entrar con una cuenta propia (SMTP con contraseña de aplicación, o Google). Sin sesión iniciada, la aplicación lo indica y no envía nada.

### Variables de entorno

Ver `.env.example`.

| Variable | Uso |
| --- | --- |
| `GOOGLE_CLIENT_ID` | Client ID de tu proyecto OAuth (para «Iniciar sesión con Google») |
| `GOOGLE_CLIENT_SECRET` | Client secret de tu proyecto OAuth |

## Historial de incidencias

Cada incidencia enviada se registra en `historial_incidencias.sql` (carpeta de datos de la aplicación: `%APPDATA%\Incidencias` en Windows, `~/.config/Incidencias` en Linux). Es un script SQL con una tabla `incidencias` y un `INSERT` por registro, con los campos: `fecha`, `hora`, `curso`, `taller`, `equipo`, `alumno`, `profesor`, `problema` y `destinatario`.

## Actualizaciones

`UpdateService` comprueba al arrancar si hay una versión más nueva en la ruta de red interna `Z:\incidencias` (ficheros `Incidencia_<versión>.jar|.exe|.AppImage`). Si la ruta no existe, la comprobación se ignora sin error.

## Compilación

Con Java 25 y Maven:

```bash
mvn clean package
```

El workflow `.github/workflows/compilar.yml` construye el AppImage de Linux. Para generar el PDF hace falta tener LibreOffice instalado.
