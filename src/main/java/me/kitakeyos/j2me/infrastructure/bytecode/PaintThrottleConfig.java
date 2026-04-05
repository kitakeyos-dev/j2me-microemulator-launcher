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

    /**
     * When true, the injected guard drops every paint immediately. Set by
     * MainApplication's WindowStateListener when the launcher JFrame is
     * minimized to the taskbar — no point rendering 100 MIDlet canvases
     * the user cannot see.
     */
    public static volatile boolean windowMinimized = false;

    /**
     * Wall-clock timestamp of the last user input event (mouse / key) seen
     * by MainApplication's AWT event listener. Bytecode compares against
     * {@link #idleTimeoutMs} and drops paints when the launcher looks idle.
     */
    public static volatile long lastActivityMs = System.currentTimeMillis();

    /**
     * Idle timeout. When {@code now - lastActivityMs &gt; idleTimeoutMs},
     * the guard drops paints. {@code 0} disables the idle check.
     */
    public static volatile long idleTimeoutMs = 0L;

    private PaintThrottleConfig() {
    }

    public static void setIdleTimeoutSeconds(int seconds) {
        idleTimeoutMs = seconds <= 0 ? 0L : seconds * 1000L;
    }

    public static int getIdleTimeoutSeconds() {
        return (int) (idleTimeoutMs / 1000L);
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
