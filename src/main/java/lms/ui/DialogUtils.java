package lms.ui;

import com.googlecode.lanterna.gui2.MultiWindowTextGUI;
import com.googlecode.lanterna.gui2.dialogs.ListSelectDialogBuilder;
import com.googlecode.lanterna.gui2.dialogs.MessageDialog;
import com.googlecode.lanterna.gui2.dialogs.MessageDialogButton;
import com.googlecode.lanterna.gui2.dialogs.TextInputDialog;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.util.List;
import java.util.Optional;

public final class DialogUtils {
    private DialogUtils() {
    }

    public static Optional<String> promptText(MultiWindowTextGUI gui, String title, String label, String initial) {
        String result = TextInputDialog.showDialog(gui, title, label, initial);
        if (result == null) {
            return Optional.empty();
        }
        return Optional.of(result.trim());
    }

    public static Optional<Integer> promptInt(MultiWindowTextGUI gui, String title, String label, int initial) {
        Optional<String> value = promptText(gui, title, label, String.valueOf(initial));
        if (value.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Integer.parseInt(value.get()));
        } catch (NumberFormatException ex) {
            showError(gui, "Invalid number", "Please enter a valid integer.");
            return Optional.empty();
        }
    }

    public static void showError(MultiWindowTextGUI gui, String title, String message) {
        MessageDialog.showMessageDialog(gui, title, message, MessageDialogButton.OK);
    }

    public static void showInfo(MultiWindowTextGUI gui, String title, String message) {
        MessageDialog.showMessageDialog(gui, title, message, MessageDialogButton.OK);
    }

    public static boolean confirm(MultiWindowTextGUI gui, String title, String message) {
        MessageDialogButton result = MessageDialog.showMessageDialog(gui, title, message,
                MessageDialogButton.Yes, MessageDialogButton.No);
        return result == MessageDialogButton.Yes;
    }

    public static <T> Optional<T> promptChoice(MultiWindowTextGUI gui, String title, String label, List<Choice<T>> choices) {
        if (choices == null || choices.isEmpty()) {
            showInfo(gui, title, "No items available.");
            return Optional.empty();
        }
        ListSelectDialogBuilder<Choice<T>> builder = new ListSelectDialogBuilder<Choice<T>>()
                .setTitle(title)
                .setDescription(label);
        for (Choice<T> choice : choices) {
            builder.addListItem(choice);
        }
        Choice<T> selected = builder.build().showDialog(gui);
        if (selected == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(selected.getValue());
    }

    public static boolean copyToClipboard(String text) {
        if (text == null) {
            return false;
        }
        try {
            Toolkit.getDefaultToolkit()
                    .getSystemClipboard()
                    .setContents(new StringSelection(text), null);
            return true;
        } catch (RuntimeException ex) {
            return false;
        }
    }

    public static class Choice<T> {
        private final String label;
        private final T value;

        public Choice(String label, T value) {
            this.label = label;
            this.value = value;
        }

        public T getValue() {
            return value;
        }

        @Override
        public String toString() {
            return label;
        }
    }
}