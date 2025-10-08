package iuh.fit.cscore_be.enums;

public enum NotificationCategory {
    ASSIGNMENT("ASSIGNMENT"),
    COURSE("COURSE"),
    SUBMISSION("SUBMISSION"), 
    GRADING("GRADING"),
    SYSTEM("SYSTEM"),
    ANNOUNCEMENT("ANNOUNCEMENT");

    private final String value;

    NotificationCategory(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}