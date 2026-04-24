package com.pidev.utils.hackthon;

import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;

public class SignatureDialog {

    private File signatureFile;

    public File showAndWait() {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Signer le contrat");

        Canvas canvas = new Canvas(400, 200);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(3);

        // Fond blanc pour l'image capturée
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, 400, 200);

        canvas.setOnMousePressed(e -> {
            gc.beginPath();
            gc.moveTo(e.getX(), e.getY());
            gc.stroke();
        });

        canvas.setOnMouseDragged(e -> {
            gc.lineTo(e.getX(), e.getY());
            gc.stroke();
        });

        Button clearBtn = new Button("Effacer");
        clearBtn.setOnAction(e -> {
            gc.setFill(Color.WHITE);
            gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        });

        Button saveBtn = new Button("Confirmer la signature");
        saveBtn.setOnAction(e -> {
            try {
                WritableImage writableImage = new WritableImage(400, 200);
                canvas.snapshot(null, writableImage);
                signatureFile = File.createTempFile("signature_", ".png");
                ImageIO.write(SwingFXUtils.fromFXImage(writableImage, null), "png", signatureFile);
                stage.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });

        HBox btnBox = new HBox(10, clearBtn, saveBtn);
        btnBox.setAlignment(Pos.CENTER);

        VBox root = new VBox(15, new javafx.scene.control.Label("Veuillez signer ci-dessous :"), canvas, btnBox);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new javafx.geometry.Insets(20));
        root.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #dee2e6; -fx-border-width: 2; -fx-border-radius: 10; -fx-background-radius: 10;");

        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.showAndWait();

        return signatureFile;
    }
}
