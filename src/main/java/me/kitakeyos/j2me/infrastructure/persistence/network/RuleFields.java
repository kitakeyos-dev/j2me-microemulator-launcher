package me.kitakeyos.j2me.infrastructure.persistence.network;

/**
 * Lenient parsing helpers shared by the rule codecs. A malformed field falls
 * back to its default rather than discarding the whole rule.
 */
final class RuleFields {

    private RuleFields() {
    }

    static int asInt(String value, int fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    static boolean asBoolean(String value, boolean fallback) {
        return value == null ? fallback : Boolean.parseBoolean(value.trim());
    }
}
