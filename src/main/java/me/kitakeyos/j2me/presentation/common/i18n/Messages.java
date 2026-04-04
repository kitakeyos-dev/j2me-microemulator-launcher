package me.kitakeyos.j2me.presentation.common.i18n;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.logging.Logger;

/**
 * Internationalization utility for UI strings.
 * Uses Java ResourceBundle to load messages from properties files.
 *
 * Usage:
 *   Messages.get("app.title")           → "J2ME MicroEmulator Launcher"
 *   Messages.get("status.instances", 5) → "5 instance(s) running"
 */
public class Messages {

    private static final Logger logger = Logger.getLogger(Messages.class.getName());
    private static final String BUNDLE_NAME = "messages";

    private static ResourceBundle bundle;
    private static Locale currentLocale;

    static {
        loadBundle(Locale.ENGLISH);
    }

    /**
     * Load or reload the resource bundle for the given locale.
     */
    public static void loadBundle(Locale locale) {
        currentLocale = locale;
        bundle = ResourceBundle.getBundle(BUNDLE_NAME, locale);
        logger.info("Loaded message bundle for locale: " + locale);
    }

    /**
     * Load bundle from a language tag string (e.g. "en", "vi").
     */
    public static void loadBundle(String languageTag) {
        loadBundle(Locale.forLanguageTag(languageTag));
    }

    /**
     * Get a localized string by key.
     */
    public static String get(String key) {
        try {
            return bundle.getString(key);
        } catch (MissingResourceException e) {
            logger.warning("Missing i18n key: " + key);
            return "!" + key + "!";
        }
    }

    /**
     * Get a localized string with parameters (uses MessageFormat).
     * Example: Messages.get("status.instances", 5) with pattern "{0} instance(s) running"
     */
    public static String get(String key, Object... args) {
        String pattern = get(key);
        try {
            return MessageFormat.format(pattern, args);
        } catch (IllegalArgumentException e) {
            return pattern;
        }
    }

    /**
     * Get the current locale.
     */
    public static Locale getCurrentLocale() {
        return currentLocale;
    }
}
