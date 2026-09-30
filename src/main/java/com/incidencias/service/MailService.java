package com.incidencias.service;

import com.incidencias.Version;
import com.incidencias.utils.LoggerUtil;

import jakarta.activation.DataHandler;
import jakarta.activation.DataSource;
import jakarta.activation.FileDataSource;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class MailService {

    public static void enviarCorreoSMTP(Path rutaArchivo, String correoEnvio, char[] credencial,
            String destinatario, String asunto,
            String smtpHost, int smtpPort, boolean usarSSL, boolean esOAuth,
            boolean incluirInfoSistema) throws Exception {

        if (com.incidencias.utils.FailureSimulator.isSimularSinInternet()) {
            LoggerUtil.log("SIMULADOR", "Disparando fallo simulado de conexión SMTP (FailureSimulator)...");
            throw new MessagingException(
                    "Could not connect to SMTP host: " + smtpHost + ", port: " + smtpPort + ", response: -1");
        }

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.host", smtpHost);
        props.put("mail.smtp.port", String.valueOf(smtpPort));
        props.put("mail.smtp.connectiontimeout", "50000");
        props.put("mail.smtp.timeout", "50000");
        props.put("mail.smtp.writetimeout", "50000");

        if (esOAuth) {
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.starttls.required", "true");
            props.put("mail.smtp.auth.mechanisms", "XOAUTH2");
        } else if (usarSSL) {
            props.put("mail.smtp.ssl.enable", "true");
            // Jakarta Mail ya gestiona la SSL directa mediante mail.smtp.ssl.enable.
        } else {
            props.put("mail.smtp.starttls.enable", "true");
        }

        if (Version.isModoDebug()) {
            props.put("mail.debug", "true");
            LoggerUtil.log("SMTP", "Propiedades de sesión JavaMail: " + props);
        }

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(correoEnvio, new String(credencial));
            }
        });

        if (Version.isModoDebug()) {
            session.setDebug(true);
            session.setDebugOut(System.out);
        }

        Message mensaje = new MimeMessage(session);
        mensaje.setFrom(new InternetAddress(correoEnvio));
        mensaje.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destinatario));
        mensaje.setSubject(asunto);

        BodyPart textoCuerpo = new MimeBodyPart();

        if (incluirInfoSistema) {
            String nombreEquipo = "Desconocido";
            String ip = "Desconocida";
            String usuario = System.getProperty("user.name", "Desconocido");
            try {
                InetAddress localHost = InetAddress.getLocalHost();
                nombreEquipo = localHost.getHostName();
                ip = localHost.getHostAddress();
            } catch (Exception e) {
                LoggerUtil.error("SMTP", "No se pudo resolver el hostname/IP local", e);
            }
            String textoMensaje = String.format("Enviado desde \"%s\" por \"%s\" con IP: \"%s\"", nombreEquipo, usuario,
                    ip);
            LoggerUtil.log("SMTP", "Cuerpo del mensaje (info sistema): " + textoMensaje);
            textoCuerpo.setText(textoMensaje);
        } else {
            textoCuerpo.setText("");
        }

        LoggerUtil.log("SMTP",
                "Adjuntando archivo: " + rutaArchivo.toAbsolutePath() + " (" + Files.size(rutaArchivo) + " bytes)");

        BodyPart archivoAdjunto = new MimeBodyPart();
        DataSource fuente = new FileDataSource(rutaArchivo.toFile());
        archivoAdjunto.setDataHandler(new DataHandler(fuente));
        archivoAdjunto.setFileName(rutaArchivo.getFileName().toString());

        Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(textoCuerpo);
        multipart.addBodyPart(archivoAdjunto);

        mensaje.setContent(multipart);

        long inicio = System.currentTimeMillis();
        LoggerUtil.log("SMTP", "Iniciando Transport.send()...");
        Transport.send(mensaje);
        LoggerUtil.log("SMTP", "Transport.send() finalizado en " + (System.currentTimeMillis() - inicio) + " ms.");
    }
}
