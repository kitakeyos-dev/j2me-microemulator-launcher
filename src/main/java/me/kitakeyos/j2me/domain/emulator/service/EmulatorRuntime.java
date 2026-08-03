package me.kitakeyos.j2me.domain.emulator.service;

import me.kitakeyos.j2me.domain.emulator.model.EmulatorInstance;
import me.kitakeyos.j2me.domain.emulator.repository.InstanceLookup;
import me.kitakeyos.j2me.domain.emulator.repository.InstanceRegistry;
import me.kitakeyos.j2me.domain.emulator.repository.InstanceTabController;

import java.util.Collections;
import java.util.List;

/**
 * The one static seam in the emulator domain.
 * <p>
 * Most collaborators receive their dependencies through constructors. Two
 * groups genuinely cannot:
 * <ul>
 * <li>{@code SystemCallHandler} and {@code XThread}, which are reached from
 * ASM-instrumented MIDlet bytecode through static call sites — there is no
 * object for the composition root to inject into;</li>
 * <li>{@code EmulatorInstance#shutdown()}, which must stay callable on a bare
 * model object held by MicroEmulator internals.</li>
 * </ul>
 * Those call sites used to reach {@code MainApplication.INSTANCE}, pointing the
 * dependency arrow outward at a Swing {@code JFrame}. They now resolve domain
 * ports instead, so the arrow points inward and tests can substitute fakes via
 * {@link #bind}.
 * <p>
 * Every accessor returns a no-op implementation until {@link #bind} is called,
 * so nothing here can NPE during early startup or in a headless test.
 */
public final class EmulatorRuntime {

    private static volatile InstanceRegistry instances = NoOpRegistry.INSTANCE;
    private static volatile InstanceTabController tabs = instance -> {
    };
    private static volatile InstanceLifecycle lifecycle = NoOpLifecycle.INSTANCE;

    private EmulatorRuntime() {
    }

    /**
     * Install the live implementations. Called once by the composition root,
     * and again by tests that need to observe or stub the runtime.
     *
     * @param registry   the live instance list
     * @param tabs       presentation hook for detaching instance tabs
     * @param lifecycle  the configured shutdown pipeline
     */
    public static void bind(InstanceRegistry registry, InstanceTabController tabs, InstanceLifecycle lifecycle) {
        EmulatorRuntime.instances = registry != null ? registry : NoOpRegistry.INSTANCE;
        EmulatorRuntime.tabs = tabs != null ? tabs : instance -> {
        };
        EmulatorRuntime.lifecycle = lifecycle != null ? lifecycle : NoOpLifecycle.INSTANCE;
    }

    /**
     * Restore the unbound state. Intended for test teardown.
     */
    public static void reset() {
        bind(null, null, null);
    }

    public static InstanceRegistry instances() {
        return instances;
    }

    public static InstanceLookup lookup() {
        return instances;
    }

    public static InstanceTabController tabs() {
        return tabs;
    }

    public static InstanceLifecycle lifecycle() {
        return lifecycle;
    }

    private enum NoOpRegistry implements InstanceRegistry {
        INSTANCE;

        @Override
        public EmulatorInstance findInstance(int instanceId) {
            return null;
        }

        @Override
        public List<EmulatorInstance> getRunningInstances() {
            return Collections.emptyList();
        }

        @Override
        public void addInstance(EmulatorInstance instance) {
        }

        @Override
        public void removeInstance(EmulatorInstance instance) {
        }
    }

    private enum NoOpLifecycle implements InstanceLifecycle {
        INSTANCE;

        @Override
        public void shutdown(EmulatorInstance instance) {
        }

        @Override
        public void forceShutdown(EmulatorInstance instance) {
        }
    }
}
