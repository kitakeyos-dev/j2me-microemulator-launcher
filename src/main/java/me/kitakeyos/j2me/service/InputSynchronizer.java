package me.kitakeyos.j2me.service;

import me.kitakeyos.j2me.model.EmulatorInstance;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Synchronizes mouse and keyboard input across multiple emulator instances
 * Simplified version that only attaches to root display components
 */
public class InputSynchronizer {

    private static final Logger logger = Logger.getLogger(InputSynchronizer.class.getName());

    private final EmulatorInstanceManager instanceManager;
    private boolean enabled = false;
    private final Set<Component> isDispatching = ConcurrentHashMap.newKeySet();

    // Store listeners per instance for cleanup
    private final Map<EmulatorInstance, MouseAdapter> mouseListeners = new ConcurrentHashMap<>();
    private final Map<EmulatorInstance, KeyAdapter> keyListeners = new ConcurrentHashMap<>();

    public InputSynchronizer(EmulatorInstanceManager instanceManager) {
        this.instanceManager = instanceManager;
    }

    /**
     * Enable or disable input synchronization
     */
    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) {
            return; // No change
        }

        this.enabled = enabled;

        if (enabled) {
            attachListenersToAllInstances();
            logger.info("Input synchronization enabled");
        } else {
            detachListenersFromAllInstances();
            logger.info("Input synchronization disabled");
        }
    }

    /**
     * Check if synchronization is enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Attach listeners to a new instance (called when instance is added)
     */
    public void attachListenersToInstance(EmulatorInstance instance) {
        if (!enabled) {
            return;
        }

        JPanel display = instance.getEmulatorDisplay();
        if (display != null) {
            MouseAdapter mouseListener = createMouseListener();
            KeyAdapter keyListener = createKeyListener();

            attachListenersRecursively(display, mouseListener, keyListener);

            mouseListeners.put(instance, mouseListener);
            keyListeners.put(instance, keyListener);

            logger.info("Attached listeners to instance #" + instance.getInstanceId());
        }
    }

    /**
     * Detach listeners from an instance (called when instance is removed)
     */
    public void detachListenersFromInstance(EmulatorInstance instance) {
        JPanel display = instance.getEmulatorDisplay();
        if (display != null) {
            MouseAdapter mouseListener = mouseListeners.remove(instance);
            KeyAdapter keyListener = keyListeners.remove(instance);

            if (mouseListener != null && keyListener != null) {
                detachListenersRecursively(display, mouseListener, keyListener);
                logger.info("Detached listeners from instance #" + instance.getInstanceId());
            }
        }
    }

    /**
     * Attach listeners to all running instances
     */
    private void attachListenersToAllInstances() {
        List<EmulatorInstance> instances = instanceManager.getRunningInstances();
        for (EmulatorInstance instance : instances) {
            attachListenersToInstance(instance);
        }
        logger.info("Attached listeners to " + instances.size() + " instance(s)");
    }

    /**
     * Detach listeners from all running instances
     */
    private void detachListenersFromAllInstances() {
        List<EmulatorInstance> instances = instanceManager.getRunningInstances();
        for (EmulatorInstance instance : instances) {
            detachListenersFromInstance(instance);
        }
        mouseListeners.clear();
        keyListeners.clear();
        logger.info("Detached listeners from " + instances.size() + " instance(s)");
    }

    /**
     * Attach listeners to a component and all its children recursively
     */
    private void attachListenersRecursively(Component component, MouseAdapter mouseListener, KeyAdapter keyListener) {
        component.addMouseListener(mouseListener);
        component.addKeyListener(keyListener);

        // Make component focusable for keyboard events
        if (component instanceof JComponent) {
            ((JComponent) component).setFocusable(true);
        }

        // Recursively attach to all child components
        if (component instanceof Container) {
            Container container = (Container) component;
            for (Component child : container.getComponents()) {
                attachListenersRecursively(child, mouseListener, keyListener);
            }
        }
    }

    /**
     * Detach listeners from a component and all its children recursively
     */
    private void detachListenersRecursively(Component component, MouseAdapter mouseListener, KeyAdapter keyListener) {
        component.removeMouseListener(mouseListener);
        component.removeKeyListener(keyListener);

        // Recursively detach from all child components
        if (component instanceof Container) {
            Container container = (Container) component;
            for (Component child : container.getComponents()) {
                detachListenersRecursively(child, mouseListener, keyListener);
            }
        }
    }

    /**
     * Create mouse listener for a specific instance
     */
    private MouseAdapter createMouseListener() {
        return new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                Component source = e.getComponent();
                if (!isDispatching.contains(source)) {
                    broadcastMouseEvent(e);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                Component source = e.getComponent();
                if (!isDispatching.contains(source)) {
                    broadcastMouseEvent(e);
                }
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                Component source = e.getComponent();
                if (!isDispatching.contains(source)) {
                    broadcastMouseEvent(e);
                }
            }
        };
    }

    /**
     * Create key listener for a specific instance
     */
    private KeyAdapter createKeyListener() {
        return new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                Component source = e.getComponent();
                if (!isDispatching.contains(source)) {
                    broadcastKeyEvent(e);
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                Component source = e.getComponent();
                if (!isDispatching.contains(source)) {
                    broadcastKeyEvent(e);
                }
            }

            @Override
            public void keyTyped(KeyEvent e) {
                Component source = e.getComponent();
                if (!isDispatching.contains(source)) {
                    broadcastKeyEvent(e);
                }
            }
        };
    }

    /**
     * Broadcast a mouse event to all instances
     */
    private void broadcastMouseEvent(MouseEvent sourceEvent) {
        Component sourceComponent = sourceEvent.getComponent();
        List<EmulatorInstance> instances = instanceManager.getRunningInstances();

        // Find which instance the source belongs to
        EmulatorInstance sourceInstance = findInstanceForComponent(sourceComponent);
        if (sourceInstance == null) {
            return;
        }

        // Get the relative position within the source display
        Point relativePoint = getRelativePointInDisplay(sourceComponent, sourceEvent.getPoint(), sourceInstance);
        if (relativePoint == null) {
            return;
        }

        // Broadcast to all other instances
        for (EmulatorInstance targetInstance : instances) {
            if (targetInstance == sourceInstance) {
                continue; // Skip source instance
            }

            JPanel targetDisplay = targetInstance.getEmulatorDisplay();
            if (targetDisplay != null) {
                // Find the corresponding component in target display
                Component targetComponent = findComponentAtPoint(targetDisplay, relativePoint);
                if (targetComponent != null) {
                    dispatchMouseEventToComponent(sourceEvent, targetComponent, relativePoint);
                }
            }
        }
    }

    /**
     * Broadcast a key event to all instances
     */
    private void broadcastKeyEvent(KeyEvent sourceEvent) {
        Component sourceComponent = sourceEvent.getComponent();
        List<EmulatorInstance> instances = instanceManager.getRunningInstances();

        // Find which instance the source belongs to
        EmulatorInstance sourceInstance = findInstanceForComponent(sourceComponent);
        if (sourceInstance == null) {
            return;
        }

        // Broadcast to all other instances
        for (EmulatorInstance targetInstance : instances) {
            if (targetInstance == sourceInstance) {
                continue; // Skip source instance
            }

            JPanel targetDisplay = targetInstance.getEmulatorDisplay();
            if (targetDisplay != null) {
                // Find the corresponding component
                Component targetComponent = findCorrespondingComponent(sourceComponent, sourceInstance, targetDisplay);
                if (targetComponent != null) {
                    dispatchKeyEventToComponent(sourceEvent, targetComponent);
                }
            }
        }
    }

    /**
     * Find which instance a component belongs to
     */
    private EmulatorInstance findInstanceForComponent(Component component) {
        List<EmulatorInstance> instances = instanceManager.getRunningInstances();
        for (EmulatorInstance instance : instances) {
            JPanel display = instance.getEmulatorDisplay();
            if (display != null && isComponentInHierarchy(component, display)) {
                return instance;
            }
        }
        return null;
    }

    /**
     * Check if a component is in the hierarchy of a container
     */
    private boolean isComponentInHierarchy(Component component, Container container) {
        Component current = component;
        while (current != null) {
            if (current == container) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }

    /**
     * Get the relative position of a point within the display
     */
    private Point getRelativePointInDisplay(Component component, Point point, EmulatorInstance instance) {
        JPanel display = instance.getEmulatorDisplay();
        if (display == null) {
            return null;
        }

        try {
            // Convert point to display's coordinate system
            Point screenPoint = component.getLocationOnScreen();
            screenPoint.translate(point.x, point.y);

            Point displayScreenPoint = display.getLocationOnScreen();

            return new Point(
                screenPoint.x - displayScreenPoint.x,
                screenPoint.y - displayScreenPoint.y
            );
        } catch (IllegalComponentNotShownException e) {
            return null;
        }
    }

    /**
     * Find component at a specific point in the display
     */
    private Component findComponentAtPoint(Container container, Point point) {
        Component deepest = SwingUtilities.getDeepestComponentAt(container, point.x, point.y);
        return deepest != null ? deepest : container;
    }

    /**
     * Find the corresponding component in another instance's display
     */
    private Component findCorrespondingComponent(Component sourceComponent, EmulatorInstance sourceInstance, JPanel targetDisplay) {
        // Build path from source component to its display root
        java.util.List<Integer> path = new java.util.ArrayList<>();
        Component current = sourceComponent;
        JPanel sourceDisplay = sourceInstance.getEmulatorDisplay();

        while (current != null && current != sourceDisplay) {
            Container parent = current.getParent();
            if (parent != null) {
                // Find index of current in parent
                for (int i = 0; i < parent.getComponentCount(); i++) {
                    if (parent.getComponent(i) == current) {
                        path.add(0, i);
                        break;
                    }
                }
            }
            current = parent;
        }

        // Navigate the same path in target display
        Component target = targetDisplay;
        for (Integer index : path) {
            if (target instanceof Container) {
                Container container = (Container) target;
                if (index < container.getComponentCount()) {
                    target = container.getComponent(index);
                } else {
                    // Path doesn't match, use root display
                    return targetDisplay;
                }
            } else {
                // Path doesn't match, use root display
                return targetDisplay;
            }
        }

        return target;
    }

    /**
     * Dispatch a mouse event to a target component
     */
    private void dispatchMouseEventToComponent(MouseEvent sourceEvent, Component targetComponent, Point relativePoint) {
        // Convert relative point to target component's coordinate system
        try {
            Point targetScreenPoint = targetComponent.getLocationOnScreen();
            JPanel targetDisplay = (JPanel) targetComponent.getParent();
            while (targetDisplay != null && !(targetDisplay.getParent() == null || targetDisplay.getClientProperty("wrapperPanel") != null)) {
                targetDisplay = (JPanel) targetDisplay.getParent();
            }

            Point displayScreenPoint = targetDisplay.getLocationOnScreen();
            Point targetPoint = new Point(
                relativePoint.x + displayScreenPoint.x - targetScreenPoint.x,
                relativePoint.y + displayScreenPoint.y - targetScreenPoint.y
            );

            // Create a new mouse event for the target component
            MouseEvent newEvent = new MouseEvent(
                    targetComponent,
                    sourceEvent.getID(),
                    System.currentTimeMillis(),
                    sourceEvent.getModifiersEx(),
                    targetPoint.x,
                    targetPoint.y,
                    sourceEvent.getClickCount(),
                    sourceEvent.isPopupTrigger(),
                    sourceEvent.getButton()
            );

            // Mark as dispatching to prevent infinite loop
            isDispatching.add(targetComponent);

            // Dispatch the event
            SwingUtilities.invokeLater(() -> {
                try {
                    targetComponent.dispatchEvent(newEvent);
                } finally {
                    isDispatching.remove(targetComponent);
                }
            });
        } catch (Exception e) {
            // Ignore errors in coordinate conversion
            isDispatching.remove(targetComponent);
        }
    }

    /**
     * Dispatch a key event to a target component
     */
    private void dispatchKeyEventToComponent(KeyEvent sourceEvent, Component targetComponent) {
        // Create a new key event for the target component
        KeyEvent newEvent = new KeyEvent(
                targetComponent,
                sourceEvent.getID(),
                System.currentTimeMillis(),
                sourceEvent.getModifiersEx(),
                sourceEvent.getKeyCode(),
                sourceEvent.getKeyChar(),
                sourceEvent.getKeyLocation()
        );

        // Mark as dispatching to prevent infinite loop
        isDispatching.add(targetComponent);

        // Dispatch the event
        SwingUtilities.invokeLater(() -> {
            try {
                targetComponent.dispatchEvent(newEvent);
            } finally {
                isDispatching.remove(targetComponent);
            }
        });
    }
}
