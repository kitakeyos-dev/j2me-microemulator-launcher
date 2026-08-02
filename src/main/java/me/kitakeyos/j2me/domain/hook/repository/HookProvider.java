package me.kitakeyos.j2me.domain.hook.repository;

import java.io.File;
import java.util.List;

/**
 * Source of hook definitions — today a compiled JAR, tomorrow whatever else.
 *
 * <p>Exists so the UI can offer "load hooks from a file" without knowing how
 * hook classes are discovered or which classloader they end up in.
 */
public interface HookProvider {

    /**
     * Load hook definitions and register them.
     *
     * @param instanceId instance the hooks apply to, or {@link HookLookup#ALL_INSTANCES}
     * @param source     file to read hooks from
     * @return human-readable descriptions of what was registered
     * @throws Exception if the source cannot be read or a hook class is malformed
     */
    List<String> loadHooks(int instanceId, File source) throws Exception;
}
