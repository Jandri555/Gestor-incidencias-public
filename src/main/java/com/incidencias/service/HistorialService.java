package com.incidencias.service;

import com.incidencias.model.Configuracion;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class HistorialService {

    private static final String ARCHIVO_SQL = "historial_incidencias.sql";

    public static class RegistroIncidencia {
        public final String fecha;
        public final String hora;
        public final String curso;
        public final String taller;
        public final String equipo;
        public final String alumno;
        public final String profesor;
        public final String problema;
        public final String destinatario;

        public RegistroIncidencia(String fecha, String hora, String curso, String taller,
                String equipo, String alumno, String profesor,
                String problema, String destinatario) {
            this.fecha = fecha;
            this.hora = hora;
            this.curso = curso;
            this.taller = taller;
            this.equipo = equipo;
            this.alumno = alumno;
            this.profesor = profesor;
            this.problema = problema;
            this.destinatario = destinatario;
        }
    }

    /**
     * Guarda una incidencia añadiendo un INSERT INTO en el archivo .sql dentro de
     * AppData / ~/.config.
     */
    public static void guardarIncidencia(String curso, String taller, String equipo,
            String alumno, String profesor, String problema,
            String destinatario) throws IOException {
        File archivo = Configuracion.obtenerArchivoDatos(ARCHIVO_SQL);
        boolean nuevoArchivo = !archivo.exists() || archivo.length() == 0;

        String fecha = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        String hora = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));

        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(archivo, true), StandardCharsets.UTF_8))) {

            if (nuevoArchivo) {
                writer.write("-- Historial de Incidencias\n");
                writer.write("CREATE TABLE IF NOT EXISTS incidencias (\n");
                writer.write("    id INT AUTO_INCREMENT PRIMARY KEY,\n");
                writer.write("    fecha VARCHAR(20),\n");
                writer.write("    hora VARCHAR(10),\n");
                writer.write("    curso VARCHAR(100),\n");
                writer.write("    taller VARCHAR(100),\n");
                writer.write("    equipo VARCHAR(100),\n");
                writer.write("    alumno VARCHAR(150),\n");
                writer.write("    profesor VARCHAR(150),\n");
                writer.write("    problema TEXT,\n");
                writer.write("    destinatario VARCHAR(150)\n");
                writer.write(");\n\n");
            }

            String sqlInsert = String.format(
                    "INSERT INTO incidencias (fecha, hora, curso, taller, equipo, alumno, profesor, problema, destinatario) "
                            +
                            "VALUES ('%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s');%n",
                    escaparSQL(fecha),
                    escaparSQL(hora),
                    escaparSQL(curso),
                    escaparSQL(taller),
                    escaparSQL(equipo),
                    escaparSQL(alumno),
                    escaparSQL(profesor),
                    escaparSQL(problema),
                    escaparSQL(destinatario));

            writer.write(sqlInsert);
        }
    }

    /**
     * Lee el archivo .sql de la carpeta de datos del usuario y extrae todos los
     * registros.
     */
    public static List<RegistroIncidencia> leerHistorial() throws IOException {
        List<RegistroIncidencia> lista = new ArrayList<>();
        File archivo = Configuracion.obtenerArchivoDatos(ARCHIVO_SQL);

        if (!archivo.exists()) {
            return lista;
        }

        List<String> lineas = Files.readAllLines(archivo.toPath(), StandardCharsets.UTF_8);
        for (String linea : lineas) {
            String limpia = linea.trim();
            if (limpia.toUpperCase().startsWith("INSERT INTO INCIDENCIAS")) {
                int idxValues = limpia.toUpperCase().indexOf("VALUES");
                if (idxValues != -1) {
                    int inicioParentesis = limpia.indexOf('(', idxValues);
                    int finParentesis = limpia.lastIndexOf(')');
                    if (inicioParentesis != -1 && finParentesis > inicioParentesis) {
                        String contenidoValores = limpia.substring(inicioParentesis + 1, finParentesis);
                        List<String> campos = parsearValoresSQL(contenidoValores);
                        if (campos.size() >= 9) {
                            lista.add(new RegistroIncidencia(
                                    campos.get(0),
                                    campos.get(1),
                                    campos.get(2),
                                    campos.get(3),
                                    campos.get(4),
                                    campos.get(5),
                                    campos.get(6),
                                    campos.get(7),
                                    campos.get(8)));
                        }
                    }
                }
            }
        }
        return lista;
    }

    private static String escaparSQL(String valor) {
        if (valor == null)
            return "";
        return valor
                .replace("\\", "\\\\")
                .replace("'", "''")
                .replace("\r", "")
                .replace("\n", "\\n");
    }

    private static List<String> parsearValoresSQL(String cadena) {
        List<String> valores = new ArrayList<>();
        StringBuilder actual = new StringBuilder();
        boolean enComillas = false;

        for (int i = 0; i < cadena.length(); i++) {
            char c = cadena.charAt(i);

            if (!enComillas) {
                if (c == '\'') {
                    enComillas = true;
                    actual.setLength(0);
                }
            } else {
                if (c == '\'') {
                    if (i + 1 < cadena.length() && cadena.charAt(i + 1) == '\'') {
                        actual.append('\'');
                        i++;
                    } else {
                        enComillas = false;
                        valores.add(actual.toString());
                    }
                } else if (c == '\\' && i + 1 < cadena.length()) {
                    char sig = cadena.charAt(i + 1);
                    if (sig == 'n') {
                        actual.append('\n');
                        i++;
                    } else if (sig == '\\') {
                        actual.append('\\');
                        i++;
                    } else {
                        actual.append(c);
                    }
                } else {
                    actual.append(c);
                }
            }
        }
        return valores;
    }
}
