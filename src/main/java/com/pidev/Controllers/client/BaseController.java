package com.pidev.Controllers.client;

import com.pidev.utils.SessionManager;
import com.pidev.Services.UserService;
import com.pidev.models.Notif;
import com.pidev.Services.NotificationService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ButtonBar;
import java.util.Optional;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import com.pidev.models.User;
import java.io.File;

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
    private MenuItem cvMenuItem;
    @FXML
    private MenuItem jobsMenuItem;
    @FXML
    private Label dashboardLink;
    @FXML
    private Label profileLink;
    @FXML
    private Button loginBtn;

    @FXML private HBox userInfoNav;
    @FXML private ImageView navProfileImg;
    @FXML private Label navUserName;
    @FXML private Label navUserRole;

    @FXML
    private VBox notificationPanel;
    @FXML
    private VBox notificationList;
    @FXML
    private Label notificationBadge;
    @FXML
    private StackPane notificationIconWrapper;

    private UserService userService = new UserService();
    private final NotificationService notificationService = new NotificationService();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        configureHoverMenu(challengesMenu);
        configureHoverMenu(CvMenu);
        updateNavbar();

        // Initial notification load if user is logged in
        if (SessionManager.getInstance().isLogged()) {
            refreshNotifications();
            // Start polling for new notifications every 30 seconds
            scheduler.scheduleAtFixedRate(this::refreshNotifications, 30, 30, TimeUnit.SECONDS);
        }
    }

    private void updateNavbar() {
        boolean isAdmin = SessionManager.getInstance().isAdmin();
        boolean isLogged = SessionManager.getInstance().isLogged();
        User user = SessionManager.getInstance().getUser();

        dashboardLink.setVisible(isAdmin);
        dashboardLink.setManaged(isAdmin);

        profileLink.setVisible(isLogged);
        profileLink.setManaged(isLogged);

        if (userInfoNav != null) {
            userInfoNav.setVisible(isLogged);
            userInfoNav.setManaged(isLogged);
        }

        if (notificationIconWrapper != null) {
            notificationIconWrapper.setVisible(isLogged);
            notificationIconWrapper.setManaged(isLogged);
        }

        // Role-based menu adaptation
        if (isLogged && user != null) {
            if (user.getRole() == User.Role.ENTREPRISE) {
                CvMenu.setText("Gestion Offres");
                cvMenuItem.setVisible(false);
                jobsMenuItem.setText("Mes Offres");
            } else if (user.getRole() == User.Role.STUDENT) {
                CvMenu.setText("Jobs & CV");
                cvMenuItem.setVisible(true);
                jobsMenuItem.setText("Jobs");
            }
        } else {
            // Default or guest state
            CvMenu.setText("Jobs & CV");
            cvMenuItem.setVisible(true);
            jobsMenuItem.setText("Jobs");
        }

        if (isLogged) {
            loginBtn.setText("Logout");

            if (navUserName != null && user != null) {
                navUserName.setText(user.getNom() + " " + user.getPrenom());
                navUserRole.setText(user.getRole() != null ? user.getRole().name() : "");

                if (user.getPhoto() != null && !user.getPhoto().isEmpty()) {
                    try {
                        String photoPath = user.getPhoto();
                        if (photoPath.startsWith("http://") || photoPath.startsWith("https://")) {
                            navProfileImg.setImage(new Image(photoPath, true));
                        } else {
                            File file = new File(photoPath);
                            if (file.exists()) {
                                navProfileImg.setImage(new Image(file.toURI().toString()));
                            }
                        }
                    } catch (Exception ignored) {}
                }
            }
        } else {
            loginBtn.setText("Sign in");
        }
    }

    private void refreshNotifications() {
        User user = SessionManager.getInstance().getUser();
        if (user == null) return;

        try {
            List<Notif> notifications = notificationService.getNotificationsForUser(user.getId());
            int unreadCount = notificationService.getUnreadCount(user.getId());

            Platform.runLater(() -> {
                updateNotificationBadge(unreadCount);
                updateNotificationList(notifications);
            });
        } catch (SQLException e) {
            System.err.println("Error fetching notifications: " + e.getMessage());
        }
    }

    private void updateNotificationBadge(int count) {
        if (notificationBadge == null) return;
        if (count > 0) {
            notificationBadge.setText(String.valueOf(count));
            notificationBadge.setVisible(true);
        } else {
            notificationBadge.setVisible(false);
        }
    }

    private void updateNotificationList(List<Notif> notifications) {
        if (notificationList == null) return;
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
        if (notificationPanel == null) return;
        boolean isVisible = notificationPanel.isVisible();
        notificationPanel.setVisible(!isVisible);
        notificationPanel.setManaged(!isVisible);
        if (!isVisible) {
            refreshNotifications();
        }
    }

    @FXML
    private void markAllAsRead() {
        User user = SessionManager.getInstance().getUser();
        if (user == null) return;
        try {
            notificationService.markAllAsRead(user.getId());
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

    private void loadViewFront(String fxmlName) {
        try {
            java.net.URL resource = getClass().getResource("/Fxml/" + fxmlName + ".fxml");
            if (resource == null) {
                System.err.println("Missing FXML view: /Fxml/" + fxmlName + ".fxml");
                return;
            }

            Parent view = FXMLLoader.load(resource);
            contentArea.getChildren().setAll(view);

        } catch (IOException e) {
            System.err.println("Error: Could not load " + fxmlName + ". Check the path.");
            e.printStackTrace();
        }
    }

    private void switchRoot(String fxmlName) {
        try {
            java.net.URL resource = getClass().getResource("/Fxml/" + fxmlName + ".fxml");
            if (resource == null) {
                System.err.println("Missing FXML view: /Fxml/" + fxmlName + ".fxml");
                return;
            }

            Parent root = FXMLLoader.load(resource);
            contentArea.getScene().setRoot(root);
        } catch (IOException e) {
            System.err.println("Error: Could not switch to " + fxmlName + ". Check the path.");
            e.printStackTrace();
        }
    }

    @FXML public void loadDashboard() { switchRoot("admin/base_back"); }
    @FXML public void loadHome() { loadViewFront("client/home"); }

    @FXML public void loadProfile() {
        if (!checkAuth()) return;
        loadViewFront("client/User/profile");
    }

    @FXML public void loadCourses() {
        if (!checkAuth()) return;
        loadViewFront("client/CoursesView");
    }

    @FXML public void loadGroups() {
        if (!checkAuth()) return;
        loadViewFront("client/GroupsView");
    }

    @FXML public void loadChallenge() {
        if (!checkAuth()) return;
        loadViewFront("client/Challenge");
    }

    @FXML public void loadMyCV() {
        if (!checkAuth()) return;
        loadViewFront("client/MyCVView");
    }

    @FXML public void loadJobs() {
        User user = SessionManager.getInstance().getUser();
        if (user != null && user.getRole() == User.Role.ENTREPRISE) {
            loadViewFront("client/MyOffers");
        } else {
            loadViewFront("client/OfferList");
        }
    }

    @FXML public void loadHackathon() {
        if (!checkAuth()) return;
        loadViewFront("client/HackathonView");
    }

    private boolean checkAuth() {
        if (!SessionManager.getInstance().isLogged()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Accès restreint");
            alert.setHeaderText(null);
            alert.setContentText("Vous devez être connecté pour accéder à cette fonctionnalité.");
            alert.showAndWait();
            return false;
        }
        return true;
    }
    @FXML public void loadLogin() {
        if (SessionManager.getInstance().isLogged()) {
            handleLogout();
        } else {
            switchRoot("client/User/login");
        }
    }

    private void handleLogout() {
        User user = SessionManager.getInstance().getUser();
        if (user != null) {
            userService.setConnectedStatus(user.getId(), false);
        }
        SessionManager.getInstance().logout();
        switchRoot("client/User/login");
    }
}
