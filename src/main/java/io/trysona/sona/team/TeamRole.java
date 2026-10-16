package io.trysona.sona.team;

public enum TeamRole {
    /** The team creator. Manages members and can delete the team. */
    OWNER,
    /** Can create and modify team venues, shows and presets. */
    EDITOR,
    /** Read-only access to team resources. */
    VIEWER;

    public boolean canEdit() {
        return this == OWNER || this == EDITOR;
    }
}
