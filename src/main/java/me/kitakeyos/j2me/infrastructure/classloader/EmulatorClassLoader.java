package me.kitakeyos.j2me.infrastructure.classloader;

import me.kitakeyos.j2me.infrastructure.bytecode.ByteCodeHelper;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Custom class loader for MIDlet execution with bytecode instrumentation
 * support.
 * <p>
 * Features:
 * - Reverse delegation (loads MIDlet classes before system classes)
 * - Bytecode instrumentation via ASM
 * - Shared instrumented bytecode cache across instances
 *
 * @author vlads
 */
public class EmulatorClassLoader extends URLClassLoader {

    private static final Logger logger = Logger.getLogger(EmulatorClassLoader.class.getName());

    // Shared cache for unmodified bytecode across instances
    private static final Map<String, byte[]> sharedBytecodeCache = new ConcurrentHashMap<>();

    private final int instanceId;

    public EmulatorClassLoader(int instanceId, URL[] urls, ClassLoader parent) {
        super(urls, parent);
        this.instanceId = instanceId;
        logger.info("Created EmulatorClassLoader for instance #" + instanceId +
                " with " + urls.length + " URL(s)");
    }

    /**
     * Get instance ID for this class loader
     */
    public int getInstanceId() {
        return instanceId;
    }

    /**
     * Appends the Class Location URL to the list of URLs to search for classes and
     * resources.
     */
    public void addClassURL(String className) throws MalformedURLException {
        String resourcePath = ByteCodeHelper.getClassResourcePath(className);
        URL url = findClassLocationURL(resourcePath);

        if (url == null) {
            throw new MalformedURLException("Unable to find class " + className + " URL");
        }

        addURL(url);
    }

    /**
     * Find the base URL for a class resource
     */
    private URL findClassLocationURL(String resourcePath) throws MalformedURLException {
        // Try parent first
        URL url = getParent() != null ? getParent().getResource(resourcePath) : null;

        // Then try this classloader
        if (url == null) {
            url = findResource(resourcePath);
        }

        if (url == null) {
            return null;
        }

        // Extract base URL (remove resource path)
        String externalForm = url.toExternalForm();
        String baseUrl = externalForm.substring(0, externalForm.length() - resourcePath.length());
        return new URL(baseUrl);
    }

    /**
     * Loads the class with the specified binary name.
     * <p>
     * Search order (reverse delegation):
     * 1. Check if already loaded
     * 2. Try to find in this classloader's URLs (MIDlet JAR)
     * 3. Delegate to parent (System ClassLoader)
     */
    @Override
    protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        // First, check if the class has already been loaded
        Class<?> loadedClass = findLoadedClass(name);

        if (loadedClass == null) {
            try {
                // Try to load from MIDlet JAR first (reverse delegation)
                loadedClass = findClass(name);
            } catch (ClassNotFoundException e) {
                // Not found in MIDlet JAR, delegate to parent
                loadedClass = super.loadClass(name, false);
            }
        }

        if (loadedClass == null) {
            throw new ClassNotFoundException(name);
        }

        if (resolve) {
            resolveClass(loadedClass);
        }

        return loadedClass;
    }

    /**
     * Find resource with reverse delegation
     */
    @Override
    public URL getResource(String name) {
        return findResource(name);
    }

    /**
     * Get resource as input stream
     */
    @Override
    public InputStream getResourceAsStream(String name) {
        URL url = getResource(name);
        if (url == null) {
            return null;
        }

        try {
            return url.openStream();
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to open resource stream: " + name, e);
            return null;
        }
    }

    /**
     * Find and load class from MIDlet JAR
     */
    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        // Try to load from shared RAM cache first
        byte[] cachedBytes = sharedBytecodeCache.get(name);
        if (cachedBytes != null) {
            return defineClass(name, cachedBytes, 0, cachedBytes.length);
        }

        String resourcePath = ByteCodeHelper.getClassResourcePath(name);
        InputStream is = getResourceAsStream(resourcePath);

        if (is == null) {
            throw new ClassNotFoundException(name);
        }

        try {
            ClassPreprocessor.InstrumentationResult result = ClassPreprocessor.instrumentAndModifyBytecode(is,
                    instanceId);
            if (result == null) {
                throw new ClassNotFoundException(name);
            }

            // If not modified, save to shared RAM cache
            if (!result.isModified) {
                sharedBytecodeCache.put(name, result.bytecode);
            }

            return defineClass(name, result.bytecode, 0, result.bytecode.length);
        } finally {
            closeQuietly(is);
        }
    }

    /**
     * Close stream quietly without throwing exceptions
     */
    private void closeQuietly(InputStream is) {
        if (is != null) {
            try {
                is.close();
            } catch (IOException e) {
                // Ignore
            }
        }
    }
}