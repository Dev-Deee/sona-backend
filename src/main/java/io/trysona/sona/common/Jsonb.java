package io.trysona.sona.common;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Helpers for JSONB-backed columns. Callers may send {@code null} for whole documents or for
 * individual keys; these helpers keep the stored value well-formed.
 */
public final class Jsonb {

    private Jsonb() {
    }

    public static Map<String, Object> objectOrEmpty(Map<String, Object> value) {
        return value == null ? new LinkedHashMap<>() : new LinkedHashMap<>(value);
    }

    public static List<Map<String, Object>> arrayOrEmpty(List<Map<String, Object>> value) {
        return value == null ? new ArrayList<>() : new ArrayList<>(value);
    }

    /**
     * Shallow merge: keys in {@code patch} overwrite keys in {@code base}; a key whose value is
     * {@code null} is removed.
     */
    public static Map<String, Object> merge(Map<String, Object> base, Map<String, Object> patch) {
        Map<String, Object> merged = objectOrEmpty(base);
        patch.forEach((key, value) -> {
            if (value == null) {
                merged.remove(key);
            } else {
                merged.put(key, value);
            }
        });
        return merged;
    }
}
