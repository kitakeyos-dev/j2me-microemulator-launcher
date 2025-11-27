package me.kitakeyos.j2me.presentation.common.dialog;

import javax.swing.*;
import java.awt.*;

/**
 * Basic confirmation dialog using standard JOptionPane
 */
public class ConfirmDialog {

    /**
     * Show a confirmation dialog with Yes/No options
     *
     * @param parent Parent frame
     * @param title Dialog title
     * @param message Confirmation message
     * @return true if user clicked Yes, false otherwise
     */
    public static boolean showConfirm(Frame parent, String title, String message) {
        int result = JOptionPane.showConfirmDialog(parent, message, title,
            JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        return result == JOptionPane.YES_OPTION;
    }

    /**
     * Show a confirmation dialog with custom button texts
     *
     * @param parent Parent frame
     * @param title Dialog title
     * @param message Confirmation message
     * @param yesText Text for confirmation button
     * @param noText Text for cancel button
     * @return true if user clicked confirmation button, false otherwise
     */
    public static boolean showConfirm(Frame parent, String title, String message, String yesText, String noText) {
        Object[] options = {yesText, noText};
        int result = JOptionPane.showOptionDialog(parent, message, title,
            JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
        return result == 0;
    }

    /**
     * Show a confirmation dialog compatible with JOptionPane return values
     *
     * @param parent Parent frame
     * @param title Dialog title
     * @param message Confirmation message
     * @return JOptionPane.YES_OPTION if confirmed, JOptionPane.NO_OPTION otherwise
     */
    public static int showConfirmDialog(Frame parent, String title, String message) {
        return JOptionPane.showConfirmDialog(parent, message, title,
            JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
    }
}
