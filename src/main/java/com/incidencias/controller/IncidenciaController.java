package com.incidencias.controller;

import com.google.api.client.auth.oauth2.TokenResponseException;
import com.incidencias.model.Configuracion;
import com.incidencias.model.ConfiguracionPublica;
import com.incidencias.service.CryptoService;
import com.incidencias.service.GoogleAuthService;
import com.incidencias.service.HistorialService;
import com.incidencias.service.MailService;
import com.incidencias.service.OdtService;
import com.incidencias.service.UpdateService;
import com.incidencias.utils.LoggerUtil;
import com.incidencias.view.ConfigDialogResult;
import com.incidencias.view.IncidenciaView;
import com.incidencias.service.SonidoService;
import com.incidencias.view.SmtpConfigResult;
import com.incidencias.view.TemaUI;
import com.incidencias.view.TemaUI.Tema;

import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import javax.swing.SwingUtilities;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public class IncidenciaController {

    private final IncidenciaView vista;
    private final Configuracion modelo;

    public IncidenciaController(IncidenciaView vista, Configuracion modelo) {
        this.vista = vista;
        this.modelo = modelo;

        this.vista.getBtnConfigCorreo().addActionListener(e -> onConfigurarCorreo());
        this.vista.getBtnGenerar().addActionListener(e -> onGenerarIncidencia());
        this.vista.getBtnAjustes().addActionListener(e -> onAjustesSmtp());
        this.vista.getBtnHistorial().addActionListener(e -> onMostrarHistorial());
    }

    public void iniciar() {
        LoggerUtil.log("CTRL", "Poblando campos iniciales en la vista desde Configuracion...");
        vista.setCurso(modelo.getCurso());
        vista.setTaller(modelo.getTaller());
        vista.setEquipo(modelo.getEquipo());
        vista.setAlumno(modelo.getAlumno());

        boolean necesitaGuardar = false;

        if (referenciaNoCompatible(modelo.getPasswordReferencia())) {
            LoggerUtil.log("CRYPTO", "Referencia de contraseña no compatible con el keyring: se descarta y deberá introducirse de nuevo.");
            modelo.setPasswordReferencia("");
            necesitaGuardar = true;
        }
        if (referenciaNoCompatible(modelo.getGoogleRefreshTokenReferencia())) {
            LoggerUtil.log("CRYPTO", "Referencia de token no compatible con el keyring: se descarta y deberá iniciarse sesión de nuevo.");
            modelo.setGoogleRefreshTokenReferencia("");
            necesitaGuardar = true;
        }

        if (!com.incidencias.Version.NUMERO.equals(modelo.getVersion())) {
            LoggerUtil.log("CONFIG", "Actualizando versión en Config.json de " + modelo.getVersion() + " a "
                    + com.incidencias.Version.NUMERO);
            modelo.setVersion(com.incidencias.Version.NUMERO);
            necesitaGuardar = true;
        }

        if (necesitaGuardar) {
            modelo.guardarDatos();
        }

        actualizarUI();
        vista.setVisible(true);
        vista.enfocarCajaProblema();

        LoggerUtil.log("CTRL", "Ventana principal visible. Lanzando comprobación de actualizaciones...");
        UpdateService.comprobarActualizaciones(vista);
    }

    private void actualizarUI() {
        LoggerUtil.log("UI", String.format(
                "Actualizando barra de estado -> Usuario='%s', Metodo='%s', Debug=%b, DebugEmail='%s'",
                modelo.getCorreoUsuario(), modelo.getMetodoAutenticacion(),
                modelo.isModoDebugActivado(), modelo.getCorreoDestinoDebug()));
        vista.actualizarEtiquetaEstado(
                modelo.getCorreoUsuario(),
                "",
                modelo.getMetodoAutenticacion(),
                modelo.isModoDebugActivado(),
                modelo.getCorreoDestinoDebug());
    }

    private void onMostrarHistorial() {
        LoggerUtil.log("HISTORIAL", "Botón Historial pulsado. Leyendo historial_incidencias.sql...");
        try {
            List<HistorialService.RegistroIncidencia> historial = HistorialService.leerHistorial();
            LoggerUtil.log("HISTORIAL", "Registros leídos del historial: " + historial.size());
            vista.mostrarDialogoHistorial(historial);
        } catch (IOException ex) {
            LoggerUtil.error("HISTORIAL", "Error al leer historial_incidencias.sql", ex);
            vista.mostrarMensaje("Error al leer el archivo historial_incidencias.sql:\n" + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onAjustesSmtp() {
        LoggerUtil.log("AJUSTES", "Abriendo diálogo de Ajustes...");
        actualizarUI();

        SmtpConfigResult r = vista.mostrarDialogoAjustesSmtp(
                modelo.getSmtpHost(), modelo.getSmtpPuerto(), modelo.isSmtpSSL(),
                modelo.isModoDebugActivado(), modelo.getCorreoDestinoDebug(),
                Tema.desdeNombre(modelo.getTema()), modelo.isSonidoUrss());

        if (r != null && r.guardado) {
            LoggerUtil.log("AJUSTES", String.format(
                    "Usuario pulsó Guardar en Ajustes -> Host='%s', Puerto='%s', SSL=%b, DebugEmail='%s'",
                    r.host, r.puerto, r.ssl, r.debugEmail));
            if (r.host.isEmpty() || r.puerto.isEmpty()) {
                LoggerUtil.log("AJUSTES", "Validación fallida: host o puerto vacíos.");
                vista.mostrarMensaje("El servidor y el puerto no pueden estar vacíos.", "Datos incompletos",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            try {
                Integer.parseInt(r.puerto);
            } catch (NumberFormatException ex) {
                LoggerUtil.log("AJUSTES", "Validación fallida: puerto no numérico (" + r.puerto + ").");
                vista.mostrarMensaje("El puerto debe ser un número.", "Datos incompletos", JOptionPane.WARNING_MESSAGE);
                return;
            }
            modelo.setSmtpHost(r.host);
            modelo.setSmtpPuerto(r.puerto);
            modelo.setSmtpSSL(r.ssl);
            if (modelo.isModoDebugActivado()) {
                modelo.setCorreoDestinoDebug(r.debugEmail);
            }
            Tema temaAnterior = Tema.desdeNombre(modelo.getTema());
            boolean sonabaAntes = (temaAnterior == Tema.URSS) && modelo.isSonidoUrss();
            Tema temaNuevo = Tema.desdeNombre(r.tema);
            modelo.setTema(temaNuevo.name());
            modelo.setSonidoUrss(r.sonidoUrss);
            modelo.guardarDatos();

            // En AUTO se vuelve a aplicar siempre, por si el sistema ha cambiado de modo desde el arranque
            if (temaNuevo != temaAnterior || temaNuevo == Tema.AUTO) {
                LoggerUtil.log("AJUSTES", "Aplicando tema de la interfaz: " + temaNuevo.name());
                TemaUI.aplicarYRefrescar(temaNuevo);
                vista.actualizarIdentidadTema(temaNuevo);
            }

            // Sonido del tema URSS: suena al entrar en URSS o al marcar «Sonido»; se corta al salir o desmarcarlo
            boolean suenaAhora = (temaNuevo == Tema.URSS) && modelo.isSonidoUrss();
            if (suenaAhora && !sonabaAntes) {
                SonidoService.reproducir(TemaUI.RECURSO_SONIDO_URSS);
            } else if (!suenaAhora) {
                SonidoService.detener();
            }
            // Repinta la barra de estado: su color se fija a mano y el cambio de tema lo restablece
            actualizarUI();
            vista.mostrarMensaje("Ajustes guardados.", "Ajustes", JOptionPane.INFORMATION_MESSAGE);
        } else {
            LoggerUtil.log("AJUSTES", "Diálogo de Ajustes cancelado o cerrado sin cambios.");
        }
    }

    private void onConfigurarCorreo() {
        LoggerUtil.log("AUTH", "Abriendo diálogo de configuración de sesión/correo...");
        actualizarUI();

        char[] passRecuperada;
        try {
            passRecuperada = "GOOGLE".equals(modelo.getMetodoAutenticacion())
                    ? new char[0]
                    : CryptoService.recuperar(CryptoService.CUENTA_SMTP, modelo.getPasswordReferencia());
        } catch (Exception ex) {
            if (modelo.isModoDebugActivado()) {
                LoggerUtil.escribirErrorLog("ERROR", "Fallo al recuperar credenciales del keyring.", ex, modelo,
                        vista);
            }
            vista.mostrarMensaje("Error al acceder al almacén de credenciales del sistema:\n" + ex.getMessage(),
                    "Error de credenciales", JOptionPane.ERROR_MESSAGE);
            return;
        }

        ConfigDialogResult resultado = vista.mostrarDialogoConfiguracion(
                modelo.getCorreoUsuario(),
                passRecuperada,
                modelo.isModoDebugActivado(),
                modelo.getMetodoAutenticacion(),
                modelo.getSmtpHost(),
                modelo.getSmtpPuerto(),
                modelo.isSmtpSSL());

        Arrays.fill(passRecuperada, '\0');
        LoggerUtil.log("AUTH", "Acción seleccionada en diálogo de sesión: " + resultado.accion);

        switch (resultado.accion) {
            case USAR_GOOGLE:
                iniciarSesionGoogle();
                break;

            case GUARDAR_PASSWORD:
                try {
                    if (resultado.correo.isEmpty()) {
                        LoggerUtil.log("AUTH", "Correo en blanco al guardar contraseña: se borrarán las credenciales.");
                        modelo.borrarCredenciales();
                        CryptoService.eliminarTodo();
                    } else {
                        LoggerUtil.log("AUTH", "Guardando credenciales SMTP manuales para: " + resultado.correo);
                        modelo.setMetodoAutenticacion("PASSWORD");
                        modelo.setCorreoUsuario(resultado.correo);
                        modelo.setPasswordReferencia(CryptoService.guardar(CryptoService.CUENTA_SMTP, resultado.password));
                        modelo.setGoogleRefreshTokenReferencia("");
                        CryptoService.eliminar(CryptoService.CUENTA_GOOGLE_REFRESH);
                        if (!resultado.smtpHost.isEmpty())
                            modelo.setSmtpHost(resultado.smtpHost);
                        if (!resultado.smtpPuerto.isEmpty())
                            modelo.setSmtpPuerto(resultado.smtpPuerto);
                        modelo.setSmtpSSL(resultado.smtpSSL);
                    }
                    modelo.guardarDatos();
                    actualizarUI();
                    vista.mostrarMensaje("Configuración actualizada.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                } finally {
                    resultado.limpiarPassword();
                }
                break;

            case BORRAR:
                LoggerUtil.log("AUTH", "Borrando credenciales almacenadas y limpiando el keyring del sistema...");
                modelo.borrarCredenciales();
                CryptoService.eliminarTodo();
                modelo.guardarDatos();
                actualizarUI();
                vista.mostrarMensaje("Se han borrado las credenciales.", "Info", JOptionPane.INFORMATION_MESSAGE);
                break;

            case CANCELAR:
            default:
                break;
        }
    }

    private void iniciarSesionGoogle() {
        LoggerUtil.log("OAUTH", "Iniciando flujo OAuth 2.0 con Google...");
        vista.mostrarMensaje(
                "Se abrirá tu navegador para iniciar sesión con Google.\nCompleta el proceso allí y vuelve a esta ventana.",
                "Iniciar sesión con Google", JOptionPane.INFORMATION_MESSAGE);

        Thread.startVirtualThread(() -> {
            try {
                GoogleAuthService.ResultadoAuth auth = GoogleAuthService.iniciarFlujoOAuth();
                LoggerUtil.log("OAUTH", "Autorización OAuth completada con éxito para la cuenta: " + auth.correo);
                modelo.setMetodoAutenticacion("GOOGLE");
                modelo.setCorreoUsuario(auth.correo);
                modelo.setPasswordReferencia("");
                CryptoService.eliminar(CryptoService.CUENTA_SMTP);
                modelo.setGoogleRefreshTokenReferencia(guardarRefreshToken(auth.refreshToken));
                modelo.guardarDatos();

                SwingUtilities.invokeLater(() -> {
                    actualizarUI();
                    vista.mostrarMensaje("Sesión de Google iniciada como: " + auth.correo, "Éxito",
                            JOptionPane.INFORMATION_MESSAGE);
                });
            } catch (Exception ex) {
                if (ex instanceof TokenResponseException) {
                    LoggerUtil.logOAuthError("OAUTH", ex);
                }
                SwingUtilities.invokeLater(() -> {
                    if (modelo.isModoDebugActivado()) {
                        LoggerUtil.escribirErrorLog("ERROR", "Fallo al iniciar sesión con Google.", ex, modelo, vista);
                    }
                    vista.mostrarMensaje("No se pudo iniciar sesión con Google:\n" + ex.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                });
            }
        });
    }

    private void onGenerarIncidencia() {
        LoggerUtil.log("ENVIO", "=== INICIO DE PROCESO DE GENERACIÓN Y ENVÍO DE INCIDENCIA ===");
        if (vista.getTaller().trim().isEmpty()
                || (vista.getAlumno().trim().isEmpty() && vista.getProfesor().trim().isEmpty())) {
            LoggerUtil.log("ENVIO", "Abortado: Faltan campos obligatorios (Taller y Alumno/Profesor).");
            vista.mostrarMensaje(
                    "Por favor, completa Taller y al menos Alumno o Profesor antes de generar la incidencia.",
                    "Faltan datos", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (!OdtService.existeLibreOffice()) {
            LoggerUtil.log("ENVIO", "Abortado: LibreOffice no encontrado (o emulador de fallo activado).");
            vista.mostrarMensaje(
                    "No se ha podido convertir el documento a PDF.\n" +
                            "Se requiere LibreOffice instalado.",
                    "Error de Dependencia", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (modelo.getCorreoUsuario().isEmpty()) {
            LoggerUtil.log("ENVIO", "Abortado: no hay sesión iniciada (la edición pública no tiene cuenta remitente por defecto).");
            vista.mostrarMensaje(
                    "Para enviar la incidencia primero tienes que iniciar sesión con tu correo.\n"
                            + "Pulsa «Iniciar Sesión» y usa tu cuenta (SMTP o Google).",
                    "Falta iniciar sesión", JOptionPane.WARNING_MESSAGE);
            return;
        }

        guardarEstadoActual();
        actualizarUI();

        String tallerSaneado = vista.getTaller().trim().replaceAll("[\\\\/:*?\"<>|]", "_");
        String nombreBase = "Incidencia_Taller_" + tallerSaneado;

        new SwingWorker<Void, String>() {
            @Override
            protected Void doInBackground() throws Exception {
                Path dirTemporal = null;
                Path archivoOdtTemporal = null;
                Path archivoPdfTemporal = null;

                try {
                    publish("Generando documento temporal...");
                    dirTemporal = Files.createTempDirectory("incidencia_pdf_");
                    archivoOdtTemporal = dirTemporal.resolve(nombreBase + ".odt");
                    archivoPdfTemporal = dirTemporal.resolve(nombreBase + ".pdf");
                    LoggerUtil.log("ODT/PDF", "Carpeta temporal creada: " + dirTemporal.toAbsolutePath());

                    publish("Generando documento...");
                    OdtService.generarPDFDesdeODT(archivoOdtTemporal, archivoPdfTemporal, vista);

                    publish("Preparando credenciales de envío...");

                    String metodo = modelo.getMetodoAutenticacion();
                    final char[] passEnvioChars;
                    final String smtpHost;
                    final int smtpPuerto;
                    final boolean smtpSSL;
                    final boolean esOAuth;

                    final boolean esCuentaPorDefecto = modelo.getCorreoUsuario().isEmpty();
                    LoggerUtil.log("AUTH-ENVIO", "Modo de cuenta: "
                            + (esCuentaPorDefecto ? "SIN SESIÓN" : "PERSONALIZADA (" + metodo + ")"));

                    if (!esCuentaPorDefecto && "GOOGLE".equals(metodo)) {
                        LoggerUtil.log("OAUTH", "Recuperando Refresh Token de Google del keyring y solicitando Access Token...");
                        char[] refresh = CryptoService.recuperar(CryptoService.CUENTA_GOOGLE_REFRESH, modelo.getGoogleRefreshTokenReferencia());
                        if (refresh == null || refresh.length == 0) {
                            throw new IOException(
                                    "No se encontró el token de sesión de Google. Vuelve a iniciar sesión.");
                        }
                        try {
                            String accessToken = GoogleAuthService.obtenerAccessToken(new String(refresh));
                            LoggerUtil.log("OAUTH", "Access Token obtenido correctamente (longitud: "
                                    + accessToken.length() + " chars).");
                            passEnvioChars = accessToken.toCharArray();
                        } catch (TokenResponseException tokenEx) {
                            throw new IOException("La sesión de Google ha expirado. Vuelve a iniciar sesión.");
                        } finally {
                            Arrays.fill(refresh, '\0');
                        }
                        smtpHost = "smtp.gmail.com";
                        smtpPuerto = 587;
                        smtpSSL = false;
                        esOAuth = true;
                    } else if (!esCuentaPorDefecto) {
                        LoggerUtil.log("AUTH-ENVIO", "Recuperando contraseña SMTP del keyring...");
                        passEnvioChars = CryptoService.recuperar(CryptoService.CUENTA_SMTP, modelo.getPasswordReferencia());
                        if (passEnvioChars.length == 0) {
                            throw new IOException("No hay contraseña guardada en el almacén del sistema. Vuelve a configurar el correo.");
                        }
                        smtpHost = modelo.getSmtpHost();
                        smtpPuerto = parsearPuerto(modelo.getSmtpPuerto(), 465);
                        smtpSSL = modelo.isSmtpSSL();
                        esOAuth = false;
                    } else {
                        throw new IOException("No hay sesión iniciada. Pulsa «Iniciar Sesión» y usa tu propio correo.");
                    }

                    try {
                        String destinatario = (!modelo.isModoDebugActivado()
                                || modelo.getCorreoDestinoDebug().isEmpty())
                                        ? ConfiguracionPublica.obtenerCorreoDestinatarioDefecto()
                                        : modelo.getCorreoDestinoDebug();
                        String correoEnvio = modelo.getCorreoUsuario();
                        String asunto = "incidencia Taller " + vista.getTaller();

                        LoggerUtil.log("SMTP", String.format(
                                "Parámetros de envío -> Remitente='%s', Destinatario='%s', Host='%s:%d', SSL=%b, OAuth=%b",
                                correoEnvio, destinatario, smtpHost, smtpPuerto, smtpSSL, esOAuth));

                        publish("Enviando correo con PDF adjunto...");
                        MailService.enviarCorreoSMTP(archivoPdfTemporal, correoEnvio, passEnvioChars,
                                destinatario,
                                asunto, smtpHost, smtpPuerto, smtpSSL, esOAuth, esCuentaPorDefecto);

                        publish("Registrando incidencia en historial...");
                        LoggerUtil.log("HISTORIAL", "Guardando registro en historial_incidencias.sql...");
                        HistorialService.guardarIncidencia(
                                vista.getCurso().trim(),
                                vista.getTaller().trim(),
                                vista.getEquipo().trim(),
                                vista.getAlumno().trim(),
                                vista.getProfesor().trim(),
                                vista.getProblema().trim(),
                                destinatario);
                    } finally {
                        if (passEnvioChars != null) {
                            Arrays.fill(passEnvioChars, '\0');
                        }
                    }
                } finally {
                    LoggerUtil.log("CLEANUP", "Eliminando archivos temporales ODT/PDF...");
                    if (archivoOdtTemporal != null) {
                        try {
                            Files.deleteIfExists(archivoOdtTemporal);
                        } catch (IOException ignored) {
                        }
                    }
                    if (archivoPdfTemporal != null) {
                        try {
                            Files.deleteIfExists(archivoPdfTemporal);
                        } catch (IOException ignored) {
                        }
                    }
                    if (dirTemporal != null) {
                        try {
                            Files.deleteIfExists(dirTemporal);
                        } catch (IOException ignored) {
                        }
                    }
                }
                return null;
            }

            @Override
            protected void process(List<String> chunks) {
                if (!chunks.isEmpty()) {
                    vista.mostrarProgreso(chunks.get(chunks.size() - 1));
                }
            }

            @Override
            protected void done() {
                vista.ocultarProgreso();
                try {
                    get();
                    String destinatario = (!modelo.isModoDebugActivado() || modelo.getCorreoDestinoDebug().isEmpty())
                            ? ConfiguracionPublica.obtenerCorreoDestinatarioDefecto()
                            : modelo.getCorreoDestinoDebug();
                    LoggerUtil.escribirLog("INFO", "Incidencia enviada y registrada con éxito.", modelo, vista);
                    vista.mostrarMensaje("Incidencia enviada a: " + destinatario, "Éxito",
                            JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    if (modelo.isModoDebugActivado()) {
                        LoggerUtil.escribirErrorLog("ERROR CRÍTICO", ex, modelo, vista);
                    }
                    String mensajeError = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
                    vista.mostrarMensaje("Error al procesar o enviar la incidencia:\n" + mensajeError,
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private int parsearPuerto(String puerto, int porDefecto) {
        try {
            return Integer.parseInt(puerto.trim());
        } catch (Exception e) {
            return porDefecto;
        }
    }

    private void guardarEstadoActual() {
        modelo.setCurso(vista.getCurso());
        modelo.setTaller(vista.getTaller());
        modelo.setEquipo(vista.getEquipo());
        modelo.setAlumno(vista.getAlumno());
        modelo.guardarDatos();
    }

    private static boolean referenciaNoCompatible(String referencia) {
        return referencia != null && !referencia.isBlank() && !CryptoService.esReferenciaKeyring(referencia);
    }

    private static String guardarRefreshToken(String token) {
        char[] chars = token.toCharArray();
        try {
            return CryptoService.guardar(CryptoService.CUENTA_GOOGLE_REFRESH, chars);
        } finally {
            Arrays.fill(chars, '\0');
        }
    }
}
