package com.incidencias.service;

import com.incidencias.utils.LoggerUtil;
import javazoom.jl.player.Player;

import java.io.BufferedInputStream;
import java.io.InputStream;

/**
 * Reproduce un MP3 incluido en los recursos de la aplicación (sin bloquear la interfaz).
 *
 * Es «a prueba de fallos»: si el recurso no existe o no hay dispositivo de audio, se
 * registra en el log y la aplicación sigue funcionando con normalidad.
 */
public final class SonidoService {

    private static Player actual;

    private SonidoService() {
    }

    /** Reproduce el recurso una vez (p. ej. {@code "/URSS.mp3"}), cortando el que estuviera sonando. */
    public static synchronized void reproducir(String recurso) {
        detener();

        InputStream in = SonidoService.class.getResourceAsStream(recurso);
        if (in == null) {
            LoggerUtil.log("SONIDO", "No se encontró " + recurso + " en los recursos; no se reproduce nada.");
            return;
        }

        try {
            Player player = new Player(new BufferedInputStream(in));
            actual = player;

            Thread.startVirtualThread(() -> {
                try {
                    player.play();
                } catch (Exception e) {
                    LoggerUtil.error("SONIDO", "Error al reproducir " + recurso, e);
                } finally {
                    player.close();
                    synchronized (SonidoService.class) {
                        if (actual == player) {
                            actual = null;
                        }
                    }
                }
            });
        } catch (Exception e) {
            LoggerUtil.error("SONIDO", "No se pudo iniciar la reproducción de " + recurso, e);
        }
    }

    /** Corta el sonido en curso, si lo hay. */
    public static synchronized void detener() {
        if (actual != null) {
            actual.close();
            actual = null;
        }
    }
}
