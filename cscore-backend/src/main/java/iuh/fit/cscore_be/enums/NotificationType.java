package iuh.fit.cscore_be.enums;

public enum NotificationType {
    INFO("INFO"),
    SUCCESS("SUCCESS"), 
    WARNING("WARNING"),
    ERROR("ERROR");

    private final String value;

    NotificationType(String value) {
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