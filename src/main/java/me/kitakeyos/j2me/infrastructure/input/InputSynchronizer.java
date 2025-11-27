package me.kitakeyos.j2me.infrastructure.input;

import me.kitakeyos.j2me.domain.emulator.service.InstanceManager;

import me.kitakeyos.j2me.domain.emulator.model.EmulatorInstance;

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

    private final InstanceManager instanceManager;
    private boolean enabled = false;
    private boolean scaleBySize = false;
    private final Set<Component> isDispatching = ConcurrentHashMap.newKeySet();

    // Store listeners per instance for cleanup
    private final Map<EmulatorInstance, MouseAdapter> mouseListeners = new ConcurrentHashMap<>();
    private final Map<EmulatorInstance, KeyAdapter> keyListeners = new ConcurrentHashMap<>();

    public InputSynchronizer(InstanceManager instanceManager) {
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
     * Enable or disable scaling by device panel size
     */
    public void setScaleBySize(boolean scaleBySize) {
        this.scaleBySize = scaleBySize;
        logger.info("Input scaling by size " + (scaleBySize ? "enabled" : "disabled"));
    }

    /**
     * Check if scaling by size is enabled
     */
    public boolean isScaleBySize() {
        return scaleBySize;
    }

    /**
     * Attach listeners to a new instance (called when instance is added)
     */
    public void attachListenersToInstance(EmulatorInstance instance) {
        if (!enabled) {
            return;
        }

        JPanel devicePanel = instance.getDevicePanel();
        if (devicePanel != null) {
            MouseAdapter mouseListener = createMouseListener();
            KeyAdapter keyListener = createKeyListener();

            attachListenersRecursively(devicePanel, mouseListener, keyListener);

            mouseListeners.put(instance, mouseListener);
            keyListeners.put(instance, keyListener);

            logger.info("Attached listeners to instance #" + instance.getInstanceId());
        }
    }

    /**
     * Detach listeners from an instance (called when instance is removed)
     */
    public void detachListenersFromInstance(EmulatorInstance instance) {
        JPanel devicePanel = instance.getDevicePanel();
        if (devicePanel != null) {
            MouseAdapter mouseListener = mouseListeners.remove(instance);
            KeyAdapter keyListener = keyListeners.remove(instance);

            if (mouseListener != null && keyListener != null) {
                detachListenersRecursively(devicePanel, mouseListener, keyListener);
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

        // Get source device panel for size calculation
        JPanel sourceDevicePanel = sourceInstance.getDevicePanel();
        if (sourceDevicePanel == null) {
            return;
        }

        // Broadcast to all other instances
        for (EmulatorInstance targetInstance : instances) {
            if (targetInstance == sourceInstance) {
                continue; // Skip source instance
            }

            JPanel targetDevicePanel = targetInstance.getDevicePanel();
            if (targetDevicePanel != null) {
                // Calculate scaled point if scaling is enabled
                Point targetPoint = relativePoint;
                if (scaleBySize) {
                    targetPoint = scalePointBySize(relativePoint, sourceDevicePanel, targetDevicePanel);
                }

                // Find the corresponding component in target display
                Component targetComponent = findComponentAtPoint(targetDevicePanel, targetPoint);
                if (targetComponent != null) {
                    dispatchMouseEventToComponent(sourceEvent, targetComponent, targetPoint, targetDevicePanel);
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

            JPanel targetDevicePanel = targetInstance.getDevicePanel();
            if (targetDevicePanel != null) {
                // Find the corresponding component
                Component targetComponent = findCorrespondingComponent(sourceComponent, sourceInstance, targetDevicePanel);
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
            JPanel devicePanel = instance.getDevicePanel();
            if (devicePanel != null && isComponentInHierarchy(component, devicePanel)) {
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
        JPanel devicePanel = instance.getDevicePanel();
        if (devicePanel == null) {
            return null;
        }

        try {
            // Convert point to display's coordinate system
            Point screenPoint = component.getLocationOnScreen();
            screenPoint.translate(point.x, point.y);

            Point displayScreenPoint = devicePanel.getLocationOnScreen();

            return new Point(
                screenPoint.x - displayScreenPoint.x,
                screenPoint.y - displayScreenPoint.y
            );
        } catch (Exception e) {
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
     * Scale a point from source device panel size to target device panel size
     */
    private Point scalePointBySize(Point sourcePoint, JPanel sourceDevicePanel, JPanel targetDevicePanel) {
        int sourceWidth = sourceDevicePanel.getWidth();
        int sourceHeight = sourceDevicePanel.getHeight();
        int targetWidth = targetDevicePanel.getWidth();
        int targetHeight = targetDevicePanel.getHeight();

        // Avoid division by zero
        if (sourceWidth == 0 || sourceHeight == 0) {
            return sourcePoint;
        }

        // Calculate scale ratios
        double scaleX = (double) targetWidth / sourceWidth;
        double scaleY = (double) targetHeight / sourceHeight;

        // Scale the point
        int scaledX = (int) Math.round(sourcePoint.x * scaleX);
        int scaledY = (int) Math.round(sourcePoint.y * scaleY);

        return new Point(scaledX, scaledY);
    }

    /**
     * Find the corresponding component in another instance's display
     */
    private Component findCorrespondingComponent(Component sourceComponent, EmulatorInstance sourceInstance, JPanel targetDevicePanel) {
        // Build path from source component to its display root
        java.util.List<Integer> path = new java.util.ArrayList<>();
        Component current = sourceComponent;
        JPanel sourceDevicePanel = sourceInstance.getDevicePanel();

        while (current != null && current != sourceDevicePanel) {
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
        Component target = targetDevicePanel;
        for (Integer index : path) {
            if (target instanceof Container) {
                Container container = (Container) target;
                if (index < container.getComponentCount()) {
                    target = container.getComponent(index);
                } else {
                    // Path doesn't match, use root display
                    return targetDevicePanel;
                }
            } else {
                // Path doesn't match, use root display
                return targetDevicePanel;
            }
        }

        return target;
    }

    /**
     * Dispatch a mouse event to a target component
     */
    private void dispatchMouseEventToComponent(MouseEvent sourceEvent, Component targetComponent, Point relativePoint, JPanel targetDevicePanel) {
        // Convert relative point from devicePanel coordinates to targetComponent coordinates
        try {
            // relativePoint is in devicePanel coordinates
            // Convert it to targetComponent's coordinate system
            Point targetPoint = SwingUtilities.convertPoint(targetDevicePanel, relativePoint, targetComponent);

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
