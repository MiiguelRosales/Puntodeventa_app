package com.example.puntodeventa;

import android.content.Context;

import androidx.appcompat.app.AppCompatDelegate;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public final class ThemeManager {

    private static final String FILE_NAME = "theme_config.json";

    private ThemeManager() {
    }

    public static void applySavedTheme(Context context) {
        AppCompatDelegate.setDefaultNightMode(getConfig(context).isNight()
                ? AppCompatDelegate.MODE_NIGHT_YES
                : AppCompatDelegate.MODE_NIGHT_NO);
    }

    public static void toggleTheme(Context context) {
        ThemeConfig currentConfig = getConfig(context);
        ThemeConfig newConfig = currentConfig.isNight()
                ? currentConfig.withMode(ThemeConfig.MODE_DAY)
                : currentConfig.withMode(ThemeConfig.MODE_NIGHT);

        saveConfig(context, newConfig);
        applySavedTheme(context);
    }

    public static String getToggleLabel(Context context) {
        return getConfig(context).isNight()
                ? context.getString(R.string.tema_claro)
                : context.getString(R.string.tema_oscuro);
    }

    public static boolean isNight(Context context) {
        return getConfig(context).isNight();
    }

    private static ThemeConfig getConfig(Context context) {
        File file = new File(context.getFilesDir(), FILE_NAME);
        if (!file.exists()) {
            ThemeConfig defaultConfig = ThemeConfig.defaultConfig();
            saveConfig(context, defaultConfig);
            return defaultConfig;
        }

        try (FileInputStream inputStream = context.openFileInput(FILE_NAME)) {
            byte[] buffer = new byte[(int) file.length()];
            int bytesRead = inputStream.read(buffer);
            if (bytesRead <= 0) {
                return ThemeConfig.defaultConfig();
            }

            String json = new String(buffer, 0, bytesRead, StandardCharsets.UTF_8);
            return ThemeConfig.fromJson(json);
        } catch (IOException exception) {
            return ThemeConfig.defaultConfig();
        }
    }

    private static void saveConfig(Context context, ThemeConfig config) {
        try (FileOutputStream outputStream = context.openFileOutput(FILE_NAME, Context.MODE_PRIVATE)) {
            outputStream.write(config.toJson().getBytes(StandardCharsets.UTF_8));
        } catch (IOException ignored) {
        }
    }
}