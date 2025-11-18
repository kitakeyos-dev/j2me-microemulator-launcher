package me.kitakeyos.j2me.service;

import me.kitakeyos.j2me.model.EmulatorInstance;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.logging.Logger;

/**
 * Synchronizes mouse and keyboard input across multiple emulator instances
 */
public class InputSynchronizer {

    private static final Logger logger = Logger.getLogger(InputSynchronizer.class.getName());

    private final EmulatorInstanceManager instanceManager;
    private boolean enabled = false;
    private MouseAdapter sharedMouseListener;
    private KeyAdapter sharedKeyListener;

    public InputSynchronizer(EmulatorInstanceManager instanceManager) {
        this.instanceManager = instanceManager;
        initializeListeners();
    }

    private void initializeListeners() {
        // Shared mouse listener that broadcasts events to all instances
        sharedMouseListener = new MouseAdapter() {
            private boolean isDispatching = false;

            @Override
            public void mousePressed(MouseEvent e) {
                if (!isDispatching) {
                    isDispatching = true;
                    broadcastMouseEvent(e);
                    isDispatching = false;
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (!isDispatching) {
                    isDispatching = true;
                    broadcastMouseEvent(e);
                    isDispatching = false;
                }
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (!isDispatching) {
                    isDispatching = true;
                    broadcastMouseEvent(e);
                    isDispatching = false;
                }
            }
        };

        // Shared key listener that broadcasts events to all instances
        sharedKeyListener = new KeyAdapter() {
            private boolean isDispatching = false;

            @Override
            public void keyPressed(KeyEvent e) {
                if (!isDispatching) {
                    isDispatching = true;
                    broadcastKeyEvent(e);
                    isDispatching = false;
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                if (!isDispatching) {
                    isDispatching = true;
                    broadcastKeyEvent(e);
                    isDispatching = false;
                }
            }

            @Override
            public void keyTyped(KeyEvent e) {
                if (!isDispatching) {
                    isDispatching = true;
                    broadcastKeyEvent(e);
                    isDispatching = false;
                }
            }
        };
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
            attachListenersToComponent(display);
            logger.fine("Attached listeners to instance #" + instance.getInstanceId());
        }
    }

    /**
     * Detach listeners from an instance (called when instance is removed)
     */
    public void detachListenersFromInstance(EmulatorInstance instance) {
        JPanel display = instance.getEmulatorDisplay();
        if (display != null) {
            detachListenersFromComponent(display);
            logger.fine("Detached listeners from instance #" + instance.getInstanceId());
        }
    }

    /**
     * Attach listeners to all running instances
     */
    private void attachListenersToAllInstances() {
        List<EmulatorInstance> instances = instanceManager.getRunningInstances();
        for (EmulatorInstance instance : instances) {
            JPanel display = instance.getEmulatorDisplay();
            if (display != null) {
                attachListenersToComponent(display);
            }
        }
    }

    /**
     * Detach listeners from all running instances
     */
    private void detachListenersFromAllInstances() {
        List<EmulatorInstance> instances = instanceManager.getRunningInstances();
        for (EmulatorInstance instance : instances) {
            JPanel display = instance.getEmulatorDisplay();
            if (display != null) {
                detachListenersFromComponent(display);
            }
        }
    }

    /**
     * Attach listeners to a component and all its children recursively
     */
    private void attachListenersToComponent(Component component) {
        component.addMouseListener(sharedMouseListener);
        component.addKeyListener(sharedKeyListener);

        // Also attach to all child components recursively
        if (component instanceof Container) {
            Container container = (Container) component;
            for (Component child : container.getComponents()) {
                attachListenersToComponent(child);
            }
        }
    }

    /**
     * Detach listeners from a component and all its children recursively
     */
    private void detachListenersFromComponent(Component component) {
        component.removeMouseListener(sharedMouseListener);
        component.removeKeyListener(sharedKeyListener);

        // Also detach from all child components recursively
        if (component instanceof Container) {
            Container container = (Container) component;
            for (Component child : container.getComponents()) {
                detachListenersFromComponent(child);
            }
        }
    }

    /**
     * Broadcast a mouse event to all instances except the source
     */
    private void broadcastMouseEvent(MouseEvent sourceEvent) {
        Component sourceComponent = sourceEvent.getComponent();
        List<EmulatorInstance> instances = instanceManager.getRunningInstances();

        for (EmulatorInstance instance : instances) {
            JPanel display = instance.getEmulatorDisplay();
            if (display != null && !isComponentInHierarchy(sourceComponent, display)) {
                // Find the corresponding component in this instance
                Component targetComponent = findCorrespondingComponent(sourceComponent, display);
                if (targetComponent != null) {
                    dispatchMouseEventToComponent(sourceEvent, targetComponent);
                }
            }
        }
    }

    /**
     * Broadcast a key event to all instances except the source
     */
    private void broadcastKeyEvent(KeyEvent sourceEvent) {
        Component sourceComponent = sourceEvent.getComponent();
        List<EmulatorInstance> instances = instanceManager.getRunningInstances();

        for (EmulatorInstance instance : instances) {
            JPanel display = instance.getEmulatorDisplay();
            if (display != null && !isComponentInHierarchy(sourceComponent, display)) {
                // Find the corresponding component in this instance
                Component targetComponent = findCorrespondingComponent(sourceComponent, display);
                if (targetComponent != null) {
                    dispatchKeyEventToComponent(sourceEvent, targetComponent);
                }
            }
        }
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
     * Find the corresponding component in another instance's display
     * This tries to find a component at the same relative position in the hierarchy
     */
    private Component findCorrespondingComponent(Component sourceComponent, JPanel targetDisplay) {
        // Build path from source component to its root
        java.util.List<Integer> path = new java.util.ArrayList<>();
        Component current = sourceComponent;
        while (current != null && current.getParent() != null) {
            Container parent = current.getParent();
            // Find index of current in parent
            for (int i = 0; i < parent.getComponentCount(); i++) {
                if (parent.getComponent(i) == current) {
                    path.add(0, i); // Add to beginning
                    break;
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
                    // Path doesn't match, fall back to root display
                    return targetDisplay;
                }
            } else {
                // Path doesn't match, fall back to root display
                return targetDisplay;
            }
        }

        return target;
    }

    /**
     * Dispatch a mouse event to a target component
     */
    private void dispatchMouseEventToComponent(MouseEvent sourceEvent, Component targetComponent) {
        // Create a new mouse event for the target component
        MouseEvent newEvent = new MouseEvent(
                targetComponent,
                sourceEvent.getID(),
                sourceEvent.getWhen(),
                sourceEvent.getModifiersEx(),
                sourceEvent.getX(),
                sourceEvent.getY(),
                sourceEvent.getClickCount(),
                sourceEvent.isPopupTrigger(),
                sourceEvent.getButton()
        );

        // Dispatch the event to the target component
        SwingUtilities.invokeLater(() -> {
            targetComponent.dispatchEvent(newEvent);
        });
    }

    /**
     * Dispatch a key event to a target component
     */
    private void dispatchKeyEventToComponent(KeyEvent sourceEvent, Component targetComponent) {
        // Create a new key event for the target component
        KeyEvent newEvent = new KeyEvent(
                targetComponent,
                sourceEvent.getID(),
                sourceEvent.getWhen(),
                sourceEvent.getModifiersEx(),
                sourceEvent.getKeyCode(),
                sourceEvent.getKeyChar(),
                sourceEvent.getKeyLocation()
        );

        // Dispatch the event to the target component
        SwingUtilities.invokeLater(() -> {
            targetComponent.dispatchEvent(newEvent);
        });
    }
}
