package me.kitakeyos.j2me.domain.hook.model;

/**
 * A single intercepted call, handed to a {@link MethodInterceptor}.
 *
 * <p>This is <em>around</em> advice: the interceptor decides whether the
 * original method body runs at all, with which arguments, and what the caller
 * finally sees.
 *
 * <pre>
 * Object intercept(HookInvocation inv) throws Throwable {
 *     inv.getArguments()[0] = 999;   // rewrite an argument
 *     Object result = inv.proceed(); // run the original body
 *     return result;                 // or return something else entirely
 * }
 * </pre>
 *
 * <p>Skipping {@link #proceed()} skips the original body. For a primitive
 * return type, returning {@code null} yields that type's default value
 * ({@code 0} / {@code false}).
 */
public interface HookInvocation {

    /** Emulator instance this call belongs to. */
    int getInstanceId();

    /** Signature of the intercepted method, always with a concrete descriptor. */
    MethodSignature getSignature();

    /**
     * Receiver of the call, or {@code null} for a static method.
     */
    Object getTarget();

    /**
     * Live argument array — boxed for primitives, in declaration order.
     * Mutating an element changes what {@link #proceed()} passes on.
     */
    Object[] getArguments();

    /**
     * Run the original (pre-hook) method body.
     *
     * @return the original method's return value, or {@code null} for {@code void}
     * @throws Throwable whatever the original body throws, unwrapped
     */
    Object proceed() throws Throwable;
}
