package me.kitakeyos.j2me.core.thread;

import me.kitakeyos.j2me.MainApplication;
import me.kitakeyos.j2me.core.classloader.InstanceContext;
import me.kitakeyos.j2me.model.EmulatorInstance;
import me.kitakeyos.j2me.service.EmulatorInstanceManager;

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
            EmulatorInstanceManager manager =MainApplication.INSTANCE.emulatorInstanceManager;
            EmulatorInstance instance = manager.findInstance(instanceId);
            if (instance != null) {
                instance.addThread(this);
            } else {
                System.out.println("Emulator Instance Not Found");
            }
        }
    }
}
