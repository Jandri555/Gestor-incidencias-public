package com.incidencias.view;

/**
 * Resultado del diálogo de "Ajustes", accesible desde la ventana principal
 * sin necesidad de tocar credenciales: host/puerto/SSL del SMTP y el modo
 * debug (funciona igual sin importar si usas Google o usuario/contraseña).
 */

public class SmtpConfigResult {
    public boolean guardado;
    public String host;
    public String puerto;
    public boolean ssl;
    public String debugEmail;
}
