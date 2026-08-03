package me.kitakeyos.j2me.domain.speed.model;

/**
 * A thread whose speed multiplier can be retargeted while it runs.
 * <p>
 * Implemented by the instrumented emulator thread in infrastructure. Declaring
 * it here lets {@code SpeedService} push new multipliers without importing that
 * implementation.
 */
public interface SpeedAdjustable {

    /**
     * Apply a new speed multiplier. Must be safe to call from another thread.
     *
     * @param multiplier 1.0 = normal, 2.0 = twice as fast
     */
    void setSpeedMultiplier(double multiplier);

    /**
     * @return the multiplier currently in effect
     */
    double getSpeedMultiplier();
}
