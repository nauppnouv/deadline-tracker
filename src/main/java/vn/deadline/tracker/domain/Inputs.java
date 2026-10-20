package vn.deadline.tracker.domain;

import java.util.Locale;

public final class Inputs {
    private Inputs() {}
    public static String required(String value, int max, String label) {
        String normalized = value == null ? "" : value.strip();
        if (normalized.isBlank() || length(normalized) > max) throw RuleViolation.invalid(label + " phải có từ 1 đến " + max + " ký tự.");
        return normalized;
    }
    public static String optional(String value, int max, String label) {
        String normalized = value == null ? "" : value.strip();
        if (length(normalized) > max) throw RuleViolation.invalid(label + " tối đa " + max + " ký tự.");
        return normalized;
    }
    public static String username(String value) {
        String name = value == null ? "" : value.strip().toLowerCase(Locale.ROOT);
        if (!name.matches("[a-z0-9._-]{3,30}")) throw RuleViolation.invalid("Tên tài khoản cần 3–30 ký tự a-z, số, dấu chấm, gạch dưới hoặc gạch ngang.");
        return name;
    }
    public static void password(String value) {
        if (value == null || length(value) < 8 || length(value) > 128) throw RuleViolation.invalid("Mật khẩu phải có 8–128 ký tự.");
    }
    private static int length(String value) { return value.codePointCount(0, value.length()); }
}
