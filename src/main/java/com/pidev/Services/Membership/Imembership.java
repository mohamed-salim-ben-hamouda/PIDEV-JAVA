package com.pidev.Services.Membership;

import com.pidev.models.Group;

import java.util.List;

public interface Imembership {
    List<Group> FindAdminGroups(int user_id);

}
