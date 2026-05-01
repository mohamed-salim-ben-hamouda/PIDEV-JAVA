package com.pidev.Controllers.admin;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.net.URL;

public final class AdminDialogStyler {
    private AdminDialogStyler() {
    }

    public static VBox apply(Dialog<?> dialog, String title, String subtitle, double width, double height) {
        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getStyleClass().add("crud-dialog-pane");
        dialogPane.setPrefSize(width, height);
        dialogPane.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        dialogPane.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        URL stylesheet = AdminDialogStyler.class.getResource("/styles/backoffice.css");
        if (stylesheet != null && !dialogPane.getStylesheets().contains(stylesheet.toExternalForm())) {
            dialogPane.getStylesheets().add(stylesheet.toExternalForm());
        }

        VBox wrapper = new VBox(14);
        wrapper.setPadding(new Insets(10));
        wrapper.getStyleClass().add("crud-dialog-wrapper");

        VBox card = new VBox(14);
        card.setPadding(new Insets(18, 20, 18, 20));
        card.getStyleClass().add("crud-dialog-card");

        VBox header = new VBox(4);
        header.getStyleClass().add("crud-dialog-header");
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("crud-dialog-title");
        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.getStyleClass().add("crud-dialog-subtitle");
        subtitleLabel.setWrapText(true);
        header.getChildren().addAll(titleLabel, subtitleLabel);
        card.getChildren().add(header);
        wrapper.getChildren().add(card);

        ScrollPane scrollPane = new ScrollPane(wrapper);
        scrollPane.getStyleClass().add("crud-dialog-scroll");
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setPannable(true);
        dialogPane.setContent(scrollPane);
        return wrapper;
    }

    public static void styleButton(Button button, boolean primary) {
        button.getStyleClass().add(primary ? "crud-dialog-primary-btn" : "crud-dialog-secondary-btn");
        button.setMinWidth(110);
        button.setPrefWidth(110);
    }

    public static HBox createFooterHint(String text) {
        HBox hint = new HBox();
        hint.setAlignment(Pos.CENTER_LEFT);
        Label label = new Label(text);
        label.getStyleClass().add("crud-dialog-hint");
        hint.getChildren().add(label);
        return hint;
    }

    public static void styleField(Node node) {
        node.getStyleClass().add("crud-field");
    }

    public static void styleFormLabel(Label label) {
        label.getStyleClass().add("crud-form-label");
    }

    public static Label createSectionLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("crud-section-label");
        return label;
    }

    public static Separator createSectionSeparator() {
        Separator separator = new Separator();
        separator.getStyleClass().add("crud-section-separator");
        return separator;
    }

    public static void styleTextArea(Node node) {
        node.getStyleClass().add("crud-text-area");
    }

    public static void styleComboBox(Node node) {
        node.getStyleClass().add("crud-combo-box");
    }
}