package com.incidencias.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import com.incidencias.Version;
import com.incidencias.service.CryptoService;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class Configuracion {

    private static final String NOMBRE_CARPETA_APP = "Incidencias_OS";
    private static final String ARCHIVO_CONFIG_JSON = "Config.json";
    private static final String ARCHIVO_CONFIG_TXT = "datos_incidencia.txt";

    public static File obtenerDirectorioDatos() {
        String os = System.getProperty("os.name", "").toLowerCase();
        String userHome = System.getProperty("user.home", ".");
        File baseDir;

        if (os.contains("win")) {
            String appData = System.getenv("APPDATA");
            if (appData != null && !appData.isBlank()) {
                baseDir = new File(appData);
            } else {
                baseDir = new File(userHome, "AppData" + File.separator + "Roaming");
            }
        } else if (os.contains("mac")) {
            baseDir = new File(userHome, "Library" + File.separator + "Application Support");
        } else {
            String xdgConfig = System.getenv("XDG_CONFIG_HOME");
            if (xdgConfig != null && !xdgConfig.isBlank()) {
                baseDir = new File(xdgConfig);
            } else {
                baseDir = new File(userHome, ".config");
            }
        }

        File appDir = new File(baseDir, NOMBRE_CARPETA_APP);
        if (!appDir.exists()) {
            appDir.mkdirs();
        }
        return appDir;
    }

    public static File obtenerArchivoDatos(String nombreArchivo) {
        return new File(obtenerDirectorioDatos(), nombreArchivo);
    }

    @SerializedName("version")
    private String version = Version.NUMERO;

    @SerializedName("firstrun")
    private boolean firstRun = true;

    @SerializedName("curso")
    private String curso = "";

    @SerializedName("taller")
    private String taller = "";

    @SerializedName("equipo")
    private String equipo = "";

    @SerializedName("alumno")
    private String alumno = "";

    @SerializedName("email")
    private String correoUsuario = "";

    // Referencia al almacén de credenciales del sistema ("KEYRING:saved"); nunca la contraseña real.
    @SerializedName("password_os")
    private String passwordReferencia = "";

    // Solo se lee o guarda en Config.json si se ejecuta la compilación _deb
    @SerializedName("debug_email")
    private String correoDestinoDebug = null;

    @SerializedName("auth_method")
    private String metodoAutenticacion = "PASSWORD";

    @SerializedName("smtp_host")
    private String smtpHost = "smtp.gmail.com";

    @SerializedName("smtp_port")
    private String smtpPuerto = "465";

    @SerializedName("smtp_ssl")
    private boolean smtpSSL = true;

    // Tema de la interfaz: AUTO, CLARO, OSCURO o URSS (ver TemaUI.Tema)
    @SerializedName("tema")
    private String tema = "AUTO";

    // Si el tema URSS reproduce URSS.mp3 (activado por defecto)
    @SerializedName("sonido_urss")
    private boolean sonidoUrss = true;

    // Referencia al almacén de credenciales del sistema ("KEYRING:saved"); nunca el token real.
    @SerializedName("google_refresh_token")
    private String googleRefreshTokenReferencia = "";

    public void cargarDatos() {
        File jsonFile = obtenerArchivoDatos(ARCHIVO_CONFIG_JSON);
        File txtFile = obtenerArchivoDatos(ARCHIVO_CONFIG_TXT);

        if (jsonFile.exists()) {
            try (Reader reader = new InputStreamReader(new FileInputStream(jsonFile), StandardCharsets.UTF_8)) {
                Gson gson = new Gson();
                Configuracion cargada = gson.fromJson(reader, Configuracion.class);
                if (cargada != null) {
                    copiarDatos(cargada);
                }
            } catch (Exception e) {
                System.err.println("Error al cargar JSON: " + e.getMessage());
            }
        } else if (txtFile.exists()) {
            System.out.println("Archivo TXT detectado. Iniciando migración a JSON...");
            try (FileReader reader = new FileReader(txtFile, StandardCharsets.UTF_8)) {
                Properties props = new Properties();
                props.load(reader);
                this.curso = props.getProperty("curso", "");
                this.taller = props.getProperty("taller", "");
                this.equipo = props.getProperty("equipo", "");
                this.alumno = props.getProperty("alumno", "");
                this.correoUsuario = props.getProperty("email", "");
                String refTxt = props.getProperty("password_os", "");
                this.passwordReferencia = CryptoService.MARCADOR_KEYRING.equals(refTxt) ? refTxt : "";
                this.version = "1.3";
            } catch (IOException e) {
                System.err.println("Error al leer el TXT antiguo.");
            }

            guardarDatos();

            if (txtFile.delete()) {
                System.out.println("Archivo TXT migrado y eliminado correctamente.");
            }
        }
    }

    public void guardarDatos() {
        this.version = Version.NUMERO;

        if (!Version.isModoDebug() || (this.correoDestinoDebug != null && this.correoDestinoDebug.isBlank())) {
            this.correoDestinoDebug = null;
        }

        File jsonFile = obtenerArchivoDatos(ARCHIVO_CONFIG_JSON);
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(jsonFile), StandardCharsets.UTF_8)) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(this, writer);
        } catch (Exception e) {
            System.err.println("Error al guardar la configuración JSON.");
        }
    }

    private void copiarDatos(Configuracion origen) {
        this.version = origen.version;
        this.firstRun = origen.firstRun;
        this.curso = origen.curso;
        this.taller = origen.taller;
        this.equipo = origen.equipo;
        this.alumno = origen.alumno;
        this.correoUsuario = origen.correoUsuario;
        this.passwordReferencia = origen.passwordReferencia;
        this.correoDestinoDebug = Version.isModoDebug() ? origen.correoDestinoDebug : null;
        this.metodoAutenticacion = (origen.metodoAutenticacion != null) ? origen.metodoAutenticacion : "PASSWORD";
        this.smtpHost = (origen.smtpHost != null && !origen.smtpHost.isEmpty()) ? origen.smtpHost : "smtp.gmail.com";
        this.smtpPuerto = (origen.smtpPuerto != null && !origen.smtpPuerto.isEmpty()) ? origen.smtpPuerto : "465";
        this.smtpSSL = origen.smtpSSL;
        this.tema = (origen.tema != null && !origen.tema.isBlank()) ? origen.tema : "AUTO";
        this.sonidoUrss = origen.sonidoUrss;
        this.googleRefreshTokenReferencia = (origen.googleRefreshTokenReferencia != null) ? origen.googleRefreshTokenReferencia
                : "";
    }

    public boolean isFirstRun() {
        return firstRun;
    }

    public void setFirstRun(boolean firstRun) {
        this.firstRun = firstRun;
    }

    public void borrarCredenciales() {
        this.correoUsuario = "";
        this.passwordReferencia = "";
        this.metodoAutenticacion = "PASSWORD";
        this.googleRefreshTokenReferencia = "";
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getCurso() {
        return curso;
    }

    public void setCurso(String curso) {
        this.curso = curso;
    }

    public String getTaller() {
        return taller;
    }

    public void setTaller(String taller) {
        this.taller = taller;
    }

    public String getEquipo() {
        return equipo;
    }

    public void setEquipo(String equipo) {
        this.equipo = equipo;
    }

    public String getAlumno() {
        return alumno;
    }

    public void setAlumno(String alumno) {
        this.alumno = alumno;
    }

    public String getCorreoUsuario() {
        return correoUsuario;
    }

    public void setCorreoUsuario(String correoUsuario) {
        this.correoUsuario = correoUsuario;
    }

    public String getPasswordReferencia() {
        return passwordReferencia;
    }

    public void setPasswordReferencia(String passwordReferencia) {
        this.passwordReferencia = passwordReferencia;
    }

    public boolean isModoDebugActivado() {
        return Version.isModoDebug();
    }

    public String getCorreoDestinoDebug() {
        return (Version.isModoDebug() && correoDestinoDebug != null) ? correoDestinoDebug : "";
    }

    public void setCorreoDestinoDebug(String correoDestinoDebug) {
        if (Version.isModoDebug() && correoDestinoDebug != null && !correoDestinoDebug.isBlank()) {
            this.correoDestinoDebug = correoDestinoDebug.trim();
        } else {
            this.correoDestinoDebug = null;
        }
    }

    public String getMetodoAutenticacion() {
        return metodoAutenticacion;
    }

    public void setMetodoAutenticacion(String metodoAutenticacion) {
        this.metodoAutenticacion = metodoAutenticacion;
    }

    public String getSmtpHost() {
        return smtpHost;
    }

    public void setSmtpHost(String smtpHost) {
        this.smtpHost = smtpHost;
    }

    public String getSmtpPuerto() {
        return smtpPuerto;
    }

    public void setSmtpPuerto(String smtpPuerto) {
        this.smtpPuerto = smtpPuerto;
    }

    public String getTema() {
        return tema;
    }

    public void setTema(String tema) {
        this.tema = (tema != null && !tema.isBlank()) ? tema : "AUTO";
    }

    public boolean isSonidoUrss() {
        return sonidoUrss;
    }

    public void setSonidoUrss(boolean sonidoUrss) {
        this.sonidoUrss = sonidoUrss;
    }

    public boolean isSmtpSSL() {
        return smtpSSL;
    }

    public void setSmtpSSL(boolean smtpSSL) {
        this.smtpSSL = smtpSSL;
    }

    public String getGoogleRefreshTokenReferencia() {
        return googleRefreshTokenReferencia;
    }

    public void setGoogleRefreshTokenReferencia(String googleRefreshTokenReferencia) {
        this.googleRefreshTokenReferencia = googleRefreshTokenReferencia;
    }
}
