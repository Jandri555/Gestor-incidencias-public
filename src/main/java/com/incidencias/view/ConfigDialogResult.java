package com.incidencias.view;

import java.util.Arrays;

public class ConfigDialogResult {

    public enum Accion {
        GUARDAR_PASSWORD, // guardar usuario/contraseña + ajustes SMTP
        USAR_GOOGLE,      // el usuario pidió iniciar sesión con Google (el controlador dispara el flujo OAuth)
        BORRAR,
        CANCELAR
    }

    public Accion accion;

    public String correo;
    public char[] password;

    // Ajustes SMTP asociados al método de usuario/contraseña
    public String smtpHost;
    public String smtpPuerto;
    public boolean smtpSSL;

    public void limpiarPassword() {
        if (password != null) {
            Arrays.fill(password, '\0');
        }
    }
}
