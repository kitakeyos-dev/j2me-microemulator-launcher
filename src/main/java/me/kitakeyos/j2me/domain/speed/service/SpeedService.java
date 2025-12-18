package me.kitakeyos.j2me.domain.speed.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Service to manage speed multipliers for emulator instances.
 * Allows real-time speed adjustment without restarting instances.
 */
public class SpeedService {

    private static final Logger logger = Logger.getLogger(SpeedService.class.getName());
    private static final SpeedService INSTANCE = new SpeedService();

    // instanceId -> speedMultiplier (1.0 = normal, 2.0 = 2x faster, 0.5 = half
    // speed)
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

        // Also set System property so SpeedHelper in J2ME JAR can read it
        System.setProperty("j2me.speed." + instanceId, String.valueOf(multiplier));

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
        // Clear System property
        System.clearProperty("j2me.speed." + instanceId);
    }

    /**
     * Calculate adjusted sleep time based on speed multiplier.
     * 
     * @param instanceId Instance ID
     * @param originalMs Original sleep time in milliseconds
     * @return Adjusted sleep time
     */
    public long adjustSleepTime(int instanceId, long originalMs) {
        double multiplier = getSpeedMultiplier(instanceId);
        long adjusted = (long) (originalMs / multiplier);
        // Minimum sleep of 1ms to prevent busy-waiting, unless original was 0
        if (originalMs > 0 && adjusted <= 0) {
            adjusted = 1;
        }
        return adjusted;
    }

    /**
     * Check if instance has custom speed.
     */
    public boolean hasCustomSpeed(int instanceId) {
        return speedMultipliers.containsKey(instanceId);
    }
}
