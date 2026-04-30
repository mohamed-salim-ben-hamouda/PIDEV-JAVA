package com.pidev.Controllers.client.User;

import com.pidev.Services.HuggingFaceService;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_videoio.VideoCapture;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;

import static org.bytedeco.opencv.global.opencv_imgcodecs.imencode;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imwrite;

public class AIAvatarCaptureController implements Initializable {

    @FXML private ImageView cameraView;
    @FXML private Label statusLabel;
    @FXML private ProgressIndicator loadingSpinner;
    @FXML private Button captureBtn;
    @FXML private Button cancelBtn;

    private VideoCapture capture;
    private boolean isCapturing = true;
    private ProfileController profileController;
    private HuggingFaceService huggingFaceService;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        huggingFaceService = new HuggingFaceService();
        startCamera();
    }

    public void setProfileController(ProfileController profileController) {
        this.profileController = profileController;
    }

    private void startCamera() {
        new Thread(() -> {
            try {
                capture = new VideoCapture(0);
                if (!capture.isOpened()) {
                    Platform.runLater(() -> statusLabel.setText("Erreur: Impossible d'ouvrir la webcam."));
                    return;
                }
                
                Mat frame = new Mat();
                while (isCapturing && capture != null && capture.isOpened()) {
                    if (capture.read(frame) && !frame.empty()) {
                        Image fxImage = matToImage(frame);
                        if (fxImage != null) {
                            Platform.runLater(() -> cameraView.setImage(fxImage));
                        }
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }).start();
    }

    @FXML
    private void handleCaptureAndGenerate(ActionEvent event) {
        if (capture == null || !capture.isOpened()) {
            return;
        }

        // 1. Capturer l'image actuelle
        Mat frame = new Mat();
        if (capture.read(frame) && !frame.empty()) {
            isCapturing = false; // Stop preview
            stopCamera();
            
            captureBtn.setDisable(true);
            cancelBtn.setDisable(true);
            loadingSpinner.setVisible(true);
            statusLabel.setText("Analyse de votre visage par l'IA (Vision)...");

            // Sauvegarder temp file
            File tempFile = new File(System.getProperty("user.dir") + "/temp_avatar_capture.jpg");
            imwrite(tempFile.getAbsolutePath(), frame);

            // 2. Analyser l'image (Image to Text)
            huggingFaceService.analyzeImage(tempFile)
                .thenCompose(description -> {
                    Platform.runLater(() -> statusLabel.setText("Génération de l'avatar (Stable Diffusion)..."));
                    System.out.println("Description IA: " + description);
                    
                    // 3. Générer l'avatar (Text to Image)
                    return huggingFaceService.generateImage(description);
                })
                .thenAccept(generatedImageFile -> {
                    // Nettoyage temp
                    tempFile.delete();
                    
                    Platform.runLater(() -> {
                        statusLabel.setText("Avatar généré avec succès !");
                        loadingSpinner.setVisible(false);
                        
                        if (profileController != null) {
                            profileController.setAvatarPath(generatedImageFile.getAbsolutePath());
                        }
                        
                        closeWindow();
                    });
                })
                .exceptionally(ex -> {
                    tempFile.delete();
                    Platform.runLater(() -> {
                        loadingSpinner.setVisible(false);
                        captureBtn.setDisable(false);
                        cancelBtn.setDisable(false);
                        statusLabel.setText("Erreur: " + ex.getMessage());
                        showAlert("Erreur IA", ex.getMessage());
                    });
                    return null;
                });
        }
    }

    @FXML
    private void handleCancel(ActionEvent event) {
        stopCamera();
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) cancelBtn.getScene().getWindow();
        if (stage != null) {
            stage.close();
        }
    }

    private void stopCamera() {
        isCapturing = false;
        try {
            if (capture != null) {
                capture.release();
                capture = null;
            }
        } catch (Exception e) {}
    }

    private Image matToImage(Mat mat) {
        try {
            BytePointer bytePointer = new BytePointer();
            imencode(".jpg", mat, bytePointer);
            byte[] byteArray = bytePointer.getStringBytes();
            return new Image(new ByteArrayInputStream(byteArray));
        } catch (Exception e) {
            return null;
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.show();
    }
}
