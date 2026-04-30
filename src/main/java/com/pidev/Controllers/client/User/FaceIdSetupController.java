package com.pidev.Controllers.client.User;

import com.pidev.Services.FaceRecognitionService;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.Size;
import org.bytedeco.opencv.opencv_videoio.VideoCapture;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.Size;

import java.io.ByteArrayInputStream;

import static org.bytedeco.opencv.global.opencv_imgcodecs.imencode;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imwrite;
import static org.bytedeco.opencv.global.opencv_imgproc.*;

public class FaceIdSetupController {

    private FaceRecognitionService faceService;
    private int userId;
    private int captureCount = 0;
    private final int REQUIRED_CAPTURES = 20;
    private VideoCapture capture;
    private boolean isCapturing = false;

    public void startSetup(int userId) {
        try {
            this.faceService = new FaceRecognitionService();
        } catch (Throwable t) {
            t.printStackTrace();
            throw new RuntimeException("Erreur OpenCV: " + t.getMessage(), t);
        }

        this.userId = userId;
        Stage stage = new Stage();
        stage.setTitle("Configuration Face ID");

        VBox root = new VBox(10);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-padding: 20; -fx-background-color: white;");

        Label infoLabel = new Label("Placez votre visage devant la caméra...");
        ImageView cameraView = new ImageView();
        cameraView.setFitWidth(400);
        cameraView.setFitHeight(300);

        Button startBtn = new Button("Commencer l'enregistrement");
        startBtn.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20;");
        
        root.getChildren().addAll(infoLabel, cameraView, startBtn);

        startBtn.setOnAction(e -> {
            if (!isCapturing) {
                isCapturing = true;
                startBtn.setDisable(true);
                new Thread(() -> captureFrames(cameraView, infoLabel, stage)).start();
            }
        });

        stage.setOnCloseRequest(e -> {
            stopGrabber();
        });

        stage.setScene(new Scene(root, 450, 450));
        stage.show();

        // Start preview
        new Thread(() -> {
            try {
                capture = new VideoCapture(0);
                if (!capture.isOpened()) {
                    System.err.println("Erreur: Impossible d'ouvrir la webcam.");
                    return;
                }
                
                Mat frame = new Mat();
                while (!isCapturing && capture != null && capture.isOpened()) {
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

    private void captureFrames(ImageView cameraView, Label infoLabel, Stage stage) {
        try {
            Mat frame = new Mat();

            while (captureCount < REQUIRED_CAPTURES && capture != null && capture.isOpened()) {
                if (!capture.read(frame) || frame.empty()) continue;

                Mat grayMat = new Mat();
                cvtColor(frame, grayMat, COLOR_BGR2GRAY);

                Rect[] faces = faceService.detectFaces(grayMat);

                if (faces.length > 0) {
                    Rect face = faces[0]; // Take the first face
                    Mat faceROI = new Mat(grayMat, face);
                    Mat resizedFace = new Mat();
                    resize(faceROI, resizedFace, new Size(200, 200));

                    String filename = "src/main/resources/images/face_id/" + userId + "_" + System.currentTimeMillis() + ".jpg";
                    imwrite(filename, resizedFace);
                    captureCount++;

                    Platform.runLater(() -> infoLabel.setText("Capture " + captureCount + "/" + REQUIRED_CAPTURES));
                }

                Image fxImage = matToImage(frame);
                if (fxImage != null) {
                    Platform.runLater(() -> cameraView.setImage(fxImage));
                }

                Thread.sleep(100); // 100ms between captures
            }

            stopGrabber();

            Platform.runLater(() -> {
                infoLabel.setText("Entraînement du modèle en cours...");
            });

            faceService.trainModel();

            Platform.runLater(() -> {
                infoLabel.setText("Face ID configuré avec succès !");
                stage.close();
            });

        } catch (Exception e) {
            e.printStackTrace();
            Platform.runLater(() -> infoLabel.setText("Erreur lors de la configuration."));
            stopGrabber();
        }
    }

    private Image matToImage(Mat mat) {
        try {
            BytePointer bytePointer = new BytePointer();
            imencode(".png", mat, bytePointer);
            byte[] byteArray = bytePointer.getStringBytes();
            return new Image(new ByteArrayInputStream(byteArray));
        } catch (Exception e) {
            return null;
        }
    }

    private void stopGrabber() {
        try {
            if (capture != null) {
                capture.release();
                capture = null;
            }
        } catch (Exception e) {}
    }
}
