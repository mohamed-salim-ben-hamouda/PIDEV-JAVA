package com.pidev.Services;

import com.pidev.utils.EmailConfig;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

public class EmailService {

    public boolean sendResetCode(String recipientEmail, String code) {
        // Fix ClassLoader issue when running JavaMail in a new Thread (especially in JavaFX/Modules)
        Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());

        Properties prop = new Properties();
        prop.put("mail.smtp.host", EmailConfig.SMTP_HOST);
        prop.put("mail.smtp.port", EmailConfig.SMTP_PORT);
        prop.put("mail.smtp.auth", "true");
        prop.put("mail.smtp.starttls.enable", "true");
        prop.put("mail.smtp.ssl.protocols", "TLSv1.2");
        prop.put("mail.smtp.ssl.trust", "smtp.gmail.com");

        Session session = Session.getInstance(prop,
                new javax.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(EmailConfig.SMTP_USERNAME, EmailConfig.SMTP_PASSWORD);
                    }
                });
        session.setDebug(true); // Active les logs détaillés de JavaMail dans la console

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(EmailConfig.SMTP_USERNAME));
            message.setRecipients(
                    Message.RecipientType.TO,
                    InternetAddress.parse(recipientEmail)
            );
            message.setSubject("Code de réinitialisation de votre mot de passe");
            
            String htmlContent = "<div style='font-family: Arial, sans-serif; padding: 20px; color: #333;'>"
                    + "<h2>Réinitialisation de mot de passe</h2>"
                    + "<p>Bonjour,</p>"
                    + "<p>Vous avez demandé à réinitialiser votre mot de passe. Voici votre code de vérification :</p>"
                    + "<h1 style='color: #5c89ff; letter-spacing: 5px; padding: 10px; background: #f4f4f4; border-radius: 5px; width: fit-content;'>" + code + "</h1>"
                    + "<p>Ce code expirera bientôt. Si vous n'avez pas fait cette demande, veuillez ignorer cet email.</p>"
                    + "<br><p>L'équipe de support.</p>"
                    + "</div>";

            message.setContent(htmlContent, "text/html; charset=utf-8");

            Transport.send(message);
            return true;

        } catch (MessagingException e) {
            System.err.println("Failed to send email: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
