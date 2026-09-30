package com.incidencias.view;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.WinReg;

import javax.swing.UIManager;
import java.awt.Font;
import java.util.HashMap;
import java.util.Map;

/**
 * Gestión de los temas de la interfaz.
 *
 * <ul>
 *   <li><b>AUTO</b>: sigue el modo claro/oscuro del sistema operativo.</li>
 *   <li><b>CLARO</b> y <b>OSCURO</b>: fijos, sin mirar el sistema.</li>
 *   <li><b>URSS</b>: fondo rojo bandera con letras y acentos dorados, otro título de
 *       ventana, otro logo y (opcional) sonido.</li>
 * </ul>
 *
 * Los colores personalizados se pasan con {@link FlatLaf#setGlobalExtraDefaults(Map)}
 * (que se reemplaza en cada cambio) en lugar de con {@code UIManager.put(...)}: así, al
 * volver de un tema personalizado a otro, no se queda ningún color del anterior.
 */
public final class TemaUI {

    private TemaUI() {
    }

    /** Título de la ventana con el tema URSS (sustituye a {@code Version.NOMBRE}). */
    public static final String NOMBRE_URSS = "Nuestro gestor de incidencias";

    /** Recursos del tema URSS, en src/main/resources. Si no existen, se ignoran sin error. */
    public static final String RECURSO_ICONO_URSS = "/URSS.png";
    public static final String RECURSO_SONIDO_URSS = "/URSS.mp3";

    public enum Tema {
        AUTO("Automático (según el sistema)"),
        CLARO("Claro"),
        OSCURO("Oscuro"),
        URSS("URSS");

        private final String etiqueta;

        Tema(String etiqueta) {
            this.etiqueta = etiqueta;
        }

        /** Texto que se muestra en el selector de temas. */
        @Override
        public String toString() {
            return this == URSS ? etiqueta + simboloUrss() : etiqueta;
        }

        /** Convierte el nombre guardado en Config.json en un tema; si es desconocido, AUTO. */
        public static Tema desdeNombre(String nombre) {
            if (nombre != null) {
                for (Tema t : values()) {
                    if (t.name().equalsIgnoreCase(nombre.trim())) {
                        return t;
                    }
                }
            }
            return AUTO;
        }
    }

    /**
     * Aplica el tema (solo cambia el Look and Feel). Úsalo antes de crear ventanas;
     * si ya hay ventanas abiertas, usa {@link #aplicarYRefrescar(Tema)}.
     */
    public static void aplicar(Tema tema) {
        Tema efectivo = (tema == Tema.AUTO)
                ? (sistemaEnModoOscuro() ? Tema.OSCURO : Tema.CLARO)
                : tema;

        switch (efectivo) {
            case OSCURO -> {
                FlatLaf.setGlobalExtraDefaults(coloresOscuro());
                FlatDarkLaf.setup();
            }
            case URSS -> {
                FlatLaf.setGlobalExtraDefaults(coloresUrss());
                FlatDarkLaf.setup();
            }
            default -> {
                FlatLaf.setGlobalExtraDefaults(null);
                FlatLightLaf.setup();
            }
        }
    }

    /** Aplica el tema y repinta todas las ventanas que ya estén abiertas. */
    public static void aplicarYRefrescar(Tema tema) {
        aplicar(tema);
        FlatLaf.updateUI();
    }

    /**
     * Detecta de forma nativa si el sistema operativo está en Modo Oscuro.
     */
    public static boolean sistemaEnModoOscuro() {
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

    /**
     * " ☭" si la fuente de la interfaz sabe dibujarlo; cadena vacía si no
     * (así nunca sale un cuadradito en su lugar).
     */
    static String simboloUrss() {
        Font fuente = UIManager.getFont("Label.font");
        return (fuente != null && fuente.canDisplay('\u262D')) ? " \u262D" : "";
    }

    // ------------------------------------------------------------------
    // Paletas
    // ------------------------------------------------------------------

    private static void poner(Map<String, String> m, String color, String... claves) {
        for (String clave : claves) {
            m.put(clave, color);
        }
    }

    /** Los mismos retoques que ya llevaba el modo oscuro de la aplicación. */
    private static Map<String, String> coloresOscuro() {
        Map<String, String> m = new HashMap<>();
        String fondo = "#181818";
        String cajas = "#242424";
        String azul = "#0078d7";
        String azulHover = "#198ceb";

        poner(m, fondo, "Panel.background", "OptionPane.background", "RootPane.background");
        poner(m, "#ffffff", "Label.foreground", "OptionPane.messageForeground");

        poner(m, cajas, "TextField.background", "TextArea.background");
        poner(m, "#ffffff", "TextField.foreground", "TextArea.foreground");

        m.put("Button.background", "#2d2d2d");
        m.put("Button.foreground", "#ffffff");
        m.put("Button.focusedBackground", "#3c3c3c");

        m.put("Component.borderColor", "#3c3c3c");
        m.put("Component.focusedBorderColor", "#787878");

        m.put("CheckBox.icon.background", cajas);
        m.put("CheckBox.icon.borderColor", "#6e6e6e");
        poner(m, azul, "CheckBox.icon.selectedBackground", "CheckBox.icon.selectedBorderColor",
                "CheckBox.icon.focusedSelectedBackground");
        m.put("CheckBox.icon.focusedSelectedBorderColor", "#78beff");
        poner(m, azulHover, "CheckBox.icon.hoverSelectedBackground", "CheckBox.icon.hoverSelectedBorderColor");
        m.put("CheckBox.icon.pressedSelectedBackground", "#005faf");
        m.put("CheckBox.icon.checkmarkColor", "#ffffff");
        return m;
    }

    /** Rojo bandera y oro. */
    private static Map<String, String> coloresUrss() {
        Map<String, String> m = new HashMap<>();
        String rojo = "#7b0f0f";
        String rojoOscuro = "#560808";
        String rojoBoton = "#a01818";
        String rojoBotonHover = "#bb2222";
        String oro = "#ffd700";
        String oroClaro = "#ffe34d";
        String oroSuave = "#e8c15a";
        String oroBorde = "#b8860b";

        // Variables base de FlatLaf: de aquí derivan muchos colores que no se listan abajo
        m.put("@background", rojo);
        m.put("@foreground", oro);
        m.put("@accentColor", oro);
        m.put("@selectionBackground", oro);
        m.put("@selectionForeground", rojoOscuro);

        // Fondos
        poner(m, rojo, "Panel.background", "OptionPane.background", "RootPane.background",
                "Viewport.background", "ScrollPane.background", "CheckBox.background", "SplitPane.background");

        // Textos
        poner(m, oro, "Label.foreground", "OptionPane.messageForeground", "CheckBox.foreground");
        m.put("Label.disabledForeground", oroSuave);

        // Cajas de texto (incluye las de solo lectura del historial)
        poner(m, rojoOscuro, "TextField.background", "PasswordField.background", "TextArea.background",
                "FormattedTextField.background", "ComboBox.background",
                "TextField.inactiveBackground", "TextArea.inactiveBackground");
        poner(m, oro, "TextField.foreground", "PasswordField.foreground", "TextArea.foreground",
                "FormattedTextField.foreground", "ComboBox.foreground",
                "TextField.inactiveForeground", "TextArea.inactiveForeground",
                "TextField.caretForeground", "PasswordField.caretForeground", "TextArea.caretForeground");
        poner(m, oro, "TextField.selectionBackground", "PasswordField.selectionBackground",
                "TextArea.selectionBackground");
        poner(m, rojoOscuro, "TextField.selectionForeground", "PasswordField.selectionForeground",
                "TextArea.selectionForeground");

        // Botones (el principal, «Enviar Incidencia», va en oro)
        m.put("Button.background", rojoBoton);
        m.put("Button.foreground", oro);
        poner(m, rojoBotonHover, "Button.focusedBackground", "Button.hoverBackground");
        m.put("Button.pressedBackground", "#8a1212");
        m.put("Button.borderColor", oroBorde);
        m.put("Button.default.background", oro);
        m.put("Button.default.foreground", rojoOscuro);
        m.put("Button.default.hoverBackground", oroClaro);
        m.put("Button.default.pressedBackground", "#e6bf00");
        m.put("Button.default.borderColor", oro);

        // Bordes y foco
        poner(m, oroBorde, "Component.borderColor", "Separator.foreground");
        m.put("Component.focusedBorderColor", oro);

        // Casillas de verificación
        m.put("CheckBox.icon.background", rojoOscuro);
        m.put("CheckBox.icon.borderColor", oroSuave);
        poner(m, oro, "CheckBox.icon.selectedBackground", "CheckBox.icon.selectedBorderColor",
                "CheckBox.icon.focusedSelectedBackground");
        m.put("CheckBox.icon.focusedSelectedBorderColor", "#fff2a8");
        poner(m, oroClaro, "CheckBox.icon.hoverSelectedBackground", "CheckBox.icon.hoverSelectedBorderColor");
        m.put("CheckBox.icon.pressedSelectedBackground", "#e6bf00");
        m.put("CheckBox.icon.checkmarkColor", rojoOscuro);

        // Tabla del historial
        m.put("Table.background", rojoOscuro);
        m.put("Table.foreground", oro);
        m.put("Table.selectionBackground", oro);
        m.put("Table.selectionForeground", rojoOscuro);
        m.put("Table.selectionInactiveBackground", "#c9a227");
        m.put("Table.selectionInactiveForeground", rojoOscuro);
        m.put("Table.gridColor", oroBorde);
        m.put("TableHeader.background", rojoBoton);
        m.put("TableHeader.foreground", oro);
        poner(m, oroBorde, "TableHeader.separatorColor", "TableHeader.bottomSeparatorColor");

        // Barras de desplazamiento, progreso, divisores y tooltips
        m.put("ScrollBar.thumb", oroBorde);
        m.put("ScrollBar.hoverThumbColor", oroSuave);
        m.put("ScrollBar.pressedThumbColor", oro);
        m.put("ProgressBar.foreground", oro);
        m.put("ProgressBar.background", rojoOscuro);
        m.put("SplitPaneDivider.gripColor", oroBorde);
        m.put("ToolTip.background", rojoOscuro);
        m.put("ToolTip.foreground", oro);

        // Barra de título (decoración de ventana propia de FlatLaf)
        poner(m, rojoOscuro, "TitlePane.background", "TitlePane.inactiveBackground");
        m.put("TitlePane.foreground", oro);
        m.put("TitlePane.inactiveForeground", oroSuave);
        m.put("TitlePane.buttonHoverBackground", rojoBoton);
        return m;
    }
}
