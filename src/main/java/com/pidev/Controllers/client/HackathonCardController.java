package com.pidev.Controllers.client;

import com.pidev.Services.ServiceSponsorHackathon;
import com.pidev.models.Hackathon;
import com.pidev.models.SponsorHackathon;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class HackathonCardController {

    @FXML private ImageView coverImage;
    @FXML private Label feeBadge, titleLabel, dateLabel, locationLabel, teamsLabel;
    @FXML private HBox sponsorsBox;

    private ServiceSponsorHackathon serviceSH = new ServiceSponsorHackathon();

    public void setHackathonData(Hackathon h) {
        titleLabel.setText(h.getTitle());
        dateLabel.setText(h.getStartAt().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")));
        locationLabel.setText(h.getLocation());
        teamsLabel.setText("Max " + h.getMaxTeams() + " Teams");
        feeBadge.setText(h.getFee() > 0 ? h.getFee() + " TND" : "Free");

        try {
            if (h.getCoverUrl() != null && !h.getCoverUrl().isEmpty()) {
                coverImage.setImage(new Image(h.getCoverUrl(), true));
            }
        } catch (Exception e) {
            // Handle invalid image URL
        }

        loadSponsors(h.getId());
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
