package com.wmsdipl.desktop;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

public class I18n {
    private static final String PREF_KEY_PREFIX = "app_locale_";
    private static final String DEFAULT_LANG = "ru";
    private static ResourceBundle bundle;
    private static Locale currentLocale;
    private static String currentUsername;

    static {
        loadDefaultLocale();
    }

    private static void loadDefaultLocale() {
        currentLocale = new Locale(DEFAULT_LANG);
        bundle = ResourceBundle.getBundle("messages", currentLocale);
    }

    public static void loadForUser(String username) {
        currentUsername = username;
        Preferences prefs = Preferences.userNodeForPackage(DesktopClientApplication.class);
        String lang = prefs.get(PREF_KEY_PREFIX + username, DEFAULT_LANG);
        setLocaleInMemory(lang);
    }

    private static void setLocaleInMemory(String lang) {
        if ("en".equals(lang)) {
            currentLocale = new Locale("en");
        } else {
            currentLocale = new Locale("ru");
        }
        bundle = ResourceBundle.getBundle("messages", currentLocale);
    }

    public static String get(String key) {
        try {
            return bundle.getString(key);
        } catch (Exception e) {
            return "!" + key + "!";
        }
    }

    public static String format(String key, Object... args) {
        try {
            return MessageFormat.format(bundle.getString(key), args);
        } catch (Exception e) {
            return "!" + key + "!";
        }
    }

    public static void setLocale(String lang) {
        if (currentUsername != null) {
            Preferences prefs = Preferences.userNodeForPackage(DesktopClientApplication.class);
            prefs.put(PREF_KEY_PREFIX + currentUsername, lang);
        }
        setLocaleInMemory(lang);
    }
    
    public static String getCurrentLang() {
        return currentLocale.getLanguage();
    }
}
