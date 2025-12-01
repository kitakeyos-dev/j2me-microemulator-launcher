/**
 * MicroEmulator
 * Copyright (C) 2006-2007 Bartek Teodorczyk <barteo@barteo.net>
 * Copyright (C) 2006-2007 Vlad Skarzhevskyy
 * <p>
 * It is licensed under the following two licenses as alternatives:
 * 1. GNU Lesser General Public License (the "LGPL") version 2.1 or any newer version
 * 2. Apache License (the "AL") Version 2.0
 * <p>
 * You may not use this file except in compliance with at least one of
 * the above two licenses.
 * <p>
 * You may obtain a copy of the LGPL at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.txt
 * <p>
 * You may obtain a copy of the AL at
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the LGPL or the AL for the specific language governing permissions and
 * limitations.
 *
 * @version $Id: MIDletClassLoader.java 2052 2009-05-13 14:31:06Z barteo $
 */
package me.kitakeyos.j2me.infrastructure.classloader;

import me.kitakeyos.j2me.infrastructure.bytecode.ByteCodeHelper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Custom class loader for MIDlet execution with bytecode instrumentation support.
 *
 * Features:
 * - Reverse delegation (loads MIDlet classes before system classes)
 * - Bytecode instrumentation via ASM
 * - Shared instrumented bytecode cache across instances
 * - Per-instance statistics tracking
 * - Thread-safe class loading
 *
 * @author vlads
 */
public class EmulatorClassLoader extends URLClassLoader {

    private static final Logger logger = Logger.getLogger(EmulatorClassLoader.class.getName());

    // Configuration
    private static final boolean DELEGATE_TO_PARENT = false;
    private static final boolean SEARCH_IN_PARENT = false;

    // Read buffer size for non-instrumented classes
    private static final int READ_CHUNK_SIZE = 2048;
    private static final int MAX_CLASS_SIZE = 16 * 1024; // 16KB

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
     * Appends the Class Location URL to the list of URLs to search for classes and resources.
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
     *
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
        // Try this classloader first
        URL url = findResource(name);

        // Delegate to parent only if configured
        if (url == null && DELEGATE_TO_PARENT && getParent() != null) {
            url = getParent().getResource(name);
        }

        return url;
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
        // Set the instance ID in ThreadLocal for dynamic instrumentation
        InstanceContext.setInstanceId(instanceId);

        try {
            String resourcePath = ByteCodeHelper.getClassResourcePath(name);
            InputStream is = getResourceAsStream(resourcePath);

            // Relax ClassLoader behavior - search in parent if needed
            if (is == null && SEARCH_IN_PARENT) {
                try {
                    addClassURL(name);
                    is = getResourceAsStream(resourcePath);
                } catch (MalformedURLException e) {
                    // Ignore and continue
                }
            }

            if (is == null) {
                throw new ClassNotFoundException(name);
            }

            byte[] bytecode;
            try {
                bytecode = ClassPreprocessor.instrumentAndModifyBytecode(is, instanceId);
            } finally {
                closeQuietly(is);
            }
            return defineClass(name, bytecode, 0, bytecode.length);

        } finally {
            // Keep instance ID in ThreadLocal for the lifetime of the emulator instance
            // Don't clear it here as the class will be used in the same thread
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