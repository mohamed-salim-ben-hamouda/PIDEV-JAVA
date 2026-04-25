package com.pidev.Services;

import com.pidev.models.StudentRiskInsight;
import java.util.Properties;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class SupervisorMailService {

    // IMPORTANT : Remplacez par votre e-mail et votre mot de passe d'application (ex: Gmail)
    private static final String SENDER_EMAIL = "nouh.mezned@esprit.tn";
    private static final String SENDER_PASSWORD = "ucax blgx bfia yvqs";

    /**
     * Envoie une alerte au superviseur concernant un etudiant a risque.
     */
    public void sendRiskAlert(StudentRiskInsight insight, String supervisorEmail) {
        if (insight == null || supervisorEmail == null || supervisorEmail.isBlank()) {
            System.err.println("Donnees manquantes pour l'envoi de l'alerte.");
            return;
        }

        System.out.println("Preparation de l'envoi de l'email a : " + supervisorEmail);

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com"); // Serveur SMTP par defaut (Gmail)
        props.put("mail.smtp.port", "587");

        Session session = Session.getInstance(props,
            new javax.mail.Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(SENDER_EMAIL, SENDER_PASSWORD);
                }
            });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(SENDER_EMAIL));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(supervisorEmail));
            message.setSubject("Alerte de risque - " + insight.getStudent().getDisplayName());

            String content = "Bonjour Superviseur,\n\n"
                    + "L'etudiant " + insight.getStudent().getDisplayName() + " presente un risque : " + insight.getRiskLevel() + ".\n"
                    + "Moyenne : " + String.format("%.1f%%", insight.getAverageScore()) + " | Tentatives : " + insight.getAttempts() + "\n"
                    + "Diagnostic : " + insight.getReason() + "\n\n"
                    + "Veuillez prendre les actions necessaires.\n";

            message.setText(content);

            Transport.send(message);
            System.out.println("Email envoye avec succes a " + supervisorEmail + " !");

        } catch (MessagingException e) {
            System.err.println("Erreur lors de l'envoi de l'email : " + e.getMessage());
            e.printStackTrace();
        }
    }
}
