package me.kitakeyos.j2me.infrastructure.hook;

import me.kitakeyos.j2me.domain.hook.model.MethodInterceptor;
import me.kitakeyos.j2me.domain.hook.model.MethodSignature;
import me.kitakeyos.j2me.domain.hook.repository.HookRegistry;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * In-memory {@link HookRegistry}, partitioned by emulator instance.
 *
 * <p>Hooks are deliberately not persisted: they are code, and code arrives via
 * the injection JAR.
 *
 * <p>Reads happen on every method call of every hooked method, so lookups are
 * plain {@link ConcurrentHashMap} gets. Writes are rare (a human installing a
 * hook) and may take the slower path of rebuilding the owner index.
 */
public final class InMemoryHookRegistry implements HookRegistry {

    private static final Logger logger = Logger.getLogger(InMemoryHookRegistry.class.getName());

    private static final InMemoryHookRegistry INSTANCE = new InMemoryHookRegistry();

    private final Map<Integer, InstanceHooks> byInstance = new ConcurrentHashMap<>();

    private InMemoryHookRegistry() {
    }

    /**
     * Shared registry used by the bytecode pipeline. Presentation code should
     * go through {@code HookService} instead.
     */
    public static InMemoryHookRegistry getInstance() {
        return INSTANCE;
    }

    @Override
    public void register(int instanceId, MethodSignature signature, MethodInterceptor interceptor) {
        if (signature == null) {
            throw new IllegalArgumentException("signature must not be null");
        }
        if (interceptor == null) {
            throw new IllegalArgumentException("interceptor must not be null");
        }
        hooksFor(instanceId, true).put(signature, interceptor);
        logger.info("Registered hook " + signature + " for instance " + describeScope(instanceId));
    }

    @Override
    public boolean unregister(int instanceId, MethodSignature signature) {
        InstanceHooks hooks = hooksFor(instanceId, false);
        if (hooks == null) {
            return false;
        }
        boolean removed = hooks.remove(signature);
        if (removed) {
            logger.info("Unregistered hook " + signature + " for instance " + describeScope(instanceId));
        }
        return removed;
    }

    @Override
    public void clear(int instanceId) {
        InstanceHooks removed = byInstance.remove(instanceId);
        if (removed != null) {
            logger.info("Cleared " + removed.size() + " hook(s) for instance " + describeScope(instanceId));
        }
    }

    @Override
    public boolean hasAnyHooks(int instanceId) {
        return isNotEmpty(hooksFor(instanceId, false)) || isNotEmpty(hooksFor(ALL_INSTANCES, false));
    }

    @Override
    public boolean hasHooksForClass(int instanceId, String ownerInternalName) {
        return ownedBy(hooksFor(instanceId, false), ownerInternalName)
                || ownedBy(hooksFor(ALL_INSTANCES, false), ownerInternalName);
    }

    @Override
    public MethodInterceptor find(int instanceId, MethodSignature signature) {
        MethodInterceptor interceptor = lookup(hooksFor(instanceId, false), signature);
        if (interceptor != null) {
            return interceptor;
        }
        return instanceId == ALL_INSTANCES ? null : lookup(hooksFor(ALL_INSTANCES, false), signature);
    }

    /**
     * Exact descriptor first, wildcard second.
     */
    private static MethodInterceptor lookup(InstanceHooks hooks, MethodSignature signature) {
        if (hooks == null) {
            return null;
        }
        MethodInterceptor exact = hooks.get(signature);
        if (exact != null) {
            return exact;
        }
        return signature.isWildcard() ? null : hooks.get(signature.toWildcard());
    }

    private static boolean isNotEmpty(InstanceHooks hooks) {
        return hooks != null && hooks.size() > 0;
    }

    private static boolean ownedBy(InstanceHooks hooks, String ownerInternalName) {
        return hooks != null && hooks.owns(ownerInternalName);
    }

    private InstanceHooks hooksFor(int instanceId, boolean createIfAbsent) {
        InstanceHooks hooks = byInstance.get(instanceId);
        if (hooks == null && createIfAbsent) {
            InstanceHooks created = new InstanceHooks();
            InstanceHooks existing = byInstance.putIfAbsent(instanceId, created);
            hooks = existing != null ? existing : created;
        }
        return hooks;
    }

    private static String describeScope(int instanceId) {
        return instanceId == ALL_INSTANCES ? "<all>" : "#" + instanceId;
    }

    /**
     * Hooks of a single instance, plus an owner index so the class visitor can
     * reject an untouched class without walking every registration.
     */
    private static final class InstanceHooks {

        private final Map<MethodSignature, MethodInterceptor> interceptors = new ConcurrentHashMap<>();
        private volatile Set<String> owners = Collections.emptySet();

        void put(MethodSignature signature, MethodInterceptor interceptor) {
            interceptors.put(signature, interceptor);
            reindexOwners();
        }

        boolean remove(MethodSignature signature) {
            boolean removed = interceptors.remove(signature) != null;
            if (removed) {
                reindexOwners();
            }
            return removed;
        }

        MethodInterceptor get(MethodSignature signature) {
            return interceptors.get(signature);
        }

        boolean owns(String ownerInternalName) {
            return owners.contains(ownerInternalName);
        }

        int size() {
            return interceptors.size();
        }

        private void reindexOwners() {
            Set<String> rebuilt = new HashSet<>();
            for (MethodSignature signature : interceptors.keySet()) {
                rebuilt.add(signature.getOwner());
            }
            owners = Collections.unmodifiableSet(rebuilt);
        }
    }
}
