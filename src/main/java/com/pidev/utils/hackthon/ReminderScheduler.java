package com.pidev.utils.hackthon;

import com.pidev.Services.ServiceHackathon;
import com.pidev.Services.ServiceParticipation;
import com.pidev.models.Hackathon;
import com.pidev.models.Participation;
import com.pidev.models.User;
import com.pidev.utils.SessionManager;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Planificateur pour envoyer des rappels automatiques aux participants du hackathon.
 */
public class ReminderScheduler {
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private static final ServiceParticipation serviceP = new ServiceParticipation();
    private static final ServiceHackathon serviceH = new ServiceHackathon();

    /**
     * Démarre la vérification périodique des rappels.
     */
    public static void start() {
        // Vérifie toutes les 30 secondes pour les tests/démo
        scheduler.scheduleAtFixedRate(ReminderScheduler::checkAndSendReminders, 0, 30, TimeUnit.SECONDS);
        System.out.println("Reminder Scheduler is running in background (DEBUG MODE: 1 min delay)...");
    }

    /**
     * Vérifie les participations et envoie un mail de test 1 minute après l'inscription.
     */
    private static void checkAndSendReminders() {
        List<Participation> participations = serviceP.getAll();
        LocalDateTime now = LocalDateTime.now();

        for (Participation p : participations) {
            Hackathon h = serviceH.getById(p.getHackathon().getId());
            if (h != null && p.getRegisteredAt() != null) {
                long secondsSinceRegistration = ChronoUnit.SECONDS.between(p.getRegisteredAt(), now);

                // MODE TEST : Envoie le rappel exactement 1 minute (entre 60s et 90s) après l'inscription
                // On utilise une plage de 30s car le scheduler tourne toutes les 30s
                if (secondsSinceRegistration >= 60 && secondsSinceRegistration < 90) {
                    sendReminderEmail(h);
                }
            }
        }
    }

    private static void sendReminderEmail(Hackathon h) {
        User user = SessionManager.getUser(); // Simulation de l'utilisateur connecté
        if (user != null && user.getEmail() != null) {
            String content = "<div style='font-family: \"Segoe UI\", Tahoma, Geneva, Verdana, sans-serif; max-width: 600px; margin: 0 auto; border: 1px solid #e0e0e0; border-radius: 12px; overflow: hidden;'>" +
                    "  <div style='background: linear-gradient(135deg, #f093fb, #f5576c); padding: 40px 20px; text-align: center; color: white;'>" +
                    "    <h1 style='margin: 0; font-size: 28px; letter-spacing: 1px;'>Rappel : C'est pour Demain !</h1>" +
                    "    <p style='margin-top: 10px; font-size: 16px; opacity: 0.9;'>Votre hackathon commence dans 24 heures ⏳</p>" +
                    "  </div>" +
                    "  <div style='padding: 30px; background: white;'>" +
                    "    <p style='color: #444; font-size: 16px;'>Bonjour <strong>" + user.getDisplayName() + "</strong>,</p>" +
                    "    <p style='color: #666; line-height: 1.6;'>Ceci est un rappel amical que le hackathon <strong>" + h.getTitle() + "</strong> approche à grands pas ! Il est temps de préparer votre café, vos outils et votre motivation.</p>" +
                    "    <div style='background: #fff5f5; border-radius: 8px; padding: 20px; margin: 25px 0; border-left: 4px solid #f5576c;'>" +
                    "      <p style='margin: 0 0 10px 0; color: #333;'><strong>📍 Lieu :</strong> " + h.getLocation() + "</p>" +
                    "      <p style='margin: 0; color: #333;'><strong>📅 Coup d'envoi :</strong> " + h.getStartAt().format(DateTimeFormatter.ofPattern("dd MMMM yyyy 'à' HH:mm")) + "</p>" +
                    "    </div>" +
                    "    <p style='color: #666; text-align: center; margin-top: 30px;'>Nous avons hâte de vous voir à l'œuvre !</p>" +
                    "  </div>" +
                    "  <div style='background: #f1f1f1; padding: 20px; text-align: center; color: #888; font-size: 12px;'>" +
                    "    <p style='margin: 0;'>© 2024 Skill Bridge - MindCare Team. Tous droits réservés.</p>" +
                    "  </div>" +
                    "</div>";

            EmailService.sendEmail(user.getEmail(), "Rappel Hackathon : " + h.getTitle() + " commence demain !", content);
            System.out.println("Reminder email sent for: " + h.getTitle());
        }
    }
}