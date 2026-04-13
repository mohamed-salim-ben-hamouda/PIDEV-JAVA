package com.pidev.Controllers.client;

import com.pidev.Services.GroupService;
import com.pidev.Services.MembershipService;
import com.pidev.Services.PostService;
import com.pidev.models.Group;
import com.pidev.models.Membership;
import com.pidev.models.Post;
import com.pidev.models.User;
import com.pidev.utils.CurrentUserContext;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class GroupController {

    private final GroupService groupService = new GroupService();
    private final MembershipService membershipService = new MembershipService();
    private final PostService postService = new PostService();

    public GroupIndexData index(String sort) throws SQLException {
        int currentUserId = CurrentUserContext.getCurrentUserId();

        List<Group> ownedGroups = groupService.findByLeaderId(currentUserId);
        List<Group> memberGroups = membershipService.findUserGroups(currentUserId, groupService);
        List<Group> myGroups = mergeUniqueById(ownedGroups, memberGroups);

        List<Post> publicPosts = listPublicPosts(sort);
        List<Group> allGroups = groupService.findAll();

        GroupIndexData data = new GroupIndexData();
        data.setMyGroups(myGroups);
        data.setPublicPosts(publicPosts);
        data.setAllGroups(allGroups);
        data.setCurrentSort(normalizeSort(sort));
        return data;
    }

    public Group add(Group group) throws SQLException {
        int currentUserId = CurrentUserContext.getCurrentUserId();

        group.setLeaderId(currentUserId);
        group.setCreationDate(group.getCreationDate() == null ? LocalDateTime.now() : group.getCreationDate());
        group.setRatingScore(group.getRatingScore());
        if (group.getIcon() == null || group.getIcon().isBlank()) {
            group.setIcon("assets/images/frontoffice/profile_pic.png");
        }

        Group created = groupService.create(group);
        membershipService.addIfMissing(currentUserId, created.getId(), "leader");
        return created;
    }

    public boolean edit(Group group) throws SQLException {
        if (group.getId() == null) {
            return false;
        }

        Optional<Group> existing = groupService.findById(group.getId());
        if (existing.isEmpty()) {
            return false;
        }

        Group dbGroup = existing.get();
        if (group.getName() != null) {
            dbGroup.setName(group.getName());
        }
        if (group.getDescription() != null) {
            dbGroup.setDescription(group.getDescription());
        }
        if (group.getType() != null) {
            dbGroup.setType(group.getType());
        }
        if (group.getLevel() != null) {
            dbGroup.setLevel(group.getLevel());
        }
        if (group.getMaxMembers() != null) {
            dbGroup.setMaxMembers(group.getMaxMembers());
        }
        if (group.getIcon() != null && !group.getIcon().isBlank()) {
            dbGroup.setIcon(group.getIcon());
        }

        return groupService.update(dbGroup);
    }

    public Optional<GroupDetailsData> show(int groupId, String searchTerm, Integer userIdToAdd) throws SQLException {
        Optional<Group> groupOptional = groupService.findById(groupId);
        if (groupOptional.isEmpty()) {
            return Optional.empty();
        }

        Group group = groupOptional.get();
        int currentUserId = CurrentUserContext.getCurrentUserId();

        if (userIdToAdd != null) {
            membershipService.addIfMissing(userIdToAdd, groupId, "member");
        }

        List<Membership> memberships = membershipService.findByGroup(groupId);
        Optional<Membership> currentMembership = membershipService.findOne(currentUserId, groupId);

        GroupDetailsData details = new GroupDetailsData();
        details.setGroup(group);
        details.setMemberships(memberships);
        details.setPosts(listPostsForGroup(groupId));
        details.setCurrentUserMembership(currentMembership.orElse(null));
        details.setUsers(searchTerm == null || searchTerm.isBlank() ? List.of() : groupService.searchUsers(searchTerm));
        return Optional.of(details);
    }

    public boolean delete(int groupId) throws SQLException {
        return groupService.deleteWithDependencies(groupId);
    }

    private List<Post> listPublicPosts(String sort) throws SQLException {
        List<Post> posts = new ArrayList<>();
        for (Post post : postService.findAllNewestFirst()) {
            if ("public".equalsIgnoreCase(post.getVisibility())) {
                posts.add(post);
            }
        }

        String normalized = normalizeSort(sort);
        if ("popular".equals(normalized)) {
            posts.sort(Comparator.comparingInt(Post::getLikesCounter).reversed());
        } else if ("oldest".equals(normalized)) {
            posts.sort(Comparator.comparing(Post::getCreatedAt,
                    Comparator.nullsLast(Comparator.naturalOrder())));
        } else {
            posts.sort(Comparator.comparing(Post::getCreatedAt,
                    Comparator.nullsLast(Comparator.reverseOrder())));
        }

        return posts;
    }

    private List<Post> listPostsForGroup(int groupId) throws SQLException {
        List<Post> posts = new ArrayList<>();
        for (Post post : postService.findAllNewestFirst()) {
            if (post.getGroupId() != null && post.getGroupId() == groupId) {
                posts.add(post);
            }
        }
        return posts;
    }

    private List<Group> mergeUniqueById(List<Group> first, List<Group> second) {
        Map<Integer, Group> map = new LinkedHashMap<>();
        for (Group group : first) {
            if (group.getId() != null) {
                map.put(group.getId(), group);
            }
        }
        for (Group group : second) {
            if (group.getId() != null) {
                map.putIfAbsent(group.getId(), group);
            }
        }
        return map.values().stream().filter(Objects::nonNull).toList();
    }

    private String normalizeSort(String sort) {
        if ("popular".equalsIgnoreCase(sort)) {
            return "popular";
        }
        if ("oldest".equalsIgnoreCase(sort)) {
            return "oldest";
        }
        return "newest";
    }

    public static class GroupIndexData {
        private List<Group> myGroups;
        private List<Post> publicPosts;
        private List<Group> allGroups;
        private String currentSort;

        public List<Group> getMyGroups() {
            return myGroups;
        }

        public void setMyGroups(List<Group> myGroups) {
            this.myGroups = myGroups;
        }

        public List<Post> getPublicPosts() {
            return publicPosts;
        }

        public void setPublicPosts(List<Post> publicPosts) {
            this.publicPosts = publicPosts;
        }

        public List<Group> getAllGroups() {
            return allGroups;
        }

        public void setAllGroups(List<Group> allGroups) {
            this.allGroups = allGroups;
        }

        public String getCurrentSort() {
            return currentSort;
        }

        public void setCurrentSort(String currentSort) {
            this.currentSort = currentSort;
        }
    }

    public static class GroupDetailsData {
        private Group group;
        private List<Membership> memberships;
        private List<Post> posts;
        private Membership currentUserMembership;
        private List<User> users;

        public Group getGroup() {
            return group;
        }

        public void setGroup(Group group) {
            this.group = group;
        }

        public List<Membership> getMemberships() {
            return memberships;
        }

        public void setMemberships(List<Membership> memberships) {
            this.memberships = memberships;
        }

        public List<Post> getPosts() {
            return posts;
        }

        public void setPosts(List<Post> posts) {
            this.posts = posts;
        }

        public Membership getCurrentUserMembership() {
            return currentUserMembership;
        }

        public void setCurrentUserMembership(Membership currentUserMembership) {
            this.currentUserMembership = currentUserMembership;
        }

        public List<User> getUsers() {
            return users;
        }

        public void setUsers(List<User> users) {
            this.users = users;
        }
    }
}
