package com.pidev.Services;

import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.opencv_face.*;
import org.bytedeco.opencv.opencv_objdetect.*;
import static org.bytedeco.opencv.global.opencv_core.*;
import static org.bytedeco.opencv.global.opencv_imgcodecs.*;
import static org.bytedeco.opencv.global.opencv_imgproc.*;

import java.io.File;
import java.io.FilenameFilter;
import java.nio.IntBuffer;

public class FaceRecognitionService {

    private FaceRecognizer faceRecognizer;
    private CascadeClassifier faceDetector;
    private String trainingDir = "src/main/resources/images/face_id";

    public FaceRecognitionService() {
        faceRecognizer = LBPHFaceRecognizer.create();
        
        // Ensure ml dir exists
        File mlDir = new File("src/main/resources/ml");
        if (!mlDir.exists()) mlDir.mkdirs();
        
        File classifierFile = new File("src/main/resources/ml/haarcascade_frontalface_default.xml");
        if (classifierFile.exists()) {
            faceDetector = new CascadeClassifier(classifierFile.getAbsolutePath());
        } else {
            System.err.println("Haar Cascade XML non trouvé !");
        }

        File dir = new File(trainingDir);
        if (!dir.exists()) dir.mkdirs();
    }

    public void trainModel() {
        File root = new File(trainingDir);
        FilenameFilter imgFilter = (dir, name) -> {
            name = name.toLowerCase();
            return name.endsWith(".jpg") || name.endsWith(".png");
        };

        File[] imageFiles = root.listFiles(imgFilter);
        if (imageFiles == null || imageFiles.length == 0) {
            System.out.println("Aucune image d'entraînement trouvée.");
            return;
        }

        MatVector images = new MatVector(imageFiles.length);
        Mat labels = new Mat(imageFiles.length, 1, CV_32SC1);
        IntBuffer labelsBuf = labels.createBuffer();

        int counter = 0;
        for (File image : imageFiles) {
            Mat img = imread(image.getAbsolutePath(), IMREAD_GRAYSCALE);
            
            // Appliquer l'égalisation d'histogramme pour améliorer la précision (contraste/luminosité)
            equalizeHist(img, img);
            
            // Extract user ID from filename: e.g., "12_1.jpg" -> id = 12
            int label = Integer.parseInt(image.getName().split("\\_")[0]);

            images.put(counter, img);
            labelsBuf.put(counter, label);
            counter++;
        }

        faceRecognizer.train(images, labels);
        faceRecognizer.save(trainingDir + "/face_model.yml");
        System.out.println("Modèle Face ID entraîné avec " + counter + " images.");
    }

    public void loadModel() {
        File modelFile = new File(trainingDir + "/face_model.yml");
        if (modelFile.exists()) {
            faceRecognizer.read(modelFile.getAbsolutePath());
        } else {
            System.out.println("Aucun modèle Face ID trouvé, entraînement requis.");
        }
    }

    /**
     * @return User ID if matched confidently, -1 otherwise
     */
    public int recognize(Mat grayImage) {
        if (faceRecognizer == null) return -1;

        int[] label = new int[1];
        double[] confidence = new double[1];
        // Apply histogram equalization before prediction for consistency with training
        Mat equalizedImage = new Mat();
        equalizeHist(grayImage, equalizedImage);

        faceRecognizer.predict(equalizedImage, label, confidence);

        System.out.println("Face ID - Utilisateur: " + label[0] + " Confiance: " + confidence[0]);
        
        // LBPH distance (confidence). 0 is a perfect match.
        // For a single-user dataset, LBPH will always return the only user's ID.
        // The distance depends heavily on lighting. A distance < 35.0 is usually safe for "same person".
        if (confidence[0] < 35.0) {
            return label[0];
        }
        return -1;
    }

    public Rect[] detectFaces(Mat frame) {
        RectVector faces = new RectVector();
        faceDetector.detectMultiScale(frame, faces, 1.1, 3, 0, new Size(100, 100), new Size(500, 500));
        Rect[] faceArray = new Rect[(int) faces.size()];
        for (int i = 0; i < faces.size(); i++) {
            faceArray[i] = faces.get(i);
        }
        return faceArray;
    }
}
