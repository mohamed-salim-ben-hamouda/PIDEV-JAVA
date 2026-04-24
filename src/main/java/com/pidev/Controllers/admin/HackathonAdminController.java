package com.pidev.Controllers.admin;

import com.pidev.Services.ServiceHackathon;
import com.pidev.models.Hackathon;
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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Rectangle;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.IOException;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.ResourceBundle;

public class HackathonAdminController implements Initializable {

    @FXML private ListView<Hackathon> hackathonListView;
    @FXML private TextField searchField;

    private ServiceHackathon serviceHackathon = new ServiceHackathon();
    private ObservableList<Hackathon> hackathonList = FXCollections.observableArrayList();
    private FilteredList<Hackathon> filteredData;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    /**
     * Initialise le contrôleur, configure la liste des hackathons et active la recherche.
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupListView();
        loadData();
        setupSearch();
    }

    /**
     * Configure la ListView pour utiliser des cellules de type "Carte" personnalisées.
     */
    private void setupListView() {
        hackathonListView.setCellFactory(listView -> new HackathonListCell());
        hackathonListView.setFixedCellSize(Region.USE_COMPUTED_SIZE);
    }

    /**
     * Charge tous les hackathons depuis la base de données.
     */
    private void loadData() {
        hackathonList.setAll(serviceHackathon.getAll());
        if (filteredData != null) {
            hackathonListView.setItems(filteredData);
        } else {
            hackathonListView.setItems(hackathonList);
        }
    }

    /**
     * Configure le filtre de recherche dynamique pour le titre et le thème.
     */
    private void setupSearch() {
        filteredData = new FilteredList<>(hackathonList, p -> true);
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(h -> {
                if (newValue == null || newValue.isEmpty()) return true;
                String lowerCaseFilter = newValue.toLowerCase();
                return h.getTitle().toLowerCase().contains(lowerCaseFilter) ||
                       h.getTheme().toLowerCase().contains(lowerCaseFilter);
            });
        });
        hackathonListView.setItems(filteredData);
    }

    /**
     * Affiche le formulaire pour créer un nouveau hackathon.
     */
    @FXML
    private void showAddForm() {
        loadForm(null);
    }

    /**
     * Affiche le formulaire pour modifier un hackathon existant.
     */
    private void showEditForm(Hackathon h) {
        loadForm(h);
    }

    /**
     * Charge le fichier FXML du formulaire et l'injecte dans l'interface.
     */
    private void loadForm(Hackathon h) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Fxml/admin/hackathon_form.fxml"));
            Parent form = loader.load();
            HackathonFormController controller = loader.getController();
            controller.setHackathon(h);

            StackPane contentArea = (StackPane) hackathonListView.getScene().lookup("#contentArea");
            contentArea.getChildren().setAll(form);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Gère la suppression d'un hackathon après confirmation de l'utilisateur.
     */
    private void handleDelete(Hackathon h) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Supprimer le Hackathon");
        alert.setHeaderText("Êtes-vous sûr de vouloir supprimer ce hackathon ?");
        alert.setContentText(h.getTitle());

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            serviceHackathon.delete(h.getId());
            loadData();
        }
    }

    /**
     * Actualise les données affichées dans la liste.
     */
    public void refreshTable() {
        loadData();
    }

    /**
     * Classe interne personnalisée pour définir le design de chaque élément de la liste (Format Carte).
     */
    private class HackathonListCell extends ListCell<Hackathon> {
        private final HBox card = new HBox(20);
        private final VBox infoBox = new VBox(6);
        private final Label titleLabel = new Label();
        private final Label themeLabel = new Label();
        private final Label detailsLabel = new Label();
        private final HBox actionsBox = new HBox(10);
        private final Button editBtn = new Button();
        private final Button deleteBtn = new Button();
        private final Region spacer = new Region();
        private final Rectangle imageRect = new Rectangle(64, 64);
        private final Label statusBadge = new Label();

        HackathonListCell() {
            // Rectangle avec coins arrondis pour l'image
            imageRect.setArcWidth(20);
            imageRect.setArcHeight(20);
            imageRect.setStroke(javafx.scene.paint.Color.web("#e2e8f0"));
            imageRect.setStrokeWidth(1);

            // Titre et Thème
            titleLabel.getStyleClass().add("list-card-title");
            themeLabel.getStyleClass().add("list-card-subtitle");
            detailsLabel.getStyleClass().add("list-card-meta");

            // Badge de statut
            statusBadge.getStyleClass().add("status-badge");
            
            HBox titleRow = new HBox(10, titleLabel, statusBadge);
            titleRow.setAlignment(Pos.CENTER_LEFT);

            infoBox.getChildren().addAll(titleRow, themeLabel, detailsLabel);
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
            card.getChildren().addAll(imageRect, infoBox, spacer, actionsBox);
            card.setAlignment(Pos.CENTER_LEFT);
            card.getStyleClass().add("list-card");
            card.setPadding(new Insets(18, 24, 18, 24));
        }

        /**
         * Met à jour l'affichage d'un hackathon spécifique dans sa cellule.
         */
        @Override
        protected void updateItem(Hackathon hackathon, boolean empty) {
            super.updateItem(hackathon, empty);
            if (empty || hackathon == null) {
                setGraphic(null);
                setText(null);
                setStyle("-fx-background-color: transparent;");
            } else {
                titleLabel.setText(hackathon.getTitle());
                themeLabel.setText(hackathon.getTheme());
                
                // Chargement de l'image avec fallback robuste
                Image img = null;
                try {
                    if (hackathon.getCoverUrl() != null && !hackathon.getCoverUrl().isEmpty()) {
                        img = new Image(hackathon.getCoverUrl(), true); // Background loading
                    } else {
                        img = new Image(getClass().getResource("/images/home_pic.jpg").toExternalForm());
                    }
                } catch (Exception e) {
                    // Si le chargement échoue, on utilise le placeholder local
                    img = new Image(getClass().getResource("/images/home_pic.jpg").toExternalForm());
                }

                // Utilisation d'un ImageView dans un StackPane pour le centrage
                ImageView iv = new ImageView(img);
                iv.setFitWidth(64);
                iv.setFitHeight(64);
                iv.setPreserveRatio(true);
                
                // Centrage forcé dans le conteneur
                StackPane imgContainer = new StackPane(iv);
                imgContainer.setMinSize(64, 64);
                imgContainer.setMaxSize(64, 64);
                imgContainer.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 12; -fx-overflow: hidden;");
                
                // Clip pour arrondir les coins de l'image elle-même
                Rectangle clip = new Rectangle(64, 64);
                clip.setArcWidth(20);
                clip.setArcHeight(20);
                imgContainer.setClip(clip);

                String dateStr = hackathon.getStartAt() != null ? DATE_FMT.format(hackathon.getStartAt()) : "À définir";
                detailsLabel.setText(hackathon.getLocation() + " • Début : " + dateStr);

                statusBadge.setText(hackathon.getStatus() != null ? hackathon.getStatus().toUpperCase() : "BROUILLON");
                statusBadge.setStyle("-fx-background-color: " + getStatusColor(hackathon.getStatus()) + "; -fx-text-fill: white; -fx-padding: 2 8; -fx-background-radius: 10; -fx-font-size: 10px; -fx-font-weight: bold;");

                editBtn.setOnAction(e -> showEditForm(hackathon));
                deleteBtn.setOnAction(e -> handleDelete(hackathon));

                card.getChildren().set(0, imgContainer); // Replace first element (the image)
                
                setGraphic(card);
                setText(null);
                setStyle("-fx-background-color: transparent; -fx-padding: 4 0 4 0;");
            }
        }

        /**
         * Retourne la couleur hexadécimale associée à chaque statut de hackathon.
         */
        private String getStatusColor(String status) {
            if (status == null) return "#94a3b8";
            switch (status.toLowerCase()) {
                case "active":
                case "ongoing": return "#10b981";
                case "upcoming": return "#3d68b2";
                case "ended":
                case "completed": return "#64748b";
                default: return "#94a3b8";
            }
        }
    }
}
