package me.kitakeyos.j2me.infrastructure.hook;

import me.kitakeyos.j2me.domain.hook.model.HookInvocation;
import me.kitakeyos.j2me.domain.hook.model.MethodSignature;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * {@link HookInvocation} whose {@link #proceed()} reflectively calls the
 * renamed original body.
 *
 * <p>Resolution is lazy: an interceptor that never proceeds pays nothing, and
 * a hook that fully replaces a method keeps working even if the original could
 * not be resolved.
 *
 * <p>Not thread-safe, and does not need to be — one instance per intercepted
 * call.
 */
final class ReflectiveHookInvocation implements HookInvocation {

    private final int instanceId;
    private final MethodSignature signature;
    private final Object target;
    private final Object[] arguments;
    private final OriginalMethodResolver resolver;

    ReflectiveHookInvocation(int instanceId, MethodSignature signature, Object target, Object[] arguments,
            OriginalMethodResolver resolver) {
        this.instanceId = instanceId;
        this.signature = signature;
        this.target = target;
        this.arguments = arguments;
        this.resolver = resolver;
    }

    @Override
    public int getInstanceId() {
        return instanceId;
    }

    @Override
    public MethodSignature getSignature() {
        return signature;
    }

    @Override
    public Object getTarget() {
        return target;
    }

    @Override
    public Object[] getArguments() {
        return arguments;
    }

    @Override
    public Object proceed() throws Throwable {
        Class<?> declaringClass = resolver.resolveDeclaringClass(instanceId, signature.getOwner(), target);
        Method original = resolver.resolveOriginal(instanceId, declaringClass, signature.getName(),
                signature.getDescriptor());
        try {
            return original.invoke(target, arguments);
        } catch (InvocationTargetException e) {
            // Hand the caller the exception the MIDlet actually threw, not the wrapper.
            throw e.getCause() != null ? e.getCause() : e;
        }
    }
}
