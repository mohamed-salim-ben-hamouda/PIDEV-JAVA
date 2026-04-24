package com.pidev.Controllers.client;

import com.pidev.models.*;
import com.pidev.Services.CVService;
import com.pidev.Services.AIService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
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
    private TextField linkedinUrlField;
    @FXML
    private TextArea summaryArea;

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
            allCvs = cvService.afficher();
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

        HBox highlights = new HBox(8);
        highlights.getChildren().addAll(
                createTextLabel("Utilisateur #" + (cv.getUser() != null && cv.getUser().getId() != null ? cv.getUser().getId() : "-"), "cv-soft-badge"),
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

        // Populate Preview Page
        previewNomLabel.setText(safe(cv.getNomCv()).toUpperCase());
        previewLangueLabel.setText(safe(cv.getLangue()));
        previewSummaryLabel.setText(cv.getSummary() != null && !cv.getSummary().isBlank() ? cv.getSummary() : "Aucun résumé fourni.");
        previewLinkedinLabel.setText(cv.getLinkedinUrl() != null && !cv.getLinkedinUrl().isBlank() ? cv.getLinkedinUrl() : "Non renseigné");

        previewExpBox.getChildren().clear();
        if (cv.getExperiences() != null) {
            for (Experience exp : cv.getExperiences()) {
                String dates = (exp.getStartDate() != null ? exp.getStartDate().format(CARD_DATE_FORMATTER) : "")
                        + " - " + (exp.getCurrentlyWorking() != null && exp.getCurrentlyWorking() ? "Aujourd'hui" : (exp.getEndDate() != null ? exp.getEndDate().format(CARD_DATE_FORMATTER) : ""));
                String subtitle = exp.getCompany() + (exp.getLocation() != null ? " | " + exp.getLocation() : "") + " (" + dates + ")";
                previewExpBox.getChildren().add(createPreviewItem(exp.getJobTitle(), subtitle, exp.getDescription()));
            }
        }

        previewEduBox.getChildren().clear();
        if (cv.getEducations() != null) {
            for (Education edu : cv.getEducations()) {
                String dates = (edu.getStartDate() != null ? edu.getStartDate().format(CARD_DATE_FORMATTER) : "")
                        + " - " + (edu.getEndDate() != null ? edu.getEndDate().format(CARD_DATE_FORMATTER) : "");
                String subtitle = edu.getSchool() + (edu.getCity() != null ? " | " + edu.getCity() : "") + " (" + dates + ")";
                previewEduBox.getChildren().add(createPreviewItem(edu.getDegree() + " en " + edu.getFieldOfStudy(), subtitle, edu.getDescription()));
            }
        }

        previewSkillBox.getChildren().clear();
        if (cv.getSkills() != null) {
            for (Skill skill : cv.getSkills()) {
                previewSkillBox.getChildren().add(createPreviewItem(skill.getNom(), skill.getType() + " | " + skill.getLevel(), null));
            }
        }

        previewCertBox.getChildren().clear();
        if (cv.getCertifs() != null) {
            for (Certif cert : cv.getCertifs()) {
                String dates = (cert.getIssueDate() != null ? "Obtenu le " + cert.getIssueDate().format(CARD_DATE_FORMATTER) : "")
                        + (cert.getExpDate() != null ? " | Expire le " + cert.getExpDate().format(CARD_DATE_FORMATTER) : "");
                previewCertBox.getChildren().add(createPreviewItem(cert.getName(), cert.getIssuedBy() + " (" + dates + ")", null));
            }
        }

        previewLangBox.getChildren().clear();
        if (cv.getLanguages() != null) {
            for (Langue lang : cv.getLanguages()) {
                previewLangBox.getChildren().add(createPreviewItem(lang.getNom(), lang.getNiveau(), null));
            }
        }

        showPreviewPage();
    }

    @FXML
    private void handleTranslateCv() {
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
        cv.setIdTemplate(templateComboBox.getValue());
        cv.setUser(new User(1));
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
                if (endD != null) exp.setEndDate(endD.getValue());
                if (currentC != null) exp.setCurrentlyWorking(currentC.isSelected());
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
        templateComboBox.getSelectionModel().clearSelection();
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

        // Show loading indicator (could be improved)
        submitButton.setDisable(true);

        new Thread(() -> {
            try {
                Cv generatedCv = aiService.generateCvWithAI(jobTitle, notes, language, sections);
                generatedCv.setNomCv("CV IA - " + jobTitle);
                generatedCv.setLangue(language);

                Platform.runLater(() -> {
                    handleShowCreateForm(); // Switch to form page
                    populateFormWithGeneratedCv(generatedCv);
                    submitButton.setDisable(false);
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

        List<String> missingFields = new ArrayList<>();
        if (normalize(nomCvField.getText()).isBlank()) {
            missingFields.add("Nom du CV");
        }
        if (langueComboBox.getValue() == null || normalize(langueComboBox.getValue()).isBlank()) {
            missingFields.add("Langue");
        }

        if (!missingFields.isEmpty()) {
            throw new IllegalArgumentException("Veuillez renseigner les champs obligatoires : " + String.join(", ", missingFields) + ".");
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

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
