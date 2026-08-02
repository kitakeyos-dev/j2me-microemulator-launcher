package me.kitakeyos.j2me.domain.hook.model;

/**
 * The one thing a hook has to implement.
 *
 * <p>Single-method by design: new hook behaviour is a new implementation of
 * this interface, never a change to the bytecode pipeline.
 */
public interface MethodInterceptor {

    /**
     * @param invocation the intercepted call
     * @return the value the original caller receives; ignored for {@code void}
     *         methods, coerced to the primitive default when {@code null}
     * @throws Throwable propagated to the original caller unchanged
     */
    Object intercept(HookInvocation invocation) throws Throwable;
}
