package com.incidencias.utils;

import com.incidencias.Version;
import com.incidencias.model.Configuracion;
import com.incidencias.view.IncidenciaView;

import javax.swing.*;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LoggerUtil {

    private static final String ARCHIVO_LOG = "debug_incidencias.log";
    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private static JFrame ventanaConsola;
    private static JTextArea areaConsola;
    private static JCheckBox chkAutoScroll;
    private static PrintStream outOriginal;
    private static PrintStream errOriginal;
    private static boolean consolaIniciada = false;

    /**
     * Inicia la ventana de consola de depuración y redirige System.out / System.err
     * exclusivamente si estamos en la compilación _deb (Version.isModoDebug() ==
     * true).
     */
    public static synchronized void iniciarConsolaDebug() {
        if (!Version.isModoDebug() || consolaIniciada) {
            return;
        }
        consolaIniciada = true;
        outOriginal = System.out;
        errOriginal = System.err;

        Runnable crearUI = () -> {
            ventanaConsola = new JFrame("Consola de Depuración Exhaustiva - v" + Version.NUMERO + " [MODO DEBUG]");
            ventanaConsola.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
            ventanaConsola.setSize(860, 650);
            ventanaConsola.setAutoRequestFocus(false);
            ventanaConsola.setFocusableWindowState(false);

            java.net.URL urlIcono = LoggerUtil.class.getResource("/icon.png");
            if (urlIcono != null) {
                ventanaConsola.setIconImage(new ImageIcon(urlIcono).getImage());
            }

            areaConsola = new JTextArea();
            areaConsola.setEditable(false);
            areaConsola.setBackground(new Color(15, 15, 18));
            areaConsola.setForeground(new Color(0, 230, 118));
            areaConsola.setCaretColor(Color.WHITE);
            areaConsola.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
            areaConsola.setMargin(new Insets(10, 10, 10, 10));

            DefaultCaret caret = (DefaultCaret) areaConsola.getCaret();
            caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

            JScrollPane scroll = new JScrollPane(areaConsola);
            scroll.setBorder(BorderFactory.createEmptyBorder());

            JPanel barraSuperior = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
            barraSuperior.setBackground(new Color(28, 28, 32));

            JButton btnCopiar = new JButton("Copiar todo");
            btnCopiar.addActionListener(e -> {
                Toolkit.getDefaultToolkit().getSystemClipboard()
                        .setContents(new StringSelection(areaConsola.getText()), null);
            });

            JButton btnLimpiar = new JButton("Limpiar pantalla");
            btnLimpiar.addActionListener(e -> areaConsola.setText(""));

            JButton btnAbrirCarpeta = new JButton("Abrir carpeta Config/Log");
            btnAbrirCarpeta.addActionListener(e -> {
                try {
                    Desktop.getDesktop().open(Configuracion.obtenerDirectorioDatos());
                } catch (Exception ex) {
                    error("CONSOLA", "No se pudo abrir el explorador de archivos", ex);
                }
            });

            chkAutoScroll = new JCheckBox("Auto-scroll", true);
            chkAutoScroll.setForeground(Color.WHITE);
            chkAutoScroll.setOpaque(false);

            barraSuperior.add(btnCopiar);
            barraSuperior.add(btnLimpiar);
            barraSuperior.add(btnAbrirCarpeta);
            barraSuperior.add(chkAutoScroll);

            ventanaConsola.setLayout(new BorderLayout());
            ventanaConsola.add(barraSuperior, BorderLayout.NORTH);
            ventanaConsola.add(scroll, BorderLayout.CENTER);

            // Colocar a la derecha de la pantalla y enviarla al fondo
            Dimension pantalla = Toolkit.getDefaultToolkit().getScreenSize();
            int x = Math.max(0, pantalla.width - ventanaConsola.getWidth() - 30);
            int y = Math.max(0, (pantalla.height - ventanaConsola.getHeight()) / 2);
            ventanaConsola.setLocation(x, y);
            ventanaConsola.setVisible(true);
            ventanaConsola.toBack();

            // Restaurar la capacidad de recibir clics una vez abierta detrás
            SwingUtilities.invokeLater(() -> ventanaConsola.setFocusableWindowState(true));

            volcarDiagnosticoInicial();
        };

        if (SwingUtilities.isEventDispatchThread()) {
            crearUI.run();
        } else {
            SwingUtilities.invokeLater(crearUI);
        }

        // Redirigir System.out y System.err a la consola y al archivo de log
        System.setOut(crearStreamInterceptor(outOriginal, "STDOUT"));
        System.setErr(crearStreamInterceptor(errOriginal, "STDERR"));

        // Capturar cualquier excepción no controlada en cualquier hilo
        Thread.setDefaultUncaughtExceptionHandler((hilo, ex) -> {
            error("UNCAUGHT-THREAD-" + hilo.getName(), "Excepción no controlada detectada", ex);
        });
    }

    public static void enviarConsolaAlFondo() {
        if (Version.isModoDebug() && ventanaConsola != null) {
            SwingUtilities.invokeLater(() -> ventanaConsola.toBack());
        }
    }

    public static void mostrarConsola() {
        if (Version.isModoDebug() && ventanaConsola != null) {
            SwingUtilities.invokeLater(() -> {
                ventanaConsola.setVisible(true);
                ventanaConsola.toFront();
            });
        }
    }

    private static void volcarDiagnosticoInicial() {
        Runtime rt = Runtime.getRuntime();
        RuntimeMXBean mx = ManagementFactory.getRuntimeMXBean();
        long maxMb = rt.maxMemory() / (1024 * 1024);
        long totalMb = rt.totalMemory() / (1024 * 1024);
        long freeMb = rt.freeMemory() / (1024 * 1024);

        log("BOOT", "==================================================================");
        log("BOOT", " INICIANDO CONSOLA DE DEPURACIÓN EXHAUSTIVA - INCIDENCIAS v" + Version.NUMERO);
        log("BOOT", "==================================================================");
        log("ENV", "Sistema Operativo : " + System.getProperty("os.name") + " (" + System.getProperty("os.version")
                + ") [" + System.getProperty("os.arch") + "]");
        log("ENV", "Java Runtime      : " + System.getProperty("java.version") + " ("
                + System.getProperty("java.vm.vendor") + " - " + System.getProperty("java.vm.name") + ")");
        log("ENV", "JAVA_HOME         : " + System.getProperty("java.home"));
        log("ENV", "Usuario OS        : " + System.getProperty("user.name"));
        log("ENV", "Directorio actual : " + System.getProperty("user.dir"));
        log("ENV", "Carpeta de datos  : " + Configuracion.obtenerDirectorioDatos().getAbsolutePath());
        log("ENV", "Archivo de log    : " + Configuracion.obtenerArchivoDatos(ARCHIVO_LOG).getAbsolutePath());
        log("ENV", "Argumentos JVM    : " + mx.getInputArguments());
        log("ENV", "Classpath         : " + System.getProperty("java.class.path"));
        log("ENV", String.format("Memoria JVM       : Usada=%dMB / Asignada=%dMB / Máxima=%dMB",
                (totalMb - freeMb), totalMb, maxMb));
        log("BOOT", "==================================================================");
    }

    private static PrintStream crearStreamInterceptor(PrintStream original, String etiqueta) {
        OutputStream out = new OutputStream() {
            private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

            @Override
            public void write(int b) {
                original.write(b);
                if (b == '\n') {
                    vaciarBuffer();
                } else if (b != '\r') {
                    buffer.write(b);
                }
            }

            @Override
            public void write(byte[] b, int off, int len) {
                original.write(b, off, len);
                for (int i = off; i < off + len; i++) {
                    byte actual = b[i];
                    if (actual == '\n') {
                        vaciarBuffer();
                    } else if (actual != '\r') {
                        buffer.write(actual);
                    }
                }
            }

            private void vaciarBuffer() {
                if (buffer.size() == 0) {
                    return;
                }
                String linea = buffer.toString(StandardCharsets.UTF_8);
                buffer.reset();
                registrarLineaCruda("[" + etiqueta + "] " + linea);
            }
        };
        return new PrintStream(out, true, StandardCharsets.UTF_8);
    }

    private static void registrarLineaCruda(String texto) {
        if (!Version.isModoDebug()) {
            return;
        }
        String timestamp = LocalDateTime.now().format(FORMATO_HORA);
        String lineaCompleta = "[" + timestamp + "] [Hilo:" + Thread.currentThread().getName() + "] " + texto;

        escribirEnConsolaUI(lineaCompleta);
        escribirEnArchivoDisco(lineaCompleta);
    }

    public static void log(String categoria, String mensaje) {
        if (!Version.isModoDebug()) {
            return;
        }
        String timestamp = LocalDateTime.now().format(FORMATO_HORA);
        String linea = String.format("[%s] [INFO ] [%s] [Hilo:%s] %s",
                timestamp, categoria, Thread.currentThread().getName(), mensaje);

        if (outOriginal != null) {
            outOriginal.println(linea);
        }
        escribirEnConsolaUI(linea);
        escribirEnArchivoDisco(linea);
    }

    public static void error(String categoria, String mensaje, Throwable ex) {
        if (!Version.isModoDebug()) {
            return;
        }
        String timestamp = LocalDateTime.now().format(FORMATO_HORA);
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("[%s] [ERROR] [%s] [Hilo:%s] %s",
                timestamp, categoria, Thread.currentThread().getName(), mensaje));

        if (ex != null) {
            StringWriter sw = new StringWriter();
            ex.printStackTrace(new PrintWriter(sw));
            sb.append("\n").append(sw.toString().trim());
        }

        String bloque = sb.toString();
        if (errOriginal != null) {
            errOriginal.println(bloque);
        }
        escribirEnConsolaUI(bloque);
        escribirEnArchivoDisco(bloque);
    }

    /**
     * Registra detalles de una TokenResponseException de Google sin exponer
     * credentials sensibles.
     */
    public static void logOAuthError(String categoria, Exception ex) {
        if (!Version.isModoDebug()) {
            return;
        }
        try {
            Class<?> tokenExClass = Class.forName("com.google.api.client.auth.oauth2.TokenResponseException");
            if (tokenExClass.isInstance(ex)) {
                Integer statusCode = (Integer) tokenExClass.getMethod("getStatusCode").invoke(ex);
                Object details = tokenExClass.getMethod("getDetails").invoke(ex);

                String mensaje = "OAUTH Error | HTTP " + statusCode;
                if (details != null) {
                    mensaje += " | Details: " + details.toString();
                }

                log(categoria, mensaje);
                return;
            }
        } catch (Exception ignored) {
        }

        // Si no es TokenResponseException, registrar como error normal
        error(categoria, "Error no identificado en OAuth", ex);
    }

    private static void escribirEnConsolaUI(String texto) {
        Runnable appendTask = () -> {
            if (areaConsola != null) {
                areaConsola.append(texto + "\n");
                if (chkAutoScroll != null && chkAutoScroll.isSelected()) {
                    areaConsola.setCaretPosition(areaConsola.getDocument().getLength());
                }
            }
        };
        if (SwingUtilities.isEventDispatchThread()) {
            appendTask.run();
        } else {
            SwingUtilities.invokeLater(appendTask);
        }
    }

    private static synchronized void escribirEnArchivoDisco(String texto) {
        if (!Version.isModoDebug()) {
            return;
        }
        File archivoLog = Configuracion.obtenerArchivoDatos(ARCHIVO_LOG);
        try (FileWriter fw = new FileWriter(archivoLog, StandardCharsets.UTF_8, true);
                PrintWriter pw = new PrintWriter(fw)) {
            pw.println(texto);
        } catch (IOException ignored) {
        }
    }

    public static void escribirLog(String nivel, String mensajeLog, Configuracion modelo, IncidenciaView vista) {
        if (!Version.isModoDebug()) {
            return;
        }
        String destino = (modelo != null && !modelo.getCorreoDestinoDebug().isEmpty())
                ? modelo.getCorreoDestinoDebug()
                : "Oficial (" + com.incidencias.model.ConfiguracionPublica.CORREO_OFICIAL_CENTRO + ")";

        String curso = vista != null ? vista.getCurso() : "";
        String taller = vista != null ? vista.getTaller() : "";
        String alumno = vista != null ? vista.getAlumno() : "";

        String detalle = String.format(
                "%s | Contexto -> Curso='%s', Taller='%s', Alumno='%s', Destino='%s'",
                mensajeLog, curso, taller, alumno, destino);

        log(nivel, detalle);
    }

    public static void escribirErrorLog(String nivel, String mensaje, Exception ex, Configuracion modelo,
            IncidenciaView vista) {
        if (!Version.isModoDebug()) {
            return;
        }
        escribirLog(nivel, mensaje, modelo, vista);
        error(nivel, "Stacktrace detallado de la excepción:", ex);
    }

    public static void escribirErrorLog(String nivel, Exception ex, Configuracion modelo, IncidenciaView vista) {
        escribirErrorLog(nivel, "Fallo al procesar o enviar el correo.", ex, modelo, vista);
    }
}
