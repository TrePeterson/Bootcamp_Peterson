package org.example;

import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Cell;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBoxBase;
import javafx.scene.control.ListView;
import javafx.scene.control.Spinner;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.TreeView;
import javafx.scene.input.KeyEvent;
import javafx.stage.PopupWindow;
import javafx.stage.Window;

import java.util.List;
import java.util.Objects;

/** Plain number keys switch views only when the user is not entering or choosing input. */
public final class ViewShortcuts {
    private static final Object INSTALLED_FILTER = new Object();

    private ViewShortcuts() {
    }

    public static void install(Scene scene, List<Button> destinations) {
        Objects.requireNonNull(scene, "Scene is required.");
        List<Button> buttons = List.copyOf(destinations);
        if (buttons.size() != 4) {
            throw new IllegalArgumentException("Provide the four navigation buttons in view order.");
        }

        Object previous = scene.getProperties().remove(INSTALLED_FILTER);
        if (previous instanceof Registration registration) {
            scene.removeEventFilter(KeyEvent.KEY_PRESSED, registration.handler());
        }

        EventHandler<KeyEvent> handler = event -> {
            if (event.isConsumed() || event.isControlDown() || event.isAltDown()
                    || event.isMetaDown() || event.isShiftDown()) return;

            int index = switch (event.getCode()) {
                case DIGIT1, NUMPAD1 -> 0;
                case DIGIT2, NUMPAD2 -> 1;
                case DIGIT3, NUMPAD3 -> 2;
                case DIGIT4, NUMPAD4 -> 3;
                default -> -1;
            };
            if (index < 0 || acceptsInput(scene.getFocusOwner())
                    || (event.getTarget() instanceof Node target && acceptsInput(target))
                    || popupIsOpen(scene)) return;

            Button destination = buttons.get(index);
            if (destination.isDisabled() || !destination.isVisible()) return;
            event.consume();
            destination.fire();
            destination.requestFocus();
        };
        scene.addEventFilter(KeyEvent.KEY_PRESSED, handler);
        scene.getProperties().put(INSTALLED_FILTER, new Registration(handler));
    }

    private static boolean acceptsInput(Node node) {
        // Editors and control skins often receive events through a child node.
        for (Node current = node; current != null; current = current.getParent()) {
            if (current instanceof TextInputControl || current instanceof ComboBoxBase<?>
                    || current instanceof ChoiceBox<?> || current instanceof Spinner<?>) return true;
            // ComboBoxBase also covers DatePicker, including non-editable pickers.
            if (current instanceof Cell<?> cell && (cell.isEditable() || cell.isEditing())) return true;
            if (current instanceof TableView<?> table && table.isEditable()) return true;
            if (current instanceof TreeTableView<?> table && table.isEditable()) return true;
            if (current instanceof ListView<?> list && list.isEditable()) return true;
            if (current instanceof TreeView<?> tree && tree.isEditable()) return true;
        }
        return false;
    }

    private static boolean popupIsOpen(Scene scene) {
        Window sceneWindow = scene.getWindow();
        if (sceneWindow instanceof PopupWindow && !(sceneWindow instanceof Tooltip)) return true;
        for (Window window : Window.getWindows()) {
            // Tooltips only explain controls; they do not take over keyboard input.
            if (window instanceof PopupWindow popup && !(popup instanceof Tooltip)
                    && popup.isShowing() && belongsTo(popup, sceneWindow)) return true;
        }
        return false;
    }

    private static boolean belongsTo(PopupWindow popup, Window sceneWindow) {
        if (sceneWindow == null) return false;
        Window owner = popup.getOwnerWindow();
        while (owner != null) {
            if (owner == sceneWindow) return true;
            owner = owner instanceof PopupWindow parent ? parent.getOwnerWindow() : null;
        }
        return false;
    }

    private record Registration(EventHandler<KeyEvent> handler) {
    }
}
