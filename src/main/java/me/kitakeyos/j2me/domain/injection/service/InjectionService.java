package me.kitakeyos.j2me.domain.injection.service;

import me.kitakeyos.j2me.domain.injection.model.InjectionEntry;
import me.kitakeyos.j2me.domain.injection.model.InjectionLogger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service for loading JAR files and discovering/executing InjectionEntry implementations.
 */
public class InjectionService {

    private static final Logger logger = Logger.getLogger(InjectionService.class.getName());

    private File loadedJarFile;
    private File clonedJarFile;
    private URLClassLoader jarClassLoader;
    private final List<Class<? extends InjectionEntry>> entryClasses = new ArrayList<>();

    /**
     * Load a JAR file and discover all classes implementing InjectionEntry.
     *
     * @param jarFile The JAR file to load
     * @return List of discovered entry class names
     * @throws IOException If the JAR cannot be read
     */
    public List<String> loadJar(File jarFile) throws IOException {
        // Close previous classloader if any
        closeCurrentJar();

        this.loadedJarFile = jarFile;
        entryClasses.clear();

        // Clone JAR to temp file to avoid file locking on Windows
        clonedJarFile = File.createTempFile("injection_", ".jar");
        clonedJarFile.deleteOnExit();
        Files.copy(jarFile.toPath(), clonedJarFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

        // Create classloader with the cloned JAR
        jarClassLoader = new URLClassLoader(
                new URL[]{clonedJarFile.toURI().toURL()},
                getClass().getClassLoader()
        );

        // Scan for InjectionEntry implementations
        List<String> classNames = scanJarForClasses(jarFile);
        List<String> foundEntries = new ArrayList<>();

        for (String className : classNames) {
            try {
                Class<?> cls = jarClassLoader.loadClass(className);
                if (InjectionEntry.class.isAssignableFrom(cls) && !cls.isInterface()) {
                    @SuppressWarnings("unchecked")
                    Class<? extends InjectionEntry> entryClass = (Class<? extends InjectionEntry>) cls;
                    entryClasses.add(entryClass);
                    foundEntries.add(className);
                    logger.info("Found InjectionEntry: " + className);
                }
            } catch (ClassNotFoundException | NoClassDefFoundError e) {
                logger.fine("Skipping class: " + className + " (" + e.getMessage() + ")");
            }
        }

        logger.info("Loaded " + foundEntries.size() + " entry class(es) from " + jarFile.getName());
        return foundEntries;
    }

    /**
     * Execute a specific entry class against an emulator instance's ClassLoader.
     * Creates a temporary classloader combining the injection JAR with the MIDlet's
     * classloader, so injected code can directly reference MIDlet classes.
     *
     * @param className      The fully qualified class name to execute
     * @param appClassLoader The MIDlet's ClassLoader from the target instance
     * @param logger         Logger for the injection script
     * @throws Exception If execution fails
     */
    public void execute(String className, ClassLoader appClassLoader, InjectionLogger logger) throws Exception {
        if (clonedJarFile == null) {
            throw new IllegalStateException("No JAR loaded");
        }

        ClassLoader appOwnClassLoader = getClass().getClassLoader();
        try (URLClassLoader execClassLoader = new URLClassLoader(
                new URL[]{clonedJarFile.toURI().toURL()}, appOwnClassLoader) {
            @Override
            protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                Class<?> c = findLoadedClass(name);
                if (c != null) return c;

                // Java/system classes -> standard delegation
                if (name.startsWith("java.") || name.startsWith("javax.") || name.startsWith("sun.") || name.startsWith("jdk.")) {
                    return super.loadClass(name, resolve);
                }

                // 1. Injection JAR (child-first: MyScript etc.)
                try {
                    c = findClass(name);
                    if (resolve) resolveClass(c);
                    return c;
                } catch (ClassNotFoundException ignored) {}

                // 2. App classloader (InjectionEntry, InjectionLogger, etc.)
                try {
                    return appOwnClassLoader.loadClass(name);
                } catch (ClassNotFoundException ignored) {}

                // 3. MIDlet classloader (game classes: Class_CZ etc.)
                return appClassLoader.loadClass(name);
            }
        }) {
            Thread.currentThread().setContextClassLoader(execClassLoader);

            Class<?> cls = execClassLoader.loadClass(className);
            if (!InjectionEntry.class.isAssignableFrom(cls)) {
                throw new IllegalArgumentException(className + " does not implement InjectionEntry");
            }

            InjectionEntry entry = (InjectionEntry) cls.getDeclaredConstructor().newInstance();
            entry.execute(appClassLoader, logger);
        }
    }

    /**
     * Get list of loaded entry class names.
     */
    public List<String> getEntryClassNames() {
        List<String> names = new ArrayList<>();
        for (Class<? extends InjectionEntry> cls : entryClasses) {
            names.add(cls.getName());
        }
        return names;
    }

    /**
     * Get the currently loaded JAR file.
     */
    public File getLoadedJarFile() {
        return loadedJarFile;
    }

    /**
     * Check if a JAR is currently loaded.
     */
    public boolean isJarLoaded() {
        return jarClassLoader != null && !entryClasses.isEmpty();
    }

    /**
     * Reload the current JAR (for hot-reload during development).
     */
    public List<String> reloadJar() throws IOException {
        if (loadedJarFile == null) {
            throw new IllegalStateException("No JAR to reload");
        }
        return loadJar(loadedJarFile);
    }

    /**
     * Close the current JAR classloader and release resources.
     */
    public void closeCurrentJar() {
        if (jarClassLoader != null) {
            try {
                jarClassLoader.close();
            } catch (IOException e) {
                logger.log(Level.WARNING, "Error closing JAR classloader", e);
            }
            jarClassLoader = null;
        }
        // Delete cloned temp file
        if (clonedJarFile != null) {
            clonedJarFile.delete();
            clonedJarFile = null;
        }
        entryClasses.clear();
        loadedJarFile = null;
    }

    /**
     * Scan a JAR file for all .class entries and return their class names.
     */
    private List<String> scanJarForClasses(File jarFile) throws IOException {
        List<String> classNames = new ArrayList<>();
        try (JarFile jar = new JarFile(jarFile)) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (name.endsWith(".class") && !name.contains("$")) {
                    // Convert path to class name
                    String className = name.replace('/', '.').replace(".class", "");
                    classNames.add(className);
                }
            }
        }
        return classNames;
    }
}
