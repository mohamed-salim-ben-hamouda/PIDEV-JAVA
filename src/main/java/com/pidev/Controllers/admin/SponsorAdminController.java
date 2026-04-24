package com.pidev.Controllers.admin;

import com.pidev.Services.ServiceSponsor;
import com.pidev.models.Sponsor;
import com.pidev.utils.hackthon.ReportGenerator;
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
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.ResourceBundle;

public class SponsorAdminController implements Initializable {

    @FXML private ListView<Sponsor> sponsorListView;
    @FXML private TextField searchField;

    private ServiceSponsor serviceSponsor = new ServiceSponsor();
    private ObservableList<Sponsor> sponsorList = FXCollections.observableArrayList();
    private FilteredList<Sponsor> filteredData;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMM dd, yyyy • HH:mm");

    /**
     * Initialise le contrôleur, configure la liste, charge les données et configure la recherche.
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupListView();
        loadData();
        setupSearch();
    }

    /**
     * Exporte la liste des sponsors vers un fichier Excel.
     */
    @FXML
    private void exportExcel() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Enregistrer l'export des sponsors");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichiers Excel", "*.xlsx"));
        fileChooser.setInitialFileName("rapport_sponsors.xlsx");
        
        File file = fileChooser.showSaveDialog(sponsorListView.getScene().getWindow());
        if (file != null) {
            try {
                ReportGenerator.exportSponsorsToExcel(sponsorList, file.getAbsolutePath());
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Exportation réussie");
                alert.setHeaderText(null);
                alert.setContentText("La liste des sponsors a été exportée vers Excel.");
                alert.showAndWait();
            } catch (Exception e) {
                e.printStackTrace();
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setContentText("Erreur lors de l'exportation Excel : " + e.getMessage());
                alert.showAndWait();
            }
        }
    }

    /**
     * Configure la ListView pour utiliser des cellules personnalisées (Cartes).
     */
    private void setupListView() {
        sponsorListView.setCellFactory(listView -> new SponsorListCell());
        sponsorListView.setFixedCellSize(Region.USE_COMPUTED_SIZE);
    }

    /**
     * Charge tous les sponsors depuis la base de données vers la liste observable.
     */
    private void loadData() {
        sponsorList.setAll(serviceSponsor.getAll());
        if (filteredData != null) {
            sponsorListView.setItems(filteredData);
        } else {
            sponsorListView.setItems(sponsorList);
        }
    }

    /**
     * Configure la recherche dynamique pour filtrer les sponsors par nom.
     */
    private void setupSearch() {
        filteredData = new FilteredList<>(sponsorList, p -> true);
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(s -> {
                if (newValue == null || newValue.isEmpty()) return true;
                String lowerCaseFilter = newValue.toLowerCase();
                return s.getName().toLowerCase().contains(lowerCaseFilter);
            });
        });
        sponsorListView.setItems(filteredData);
    }

    /**
     * Affiche le formulaire pour ajouter un nouveau sponsor.
     */
    @FXML
    private void showAddForm() {
        loadForm(null);
    }

    /**
     * Affiche le formulaire pour modifier un sponsor existant.
     */
    private void showEditForm(Sponsor s) {
        loadForm(s);
    }

    /**
     * Charge le fichier FXML du formulaire et l'injecte dans la zone de contenu principale.
     */
    private void loadForm(Sponsor s) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Fxml/admin/sponsor_form.fxml"));
            Parent form = loader.load();
            SponsorFormController controller = loader.getController();
            controller.setSponsor(s);

            StackPane contentArea = (StackPane) sponsorListView.getScene().lookup("#contentArea");
            contentArea.getChildren().setAll(form);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Gère la suppression d'un sponsor avec une confirmation de l'utilisateur.
     */
    private void handleDelete(Sponsor s) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Supprimer le sponsor");
        alert.setHeaderText("Êtes-vous sûr de vouloir supprimer ce sponsor ?");
        alert.setContentText(s.getName());

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            serviceSponsor.delete(s.getId());
            loadData();
        }
    }

    /**
     * Classe interne pour définir l'apparence de chaque ligne de la liste (Format Carte).
     */
    private class SponsorListCell extends ListCell<Sponsor> {
        private final HBox card = new HBox(20);
        private final VBox infoBox = new VBox(6);
        private final Label nameLabel = new Label();
        private final Label websiteLabel = new Label();
        private final Label dateLabel = new Label();
        private final HBox actionsBox = new HBox(10);
        private final Button editBtn = new Button();
        private final Button deleteBtn = new Button();
        private final Region spacer = new Region();
        private final StackPane iconCircle = new StackPane();

        SponsorListCell() {
            // Cercle d'icône
            FontIcon userIcon = new FontIcon("fas-building");
            userIcon.setIconSize(20);
            userIcon.getStyleClass().add("list-card-icon");
            iconCircle.getChildren().add(userIcon);
            iconCircle.getStyleClass().add("list-card-icon-circle");
            iconCircle.setMinSize(48, 48);
            iconCircle.setMaxSize(48, 48);

            // Nom
            nameLabel.getStyleClass().add("list-card-title");

            // Site web
            websiteLabel.getStyleClass().add("list-card-subtitle");

            // Date
            dateLabel.getStyleClass().add("list-card-meta");

            infoBox.getChildren().addAll(nameLabel, websiteLabel, dateLabel);
            infoBox.setAlignment(Pos.CENTER_LEFT);

            // Actions d'édition
            FontIcon editIcon = new FontIcon("fas-edit");
            editIcon.setIconSize(14);
            editIcon.getStyleClass().add("action-icon-edit");
            editBtn.setGraphic(editIcon);
            editBtn.getStyleClass().add("list-action-btn-edit");
            editBtn.setText("Modifier");

            // Actions de suppression
            FontIcon trashIcon = new FontIcon("fas-trash");
            trashIcon.setIconSize(14);
            trashIcon.getStyleClass().add("action-icon-delete");
            deleteBtn.setGraphic(trashIcon);
            deleteBtn.getStyleClass().add("list-action-btn-delete");
            deleteBtn.setText("Supprimer");

            actionsBox.getChildren().addAll(editBtn, deleteBtn);
            actionsBox.setAlignment(Pos.CENTER_RIGHT);

            HBox.setHgrow(spacer, Priority.ALWAYS);

            card.getChildren().addAll(iconCircle, infoBox, spacer, actionsBox);
            card.setAlignment(Pos.CENTER_LEFT);
            card.getStyleClass().add("list-card");
            card.setPadding(new Insets(18, 24, 18, 24));
        }

        /**
         * Met à jour le contenu de la cellule avec les données d'un sponsor.
         */
        @Override
        protected void updateItem(Sponsor sponsor, boolean empty) {
            super.updateItem(sponsor, empty);
            if (empty || sponsor == null) {
                setGraphic(null);
                setText(null);
                setStyle("-fx-background-color: transparent;");
            } else {
                nameLabel.setText(sponsor.getName());
                websiteLabel.setText(sponsor.getWebsiteUrl() != null ? sponsor.getWebsiteUrl() : "Aucun site web");
                dateLabel.setText(sponsor.getCreatedAt() != null ? DATE_FMT.format(sponsor.getCreatedAt()) : "—");

                editBtn.setOnAction(e -> showEditForm(sponsor));
                deleteBtn.setOnAction(e -> handleDelete(sponsor));

                setGraphic(card);
                setText(null);
                setStyle("-fx-background-color: transparent; -fx-padding: 4 0 4 0;");
            }
        }
    }
}
