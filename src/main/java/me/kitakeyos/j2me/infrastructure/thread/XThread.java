package me.kitakeyos.j2me.infrastructure.thread;

import me.kitakeyos.j2me.app.MainApplication;
import me.kitakeyos.j2me.infrastructure.classloader.InstanceContext;
import me.kitakeyos.j2me.domain.emulator.model.EmulatorInstance;
import me.kitakeyos.j2me.domain.emulator.service.InstanceManager;

public class XThread extends Thread {

    public XThread(String name) {
        super(name);
        addToEmulatorInstance();
    }

    public XThread(Runnable target, String name) {
        super(target, name);
        addToEmulatorInstance();
    }

    private void addToEmulatorInstance() {
        int instanceId = InstanceContext.getInstanceId();
        if (instanceId != -1) {
            InstanceManager manager =MainApplication.INSTANCE.emulatorInstanceManager;
            EmulatorInstance instance = manager.findInstance(instanceId);
            if (instance != null) {
                instance.addThread(this);
            } else {
                System.out.println("Emulator Instance Not Found");
            }
        }
    }
}
