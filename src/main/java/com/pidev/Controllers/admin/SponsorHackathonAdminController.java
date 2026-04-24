package com.pidev.Controllers.admin;

import com.pidev.Services.ServiceSponsorHackathon;
import com.pidev.models.SponsorHackathon;
import com.pidev.utils.hackthon.ReportGenerator;
import com.pidev.utils.hackthon.SignatureDialog;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

public class SponsorHackathonAdminController implements Initializable {

    @FXML private ListView<SponsorHackathon> relationListView;
    @FXML private TextField searchField;

    private ServiceSponsorHackathon serviceSH = new ServiceSponsorHackathon();
    private ObservableList<SponsorHackathon> relationList = FXCollections.observableArrayList();
    private FilteredList<SponsorHackathon> filteredData;

    /**
     * Initialise le contrôleur, configure la liste et charge les données.
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupListView();
        loadData();
        setupSearch();
    }

    /**
     * Configure la ListView pour afficher les relations sous forme de cartes.
     */
    private void setupListView() {
        relationListView.setCellFactory(listView -> new SponsorHackathonListCell());
        relationListView.setFixedCellSize(Region.USE_COMPUTED_SIZE);
    }

    /**
     * Charge toutes les affectations de sponsors depuis le service.
     */
    private void loadData() {
        relationList.setAll(serviceSH.getAll());
        if (filteredData != null) {
            relationListView.setItems(filteredData);
        } else {
            relationListView.setItems(relationList);
        }
    }

    /**
     * Configure le filtrage dynamique pour rechercher par nom de hackathon ou de sponsor.
     */
    private void setupSearch() {
        filteredData = new FilteredList<>(relationList, p -> true);
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(sh -> {
                if (newValue == null || newValue.isEmpty()) return true;
                String lowerCaseFilter = newValue.toLowerCase();
                return sh.getHackathon().getTitle().toLowerCase().contains(lowerCaseFilter) ||
                       sh.getSponsor().getName().toLowerCase().contains(lowerCaseFilter);
            });
        });
        relationListView.setItems(filteredData);
    }

    /**
     * Ouvre le formulaire pour créer une nouvelle affectation sponsor-hackathon.
     */
    @FXML
    private void showAddForm() {
        loadForm(null);
    }

    /**
     * Ouvre le formulaire pour modifier une affectation existante.
     */
    private void showEditForm(SponsorHackathon sh) {
        loadForm(sh);
    }

    /**
     * Injecte le formulaire FXML dans la zone de contenu principale.
     */
    private void loadForm(SponsorHackathon sh) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Fxml/admin/sponsor_hackathon_form.fxml"));
            Parent form = loader.load();
            SponsorHackathonFormController controller = loader.getController();
            controller.setSponsorHackathon(sh);

            StackPane contentArea = (StackPane) relationListView.getScene().lookup("#contentArea");
            contentArea.getChildren().setAll(form);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Supprime une affectation après confirmation.
     */
    private void handleDelete(SponsorHackathon sh) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Supprimer l'affectation");
        alert.setHeaderText("Retirer le sponsor du hackathon ?");
        alert.setContentText(sh.getSponsor().getName() + " → " + sh.getHackathon().getTitle());

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            serviceSH.delete(sh.getId());
            loadData();
        }
    }

    /**
     * Génère un contrat de sponsoring au format PDF pour l'affectation sélectionnée.
     */
    private void handleExportContract(SponsorHackathon sh) {
        // Step 1: Open Signature Popup
        SignatureDialog sigDialog = new SignatureDialog();
        File signatureFile = sigDialog.showAndWait();
        
        if (signatureFile == null) {
            // User cancelled signature
            return;
        }

        // Step 2: Save File Dialog
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Enregistrer le contrat de sponsoring");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichiers PDF", "*.pdf"));
        fileChooser.setInitialFileName("Contrat_" + sh.getSponsor().getName().replace(" ", "_") + ".pdf");
        
        File file = fileChooser.showSaveDialog(relationListView.getScene().getWindow());
        if (file != null) {
            try {
                // Step 3: Generate PDF with Signature
                ReportGenerator.generateSponsorshipContract(sh, file.getAbsolutePath(), signatureFile.getAbsolutePath());
                
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Contrat généré");
                alert.setHeaderText(null);
                alert.setContentText("Le contrat de sponsoring a été généré avec succès.");
                alert.showAndWait();
            } catch (Exception e) {
                e.printStackTrace();
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setContentText("Erreur lors de la génération du contrat : " + e.getMessage());
                alert.showAndWait();
            }
        }
    }

    /**
     * Cellule personnalisée pour afficher les détails d'une affectation sponsor-hackathon.
     */
    private class SponsorHackathonListCell extends ListCell<SponsorHackathon> {
        private final HBox card = new HBox(20);
        private final VBox infoBox = new VBox(6);
        private final Label hackathonLabel = new Label();
        private final Label sponsorLabel = new Label();
        private final Label contributionLabel = new Label();
        private final HBox actionsBox = new HBox(10);
        private final Button editBtn = new Button();
        private final Button deleteBtn = new Button();
        private final Button contractBtn = new Button();
        private final Region spacer = new Region();
        private final StackPane iconCircle = new StackPane();

        SponsorHackathonListCell() {
            // Icône stylisée
            FontIcon linkIcon = new FontIcon("fas-link");
            linkIcon.setIconSize(20);
            linkIcon.getStyleClass().add("list-card-icon");
            iconCircle.getChildren().add(linkIcon);
            iconCircle.getStyleClass().add("list-card-icon-circle-accent");
            iconCircle.setMinSize(48, 48);
            iconCircle.setMaxSize(48, 48);

            // Labels d'information
            hackathonLabel.getStyleClass().add("list-card-title");
            sponsorLabel.getStyleClass().add("list-card-subtitle");
            contributionLabel.getStyleClass().add("list-card-meta");

            infoBox.getChildren().addAll(hackathonLabel, sponsorLabel, contributionLabel);
            infoBox.setAlignment(Pos.CENTER_LEFT);

            // Bouton de contrat PDF
            FontIcon pdfIcon = new FontIcon("fas-file-pdf");
            pdfIcon.setIconSize(14);
            pdfIcon.getStyleClass().add("action-icon-pdf");
            contractBtn.setGraphic(pdfIcon);
            contractBtn.getStyleClass().add("list-action-btn-pdf");
            contractBtn.setText("Contrat");

            // Bouton de modification
            FontIcon editIcon = new FontIcon("fas-edit");
            editIcon.setIconSize(14);
            editIcon.getStyleClass().add("action-icon-edit");
            editBtn.setGraphic(editIcon);
            editBtn.getStyleClass().add("list-action-btn-edit");
            editBtn.setText("Modifier");

            // Bouton de suppression
            FontIcon trashIcon = new FontIcon("fas-trash");
            trashIcon.setIconSize(14);
            trashIcon.getStyleClass().add("action-icon-delete");
            deleteBtn.setGraphic(trashIcon);
            deleteBtn.getStyleClass().add("list-action-btn-delete");
            deleteBtn.setText("Retirer");

            actionsBox.getChildren().addAll(contractBtn, editBtn, deleteBtn);
            actionsBox.setAlignment(Pos.CENTER_RIGHT);

            HBox.setHgrow(spacer, Priority.ALWAYS);

            card.getChildren().addAll(iconCircle, infoBox, spacer, actionsBox);
            card.setAlignment(Pos.CENTER_LEFT);
            card.getStyleClass().add("list-card");
            card.setPadding(new Insets(18, 24, 18, 24));
        }

        /**
         * Met à jour l'affichage de la carte avec les données de l'affectation.
         */
        @Override
        protected void updateItem(SponsorHackathon sh, boolean empty) {
            super.updateItem(sh, empty);
            if (empty || sh == null) {
                setGraphic(null);
                setText(null);
                setStyle("-fx-background-color: transparent;");
            } else {
                hackathonLabel.setText(sh.getHackathon().getTitle());
                sponsorLabel.setText("Sponsorisé par : " + (sh.getSponsor().getName() != null ? sh.getSponsor().getName() : "Inconnu"));

                String contribText = "";
                if (sh.getContributionType() != null && !sh.getContributionType().isEmpty()) {
                    contribText = sh.getContributionType();
                }
                if (sh.getContributionValue() != null) {
                    contribText += (contribText.isEmpty() ? "" : " • ") + String.format("$%.2f", sh.getContributionValue());
                }
                contributionLabel.setText(contribText.isEmpty() ? "Aucun détail de contribution" : contribText);

                contractBtn.setOnAction(e -> handleExportContract(sh));
                editBtn.setOnAction(e -> showEditForm(sh));
                deleteBtn.setOnAction(e -> handleDelete(sh));

                setGraphic(card);
                setText(null);
                setStyle("-fx-background-color: transparent; -fx-padding: 4 0 4 0;");
            }
        }
    }
}
