package me.kitakeyos.j2me.domain.emulator.service;

import me.kitakeyos.j2me.domain.emulator.model.EmulatorInstance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/**
 * Runs the configured {@link InstanceCleanupStep} pipeline against an instance.
 * <p>
 * Replaces the previous eight-deep nested {@code try-finally} pyramid. The
 * guarantee is unchanged — every step runs even when an earlier one throws —
 * but the sequence is now data, so adding or reordering teardown work happens
 * at the composition root instead of inside this class.
 */
public class InstanceLifecycleManager implements InstanceLifecycle {

    private static final Logger logger = Logger.getLogger(InstanceLifecycleManager.class.getName());

    private final List<InstanceCleanupStep> steps;

    public InstanceLifecycleManager(List<InstanceCleanupStep> steps) {
        this.steps = Collections.unmodifiableList(new ArrayList<>(steps));
    }

    @Override
    public void shutdown(EmulatorInstance instance) {
        if (instance.getState() == EmulatorInstance.InstanceState.STOPPED) {
            logger.fine("Instance #" + instance.getInstanceId() + " already stopped, skipping shutdown");
            return;
        }
        // Flip the state first so a concurrent shutdown returns above.
        instance.setState(EmulatorInstance.InstanceState.STOPPED);

        logger.info("Shutting down instance #" + instance.getInstanceId() + " and releasing resources...");
        runAll(instance);
        logger.info("Instance #" + instance.getInstanceId() + " shutdown completed");

        // The instrumented class graph is large and short-lived; nudging the
        // collector here keeps multi-instance sessions from creeping upward.
        System.gc();
    }

    @Override
    public void forceShutdown(EmulatorInstance instance) {
        logger.info("Force shutdown instance #" + instance.getInstanceId());
        instance.setState(EmulatorInstance.InstanceState.STOPPED);
        runAll(instance);
    }

    /**
     * Execute every step in order, isolating failures so one broken step
     * cannot strand the resources owned by the steps behind it.
     */
    private void runAll(EmulatorInstance instance) {
        for (InstanceCleanupStep step : steps) {
            try {
                step.clean(instance);
            } catch (Exception | LinkageError e) {
                logger.warning("Cleanup step '" + step.name() + "' failed for instance #"
                        + instance.getInstanceId() + ": " + e);
            }
        }
    }

    /**
     * @return the configured steps, in execution order
     */
    public List<InstanceCleanupStep> getSteps() {
        return steps;
    }
}
