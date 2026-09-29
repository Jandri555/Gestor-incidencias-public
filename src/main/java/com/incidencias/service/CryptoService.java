package com.incidencias.service;

import com.github.javakeyring.Keyring;

/**
 * Integración mínima con el almacén de credenciales del sistema (java-keyring).
 *
 * Solo permite guardar, recuperar y eliminar un secreto. Config.json conserva
 * únicamente la referencia {@link #MARCADOR_KEYRING}; el secreto real permanece en
 * el almacén del sistema operativo.
 */
public final class CryptoService {

    public static final String SERVICIO = "IncidenciasApp";
    public static final String CUENTA_SMTP = "SMTP_User";
    public static final String CUENTA_GOOGLE_REFRESH = "Google_RefreshToken";
    public static final String MARCADOR_KEYRING = "KEYRING:saved";

    private CryptoService() {}

    /** Guarda el secreto en el keyring y devuelve la referencia que se escribe en Config.json. */
    public static String guardar(String cuenta, char[] secreto) {
        if (secreto == null || secreto.length == 0) {
            return "";
        }
        try {
            Keyring.create().setPassword(SERVICIO, cuenta, new String(secreto));
            return MARCADOR_KEYRING;
        } catch (Exception ex) {
            throw new IllegalStateException("No se ha podido guardar la credencial en el almacén del sistema.", ex);
        }
    }

    /** Recupera el secreto del keyring a partir de la referencia de Config.json. */
    public static char[] recuperar(String cuenta, String referencia) {
        if (referencia == null || referencia.isBlank()) {
            return new char[0];
        }
        if (!MARCADOR_KEYRING.equals(referencia)) {
            throw new IllegalStateException("La referencia de credencial guardada no es compatible. Vuelve a introducirla.");
        }
        try {
            String secreto = Keyring.create().getPassword(SERVICIO, cuenta);
            return secreto != null ? secreto.toCharArray() : new char[0];
        } catch (Exception ex) {
            throw new IllegalStateException("No se ha podido recuperar la credencial del almacén del sistema.", ex);
        }
    }

    /** Elimina el secreto del keyring. Si no existe, no hace nada. */
    public static void eliminar(String cuenta) {
        try {
            Keyring.create().deletePassword(SERVICIO, cuenta);
        } catch (Exception ignored) {
            // No existía o el almacén no está disponible: no hay nada que borrar.
        }
    }

    /** Elimina todas las credenciales que guarda la aplicación. */
    public static void eliminarTodo() {
        eliminar(CUENTA_SMTP);
        eliminar(CUENTA_GOOGLE_REFRESH);
    }

    public static boolean esReferenciaKeyring(String referencia) {
        return MARCADOR_KEYRING.equals(referencia);
    }
}
