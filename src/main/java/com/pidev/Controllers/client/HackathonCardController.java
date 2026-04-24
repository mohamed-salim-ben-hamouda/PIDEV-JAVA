package com.pidev.Controllers.client;

import com.pidev.Services.ServiceSponsorHackathon;
import com.pidev.models.Hackathon;
import com.pidev.Services.ServiceParticipation;
import com.pidev.models.Participation;
import com.pidev.utils.hackthon.StripeService;
import com.pidev.utils.hackthon.StripePaymentWindow;
import com.pidev.utils.hackthon.CalendarService;
import com.pidev.utils.hackthon.EmailService;
import com.pidev.utils.SessionManager;
import com.pidev.models.User;
import com.pidev.models.SponsorHackathon;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import java.io.IOException;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class HackathonCardController {

    @FXML private ImageView coverImage;
    @FXML private Label feeBadge, titleLabel, dateLabel, locationLabel, teamsLabel;
    @FXML private HBox sponsorsBox;
    @FXML private Button participateBtn;

    private ServiceSponsorHackathon serviceSH = new ServiceSponsorHackathon();
    private ServiceParticipation serviceP = new ServiceParticipation();
    private Hackathon currentHackathon;

    public void setHackathonData(Hackathon h) {
        this.currentHackathon = h;
        titleLabel.setText(h.getTitle());
        dateLabel.setText(h.getStartAt().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")));
        locationLabel.setText(h.getLocation());
        teamsLabel.setText("Max " + h.getMaxTeams() + " Teams");
        feeBadge.setText(h.getFee() > 0 ? h.getFee() + " TND" : "Free");

        if (h.getCoverUrl() != null && !h.getCoverUrl().isEmpty()) {
            try {
                // Background loading = true to prevent UI freezing
                Image img = new Image(h.getCoverUrl(), true);
                coverImage.setImage(img);
            } catch (Exception e) {
                // Fallback to placeholder if URL is malformed
                coverImage.setImage(new Image(getClass().getResourceAsStream("/images/home_pic.jpg")));
            }
        } else {
            // Default placeholder if no URL
            coverImage.setImage(new Image(getClass().getResourceAsStream("/images/home_pic.jpg")));
        }

        // Apply clipping for rounded corners (top only is better, but this matches card shape)
        javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(320, 180);
        clip.setArcWidth(40);
        clip.setArcHeight(40);
        coverImage.setClip(clip);

        loadSponsors(h.getId());
    }

    @FXML
    private void handleAIConsult() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Fxml/client/ai_advice_view.fxml"));
            Parent view = loader.load();
            
            AIAdviceController controller = loader.getController();
            controller.setHackathon(currentHackathon);

            StackPane contentArea = (StackPane) participateBtn.getScene().lookup("#contentArea");
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleParticipate() {
        if (currentHackathon == null) return;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Participation au Hackathon");
        alert.setHeaderText("Confirmer votre participation");
        alert.setContentText("Voulez-vous vous inscrire au hackathon : " + currentHackathon.getTitle() + " ?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                // If hackathon is free, participate immediately
                if (currentHackathon.getFee() == null || currentHackathon.getFee() <= 0) {
                    completeParticipation();
                    return;
                }

                // If paid, show WebView
                String paymentUrl = StripeService.getPaymentUrl(currentHackathon);
                StripePaymentWindow.show(paymentUrl, this::completeParticipation, () -> {
                    Alert cancel = new Alert(Alert.AlertType.WARNING);
                    cancel.setContentText("Le paiement a été annulé.");
                    cancel.show();
                });

            } catch (Exception e) {
                e.printStackTrace();
                Alert error = new Alert(Alert.AlertType.ERROR);
                error.setContentText("Erreur lors de l'initialisation du paiement : " + e.getMessage());
                error.showAndWait();
            }
        }
    }

    private void completeParticipation() {
        Participation p = new Participation();
        p.setHackathon(currentHackathon);
        p.setStatus("inscrit");
        p.setPaymentStatus("payé");
        p.setPaymentRef("REF-" + System.currentTimeMillis()); // Avoids 'payment_ref' cannot be null
        
        serviceP.add(p);

        // Send Email Confirmation
        User user = SessionManager.getUser();
        if (user != null && user.getEmail() != null) {
            String emailContent = "<div style='font-family: \"Segoe UI\", Tahoma, Geneva, Verdana, sans-serif; max-width: 600px; margin: 0 auto; border: 1px solid #e0e0e0; border-radius: 12px; overflow: hidden;'>" +
                    "  <div style='background: linear-gradient(135deg, #6e8efb, #a777e3); padding: 40px 20px; text-align: center; color: white;'>" +
                    "    <h1 style='margin: 0; font-size: 28px; letter-spacing: 1px;'>Participation Confirmée !</h1>" +
                    "    <p style='margin-top: 10px; font-size: 16px; opacity: 0.9;'>Préparez-vous pour l'aventure 🚀</p>" +
                    "  </div>" +
                    "  <div style='padding: 30px; background: white;'>" +
                    "    <p style='color: #444; font-size: 16px;'>Bonjour <strong>" + user.getDisplayName() + "</strong>,</p>" +
                    "    <p style='color: #666; line-height: 1.6;'>Nous sommes ravis de vous confirmer votre participation au hackathon <strong>" + currentHackathon.getTitle() + "</strong>. C'est une opportunité incroyable de montrer vos talents et de rencontrer des passionnés !</p>" +
                    "    <div style='background: #f8f9fa; border-radius: 8px; padding: 20px; margin: 25px 0; border-left: 4px solid #6e8efb;'>" +
                    "      <p style='margin: 0 0 10px 0; color: #333;'><strong>📍 Lieu :</strong> " + currentHackathon.getLocation() + "</p>" +
                    "      <p style='margin: 0; color: #333;'><strong>📅 Début :</strong> " + currentHackathon.getStartAt().format(DateTimeFormatter.ofPattern("dd MMMM yyyy 'à' HH:mm")) + "</p>" +
                    "    </div>" +
                    "    <p style='color: #666; font-size: 14px; font-style: italic; text-align: center; margin-top: 30px;'>" +
                    "      💡 Un rappel automatique vous sera envoyé 24 heures avant le début de l'événement." +
                    "    </p>" +
                    "  </div>" +
                    "  <div style='background: #f1f1f1; padding: 20px; text-align: center; color: #888; font-size: 12px;'>" +
                    "    <p style='margin: 0;'>© 2024 Skill Bridge - MindCare Team. Tous droits réservés.</p>" +
                    "  </div>" +
                    "</div>";
            
            // Running email task in background to avoid UI freeze
            new Thread(() -> {
                EmailService.sendEmail(user.getEmail(), "Participation confirmée : " + currentHackathon.getTitle(), emailContent);
            }).start();
        }

        Alert success = new Alert(Alert.AlertType.INFORMATION);
        success.setTitle("Succès");
        success.setHeaderText(null);
        success.setContentText("Votre participation a été enregistrée avec succès ! Un e-mail de confirmation vous a été envoyé.");
        success.showAndWait();

        // Propose to add to Google Calendar
        Alert calendarAlert = new Alert(Alert.AlertType.CONFIRMATION);
        calendarAlert.setTitle("Google Calendar");
        calendarAlert.setHeaderText("Ajouter à votre calendrier ?");
        calendarAlert.setContentText("Voulez-vous ajouter cet événement à votre Google Calendar pour ne pas l'oublier ?");
        
        ButtonType btnYes = new ButtonType("Oui, ajouter");
        ButtonType btnNo = new ButtonType("Non, merci", ButtonBar.ButtonData.CANCEL_CLOSE);
        calendarAlert.getButtonTypes().setAll(btnYes, btnNo);

        calendarAlert.showAndWait().ifPresent(response -> {
            if (response == btnYes) {
                CalendarService.addToGoogleCalendar(currentHackathon);
            }
        });
    }

    private void loadSponsors(int hackathonId) {
        List<SponsorHackathon> sponsors = serviceSH.getByHackathon(hackathonId);
        sponsorsBox.getChildren().clear();
        for (SponsorHackathon sh : sponsors) {
            ImageView logo = new ImageView();
            try {
                if (sh.getSponsor().getLogoUrl() != null) {
                    logo.setImage(new Image(sh.getSponsor().getLogoUrl(), true));
                    logo.setFitHeight(30);
                    logo.setPreserveRatio(true);
                    sponsorsBox.getChildren().add(logo);
                }
            } catch (Exception e) {
                // Skip invalid logo
            }
        }
        if (sponsors.isEmpty()) {
            Label noSponsors = new Label("No sponsors yet");
            noSponsors.setStyle("-fx-font-size: 11px; -fx-text-fill: #aaa;");
            sponsorsBox.getChildren().add(noSponsors);
        }
    }
}
