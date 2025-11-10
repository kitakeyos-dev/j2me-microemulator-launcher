package me.kitakeyos.j2me.util;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ReflectionHelper {

    /**
     * Gets the value of a field from an object using reflection
     *
     * @param obj       The object to get the field from
     * @param fieldName The name of the field
     * @param type      The expected type of the field
     * @param <T>       The type parameter
     * @return The field value cast to the specified type, or null if not found
     * @throws NoSuchFieldException     If the field doesn't exist
     * @throws IllegalAccessException    If the method cannot be accessed
     */
    public static <T> T getFieldValue(Object obj, String fieldName, Class<T> type) throws NoSuchFieldException, IllegalAccessException {
        Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return type.cast(field.get(obj));

    }

    /**
     * Sets the value of a field in an object using reflection
     *
     * @param obj       The object to set the field in
     * @param fieldName The name of the field
     * @param value     The value to set
     * @return true if successful, false otherwise
     * @throws NoSuchFieldException     If the field doesn't exist
     * @throws IllegalAccessException    If the method cannot be accessed
     */
    public static boolean setFieldValue(Object obj, String fieldName, Object value) throws NoSuchFieldException, IllegalAccessException {
        Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(obj, value);
        return true;
    }

    /**
     * Gets the value of a field from an object using reflection
     *
     * @param obj       The object to get the field from
     * @param fieldName The name of the field
     * @return The field value as Object
     * @throws NoSuchFieldException   If the field doesn't exist
     * @throws IllegalAccessException If the field cannot be accessed
     */
    public static Object getFieldValue(Object obj, String fieldName) throws NoSuchFieldException, IllegalAccessException {
        Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(obj);
    }

    /**
     * Invokes a method without parameters using reflection
     *
     * @param obj        The object to invoke the method on
     * @param methodName The name of the method
     * @return The return value of the method
     * @throws NoSuchMethodException     If the method doesn't exist
     * @throws InvocationTargetException If the method throws an exception
     * @throws IllegalAccessException    If the method cannot be accessed
     */
    public static Object invokeMethod(Object obj, String methodName) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method method = obj.getClass().getMethod(methodName);
        return method.invoke(obj);
    }

    /**
     * Invokes a method with parameters using reflection
     *
     * @param obj        The object to invoke the method on
     * @param methodName The name of the method
     * @param paramTypes The parameter types of the method
     * @param args       The arguments to pass to the method
     * @return The return value of the method
     * @throws NoSuchMethodException     If the method doesn't exist
     * @throws InvocationTargetException If the method throws an exception
     * @throws IllegalAccessException    If the method cannot be accessed
     */
    public static Object invokeMethod(Object obj, String methodName, Class<?>[] paramTypes, Object... args) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method method = obj.getClass().getMethod(methodName, paramTypes);
        return method.invoke(obj, args);
    }

    /**
     * Invokes a declared method (including private methods) without parameters using reflection
     *
     * @param obj        The object to invoke the method on
     * @param methodName The name of the method
     * @return The return value of the method
     * @throws NoSuchMethodException     If the method doesn't exist
     * @throws InvocationTargetException If the method throws an exception
     * @throws IllegalAccessException    If the method cannot be accessed
     */
    public static Object invokeDeclaredMethod(Object obj, String methodName) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method method = obj.getClass().getDeclaredMethod(methodName);
        method.setAccessible(true);
        return method.invoke(obj);
    }

    /**
     * Invokes a declared method (including private methods) with parameters using reflection
     *
     * @param obj        The object to invoke the method on
     * @param methodName The name of the method
     * @param paramTypes The parameter types of the method
     * @param args       The arguments to pass to the method
     * @return The return value of the method
     * @throws NoSuchMethodException     If the method doesn't exist
     * @throws InvocationTargetException If the method throws an exception
     * @throws IllegalAccessException    If the method cannot be accessed
     */
    public static Object invokeDeclaredMethod(Object obj, String methodName, Class<?>[] paramTypes, Object... args) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method method = obj.getClass().getDeclaredMethod(methodName, paramTypes);
        method.setAccessible(true);
        return method.invoke(obj, args);
    }

    /**
     * Invokes a static method using reflection
     *
     * @param clazz      The class containing the static method
     * @param methodName The name of the method
     * @param paramTypes The parameter types of the method
     * @param args       The arguments to pass to the method
     * @return The return value of the method
     * @throws NoSuchMethodException     If the method doesn't exist
     * @throws InvocationTargetException If the method throws an exception
     * @throws IllegalAccessException    If the method cannot be accessed
     */
    public static Object invokeStaticMethod(Class<?> clazz, String methodName, Class<?>[] paramTypes, Object... args) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method method = clazz.getMethod(methodName, paramTypes);
        return method.invoke(null, args);
    }

    /**
     * Loads a class using the specified ClassLoader
     *
     * @param classLoader The ClassLoader to use
     * @param className   The fully qualified name of the class
     * @return The loaded Class object
     * @throws ClassNotFoundException If the class cannot be found
     */
    public static Class<?> loadClass(ClassLoader classLoader, String className) throws ClassNotFoundException {
        return classLoader.loadClass(className);
    }

    /**
     * Creates a new instance of a class using reflection
     *
     * @param classLoader The ClassLoader to use
     * @param className   The fully qualified name of the class
     * @return A new instance of the class
     * @throws ClassNotFoundException    If the class cannot be found
     * @throws NoSuchMethodException     If the constructor doesn't exist
     * @throws InvocationTargetException If the constructor throws an exception
     * @throws InstantiationException    If the class cannot be instantiated
     * @throws IllegalAccessException    If the constructor cannot be accessed
     */
    public static Object createInstance(ClassLoader classLoader, String className) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        Class<?> clazz = classLoader.loadClass(className);
        return clazz.getDeclaredConstructor().newInstance();
    }
}