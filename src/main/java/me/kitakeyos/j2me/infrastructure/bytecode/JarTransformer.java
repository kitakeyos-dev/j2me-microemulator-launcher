package me.kitakeyos.j2me.infrastructure.bytecode;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Transforms J2ME JAR files by applying bytecode modifications.
 * Creates a modified copy of the JAR with Thread.sleep() calls intercepted.
 * 
 * The transformation is done during application installation, not at runtime.
 * The transformed JAR contains SpeedHelper class which gets instanceId from
 * the current Thread at runtime.
 */
public class JarTransformer {

    private static final Logger logger = Logger.getLogger(JarTransformer.class.getName());

    /**
     * Transform a J2ME JAR file for speed control.
     * This should be called during application installation.
     *
     * @param sourceJar Original JAR file path
     * @return Path to the transformed JAR file (replaces original with _transformed
     *         suffix)
     */
    public static Path transformJar(Path sourceJar) throws IOException {
        // Create transformed JAR path
        String fileName = sourceJar.getFileName().toString();
        String baseName = fileName.substring(0, fileName.lastIndexOf('.'));
        Path transformedJar = sourceJar.getParent().resolve(baseName + "_transformed.jar");

        logger.info("Transforming JAR: " + sourceJar + " -> " + transformedJar);

        int transformedClasses = 0;
        int totalClasses = 0;

        try (JarFile jar = new JarFile(sourceJar.toFile());
                JarOutputStream jos = new JarOutputStream(new FileOutputStream(transformedJar.toFile()))) {

            Enumeration<JarEntry> entries = jar.entries();

            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();

                // Create new entry (reset compression to avoid issues)
                JarEntry newEntry = new JarEntry(name);
                jos.putNextEntry(newEntry);

                try (InputStream is = jar.getInputStream(entry)) {
                    if (name.endsWith(".class")) {
                        totalClasses++;
                        // Transform class bytecode
                        byte[] transformed = transformClass(is);
                        if (transformed != null) {
                            jos.write(transformed);
                            transformedClasses++;
                        } else {
                            // Copy original if transformation failed
                            copyStream(jar.getInputStream(entry), jos);
                        }
                    } else {
                        // Copy non-class files as-is
                        copyStream(is, jos);
                    }
                }

                jos.closeEntry();
            }

            // Inject SpeedHelper class into the JAR
            injectSpeedHelperClass(jos);
        }

        logger.info(String.format("JAR transformation complete: %d/%d classes transformed",
                transformedClasses, totalClasses));

        return transformedJar;
    }

    /**
     * Legacy method for backwards compatibility.
     * 
     * @deprecated Use transformJar(Path) instead
     */
    @Deprecated
    public static Path transformJar(Path sourceJar, int instanceId) throws IOException {
        return transformJar(sourceJar);
    }

    /**
     * Inject SpeedHelper class into the JAR.
     * This class handles speed adjustment and can be called by transformed J2ME
     * code.
     */
    private static void injectSpeedHelperClass(JarOutputStream jos) throws IOException {
        // Create directory entry
        JarEntry dirEntry = new JarEntry("j2me_speed_helper/");
        jos.putNextEntry(dirEntry);
        jos.closeEntry();

        // Create class entry
        JarEntry classEntry = new JarEntry(SpeedHelperGenerator.CLASS_FILE_NAME);
        jos.putNextEntry(classEntry);
        jos.write(SpeedHelperGenerator.generateClass());
        jos.closeEntry();

        logger.info("Injected SpeedHelper class into JAR");
    }

    /**
     * Transform a single class bytecode.
     */
    private static byte[] transformClass(InputStream classInputStream) {
        try {
            byte[] originalBytes = readAllBytes(classInputStream);

            ClassReader cr = new ClassReader(originalBytes);
            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS); // Auto-compute max stack/locals
            ModificationTracker tracker = new ModificationTracker();
            ClassVisitor cv = new J2meSpeedClassVisitor(cw, tracker);
            cr.accept(cv, 0);

            if (tracker.isModified()) {
                logger.fine("Transformed class with Thread.sleep interception");
                return cw.toByteArray();
            } else {
                return originalBytes; // Return original if no modifications
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to transform class", e);
            return null;
        }
    }

    private static byte[] readAllBytes(InputStream is) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[4096];
        int n;
        while ((n = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, n);
        }
        return buffer.toByteArray();
    }

    private static void copyStream(InputStream is, OutputStream os) throws IOException {
        byte[] buffer = new byte[4096];
        int n;
        while ((n = is.read(buffer)) != -1) {
            os.write(buffer, 0, n);
        }
    }

    /**
     * Delete transformed JAR file (cleanup).
     */
    public static void cleanupTransformedJar(Path transformedJar) {
        try {
            if (transformedJar != null && Files.exists(transformedJar)) {
                Files.delete(transformedJar);
                logger.info("Cleaned up transformed JAR: " + transformedJar);
            }
        } catch (IOException e) {
            logger.warning("Failed to cleanup transformed JAR: " + e.getMessage());
        }
    }
}
