package com.incidencias.model;

/**
 * Valores de configuración de la edición pública.
 *
 * Solo contiene datos no secretos.
 *
 * La cuenta oficial del centro es únicamente el DESTINATARIO de las incidencias.
 * Para enviar hay que iniciar sesión con una cuenta propia («Iniciar Sesión»).
 */
public final class ConfiguracionPublica {

    /** Cuenta oficial del centro: destinatario de las incidencias. */
    public static final String CORREO_OFICIAL_CENTRO = "informatica.ies.teis@edu.xunta.gal";

    private ConfiguracionPublica() {}

    public static String obtenerCorreoDestinatarioDefecto() {
        return CORREO_OFICIAL_CENTRO;
    }

    public static String obtenerGoogleClientId() {
        return obligatorio("GOOGLE_CLIENT_ID");
    }

    public static String obtenerGoogleClientSecret() {
        return obligatorio("GOOGLE_CLIENT_SECRET");
    }

    private static String obligatorio(String nombre) {
        String valor = System.getenv(nombre);
        valor = valor != null ? valor.trim() : "";
        if (valor.isBlank()) {
            throw new IllegalStateException("Falta la variable de entorno " + nombre + ". Consulta el README.");
        }
        return valor;
    }
}
