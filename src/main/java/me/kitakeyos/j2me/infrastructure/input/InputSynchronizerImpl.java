package me.kitakeyos.j2me.infrastructure.input;

import me.kitakeyos.j2me.domain.emulator.input.InputSynchronizer;
import me.kitakeyos.j2me.domain.emulator.model.EmulatorInstance;
import me.kitakeyos.j2me.domain.emulator.service.InstanceManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Synchronizes mouse and keyboard input across multiple emulator instances.
 *
 * Optimizations over naive approach:
 * - Source instance is stored in listener, eliminating per-event instance lookup.
 * - Mouse coordinates use SwingUtilities.convertPoint instead of getLocationOnScreen.
 * - Key events dispatch directly to target devicePanel (no component path matching).
 * - Re-entrancy guard uses a simple thread-local boolean instead of a component set.
 */
public class InputSynchronizerImpl implements InputSynchronizer {

    private static final Logger logger = Logger.getLogger(InputSynchronizerImpl.class.getName());

    private final InstanceManager instanceManager;
    private boolean enabled = false;
    private boolean scaleBySize = false;

    // Thread-local guard to prevent infinite dispatch loops
    private final ThreadLocal<Boolean> dispatching = ThreadLocal.withInitial(() -> Boolean.FALSE);

    // Store listeners per instance for cleanup
    private final Map<EmulatorInstance, MouseAdapter> mouseListeners = new ConcurrentHashMap<>();
    private final Map<EmulatorInstance, KeyAdapter> keyListeners = new ConcurrentHashMap<>();

    public InputSynchronizerImpl(InstanceManager instanceManager) {
        this.instanceManager = instanceManager;
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) {
            return;
        }

        this.enabled = enabled;

        if (enabled) {
            for (EmulatorInstance instance : instanceManager.getRunningInstances()) {
                attachListenersToInstance(instance);
            }
            logger.info("Input synchronization enabled");
        } else {
            for (EmulatorInstance instance : instanceManager.getRunningInstances()) {
                detachListenersFromInstance(instance);
            }
            mouseListeners.clear();
            keyListeners.clear();
            logger.info("Input synchronization disabled");
        }
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setScaleBySize(boolean scaleBySize) {
        this.scaleBySize = scaleBySize;
        logger.info("Input scaling by size " + (scaleBySize ? "enabled" : "disabled"));
    }

    @Override
    public boolean isScaleBySize() {
        return scaleBySize;
    }

    @Override
    public void attachListenersToInstance(EmulatorInstance instance) {
        if (!enabled) {
            return;
        }

        JPanel devicePanel = instance.getDevicePanel();
        if (devicePanel == null) {
            return;
        }

        // Each listener knows its source instance - no per-event lookup needed
        MouseAdapter mouseListener = new SyncMouseAdapter(instance);
        KeyAdapter keyListener = new SyncKeyAdapter(instance);

        attachRecursively(devicePanel, mouseListener, keyListener);

        mouseListeners.put(instance, mouseListener);
        keyListeners.put(instance, keyListener);

        logger.info("Attached listeners to instance #" + instance.getInstanceId());
    }

    @Override
    public void detachListenersFromInstance(EmulatorInstance instance) {
        JPanel devicePanel = instance.getDevicePanel();
        if (devicePanel == null) {
            return;
        }

        MouseAdapter mouseListener = mouseListeners.remove(instance);
        KeyAdapter keyListener = keyListeners.remove(instance);

        if (mouseListener != null && keyListener != null) {
            detachRecursively(devicePanel, mouseListener, keyListener);
            logger.info("Detached listeners from instance #" + instance.getInstanceId());
        }
    }

    // ======== Mouse broadcast ========

    private void broadcastMouse(EmulatorInstance sourceInstance, MouseEvent e) {
        if (dispatching.get()) {
            return;
        }

        JPanel sourcePanel = sourceInstance.getDevicePanel();
        if (sourcePanel == null) {
            return;
        }

        // Convert event point to source devicePanel coordinates
        Point pointInSource;
        try {
            pointInSource = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), sourcePanel);
        } catch (Exception ex) {
            return;
        }

        List<EmulatorInstance> targets = instanceManager.getRunningInstances();
        for (EmulatorInstance target : targets) {
            if (target == sourceInstance) {
                continue;
            }

            JPanel targetPanel = target.getDevicePanel();
            if (targetPanel == null) {
                continue;
            }

            // Scale coordinates if panels have different sizes
            Point pointInTarget = scaleBySize
                    ? scalePoint(pointInSource, sourcePanel, targetPanel)
                    : pointInSource;

            // Find deepest component at target point
            Component targetComponent = SwingUtilities.getDeepestComponentAt(
                    targetPanel, pointInTarget.x, pointInTarget.y);
            if (targetComponent == null) {
                targetComponent = targetPanel;
            }

            // Convert to target component's local coordinates
            Point localPoint = SwingUtilities.convertPoint(targetPanel, pointInTarget, targetComponent);

            MouseEvent newEvent = new MouseEvent(
                    targetComponent,
                    e.getID(),
                    System.currentTimeMillis(),
                    e.getModifiersEx(),
                    localPoint.x,
                    localPoint.y,
                    e.getClickCount(),
                    e.isPopupTrigger(),
                    e.getButton());

            dispatching.set(Boolean.TRUE);
            try {
                targetComponent.dispatchEvent(newEvent);
            } finally {
                dispatching.set(Boolean.FALSE);
            }
        }
    }

    // ======== Key broadcast ========

    private void broadcastKey(EmulatorInstance sourceInstance, KeyEvent e) {
        if (dispatching.get()) {
            return;
        }

        List<EmulatorInstance> targets = instanceManager.getRunningInstances();
        for (EmulatorInstance target : targets) {
            if (target == sourceInstance) {
                continue;
            }

            // Dispatch key events directly to target devicePanel
            JPanel targetPanel = target.getDevicePanel();
            if (targetPanel == null) {
                continue;
            }

            KeyEvent newEvent = new KeyEvent(
                    targetPanel,
                    e.getID(),
                    System.currentTimeMillis(),
                    e.getModifiersEx(),
                    e.getKeyCode(),
                    e.getKeyChar(),
                    e.getKeyLocation());

            dispatching.set(Boolean.TRUE);
            try {
                targetPanel.dispatchEvent(newEvent);
            } finally {
                dispatching.set(Boolean.FALSE);
            }
        }
    }

    // ======== Scaling ========

    private Point scalePoint(Point source, JPanel sourcePanel, JPanel targetPanel) {
        int sw = sourcePanel.getWidth();
        int sh = sourcePanel.getHeight();
        if (sw == 0 || sh == 0) {
            return source;
        }
        int tw = targetPanel.getWidth();
        int th = targetPanel.getHeight();
        return new Point(
                (int) Math.round((double) source.x * tw / sw),
                (int) Math.round((double) source.y * th / sh));
    }

    // ======== Recursive attach/detach ========

    private void attachRecursively(Component component, MouseAdapter mouse, KeyAdapter key) {
        component.addMouseListener(mouse);
        component.addKeyListener(key);
        if (component instanceof JComponent) {
            component.setFocusable(true);
        }
        if (component instanceof Container) {
            for (Component child : ((Container) component).getComponents()) {
                attachRecursively(child, mouse, key);
            }
        }
    }

    private void detachRecursively(Component component, MouseAdapter mouse, KeyAdapter key) {
        component.removeMouseListener(mouse);
        component.removeKeyListener(key);
        if (component instanceof Container) {
            for (Component child : ((Container) component).getComponents()) {
                detachRecursively(child, mouse, key);
            }
        }
    }

    // ======== Listener classes that know their source instance ========

    private class SyncMouseAdapter extends MouseAdapter {
        private final EmulatorInstance sourceInstance;

        SyncMouseAdapter(EmulatorInstance sourceInstance) {
            this.sourceInstance = sourceInstance;
        }

        @Override
        public void mousePressed(MouseEvent e) {
            broadcastMouse(sourceInstance, e);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            broadcastMouse(sourceInstance, e);
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            broadcastMouse(sourceInstance, e);
        }
    }

    private class SyncKeyAdapter extends KeyAdapter {
        private final EmulatorInstance sourceInstance;

        SyncKeyAdapter(EmulatorInstance sourceInstance) {
            this.sourceInstance = sourceInstance;
        }

        @Override
        public void keyPressed(KeyEvent e) {
            broadcastKey(sourceInstance, e);
        }

        @Override
        public void keyReleased(KeyEvent e) {
            broadcastKey(sourceInstance, e);
        }

        @Override
        public void keyTyped(KeyEvent e) {
            broadcastKey(sourceInstance, e);
        }
    }
}
