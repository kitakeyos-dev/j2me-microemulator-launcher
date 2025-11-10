package me.kitakeyos.j2me.launcher;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Helper class for reflection operations
 */
public class ReflectionHelper {

    /**
     * Gets the value of a field from an object using reflection
     *
     * @param obj       The object to get the field from
     * @param fieldName The name of the field
     * @param type      The expected type of the field
     * @param <T>       The type parameter
     * @return The field value cast to the specified type, or null if not found
     */
    public static <T> T getFieldValue(Object obj, String fieldName, Class<T> type) {
        try {
            Field field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return type.cast(field.get(obj));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            return null;
        }
    }

    /**
     * Sets the value of a field in an object using reflection
     *
     * @param obj       The object to set the field in
     * @param fieldName The name of the field
     * @param value     The value to set
     * @return true if successful, false otherwise
     */
    public static boolean setFieldValue(Object obj, String fieldName, Object value) {
        try {
            Field field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(obj, value);
            return true;
        } catch (NoSuchFieldException | IllegalAccessException e) {
            return false;
        }
    }

    /**
     * Invokes a method on an object using reflection
     *
     * @param obj        The object to invoke the method on
     * @param methodName The name of the method
     * @param paramTypes The parameter types
     * @param args       The arguments
     * @return The result of the method invocation
     * @throws Exception if the method cannot be invoked
     */
    public static Object invokeMethod(Object obj, String methodName, Class<?>[] paramTypes, Object... args) throws Exception {
        Method method = obj.getClass().getDeclaredMethod(methodName, paramTypes);
        method.setAccessible(true);
        return method.invoke(obj, args);
    }
}
