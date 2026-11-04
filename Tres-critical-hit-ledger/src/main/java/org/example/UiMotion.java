package org.example;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.InvalidationListener;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.scene.Node;
import javafx.scene.control.ButtonBase;
import javafx.util.Duration;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;

/** Small visual transitions only; call from the JavaFX application thread. */
public final class UiMotion {
    private final BooleanProperty reducedMotion = new SimpleBooleanProperty(
            this, "reducedMotion", Boolean.getBoolean("ledger.reduceMotion"));
    private final Map<Node, MotionState> nodes = new WeakHashMap<>();
    private final InvalidationListener preferenceListener = ignored -> {
        if (reducedMotion.get()) resetAll();
    };
    private boolean stopped;

    public UiMotion() {
        reducedMotion.addListener(preferenceListener);
    }

    public BooleanProperty reducedMotionProperty() {
        return reducedMotion;
    }

    /** Leaves the button's action handler alone, including keyboard activation. */
    public void install(ButtonBase button) {
        Objects.requireNonNull(button, "Button is required.");
        if (stopped) return;
        MotionState state = stateFor(button);
        if (state.buttonListener != null) return;

        // A discarded view should not be kept alive just because it once had buttons.
        WeakReference<ButtonBase> reference = new WeakReference<>(button);
        state.buttonListener = (observable, previous, currentValue) -> {
            ButtonBase current = reference.get();
            if (current != null) updateButton(current);
        };
        button.hoverProperty().addListener(state.buttonListener);
        button.armedProperty().addListener(state.buttonListener);
        button.focusedProperty().addListener(state.buttonListener);
        button.disabledProperty().addListener(state.buttonListener);
        if (reducedMotion.get() || button.isDisabled()) reset(button, state);
    }

    /** The new content is already usable while its appearance settles into place. */
    public void reveal(Node node) {
        Objects.requireNonNull(node, "Node is required.");
        if (stopped) return;
        MotionState state = stateFor(node);
        if (reducedMotion.get() || node.isDisabled()) {
            reset(node, state);
            return;
        }
        cancelGeometry(node, state);
        cancelOpacity(node, state);
        node.setTranslateY(8);
        node.setOpacity(0.8);
        animateGeometry(node, state, 200, 0, 1);
        animateOpacity(node, state, 200);
    }

    /** A single gentle fade makes an updated status noticeable without flashing. */
    public void feedback(Node node) {
        Objects.requireNonNull(node, "Node is required.");
        if (stopped) return;
        MotionState state = stateFor(node);
        if (reducedMotion.get() || node.isDisabled()) {
            reset(node, state);
            return;
        }
        cancelOpacity(node, state);
        node.setOpacity(Math.min(node.getOpacity(), 0.9));
        animateOpacity(node, state, 160);
    }

    /** Called when the application closes; removes listeners and restores every node. */
    public void stop() {
        if (stopped) return;
        stopped = true;
        reducedMotion.removeListener(preferenceListener);
        for (Map.Entry<Node, MotionState> entry : nodes.entrySet()) {
            Node node = entry.getKey();
            MotionState state = entry.getValue();
            reset(node, state);
            if (node instanceof ButtonBase button && state.buttonListener != null) {
                button.hoverProperty().removeListener(state.buttonListener);
                button.armedProperty().removeListener(state.buttonListener);
                button.focusedProperty().removeListener(state.buttonListener);
                button.disabledProperty().removeListener(state.buttonListener);
            }
        }
        nodes.clear();
    }

    private void updateButton(ButtonBase button) {
        if (stopped) return;
        MotionState state = stateFor(button);
        if (reducedMotion.get() || button.isDisabled()) {
            reset(button, state);
            return;
        }
        boolean highlighted = button.isHover() || button.isFocused();
        double scale = button.isArmed() ? 0.975 : highlighted ? 1.01 : 1;
        double lift = highlighted && !button.isArmed() ? -1 : 0;
        animateGeometry(button, state, button.isArmed() ? 80 : 120, lift, scale);
    }

    private MotionState stateFor(Node node) {
        return nodes.computeIfAbsent(node, ignored -> new MotionState());
    }

    private void animateGeometry(Node node, MotionState state, double milliseconds,
                                 double translateY, double scale) {
        cancelGeometry(node, state);
        Timeline animation = new Timeline(new KeyFrame(Duration.millis(milliseconds),
                new KeyValue(node.translateYProperty(), translateY, Interpolator.EASE_OUT),
                new KeyValue(node.scaleXProperty(), scale, Interpolator.EASE_OUT),
                new KeyValue(node.scaleYProperty(), scale, Interpolator.EASE_OUT)));
        state.geometry = animation;
        animation.setOnFinished(event -> {
            if (state.geometry == animation) state.geometry = null;
            animation.setOnFinished(null);
        });
        animation.play();
    }

    private void animateOpacity(Node node, MotionState state, double milliseconds) {
        cancelOpacity(node, state);
        Timeline animation = new Timeline(new KeyFrame(Duration.millis(milliseconds),
                new KeyValue(node.opacityProperty(), 1, Interpolator.EASE_OUT)));
        state.opacity = animation;
        animation.setOnFinished(event -> {
            if (state.opacity == animation) state.opacity = null;
            animation.setOnFinished(null);
        });
        animation.play();
    }

    private static void cancelGeometry(Node node, MotionState state) {
        if (state.geometry == null) return;
        // Preserve the current pose when retargeting a fast hover or navigation change.
        double translateY = node.getTranslateY();
        double scaleX = node.getScaleX();
        double scaleY = node.getScaleY();
        state.geometry.setOnFinished(null);
        state.geometry.stop();
        state.geometry = null;
        node.setTranslateY(translateY);
        node.setScaleX(scaleX);
        node.setScaleY(scaleY);
    }

    private static void cancelOpacity(Node node, MotionState state) {
        if (state.opacity == null) return;
        double opacity = node.getOpacity();
        state.opacity.setOnFinished(null);
        state.opacity.stop();
        state.opacity = null;
        node.setOpacity(opacity);
    }

    private void resetAll() {
        for (Map.Entry<Node, MotionState> entry : nodes.entrySet()) {
            reset(entry.getKey(), entry.getValue());
        }
    }

    private static void reset(Node node, MotionState state) {
        cancelGeometry(node, state);
        cancelOpacity(node, state);
        node.setOpacity(1);
        node.setTranslateY(0);
        node.setScaleX(1);
        node.setScaleY(1);
    }

    private static final class MotionState {
        private Timeline geometry;
        private Timeline opacity;
        private ChangeListener<Boolean> buttonListener;
    }
}
