package com.pidev.Controllers.admin;

import com.pidev.Services.GrokQuizService;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

public class GenerateQuizAIController {

    // Clé API Grok (xAI) — intégrée en dur et cachée de l'interface
    private static final String GROK_API_KEY = System.getenv("GROQ_API_KEY");

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
    private final GrokQuizService grokService = new GrokQuizService();

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
            showAlert(Alert.AlertType.WARNING, "Attention",
                    "Veuillez d'abord sélectionner un fichier PDF contenant le cours.");
            return;
        }

        generateBtn.setDisable(true);
        progressIndicator.setVisible(true);
        resultArea.setText("Analyse du PDF en cours...\nEnvoi à Grok (xAI), veuillez patienter.");

        Task<String> generateTask = new Task<>() {
            @Override
            protected String call() throws Exception {
                return grokService.generateQuiz(selectedPdfFile, GROK_API_KEY, numQuestions);
            }
        };

        generateTask.setOnSucceeded(e -> {
            generateBtn.setDisable(false);
            progressIndicator.setVisible(false);
            resultArea.setText(generateTask.getValue());
            showAlert(Alert.AlertType.INFORMATION, "Génération Réussie",
                    "L'IA a généré le quiz avec succès !");
        });

        generateTask.setOnFailed(e -> {
            generateBtn.setDisable(false);
            progressIndicator.setVisible(false);
            String errMsg = generateTask.getException().getMessage();
            resultArea.setText("❌ Erreur :\n\n" + errMsg);
            showAlert(Alert.AlertType.ERROR, "Erreur de Génération", errMsg);
        });

         new Thread(generateTask).start();
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
