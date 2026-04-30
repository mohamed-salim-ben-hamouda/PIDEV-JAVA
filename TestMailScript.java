import java.util.Properties;
import javax.mail.*;
import javax.mail.internet.*;

public class TestMailScript {
    public static void main(String[] args) {
        Properties prop = new Properties();
        prop.put("mail.smtp.host", "smtp.gmail.com");
        prop.put("mail.smtp.port", "587");
        prop.put("mail.smtp.auth", "true");
        prop.put("mail.smtp.starttls.enable", "true");
        prop.put("mail.smtp.ssl.protocols", "TLSv1.2");
        prop.put("mail.smtp.ssl.trust", "smtp.gmail.com");

        Session session = Session.getInstance(prop, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication("tas.sam.se@gmail.com", "wvhqdpzchbvdbjpk");
            }
        });
        session.setDebug(true); // Print debug info to console

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress("tas.sam.se@gmail.com"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse("tas.sam.se@gmail.com"));
            message.setSubject("Test");
            message.setText("Test Email");
            Transport.send(message);
            System.out.println("SUCCESS");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
