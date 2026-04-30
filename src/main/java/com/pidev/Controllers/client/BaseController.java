package com.pidev.Controllers.client;

import com.pidev.models.Notif;
import com.pidev.Services.NotificationService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.control.MenuButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class BaseController implements Initializable {

    @FXML
    private StackPane contentArea;

    @FXML
    private MenuButton challengesMenu;

    @FXML
    private MenuButton CvMenu;

    @FXML
    private VBox notificationPanel;

    @FXML
    private VBox notificationList;

    @FXML
    private Label notificationBadge;

    private final NotificationService notificationService = new NotificationService();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    // Mock current user ID for now - should be replaced with actual logged in user session
    private static final int CURRENT_USER_ID = 1;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        configureHoverMenu(challengesMenu);
        configureHoverMenu(CvMenu);

        // Initial notification load
        refreshNotifications();

        // Start polling for new notifications every 30 seconds
        scheduler.scheduleAtFixedRate(this::refreshNotifications, 30, 30, TimeUnit.SECONDS);
    }

    private void refreshNotifications() {
        try {
            List<Notif> notifications = notificationService.getNotificationsForUser(CURRENT_USER_ID);
            int unreadCount = notificationService.getUnreadCount(CURRENT_USER_ID);

            Platform.runLater(() -> {
                updateNotificationBadge(unreadCount);
                updateNotificationList(notifications);
            });
        } catch (SQLException e) {
            System.err.println("Error fetching notifications: " + e.getMessage());
        }
    }

    private void updateNotificationBadge(int count) {
        if (count > 0) {
            notificationBadge.setText(String.valueOf(count));
            notificationBadge.setVisible(true);
        } else {
            notificationBadge.setVisible(false);
        }
    }

    private void updateNotificationList(List<Notif> notifications) {
        notificationList.getChildren().clear();
        if (notifications.isEmpty()) {
            Label emptyLabel = new Label("No notifications yet");
            emptyLabel.setStyle("-fx-padding: 20; -fx-text-fill: #94A3B8;");
            notificationList.getChildren().add(emptyLabel);
            return;
        }

        for (Notif n : notifications) {
            notificationList.getChildren().add(createNotificationItem(n));
        }
    }

    private VBox createNotificationItem(Notif n) {
        VBox item = new VBox(5);
        item.getStyleClass().add("notification-item");
        if (!n.isIs_read()) {
            item.getStyleClass().add("notification-item-unread");
        }

        HBox header = new HBox(10);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label message = new Label(n.getMessage());
        message.getStyleClass().add("notification-message");
        message.setWrapText(true);
        HBox.setHgrow(message, Priority.ALWAYS);

        Region dot = new Region();
        dot.getStyleClass().add("notification-dot");
        dot.setVisible(!n.isIs_read());

        header.getChildren().addAll(message, dot);

        Label time = new Label(getTimeAgo(n.getCreated_at()));
        time.getStyleClass().add("notification-time");

        item.getChildren().addAll(header, time);

        item.setOnMouseClicked(event -> {
            try {
                if (!n.isIs_read()) {
                    notificationService.markAsRead(n.getId());
                    refreshNotifications();
                }
                // Handle navigation based on notification type if needed
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });

        return item;
    }

    private String getTimeAgo(LocalDateTime dateTime) {
        Duration duration = Duration.between(dateTime, LocalDateTime.now());
        if (duration.toMinutes() < 1) return "Just now";
        if (duration.toMinutes() < 60) return duration.toMinutes() + "m ago";
        if (duration.toHours() < 24) return duration.toHours() + "h ago";
        return duration.toDays() + "d ago";
    }

    @FXML
    private void toggleNotificationPanel() {
        boolean isVisible = notificationPanel.isVisible();
        notificationPanel.setVisible(!isVisible);
        notificationPanel.setManaged(!isVisible);
        if (!isVisible) {
            refreshNotifications();
        }
    }

    @FXML
    private void markAllAsRead() {
        try {
            notificationService.markAllAsRead(CURRENT_USER_ID);
            refreshNotifications();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void configureHoverMenu(MenuButton menuButton) {
        if (menuButton == null) {
            return;
        }

        menuButton.setOnMouseEntered(event -> menuButton.show());
        menuButton.setOnMouseExited(event -> {
            if (!menuButton.isShowing()) {
                menuButton.hide();
            }
        });
    }

    // 🔹 Charger une vue dans le contentArea
    private void loadViewFront(String fxmlName) {
        Platform.runLater(() -> {
            try {
                System.out.println("Attempting to load view: " + fxmlName);

                URL url = getClass().getResource("/Fxml/" + fxmlName + ".fxml");

                if (url == null) {
                    System.err.println("❌ FXML not found: /Fxml/" + fxmlName + ".fxml");
                    return;
                }

                FXMLLoader loader = new FXMLLoader(url);
                Parent view = loader.load();

                if (contentArea != null) {
                    contentArea.getChildren().setAll(view);
                    System.out.println("✅ View loaded: " + fxmlName);
                } else {
                    System.err.println("❌ contentArea is null!");
                }

            } catch (Exception e) {
                System.err.println("❌ Error loading " + fxmlName);
                e.printStackTrace();
                Platform.runLater(() -> {
                    javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
                    alert.setTitle("Erreur de chargement");
                    alert.setHeaderText("Impossible de charger la vue: " + fxmlName);
                    alert.setContentText(e.toString());
                    alert.showAndWait();
                });
            }
        });
    }

    // 🔹 Changer toute la scène (root)
    private void switchRoot(String fxmlName) {
        try {
            Parent root = FXMLLoader.load(
                    getClass().getResource("/Fxml/" + fxmlName + ".fxml")
            );
            contentArea.getScene().setRoot(root);

        } catch (IOException e) {
            System.err.println("❌ Could not switch to " + fxmlName);
            e.printStackTrace();
        }
    }

    // 🔥 EVENTS (IMPORTANT)

    @FXML
    public void loadDashboard() {
        switchRoot("admin/base_back");
    }

    @FXML
    public void loadHome() {
        loadViewFront("client/home");
    }

    @FXML
    public void loadLogin() {
        loadViewFront("client/User/login");
    }

    @FXML
    public void loadCourses() {
        loadViewFront("client/CoursesView");
    }

    // 🔥 CORRECTION ICI (manquait)
    @FXML
    public void loadGroups() {
        loadViewFront("client/GroupsView"); // assure-toi que le fichier existe
    }

    @FXML
    public void loadMyCV() {
        loadViewFront("client/MyCVView");
    }

    @FXML
    public void loadJobs() {
        loadViewFront("client/OfferList");
    }

    @FXML
    public void loadMyOffers() {
        loadViewFront("client/MyOffers");
    }

    @FXML
    public void loadChallenge() {
        loadViewFront("client/Challenge");
    }

    @FXML
    public void loadHackathon() {
        loadViewFront("client/HackathonView");
    }
}