package com.incidencias.utils;

public final class FailureSimulator {
    private static boolean simularFaltaLibreOffice = false;
    private static boolean simularSinInternet = false;

    private FailureSimulator() {}

    public static boolean isSimularFaltaLibreOffice() { return simularFaltaLibreOffice; }
    public static void setSimularFaltaLibreOffice(boolean valor) { simularFaltaLibreOffice = valor; }
    public static boolean isSimularSinInternet() { return simularSinInternet; }
    public static void setSimularSinInternet(boolean valor) { simularSinInternet = valor; }
}
