package me.kitakeyos.j2me.domain.hook.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as a container of hooks against one MIDlet class.
 *
 * <pre>
 * &#64;Hook("com.game.GameCanvas")
 * public class GameCanvasHooks {
 *
 *     &#64;Apply(method = "keyPressed", descriptor = "(I)V")
 *     public Object onKeyPressed(HookInvocation inv) throws Throwable {
 *         System.out.println("key = " + inv.getArguments()[0]);
 *         return inv.proceed();
 *     }
 * }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Hook {

    /**
     * Target class name, dotted or slashed.
     */
    String value();
}
