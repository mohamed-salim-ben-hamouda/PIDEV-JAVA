package com.pidev.Controllers.client;

import com.pidev.models.*;
import com.pidev.Services.CVService;
import com.pidev.Services.AIService;
import com.pidev.Services.PdfService;
import com.pidev.utils.SessionManager;
import javafx.event.ActionEvent;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.paint.ImagePattern;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.MouseButton;
import javafx.scene.shape.Circle;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import java.io.File;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

import javafx.scene.control.CheckBox;
import javafx.scene.control.DatePicker;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import java.io.IOException;
import java.time.LocalDate;

public class MyCVController implements Initializable {
    private static final DateTimeFormatter CARD_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML
    private VBox listPage;
    @FXML
    private VBox formPage;
    @FXML
    private VBox previewPage;
    @FXML
    private VBox aiPage;
    @FXML
    private FlowPane cardsContainer;

    // AI Fields
    @FXML
    private ComboBox<String> aiLangueComboBox;
    @FXML
    private TextField aiJobTitleField;
    @FXML
    private TextArea aiNotesArea;
    @FXML
    private CheckBox aiGenSummary;
    @FXML
    private CheckBox aiGenExp;
    @FXML
    private CheckBox aiGenEdu;
    @FXML
    private CheckBox aiGenSkills;
    @FXML
    private CheckBox aiGenLang;
    @FXML
    private CheckBox aiGenCertifs;

    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> filterLangueComboBox;
    @FXML
    private Label pageSubtitleLabel;
    @FXML
    private Label formTitleLabel;
    @FXML
    private Label formSubtitleLabel;
    @FXML
    private Button submitButton;
    @FXML
    private Button clearButton;

    // Preview Fields
    @FXML private Button atsButton;
    @FXML private VBox templatePreviewContainer;
    @FXML private Label previewNomLabel;
    @FXML private Label previewLangueLabel;
    @FXML private Label previewSummaryLabel;
    @FXML private Label previewLinkedinLabel;
    @FXML private VBox previewExpBox;
    @FXML private VBox previewEduBox;
    @FXML private VBox previewSkillBox;
    @FXML private VBox previewCertBox;
    @FXML private VBox previewLangBox;

    @FXML
    private TextField nomCvField;
    @FXML
    private ComboBox<String> langueComboBox;
    @FXML
    private ComboBox<Integer> templateComboBox;
    @FXML
    private HBox templateGallery;
    @FXML
    private TextField linkedinUrlField;
    @FXML
    private TextArea summaryArea;

    private String currentPhotoUrl;

    @FXML
    private VBox experiencesContainer;
    @FXML
    private VBox educationContainer;
    @FXML
    private VBox skillsContainer;
    @FXML
    private VBox certifsContainer;
    @FXML
    private VBox languagesContainer;

    @FXML
    private Button addExperienceBtn;
    @FXML
    private Button addEducationBtn;
    @FXML
    private Button addSkillBtn;
    @FXML
    private Button addCertifBtn;
    @FXML
    private Button addLanguageBtn;

    private final CVService cvService = new CVService();
    private final AIService aiService = new AIService();
    private final PdfService pdfService = new PdfService();
    private List<Cv> allCvs = new ArrayList<>();
    private Cv selectedCv;
    private boolean readOnlyMode;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        langueComboBox.getItems().setAll("Français", "Anglais", "Arabe", "Allemand");
        aiLangueComboBox.getItems().setAll("Français", "Anglais", "Arabe", "Allemand");
        aiLangueComboBox.setValue("Français");
        templateComboBox.getItems().setAll(1, 2, 3); // Example templates
        filterLangueComboBox.getItems().setAll("Toutes les langues", "Français", "Anglais", "Arabe", "Allemand");
        filterLangueComboBox.setValue("Toutes les langues");

        searchField.textProperty().addListener((observable, oldValue, newValue) -> refreshCards());
        filterLangueComboBox.valueProperty().addListener((observable, oldValue, newValue) -> refreshCards());

        nomCvField.textProperty().addListener((observable, oldValue, newValue) -> updateRequiredFieldHighlights());
        langueComboBox.valueProperty().addListener((observable, oldValue, newValue) -> updateRequiredFieldHighlights());

        setFormEditable(true);
        showListPage();
        loadCvs();
        updateRequiredFieldHighlights();
        populateTemplateGallery();
    }

    private void populateTemplateGallery() {
        templateGallery.getChildren().clear();
        for (int i = 1; i <= 3; i++) {
            final int templateId = i;
            VBox card = new VBox(10);
            card.getStyleClass().add("template-preview-card");
            card.setAlignment(Pos.CENTER);
            card.setPrefSize(130, 180);

            // Image Preview (ImageView instead of styled VBox)
            ImageView imgPreview = new ImageView();
            try {
                String imgPath = "/images/templates/template" + i + ".png";
                Image image = new Image(getClass().getResourceAsStream(imgPath));
                imgPreview.setImage(image);
            } catch (Exception e) {
                System.err.println("Could not load template image: " + e.getMessage());
                // Fallback style if image missing
                imgPreview.getStyleClass().add("template-img-placeholder");
            }

            imgPreview.setFitWidth(110);
            imgPreview.setFitHeight(140);
            imgPreview.setPreserveRatio(true);
            imgPreview.getStyleClass().add("template-img-preview");

            Label name = new Label("Modèle " + i);
            name.getStyleClass().add("template-name");

            card.getChildren().addAll(imgPreview, name);
            card.setOnMouseClicked(e -> selectTemplate(templateId));

            // Highlight if selected
            if (templateComboBox.getValue() != null && templateComboBox.getValue() == templateId) {
                card.getStyleClass().add("selected");
            }

            templateGallery.getChildren().add(card);
        }
    }

    private void selectTemplate(int id) {
        templateComboBox.setValue(id);
        populateTemplateGallery(); // Refresh highlights
    }

    @FXML
    private void handleUploadPhoto() {
        if (selectedCv == null) {
            showError("Erreur", "Veuillez d'abord sélectionner ou créer un CV.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une photo de profil");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg")
        );

        File selectedFile = fileChooser.showOpenDialog(listPage.getScene().getWindow());
        if (selectedFile != null) {
            try {
                // Store the absolute path for reliability
                String photoPath = selectedFile.getAbsolutePath();
                selectedCv.setPhotoUrl(photoPath);

                // Save to database
                cvService.modifier(selectedCv);

                // Refresh preview immediately
                handleViewCv(selectedCv);

            } catch (Exception e) {
                e.printStackTrace();
                showError("Erreur", "Impossible de sauvegarder la photo : " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleRemovePhoto() {
        if (selectedCv == null) return;

        selectedCv.setPhotoUrl(null);
        try {
            cvService.modifier(selectedCv);
            handleViewCv(selectedCv);
        } catch (SQLException e) {
            showError("Erreur", "Impossible de supprimer l'image : " + e.getMessage());
        }
    }

    @FXML
    private void addExperienceRow() {
        experiencesContainer.getChildren().add(createExperienceRow(null));
    }

    @FXML
    private void addEducationRow() {
        educationContainer.getChildren().add(createEducationRow(null));
    }

    @FXML
    private void addSkillRow() {
        skillsContainer.getChildren().add(createSkillRow(null));
    }

    @FXML
    private void addCertifRow() {
        certifsContainer.getChildren().add(createCertifRow(null));
    }

    @FXML
    private void addLanguageRow() {
        languagesContainer.getChildren().add(createLanguageRow(null));
    }

    private VBox createExperienceRow(Experience exp) {
        VBox row = new VBox(10);
        row.getStyleClass().add("cv-item-card");
        row.setPadding(new Insets(15));

        HBox topRow = new HBox(10);
        TextField titleField = createTextField("Titre du poste", exp != null ? exp.getJobTitle() : null);
        TextField companyField = createTextField("Entreprise", exp != null ? exp.getCompany() : null);
        TextField locationField = createTextField("Lieu", exp != null ? exp.getLocation() : null);
        topRow.getChildren().addAll(titleField, companyField, locationField);

        HBox dateRow = new HBox(10);
        DatePicker startPicker = createDatePicker(exp != null ? exp.getStartDate() : null);
        DatePicker endPicker = createDatePicker(exp != null ? exp.getEndDate() : null);
        CheckBox currentCheck = new CheckBox("J'y travaille actuellement");
        if (exp != null) currentCheck.setSelected(exp.getCurrentlyWorking() != null && exp.getCurrentlyWorking());
        endPicker.disableProperty().bind(currentCheck.selectedProperty());
        dateRow.getChildren().addAll(new Label("Début:"), startPicker, new Label("Fin:"), endPicker, currentCheck);

        TextArea descArea = new TextArea(exp != null ? exp.getDescription() : "");
        descArea.setPromptText("Description...");
        descArea.setPrefHeight(60);
        descArea.setWrapText(true);

        Button removeBtn = createRemoveBtn(row, experiencesContainer);
        HBox actions = new HBox(removeBtn);
        actions.setAlignment(javafx.geometry.Pos.TOP_RIGHT);

        row.getChildren().addAll(actions, topRow, dateRow, descArea);
        return row;
    }

    private VBox createEducationRow(Education edu) {
        VBox row = new VBox(10);
        row.getStyleClass().add("cv-item-card");
        row.setPadding(new Insets(15));

        HBox topRow = new HBox(10);
        TextField degreeField = createTextField("Diplôme", edu != null ? edu.getDegree() : null);
        TextField studyField = createTextField("Domaine d'étude", edu != null ? edu.getFieldOfStudy() : null);
        topRow.getChildren().addAll(degreeField, studyField);

        HBox midRow = new HBox(10);
        TextField schoolField = createTextField("Établissement", edu != null ? edu.getSchool() : null);
        TextField cityField = createTextField("Ville", edu != null ? edu.getCity() : null);
        midRow.getChildren().addAll(schoolField, cityField);

        HBox dateRow = new HBox(10);
        DatePicker startPicker = createDatePicker(edu != null ? edu.getStartDate() : null);
        DatePicker endPicker = createDatePicker(edu != null ? edu.getEndDate() : null);
        dateRow.getChildren().addAll(new Label("Début:"), startPicker, new Label("Fin:"), endPicker);

        TextArea descArea = new TextArea(edu != null ? edu.getDescription() : "");
        descArea.setPromptText("Description...");
        descArea.setPrefHeight(60);
        descArea.setWrapText(true);

        Button removeBtn = createRemoveBtn(row, educationContainer);
        HBox actions = new HBox(removeBtn);
        actions.setAlignment(javafx.geometry.Pos.TOP_RIGHT);

        row.getChildren().addAll(actions, topRow, midRow, dateRow, descArea);
        return row;
    }

    private VBox createSkillRow(Skill skill) {
        VBox row = new VBox(10);
        row.getStyleClass().add("cv-item-card");
        row.setPadding(new Insets(10));

        HBox content = new HBox(10);
        TextField nameField = createTextField("Compétence", skill != null ? skill.getNom() : null);
        TextField typeField = createTextField("Type (ex: Hard Skill)", skill != null ? skill.getType() : null);
        TextField levelField = createTextField("Niveau", skill != null ? skill.getLevel() : null);
        Button removeBtn = createRemoveBtn(row, skillsContainer);
        content.getChildren().addAll(nameField, typeField, levelField, removeBtn);

        row.getChildren().add(content);
        return row;
    }

    private VBox createCertifRow(Certif cert) {
        VBox row = new VBox(10);
        row.getStyleClass().add("cv-item-card");
        row.setPadding(new Insets(10));

        HBox top = new HBox(10);
        TextField nameField = createTextField("Certification", cert != null ? cert.getName() : null);
        TextField issuerField = createTextField("Organisme", cert != null ? cert.getIssuedBy() : null);
        top.getChildren().addAll(nameField, issuerField);

        HBox dates = new HBox(10);
        DatePicker issuePicker = createDatePicker(cert != null ? cert.getIssueDate() : null);
        DatePicker expPicker = createDatePicker(cert != null ? cert.getExpDate() : null);
        dates.getChildren().addAll(new Label("Date d'obtention:"), issuePicker, new Label("Expiration:"), expPicker);

        Button removeBtn = createRemoveBtn(row, certifsContainer);
        HBox actions = new HBox(removeBtn);
        actions.setAlignment(javafx.geometry.Pos.TOP_RIGHT);

        row.getChildren().addAll(actions, top, dates);
        return row;
    }

    private VBox createLanguageRow(Langue lang) {
        VBox row = new VBox(10);
        row.getStyleClass().add("cv-item-card");
        row.setPadding(new Insets(10));

        HBox content = new HBox(10);
        TextField nameField = createTextField("Langue", lang != null ? lang.getNom() : null);
        TextField levelField = createTextField("Niveau", lang != null ? lang.getNiveau() : null);
        Button removeBtn = createRemoveBtn(row, languagesContainer);
        content.getChildren().addAll(nameField, levelField, removeBtn);

        row.getChildren().add(content);
        return row;
    }

    private TextField createTextField(String prompt, String value) {
        TextField tf = new TextField(value != null ? value : "");
        tf.setPromptText(prompt);
        tf.getStyleClass().add("cv-form-field");
        HBox.setHgrow(tf, Priority.ALWAYS);
        return tf;
    }

    private DatePicker createDatePicker(LocalDate date) {
        DatePicker dp = new DatePicker(date);
        dp.getStyleClass().add("cv-form-field");
        return dp;
    }

    private Button createRemoveBtn(VBox row, VBox container) {
        Button btn = new Button("✕");
        btn.getStyleClass().add("cv-remove-btn");
        btn.setOnAction(e -> container.getChildren().remove(row));
        return btn;
    }

    private void clearContainers() {
        experiencesContainer.getChildren().clear();
        educationContainer.getChildren().clear();
        skillsContainer.getChildren().clear();
        certifsContainer.getChildren().clear();
        languagesContainer.getChildren().clear();
    }

    @FXML
    private void handleShowCreateForm() {
        selectedCv = null;
        clearForm();
        setFormEditable(true);
        formTitleLabel.setText("Créer un nouveau CV");
        formSubtitleLabel.setText("Remplissez les informations principales et mettez en avant les champs essentiels.");
        submitButton.setText("Créer le CV");
        showFormPage();
    }

    @FXML
    private void handleSubmitCv() {
        try {
            validateRequiredFields();

            Cv cvData = buildCvFromForm(selectedCv);

            if (selectedCv == null) {
                // ADD NEW CV
                cvData.setCreationDate(LocalDateTime.now());
                cvData.setUpdatedAt(LocalDateTime.now());
                cvService.ajouter(cvData);
                showInfo("Succès", "Le CV a été ajouté avec succès.");
            } else {
                // UPDATE EXISTING CV
                cvData.setId(selectedCv.getId());
                cvData.setCreationDate(selectedCv.getCreationDate() != null ? selectedCv.getCreationDate() : LocalDateTime.now());
                cvData.setUpdatedAt(LocalDateTime.now());

                cvService.modifier(cvData);
                showInfo("Succès", "Le CV a été modifié avec succès.");
            }

            // Reset state and return to list
            selectedCv = null;
            loadCvs();
            clearForm();
            showListPage();
        } catch (Exception exception) {
            exception.printStackTrace();
            showError("Erreur lors de l'enregistrement", exception.getMessage());
        }
    }

    @FXML
    private void handleBackToList() {
        clearForm();
        setFormEditable(true);
        loadCvs();
        showListPage();
    }

    @FXML
    private void handleRefreshCv() {
        loadCvs();
    }

    @FXML
    private void handleClearForm() {
        if (readOnlyMode) {
            handleBackToList();
            return;
        }
        clearForm();
    }

    private void loadCvs() {
        try {
            User currentUser = SessionManager.getInstance().getUser();
            if (currentUser != null) {
                allCvs = cvService.afficherParUtilisateur(currentUser.getId());
            } else {
                allCvs = new ArrayList<>(); // Show nothing if not logged in
            }
            refreshCards();
        } catch (SQLException exception) {
            showError("Chargement impossible", exception.getMessage());
        }
    }

    private void refreshCards() {
        cardsContainer.getChildren().clear();

        String searchValue = normalize(searchField.getText()).toLowerCase();
        String selectedLangue = filterLangueComboBox.getValue();

        List<Cv> filteredCvs = allCvs.stream()
                .filter(cv -> matchesSearch(cv, searchValue))
                .filter(cv -> matchesLangue(cv, selectedLangue))
                .toList();

        pageSubtitleLabel.setText(filteredCvs.isEmpty()
                ? "Aucun CV ne correspond à votre recherche."
                : "Gérez et créez vos curriculum vitae.");

        for (Cv cv : filteredCvs) {
            cardsContainer.getChildren().add(createCvCard(cv));
        }

        if (filteredCvs.isEmpty()) {
            VBox emptyCard = new VBox(8);
            emptyCard.getStyleClass().add("cv-empty-card");
            emptyCard.getChildren().addAll(
                    createTextLabel("Aucun CV trouvé", "cv-empty-title"),
                    createTextLabel("Essayez un autre mot-clé ou ajoutez un nouveau CV.", "cv-empty-text")
            );
            cardsContainer.getChildren().add(emptyCard);
        }
    }

    private VBox createCvCard(Cv cv) {
        VBox card = new VBox(14);
        card.getStyleClass().add("cv-card");

        HBox topRow = new HBox(12);
        VBox titleBox = new VBox(4);
        Label title = createTextLabel(safe(cv.getNomCv()), "cv-card-title");
        Label dates = createTextLabel("Créé le " + formatCardDate(cv.getCreationDate()) + " • Mis à jour le " + formatCardDate(cv.getUpdatedAt()), "cv-card-meta");
        titleBox.getChildren().add(title);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label languageBadge = createTextLabel(safe(cv.getLangue()), "cv-badge");
        topRow.getChildren().addAll(titleBox, spacer, languageBadge);

        String userDisplayName = "Utilisateur Inconnu";
        if (cv.getUser() != null) {
            if (cv.getUser().getPrenom() != null && cv.getUser().getNom() != null) {
                userDisplayName = cv.getUser().getPrenom() + " " + cv.getUser().getNom();
            } else if (cv.getUser().getId() != null) {
                userDisplayName = "Utilisateur #" + cv.getUser().getId();
            }
        }

        HBox highlights = new HBox(8);
        highlights.getChildren().addAll(
                createTextLabel(userDisplayName, "cv-soft-badge"),
                createTextLabel("Template " + (cv.getIdTemplate() != null ? cv.getIdTemplate() : "auto"), "cv-soft-badge"),
                createTextLabel((cv.getProgression() != null ? cv.getProgression() : 0) + "% complété", "cv-soft-badge")
        );

        ProgressBar progressBar = new ProgressBar(Math.max(0, Math.min(1, (cv.getProgression() != null ? cv.getProgression() : 0) / 100.0)));
        progressBar.getStyleClass().add("cv-progress-bar");
        progressBar.setMaxWidth(Double.MAX_VALUE);

        Label progressLabel = createTextLabel("Progression : " + (cv.getProgression() != null ? cv.getProgression() : 0) + "%", "cv-progress-text");
        Label summaryLabel = createTextLabel(
                cv.getSummary() != null && !cv.getSummary().isBlank() ? cv.getSummary() : "Aucun résumé disponible pour ce CV.",
                "cv-card-summary"
        );
        summaryLabel.setWrapText(true);
        Label linkedInLabel = createTextLabel(
                cv.getLinkedinUrl() != null && !cv.getLinkedinUrl().isBlank() ? "LinkedIn renseigné" : "LinkedIn non renseigné",
                "cv-card-meta"
        );

        HBox actions = new HBox(10);
        Button viewButton = createActionButton("Voir", "cv-view-btn");
        viewButton.setOnAction(event -> handleViewCv(cv));
        Button editButton = createActionButton("Modifier", "cv-edit-btn");
        editButton.setOnAction(event -> handleEditCv(cv));
        Button deleteButton = createActionButton("Supprimer", "cv-delete-btn");
        deleteButton.setOnAction(event -> handleDeleteCv(cv));
        HBox.setHgrow(viewButton, Priority.ALWAYS);
        HBox.setHgrow(editButton, Priority.ALWAYS);
        HBox.setHgrow(deleteButton, Priority.ALWAYS);
        viewButton.setMaxWidth(Double.MAX_VALUE);
        editButton.setMaxWidth(Double.MAX_VALUE);
        deleteButton.setMaxWidth(Double.MAX_VALUE);
        actions.getChildren().addAll(viewButton, editButton, deleteButton);

        VBox.setMargin(actions, new Insets(4, 0, 0, 0));
        card.getChildren().addAll(topRow, dates, highlights, progressBar, progressLabel, summaryLabel, linkedInLabel, actions);
        return card;
    }

    private void handleViewCv(Cv cv) {
        selectedCv = cv;
        int templateId = (cv.getIdTemplate() != null) ? cv.getIdTemplate() : 1;

        try {
            String fxmlPath = "/Fxml/client/templates/Template" + templateId + ".fxml";
            URL fxmlLocation = getClass().getResource(fxmlPath);
            if (fxmlLocation == null) {
                showError("Erreur", "Template introuvable: " + fxmlPath);
                return;
            }

            FXMLLoader loader = new FXMLLoader(fxmlLocation);
            Node templateRoot = loader.load();

            // Critical: Refresh data from DB to ensure we have the latest photo URL
            Cv latestCv = cvService.getById(cv.getId());
            if (latestCv != null) {
                selectedCv = latestCv;
            }

            templatePreviewContainer.getChildren().clear();
            templatePreviewContainer.getChildren().add(templateRoot);

            // Add PDF button at the bottom of the preview
            Button pdfBtn = new Button("Exporter en PDF");
            pdfBtn.getStyleClass().add("action-button-primary"); // Assuming this class exists or similar
            pdfBtn.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20; -fx-background-radius: 5; -fx-cursor: hand;");
            pdfBtn.setOnAction(e -> handleDownloadPdf(selectedCv));

            VBox buttonContainer = new VBox(pdfBtn);
            buttonContainer.setAlignment(Pos.CENTER);
            buttonContainer.setPadding(new Insets(20));
            templatePreviewContainer.getChildren().add(buttonContainer);

            // Populate Template Data using lookups
            populateTemplateData(templateRoot, selectedCv);

            // Update Preview Header Labels
            if (selectedCv.getUser() != null) {
                previewNomLabel.setText((selectedCv.getUser().getPrenom() + " " + selectedCv.getUser().getNom()).toUpperCase());
            }
            previewLangueLabel.setText(selectedCv.getLangue() != null ? selectedCv.getLangue() : "Langue non spécifiée");

            showPreviewPage();
        } catch (Exception e) {
            e.printStackTrace();
            showError("Erreur", "Impossible de charger le template : " + e.getMessage());
        }
    }

    private void populateTemplateData(Node root, Cv cv) {
        // Profile Image
        Node profileNode = root.lookup("#profileCircle");
        if (profileNode instanceof Circle) {
            Circle circle = (Circle) profileNode;
            String photoPath = cv.getPhotoUrl();

            // Find upload icon label (if present in template)
            Node uploadIcon = root.lookup("#uploadIcon");

            // Fallback to user photo if CV photo is null
            if (photoPath == null && cv.getUser() != null) {
                photoPath = cv.getUser().getPhoto();
            }

            if (photoPath != null && !photoPath.isBlank()) {
                try {
                    // Check if path is valid and accessible
                    String finalPath;
                    if (!photoPath.startsWith("http") && !photoPath.startsWith("file:")) {
                        File file = new File(photoPath);
                        if (file.exists()) {
                            finalPath = file.toURI().toString();
                        } else {
                            finalPath = "file:" + photoPath;
                        }
                    } else {
                        finalPath = photoPath;
                    }

                    // Load image synchronously
                    Image img = new Image(finalPath, false);

                    if (img.isError()) {
                        System.err.println("Error loading image from: " + finalPath);
                        if (uploadIcon != null) uploadIcon.setVisible(true);
                        circle.setFill(javafx.scene.paint.Color.web("#f1f5f9"));
                    } else {
                        // Use ImagePattern with proportional=true to FILL and CENTER automatically
                        circle.setFill(new javafx.scene.paint.ImagePattern(img, 0, 0, 1, 1, true));

                        // Remove any style that might interfere with setFill
                        circle.setStyle("");

                        if (uploadIcon != null) uploadIcon.setVisible(false);
                    }
                } catch (Exception e) {
                    System.err.println("Failed to load profile image: " + e.getMessage());
                    if (uploadIcon != null) uploadIcon.setVisible(true);
                    circle.setFill(javafx.scene.paint.Color.web("#f1f5f9"));
                }
            } else {
                if (uploadIcon != null) uploadIcon.setVisible(true);
                circle.setFill(javafx.scene.paint.Color.web("#f1f5f9"));
                circle.setStyle("");
            }

            // Make circle clickable for upload (if not in readOnlyMode)
            if (!readOnlyMode) {
                circle.setCursor(javafx.scene.Cursor.HAND);

                // Click to upload
                circle.setOnMouseClicked(event -> {
                    if (event.getButton() == MouseButton.PRIMARY) {
                        handleUploadPhoto();
                    }
                });

                // Context menu to remove
                ContextMenu contextMenu = new ContextMenu();
                MenuItem uploadItem = new MenuItem("📷 Changer la photo");
                uploadItem.setOnAction(e -> handleUploadPhoto());
                MenuItem removeItem = new MenuItem("🗑️ Supprimer la photo");
                removeItem.setOnAction(e -> handleRemovePhoto());
                contextMenu.getItems().addAll(uploadItem, removeItem);

                circle.setOnContextMenuRequested(event ->
                        contextMenu.show(circle, event.getScreenX(), event.getScreenY())
                );
            }
        }

        // Basic Info
        String candidateName = "NOM DU CANDIDAT";
        if (cv.getUser() != null && cv.getUser().getNom() != null) {
            candidateName = cv.getUser().getPrenom() + " " + cv.getUser().getNom();
        }

        String jobTitle = "PROFESSIONNEL";
        if (cv.getNomCv() != null) {
            if (cv.getNomCv().startsWith("CV IA - ")) {
                jobTitle = cv.getNomCv().replace("CV IA - ", "");
            } else {
                jobTitle = cv.getNomCv();
            }
        } else if (cv.getExperiences() != null && !cv.getExperiences().isEmpty()) {
            jobTitle = cv.getExperiences().get(0).getJobTitle();
        }

        setLabelText(root, "#nameLabel", candidateName.toUpperCase());
        setLabelText(root, "#titleLabel", jobTitle.toUpperCase());
        setLabelText(root, "#emailLabel", cv.getUser() != null ? cv.getUser().getEmail() : "");

        // Handle LinkedIn specifically
        Node linkedinNode = root.lookup("#linkedinLabel");
        if (linkedinNode instanceof Label) {
            Label linkedinLabel = (Label) linkedinNode;
            if (cv.getLinkedinUrl() != null && !cv.getLinkedinUrl().isBlank()) {
                linkedinLabel.setText(cv.getLinkedinUrl());
                linkedinLabel.setVisible(true);
                linkedinLabel.setManaged(true);
            } else {
                linkedinLabel.setVisible(false);
                linkedinLabel.setManaged(false);
            }
        }

        setLabelText(root, "#summaryLabel", cv.getSummary());

        // Hide phone and location if not available (they are placeholders for now)
        hideNodeIfEmpty(root, "#phoneLabel", null);
        hideNodeIfEmpty(root, "#locationLabel", null);

        // Collections
        populateVBox(root, "#experienceBox", cv.getExperiences(), exp -> {
            VBox box = new VBox(5);
            Label title = new Label(exp.getJobTitle());
            title.getStyleClass().add("item-title");

            String dates = (exp.getStartDate() != null ? exp.getStartDate().format(CARD_DATE_FORMATTER) : "")
                    + " - " + (exp.getCurrentlyWorking() != null && exp.getCurrentlyWorking() ? "Présent" : (exp.getEndDate() != null ? exp.getEndDate().format(CARD_DATE_FORMATTER) : "Présent"));
            Label subtitle = new Label(exp.getCompany() + " | " + exp.getLocation() + " (" + dates + ")");
            subtitle.getStyleClass().add("item-subtitle");

            Label desc = new Label(exp.getDescription());
            desc.setWrapText(true);
            desc.getStyleClass().add("item-description");

            box.getChildren().addAll(title, subtitle, desc);
            return box;
        });

        populateVBox(root, "#educationBox", cv.getEducations(), edu -> {
            VBox box = new VBox(5);
            Label title = new Label(edu.getDegree() + " en " + edu.getFieldOfStudy());
            title.getStyleClass().add("item-title");

            String dates = (edu.getStartDate() != null ? edu.getStartDate().format(CARD_DATE_FORMATTER) : "")
                    + " - " + (edu.getEndDate() != null ? edu.getEndDate().format(CARD_DATE_FORMATTER) : "Présent");
            Label subtitle = new Label(edu.getSchool() + " | " + edu.getCity() + " (" + dates + ")");
            subtitle.getStyleClass().add("item-subtitle");

            box.getChildren().addAll(title, subtitle);
            return box;
        });

        populateVBox(root, "#skillsBox", cv.getSkills(), skill -> {
            Label label = new Label(skill.getNom());
            label.getStyleClass().add("skill-tag");
            return label;
        });

        populateVBox(root, "#languagesBox", cv.getLanguages(), lang -> {
            Label label = new Label(lang.getNom() + " - " + lang.getNiveau());
            label.getStyleClass().add("language-text");
            return label;
        });

        populateVBox(root, "#certificationsBox", cv.getCertifs(), cert -> {
            VBox box = new VBox(2);
            Label name = new Label(cert.getName());
            name.getStyleClass().add("item-title");

            String dates = (cert.getIssueDate() != null ? cert.getIssueDate().format(CARD_DATE_FORMATTER) : "")
                    + (cert.getExpDate() != null ? " - Expire le: " + cert.getExpDate().format(CARD_DATE_FORMATTER) : "");
            Label subtitle = new Label(cert.getIssuedBy() + " (" + dates + ")");
            subtitle.getStyleClass().add("item-subtitle");

            box.getChildren().addAll(name, subtitle);
            return box;
        });
    }

    private void hideNodeIfEmpty(Node root, String id, String value) {
        Node node = root.lookup(id);
        if (node != null) {
            if (value == null || value.isBlank()) {
                node.setVisible(false);
                node.setManaged(false);
            } else {
                if (node instanceof Label) {
                    ((Label) node).setText(value);
                }
                node.setVisible(true);
                node.setManaged(true);
            }
        }
    }

    private void setLabelText(Node root, String id, String text) {
        Node node = root.lookup(id);
        if (node instanceof Label) {
            ((Label) node).setText(text != null && !text.isBlank() ? text : "...");
        }
    }

    private <T> void populateVBox(Node root, String id, List<T> items, java.util.function.Function<T, Node> mapper) {
        Node node = root.lookup(id);
        if (node instanceof VBox && items != null) {
            VBox box = (VBox) node;
            box.getChildren().clear();
            for (T item : items) {
                box.getChildren().add(mapper.apply(item));
            }
        } else if (node instanceof HBox && items != null) {
            HBox box = (HBox) node;
            box.getChildren().clear();
            for (T item : items) {
                box.getChildren().add(mapper.apply(item));
            }
        }
    }

    @FXML
    public void handleTranslateCv() {
        if (selectedCv == null) return;

        List<String> choices = List.of("Français", "Anglais", "Allemand");
        ChoiceDialog<String> dialog = new ChoiceDialog<>("Français", choices);
        dialog.setTitle("Traduire le CV");
        dialog.setHeaderText("Traduire le CV actuel");
        dialog.setContentText("Choisissez la langue cible :");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(language -> {
            // Show a simple loading state or just start the thread
            new Thread(() -> {
                try {
                    Cv translatedData = aiService.translateCvWithAI(selectedCv, language);

                    // Update existing selectedCv with translated data
                    selectedCv.setSummary(translatedData.getSummary());
                    selectedCv.setExperiences(translatedData.getExperiences());
                    selectedCv.setEducations(translatedData.getEducations());
                    selectedCv.setSkills(translatedData.getSkills());
                    selectedCv.setLanguages(translatedData.getLanguages());
                    selectedCv.setCertifs(translatedData.getCertifs());

                    // Also update basic info
                    selectedCv.setLangue(language);
                    selectedCv.setUpdatedAt(LocalDateTime.now());

                    Platform.runLater(() -> {
                        try {
                            // Save changes to database
                            cvService.updateFullCv(selectedCv);

                            // Refresh the preview page display with the new data
                            handleViewCv(selectedCv);

                            showInfo("Succès", "Le CV a été traduit avec succès.");
                        } catch (SQLException e) {
                            showError("Erreur", "Impossible de sauvegarder la traduction : " + e.getMessage());
                        }
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> showError("Erreur de traduction", e.getMessage()));
                }
            }).start();
        });
    }

    @FXML
    public void handleShowAtsAnalysis(ActionEvent event) {
        if (selectedCv == null) {
            showError("Erreur", "Veuillez d'abord sélectionner un CV à analyser.");
            return;
        }

        try {
            URL fxmlLocation = getClass().getResource("/Fxml/client/AtsAnalysisDialog.fxml");
            if (fxmlLocation == null) {
                showError("Erreur", "Fichier FXML introuvable: /Fxml/client/AtsAnalysisDialog.fxml");
                return;
            }

            FXMLLoader loader = new FXMLLoader(fxmlLocation);
            VBox root = loader.load();

            AtsAnalysisController controller = loader.getController();
            if (controller != null) {
                controller.setCv(selectedCv);
            }

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Analyse ATS - " + safe(selectedCv.getNomCv()));
            stage.setScene(new Scene(root));
            stage.centerOnScreen();
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showError("Erreur", "Erreur lors de l'ouverture du dialogue: " + e.getMessage());
        }
    }

    private VBox createPreviewItem(String title, String subtitle, String description) {
        VBox box = new VBox(2);
        Label t = new Label(safe(title));
        t.getStyleClass().add("cv-preview-item-title");
        Label s = new Label(safe(subtitle));
        s.getStyleClass().add("cv-preview-item-subtitle");
        box.getChildren().addAll(t, s);

        if (description != null && !description.isBlank()) {
            Label d = new Label(description);
            d.setWrapText(true);
            d.getStyleClass().add("cv-preview-item-description");
            box.getChildren().add(d);
        }

        return box;
    }

    private void handleEditCv(Cv cv) {
        try {
            selectedCv = cv;
            populateForm(cv);
            setFormEditable(true);
            formTitleLabel.setText("Modifier le CV");
            formSubtitleLabel.setText("Mettez à jour les informations de votre CV.");
            submitButton.setText("Enregistrer les modifications");
            showFormPage();
        } catch (Exception e) {
            e.printStackTrace();
            showError("Erreur", "Impossible d'ouvrir le formulaire de modification : " + e.getMessage());
        }
    }

    private void handleDeleteCv(Cv cv) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirmation");
        confirmation.setHeaderText("Supprimer le CV");
        confirmation.setContentText("Voulez-vous vraiment supprimer le CV \"" + safe(cv.getNomCv()) + "\" ?");

        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }

        try {
            cvService.supprimer(cv.getId());
            if (selectedCv != null && selectedCv.getId() != null && selectedCv.getId().equals(cv.getId())) {
                clearForm();
            }
            loadCvs();
            showInfo("Succès", "Le CV a été supprimé avec succès.");
        } catch (IllegalArgumentException | SQLException exception) {
            showError("Suppression impossible", exception.getMessage());
        }
    }

    private Cv buildCvFromForm(Cv sourceCv) {
        Cv cv = new Cv();
        cv.setNomCv(nomCvField.getText());
        cv.setLangue(langueComboBox.getValue());

        Integer templateId = templateComboBox.getValue();
        cv.setIdTemplate(templateId != null ? templateId : 1); // Default to Template 1

        User currentUser = SessionManager.getInstance().getUser();
        if (currentUser != null) {
            cv.setUser(currentUser);
        } else {
            // If session is lost, we should probably warn the user or redirect to login
            // For now, keep the fallback but log it
            System.err.println("Warning: Session lost while building CV. Using default user.");
            cv.setUser(new User(1));
        }

        cv.setLinkedinUrl(emptyToNull(linkedinUrlField.getText()));
        cv.setSummary(emptyToNull(summaryArea.getText()));

        // Build related entities
        cv.setExperiences(new ArrayList<>());
        for (javafx.scene.Node node : experiencesContainer.getChildren()) {
            if (node instanceof VBox) {
                VBox row = (VBox) node;
                Experience exp = new Experience();

                TextField titleF = (TextField) findNodeInParent(row, "Titre du poste");
                TextField companyF = (TextField) findNodeInParent(row, "Entreprise");
                TextField locationF = (TextField) findNodeInParent(row, "Lieu");
                DatePicker startD = (DatePicker) findDatePicker(row, 0);
                DatePicker endD = (DatePicker) findDatePicker(row, 1);
                CheckBox currentC = (CheckBox) findCheckBox(row);
                TextArea descA = (TextArea) findTextArea(row);

                if (titleF != null) exp.setJobTitle(titleF.getText());
                if (companyF != null) exp.setCompany(companyF.getText());
                if (locationF != null) exp.setLocation(locationF.getText());
                if (startD != null) exp.setStartDate(startD.getValue());
                if (currentC != null && currentC.isSelected()) {
                    exp.setCurrentlyWorking(true);
                    exp.setEndDate(null);
                } else {
                    exp.setCurrentlyWorking(false);
                    if (endD != null) exp.setEndDate(endD.getValue());
                }
                if (descA != null) exp.setDescription(descA.getText());

                if (!normalize(exp.getJobTitle()).isBlank() || !normalize(exp.getCompany()).isBlank()) {
                    cv.getExperiences().add(exp);
                }
            }
        }

        cv.setEducations(new ArrayList<>());
        for (javafx.scene.Node node : educationContainer.getChildren()) {
            if (node instanceof VBox) {
                VBox row = (VBox) node;
                Education edu = new Education();

                TextField degreeF = (TextField) findNodeInParent(row, "Diplôme");
                TextField studyF = (TextField) findNodeInParent(row, "Domaine d'étude");
                TextField schoolF = (TextField) findNodeInParent(row, "Établissement");
                TextField cityF = (TextField) findNodeInParent(row, "Ville");
                DatePicker startD = (DatePicker) findDatePicker(row, 0);
                DatePicker endD = (DatePicker) findDatePicker(row, 1);
                TextArea descA = (TextArea) findTextArea(row);

                if (degreeF != null) edu.setDegree(degreeF.getText());
                if (studyF != null) edu.setFieldOfStudy(studyF.getText());
                if (schoolF != null) edu.setSchool(schoolF.getText());
                if (cityF != null) edu.setCity(cityF.getText());
                if (startD != null) edu.setStartDate(startD.getValue());
                if (endD != null) edu.setEndDate(endD.getValue());
                if (descA != null) edu.setDescription(descA.getText());

                if (!normalize(edu.getDegree()).isBlank() || !normalize(edu.getSchool()).isBlank()) {
                    cv.getEducations().add(edu);
                }
            }
        }

        cv.setSkills(new ArrayList<>());
        for (javafx.scene.Node node : skillsContainer.getChildren()) {
            if (node instanceof VBox) {
                VBox row = (VBox) node;
                Skill skill = new Skill();

                TextField nameF = (TextField) findNodeInParent(row, "Compétence");
                TextField typeF = (TextField) findNodeInParent(row, "Type (ex: Hard Skill)");
                TextField levelF = (TextField) findNodeInParent(row, "Niveau");

                if (nameF != null) skill.setNom(nameF.getText());
                if (typeF != null) skill.setType(typeF.getText());
                if (levelF != null) skill.setLevel(levelF.getText());

                if (!normalize(skill.getNom()).isBlank()) {
                    cv.getSkills().add(skill);
                }
            }
        }

        cv.setCertifs(new ArrayList<>());
        for (javafx.scene.Node node : certifsContainer.getChildren()) {
            if (node instanceof VBox) {
                VBox row = (VBox) node;
                Certif cert = new Certif();

                TextField nameF = (TextField) findNodeInParent(row, "Certification");
                TextField issuerF = (TextField) findNodeInParent(row, "Organisme");
                DatePicker issueD = (DatePicker) findDatePicker(row, 0);
                DatePicker expD = (DatePicker) findDatePicker(row, 1);

                if (nameF != null) cert.setName(nameF.getText());
                if (issuerF != null) cert.setIssuedBy(issuerF.getText());
                if (issueD != null) cert.setIssueDate(issueD.getValue());
                if (expD != null) cert.setExpDate(expD.getValue());

                if (!normalize(cert.getName()).isBlank()) {
                    cv.getCertifs().add(cert);
                }
            }
        }

        cv.setLanguages(new ArrayList<>());
        for (javafx.scene.Node node : languagesContainer.getChildren()) {
            if (node instanceof VBox) {
                VBox row = (VBox) node;
                Langue lang = new Langue();

                TextField nameF = (TextField) findNodeInParent(row, "Langue");
                TextField levelF = (TextField) findNodeInParent(row, "Niveau");

                if (nameF != null) lang.setNom(nameF.getText());
                if (levelF != null) lang.setNiveau(levelF.getText());

                if (!normalize(lang.getNom()).isBlank()) {
                    cv.getLanguages().add(lang);
                }
            }
        }

        if (sourceCv != null) {
            cv.setCreationDate(sourceCv.getCreationDate());
        }

        return cv;
    }

    private javafx.scene.Node findNodeInParent(javafx.scene.Parent parent, String prompt) {
        for (javafx.scene.Node node : parent.getChildrenUnmodifiable()) {
            if (node instanceof TextField && prompt.equals(((TextField) node).getPromptText())) {
                return node;
            }
            if (node instanceof javafx.scene.Parent) {
                javafx.scene.Node found = findNodeInParent((javafx.scene.Parent) node, prompt);
                if (found != null) return found;
            }
        }
        return null;
    }

    private DatePicker findDatePicker(javafx.scene.Parent parent, int index) {
        int count = 0;
        return findDatePickerRecursive(parent, index, new int[]{0});
    }

    private DatePicker findDatePickerRecursive(javafx.scene.Parent parent, int targetIndex, int[] currentCount) {
        for (javafx.scene.Node node : parent.getChildrenUnmodifiable()) {
            if (node instanceof DatePicker) {
                if (currentCount[0] == targetIndex) return (DatePicker) node;
                currentCount[0]++;
            }
            if (node instanceof javafx.scene.Parent) {
                DatePicker found = findDatePickerRecursive((javafx.scene.Parent) node, targetIndex, currentCount);
                if (found != null) return found;
            }
        }
        return null;
    }

    private CheckBox findCheckBox(javafx.scene.Parent parent) {
        for (javafx.scene.Node node : parent.getChildrenUnmodifiable()) {
            if (node instanceof CheckBox) return (CheckBox) node;
            if (node instanceof javafx.scene.Parent) {
                CheckBox found = findCheckBox((javafx.scene.Parent) node);
                if (found != null) return found;
            }
        }
        return null;
    }

    private TextArea findTextArea(javafx.scene.Parent parent) {
        for (javafx.scene.Node node : parent.getChildrenUnmodifiable()) {
            if (node instanceof TextArea) return (TextArea) node;
            if (node instanceof javafx.scene.Parent) {
                TextArea found = findTextArea((javafx.scene.Parent) node);
                if (found != null) return found;
            }
        }
        return null;
    }

    private void populateForm(Cv cv) {
        if (cv == null) return;

        nomCvField.setText(safe(cv.getNomCv()));
        langueComboBox.setValue(cv.getLangue());
        templateComboBox.setValue(cv.getIdTemplate());
        populateTemplateGallery(); // Update gallery highlights
        linkedinUrlField.setText(safe(cv.getLinkedinUrl()));
        summaryArea.setText(safe(cv.getSummary()));

        clearContainers();

        if (cv.getExperiences() != null && experiencesContainer != null) {
            for (Experience exp : cv.getExperiences()) {
                experiencesContainer.getChildren().add(createExperienceRow(exp));
            }
        }
        if (cv.getEducations() != null && educationContainer != null) {
            for (Education edu : cv.getEducations()) {
                educationContainer.getChildren().add(createEducationRow(edu));
            }
        }
        if (cv.getSkills() != null && skillsContainer != null) {
            for (Skill skill : cv.getSkills()) {
                skillsContainer.getChildren().add(createSkillRow(skill));
            }
        }
        if (cv.getCertifs() != null && certifsContainer != null) {
            for (Certif cert : cv.getCertifs()) {
                certifsContainer.getChildren().add(createCertifRow(cert));
            }
        }
        if (cv.getLanguages() != null && languagesContainer != null) {
            for (Langue lang : cv.getLanguages()) {
                languagesContainer.getChildren().add(createLanguageRow(lang));
            }
        }
    }

    private void clearForm() {
        selectedCv = null;
        nomCvField.clear();
        langueComboBox.getSelectionModel().clearSelection();
        templateComboBox.setValue(1); // Default to Template 1
        populateTemplateGallery(); // Show gallery highlights
        linkedinUrlField.clear();
        summaryArea.clear();
        clearContainers();
        updateRequiredFieldHighlights();
    }

    private void showListPage() {
        listPage.setVisible(true);
        listPage.setManaged(true);
        formPage.setVisible(false);
        formPage.setManaged(false);
        previewPage.setVisible(false);
        previewPage.setManaged(false);
        aiPage.setVisible(false);
        aiPage.setManaged(false);
    }

    private void showFormPage() {
        listPage.setVisible(false);
        listPage.setManaged(false);
        formPage.setVisible(true);
        formPage.setManaged(true);
        previewPage.setVisible(false);
        previewPage.setManaged(false);
        aiPage.setVisible(false);
        aiPage.setManaged(false);
    }

    private void showPreviewPage() {
        listPage.setVisible(false);
        listPage.setManaged(false);
        formPage.setVisible(false);
        formPage.setManaged(false);
        previewPage.setVisible(true);
        previewPage.setManaged(true);
        aiPage.setVisible(false);
        aiPage.setManaged(false);
    }

    private void showAIPage() {
        listPage.setVisible(false);
        listPage.setManaged(false);
        formPage.setVisible(false);
        formPage.setManaged(false);
        previewPage.setVisible(false);
        previewPage.setManaged(false);
        aiPage.setVisible(true);
        aiPage.setManaged(true);
    }

    @FXML
    private void handleShowAIForm() {
        aiJobTitleField.clear();
        aiNotesArea.clear();
        showAIPage();
    }

    @FXML
    private void handleGenerateWithAI() {
        String jobTitle = aiJobTitleField.getText();
        String notes = aiNotesArea.getText();
        String language = aiLangueComboBox.getValue();

        if (jobTitle == null || jobTitle.isBlank()) {
            showError("Erreur", "Le poste est obligatoire.");
            return;
        }

        List<String> sections = new ArrayList<>();
        if (aiGenSummary.isSelected()) sections.add("Résumé");
        if (aiGenExp.isSelected()) sections.add("Expériences");
        if (aiGenEdu.isSelected()) sections.add("Formations");
        if (aiGenSkills.isSelected()) sections.add("Compétences");
        if (aiGenLang.isSelected()) sections.add("Langues");
        if (aiGenCertifs.isSelected()) sections.add("Certifications");

        // Show loading indicator (could be improved)
        submitButton.setDisable(true);

        new Thread(() -> {
            try {
                User currentUser = SessionManager.getInstance().getUser();
                String userInfo = "";
                if (currentUser != null) {
                    userInfo = "\nIMPORTANT: The candidate's name is " + currentUser.getNom() + " " + currentUser.getPrenom()
                            + " and their email is " + currentUser.getEmail() + ". Use this information for the CV header/contact info.";
                }

                Cv generatedCv = aiService.generateCvWithAI(jobTitle, notes + userInfo, language, sections);
                generatedCv.setNomCv("CV IA - " + jobTitle);
                generatedCv.setLangue(language);
                generatedCv.setCreationDate(LocalDateTime.now());
                generatedCv.setUpdatedAt(LocalDateTime.now());
                generatedCv.setIdTemplate(1); // Default template

                if (currentUser != null) {
                    generatedCv.setUser(currentUser);
                } else {
                    generatedCv.setUser(new User(1)); // Fallback if session lost
                }

                // Save to DB immediately so we can view it
                Cv savedCv = cvService.ajouter(generatedCv);

                Platform.runLater(() -> {
                    submitButton.setDisable(false);
                    handleViewCv(savedCv); // Show final preview directly
                    showInfo("Génération réussie", "Votre CV a été généré avec succès par l'IA.");
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showError("Erreur", "Échec de la génération : " + e.getMessage());
                    submitButton.setDisable(false);
                });
            }
        }).start();
    }

    private void populateFormWithGeneratedCv(Cv cv) {
        nomCvField.setText(cv.getNomCv());
        langueComboBox.setValue(cv.getLangue());
        summaryArea.setText(cv.getSummary());

        clearContainers();

        if (cv.getExperiences() != null) {
            for (Experience exp : cv.getExperiences()) {
                experiencesContainer.getChildren().add(createExperienceRow(exp));
            }
        }
        if (cv.getEducations() != null) {
            for (Education edu : cv.getEducations()) {
                educationContainer.getChildren().add(createEducationRow(edu));
            }
        }
        if (cv.getSkills() != null) {
            for (Skill skill : cv.getSkills()) {
                skillsContainer.getChildren().add(createSkillRow(skill));
            }
        }
        if (cv.getCertifs() != null) {
            for (Certif cert : cv.getCertifs()) {
                certifsContainer.getChildren().add(createCertifRow(cert));
            }
        }
        if (cv.getLanguages() != null) {
            for (Langue lang : cv.getLanguages()) {
                languagesContainer.getChildren().add(createLanguageRow(lang));
            }
        }
    }

    private boolean matchesSearch(Cv cv, String searchValue) {
        if (searchValue == null || searchValue.isBlank()) {
            return true;
        }

        return normalize(cv.getNomCv()).toLowerCase().contains(searchValue)
                || normalize(cv.getSummary()).toLowerCase().contains(searchValue)
                || normalize(cv.getLangue()).toLowerCase().contains(searchValue);
    }

    private boolean matchesLangue(Cv cv, String selectedLangue) {
        if (selectedLangue == null || selectedLangue.equals("Toutes les langues")) {
            return true;
        }

        return selectedLangue.equalsIgnoreCase(normalize(cv.getLangue()));
    }

    private Label createTextLabel(String text, String styleClass) {
        Label label = new Label(text);
        label.getStyleClass().add(styleClass);
        return label;
    }

    private Button createActionButton(String text, String styleClass) {
        Button button = new Button(text);
        button.getStyleClass().addAll("cv-action-btn", styleClass);
        return button;
    }

    private String formatCardDate(LocalDateTime value) {
        return value != null ? value.format(CARD_DATE_FORMATTER) : "--/--/----";
    }

    private int parseRequiredInteger(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }

        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(message);
        }
    }

    private Integer parseOptionalInteger(String value, String message) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(message);
        }
    }

    private String emptyToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private void validateRequiredFields() {
        updateRequiredFieldHighlights();

        List<String> errors = new ArrayList<>();

        // Basic Info Validation
        if (normalize(nomCvField.getText()).isBlank()) {
            errors.add("Le nom du CV est obligatoire.");
        } else if (nomCvField.getText().length() > 255) {
            errors.add("Le nom du CV est trop long (max 255 caractères).");
        }

        if (langueComboBox.getValue() == null || normalize(langueComboBox.getValue()).isBlank()) {
            errors.add("La langue est obligatoire.");
        }

        if (linkedinUrlField.getText() != null && linkedinUrlField.getText().length() > 255) {
            errors.add("L'URL LinkedIn est trop longue (max 255 caractères).");
        }

        if (summaryArea.getText() != null && summaryArea.getText().length() > 5000) {
            errors.add("Le résumé est trop long (max 5000 caractères).");
        }

        // Experiences Validation
        for (javafx.scene.Node node : experiencesContainer.getChildren()) {
            if (node instanceof VBox) {
                VBox row = (VBox) node;
                TextField titleF = (TextField) findNodeInParent(row, "Titre du poste");
                TextField companyF = (TextField) findNodeInParent(row, "Entreprise");
                TextField locationF = (TextField) findNodeInParent(row, "Lieu");

                if (titleF != null && titleF.getText().length() > 255) errors.add("Titre du poste trop long dans les expériences.");
                if (companyF != null && companyF.getText().length() > 255) errors.add("Nom d'entreprise trop long dans les expériences.");
                if (locationF != null && locationF.getText().length() > 255) errors.add("Lieu trop long dans les expériences.");
            }
        }

        // Education Validation
        for (javafx.scene.Node node : educationContainer.getChildren()) {
            if (node instanceof VBox) {
                VBox row = (VBox) node;
                TextField degreeF = (TextField) findNodeInParent(row, "Diplôme");
                TextField studyF = (TextField) findNodeInParent(row, "Domaine d'étude");
                TextField cityF = (TextField) findNodeInParent(row, "Ville");
                TextField schoolF = (TextField) findNodeInParent(row, "Établissement");

                if (degreeF != null && degreeF.getText().length() > 255) errors.add("Diplôme trop long dans les formations.");
                if (studyF != null && studyF.getText().length() > 255) errors.add("Domaine d'étude trop long dans les formations.");
                if (cityF != null && cityF.getText().length() > 255) errors.add("Ville trop longue dans les formations.");
                // School is TEXT in DB, but let's keep it reasonable
                if (schoolF != null && schoolF.getText().length() > 2000) errors.add("Le nom de l'établissement est trop long.");
            }
        }

        // Skills Validation
        for (javafx.scene.Node node : skillsContainer.getChildren()) {
            if (node instanceof VBox) {
                VBox row = (VBox) node;
                TextField nameF = (TextField) findNodeInParent(row, "Compétence");
                TextField typeF = (TextField) findNodeInParent(row, "Type (ex: Hard Skill)");
                TextField levelF = (TextField) findNodeInParent(row, "Niveau");

                if (nameF != null && nameF.getText().length() > 255) errors.add("Nom de compétence trop long.");
                if (typeF != null && typeF.getText().length() > 50) errors.add("Type de compétence trop long (max 50).");
                if (levelF != null && levelF.getText().length() > 255) errors.add("Niveau de compétence trop long.");
            }
        }

        // Certifications Validation
        for (javafx.scene.Node node : certifsContainer.getChildren()) {
            if (node instanceof VBox) {
                VBox row = (VBox) node;
                TextField nameF = (TextField) findNodeInParent(row, "Certification");
                TextField issuerF = (TextField) findNodeInParent(row, "Organisme");

                if (nameF != null && nameF.getText().length() > 255) errors.add("Nom de certification trop long.");
                if (issuerF != null && issuerF.getText().length() > 255) errors.add("Organisme de certification trop long.");
            }
        }

        // Languages Validation
        for (javafx.scene.Node node : languagesContainer.getChildren()) {
            if (node instanceof VBox) {
                VBox row = (VBox) node;
                TextField nameF = (TextField) findNodeInParent(row, "Langue");
                TextField levelF = (TextField) findNodeInParent(row, "Niveau");

                if (nameF != null && nameF.getText().length() > 255) errors.add("Nom de langue trop long.");
                if (levelF != null && levelF.getText().length() > 255) errors.add("Niveau de langue trop long.");
            }
        }

        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join("\n", errors));
        }
    }

    private void setFormEditable(boolean editable) {
        readOnlyMode = !editable;
        nomCvField.setEditable(editable);
        langueComboBox.setDisable(!editable);
        templateComboBox.setDisable(!editable);
        linkedinUrlField.setEditable(editable);
        summaryArea.setEditable(editable);

        submitButton.setVisible(editable);
        submitButton.setManaged(editable);
        submitButton.setDisable(!editable); // Ensure it's enabled when editable

        // Hide/Show Add buttons
        addExperienceBtn.setVisible(editable);
        addExperienceBtn.setManaged(editable);
        addEducationBtn.setVisible(editable);
        addEducationBtn.setManaged(editable);
        addSkillBtn.setVisible(editable);
        addSkillBtn.setManaged(editable);
        addCertifBtn.setVisible(editable);
        addCertifBtn.setManaged(editable);
        addLanguageBtn.setVisible(editable);
        addLanguageBtn.setManaged(editable);

        // Hide/Show Remove buttons in rows
        updateRowsEditable(experiencesContainer, editable);
        updateRowsEditable(educationContainer, editable);
        updateRowsEditable(skillsContainer, editable);
        updateRowsEditable(certifsContainer, editable);
        updateRowsEditable(languagesContainer, editable);

        clearButton.setText(editable ? "Réinitialiser" : "Fermer l'aperçu");
        updateRequiredFieldHighlights();
    }

    private void updateRowsEditable(VBox container, boolean editable) {
        for (javafx.scene.Node node : container.getChildren()) {
            if (node instanceof VBox) {
                VBox row = (VBox) node;
                // Recursive search for TextFields, DatePickers, TextAreas, CheckBoxes
                setEditableRecursive(row, editable);

                // The remove button is usually in the first HBox or as a child of VBox
                // In my new implementation, it's in the first HBox (actions)
                if (!row.getChildren().isEmpty() && row.getChildren().get(0) instanceof HBox) {
                    HBox actions = (HBox) row.getChildren().get(0);
                    if (!actions.getChildren().isEmpty()) {
                        javafx.scene.Node removeBtn = actions.getChildren().get(actions.getChildren().size() - 1);
                        removeBtn.setVisible(editable);
                        removeBtn.setManaged(editable);
                    }
                }
            }
        }
    }

    private void setEditableRecursive(javafx.scene.Parent parent, boolean editable) {
        for (javafx.scene.Node child : parent.getChildrenUnmodifiable()) {
            if (child instanceof TextField) {
                ((TextField) child).setEditable(editable);
            } else if (child instanceof TextArea) {
                ((TextArea) child).setEditable(editable);
            } else if (child instanceof DatePicker) {
                DatePicker dp = (DatePicker) child;
                dp.setEditable(editable);
                if (!dp.disableProperty().isBound()) {
                    dp.setDisable(!editable);
                }
            } else if (child instanceof CheckBox) {
                CheckBox cb = (CheckBox) child;
                if (!cb.disableProperty().isBound()) {
                    cb.setDisable(!editable);
                }
            } else if (child instanceof javafx.scene.Parent) {
                setEditableRecursive((javafx.scene.Parent) child, editable);
            }
        }
    }

    private void updateRequiredFieldHighlights() {
        if (readOnlyMode) {
            updateFieldState(nomCvField, true);
            updateFieldState(langueComboBox, true);
            return;
        }

        updateFieldState(nomCvField, !normalize(nomCvField.getText()).isBlank());
        updateFieldState(langueComboBox, langueComboBox.getValue() != null && !normalize(langueComboBox.getValue()).isBlank());
    }

    private void updateFieldState(Control control, boolean valid) {
        if (valid) {
            control.getStyleClass().remove("cv-required-missing");
        } else if (!control.getStyleClass().contains("cv-required-missing")) {
            control.getStyleClass().add("cv-required-missing");
        }
    }

    private void handleDownloadPdf(Cv cv) {
        if (cv == null) return;

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exporter le CV en PDF");
        fileChooser.setInitialFileName("CV_" + (cv.getUser() != null ? cv.getUser().getNom() : cv.getNomCv()).replace(" ", "_") + ".pdf");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichiers PDF", "*.pdf"));

        File file = fileChooser.showSaveDialog(previewPage.getScene().getWindow());
        if (file != null) {
            try {
                pdfService.generateCvPdf(cv, file.getAbsolutePath());
                showInfo("Succès", "Votre CV a été exporté avec succès en PDF !");
            } catch (Exception e) {
                e.printStackTrace();
                showError("Erreur", "Impossible de générer le PDF : " + e.getMessage());
            }
        }
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.show();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
