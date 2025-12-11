package me.kitakeyos.j2me.infrastructure.thread;

import me.kitakeyos.j2me.application.MainApplication;
import me.kitakeyos.j2me.domain.emulator.model.EmulatorInstance;
import me.kitakeyos.j2me.domain.emulator.service.InstanceManager;

import java.util.logging.Logger;

@SuppressWarnings("unused")
public class XThread extends Thread {

    private static final Logger logger = Logger.getLogger(XThread.class.getName());
    private final int instanceId;

    public XThread(String name, int instanceId) {
        super(name);
        this.instanceId = instanceId;
        System.out.println("Creating thread " +  name + " with id " + instanceId);
        addToEmulatorInstance();
    }

    public XThread(Runnable target, String name, int instanceId) {
        super(target, name);
        this.instanceId = instanceId;
        System.out.println("Creating thread " +  name + " with id " + instanceId);
        addToEmulatorInstance();
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
}
