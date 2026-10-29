package com.wazopulse.zoho;

import com.fasterxml.jackson.databind.JsonNode;

final class JsonUtil {
    private JsonUtil() {}

    /** True if any text value anywhere in the tree equals the given text (case-insensitive). */
    static boolean containsText(JsonNode node, String text) {
        if (node == null) return false;
        if (node.isTextual()) return node.asText().equalsIgnoreCase(text);
        for (JsonNode child : node) {
            if (containsText(child, text)) return true;
        }
        return false;
    }

    /** First value found for any of the given field names (depth-first). */
    static String findText(JsonNode node, String... names) {
        if (node == null) return null;
        if (node.isObject()) {
            for (String n : names) {
                JsonNode v = node.get(n);
                if (v != null && (v.isTextual() || v.isNumber()) && !v.asText().isBlank()) {
                    return v.asText();
                }
            }
        }
        for (JsonNode child : node) {
            String r = findText(child, names);
            if (r != null) return r;
        }
        return null;
    }
}
