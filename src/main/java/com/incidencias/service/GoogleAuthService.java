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
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

public class GoogleAuthService {

    private static String obtenerClientId() {
        return ConfiguracionPublica.obtenerGoogleClientId();
    }

    private static String obtenerClientSecret() {
        return ConfiguracionPublica.obtenerGoogleClientSecret();
    }

    private static final List<String> SCOPES = List.of(
            "https://mail.google.com/",
            "https://www.googleapis.com/auth/userinfo.email",
            "openid");
    private static final String CARPETA_TOKENS_TEMPORAL = "tokens_google_tmp";
    private static final URI URI_USERINFO = URI.create("https://www.googleapis.com/oauth2/v3/userinfo");
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

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

    private static String obtenerCorreoDesdeAccessToken(String accessToken)
            throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI_USERINFO)
                .timeout(Duration.ofSeconds(15))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();

        final HttpResponse<String> response;
        try {
            response = HTTP_CLIENT.send(
                    request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw ex;
        }

        if (response.statusCode() / 100 != 2) {
            throw new IOException(
                    "Google devolvió HTTP " + response.statusCode() + " al consultar la cuenta.");
        }

        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
        return json.has("email") ? json.get("email").getAsString() : "";
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
