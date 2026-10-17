package vn.deadline.tracker.domain;

public class RuleViolation extends RuntimeException {
    public enum Kind { VALIDATION, NOT_FOUND, FORBIDDEN, CONFLICT }
    private final Kind kind;
    public RuleViolation(Kind kind, String message) { super(message); this.kind = kind; }
    public Kind getKind() { return kind; }
    public static RuleViolation invalid(String message) { return new RuleViolation(Kind.VALIDATION, message); }
    public static RuleViolation missing() { return new RuleViolation(Kind.NOT_FOUND, "Không tìm thấy tài nguyên."); }
    public static RuleViolation forbidden() { return new RuleViolation(Kind.FORBIDDEN, "Bạn không có quyền thực hiện thao tác này."); }
    public static RuleViolation conflict() { return new RuleViolation(Kind.CONFLICT, "Dữ liệu đã thay đổi. Vui lòng tải lại trước khi lưu."); }
}
