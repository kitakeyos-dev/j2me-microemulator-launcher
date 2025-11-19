package me.kitakeyos.j2me.ui.dialog;

import javax.swing.*;
import java.awt.*;

/**
 * Basic message dialog using standard JOptionPane
 */
public class MessageDialog {

    public enum MessageType {
        SUCCESS,
        ERROR,
        WARNING,
        INFO
    }

    /**
     * Show a success message dialog
     */
    public static void showSuccess(Frame parent, String title, String message) {
        JOptionPane.showMessageDialog(parent, message, title, JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Show an error message dialog
     */
    public static void showError(Frame parent, String title, String message) {
        JOptionPane.showMessageDialog(parent, message, title, JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Show a warning message dialog
     */
    public static void showWarning(Frame parent, String title, String message) {
        JOptionPane.showMessageDialog(parent, message, title, JOptionPane.WARNING_MESSAGE);
    }

    /**
     * Show an info message dialog
     */
    public static void showInfo(Frame parent, String title, String message) {
        JOptionPane.showMessageDialog(parent, message, title, JOptionPane.INFORMATION_MESSAGE);
    }
}
