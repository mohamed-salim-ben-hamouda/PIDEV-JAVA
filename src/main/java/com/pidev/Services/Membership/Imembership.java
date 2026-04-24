package com.pidev.Services.Membership;

import com.pidev.models.Group;
import com.pidev.models.User;

import java.util.List;

public interface Imembership {
    List<Group> FindAdminGroups(int user_id);
    List<User> getAllGroupMembersForGit(int g);
}
