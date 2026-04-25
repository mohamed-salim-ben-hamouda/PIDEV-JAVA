package com.pidev.Controllers.admin;

import com.pidev.Services.LearningIntelligenceService;
import com.pidev.Services.SupervisorMailService;
import com.pidev.models.StudentRiskInsight;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class LearningIntelligenceController {
    @FXML private TextField searchField;
    @FXML private javafx.scene.control.ComboBox<String> riskFilterCombo;
    @FXML private VBox insightsContainer;
    @FXML private Label emptyStateLabel;

    @FXML private Label totalStudentsLabel;
    @FXML private Label highRiskLabel;
    @FXML private Label mediumRiskLabel;
    @FXML private Label lowRiskLabel;
    @FXML private Label globalAverageLabel;

    private final LearningIntelligenceService service = new LearningIntelligenceService();
    private final SupervisorMailService mailService = new SupervisorMailService();
    private List<StudentRiskInsight> allInsights = new ArrayList<>();

    @FXML
    public void initialize() {
        riskFilterCombo.getItems().setAll("Tous", "Risque eleve", "Risque moyen", "Risque faible");
        riskFilterCombo.setValue("Tous");
        searchField.textProperty().addListener((obs, oldVal, newVal) -> renderFiltered());
        riskFilterCombo.valueProperty().addListener((obs, oldVal, newVal) -> renderFiltered());
        onRefresh();
    }

    @FXML
    private void onSearch() {
        renderFiltered();
    }

    @FXML
    private void onRefresh() {
        try {
            allInsights = service.findRiskInsights();
            updateMetrics();
            renderFiltered();
        } catch (SQLException e) {
            showError("DB Error", e.getMessage());
            allInsights = new ArrayList<>();
            updateMetrics();
            renderFiltered();
        }
    }

    private void updateMetrics() {
        LearningIntelligenceService.RiskDashboardMetrics metrics = service.computeMetrics(allInsights);
        totalStudentsLabel.setText(String.valueOf(metrics.totalStudents()));
        highRiskLabel.setText(String.valueOf(metrics.highRisk()));
        mediumRiskLabel.setText(String.valueOf(metrics.mediumRisk()));
        lowRiskLabel.setText(String.valueOf(metrics.lowRisk()));
        globalAverageLabel.setText(String.format("%.1f%%", metrics.globalAverageScore()));
    }

    private void renderFiltered() {
        insightsContainer.getChildren().clear();
        String search = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        String filter = riskFilterCombo.getValue() == null ? "Tous" : riskFilterCombo.getValue();

        List<StudentRiskInsight> filtered = allInsights.stream().filter(item -> {
            boolean textOk = search.isEmpty()
                    || item.getStudent().getDisplayName().toLowerCase().contains(search)
                    || (item.getStudent().getEmail() != null && item.getStudent().getEmail().toLowerCase().contains(search));

            boolean riskOk = switch (filter) {
                case "Risque eleve" -> item.getRiskLevel() == StudentRiskInsight.RiskLevel.HIGH;
                case "Risque moyen" -> item.getRiskLevel() == StudentRiskInsight.RiskLevel.MEDIUM;
                case "Risque faible" -> item.getRiskLevel() == StudentRiskInsight.RiskLevel.LOW;
                default -> true;
            };
            return textOk && riskOk;
        }).toList();

        emptyStateLabel.setVisible(filtered.isEmpty());
        emptyStateLabel.setManaged(filtered.isEmpty());

        for (StudentRiskInsight insight : filtered) {
            insightsContainer.getChildren().add(buildRiskCard(insight));
        }
    }

    private VBox buildRiskCard(StudentRiskInsight insight) {
        VBox card = new VBox(10);
        card.getStyleClass().add("intelligence-card");
        card.setPadding(new Insets(14));

        Label name = new Label(insight.getStudent().getDisplayName());
        name.getStyleClass().add("intelligence-student-name");
        Label email = new Label(insight.getStudent().getEmail() == null ? "-" : insight.getStudent().getEmail());
        email.getStyleClass().add("intelligence-student-email");

        Label riskBadge = new Label(riskText(insight.getRiskLevel()) + " - " + insight.getRiskScore() + "/100");
        riskBadge.getStyleClass().addAll("intelligence-risk-badge", riskClass(insight.getRiskLevel()));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        VBox idBox = new VBox(2, name, email);
        
        Button alertBtn = new Button("Alerter Superviseur");
        alertBtn.getStyleClass().addAll("management-btn", "danger");
        alertBtn.setOnAction(e -> onAlertSupervisor(insight));
        
        HBox head = new HBox(10, idBox, spacer, alertBtn, riskBadge);
        head.setAlignment(Pos.CENTER_LEFT);

        HBox metrics = new HBox(12,
                metricPill("Tentatives", String.valueOf(insight.getAttempts())),
                metricPill("Moyenne", String.format("%.1f%%", insight.getAverageScore())),
                metricPill("Reussite", String.format("%.1f%%", insight.getPassRate()))
        );

        Label reason = new Label("Diagnostic: " + insight.getReason());
        reason.getStyleClass().add("intelligence-reason");
        reason.setWrapText(true);

        Label actionsTitle = new Label("Actions recommandees");
        actionsTitle.getStyleClass().add("intelligence-section-title");
        VBox actionsBox = new VBox(4);
        List<String> actions = insight.getRecommendedActions() == null
                ? List.of("Aucune action disponible")
                : insight.getRecommendedActions();
        for (String action : actions) {
            Label line = new Label("- " + action);
            line.getStyleClass().add("intelligence-list-item");
            line.setWrapText(true);
            actionsBox.getChildren().add(line);
        }

        Label coursesTitle = new Label("Cours suggeres");
        coursesTitle.getStyleClass().add("intelligence-section-title");
        VBox courseBox = new VBox(4);
        List<String> courses = insight.getRecommendedCourses() == null
                ? List.of("Aucune recommandation disponible")
                : insight.getRecommendedCourses();
        for (String course : courses) {
            Label line = new Label("- " + course);
            line.getStyleClass().add("intelligence-list-item");
            line.setWrapText(true);
            courseBox.getChildren().add(line);
        }

        card.getChildren().addAll(head, metrics, reason, actionsTitle, actionsBox, coursesTitle, courseBox);
        return card;
    }

    private void onAlertSupervisor(StudentRiskInsight insight) {
        javafx.scene.control.TextInputDialog dialog = new javafx.scene.control.TextInputDialog("superviseur@domaine.com");
        dialog.setTitle("Adresse du superviseur");
        dialog.setHeaderText("Alerte pour " + insight.getStudent().getDisplayName());
        dialog.setContentText("Veuillez saisir l'adresse e-mail du superviseur :");

        java.util.Optional<String> result = dialog.showAndWait();
        result.ifPresent(email -> {
            if (!email.trim().isEmpty()) {
                mailService.sendRiskAlert(insight, email.trim());
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Un email de recommandation a ete envoye au superviseur concernant " + insight.getStudent().getDisplayName() + "\n\n(Simulation dans la console)");
                alert.setTitle("Alerte envoyee");
                alert.setHeaderText(null);
                alert.showAndWait();
            } else {
                Alert errorAlert = new Alert(Alert.AlertType.ERROR, "L'adresse e-mail ne peut pas être vide.");
                errorAlert.setTitle("Erreur");
                errorAlert.setHeaderText(null);
                errorAlert.showAndWait();
            }
        });
    }

    private VBox metricPill(String label, String value) {
        Label l = new Label(label);
        l.getStyleClass().add("intelligence-metric-label");
        Label v = new Label(value);
        v.getStyleClass().add("intelligence-metric-value");
        VBox box = new VBox(2, v, l);
        box.getStyleClass().add("intelligence-metric-pill");
        box.setAlignment(Pos.CENTER);
        HBox.setHgrow(box, Priority.ALWAYS);
        return box;
    }

    private String riskText(StudentRiskInsight.RiskLevel risk) {
        return switch (risk) {
            case HIGH -> "Risque eleve";
            case MEDIUM -> "Risque moyen";
            default -> "Risque faible";
        };
    }

    private String riskClass(StudentRiskInsight.RiskLevel risk) {
        return switch (risk) {
            case HIGH -> "intelligence-risk-high";
            case MEDIUM -> "intelligence-risk-medium";
            default -> "intelligence-risk-low";
        };
    }

    private void showError(String title, String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR, message);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}