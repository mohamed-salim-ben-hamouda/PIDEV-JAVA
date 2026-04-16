package com.pidev.utils;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ExtractTextFromPdfUtil {
    public static String extractText(String path) {
        Path resolvedPath = resolvePdfPath(path);
        if (resolvedPath == null) {
            throw new RuntimeException("PDF not found: " + path);
        }

        try (PDDocument document = Loader.loadPDF(resolvedPath.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            if (text == null || text.trim().isEmpty()) {
                throw new RuntimeException("Empty PDF content: " + resolvedPath);
            }
            return text.replaceAll("\\s+", " ").trim();
        } catch (Exception e) {
            throw new RuntimeException("PDF extraction failed for: " + path, e);
        }
    }

    private static Path resolvePdfPath(String rawPath) {
        if (rawPath == null || rawPath.isBlank()) {
            return null;
        }

        String normalizedPath = rawPath.trim().replace("\\", "/");
        if (normalizedPath.startsWith("/")) {
            normalizedPath = normalizedPath.substring(1);
        }

        Path directPath = Paths.get(normalizedPath);
        if (Files.exists(directPath)) {
            return directPath.toAbsolutePath().normalize();
        }

        Path projectRoot = Paths.get(System.getProperty("user.dir"));

        Path fromProjectRoot = projectRoot.resolve(normalizedPath);
        if (Files.exists(fromProjectRoot)) {
            return fromProjectRoot.normalize();
        }

        Path srcResources = projectRoot.resolve(Paths.get("src", "main", "resources", normalizedPath));
        if (Files.exists(srcResources)) {
            return srcResources.normalize();
        }

        Path targetResources = projectRoot.resolve(Paths.get("target", "classes", normalizedPath));
        if (Files.exists(targetResources)) {
            return targetResources.normalize();
        }

        return null;
    }

}
