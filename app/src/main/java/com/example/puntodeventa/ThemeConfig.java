package com.example.puntodeventa;

import org.json.JSONException;
import org.json.JSONObject;

public final class ThemeConfig {

    public static final String MODE_DAY = "day";
    public static final String MODE_NIGHT = "night";

    private static final String KEY_MODE = "mode";
    private static final String KEY_UPDATED_AT = "updatedAt";

    private final String mode;
    private final long updatedAt;

    public ThemeConfig(String mode, long updatedAt) {
        this.mode = normalizeMode(mode);
        this.updatedAt = updatedAt;
    }

    public static ThemeConfig defaultConfig() {
        return new ThemeConfig(MODE_DAY, System.currentTimeMillis());
    }

    public static ThemeConfig fromJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return defaultConfig();
        }

        try {
            JSONObject object = new JSONObject(json);
            String mode = object.optString(KEY_MODE, MODE_DAY);
            long updatedAt = object.optLong(KEY_UPDATED_AT, System.currentTimeMillis());
            return new ThemeConfig(mode, updatedAt);
        } catch (JSONException exception) {
            return defaultConfig();
        }
    }

    public String toJson() {
        JSONObject object = new JSONObject();
        try {
            object.put(KEY_MODE, mode);
            object.put(KEY_UPDATED_AT, updatedAt);
        } catch (JSONException exception) {
            return "{}";
        }

        return object.toString();
    }

    public String getMode() {
        return mode;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public boolean isNight() {
        return MODE_NIGHT.equals(mode);
    }

    public ThemeConfig withMode(String newMode) {
        return new ThemeConfig(newMode, System.currentTimeMillis());
    }

    private static String normalizeMode(String value) {
        return MODE_NIGHT.equals(value) ? MODE_NIGHT : MODE_DAY;
    }
}