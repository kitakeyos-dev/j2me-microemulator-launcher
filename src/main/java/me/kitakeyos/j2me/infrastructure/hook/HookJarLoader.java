package me.kitakeyos.j2me.infrastructure.hook;

import me.kitakeyos.j2me.domain.hook.annotation.Hook;
import me.kitakeyos.j2me.domain.hook.repository.HookProvider;
import me.kitakeyos.j2me.domain.hook.repository.HookRegistrar;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Logger;

/**
 * Loads {@link Hook} classes out of a JAR and registers them for an instance
 * that has not started yet.
 *
 * <p>Timing is the whole point: the rewrite happens in
 * {@code EmulatorClassLoader.findClass}, so hooks must exist <em>before</em>
 * the MIDlet class is loaded. That is also why the JAR is loaded against the
 * launcher's own classloader and not the MIDlet's — the MIDlet's does not
 * exist yet. A hook class may therefore reference the hook API freely, but
 * must reach game classes through {@code HookInvocation} and reflection.
 *
 * <p>The JAR is copied to a temp file before loading so the original stays
 * unlocked on Windows and can be rebuilt while the launcher runs.
 */
public final class HookJarLoader implements HookProvider {

    private static final Logger logger = Logger.getLogger(HookJarLoader.class.getName());

    private final AnnotationHookScanner scanner;

    private URLClassLoader currentLoader;
    private File currentCopy;

    public HookJarLoader(HookRegistrar registrar) {
        this.scanner = new AnnotationHookScanner(registrar);
    }

    /**
     * Register every {@code @Hook} class found in {@code jarFile}.
     *
     * @param instanceId instance the hooks apply to, or
     *                   {@link me.kitakeyos.j2me.domain.hook.repository.HookLookup#ALL_INSTANCES}
     * @return names of the hook classes that were registered
     */
    @Override
    public List<String> loadHooks(int instanceId, File jarFile) throws IOException, ReflectiveOperationException {
        close();

        currentCopy = File.createTempFile("hooks_", ".jar");
        currentCopy.deleteOnExit();
        Files.copy(jarFile.toPath(), currentCopy.toPath(), StandardCopyOption.REPLACE_EXISTING);

        currentLoader = new URLClassLoader(new URL[] { currentCopy.toURI().toURL() },
                getClass().getClassLoader());

        List<String> registered = new ArrayList<>();
        for (String className : listClassNames(jarFile)) {
            Class<?> candidate;
            try {
                candidate = currentLoader.loadClass(className);
            } catch (ClassNotFoundException | NoClassDefFoundError e) {
                logger.fine("Skipping " + className + " (" + e.getMessage() + ")");
                continue;
            }
            if (AnnotationHookScanner.isHookClass(candidate)) {
                int count = scanner.register(instanceId, candidate);
                registered.add(className + " (" + count + " hook(s))");
            }
        }

        logger.info("Loaded " + registered.size() + " hook class(es) from " + jarFile.getName());
        return registered;
    }

    /**
     * Release the JAR copy. Already-registered interceptors keep working —
     * they hold their own references to the loaded classes.
     */
    public void close() {
        if (currentLoader != null) {
            try {
                currentLoader.close();
            } catch (IOException e) {
                logger.warning("Error closing hook classloader: " + e.getMessage());
            }
            currentLoader = null;
        }
        if (currentCopy != null) {
            currentCopy.delete();
            currentCopy = null;
        }
    }

    private static List<String> listClassNames(File jarFile) throws IOException {
        List<String> classNames = new ArrayList<>();
        try (JarFile jar = new JarFile(jarFile)) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                String name = entries.nextElement().getName();
                if (name.endsWith(".class")) {
                    classNames.add(name.substring(0, name.length() - ".class".length()).replace('/', '.'));
                }
            }
        }
        return classNames;
    }
}
