package com.pidev.utils;

import com.pidev.models.IndividualRankingEntry;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.io.File;
import java.net.URL;
import java.util.List;

public class IndividualRankingStageController {
    @FXML
    private Label challengeTitleLabel;
    @FXML
    private StackPane firstAvatarContainer;
    @FXML
    private StackPane secondAvatarContainer;
    @FXML
    private StackPane thirdAvatarContainer;
    @FXML
    private Label firstBadgeLabel;
    @FXML
    private Label secondBadgeLabel;
    @FXML
    private Label thirdBadgeLabel;
    @FXML
    private Label firstNameLabel;
    @FXML
    private Label secondNameLabel;
    @FXML
    private Label thirdNameLabel;
    @FXML
    private Label firstScoreLabel;
    @FXML
    private Label secondScoreLabel;
    @FXML
    private Label thirdScoreLabel;
    @FXML
    private Label yourRankValueLabel;
    @FXML
    private Label yourRankDescriptionLabel;
    @FXML
    private VBox rankingListContainer;
    @FXML
    private Label emptyStateLabel;

    public void setData(String challengeTitle, List<IndividualRankingEntry> ranking, int currentUserId) {
        challengeTitleLabel.setText(challengeTitle == null || challengeTitle.isBlank()
                ? "Passed students ranked by final score"
                : challengeTitle + " - Passed students ranked by final score");

        setPodiumSlot(ranking, 0, firstAvatarContainer, firstBadgeLabel, firstNameLabel, firstScoreLabel, "#f59e0b");
        setPodiumSlot(ranking, 1, secondAvatarContainer, secondBadgeLabel, secondNameLabel, secondScoreLabel, "#cbd5e1");
        setPodiumSlot(ranking, 2, thirdAvatarContainer, thirdBadgeLabel, thirdNameLabel, thirdScoreLabel, "#d97706");
        setCurrentUserRank(ranking, currentUserId);
        populateRankingList(ranking, currentUserId);
    }

    private void setPodiumSlot(List<IndividualRankingEntry> ranking,
                               int index,
                               StackPane avatarContainer,
                               Label badgeLabel,
                               Label nameLabel,
                               Label scoreLabel,
                               String accentColor) {
        IndividualRankingEntry entry = ranking.size() > index ? ranking.get(index) : null;
        badgeLabel.setText(entry == null ? "--" : String.valueOf(entry.getRank()));
        nameLabel.setText(entry == null ? "--" : entry.getDisplayName());
        scoreLabel.setText(entry == null ? "--" : String.format("%.2f", entry.getFinalScore()));
        renderAvatar(avatarContainer, entry, accentColor);
    }

    private void setCurrentUserRank(List<IndividualRankingEntry> ranking, int currentUserId) {
        IndividualRankingEntry currentUserEntry = null;
        for (IndividualRankingEntry entry : ranking) {
            if (entry.getUserId() == currentUserId) {
                currentUserEntry = entry;
                break;
            }
        }

        if (currentUserEntry == null) {
            yourRankValueLabel.setText("-- / " + ranking.size());
            yourRankDescriptionLabel.setText(ranking.isEmpty()
                    ? "No student has passed this challenge yet."
                    : "You are not part of the passed students ranking for this challenge.");
            return;
        }

        yourRankValueLabel.setText(currentUserEntry.getRank() + " / " + ranking.size());
        yourRankDescriptionLabel.setText(currentUserEntry.getDisplayName()
                + " - Final score " + String.format("%.2f", currentUserEntry.getFinalScore()));
    }

    private void populateRankingList(List<IndividualRankingEntry> ranking, int currentUserId) {
        rankingListContainer.getChildren().clear();

        boolean empty = ranking.isEmpty();
        emptyStateLabel.setVisible(empty);
        emptyStateLabel.setManaged(empty);
        if (empty) {
            return;
        }

        for (IndividualRankingEntry entry : ranking) {
            HBox row = new HBox(16);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setStyle(entry.getUserId() == currentUserId
                    ? "-fx-background-color: linear-gradient(to right, #eef2ff, #ffffff);"
                      + "-fx-background-radius: 22; -fx-border-color: #c7d2fe; -fx-border-radius: 22;"
                      + "-fx-padding: 14 18; -fx-effect: dropshadow(three-pass-box, rgba(79,70,229,0.10), 16, 0, 0, 6);"
                    : "-fx-background-color: white; -fx-background-radius: 22; -fx-border-color: #e2e8f0;"
                      + "-fx-border-radius: 22; -fx-padding: 14 18; -fx-effect: dropshadow(three-pass-box, rgba(15,23,42,0.05), 16, 0, 0, 6);");

            Label rankLabel = new Label(String.valueOf(entry.getRank()));
            rankLabel.setMinSize(38, 38);
            rankLabel.setPrefSize(38, 38);
            rankLabel.setAlignment(Pos.CENTER);
            rankLabel.setStyle("-fx-background-color: #0f172a; -fx-background-radius: 999;"
                    + "-fx-text-fill: white; -fx-font-size: 16; -fx-font-weight: 800;");

            StackPane avatar = new StackPane();
            avatar.setMinSize(48, 48);
            avatar.setPrefSize(48, 48);
            renderAvatar(avatar, entry, "#1d4ed8");

            Label nameLabel = new Label(entry.getDisplayName());
            nameLabel.setStyle("-fx-text-fill: #0f172a; -fx-font-size: 16; -fx-font-weight: 800;");

            Label currentUserTag = new Label(entry.getUserId() == currentUserId ? "YOU" : "");
            currentUserTag.setStyle("-fx-background-color: #dbeafe; -fx-text-fill: #1d4ed8; -fx-font-size: 11;"
                    + "-fx-font-weight: 800; -fx-padding: 4 10; -fx-background-radius: 999;");
            currentUserTag.setVisible(entry.getUserId() == currentUserId);
            currentUserTag.setManaged(entry.getUserId() == currentUserId);

            HBox nameBox = new HBox(10, nameLabel, currentUserTag);
            nameBox.setAlignment(Pos.CENTER_LEFT);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Label scoreLabel = new Label(String.format("%.2f", entry.getFinalScore()));
            scoreLabel.setStyle("-fx-text-fill: #0f172a; -fx-font-size: 18; -fx-font-weight: 800;");

            row.getChildren().addAll(rankLabel, avatar, nameBox, spacer, scoreLabel);
            rankingListContainer.getChildren().add(row);
        }
    }

    private void renderAvatar(StackPane container, IndividualRankingEntry entry, String accentColor) {
        container.getChildren().clear();

        double size = container.getPrefWidth() > 0 ? container.getPrefWidth() : 72;
        Circle border = new Circle(size / 2);
        border.setFill(Color.web("#1f2937"));
        border.setStroke(Color.web(accentColor));
        border.setStrokeWidth(2.5);

        Image photo = entry == null ? null : loadPhoto(entry.getPhotoPath());
        if (photo != null && !photo.isError()) {
            ImageView imageView = new ImageView(photo);
            imageView.setFitWidth(size - 6);
            imageView.setFitHeight(size - 6);
            imageView.setPreserveRatio(false);

            Circle clip = new Circle((size - 6) / 2);
            clip.setCenterX((size - 6) / 2);
            clip.setCenterY((size - 6) / 2);
            imageView.setClip(clip);

            container.getChildren().addAll(border, imageView);
            return;
        }

        Label initialsLabel = new Label(entry == null ? "?" : entry.getInitials());
        initialsLabel.setStyle("-fx-text-fill: white; -fx-font-size: 20; -fx-font-weight: 800;");
        container.getChildren().addAll(border, initialsLabel);
    }

    private Image loadPhoto(String photoPath) {
        if (photoPath == null || photoPath.isBlank()) {
            return null;
        }

        URL directResource = getClass().getResource(photoPath);
        if (directResource != null) {
            return new Image(directResource.toExternalForm(), true);
        }

        URL memberResource = getClass().getResource("/images/members/" + photoPath);
        if (memberResource != null) {
            return new Image(memberResource.toExternalForm(), true);
        }

        File file = new File(photoPath);
        if (file.exists()) {
            return new Image(file.toURI().toString(), true);
        }

        return null;
    }
}
