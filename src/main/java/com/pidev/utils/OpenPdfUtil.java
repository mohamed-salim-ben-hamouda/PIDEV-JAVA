package com.pidev.utils;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class OpenPdfUtil {
    private OpenPdfUtil() {
    }

    public static void openPdfInApp(String rawPath) throws IOException {
        openPdfInApp(rawPath, "PDF Viewer");
    }

    public static void openPdfInApp(String rawPath, String title) throws IOException {
        if (rawPath == null || rawPath.isBlank()) {
            throw new IllegalArgumentException("No PDF path provided.");
        }

        Path resolvedPath = resolvePdfPath(rawPath);
        if (resolvedPath == null || !Files.exists(resolvedPath)) {
            throw new IOException("Feedback file not found:\n" + rawPath);
        }

        openPdfInViewer(resolvedPath, (title == null || title.isBlank()) ? "PDF Viewer" : title);
    }

    public static Path resolvePdfPath(String rawPath) {
        String normalizedPath = rawPath.trim().replace("\\", "/");
        if (normalizedPath.startsWith("/")) {
            normalizedPath = normalizedPath.substring(1);
        }

        Path directPath = Paths.get(normalizedPath);
        if (directPath.isAbsolute() && Files.exists(directPath)) {
            return directPath;
        }

        Path projectRoot = Paths.get(System.getProperty("user.dir"));

        Path fromProjectRoot = projectRoot.resolve(normalizedPath);
        if (Files.exists(fromProjectRoot)) {
            return fromProjectRoot;
        }

        Path srcResources = projectRoot.resolve(Paths.get("src", "main", "resources", normalizedPath));
        if (Files.exists(srcResources)) {
            return srcResources;
        }

        Path targetResources = projectRoot.resolve(Paths.get("target", "classes", normalizedPath));
        if (Files.exists(targetResources)) {
            return targetResources;
        }

        return null;
    }

    private static void openPdfInViewer(Path pdfPath, String title) throws IOException {
        PDDocument document = Loader.loadPDF(pdfPath.toFile());
        int totalPages = document.getNumberOfPages();
        if (totalPages == 0) {
            document.close();
            throw new IOException("The selected PDF is empty.");
        }

        PDFRenderer renderer = new PDFRenderer(document);
        ImageView pageView = new ImageView();
        pageView.setPreserveRatio(true);
        pageView.setSmooth(true);

        StackPane pageContainer = new StackPane(pageView);
        pageContainer.setAlignment(Pos.TOP_CENTER);
        pageContainer.setPadding(new Insets(14));

        ScrollPane scrollPane = new ScrollPane(pageContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setPannable(true);

        Button previousBtn = new Button("Previous");
        Button nextBtn = new Button("Next");
        Label pageIndicator = new Label();
        int[] currentPage = {0};

        Runnable updatePage = () -> {
            try {
                pageView.setImage(renderPage(renderer, currentPage[0]));
                pageIndicator.setText("Page " + (currentPage[0] + 1) + " / " + totalPages);
                previousBtn.setDisable(currentPage[0] == 0);
                nextBtn.setDisable(currentPage[0] == totalPages - 1);
                scrollPane.setVvalue(0);
            } catch (IOException ignored) {
            }
        };

        previousBtn.setOnAction(event -> {
            if (currentPage[0] > 0) {
                currentPage[0]--;
                updatePage.run();
            }
        });

        nextBtn.setOnAction(event -> {
            if (currentPage[0] < totalPages - 1) {
                currentPage[0]++;
                updatePage.run();
            }
        });

        HBox controls = new HBox(12, previousBtn, pageIndicator, nextBtn);
        controls.setAlignment(Pos.CENTER);
        controls.setPadding(new Insets(10));

        BorderPane root = new BorderPane();
        root.setCenter(scrollPane);
        root.setBottom(controls);

        Scene scene = new Scene(root, 860, 700);
        Stage pdfStage = new Stage();
        pdfStage.setTitle(title);
        pdfStage.setScene(scene);
        pdfStage.setOnCloseRequest(event -> {
            try {
                document.close();
            } catch (IOException ignored) {
            }
        });

        scrollPane.viewportBoundsProperty().addListener((obs, oldValue, newValue) ->
                pageView.setFitWidth(Math.max(260, (newValue.getWidth() - 48) * 0.72))
        );

        updatePage.run();
        pdfStage.show();
    }

    private static Image renderPage(PDFRenderer renderer, int pageIndex) throws IOException {
        BufferedImage page = renderer.renderImageWithDPI(pageIndex, 105);
        Path tempFile = Files.createTempFile("feedback-pdf-page-" + pageIndex + "-", ".png");
        tempFile.toFile().deleteOnExit();
        ImageIO.write(page, "png", tempFile.toFile());
        return new Image(tempFile.toUri().toString());
    }
}
