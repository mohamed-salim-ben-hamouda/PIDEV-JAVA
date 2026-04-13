package com.pidev.Controllers.client;

import com.pidev.Services.GroupService;
import com.pidev.Services.MembershipService;
import com.pidev.models.Membership;
import com.pidev.models.User;
import com.pidev.utils.CurrentUserContext;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class MembershipController {

    private final MembershipService membershipService = new MembershipService();
    private final GroupService groupService = new GroupService();

    public Membership join(int groupId) throws SQLException {
        int currentUserId = CurrentUserContext.getCurrentUserId();
        return membershipService.addIfMissing(currentUserId, groupId, "member");
    }

    public boolean leave(int groupId) throws SQLException {
        int currentUserId = CurrentUserContext.getCurrentUserId();
        return membershipService.removeByUserAndGroup(currentUserId, groupId);
    }

    public Membership addMemberConfirm(int groupId, int userId) throws SQLException {
        return membershipService.addIfMissing(userId, groupId, "member");
    }

    public boolean kick(int membershipId) throws SQLException {
        return membershipService.removeById(membershipId);
    }

    public boolean setRole(int membershipId, String role) throws SQLException {
        return membershipService.setRole(membershipId, role);
    }

    public List<Membership> listGroupMembers(int groupId) throws SQLException {
        return membershipService.findByGroup(groupId);
    }

    public Optional<Membership> currentUserMembership(int groupId) throws SQLException {
        int currentUserId = CurrentUserContext.getCurrentUserId();
        return membershipService.findOne(currentUserId, groupId);
    }

    public List<User> searchUsersForGroupInvite(String searchTerm) throws SQLException {
        return groupService.searchUsers(searchTerm);
    }
}
