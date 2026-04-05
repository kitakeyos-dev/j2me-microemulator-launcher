package me.kitakeyos.j2me.infrastructure.bytecode;

/**
 * Runtime config read by the injected paint-throttle guard in
 * {@code SwingDisplayComponent.repaintRequest(IIII)V}.
 *
 * <p>The bytecode emits a {@code GETSTATIC} against
 * {@link #intervalMs}, so mutating this field takes effect on the very next
 * paint call — no restart needed.
 *
 * <p>This class lives in the launcher classpath. MicroEmulator classes are
 * loaded by {@code EmulatorClassLoader} which delegates non-MIDlet classes
 * to its parent (system classloader), so the injected bytecode can resolve
 * this class without any cross-loader tricks.
 */
public final class PaintThrottleConfig {

    /** Minimum milliseconds between accepted paints. 0 = unlimited. */
    public static volatile long intervalMs = 33L; // 30 FPS default

    private PaintThrottleConfig() {
    }

    public static void setFps(int fps) {
        if (fps <= 0) {
            intervalMs = 0L; // unlimited
        } else {
            intervalMs = 1000L / fps;
        }
    }

    public static int getFps() {
        return intervalMs <= 0 ? 0 : (int) (1000L / intervalMs);
    }
}
