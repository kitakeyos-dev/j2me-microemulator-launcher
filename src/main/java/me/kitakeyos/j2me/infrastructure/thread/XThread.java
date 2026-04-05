package me.kitakeyos.j2me.infrastructure.thread;

import me.kitakeyos.j2me.application.MainApplication;
import me.kitakeyos.j2me.domain.emulator.model.EmulatorInstance;
import me.kitakeyos.j2me.domain.emulator.service.InstanceManager;
import me.kitakeyos.j2me.domain.speed.service.SpeedService;

import java.util.logging.Logger;

@SuppressWarnings("unused")
public class XThread extends Thread {

    private static final Logger logger = Logger.getLogger(XThread.class.getName());

    /**
     * Sentinel char embedded in Thread.getName() to carry the current speed
     * multiplier across the classloader boundary to the injected SpeedHelper.
     * SOH (U+0001) is vanishingly unlikely to appear in any real thread name.
     */
    public static final char SPEED_MARKER = '\u0001';

    private final int instanceId;
    // Base (user-visible) thread name, without the encoded speed suffix.
    private volatile String baseName;
    private volatile double speedMultiplier = 1.0;

    public XThread(String name, int instanceId) {
        super(name);
        this.instanceId = instanceId;
        this.baseName = name;
        initSpeedFromService();
        addToEmulatorInstance();
    }

    public XThread(Runnable target, String name, int instanceId) {
        super(target, name);
        this.instanceId = instanceId;
        this.baseName = name;
        initSpeedFromService();
        addToEmulatorInstance();
    }

    private void initSpeedFromService() {
        this.speedMultiplier = SpeedService.getInstance().getSpeedMultiplier(instanceId);
        applyEncodedName();
    }

    private void addToEmulatorInstance() {
        InstanceManager manager = MainApplication.INSTANCE.emulatorInstanceManager;
        EmulatorInstance instance = manager.findInstance(instanceId);
        if (instance != null) {
            instance.addThread(this);
        } else {
            logger.info("Emulator Instance Not Found");
        }
    }

    public int getInstanceId() {
        return instanceId;
    }

    public double getSpeedMultiplier() {
        return speedMultiplier;
    }

    /**
     * Called by SpeedService when the user changes instance speed. Re-encodes
     * the thread name so the next SpeedHelper.sleep() call picks up the new
     * value without any lock or map lookup.
     */
    public void setSpeedMultiplier(double multiplier) {
        this.speedMultiplier = multiplier;
        applyEncodedName();
    }

    /**
     * When speed is 1.0 we leave the name untouched (no marker, no parsing on
     * the sleep hot path). Only instances with a real override pay the
     * encoding cost.
     */
    private void applyEncodedName() {
        double m = this.speedMultiplier;
        String name = this.baseName;
        if (m == 1.0) {
            super.setName(name);
        } else {
            int milli = (int) (m * 1000.0 + 0.5);
            if (milli <= 0) {
                milli = 1;
            }
            super.setName(name + SPEED_MARKER + milli);
        }
    }
}
