package com.pidev.utils;

public final class GroupViewContext {

    private static Integer selectedGroupId;
    private static Integer editingGroupId;

    private GroupViewContext() {
    }

    public static Integer getSelectedGroupId() {
        return selectedGroupId;
    }

    public static void setSelectedGroupId(Integer selectedGroupId) {
        GroupViewContext.selectedGroupId = selectedGroupId;
    }

    public static Integer getEditingGroupId() {
        return editingGroupId;
    }

    public static void setEditingGroupId(Integer editingGroupId) {
        GroupViewContext.editingGroupId = editingGroupId;
    }

    public static void clearEditingGroupId() {
        GroupViewContext.editingGroupId = null;
    }
}
