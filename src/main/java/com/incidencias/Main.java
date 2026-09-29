package com.incidencias;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.incidencias.controller.IncidenciaController;
import com.incidencias.model.Configuracion;
import com.incidencias.utils.LoggerUtil;
import com.incidencias.view.IncidenciaView;
import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.WinReg;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.Color;
import java.lang.management.ManagementFactory;
import java.util.List;

public class Main {

    // Bloque Anti-Hooking / Anti-Debugger
    static {
        List<String> jvmArgs = ManagementFactory.getRuntimeMXBean().getInputArguments();
        for (String arg : jvmArgs) {
            if (arg.contains("-javaagent") ||
                    arg.contains("-Xdebug") ||
                    arg.contains("-Xrunjdwp") ||
                    arg.contains("-agentlib:jdwp")) {
                System.exit(0);
            }
        }
    }

    public static void main(String[] args) {
        // Decoración de ventanas moderna
        System.setProperty("flatlaf.useWindowDecorations", "true");
        System.setProperty("flatlaf.menuBarEmbedded", "true");

        SwingUtilities.invokeLater(() -> {
            // 1. Configurar FlatLaf leyendo el sistema operativo
            boolean modoOscuro = isModoOscuroActivo();
            if (modoOscuro) {
                FlatDarkLaf.setup();

                Color fondoOscuro = new Color(24, 24, 24);
                Color cajasTexto = new Color(36, 36, 36);
                Color borde = new Color(60, 60, 60);
                Color azulCasilla = new Color(0, 120, 215);
                Color azulCasillaHover = new Color(25, 140, 235);

                UIManager.put("Panel.background", fondoOscuro);
                UIManager.put("OptionPane.background", fondoOscuro);
                UIManager.put("RootPane.background", fondoOscuro);
                UIManager.put("Label.foreground", Color.WHITE);
                UIManager.put("OptionPane.messageForeground", Color.WHITE);

                UIManager.put("TextField.background", cajasTexto);
                UIManager.put("TextField.foreground", Color.WHITE);
                UIManager.put("TextArea.background", cajasTexto);
                UIManager.put("TextArea.foreground", Color.WHITE);

                UIManager.put("Button.background", new Color(45, 45, 45));
                UIManager.put("Button.foreground", Color.WHITE);
                UIManager.put("Button.focusedBackground", new Color(60, 60, 60));

                UIManager.put("Component.borderColor", borde);
                UIManager.put("Component.focusedBorderColor", new Color(120, 120, 120));

                UIManager.put("CheckBox.icon.background", cajasTexto);
                UIManager.put("CheckBox.icon.borderColor", new Color(110, 110, 110));
                UIManager.put("CheckBox.icon.selectedBackground", azulCasilla);
                UIManager.put("CheckBox.icon.selectedBorderColor", azulCasilla);
                UIManager.put("CheckBox.icon.focusedSelectedBackground", azulCasilla);
                UIManager.put("CheckBox.icon.focusedSelectedBorderColor", new Color(120, 190, 255));
                UIManager.put("CheckBox.icon.hoverSelectedBackground", azulCasillaHover);
                UIManager.put("CheckBox.icon.hoverSelectedBorderColor", azulCasillaHover);
                UIManager.put("CheckBox.icon.pressedSelectedBackground", new Color(0, 95, 175));
                UIManager.put("CheckBox.icon.checkmarkColor", Color.WHITE);
            } else {
                FlatLightLaf.setup();
            }

            // 2. Iniciar consola de depuración si estamos en la compilación _deb
            if (Version.isModoDebug()) {
                LoggerUtil.iniciarConsolaDebug();
                LoggerUtil.log("UI", "Tema del sistema detectado: "
                        + (modoOscuro ? "OSCURO (FlatDarkLaf)" : "CLARO (FlatLightLaf)"));
            }

            // 3. Inicializar Arquitectura
            LoggerUtil.log("INIT", "Instanciando modelo Configuracion y cargando datos desde disco...");
            Configuracion modelo = new Configuracion();
            modelo.cargarDatos();

            boolean mostrarAvisoPrimeraEjecucion = modelo.isFirstRun();
            if (mostrarAvisoPrimeraEjecucion) {
                JOptionPane.showMessageDialog(null,
                        "Atención: Esta aplicación no es del centro, si el programa falla de alguna manera no mandes una incidencia sobre ello.\n\n"
                                + "En su lugar avísame a mí (Alejandro Silva) =).",
                        "Aviso importante",
                        JOptionPane.WARNING_MESSAGE);
                modelo.setFirstRun(false);
                modelo.guardarDatos();
            }

            LoggerUtil.log("INIT", "Construyendo ventana principal IncidenciaView...");
            IncidenciaView vista = new IncidenciaView();

            LoggerUtil.log("INIT", "Inicializando IncidenciaController...");
            IncidenciaController controlador = new IncidenciaController(vista, modelo);
            controlador.iniciar();

            // 4. Asegurar que la consola quede detrás y la ventana principal delante con el
            // foco
            SwingUtilities.invokeLater(() -> {
                LoggerUtil.enviarConsolaAlFondo();
                vista.toFront();
                vista.requestFocus();
                vista.enfocarCajaProblema();
            });
        });
    }

    /**
     * Detecta de forma nativa si el sistema operativo está en Modo Oscuro.
     */
    private static boolean isModoOscuroActivo() {
        String os = System.getProperty("os.name").toLowerCase();
        try {
            if (os.contains("win")) {
                int isLightTheme = Advapi32Util.registryGetIntValue(
                        WinReg.HKEY_CURRENT_USER,
                        "Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize",
                        "AppsUseLightTheme");
                return isLightTheme == 0;
            } else if (os.contains("mac")) {
                Process process = Runtime.getRuntime()
                        .exec(new String[] { "defaults", "read", "-g", "AppleInterfaceStyle" });
                process.waitFor();
                return process.exitValue() == 0;
            }
        } catch (Exception e) {
            // Ignoramos errores de lectura y caemos en modo claro
        }
        return false;
    }
}
