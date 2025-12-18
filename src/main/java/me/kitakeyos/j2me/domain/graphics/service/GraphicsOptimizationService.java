package me.kitakeyos.j2me.domain.graphics.service;

import me.kitakeyos.j2me.domain.emulator.model.EmulatorInstance;
import me.kitakeyos.j2me.util.reflection.ReflectionHelper;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.logging.Logger;

/**
 * Service to manage graphics optimization (Stop Painting feature).
 * Uses Dynamic Proxy to intercept paint calls in org.microemu.DisplayAccess.
 */
public class GraphicsOptimizationService {

    private static final Logger logger = Logger.getLogger(GraphicsOptimizationService.class.getName());
    private static GraphicsOptimizationService instance;

    public static synchronized GraphicsOptimizationService getInstance() {
        if (instance == null) {
            instance = new GraphicsOptimizationService();
        }
        return instance;
    }

    /**
     * Toggle graphics rendering for the given instance.
     *
     * @param emulatorInstance The emulator instance
     * @param enabled          True to enable painting, false to stop it
     */
    public void setGraphicsEnabled(EmulatorInstance emulatorInstance, boolean enabled) {
        emulatorInstance.setGraphicsEnabled(enabled);

        try {
            ClassLoader cl = emulatorInstance.getEmulatorClassLoader();

            // Get MIDletBridge class
            Class<?> bridgeClass = ReflectionHelper.loadClass(cl, "org.microemu.MIDletBridge");

            // MIDletAccess access = MIDletBridge.getMIDletAccess(midlet);
            Object midletAccess = ReflectionHelper.invokeStaticMethod(bridgeClass, "getMIDletAccess", new Class<?>[0]);

            if (midletAccess == null) {
                logger.warning("Could not get MIDletAccess for instance #" + emulatorInstance.getInstanceId());
                return;
            }

            // Get current DisplayAccess
            // DisplayAccess currentDisplayAccess = access.getDisplayAccess();
            Object currentDisplayAccess = ReflectionHelper.invokeMethod(midletAccess, "getDisplayAccess");

            // Check if we already wrapped it with our proxy
            if (Proxy.isProxyClass(currentDisplayAccess.getClass())) {
                InvocationHandler handler = Proxy.getInvocationHandler(currentDisplayAccess);
                if (handler instanceof GraphicsToggleHandler) {
                    ((GraphicsToggleHandler) handler).setEnabled(enabled);
                    logger.info("Updated existing Graphics Proxy for instance #" + emulatorInstance.getInstanceId()
                            + " to " + enabled);

                    // If enabling, force a repaint to restore the screen
                    if (enabled) {
                        try {
                            ReflectionHelper.invokeMethod(currentDisplayAccess, "repaint");
                        } catch (Exception ignored) {
                        }
                    }
                    return;
                }
            }

            // If not wrapped yet, wrap it!
            Class<?> displayAccessInterface = ReflectionHelper.loadClass(cl, "org.microemu.DisplayAccess");
            GraphicsToggleHandler handler = new GraphicsToggleHandler(currentDisplayAccess, enabled);

            Object proxy = Proxy.newProxyInstance(cl, new Class<?>[] { displayAccessInterface }, handler);

            // Set new DisplayAccess
            // access.setDisplayAccess(proxy);
            ReflectionHelper.invokeMethod(midletAccess, "setDisplayAccess",
                    new Class<?>[] { displayAccessInterface }, proxy);

            logger.info("Installed new Graphics Proxy for instance #" + emulatorInstance.getInstanceId() + ". Enabled: "
                    + enabled);

        } catch (Exception e) {
            logger.severe("Failed to toggle graphics for instance #" + emulatorInstance.getInstanceId() + ": "
                    + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * InvocationHandler to intercept paint calls.
     */
    private static class GraphicsToggleHandler implements InvocationHandler {
        private final Object original;
        private boolean enabled;

        public GraphicsToggleHandler(Object original, boolean enabled) {
            this.original = original;
            this.enabled = enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String name = method.getName();

            // Intercept painting methods when disabled
            if (!enabled) {
                if (name.equals("paint") || name.equals("repaint") || name.equals("serviceRepaints")) {
                    // Do nothing -> Stop painting
                    return null;
                }
            }

            // Delegate everything else to original
            return method.invoke(original, args);
        }
    }
}
