package me.kitakeyos.j2me.domain.speed.service;

import me.kitakeyos.j2me.application.MainApplication;
import me.kitakeyos.j2me.domain.emulator.model.EmulatorInstance;
import me.kitakeyos.j2me.domain.emulator.service.InstanceManager;
import me.kitakeyos.j2me.infrastructure.thread.XThread;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Service to manage speed multipliers for emulator instances.
 * Allows real-time speed adjustment without restarting instances.
 *
 * The multiplier is stored here as the canonical source of truth, and
 * propagated directly to every {@link XThread} of the target instance so
 * the injected SpeedHelper can read it with zero reflection or lookup.
 */
public class SpeedService {

    private static final Logger logger = Logger.getLogger(SpeedService.class.getName());
    private static final SpeedService INSTANCE = new SpeedService();

    // instanceId -> speedMultiplier (1.0 = normal, 2.0 = 2x faster, 0.5 = half
    // speed). Canonical value; also pushed onto each XThread of the instance.
    private final Map<Integer, Double> speedMultipliers = new ConcurrentHashMap<>();

    private SpeedService() {
    }

    public static SpeedService getInstance() {
        return INSTANCE;
    }

    /**
     * Set speed multiplier for an instance.
     *
     * @param instanceId Instance ID
     * @param multiplier Speed multiplier (1.0 = normal, 2.0 = 2x faster)
     */
    public void setSpeedMultiplier(int instanceId, double multiplier) {
        if (multiplier <= 0) {
            throw new IllegalArgumentException("Speed multiplier must be positive");
        }
        speedMultipliers.put(instanceId, multiplier);

        // Push the multiplier onto each XThread's encoded name so the injected
        // SpeedHelper can read it with a single getName() call — no reflection,
        // no cross-classloader reference, no lock contention.
        propagateToThreads(instanceId, multiplier);
        logger.info(String.format("Instance #%d speed set to %.1fx", instanceId, multiplier));
    }

    /**
     * Get speed multiplier for an instance.
     *
     * @param instanceId Instance ID
     * @return Speed multiplier (default 1.0 if not set)
     */
    public double getSpeedMultiplier(int instanceId) {
        return speedMultipliers.getOrDefault(instanceId, 1.0);
    }

    /**
     * Remove speed settings for an instance (on shutdown).
     *
     * @param instanceId Instance ID
     */
    public void removeInstance(int instanceId) {
        speedMultipliers.remove(instanceId);
    }

    /**
     * Check if instance has custom speed.
     */
    public boolean hasCustomSpeed(int instanceId) {
        return speedMultipliers.containsKey(instanceId);
    }

    /**
     * Push the multiplier onto every XThread already running for this instance.
     * Newly-created XThreads pick up the value via their constructor.
     */
    private void propagateToThreads(int instanceId, double multiplier) {
        InstanceManager manager = MainApplication.INSTANCE.emulatorInstanceManager;
        if (manager == null) {
            return;
        }
        EmulatorInstance instance = manager.findInstance(instanceId);
        if (instance == null) {
            return;
        }
        for (Thread t : instance.getResourceManager().getThreads()) {
            if (t instanceof XThread) {
                ((XThread) t).setSpeedMultiplier(multiplier);
            }
        }
    }
}
