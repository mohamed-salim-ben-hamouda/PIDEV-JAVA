package com.pidev.utils;

import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.Random;

public class CaptchaGenerator {

    private String currentAnswer;
    private final int width;
    private final int height;
    private final Random random = new Random();

    public CaptchaGenerator(int width, int height) {
        this.width = width;
        this.height = height;
    }

    /**
     * Generates a Math CAPTCHA.
     * MUST be called from the JavaFX Application Thread.
     * @return WritableImage containing the CAPTCHA.
     */
    public WritableImage generateCaptchaImage() {
        Canvas canvas = new Canvas(width, height);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        // 1. Fill background
        gc.setFill(Color.web("#f4f4f4"));
        gc.fillRect(0, 0, width, height);

        // 2. Generate Math problem
        int num1 = random.nextInt(10); // 0-9
        int num2 = random.nextInt(10); // 0-9
        int operatorType = random.nextInt(3); // 0: +, 1: -, 2: *
        String operator = "+";
        
        // Ensure positive result for subtraction
        if (operatorType == 1 && num1 < num2) {
            int temp = num1;
            num1 = num2;
            num2 = temp;
        }

        switch (operatorType) {
            case 0:
                operator = "+";
                currentAnswer = String.valueOf(num1 + num2);
                break;
            case 1:
                operator = "-";
                currentAnswer = String.valueOf(num1 - num2);
                break;
            case 2:
                operator = "*";
                currentAnswer = String.valueOf(num1 * num2);
                break;
        }

        String captchaText = num1 + " " + operator + " " + num2 + " = ?";

        // 3. Draw text with some distortion
        gc.setFont(Font.font("Courier New", FontWeight.BOLD, 28));
        for (int i = 0; i < captchaText.length(); i++) {
            gc.setFill(Color.color(random.nextDouble() * 0.5, random.nextDouble() * 0.5, random.nextDouble() * 0.5));
            // Slight random rotation/offset
            gc.save();
            gc.translate(20 + (i * 20), 35 + random.nextInt(10) - 5);
            gc.rotate(random.nextInt(30) - 15);
            gc.fillText(String.valueOf(captchaText.charAt(i)), 0, 0);
            gc.restore();
        }

        // 4. Draw noise (lines)
        for (int i = 0; i < 6; i++) {
            gc.setStroke(Color.color(random.nextDouble(), random.nextDouble(), random.nextDouble(), 0.5));
            gc.setLineWidth(1.5);
            gc.strokeLine(random.nextInt(width), random.nextInt(height), random.nextInt(width), random.nextInt(height));
        }

        // 5. Draw noise (dots)
        for (int i = 0; i < 50; i++) {
            gc.setFill(Color.color(random.nextDouble(), random.nextDouble(), random.nextDouble(), 0.5));
            gc.fillOval(random.nextInt(width), random.nextInt(height), 2, 2);
        }

        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        return canvas.snapshot(params, null);
    }

    public String getCurrentAnswer() {
        return currentAnswer;
    }
}
