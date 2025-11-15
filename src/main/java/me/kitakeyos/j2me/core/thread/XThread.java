package me.kitakeyos.j2me.core.thread;

import me.kitakeyos.j2me.core.classloader.InstanceContext;

public class XThread extends Thread {

    public XThread(String name) {
        super(name);
        int instanceId = InstanceContext.getInstanceId();
        System.out.println("New XThread: " + instanceId);
    }

    public XThread(Runnable target, String name) {
        super(target, name);
        int instanceId = InstanceContext.getInstanceId();
        System.out.println("New XThread: " + instanceId);
    }
}
