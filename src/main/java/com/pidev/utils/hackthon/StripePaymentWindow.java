package com.pidev.utils.hackthon;

import javafx.scene.Scene;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class StripePaymentWindow {

    public static void show(String url, Runnable onSuccess, Runnable onCancel) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Paiement Sécurisé Stripe");

        WebView webView = new WebView();
        webView.getEngine().load(url);

        // Surveiller l'URL pour détecter le succès ou l'annulation
        webView.getEngine().locationProperty().addListener((obs, oldLocation, newLocation) -> {
            if (newLocation.contains("success.example.com")) {
                stage.close();
                if (onSuccess != null) javafx.application.Platform.runLater(onSuccess);
            } else if (newLocation.contains("cancel.example.com")) {
                stage.close();
                if (onCancel != null) javafx.application.Platform.runLater(onCancel);
            }
        });

        Scene scene = new Scene(webView, 1000, 700);
        stage.setScene(scene);
        stage.showAndWait();
    }
}
