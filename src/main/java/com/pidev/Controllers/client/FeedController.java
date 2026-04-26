package com.pidev.Controllers.client;

import com.pidev.Services.GroupService;
import com.pidev.Services.MembershipService;
import com.pidev.Services.PostService;
import com.pidev.Services.FightModerationService;
import com.pidev.Services.PostReactionService;
import com.pidev.Services.PerspectiveModerationService;
import com.pidev.models.Group;
import com.pidev.models.Membership;
import com.pidev.models.Post;
import com.pidev.models.ReactionType;
import com.pidev.utils.CurrentUserContext;
import com.pidev.utils.GroupViewContext;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.StringJoiner;

public class FeedController implements Initializable {
    private static final int MAX_TITLE_LENGTH = 80;
    private static final int MAX_CONTENT_LENGTH = 500;

    @FXML
    private TextField titleField;
    @FXML
    private TextArea descriptionArea;
    @FXML
    private ComboBox<String> visibilityCombo;
    @FXML
    private ComboBox<String> statusCombo;
    @FXML
    private TextField attachedFileField;
    @FXML
    private TextField groupDetailsIdField;
    @FXML
    private Button submitButton;
    @FXML
    private Button cancelEditButton;
    @FXML
    private Label formModeLabel;
    @FXML
    private Label feedbackLabel;
    @FXML
    private VBox postsContainer;
    @FXML
    private VBox myGroupsContainer;

    private final PostService postService = new PostService();
    private final GroupService groupService = new GroupService();
    private final MembershipService membershipService = new MembershipService();
    private final FightModerationService fightModerationService = new FightModerationService();
    private final PostReactionService postReactionService = new PostReactionService();
    private final PerspectiveModerationService perspectiveModerationService = new PerspectiveModerationService();

    private Post editingPost;
    private final Map<Integer, Group> groupsById = new HashMap<>();
    private final Set<Integer> myGroupIds = new HashSet<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        visibilityCombo.getItems().addAll("public");
        visibilityCombo.setValue("public");
        visibilityCombo.setDisable(true);

        statusCombo.getItems().addAll("active", "draft", "archived");
        statusCombo.setValue("active");
        formModeLabel.setText("Create Public Post");

        setFeedback("Main feed creates public posts. Use group page for private/group posts.", false);
        cancelEditButton.setVisible(false);
        cancelEditButton.setManaged(false);

        refreshData();
    }

    @FXML
    private void handleSubmitPost() {
        if (!CurrentUserContext.isLoggedIn()) {
            setFeedback("Please sign in first to create or edit posts.", true);
            return;
        }

        String title = clean(titleField.getText());
        String description = clean(descriptionArea.getText());
        String status = statusCombo.getValue();
        String attachedFile = clean(attachedFileField.getText());

        if (title.isEmpty() || description.isEmpty() || status == null) {
            setFeedback("Title, description and status are required.", true);
            return;
        }

        if (title.length() > MAX_TITLE_LENGTH) {
            setFeedback("Title must be " + MAX_TITLE_LENGTH + " characters max.", true);
            return;
        }

        if (description.length() > MAX_CONTENT_LENGTH) {
            setFeedback("Content must be " + MAX_CONTENT_LENGTH + " characters max.", true);
            return;
        }

        if (status.length() > 30) {
            setFeedback("Status must be 30 characters max.", true);
            return;
        }

        try {
            PerspectiveModerationService.ModerationDecision textDecision =
                    perspectiveModerationService.moderateText(title, description);
            if (!textDecision.allowed()) {
                setFeedback("Post blocked by text moderation: " + textDecision.reason(), true);
                return;
            }

            FightModerationService.ModerationDecision decision =
                    fightModerationService.moderatePost(title, description, attachedFile.isEmpty() ? null : attachedFile);
            if (!decision.allowed()) {
                setFeedback("Post blocked by moderation: " + decision.reason(), true);
                return;
            }

            if (editingPost == null) {
                Post newPost = new Post();
                newPost.setTitre(title);
                newPost.setDescription(description);
                newPost.setVisibility("public");
                newPost.setStatus(status);
                newPost.setAttachedFile(attachedFile.isEmpty() ? null : attachedFile);
                newPost.setGroupId(null);
                newPost.setAuthorId(currentUserId());
                newPost.setLikesCounter(0);
                postService.createPost(newPost);
                setFeedback("Public post created successfully.", false);
            } else {
                if (!"public".equalsIgnoreCase(editingPost.getVisibility()) || editingPost.getGroupId() != null) {
                    setFeedback("Edit group/private posts from the group page.", true);
                    return;
                }

                editingPost.setTitre(title);
                editingPost.setDescription(description);
                editingPost.setStatus(status);
                editingPost.setAttachedFile(attachedFile.isEmpty() ? null : attachedFile);
                editingPost.setVisibility("public");
                editingPost.setGroupId(null);
                editingPost.setAuthorId(currentUserId());
                postService.updatePost(editingPost);
                setFeedback("Post updated successfully.", false);
            }

            clearForm();
            refreshData();
        } catch (PerspectiveModerationService.ModerationException e) {
            setFeedback("Text moderation failed: " + e.getMessage(), true);
        } catch (FightModerationService.ModerationException e) {
            setFeedback("Moderation failed: " + e.getMessage(), true);
        } catch (Exception e) {
            setFeedback("Database error: " + e.getMessage(), true);
        }
    }

    @FXML
    private void handleBrowseAttachment() {
        try {
            if (postsContainer == null || postsContainer.getScene() == null) {
                setFeedback("Attachment picker is not ready yet. Try again.", true);
                return;
            }

            FileChooser chooser = new FileChooser();
            chooser.setTitle("Choose Post Image");
            chooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp")
            );

            File selected = chooser.showOpenDialog(postsContainer.getScene().getWindow());
            if (selected != null) {
                attachedFileField.setText(selected.getAbsolutePath());
            }
        } catch (Exception e) {
            setFeedback("Could not open attachment picker: " + e.getMessage(), true);
        }
    }

    @FXML
    private void handleCancelEdit() {
        clearForm();
        setFeedback("Edit cancelled.", false);
    }

    @FXML
    private void handleOpenGroupAdd() {
        GroupViewContext.clearEditingGroupId();
        navigateTo("/Fxml/client/GroupFormView.fxml");
    }

    @FXML
    private void handleOpenGroupDetails() {
        Integer selectedGroupId = parseInteger(groupDetailsIdField == null ? null : groupDetailsIdField.getText());
        if (selectedGroupId == null || selectedGroupId <= 0) {
            setFeedback("Enter a valid group ID to open details.", true);
            return;
        }

        GroupViewContext.setSelectedGroupId(selectedGroupId);
        GroupViewContext.clearEditingGroupId();
        navigateTo("/Fxml/client/GroupShowView.fxml");
    }

    private void refreshData() {
        try {
            loadMyGroups();
            loadPosts();
        } catch (Exception e) {
            setFeedback("Could not load feed data: " + e.getMessage(), true);
        }
    }

    private void loadMyGroups() throws Exception {
        groupsById.clear();
        myGroupIds.clear();

        for (Group group : groupService.findAll()) {
            if (group.getId() != null) {
                groupsById.put(group.getId(), group);
            }
        }

        List<Group> myGroups = new ArrayList<>();
        myGroups.addAll(groupService.findByLeaderId(currentUserId()));

        for (Membership membership : membershipService.findByUser(currentUserId())) {
            if (membership.getGroupId() == null) {
                continue;
            }
            Group group = groupsById.get(membership.getGroupId());
            if (group != null) {
                myGroups.add(group);
            }
        }

        Map<Integer, Group> unique = new LinkedHashMap<>();
        for (Group g : myGroups) {
            if (g.getId() != null) {
                unique.put(g.getId(), g);
                myGroupIds.add(g.getId());
            }
        }

        myGroupsContainer.getChildren().clear();
        for (Group group : unique.values()) {
            Button btn = new Button(group.getName() + " (#" + group.getId() + ")");
            btn.getStyleClass().add("group-link");
            btn.setMaxWidth(Double.MAX_VALUE);

            ImageView iconView = createImageView(group.getIcon(), 20, 20);
            if (iconView != null) {
                btn.setGraphic(iconView);
            }

            btn.setOnAction(e -> {
                GroupViewContext.setSelectedGroupId(group.getId());
                GroupViewContext.clearEditingGroupId();
                navigateTo("/Fxml/client/GroupShowView.fxml");
            });
            myGroupsContainer.getChildren().add(btn);
        }

        if (myGroupsContainer.getChildren().isEmpty()) {
            Label empty = new Label("You are not a member of any group yet.");
            empty.getStyleClass().add("post-meta");
            myGroupsContainer.getChildren().add(empty);
        }
    }

    private void loadPosts() {
        try {
            List<Post> posts = postService.findAllNewestFirst();
            List<Post> visiblePosts = posts.stream().filter(this::canUserSeePost).toList();
            renderPosts(visiblePosts);
        } catch (Exception e) {
            setFeedback("Could not load posts: " + e.getMessage(), true);
        }
    }

    private boolean canUserSeePost(Post post) {
        if (post == null) {
            return false;
        }

        if ("public".equalsIgnoreCase(post.getVisibility())) {
            return true;
        }

        Integer groupId = post.getGroupId();
        return groupId != null && myGroupIds.contains(groupId);
    }

    private void renderPosts(List<Post> posts) {
        postsContainer.getChildren().clear();
        if (posts.isEmpty()) {
            Label empty = new Label("No visible posts yet. Join a group or share something public.");
            empty.getStyleClass().add("empty-label");
            postsContainer.getChildren().add(empty);
            return;
        }

        for (Post post : posts) {
            postsContainer.getChildren().add(buildPostCard(post));
        }
    }

    private VBox buildPostCard(Post post) {
        VBox card = new VBox(10);
        card.getStyleClass().add("post-card");

        Group group = post.getGroupId() == null ? null : groupsById.get(post.getGroupId());

        HBox top = new HBox(10);
        ImageView groupIcon = createImageView(group == null ? null : group.getIcon(), 36, 36);
        if (groupIcon != null) {
            top.getChildren().add(groupIcon);
        }

        VBox topText = new VBox(2);
        Label author = new Label("Author #" + post.getAuthorId());
        author.getStyleClass().add("post-author");
        String groupText = group == null ? "Main Feed" : ("Group: " + safe(group.getName()));
        Label groupLabel = new Label(groupText);
        groupLabel.getStyleClass().add("post-meta");
        topText.getChildren().addAll(author, groupLabel);
        top.getChildren().add(topText);

        String metaText = "Visibility: " + safe(post.getVisibility())
                + "  |  Status: " + safe(post.getStatus());
        Label meta = new Label(metaText);
        meta.getStyleClass().add("post-meta");

        Label title = new Label(safe(post.getTitre()));
        title.getStyleClass().add("post-title");

        Label content = new Label(safe(post.getDescription()));
        content.setWrapText(true);
        content.getStyleClass().add("post-description");

        String date = post.getCreatedAt() == null ? "-" :
                post.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        Label createdAt = new Label("Posted at: " + date);
        createdAt.getStyleClass().add("post-meta");

        Label reactionsSummary = new Label(reactionSummaryText(post));
        reactionsSummary.getStyleClass().add("post-meta");

        HBox actions = new HBox(10);
        MenuButton reactMenu = new MenuButton("React");
        reactMenu.getStyleClass().add("action-btn");
        for (ReactionType type : ReactionType.values()) {
            MenuItem item = new MenuItem(type.emoji() + " " + type.label());
            item.setOnAction(evt -> handleReact(post, type));
            reactMenu.getItems().add(item);
        }

        Button clearReactionBtn = new Button("Clear Reaction");
        clearReactionBtn.getStyleClass().add("secondary-action");
        clearReactionBtn.setOnAction(evt -> handleClearReaction(post));

        Button editBtn = new Button("Edit");
        editBtn.getStyleClass().add("action-btn");
        editBtn.setOnAction(evt -> handleEdit(post));

        Button deleteBtn = new Button("Delete");
        deleteBtn.getStyleClass().add("danger-btn");
        deleteBtn.setOnAction(evt -> handleDelete(post));

        actions.getChildren().addAll(reactMenu, clearReactionBtn, editBtn, deleteBtn);

        card.getChildren().addAll(top, meta, title, content);

        if (post.getAttachedFile() != null && !post.getAttachedFile().isBlank()) {
            ImageView attached = createImageView(post.getAttachedFile(), 360, 220);
            if (attached != null) {
                card.getChildren().add(attached);
            } else {
                Label file = new Label("Attachment: " + post.getAttachedFile());
                file.getStyleClass().add("post-meta");
                card.getChildren().add(file);
            }
        }

        card.getChildren().addAll(createdAt, reactionsSummary, actions);
        return card;
    }

    private void handleEdit(Post post) {
        if (!CurrentUserContext.isLoggedIn()) {
            setFeedback("Please sign in first.", true);
            return;
        }
        if (!isOwnedByCurrentUser(post)) {
            setFeedback("Only user #" + currentUserId() + " can edit during testing.", true);
            return;
        }

        if (!"public".equalsIgnoreCase(post.getVisibility()) || post.getGroupId() != null) {
            setFeedback("Edit this post from the group page.", true);
            return;
        }

        editingPost = post;
        titleField.setText(post.getTitre());
        descriptionArea.setText(post.getDescription());
        statusCombo.setValue(post.getStatus());
        attachedFileField.setText(post.getAttachedFile() == null ? "" : post.getAttachedFile());

        formModeLabel.setText("Edit Public Post #" + post.getId());
        submitButton.setText("Update Post");
        cancelEditButton.setVisible(true);
        cancelEditButton.setManaged(true);
    }

    private void handleDelete(Post post) {
        if (!CurrentUserContext.isLoggedIn()) {
            setFeedback("Please sign in first.", true);
            return;
        }
        if (!isOwnedByCurrentUser(post)) {
            setFeedback("Only user #" + currentUserId() + " can delete during testing.", true);
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Post");
        alert.setHeaderText("Delete post #" + post.getId() + "?");
        alert.setContentText("This action cannot be undone.");
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }

        try {
            postService.deletePost(post.getId());
            if (editingPost != null && post.getId().equals(editingPost.getId())) {
                clearForm();
            }
            setFeedback("Post deleted.", false);
            refreshData();
        } catch (Exception e) {
            setFeedback("Could not delete post: " + e.getMessage(), true);
        }
    }

    private void handleReact(Post post, ReactionType type) {
        if (!CurrentUserContext.isLoggedIn()) {
            setFeedback("Please sign in first to react.", true);
            return;
        }

        try {
            postReactionService.setReaction(post.getId(), currentUserId(), type);
            setFeedback("Reaction updated to " + type.emoji() + " " + type.label() + ".", false);
            refreshData();
        } catch (Exception e) {
            setFeedback("Could not react to post: " + e.getMessage(), true);
        }
    }

    private void handleClearReaction(Post post) {
        if (!CurrentUserContext.isLoggedIn()) {
            setFeedback("Please sign in first to react.", true);
            return;
        }

        try {
            postReactionService.removeReaction(post.getId(), currentUserId());
            setFeedback("Reaction removed.", false);
            refreshData();
        } catch (Exception e) {
            setFeedback("Could not clear reaction: " + e.getMessage(), true);
        }
    }

    private void clearForm() {
        editingPost = null;
        titleField.clear();
        descriptionArea.clear();
        attachedFileField.clear();
        visibilityCombo.setValue("public");
        statusCombo.setValue("active");
        submitButton.setText("Post Now");
        formModeLabel.setText("Create Public Post");
        cancelEditButton.setVisible(false);
        cancelEditButton.setManaged(false);
    }

    private Integer parseInteger(String value) {
        String clean = clean(value);
        if (clean.isEmpty()) {
            return null;
        }

        try {
            return Integer.parseInt(clean);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private void setFeedback(String message, boolean error) {
        feedbackLabel.setText(message);
        feedbackLabel.getStyleClass().removeAll("error-text", "success-text");
        feedbackLabel.getStyleClass().add(error ? "error-text" : "success-text");
    }

    private boolean isOwnedByCurrentUser(Post post) {
        return post.getAuthorId() != null && post.getAuthorId() == currentUserId();
    }

    private int currentUserId() {
        return CurrentUserContext.getCurrentUserId();
    }

    private String reactionSummaryText(Post post) {
        try {
            int currentUserId = currentUserId();
            Map<ReactionType, Integer> counts = postReactionService.countByPost(post.getId());
            ReactionType mine = currentUserId > 0 ? postReactionService.findUserReaction(post.getId(), currentUserId) : null;

            StringJoiner joiner = new StringJoiner("   ");
            int total = 0;
            for (ReactionType type : ReactionType.values()) {
                int count = counts.getOrDefault(type, 0);
                total += count;
                if (count > 0) {
                    joiner.add(type.emoji() + " " + count);
                }
            }

            String summary = total == 0 ? "No reactions yet." : ("Reactions: " + joiner);
            if (mine != null) {
                summary += "  |  You reacted: " + mine.emoji() + " " + mine.label();
            }
            return summary;
        } catch (Exception e) {
            return "Reactions unavailable: " + e.getMessage();
        }
    }

    private void navigateTo(String fxmlPath) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            javafx.scene.layout.StackPane contentArea = (javafx.scene.layout.StackPane) postsContainer.getScene().lookup("#contentArea");
            if (contentArea != null) {
                contentArea.getChildren().setAll(root);
            }
        } catch (IOException e) {
            setFeedback("Could not open page: " + e.getMessage(), true);
        }
    }

    private ImageView createImageView(String source, double width, double height) {
        String resolved = resolveImageSource(source);
        if (resolved == null) {
            return null;
        }

        try {
            Image image = new Image(resolved, true);
            if (image.isError()) {
                return null;
            }
            ImageView imageView = new ImageView(image);
            imageView.setFitWidth(width);
            imageView.setFitHeight(height);
            imageView.setPreserveRatio(true);
            imageView.getStyleClass().add("group-icon");
            return imageView;
        } catch (Exception ignored) {
            return null;
        }
    }

    private String resolveImageSource(String rawPath) {
        String value = clean(rawPath);
        if (value.isEmpty()) {
            return null;
        }

        String lower = value.toLowerCase();
        if (lower.startsWith("http://") || lower.startsWith("https://") || lower.startsWith("file:")) {
            return value;
        }

        File direct = new File(value);
        if (direct.exists()) {
            return direct.toURI().toString();
        }

        File relativeToProject = new File(System.getProperty("user.dir"), value);
        if (relativeToProject.exists()) {
            return relativeToProject.toURI().toString();
        }

        return null;
    }
}
