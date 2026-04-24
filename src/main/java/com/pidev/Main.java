package com.pidev;

import com.pidev.Controllers.client.BaseController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import com.pidev.utils.DataSource;
import com.pidev.utils.hackthon.ReminderScheduler;
import java.io.IOException;
import java.sql.Connection;
public class Main extends Application {
    @Override
    public void start(Stage primaryStage) {
        // Start the background reminder scheduler
        ReminderScheduler.start();
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
}