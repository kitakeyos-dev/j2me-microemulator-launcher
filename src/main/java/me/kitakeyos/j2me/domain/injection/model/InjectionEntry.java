package me.kitakeyos.j2me.domain.injection.model;

/**
 * Interface for Java injection entry points.
 * Developers implement this interface in their own project,
 * build a JAR, and import it into the launcher to execute
 * against running emulator instances.
 *
 * Example usage:
 * <pre>
 * public class MyScript implements InjectionEntry {
 *     @Override
 *     public void execute(ClassLoader appClassLoader, InjectionLogger logger) {
 *         logger.info("Starting...");
 *         Class<?> cls = appClassLoader.loadClass("com.game.MainCanvas");
 *         logger.success("Class loaded!");
 *     }
 * }
 * </pre>
 */
public interface InjectionEntry {

    /**
     * Execute this injection against an emulator instance's ClassLoader.
     *
     * @param appClassLoader The MIDlet's ClassLoader from the running instance.
     *                       Use this to load and interact with MIDlet classes.
     * @param logger         Logger for outputting messages to the UI.
     */
    void execute(ClassLoader appClassLoader, InjectionLogger logger);
}
