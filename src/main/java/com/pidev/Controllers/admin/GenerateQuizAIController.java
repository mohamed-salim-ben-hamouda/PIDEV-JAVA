package com.pidev.Controllers.admin;

import com.pidev.Services.GeminiQuizService;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

public class GenerateQuizAIController {

    // La clé API est maintenant intégrée en dur et cachée de l'interface
    private static final String GEMINI_API_KEY = "AIzaSyAP8AcHWKYOPaUXRwagCXYVYynIoMeakQM";

    @FXML
    private Spinner<Integer> numQuestionsSpinner;

    @FXML
    private Label selectedFileLabel;

    @FXML
    private Button generateBtn;

    @FXML
    private TextArea resultArea;

    @FXML
    private ProgressIndicator progressIndicator;

    private File selectedPdfFile;
    private final GeminiQuizService geminiService = new GeminiQuizService();

    @FXML
    public void initialize() {
        // Initialisation du spinner (de 1 à 20 questions, par défaut 5)
        SpinnerValueFactory<Integer> valueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 20, 5);
        numQuestionsSpinner.setValueFactory(valueFactory);
        progressIndicator.setVisible(false);
    }

    @FXML
    public void onSelectFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Sélectionner un cours (PDF)");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichiers PDF", "*.pdf"));
        
        Stage stage = (Stage) selectedFileLabel.getScene().getWindow();
        selectedPdfFile = fileChooser.showOpenDialog(stage);

        if (selectedPdfFile != null) {
            selectedFileLabel.setText(selectedPdfFile.getName());
            selectedFileLabel.setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;");
        } else {
            selectedFileLabel.setText("Aucun fichier sélectionné");
            selectedFileLabel.setStyle("-fx-text-fill: #ef4444;");
        }
    }

    @FXML
    public void onGenerate() {
        int numQuestions = numQuestionsSpinner.getValue();

        if (selectedPdfFile == null) {
            showAlert(Alert.AlertType.WARNING, "Attention", "Veuillez d'abord sélectionner un fichier PDF contenant le cours.");
            return;
        }

        // Mise à jour de l'UI pendant le chargement
        generateBtn.setDisable(true);
        progressIndicator.setVisible(true);
        resultArea.setText("Analyse du PDF et génération par l'IA en cours...\nCela peut prendre quelques secondes (merci de patienter).");

        // Création d'une tâche en arrière-plan pour ne pas bloquer l'interface (JavaFX Thread)
        Task<String> generateTask = new Task<>() {
            @Override
            protected String call() throws Exception {
                return geminiService.generateQuiz(selectedPdfFile, GEMINI_API_KEY, numQuestions);
            }
        };

        generateTask.setOnSucceeded(e -> {
            generateBtn.setDisable(false);
            progressIndicator.setVisible(false);
            resultArea.setText(generateTask.getValue());
            showAlert(Alert.AlertType.INFORMATION, "Génération Réussie", "L'IA a généré le quiz avec succès !");
            
            // TODO: Ici vous pourrez ajouter la logique pour insérer ce JSON dans votre base de données MySQL
            // en appelant vos propres Services de quiz/questions existants.
        });

        generateTask.setOnFailed(e -> {
            generateBtn.setDisable(false);
            progressIndicator.setVisible(false);
            resultArea.setText("Erreur : " + generateTask.getException().getMessage());
            showAlert(Alert.AlertType.ERROR, "Erreur de Génération", "Une erreur s'est produite lors de la communication avec l'IA.\n" + generateTask.getException().getMessage());
        });

        // Lancement de la tâche dans un nouveau Thread
        new Thread(generateTask).start();
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
