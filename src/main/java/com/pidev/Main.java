package com.pidev;

import com.pidev.Controllers.client.BaseController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import com.pidev.utils.DataSource;
import java.io.IOException;
import java.sql.Connection;
public class Main extends Application {
    @Override
    public void start(Stage primaryStage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Fxml/client/base.fxml"));
            Parent root = loader.load();
            BaseController controller = loader.getController();
            controller.loadHome();
            Scene scene = new Scene(root);
            primaryStage.setTitle("Skill Bridge");
            primaryStage.setScene(scene);
            primaryStage.show();

        } catch (IOException e) {
            System.err.println("Could not load base.fxml. Check the path in src/main/resources/Fxml/");
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        configureJavaFxRendering();
        System.out.println("--- Testing Database Connection ---");
        DataSource ds = DataSource.getInstance();
        Connection conn = ds.getConnection();

        if (conn != null) {
            System.out.println("SUCCESS: Connection is ready for Skill Bridge!");
        } else {
            System.out.println("FAILURE: Connection failed. Check XAMPP and your Credentials.");
        }
        System.out.println("-----------------------------------");

        launch(args);
    }

    private static void configureJavaFxRendering() {
        if (System.getProperty("prism.order") == null) {
            // Prefer the hardware pipeline on Windows before falling back to software.
            System.setProperty("prism.order", "d3d,sw");
        }
    }
}
