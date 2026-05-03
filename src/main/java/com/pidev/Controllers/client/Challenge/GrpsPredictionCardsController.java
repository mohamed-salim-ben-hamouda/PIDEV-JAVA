package com.pidev.Controllers.client.Challenge;

import com.pidev.models.GroupPredictionCardData;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class GrpsPredictionCardsController {
    private static final double BAR_WIDTH = 270.0;

    @FXML
    private VBox cardRoot;
    @FXML
    private Label groupName;
    @FXML
    private Label groupSubtitle;
    @FXML
    private StackPane statusBadge;
    @FXML
    private Label statusBadgeLabel;
    @FXML
    private Label successPercentageLabel;
    @FXML
    private Label failPercentageLabel;
    @FXML
    private Pane progressFill;
    @FXML
    private VBox recommendationBox;
    @FXML
    private Label recommendationLabel;

    public void setCardData(GroupPredictionCardData cardData) {
        groupName.setText(cardData.getGroup().getName());
        groupSubtitle.setText(cardData.getSubtitle());
        recommendationLabel.setText(cardData.getRecommendation());

        if (!cardData.isAvailable()) {
            statusBadgeLabel.setText(cardData.getStatus());
            successPercentageLabel.setText("--");
            failPercentageLabel.setText("--");
            progressFill.setPrefWidth(0);
            progressFill.setMinWidth(0);
            progressFill.setMaxWidth(0);

            if ("PENDING".equalsIgnoreCase(cardData.getStatus())) {
                applyPendingTheme();
            } else {
                applyNoDataTheme();
            }
            return;
        }

        statusBadgeLabel.setText(cardData.getStatus().toUpperCase());
        successPercentageLabel.setText(String.format("%.2f%%", cardData.getSuccessPercentage()));
        failPercentageLabel.setText(String.format("%.2f%%", cardData.getFailPercentage()));

        double barWidth = Math.max(0, Math.min(BAR_WIDTH, (cardData.getSuccessPercentage() / 100.0) * BAR_WIDTH));
        progressFill.setPrefWidth(barWidth);
        progressFill.setMinWidth(barWidth);
        progressFill.setMaxWidth(barWidth);

        if ("success".equalsIgnoreCase(cardData.getStatus())) {
            applySuccessTheme();
        } else {
            applyRiskTheme();
        }
    }

    private void applySuccessTheme() {
        statusBadge.setStyle("-fx-background-color: #dcfce7; -fx-background-radius: 999;");
        statusBadgeLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #15803d;");
        successPercentageLabel.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: #16a34a;");
        progressFill.setStyle("-fx-background-color: #22c55e; -fx-background-radius: 999;");
        recommendationBox.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 16; -fx-padding: 14;");
        cardRoot.setStyle("-fx-background-color: white; -fx-background-radius: 22; -fx-padding: 20; -fx-effect: dropshadow(three-pass-box, rgba(15,23,42,0.08), 14, 0, 0, 4);");
    }

    private void applyRiskTheme() {
        statusBadge.setStyle("-fx-background-color: #fee2e2; -fx-background-radius: 999;");
        statusBadgeLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #b91c1c;");
        successPercentageLabel.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: #dc2626;");
        progressFill.setStyle("-fx-background-color: #ef4444; -fx-background-radius: 999;");
        recommendationBox.setStyle("-fx-background-color: #fff7ed; -fx-background-radius: 16; -fx-padding: 14;");
        cardRoot.setStyle("-fx-background-color: white; -fx-background-radius: 22; -fx-padding: 20; -fx-effect: dropshadow(three-pass-box, rgba(15,23,42,0.08), 14, 0, 0, 4);");
    }

    private void applyNoDataTheme() {
        statusBadge.setStyle("-fx-background-color: #e2e8f0; -fx-background-radius: 999;");
        statusBadgeLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #475569;");
        successPercentageLabel.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: #64748b;");
        progressFill.setStyle("-fx-background-color: transparent; -fx-background-radius: 999;");
        recommendationBox.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 16; -fx-padding: 14;");
        cardRoot.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 22; -fx-padding: 20; -fx-effect: dropshadow(three-pass-box, rgba(15,23,42,0.06), 14, 0, 0, 4);");
    }

    private void applyPendingTheme() {
        statusBadge.setStyle("-fx-background-color: #dbeafe; -fx-background-radius: 999;");
        statusBadgeLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #1d4ed8;");
        successPercentageLabel.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: #94a3b8;");
        progressFill.setStyle("-fx-background-color: transparent; -fx-background-radius: 999;");
        recommendationBox.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 16; -fx-padding: 14;");
        cardRoot.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 22; -fx-padding: 20; -fx-effect: dropshadow(three-pass-box, rgba(15,23,42,0.06), 14, 0, 0, 4);");
    }
}
