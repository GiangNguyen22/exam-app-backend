package com.android.app.exam_app_backend.entity.enums;

public enum SecurityEventType {
    APP_BACKGROUND,
    APP_FOREGROUND,
    SCREENSHOT_ATTEMPT,
    CONNECTION_LOST,
    CONNECTION_RESTORED,
    SPLIT_SCREEN_DETECTED;

    public static SecurityEventType fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String normalized = value.trim().toUpperCase();
        switch (normalized) {
            case "FOCUS_LOST":
            case "APP_BACKGROUND":
                return APP_BACKGROUND;
            case "FOCUS_RESTORED":
            case "APP_FOREGROUND":
                return APP_FOREGROUND;
            case "SCREENSHOT":
            case "SCREENSHOT_ATTEMPT":
                return SCREENSHOT_ATTEMPT;
            case "CONNECTION_LOST":
                return CONNECTION_LOST;
            case "CONNECTION_RESTORED":
                return CONNECTION_RESTORED;
            case "SPLIT_SCREEN":
            case "SPLIT_SCREEN_DETECTED":
                return SPLIT_SCREEN_DETECTED;
            default:
                try {
                    return SecurityEventType.valueOf(normalized);
                } catch (IllegalArgumentException e) {
                    return null;
                }
        }
    }
}
