package com.incidencias.service;

import com.incidencias.Version;
import com.incidencias.utils.LoggerUtil;

import javax.swing.*;
import java.awt.*;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

public class UpdateService {

    private static final Path RUTA_ACTUALIZACIONES = Path.of("Z:\\incidencias");
    private static final String PREFIJO = "Incidencia_";

    public static void comprobarActualizaciones(Component parentComponent) {
        Thread.startVirtualThread(() -> {
            try {
                TipoDistribucion tipo = detectarDistribucion();
                LoggerUtil.log("UPDATE", "Formato de distribución detectado: " + tipo.descripcion);

                if (tipo == TipoDistribucion.DESCONOCIDA) {
                    return;
                }

                LoggerUtil.log("UPDATE", "Comprobando directorio de red: " + RUTA_ACTUALIZACIONES.toAbsolutePath()
                        + " (existe=" + Files.isDirectory(RUTA_ACTUALIZACIONES) + ")");
                if (!Files.isDirectory(RUTA_ACTUALIZACIONES)) {
                    return;
                }

                List<Path> archivos;
                try (Stream<Path> stream = Files.list(RUTA_ACTUALIZACIONES)) {
                    archivos = stream
                            .filter(path -> esArchivoDelTipo(path.getFileName().toString(), tipo))
                            .toList();
                }

                if (archivos.isEmpty()) {
                    LoggerUtil.log("UPDATE",
                            "No se encontraron paquetes del tipo " + tipo.descripcion + " en " + RUTA_ACTUALIZACIONES);
                    return;
                }

                String mejorVersion = Version.NUMERO;
                String mejorArchivo = null;

                for (Path archivo : archivos) {
                    String nombreArchivo = archivo.getFileName().toString();
                    String version = extraerVersion(nombreArchivo, tipo);
                    LoggerUtil.log("UPDATE",
                            "Analizando archivo '" + nombreArchivo + "' -> versión extraída: " + version);

                    if (version == null) {
                        continue;
                    }

                    if (esVersionMasNueva(version, mejorVersion)) {
                        mejorVersion = version;
                        mejorArchivo = archivo.getFileName().toString();
                    }
                }

                if (mejorArchivo != null) {
                    Path rutaFinal = RUTA_ACTUALIZACIONES.resolve(mejorArchivo);
                    String versionFinal = mejorVersion;
                    LoggerUtil.log("UPDATE", "Nueva actualización encontrada: v" + versionFinal + " en " + rutaFinal);

                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
                            parentComponent,
                            "Hay una nueva versión disponible: v"
                                    + versionFinal
                                    + "\n"
                                    + "Estás usando la v"
                                    + Version.NUMERO
                                    + ".\n\n"
                                    + "Formato: "
                                    + tipo.descripcion
                                    + "\n\n"
                                    + "Descárgala desde:\n"
                                    + rutaFinal,
                            "Actualización disponible",
                            JOptionPane.INFORMATION_MESSAGE));
                } else {
                    LoggerUtil.log("UPDATE", "La aplicación ya está en la última versión (v" + Version.NUMERO + ").");
                }

            } catch (Exception ex) {
                LoggerUtil.error("UPDATE", "Error comprobando actualizaciones", ex);
            }
        });
    }

    private static TipoDistribucion detectarDistribucion() {
        String appImage = System.getenv("APPIMAGE");
        if (appImage != null && !appImage.isBlank()) {
            return TipoDistribucion.APPIMAGE;
        }

        String jpackagePath = System.getProperty("jpackage.app-path");
        if (jpackagePath != null
                && !jpackagePath.isBlank()
                && jpackagePath.toLowerCase().endsWith(".exe")) {
            return TipoDistribucion.EXE;
        }

        try {
            URI ubicacion = UpdateService.class
                    .getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI();

            if ("file".equalsIgnoreCase(ubicacion.getScheme())) {
                Path ruta = Path.of(ubicacion);
                String nombre = ruta.getFileName() != null
                        ? ruta.getFileName().toString().toLowerCase(Locale.ROOT)
                        : "";
                if (Files.isRegularFile(ruta) && nombre.endsWith(".jar")) {
                    return TipoDistribucion.JAR;
                }
            }
        } catch (Exception ignored) {
        }

        String classPath = System.getProperty("java.class.path", "").toLowerCase(Locale.ROOT);
        if (classPath.contains(".jar")) {
            return TipoDistribucion.JAR;
        }

        return TipoDistribucion.DESCONOCIDA;
    }

    private static boolean esArchivoDelTipo(String nombre, TipoDistribucion tipo) {
        String n = nombre.toLowerCase(Locale.ROOT);
        String prefijo = PREFIJO.toLowerCase(Locale.ROOT);

        if (!n.startsWith(prefijo)) {
            return false;
        }

        return switch (tipo) {
            case JAR -> n.endsWith(".jar");
            case EXE -> n.endsWith(".exe");
            case APPIMAGE -> n.endsWith(".appimage");
            case DESCONOCIDA -> false;
        };
    }

    private static String extraerVersion(String nombre, TipoDistribucion tipo) {
        String extension = switch (tipo) {
            case JAR -> ".jar";
            case EXE -> ".exe";
            case APPIMAGE -> ".appimage";
            case DESCONOCIDA -> null;
        };
        if (extension == null) {
            return null;
        }

        String nombreMinusculas = nombre.toLowerCase(Locale.ROOT);
        if (!nombreMinusculas.startsWith(PREFIJO.toLowerCase(Locale.ROOT)) || !nombreMinusculas.endsWith(extension)) {
            return null;
        }

        String version = nombre.substring(PREFIJO.length(), nombre.length() - extension.length());

        // La edición OpenSource solo puede actualizarse desde paquetes
        // cuyo nombre termine explícitamente en "-OpenSource".
        if (!version.matches("\\d+(\\.\\d+)*-OpenSource")) {
            return null;
        }

        return version;
    }

    private static boolean esVersionMasNueva(String candidata, String actual) {
        try {
            String candidataBase = candidata.trim().split("-", 2)[0];
            String actualBase = actual.trim().split("-", 2)[0];

            String[] partesCandidata = candidataBase.split("\\.");
            String[] partesActual = actualBase.split("\\.");
            int longitud = Math.max(partesCandidata.length, partesActual.length);

            for (int i = 0; i < longitud; i++) {
                int c = i < partesCandidata.length ? Integer.parseInt(partesCandidata[i].trim()) : 0;
                int a = i < partesActual.length ? Integer.parseInt(partesActual[i].trim()) : 0;
                if (c != a) {
                    return c > a;
                }
            }
            return false;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private enum TipoDistribucion {
        JAR("JAR"),
        EXE("EXE"),
        APPIMAGE("AppImage"),
        DESCONOCIDA("desconocido");

        private final String descripcion;

        TipoDistribucion(String descripcion) {
            this.descripcion = descripcion;
        }
    }
}
