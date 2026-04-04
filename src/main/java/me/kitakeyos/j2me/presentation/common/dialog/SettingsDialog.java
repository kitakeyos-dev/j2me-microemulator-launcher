package me.kitakeyos.j2me.presentation.common.dialog;

import me.kitakeyos.j2me.application.MainApplication;
import me.kitakeyos.j2me.application.config.ApplicationConfig;
import me.kitakeyos.j2me.presentation.common.i18n.Messages;

import javax.swing.*;
import java.awt.*;

/**
 * Minimal settings dialog - language selection only.
 * Uses JOptionPane to avoid custom styling issues.
 */
public class SettingsDialog {

    private static final String[] LANGUAGE_NAMES = {"English", "Tiếng Việt"};
    private static final String[] LANGUAGE_CODES = {"en", "vi"};

    /**
     * Show the settings dialog.
     */
    public static void show(Frame parent, ApplicationConfig config) {
        String currentLang = config.getLanguage();
        int currentIndex = 0;
        for (int i = 0; i < LANGUAGE_CODES.length; i++) {
            if (LANGUAGE_CODES[i].equals(currentLang)) {
                currentIndex = i;
                break;
            }
        }

        JComboBox<String> languageComboBox = new JComboBox<>(LANGUAGE_NAMES);
        languageComboBox.setSelectedIndex(currentIndex);

        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.add(new JLabel(Messages.get("settings.language")), BorderLayout.WEST);
        panel.add(languageComboBox, BorderLayout.CENTER);

        int result = JOptionPane.showConfirmDialog(
                parent, panel, Messages.get("settings.title"),
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            String newLanguage = LANGUAGE_CODES[languageComboBox.getSelectedIndex()];
            if (!newLanguage.equals(currentLang)) {
                config.setLanguage(newLanguage);
                config.saveConfiguration();
                Messages.loadBundle(newLanguage);
                MainApplication.INSTANCE.rebuildUI();
            }
        }
    }
}
