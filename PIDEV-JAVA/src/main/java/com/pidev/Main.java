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
            // Check auto-login first
            java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userNodeForPackage(com.pidev.Controllers.client.User.login_Controller.class);
            String savedEmail = prefs.get("savedEmail", null);
            String savedPassword = prefs.get("savedPassword", null);
            
            boolean autoLoginSuccess = false;
            if (savedEmail != null && savedPassword != null) {
                com.pidev.Services.UserService userService = new com.pidev.Services.UserService();
                com.pidev.models.User user = userService.login(savedEmail, savedPassword);
                
                if (user != null && !user.isBanned()) {
                    com.pidev.utils.SessionManager.getInstance().setUser(user);
                    userService.setConnectedStatus(user.getId(), true); // Mark as online
                    
                    if (user.getRole() == com.pidev.models.User.Role.ADMIN) {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/Fxml/admin/base_back.fxml"));
                        Parent root = loader.load();
                        com.pidev.Controllers.admin.BaseController controller = loader.getController();
                        root.setUserData(controller);
                        primaryStage.setTitle("Admin Dashboard");
                        primaryStage.setScene(new Scene(root));
                        primaryStage.show();
                        autoLoginSuccess = true;
                    } else {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/Fxml/client/base.fxml"));
                        Parent root = loader.load();
                        com.pidev.Controllers.client.BaseController controller = loader.getController();
                        controller.loadHome();
                        primaryStage.setTitle("Skill Bridge");
                        primaryStage.setScene(new Scene(root));
                        primaryStage.show();
                        autoLoginSuccess = true;
                    }
                }
            }

            if (!autoLoginSuccess) {
                // Regular start
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/Fxml/client/base.fxml"));
                Parent root = loader.load();
                BaseController controller = loader.getController();
                controller.loadHome();
                Scene scene = new Scene(root);
                primaryStage.setTitle("Skill Bridge");
                primaryStage.setScene(scene);
                primaryStage.show();
            }

        } catch (Exception e) {
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