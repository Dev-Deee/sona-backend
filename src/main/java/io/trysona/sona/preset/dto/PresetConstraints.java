package io.trysona.sona.preset.dto;

final class PresetConstraints {

    /** Lower-case kebab-case tab id, e.g. "acoustics" or "gain-signal". */
    static final String TAB_PATTERN = "^[a-z][a-z0-9-]{0,99}$";
    static final String TAB_MESSAGE = "must be a lower-case calculator tab id, e.g. 'acoustics'";

    private PresetConstraints() {
    }
}
