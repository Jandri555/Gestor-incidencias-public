package com.incidencias;

import com.incidencias.controller.IncidenciaController;
import com.incidencias.model.Configuracion;
import com.incidencias.service.SonidoService;
import com.incidencias.utils.LoggerUtil;
import com.incidencias.view.IncidenciaView;
import com.incidencias.view.TemaUI;
import com.incidencias.view.TemaUI.Tema;

import javax.swing.SwingUtilities;
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
            // 1. Cargar la configuración (guarda, entre otras cosas, el tema elegido)
            Configuracion modelo = new Configuracion();
            modelo.cargarDatos();

            // 2. Configurar FlatLaf con el tema guardado (Automático = el del sistema)
            Tema tema = Tema.desdeNombre(modelo.getTema());
            TemaUI.aplicar(tema);

            // 3. Iniciar consola de depuración si estamos en la compilación _deb
            if (Version.isModoDebug()) {
                LoggerUtil.iniciarConsolaDebug();
                LoggerUtil.log("UI", "Tema aplicado: " + tema.name()
                        + " (sistema en modo oscuro: " + TemaUI.sistemaEnModoOscuro() + ")");
            }
            LoggerUtil.log("INIT", "Configuracion cargada desde disco.");

            LoggerUtil.log("INIT", "Construyendo ventana principal IncidenciaView...");
            IncidenciaView vista = new IncidenciaView();
            vista.actualizarIdentidadTema(tema);

            LoggerUtil.log("INIT", "Inicializando IncidenciaController...");
            IncidenciaController controlador = new IncidenciaController(vista, modelo);
            controlador.iniciar();

            // Con el tema URSS y el sonido activado, suena al arrancar
            if (tema == Tema.URSS && modelo.isSonidoUrss()) {
                SonidoService.reproducir(TemaUI.RECURSO_SONIDO_URSS);
            }

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
}
