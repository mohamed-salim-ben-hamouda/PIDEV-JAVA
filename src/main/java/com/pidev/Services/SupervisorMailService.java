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
     * Envoie une alerte de soutien directement a l'etudiant concerne.
     */
    public void sendRiskAlert(StudentRiskInsight insight, String studentEmail) {
        // Si studentEmail est null, on essaie de le recuperer via l'insight
        String recipient = (studentEmail != null && !studentEmail.isBlank()) 
                           ? studentEmail 
                           : (insight != null && insight.getStudent() != null ? insight.getStudent().getEmail() : null);

        if (insight == null || recipient == null || recipient.isBlank()) {
            System.err.println("Donnees manquantes pour l'envoi de l'alerte a l'etudiant.");
            return;
        }

        System.out.println("Preparation de l'envoi de l'email de soutien a : " + recipient);

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
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
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient));
            message.setSubject("Skill Bridge - Un petit coup de pouce pour vos quiz ?");

            String studentName = insight.getStudent().getDisplayName();
            String content = "Bonjour " + studentName + ",\n\n"
                    + "Nous avons remarque que vous rencontrez quelques difficultes sur vos derniers quiz (Score moyen : " 
                    + String.format("%.1f%%", insight.getAverageScore()) + ").\n\n"
                    + "Ne vous decouragez pas ! Voici un petit diagnostic pour vous aider :\n"
                    + "> " + insight.getReason() + "\n\n"
                    + "Conseils de l'equipe pedagogique :\n";
            
            if (insight.getRecommendedActions() != null && !insight.getRecommendedActions().isEmpty()) {
                for (String action : insight.getRecommendedActions()) {
                    content += "- " + action + "\n";
                }
            } else {
                content += "- Prenez le temps de relire les chapitres cles.\n- N'hesitez pas a poser des questions sur le forum.\n";
            }

            content += "\nContinuez vos efforts, la reussite est au bout du chemin !\n\n"
                    + "L'equipe Skill Bridge";

            message.setText(content);

            Transport.send(message);
            System.out.println("Email de soutien envoye avec succes a " + recipient + " !");

        } catch (MessagingException e) {
            System.err.println("Erreur lors de l'envoi de l'email a l'etudiant : " + e.getMessage());
            e.printStackTrace();
        }
    }
}
