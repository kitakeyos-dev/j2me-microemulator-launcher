package me.kitakeyos.j2me.infrastructure.hook;

import me.kitakeyos.j2me.infrastructure.bytecode.ByteCodeHelper;
import me.kitakeyos.j2me.infrastructure.bytecode.HookClassVisitor;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Finds the renamed original body that {@link HookClassVisitor} left behind,
 * so {@code HookInvocation.proceed()} can call it.
 *
 * <p>Two details make this correct rather than merely working:
 * <ul>
 *   <li>The original is looked up on its <em>declaring</em> class, not on the
 *       receiver's runtime class. A {@code super.foo()} call lands in the
 *       superclass trampoline and must proceed into the superclass body — not
 *       recurse back into the subclass.</li>
 *   <li>{@code HookClassVisitor} makes the renamed body {@code private}, so
 *       {@link Method#invoke} does not re-dispatch virtually and a subclass's
 *       own renamed body can never shadow it.</li>
 * </ul>
 *
 * <p>Resolved {@link Method} objects are cached per instance and dropped by
 * {@link #release(int)}, because each running instance has its own
 * {@code EmulatorClassLoader} and holding methods forever would pin it.
 */
final class OriginalMethodResolver {

    private final Map<Integer, Map<String, Method>> methodCache = new ConcurrentHashMap<>();
    private final Map<Integer, WeakReference<ClassLoader>> classLoaders = new ConcurrentHashMap<>();

    /**
     * Remember which loader owns an instance's classes. Needed to resolve the
     * declaring class of a hooked <em>static</em> method, where there is no
     * receiver to ask.
     */
    void bindClassLoader(int instanceId, ClassLoader classLoader) {
        classLoaders.put(instanceId, new WeakReference<>(classLoader));
    }

    void release(int instanceId) {
        methodCache.remove(instanceId);
        classLoaders.remove(instanceId);
    }

    /**
     * @param ownerInternalName class that declares the hooked method, e.g. {@code com/game/Canvas}
     * @param target            receiver, or {@code null} for a static method
     */
    Class<?> resolveDeclaringClass(int instanceId, String ownerInternalName, Object target)
            throws ClassNotFoundException {
        String className = ByteCodeHelper.toClassName(ownerInternalName);

        if (target != null) {
            for (Class<?> c = target.getClass(); c != null; c = c.getSuperclass()) {
                if (c.getName().equals(className)) {
                    return c;
                }
            }
            ClassLoader targetLoader = target.getClass().getClassLoader();
            if (targetLoader != null) {
                return targetLoader.loadClass(className);
            }
        }

        ClassLoader loader = classLoaderFor(instanceId);
        if (loader == null) {
            throw new ClassNotFoundException(
                    className + " (no classloader bound for instance #" + instanceId + ")");
        }
        return loader.loadClass(className);
    }

    /**
     * @param name       hooked method's original name (without the rename suffix)
     * @param descriptor hooked method's descriptor
     * @return the accessible renamed body
     */
    Method resolveOriginal(int instanceId, Class<?> declaringClass, String name, String descriptor)
            throws NoSuchMethodException {
        String cacheKey = declaringClass.getName() + '#' + name + descriptor;
        Map<String, Method> cache = cacheFor(instanceId);

        Method cached = cache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        Method found = findDeclared(declaringClass, name + HookClassVisitor.ORIGINAL_SUFFIX, descriptor);
        if (found == null) {
            throw new NoSuchMethodException(
                    "Original body not found: " + declaringClass.getName() + '.' + name + descriptor
                            + " — the class was loaded before the hook was registered");
        }
        found.setAccessible(true);
        cache.put(cacheKey, found);
        return found;
    }

    private static Method findDeclared(Class<?> declaringClass, String renamed, String descriptor) {
        for (Method method : declaringClass.getDeclaredMethods()) {
            if (!method.getName().equals(renamed)) {
                continue;
            }
            String candidate = ByteCodeHelper.getMethodDescriptor(method.getReturnType(),
                    method.getParameterTypes());
            if (candidate.equals(descriptor)) {
                return method;
            }
        }
        return null;
    }

    private Map<String, Method> cacheFor(int instanceId) {
        Map<String, Method> cache = methodCache.get(instanceId);
        if (cache == null) {
            Map<String, Method> created = new ConcurrentHashMap<>();
            Map<String, Method> existing = methodCache.putIfAbsent(instanceId, created);
            cache = existing != null ? existing : created;
        }
        return cache;
    }

    private ClassLoader classLoaderFor(int instanceId) {
        WeakReference<ClassLoader> ref = classLoaders.get(instanceId);
        return ref == null ? null : ref.get();
    }
}
