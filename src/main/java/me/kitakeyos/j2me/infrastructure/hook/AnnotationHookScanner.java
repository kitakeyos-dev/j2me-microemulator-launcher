package me.kitakeyos.j2me.infrastructure.hook;

import me.kitakeyos.j2me.domain.hook.annotation.Apply;
import me.kitakeyos.j2me.domain.hook.annotation.Hook;
import me.kitakeyos.j2me.domain.hook.model.HookInvocation;
import me.kitakeyos.j2me.domain.hook.model.MethodInterceptor;
import me.kitakeyos.j2me.domain.hook.model.MethodSignature;
import me.kitakeyos.j2me.domain.hook.repository.HookRegistrar;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.logging.Logger;

/**
 * Turns {@link Hook}/{@link Apply} annotated classes into registered
 * {@link MethodInterceptor}s.
 *
 * <p>This is the only place that knows about the annotations — the registry,
 * the dispatcher and the class visitor all deal in plain interceptors, so a
 * hook written by hand is indistinguishable from an annotated one.
 *
 * <p>One instance of each hook class is created and reused for every
 * interception, so hooks may keep state in fields.
 */
public final class AnnotationHookScanner {

    private static final Logger logger = Logger.getLogger(AnnotationHookScanner.class.getName());

    private final HookRegistrar registrar;

    public AnnotationHookScanner(HookRegistrar registrar) {
        if (registrar == null) {
            throw new IllegalArgumentException("registrar must not be null");
        }
        this.registrar = registrar;
    }

    public static boolean isHookClass(Class<?> candidate) {
        return candidate != null
                && !candidate.isInterface()
                && candidate.isAnnotationPresent(Hook.class);
    }

    /**
     * Register every {@link Apply} method of one hook class.
     *
     * @return number of hooks registered
     * @throws ReflectiveOperationException if the class cannot be instantiated
     * @throws IllegalArgumentException     if the class or one of its methods is malformed
     */
    public int register(int instanceId, Class<?> hookClass) throws ReflectiveOperationException {
        Hook hook = hookClass.getAnnotation(Hook.class);
        if (hook == null) {
            throw new IllegalArgumentException(hookClass.getName() + " is not annotated with @Hook");
        }

        Object hookInstance = hookClass.getDeclaredConstructor().newInstance();
        int registered = 0;

        for (Method handler : hookClass.getDeclaredMethods()) {
            Apply apply = handler.getAnnotation(Apply.class);
            if (apply == null) {
                continue;
            }
            requireInterceptorShape(handler);
            handler.setAccessible(true);

            String targetName = apply.method().isEmpty() ? handler.getName() : apply.method();
            MethodSignature signature = MethodSignature.of(hook.value(), targetName, apply.descriptor());

            registrar.register(instanceId, signature, new ReflectiveInterceptor(hookInstance, handler));
            registered++;
        }

        if (registered == 0) {
            logger.warning(hookClass.getName() + " is annotated with @Hook but declares no @Apply method");
        }
        return registered;
    }

    /**
     * @return total number of hooks registered across all given classes
     */
    public int registerAll(int instanceId, Collection<Class<?>> hookClasses) throws ReflectiveOperationException {
        int registered = 0;
        for (Class<?> hookClass : hookClasses) {
            if (isHookClass(hookClass)) {
                registered += register(instanceId, hookClass);
            }
        }
        return registered;
    }

    /**
     * An {@code @Apply} method must be callable as an interceptor:
     * {@code Object name(HookInvocation)}.
     */
    private static void requireInterceptorShape(Method handler) {
        Class<?>[] parameters = handler.getParameterTypes();
        if (parameters.length != 1 || !parameters[0].isAssignableFrom(HookInvocation.class)) {
            throw new IllegalArgumentException("@Apply method " + handler
                    + " must take exactly one " + HookInvocation.class.getSimpleName() + " parameter");
        }
    }

    /**
     * Adapts an annotated method to the {@link MethodInterceptor} contract.
     */
    private static final class ReflectiveInterceptor implements MethodInterceptor {

        private final Object hookInstance;
        private final Method handler;

        ReflectiveInterceptor(Object hookInstance, Method handler) {
            this.hookInstance = hookInstance;
            this.handler = handler;
        }

        @Override
        public Object intercept(HookInvocation invocation) throws Throwable {
            try {
                return handler.invoke(hookInstance, invocation);
            } catch (InvocationTargetException e) {
                throw e.getCause() != null ? e.getCause() : e;
            }
        }
    }
}
