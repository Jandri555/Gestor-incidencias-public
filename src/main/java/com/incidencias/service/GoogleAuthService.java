package com.incidencias.service;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleRefreshTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.incidencias.model.Configuracion;
import com.incidencias.model.ConfiguracionPublica;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public class GoogleAuthService {

    private static String obtenerClientId() {
        return ConfiguracionPublica.obtenerGoogleClientId();
    }

    private static String obtenerClientSecret() {
        return ConfiguracionPublica.obtenerGoogleClientSecret();
    }

    private static final List<String> SCOPES = Arrays.asList(
            "https://mail.google.com/", "https://www.googleapis.com/auth/userinfo.email", "openid");
    private static final String CARPETA_TOKENS_TEMPORAL = "tokens_google_tmp";

    public static class ResultadoAuth {
        public String correo;
        public String refreshToken;
    }

    public static ResultadoAuth iniciarFlujoOAuth() throws Exception {
        NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        GsonFactory jsonFactory = GsonFactory.getDefaultInstance();
        GoogleClientSecrets.Details detalles = new GoogleClientSecrets.Details();
        detalles.setClientId(obtenerClientId());
        detalles.setClientSecret(obtenerClientSecret());
        detalles.setAuthUri("https://accounts.google.com/o/oauth2/auth");
        detalles.setTokenUri("https://oauth2.googleapis.com/token");
        GoogleClientSecrets secretos = new GoogleClientSecrets().setInstalled(detalles);
        File carpetaTokensTmp = Configuracion.obtenerArchivoDatos(CARPETA_TOKENS_TEMPORAL);
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                httpTransport, jsonFactory, secretos, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(carpetaTokensTmp))
                .setAccessType("offline").build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(0).build();
        try {
            Credential credential = new AuthorizationCodeInstalledApp(flow, receiver) {
                @Override
                protected void onAuthorization(com.google.api.client.auth.oauth2.AuthorizationCodeRequestUrl url)
                        throws IOException {
                    url.set("prompt", "consent");
                    super.onAuthorization(url);
                }
            }.authorize("usuario-local");
            if (credential.getRefreshToken() == null)
                throw new IllegalStateException("Google no devolvió un refresh token. Revoca el acceso de la app en https://myaccount.google.com/permissions e inténtalo de nuevo.");
            ResultadoAuth resultado = new ResultadoAuth();
            resultado.correo = obtenerCorreoDesdeAccessToken(credential.getAccessToken());
            resultado.refreshToken = credential.getRefreshToken();
            return resultado;
        } finally {
            borrarCarpetaTemporal(carpetaTokensTmp);
        }
    }

    public static String obtenerAccessToken(String refreshToken)
            throws IOException, java.security.GeneralSecurityException {
        NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        GoogleTokenResponse response = new GoogleRefreshTokenRequest(
                httpTransport, GsonFactory.getDefaultInstance(), refreshToken,
                obtenerClientId(), obtenerClientSecret()).execute();
        return response.getAccessToken();
    }

    private static String obtenerCorreoDesdeAccessToken(String accessToken) throws IOException {
        URL url = new URL("https://www.googleapis.com/oauth2/v3/userinfo");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestProperty("Authorization", "Bearer " + accessToken);
        con.setConnectTimeout(15000);
        con.setReadTimeout(15000);
        try (InputStream is = con.getInputStream();
                InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            return json.has("email") ? json.get("email").getAsString() : "";
        }
    }

    private static void borrarCarpetaTemporal(File carpeta) {
        if (carpeta == null || !carpeta.exists()) return;
        File[] archivos = carpeta.listFiles();
        if (archivos != null) {
            for (File f : archivos) {
                if (f.isDirectory()) borrarCarpetaTemporal(f); else f.delete();
            }
        }
        carpeta.delete();
    }
}
