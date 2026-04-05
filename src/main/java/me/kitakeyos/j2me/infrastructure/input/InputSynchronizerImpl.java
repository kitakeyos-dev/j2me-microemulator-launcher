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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Synchronizes mouse and keyboard input across selected emulator instances.
 * Only instances whose IDs are in the syncedInstanceIds set will participate.
 */
public class InputSynchronizerImpl implements InputSynchronizer {

    private static final Logger logger = Logger.getLogger(InputSynchronizerImpl.class.getName());

    private final InstanceManager instanceManager;
    private boolean enabled = false;
    private boolean scaleBySize = false;

    // Which instances participate in sync
    private final Set<Integer> syncedInstanceIds = ConcurrentHashMap.newKeySet();

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
            // Attach listeners to all synced running instances
            for (EmulatorInstance instance : instanceManager.getRunningInstances()) {
                if (syncedInstanceIds.contains(instance.getInstanceId())) {
                    attachListenersToInstance(instance);
                }
            }
            logger.info("Input synchronization enabled for instances: " + syncedInstanceIds);
        } else {
            // Detach all listeners
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
    }

    @Override
    public boolean isScaleBySize() {
        return scaleBySize;
    }

    @Override
    public void setSyncedInstanceIds(Set<Integer> instanceIds) {
        // Detach from instances no longer synced
        Set<Integer> removed = new HashSet<>(syncedInstanceIds);
        removed.removeAll(instanceIds);
        for (int id : removed) {
            EmulatorInstance instance = instanceManager.findInstance(id);
            if (instance != null) {
                detachListenersFromInstance(instance);
            }
        }

        // Update the set
        syncedInstanceIds.clear();
        syncedInstanceIds.addAll(instanceIds);

        // Attach to newly synced instances
        if (enabled) {
            Set<Integer> added = new HashSet<>(instanceIds);
            added.removeAll(removed); // Only truly new ones
            for (int id : syncedInstanceIds) {
                EmulatorInstance instance = instanceManager.findInstance(id);
                if (instance != null && !mouseListeners.containsKey(instance)) {
                    attachListenersToInstance(instance);
                }
            }
        }

        logger.info("Synced instances updated: " + syncedInstanceIds);
    }

    @Override
    public Set<Integer> getSyncedInstanceIds() {
        return new HashSet<>(syncedInstanceIds);
    }

    @Override
    public void attachListenersToInstance(EmulatorInstance instance) {
        if (!enabled || !syncedInstanceIds.contains(instance.getInstanceId())) {
            return;
        }

        JPanel devicePanel = instance.getDevicePanel();
        if (devicePanel == null) {
            return;
        }

        // Don't attach twice
        if (mouseListeners.containsKey(instance)) {
            return;
        }

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
            // Only dispatch to synced instances
            if (!syncedInstanceIds.contains(target.getInstanceId())) {
                continue;
            }

            JPanel targetPanel = target.getDevicePanel();
            if (targetPanel == null) {
                continue;
            }

            Point pointInTarget = scaleBySize
                    ? scalePoint(pointInSource, sourcePanel, targetPanel)
                    : pointInSource;

            // Clamp point to find target component, but use unclamped for the event
            int clampedX = Math.max(0, Math.min(pointInTarget.x, targetPanel.getWidth() - 1));
            int clampedY = Math.max(0, Math.min(pointInTarget.y, targetPanel.getHeight() - 1));

            Component targetComponent = SwingUtilities.getDeepestComponentAt(
                    targetPanel, clampedX, clampedY);
            if (targetComponent == null) {
                targetComponent = targetPanel;
            }

            // Convert the actual (unclamped) point to target component coordinates
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
            if (!syncedInstanceIds.contains(target.getInstanceId())) {
                continue;
            }

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
        component.addMouseMotionListener(mouse);
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
        component.removeMouseMotionListener(mouse);
        component.removeKeyListener(key);
        if (component instanceof Container) {
            for (Component child : ((Container) component).getComponents()) {
                detachRecursively(child, mouse, key);
            }
        }
    }

    // ======== Listener classes ========

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

        @Override
        public void mouseDragged(MouseEvent e) {
            broadcastMouse(sourceInstance, e);
        }

        @Override
        public void mouseMoved(MouseEvent e) {
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
