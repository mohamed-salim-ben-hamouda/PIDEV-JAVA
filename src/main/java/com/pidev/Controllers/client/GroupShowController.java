package com.pidev.Controllers.client;

import com.pidev.Services.PostService;
import com.pidev.Services.FightModerationService;
import com.pidev.Services.PostReactionService;
import com.pidev.Services.PerspectiveModerationService;
import com.pidev.models.Membership;
import com.pidev.models.Post;
import com.pidev.models.ReactionType;
import com.pidev.utils.CurrentUserContext;
import com.pidev.utils.GroupViewContext;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Button;
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
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.StringJoiner;

public class GroupShowController implements Initializable {
    private static final int MAX_TITLE_LENGTH = 80;
    private static final int MAX_CONTENT_LENGTH = 500;

    @FXML
    private Label groupNameLabel;
    @FXML
    private Label groupMetaLabel;
    @FXML
    private Label membersCountLabel;
    @FXML
    private Label aboutGroupLabel;
    @FXML
    private ImageView groupIconView;
    @FXML
    private Button joinLeaveButton;
    @FXML
    private Button editButton;
    @FXML
    private VBox postComposerCard;
    @FXML
    private TextField postTitleField;
    @FXML
    private TextArea postDescriptionArea;
    @FXML
    private ComboBox<String> postVisibilityCombo;
    @FXML
    private TextField postAttachmentField;
    @FXML
    private VBox postsContainer;
    @FXML
    private VBox membersContainer;
    @FXML
    private TextField memberSearchField;
    @FXML
    private VBox searchResultsContainer;
    @FXML
    private Label feedbackLabel;

    private final GroupController groupController = new GroupController();
    private final MembershipController membershipController = new MembershipController();
    private final PostService postService = new PostService();
    private final FightModerationService fightModerationService = new FightModerationService();
    private final PostReactionService postReactionService = new PostReactionService();
    private final PerspectiveModerationService perspectiveModerationService = new PerspectiveModerationService();

    private Integer groupId;
    private GroupController.GroupDetailsData currentData;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        postVisibilityCombo.getItems().addAll("private", "group", "public");
        postVisibilityCombo.setValue("private");

        groupId = GroupViewContext.getSelectedGroupId();
        if (groupId == null) {
            try {
                var groups = groupController.index("newest").getAllGroups();
                if (!groups.isEmpty()) {
                    groupId = groups.get(0).getId();
                    GroupViewContext.setSelectedGroupId(groupId);
                }
            } catch (SQLException ignored) {
            }
        }

        if (groupId == null) {
            setFeedback("No group found. Create one first.", true);
            joinLeaveButton.setDisable(true);
            editButton.setDisable(true);
            if (postComposerCard != null) {
                postComposerCard.setVisible(false);
                postComposerCard.setManaged(false);
            }
            return;
        }

        reload(null);
    }

    @FXML
    private void handleJoinOrLeave() {
        if (!CurrentUserContext.isLoggedIn()) {
            setFeedback("Please sign in first to join or leave groups.", true);
            return;
        }
        if (currentData == null || groupId == null) {
            return;
        }

        try {
            Membership current = currentData.getCurrentUserMembership();
            if (current == null) {
                membershipController.join(groupId);
            } else {
                membershipController.leave(groupId);
            }
            reload(null);
        } catch (SQLException e) {
            setFeedback("Action failed: " + e.getMessage(), true);
        }
    }

    @FXML
    private void handleEditGroup() {
        if (groupId == null) {
            return;
        }
        GroupViewContext.setEditingGroupId(groupId);
        openView("/Fxml/client/GroupFormView.fxml");
    }

    @FXML
    private void handleSearchMembers() {
        String term = memberSearchField.getText();
        reload(term == null ? null : term.trim());
    }

    @FXML
    private void handleOpenAddMemberView() {
        openView("/Fxml/client/AddMemberView.fxml");
    }

    @FXML
    private void handleBackToFeed() {
        openView("/Fxml/client/GroupsView.fxml");
    }

    @FXML
    private void handleBrowseGroupPostAttachment() {
        try {
            if (postsContainer == null || postsContainer.getScene() == null) {
                setFeedback("Attachment picker is not ready yet. Try again.", true);
                return;
            }

            FileChooser chooser = new FileChooser();
            chooser.setTitle("Choose Group Post Image");
            chooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp")
            );

            File selected = chooser.showOpenDialog(postsContainer.getScene().getWindow());
            if (selected != null) {
                postAttachmentField.setText(selected.getAbsolutePath());
            }
        } catch (Exception e) {
            setFeedback("Could not open attachment picker: " + e.getMessage(), true);
        }
    }

    @FXML
    private void handleSubmitGroupPost() {
        if (!CurrentUserContext.isLoggedIn()) {
            setFeedback("Please sign in first to create group posts.", true);
            return;
        }
        if (groupId == null || currentData == null || currentData.getCurrentUserMembership() == null) {
            setFeedback("Join the group to create posts.", true);
            return;
        }

        String title = clean(postTitleField.getText());
        String description = clean(postDescriptionArea.getText());
        String visibility = postVisibilityCombo.getValue();
        String attachment = clean(postAttachmentField.getText());

        if (title.isEmpty() || description.isEmpty() || visibility == null) {
            setFeedback("Title, description and visibility are required.", true);
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

        try {
            PerspectiveModerationService.ModerationDecision textDecision =
                    perspectiveModerationService.moderateText(title, description);
            if (!textDecision.allowed()) {
                setFeedback("Post blocked by text moderation: " + textDecision.reason(), true);
                return;
            }

            FightModerationService.ModerationDecision decision =
                    fightModerationService.moderatePost(title, description, attachment.isEmpty() ? null : attachment);
            if (!decision.allowed()) {
                setFeedback("Post blocked by moderation: " + decision.reason(), true);
                return;
            }

            Post post = new Post();
            post.setTitre(title);
            post.setDescription(description);
            post.setVisibility(visibility);
            post.setStatus("active");
            post.setAttachedFile(attachment.isEmpty() ? null : attachment);
            post.setGroupId(groupId);
            post.setAuthorId(CurrentUserContext.getCurrentUserId());
            post.setLikesCounter(0);

            postService.createPost(post);
            postTitleField.clear();
            postDescriptionArea.clear();
            postAttachmentField.clear();
            postVisibilityCombo.setValue("private");

            setFeedback("Post published in group.", false);
            reload(null);
        } catch (PerspectiveModerationService.ModerationException e) {
            setFeedback("Text moderation failed: " + e.getMessage(), true);
        } catch (FightModerationService.ModerationException e) {
            setFeedback("Moderation failed: " + e.getMessage(), true);
        } catch (SQLException e) {
            setFeedback("Could not publish post: " + e.getMessage(), true);
        } catch (Exception e) {
            setFeedback("Unexpected error while publishing: " + e.getMessage(), true);
        }
    }

    private void reload(String search) {
        try {
            Optional<GroupController.GroupDetailsData> data = groupController.show(groupId, search, null);
            if (data.isEmpty()) {
                setFeedback("Group not found.", true);
                return;
            }

            currentData = data.get();
            renderHeader();
            renderMembers();
            renderPosts();
            renderSearchResults();
            updateComposerAccess();
            if (feedbackLabel.getText() == null || feedbackLabel.getText().isBlank()) {
                setFeedback("", false);
            }
        } catch (SQLException e) {
            setFeedback("Could not load group: " + e.getMessage(), true);
        }
    }

    private void renderHeader() {
        var group = currentData.getGroup();
        int memberCount = currentData.getMemberships() == null ? 0 : currentData.getMemberships().size();

        groupNameLabel.setText(group.getName());
        groupMetaLabel.setText((group.getType() == null ? "Group" : capitalize(group.getType())) + " Group | " + memberCount + " members");
        membersCountLabel.setText(String.valueOf(memberCount));
        aboutGroupLabel.setText("This is a " + (group.getType() == null ? "community" : group.getType()) + " group for Skill Bridge members.");

        Image image = loadImage(group.getIcon());
        if (image != null) {
            groupIconView.setImage(image);
            groupIconView.setVisible(true);
            groupIconView.setManaged(true);
        } else {
            groupIconView.setImage(null);
            groupIconView.setVisible(false);
            groupIconView.setManaged(false);
        }

        Membership current = currentData.getCurrentUserMembership();
        if (current == null) {
            joinLeaveButton.setText("Join Group");
            editButton.setVisible(false);
            editButton.setManaged(false);
        } else {
            joinLeaveButton.setText("Leave Group (" + current.getRole() + ")");
            boolean leader = "leader".equalsIgnoreCase(current.getRole());
            editButton.setVisible(leader);
            editButton.setManaged(leader);
        }
    }

    private void updateComposerAccess() {
        boolean canPost = currentData != null && currentData.getCurrentUserMembership() != null;
        postComposerCard.setVisible(canPost);
        postComposerCard.setManaged(canPost);
    }

    private void renderMembers() {
        membersContainer.getChildren().clear();

        Membership current = currentData.getCurrentUserMembership();
        boolean canManage = current != null && ("leader".equalsIgnoreCase(current.getRole()) || "moderator".equalsIgnoreCase(current.getRole()));
        Integer currentUserId = current == null ? null : current.getUserId();

        for (Membership membership : currentData.getMemberships()) {
            HBox row = new HBox(10);
            row.getStyleClass().add("member-row");

            Label userLabel = new Label("User #" + membership.getUserId());
            userLabel.getStyleClass().add("member-name");

            Label roleLabel = new Label(membership.getRole());
            roleLabel.getStyleClass().add("member-role");

            row.getChildren().addAll(userLabel, roleLabel);

            if (canManage && currentUserId != null && !currentUserId.equals(membership.getUserId())) {
                Button kickBtn = new Button("Kick");
                kickBtn.getStyleClass().add("danger-btn");
                kickBtn.setOnAction(e -> {
                    try {
                        membershipController.kick(membership.getId());
                        reload(memberSearchField.getText());
                    } catch (SQLException ex) {
                        setFeedback("Could not kick member: " + ex.getMessage(), true);
                    }
                });
                row.getChildren().add(kickBtn);
            }

            membersContainer.getChildren().add(row);
        }
    }

    private void renderPosts() {
        postsContainer.getChildren().clear();

        List<Post> posts = currentData.getPosts();
        Membership current = currentData.getCurrentUserMembership();

        if (current == null) {
            Label locked = new Label("Private community: join this group to see posts and create one.");
            locked.getStyleClass().add("empty-label");
            postsContainer.getChildren().add(locked);
            return;
        }

        if (posts == null || posts.isEmpty()) {
            Label empty = new Label("No posts yet. Start the conversation!");
            empty.getStyleClass().add("empty-label");
            postsContainer.getChildren().add(empty);
            return;
        }

        for (Post post : posts) {
            VBox card = new VBox(8);
            card.getStyleClass().add("post-card");

            Label author = new Label("Author #" + post.getAuthorId());
            author.getStyleClass().add("post-author");

            Label title = new Label(post.getTitre() == null ? "(No title)" : post.getTitre());
            title.getStyleClass().add("post-title");

            Label desc = new Label(post.getDescription() == null ? "" : post.getDescription());
            desc.setWrapText(true);

            String dateText = post.getCreatedAt() == null ? "-" : post.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            Label meta = new Label(dateText + " | " + post.getVisibility());
            meta.getStyleClass().add("post-meta");

            Label reactionsSummary = new Label(reactionSummaryText(post));
            reactionsSummary.getStyleClass().add("post-meta");

            card.getChildren().addAll(author, title, desc, meta, reactionsSummary);

            if (post.getAttachedFile() != null && !post.getAttachedFile().isBlank()) {
                Image image = loadImage(post.getAttachedFile());
                if (image != null) {
                    ImageView attachment = new ImageView(image);
                    attachment.setFitWidth(420);
                    attachment.setFitHeight(260);
                    attachment.setPreserveRatio(true);
                    card.getChildren().add(attachment);
                } else {
                    Label path = new Label("Attachment: " + post.getAttachedFile());
                    path.getStyleClass().add("post-meta");
                    card.getChildren().add(path);
                }
            }

            HBox reactionActions = new HBox(8);
            MenuButton reactButton = new MenuButton("React");
            reactButton.getStyleClass().add("primary-action");
            for (ReactionType type : ReactionType.values()) {
                MenuItem item = new MenuItem(type.emoji() + " " + type.label());
                item.setOnAction(e -> handleReact(post, type));
                reactButton.getItems().add(item);
            }

            Button clearReactionBtn = new Button("Clear Reaction");
            clearReactionBtn.getStyleClass().add("secondary-action");
            clearReactionBtn.setOnAction(e -> handleClearReaction(post));
            reactionActions.getChildren().addAll(reactButton, clearReactionBtn);
            card.getChildren().add(reactionActions);

            postsContainer.getChildren().add(card);
        }
    }

    private void renderSearchResults() {
        searchResultsContainer.getChildren().clear();
        Membership current = currentData.getCurrentUserMembership();
        boolean canInvite = current != null && ("leader".equalsIgnoreCase(current.getRole()) || "moderator".equalsIgnoreCase(current.getRole()));
        if (!canInvite) {
            return;
        }

        List<com.pidev.models.User> users = currentData.getUsers();
        if (users == null || users.isEmpty()) {
            return;
        }

        for (com.pidev.models.User user : users) {
            HBox row = new HBox(10);
            row.getStyleClass().add("search-user-row");

            Label name = new Label((user.getPrenom() == null ? "" : user.getPrenom()) + " " + (user.getNom() == null ? "" : user.getNom()));
            Button addBtn = new Button("Add");
            addBtn.getStyleClass().add("primary-action");
            addBtn.setOnAction(e -> {
                try {
                    membershipController.addMemberConfirm(groupId, user.getId());
                    reload(memberSearchField.getText());
                } catch (SQLException ex) {
                    setFeedback("Could not add member: " + ex.getMessage(), true);
                }
            });

            row.getChildren().addAll(name, addBtn);
            searchResultsContainer.getChildren().add(row);
        }
    }

    private String capitalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private void handleReact(Post post, ReactionType type) {
        if (!CurrentUserContext.isLoggedIn()) {
            setFeedback("Please sign in first to react.", true);
            return;
        }
        try {
            postReactionService.setReaction(post.getId(), CurrentUserContext.getCurrentUserId(), type);
            setFeedback("Reaction updated to " + type.emoji() + " " + type.label() + ".", false);
            reload(memberSearchField.getText());
        } catch (SQLException e) {
            setFeedback("Could not react to post: " + e.getMessage(), true);
        }
    }

    private void handleClearReaction(Post post) {
        if (!CurrentUserContext.isLoggedIn()) {
            setFeedback("Please sign in first to react.", true);
            return;
        }
        try {
            postReactionService.removeReaction(post.getId(), CurrentUserContext.getCurrentUserId());
            setFeedback("Reaction removed.", false);
            reload(memberSearchField.getText());
        } catch (SQLException e) {
            setFeedback("Could not clear reaction: " + e.getMessage(), true);
        }
    }

    private String reactionSummaryText(Post post) {
        try {
            int currentUserId = CurrentUserContext.getCurrentUserId();
            var counts = postReactionService.countByPost(post.getId());
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
        } catch (SQLException e) {
            return "Reactions unavailable: " + e.getMessage();
        }
    }

    private void setFeedback(String message, boolean error) {
        feedbackLabel.setText(message == null ? "" : message);
        feedbackLabel.getStyleClass().removeAll("error-text", "success-text");
        feedbackLabel.getStyleClass().add(error ? "error-text" : "success-text");
    }

    private void openView(String fxmlPath) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            javafx.scene.layout.StackPane contentArea = (javafx.scene.layout.StackPane) groupNameLabel.getScene().lookup("#contentArea");
            if (contentArea != null) {
                contentArea.getChildren().setAll(root);
            }
        } catch (IOException ignored) {
        }
    }

    private Image loadImage(String rawPath) {
        String source = resolveImageSource(rawPath);
        if (source == null) {
            return null;
        }

        try {
            Image image = new Image(source, true);
            if (image.isError()) {
                return null;
            }
            return image;
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

        File relative = new File(System.getProperty("user.dir"), value);
        if (relative.exists()) {
            return relative.toURI().toString();
        }

        return null;
    }
}
