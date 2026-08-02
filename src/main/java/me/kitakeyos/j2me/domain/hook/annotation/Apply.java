package me.kitakeyos.j2me.domain.hook.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Binds one method of a {@link Hook} class to one method of the target class.
 *
 * <p>The annotated method must have the shape
 * {@code Object name(HookInvocation invocation)} — the same contract as
 * {@link me.kitakeyos.j2me.domain.hook.model.MethodInterceptor#intercept}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Apply {

    /**
     * Name of the target method. Defaults to the annotated method's own name.
     */
    String method() default "";

    /**
     * JVM descriptor of the target method, e.g. {@code (I)V}. Empty means
     * "every overload of {@link #method()}".
     */
    String descriptor() default "";
}
