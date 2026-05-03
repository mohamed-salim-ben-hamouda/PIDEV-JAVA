package com.pidev.Controllers.client;

import com.pidev.Services.GroupService;
import com.pidev.Services.MembershipService;
import com.pidev.Services.NewsApiService;
import com.pidev.Services.PostCommentService;
import com.pidev.Services.PostService;
import com.pidev.Services.FightModerationService;
import com.pidev.Services.PostReactionService;
import com.pidev.Services.PerspectiveModerationService;
import com.pidev.Services.UserService;
import com.pidev.models.Group;
import com.pidev.models.Membership;
import com.pidev.models.PostComment;
import com.pidev.models.Post;
import com.pidev.models.ReactionType;
import com.pidev.models.User;
import com.pidev.utils.CurrentUserContext;
import com.pidev.utils.GroupViewContext;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.geometry.Side;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Region;
import javafx.stage.FileChooser;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.awt.Desktop;
import java.net.URI;
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
    @FXML
    private VBox newsContainer;
    @FXML
    private VBox recommendedGroupsContainer;
    @FXML
    private Button toggleRecommendedButton;

    private final PostService postService = new PostService();
    private final GroupService groupService = new GroupService();
    private final MembershipService membershipService = new MembershipService();
    private final NewsApiService newsApiService = new NewsApiService();
    private final FightModerationService fightModerationService = new FightModerationService();
    private final PostReactionService postReactionService = new PostReactionService();
    private final PostCommentService postCommentService = new PostCommentService();
    private final PerspectiveModerationService perspectiveModerationService = new PerspectiveModerationService();
    private final UserService userService = new UserService();

    private Post editingPost;
    private final Map<Integer, Group> groupsById = new HashMap<>();
    private final Map<Integer, String> userDisplayNameCache = new HashMap<>();
    private final Set<Integer> myGroupIds = new HashSet<>();
    private boolean recommendedVisible = true;

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
        loadLatestNews();
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
            setFeedback("Enter a valid group to open details.", true);
            return;
        }

        GroupViewContext.setSelectedGroupId(selectedGroupId);
        GroupViewContext.clearEditingGroupId();
        navigateTo("/Fxml/client/GroupShowView.fxml");
    }

    private void refreshData() {
        try {
            loadMyGroups();
            loadRecommendedGroups();
            loadPosts();
        } catch (Exception e) {
            setFeedback("Could not load feed data: " + e.getMessage(), true);
        }
    }

    @FXML
    private void handleToggleRecommendedGroups() {
        recommendedVisible = !recommendedVisible;
        if (recommendedGroupsContainer != null) {
            recommendedGroupsContainer.setVisible(recommendedVisible);
            recommendedGroupsContainer.setManaged(recommendedVisible);
        }
        if (toggleRecommendedButton != null) {
            toggleRecommendedButton.setText(recommendedVisible ? "Hide" : "Show");
        }
    }

    private void loadLatestNews() {
        if (newsContainer == null) {
            return;
        }

        newsContainer.getChildren().clear();
        Label loading = new Label("Loading latest headlines...");
        loading.getStyleClass().add("post-meta");
        newsContainer.getChildren().add(loading);

        Thread worker = new Thread(() -> {
            try {
                List<NewsApiService.Headline> headlines = newsApiService.fetchTopHeadlinesWithLinks(5);
                Platform.runLater(() -> renderNews(headlines));
            } catch (Exception e) {
                Platform.runLater(() -> renderNewsFallback(e.getMessage()));
            }
        }, "news-api-loader");
        worker.setDaemon(true);
        worker.start();
    }

    private void renderNews(List<NewsApiService.Headline> headlines) {
        newsContainer.getChildren().clear();
        for (NewsApiService.Headline headline : headlines) {
            Label item = new Label(headline.title());
            item.setWrapText(true);
            item.getStyleClass().add("news-item");
            item.setOnMouseClicked(evt -> openExternalLink(headline.url()));
            newsContainer.getChildren().add(item);
        }
    }

    private void renderNewsFallback(String error) {
        newsContainer.getChildren().clear();
        Label unavailable = new Label("Latest news unavailable right now.");
        unavailable.getStyleClass().add("news-item");
        unavailable.setWrapText(true);
        newsContainer.getChildren().add(unavailable);

        if (error != null && !error.isBlank()) {
            Label details = new Label("Reason: " + error);
            details.getStyleClass().add("post-meta");
            details.setWrapText(true);
            newsContainer.getChildren().add(details);
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
            Button btn = new Button(group.getName());
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

    private void loadRecommendedGroups() throws Exception {
        if (recommendedGroupsContainer == null) {
            return;
        }

        recommendedGroupsContainer.getChildren().clear();
        List<Group> all = groupService.findAll();
        int shown = 0;
        for (Group group : all) {
            if (group == null || group.getId() == null) {
                continue;
            }
            if (myGroupIds.contains(group.getId())) {
                continue;
            }

            Button btn = new Button(group.getName());
            btn.getStyleClass().add("group-link");
            btn.setMaxWidth(Double.MAX_VALUE);
            btn.setOnAction(e -> {
                GroupViewContext.setSelectedGroupId(group.getId());
                GroupViewContext.clearEditingGroupId();
                navigateTo("/Fxml/client/GroupShowView.fxml");
            });
            recommendedGroupsContainer.getChildren().add(btn);
            shown++;
            if (shown >= 8) {
                break;
            }
        }

        if (shown == 0) {
            Label empty = new Label("No recommendations right now.");
            empty.getStyleClass().add("post-meta");
            recommendedGroupsContainer.getChildren().add(empty);
        }
    }

    private void loadPosts() {
        try {
            List<Post> posts = postService.findAllForFeedRanked();
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
        Label author = new Label(resolveUserDisplayName(post.getAuthorId()));
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
        reactionsSummary.getStyleClass().add("social-count-label");

        Label commentsSummary = new Label(commentSummaryText(post));
        commentsSummary.getStyleClass().add("social-count-label");

        HBox socialStats = buildSocialStatsRow(reactionsSummary, commentsSummary);
        Region divider = createPostDivider();

        HBox actions = new HBox(10);
        actions.getStyleClass().add("reaction-bar");

        Button reactButton = new Button("React");
        reactButton.getStyleClass().addAll("action-btn", "reaction-chip", "reaction-trigger");
        reactButton.setGraphic(createReactionIcon(ReactionType.LIKE, 18));
        ContextMenu reactMenu = buildReactionMenu(post);
        attachHoverReactionMenu(reactButton, reactMenu);
        reactButton.setOnMouseClicked(evt -> {
            if (evt.getButton() == MouseButton.PRIMARY && evt.getClickCount() == 2) {
                handleClearReaction(post);
                evt.consume();
            }
        });

        TextField commentField = new TextField();
        commentField.setPromptText("Write a comment...");
        commentField.getStyleClass().add("comment-input");
        commentField.setPrefWidth(220);
        HBox.setHgrow(commentField, javafx.scene.layout.Priority.ALWAYS);
        commentField.setVisible(false);
        commentField.setManaged(false);
        commentField.setOnAction(evt -> handleAddComment(post, commentField));

        Button commentButton = new Button("Comment");
        commentButton.getStyleClass().addAll("action-btn", "comment-btn");
        commentButton.setText("");
        commentButton.setGraphic(createPostLowerBarIcon("comment.png", 16));
        commentButton.setOnAction(evt -> {
            if (!commentField.isVisible()) {
                commentField.setVisible(true);
                commentField.setManaged(true);
                commentField.requestFocus();
                return;
            }
            handleAddComment(post, commentField);
        });

        MenuButton moreButton = new MenuButton("...");
        moreButton.getStyleClass().addAll("action-btn", "post-menu-btn");
        moreButton.setText("");
        moreButton.setGraphic(createPostLowerBarIcon("threedots.png", 14));
        MenuItem editItem = new MenuItem("Edit");
        editItem.setOnAction(evt -> handleEdit(post));
        MenuItem deleteItem = new MenuItem("Delete");
        deleteItem.setOnAction(evt -> handleDelete(post));
        moreButton.getItems().addAll(editItem, deleteItem);

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        actions.getChildren().addAll(reactButton, commentField, commentButton, spacer, moreButton);

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

        VBox commentsBox = buildCommentsBox(post);
        card.getChildren().addAll(createdAt, socialStats, divider, actions, commentsBox);
        return card;
    }

    private void handleEdit(Post post) {
        if (!CurrentUserContext.isLoggedIn()) {
            setFeedback("Please sign in first.", true);
            return;
        }
        if (!isOwnedByCurrentUser(post)) {
            setFeedback("Only the post owner can edit this post.", true);
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

        formModeLabel.setText("Edit Public Post");
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
            setFeedback("Only the post owner can delete this post.", true);
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Post");
        alert.setHeaderText("Delete this post?");
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

    private void handleAddComment(Post post, TextField inputField) {
        if (!CurrentUserContext.isLoggedIn()) {
            setFeedback("Please sign in first to comment.", true);
            return;
        }
        if (post == null || inputField == null) {
            return;
        }

        String comment = clean(inputField.getText());
        if (comment.isEmpty()) {
            setFeedback("Comment cannot be empty.", true);
            return;
        }

        try {
            postCommentService.addComment(post.getId(), currentUserId(), comment);
            inputField.clear();
            inputField.setVisible(false);
            inputField.setManaged(false);
            setFeedback("Comment added.", false);
            refreshData();
        } catch (Exception e) {
            setFeedback("Could not add comment: " + e.getMessage(), true);
        }
    }

    private ContextMenu buildReactionMenu(Post post) {
        ContextMenu menu = new ContextMenu();
        for (ReactionType type : ReactionType.values()) {
            MenuItem item = new MenuItem(type.label());
            item.setGraphic(createReactionIcon(type, 18));
            item.setOnAction(evt -> handleReact(post, type));
            menu.getItems().add(item);
        }
        return menu;
    }

    private void attachHoverReactionMenu(Button trigger, ContextMenu menu) {
        PauseTransition delay = new PauseTransition(Duration.seconds(2));
        delay.setOnFinished(evt -> {
            if (trigger.isHover() && !menu.isShowing()) {
                menu.show(trigger, Side.TOP, 0, 8);
            }
        });

        trigger.setOnMouseEntered(evt -> delay.playFromStart());
        trigger.setOnMouseExited(evt -> delay.stop());
        trigger.setOnAction(evt -> {
            if (menu.isShowing()) {
                menu.hide();
            } else {
                menu.show(trigger, Side.TOP, 0, 8);
            }
        });
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

    private String resolveUserDisplayName(Integer userId) {
        if (userId == null || userId <= 0) {
            return "Member";
        }
        String cached = userDisplayNameCache.get(userId);
        if (cached != null && !cached.isBlank()) {
            return cached;
        }
        try {
            User user = userService.findById(userId);
            String displayName = user == null ? "Member" : safe(user.getDisplayName());
            if (displayName.isBlank()) {
                displayName = "Member";
            }
            userDisplayNameCache.put(userId, displayName);
            return displayName;
        } catch (Exception ignored) {
            return "Member";
        }
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
            int total = Math.max(0, post.getLikesCounter());
            return total == 1 ? "1 reaction" : total + " reactions";
        } catch (Exception e) {
            return "Reactions unavailable: " + e.getMessage();
        }
    }

    private String commentSummaryText(Post post) {
        try {
            int total = postCommentService.countByPost(post.getId());
            return total == 1 ? "1 comment" : total + " comments";
        } catch (Exception e) {
            return "Comments unavailable: " + e.getMessage();
        }
    }

    private VBox buildCommentsBox(Post post) {
        VBox box = new VBox(6);
        box.getStyleClass().add("comments-box");

        try {
            List<PostComment> comments = postCommentService.findRecentByPost(post.getId(), 3);
            if (comments.isEmpty()) {
                Label empty = new Label("Be the first to comment.");
                empty.getStyleClass().add("post-meta");
                box.getChildren().add(empty);
                return box;
            }

            for (PostComment comment : comments) {
                String displayName = resolveUserDisplayName(comment.getUserId());
                HBox item = new HBox(8);
                item.getStyleClass().add("comment-item");

                Label avatar = new Label(initialsFor(displayName));
                avatar.getStyleClass().add("comment-avatar");

                VBox bubble = new VBox(3);
                bubble.getStyleClass().add("comment-bubble");
                HBox.setHgrow(bubble, javafx.scene.layout.Priority.ALWAYS);

                Label author = new Label(displayName);
                author.getStyleClass().add("comment-author");

                Label body = new Label(safe(comment.getContent()));
                body.setWrapText(true);
                body.getStyleClass().add("comment-body");

                bubble.getChildren().addAll(author, body);
                item.getChildren().addAll(avatar, bubble);
                box.getChildren().add(item);
            }
        } catch (Exception e) {
            Label error = new Label("Could not load comments: " + e.getMessage());
            error.getStyleClass().add("post-meta");
            box.getChildren().add(error);
        }

        return box;
    }

    private HBox buildSocialStatsRow(Label reactionsSummary, Label commentsSummary) {
        HBox row = new HBox(10);
        row.getStyleClass().add("social-stats-row");

        HBox left = new HBox(4);
        left.getStyleClass().add("social-counts");

        Node likeIcon = createReactionIcon(ReactionType.LIKE, 13);
        Node loveIcon = createReactionIcon(ReactionType.LOVE, 13);
        if (likeIcon != null) {
            left.getChildren().add(likeIcon);
        }
        if (loveIcon != null) {
            left.getChildren().add(loveIcon);
        }
        left.getChildren().add(reactionsSummary);

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        row.getChildren().addAll(left, spacer, commentsSummary);
        return row;
    }

    private Region createPostDivider() {
        Region divider = new Region();
        divider.getStyleClass().add("post-divider");
        return divider;
    }

    private String initialsFor(String displayName) {
        String cleanName = safe(displayName).trim();
        if (cleanName.isEmpty()) {
            return "M";
        }

        String[] parts = cleanName.split("\\s+");
        StringBuilder initials = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                initials.append(Character.toUpperCase(part.charAt(0)));
            }
            if (initials.length() == 2) {
                break;
            }
        }
        return initials.isEmpty() ? "M" : initials.toString();
    }

    private Node createReactionIcon(ReactionType type, double size) {
        Image icon = loadReactionImage(type);
        if (icon == null) {
            return null;
        }

        ImageView imageView = new ImageView(icon);
        imageView.setFitWidth(size);
        imageView.setFitHeight(size);
        imageView.setPreserveRatio(true);
        return imageView;
    }

    private Image loadReactionImage(ReactionType type) {
        if (type == null) {
            return null;
        }

        String resourcePath = "/images/reactions/" + type.iconFile();
        try (InputStream stream = getClass().getResourceAsStream(resourcePath)) {
            if (stream == null) {
                return null;
            }
            return new Image(stream);
        } catch (Exception ignored) {
            return null;
        }
    }

    private Node createPostLowerBarIcon(String iconFile, double size) {
        Image icon = loadPostLowerBarImage(iconFile);
        if (icon == null) {
            return null;
        }

        ImageView imageView = new ImageView(icon);
        imageView.setFitWidth(size);
        imageView.setFitHeight(size);
        imageView.setPreserveRatio(true);
        return imageView;
    }

    private Image loadPostLowerBarImage(String iconFile) {
        if (iconFile == null || iconFile.isBlank()) {
            return null;
        }

        String resourcePath = "/images/post lower bar/" + iconFile;
        try (InputStream stream = getClass().getResourceAsStream(resourcePath)) {
            if (stream == null) {
                return null;
            }
            return new Image(stream);
        } catch (Exception ignored) {
            return null;
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

    private void openExternalLink(String url) {
        if (url == null || url.isBlank()) {
            setFeedback("This news item has no link.", true);
            return;
        }
        try {
            if (!Desktop.isDesktopSupported()) {
                setFeedback("Cannot open link on this system.", true);
                return;
            }
            Desktop.getDesktop().browse(URI.create(url));
        } catch (Exception e) {
            setFeedback("Could not open news link: " + e.getMessage(), true);
        }
    }
}

