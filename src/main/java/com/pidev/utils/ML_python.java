package com.pidev.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ML_python {

    private static final String DEFAULT_PYTHON_EXE =
            "C:\\Program Files\\Python311\\python.exe";

    private static final String DEFAULT_SCRIPT_PATH =
            "D:\\javaFX\\python ML\\scripts\\predict.py";

    private static final String DEFAULT_WORKING_DIR =
            "D:\\javaFX\\python ML";

    private static final int PREDICTION_TIMEOUT_SECONDS = 15;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<List<PredictionResult>> PREDICTION_RESULT_LIST_TYPE = new TypeReference<>() {};

    public static PredictionResult predict(String jsonInput) throws Exception {
        String predictionOutput = runPrediction(jsonInput);
        return OBJECT_MAPPER.readValue(predictionOutput, PredictionResult.class);
    }

    public static List<PredictionResult> predictAll(String jsonInput) throws Exception {
        String predictionOutput = runPrediction(jsonInput);
        if (predictionOutput.startsWith("[")) {
            return OBJECT_MAPPER.readValue(predictionOutput, PREDICTION_RESULT_LIST_TYPE);
        }
        return List.of(OBJECT_MAPPER.readValue(predictionOutput, PredictionResult.class));
    }

    private static String runPrediction(String jsonInput) throws Exception {
        if (jsonInput == null || jsonInput.isBlank()) {
            throw new IllegalArgumentException("Prediction input JSON cannot be empty.");
        }

        String pythonExe = resolvePath("ml.python.exe", "ML_PYTHON_EXE", DEFAULT_PYTHON_EXE);
        String scriptPath = resolvePath("ml.python.script", "ML_PYTHON_SCRIPT", DEFAULT_SCRIPT_PATH);
        String workingDir = resolvePath("ml.python.workdir", "ML_PYTHON_WORKDIR", DEFAULT_WORKING_DIR);

        validateReadableFile(pythonExe, "Python executable");
        validateReadableFile(scriptPath, "Prediction script");
        validateReadableDirectory(workingDir, "Python working directory");
        validateReadableFile(Path.of(workingDir, "artifacts", "model.pkl").toString(), "Trained model");
        validateReadableFile(Path.of(workingDir, "artifacts", "meta.pkl").toString(), "Prediction metadata");

        ProcessBuilder pb = new ProcessBuilder(pythonExe, scriptPath);
        pb.directory(new File(workingDir));
        pb.redirectErrorStream(true);

        Process process = pb.start();

        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8))) {
            writer.write(jsonInput);
            writer.flush();
        }

        StringBuilder output = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }
        }

        boolean finished = process.waitFor(PREDICTION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new RuntimeException("Python prediction timed out.");
        }

        int exitCode = process.exitValue();
        if (exitCode != 0) {
            throw new RuntimeException("Python failed: " + output);
        }

        String predictionOutput = output.toString().trim();
        if (predictionOutput.isEmpty()) {
            throw new RuntimeException("Python returned an empty prediction response.");
        }
        return predictionOutput;
    }

    private static String resolvePath(String systemPropertyName, String environmentVariableName, String fallbackValue) {
        String systemPropertyValue = System.getProperty(systemPropertyName);
        if (systemPropertyValue != null && !systemPropertyValue.isBlank()) {
            return systemPropertyValue;
        }

        String environmentValue = System.getenv(environmentVariableName);
        if (environmentValue != null && !environmentValue.isBlank()) {
            return environmentValue;
        }

        return fallbackValue;
    }

    private static void validateReadableFile(String pathValue, String label) {
        Path path = Path.of(pathValue);
        if (!Files.isRegularFile(path)) {
            throw new IllegalStateException(label + " was not found: " + path);
        }
    }

    private static void validateReadableDirectory(String pathValue, String label) {
        Path path = Path.of(pathValue);
        if (!Files.isDirectory(path)) {
            throw new IllegalStateException(label + " was not found: " + path);
        }
    }

    public static class PredictionResult {
        public int prediction;
        public String label;
        public double fail_percentage;
        public double success_percentage;

        public int getPrediction() {
            return prediction;
        }

        public String getLabel() {
            return label;
        }

        public double getFail_percentage() {
            return fail_percentage;
        }

        public double getSuccess_percentage() {
            return success_percentage;
        }

        @Override
        public String toString() {
            return "PredictionResult{" +
                    "prediction=" + prediction +
                    ", label='" + label + '\'' +
                    ", fail_percentage=" + fail_percentage +
                    ", success_percentage=" + success_percentage +
                    '}';
        }
    }
}
