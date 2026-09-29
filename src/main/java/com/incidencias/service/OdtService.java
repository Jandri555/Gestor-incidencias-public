package com.incidencias.service;

import com.incidencias.utils.LoggerUtil;
import com.incidencias.view.IncidenciaView;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class OdtService {

    private static final String PLANTILLA_ODT = "Rexistro de incidencia.odt";

    public static boolean existeLibreOffice() {
        if (com.incidencias.utils.FailureSimulator.isSimularFaltaLibreOffice()) {
            LoggerUtil.log("SIMULADOR", "Simulando ausencia de LibreOffice (FailureSimulator activo).");
            return false;
        }

        List<String> candidatos = obtenerCandidatosLibreOffice();
        for (String ejecutable : candidatos) {
            if (ejecutable.contains(File.separator)) {
                boolean existe = new File(ejecutable).exists();
                LoggerUtil.log("LIBREOFFICE", "Comprobando ruta absoluta '" + ejecutable + "' -> existe=" + existe);
                if (existe) {
                    return true;
                }
            } else {
                LoggerUtil.log("LIBREOFFICE", "Asumiendo candidato en PATH del sistema: '" + ejecutable + "'");
                return true;
            }
        }
        return false;
    }

    private static List<String> obtenerCandidatosLibreOffice() {
        List<String> candidatos = new ArrayList<>();
        String os = System.getProperty("os.name", "").toLowerCase();

        if (os.contains("win")) {
            candidatos.add("C:\\Program Files\\LibreOffice\\program\\soffice.exe");
            candidatos.add("C:\\Program Files (x86)\\LibreOffice\\program\\soffice.exe");
            candidatos.add("soffice.exe");
        } else if (os.contains("mac")) {
            candidatos.add("/Applications/LibreOffice.app/Contents/MacOS/soffice");
            candidatos.add("soffice");
            candidatos.add("libreoffice");
        } else {
            candidatos.add("/usr/bin/libreoffice");
            candidatos.add("/usr/bin/soffice");
            candidatos.add("/snap/bin/libreoffice");
            candidatos.add("libreoffice");
            candidatos.add("soffice");
        }
        return candidatos;
    }

    public static void generarPDFDesdeODT(Path rutaOdtTemporal, Path rutaPdfSalida, IncidenciaView vista)
            throws IOException {
        generarNuevoODT(rutaOdtTemporal.toString(), vista);
        LoggerUtil.log("ODT", "Archivo ODT temporal generado: " + rutaOdtTemporal + " ("
                + Files.size(rutaOdtTemporal) + " bytes)");

        boolean convertido = convertirConLibreOffice(rutaOdtTemporal, rutaPdfSalida);
        if (!convertido || !Files.exists(rutaPdfSalida) || Files.size(rutaPdfSalida) == 0) {
            throw new IOException("No se ha podido generar el documento, se necesita LibreOffice instalado.");
        }
        LoggerUtil.log("PDF", "Archivo PDF generado con éxito: " + rutaPdfSalida + " ("
                + Files.size(rutaPdfSalida) + " bytes)");
    }

    public static void generarNuevoODT(String archivoSalida, IncidenciaView vista) throws IOException {
        String fechaActual = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        String horaActual = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));

        InputStream is = OdtService.class.getResourceAsStream("/" + PLANTILLA_ODT);
        if (is == null)
            throw new FileNotFoundException("No se encontró la plantilla ODT dentro del JAR.");

        try (ZipInputStream zis = new ZipInputStream(is);
                ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(archivoSalida))) {

            ZipEntry entrada;
            while ((entrada = zis.getNextEntry()) != null) {
                zos.putNextEntry(new ZipEntry(entrada.getName()));

                if (entrada.getName().equals("content.xml")) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(zis, StandardCharsets.UTF_8));
                    StringBuilder contenido = new StringBuilder();
                    String linea;
                    while ((linea = reader.readLine()) != null)
                        contenido.append(linea).append("\n");

                    String xmlModificado = contenido.toString()
                            .replace("Grupo/Curso:", "Grupo/Curso: " + escaparXml(vista.getCurso()))
                            .replace("Taller:", "Taller: " + escaparXml(vista.getTaller()))
                            .replace("Data:", "Data: " + fechaActual)
                            .replace("Hora:", "Hora: " + horaActual)
                            .replace("Equipo:", "Equipo: " + escaparXml(vista.getEquipo()))
                            .replace("Nome Alumna/Alumno:", "Nome Alumna/Alumno: " + escaparXml(vista.getAlumno()))
                            .replace("Nome Profesor/Profesora:",
                                    "Nome Profesor/Profesora: " + escaparXml(vista.getProfesor()))
                            .replace("Explicación do problema :",
                                    "Explicación do problema : " + escaparXml(vista.getProblema()));

                    zos.write(xmlModificado.getBytes(StandardCharsets.UTF_8));
                } else {
                    byte[] buffer = new byte[1024];
                    int leido;
                    while ((leido = zis.read(buffer)) > 0)
                        zos.write(buffer, 0, leido);
                }
                zos.closeEntry();
                zis.closeEntry();
            }
        }
    }

    private static boolean convertirConLibreOffice(Path rutaOdt, Path rutaPdfEsperada) {
        if (!existeLibreOffice()) {
            return false;
        }

        List<String> candidatos = obtenerCandidatosLibreOffice();
        Path dirSalida = rutaPdfEsperada.getParent();
        Path perfilUsuarioTmp = dirSalida.resolve("lo_profile");
        String urlPerfil = perfilUsuarioTmp.toUri().toString();

        for (String ejecutable : candidatos) {
            if (ejecutable.contains(File.separator) && !new File(ejecutable).exists()) {
                continue;
            }

            try {
                ProcessBuilder pb = new ProcessBuilder(
                        ejecutable,
                        "-env:UserInstallation=" + urlPerfil,
                        "--headless",
                        "--convert-to", "pdf",
                        "--outdir", dirSalida.toString(),
                        rutaOdt.toString());

                LoggerUtil.log("LIBREOFFICE", "Ejecutando comando: " + String.join(" ", pb.command()));
                pb.redirectErrorStream(true);
                long t0 = System.currentTimeMillis();
                Process proceso = pb.start();

                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(proceso.getInputStream(), StandardCharsets.UTF_8))) {
                    String linea;
                    while ((linea = br.readLine()) != null) {
                        LoggerUtil.log("LO-PROC", linea);
                    }
                }

                boolean terminado = proceso.waitFor(30, TimeUnit.SECONDS);
                long duracion = System.currentTimeMillis() - t0;
                LoggerUtil.log("LIBREOFFICE", "Proceso finalizado: terminado=" + terminado
                        + ", exitCode=" + (terminado ? proceso.exitValue() : "TIMEOUT") + ", tiempo=" + duracion
                        + "ms");

                if (terminado && proceso.exitValue() == 0 && Files.exists(rutaPdfEsperada)
                        && Files.size(rutaPdfEsperada) > 0) {
                    borrarDirectorioRecursivo(perfilUsuarioTmp.toFile());
                    return true;
                }
            } catch (Exception ex) {
                LoggerUtil.error("LIBREOFFICE", "Fallo al intentar ejecutar candidato: " + ejecutable, ex);
            }
        }

        borrarDirectorioRecursivo(perfilUsuarioTmp.toFile());
        return false;
    }

    private static String escaparXml(String texto) {
        if (texto == null)
            return "";
        return texto
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private static void borrarDirectorioRecursivo(File dir) {
        if (dir == null || !dir.exists())
            return;
        File[] archivos = dir.listFiles();
        if (archivos != null) {
            for (File f : archivos) {
                if (f.isDirectory())
                    borrarDirectorioRecursivo(f);
                else
                    f.delete();
            }
        }
        dir.delete();
    }
}
