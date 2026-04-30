package com.pidev.Controllers.admin;

import com.pidev.Services.UserService;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;

import java.net.URL;
import java.util.ResourceBundle;

public class DashboardController implements Initializable {

    @FXML private Label userCountLabel;
    @FXML private Label connectedUserCountLabel;

    private UserService userService = new UserService();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        loadStatistics();
    }

    private void loadStatistics() {
        System.out.println("Debug: Dashboard - Requesting stats from UserService...");
        int totalUsers = userService.getTotalUsersCount();
        int connectedUsers = userService.getConnectedUsersCount();
        
        System.out.println("Debug: Dashboard - Total: " + totalUsers + ", Connected: " + connectedUsers);

        userCountLabel.setText(String.valueOf(totalUsers));
        connectedUserCountLabel.setText(String.valueOf(connectedUsers));
    }
}
