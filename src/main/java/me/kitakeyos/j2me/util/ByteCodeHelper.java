package me.kitakeyos.j2me.util;

/**
 * Utility class for bytecode manipulation operations.
 * Provides helper methods for ASM bytecode transformations.
 */
public class ByteCodeHelper {

    /**
     * Convert Java class name to internal bytecode format.
     * Example: java.lang.String → java/lang/String
     *
     * @param klass The class to convert
     * @return Internal name format (with slashes)
     */
    public static String toInternalName(Class<?> klass) {
        return klass.getName().replace('.', '/');
    }

    /**
     * Convert Java class name string to internal bytecode format.
     * Example: "java.lang.String" → "java/lang/String"
     *
     * @param className The class name to convert
     * @return Internal name format (with slashes)
     */
    public static String toInternalName(String className) {
        return className.replace('.', '/');
    }

    /**
     * Convert internal bytecode name to Java class name.
     * Example: java/lang/String → java.lang.String
     *
     * @param internalName The internal name to convert
     * @return Java class name format (with dots)
     */
    public static String toClassName(String internalName) {
        return internalName.replace('/', '.');
    }

    /**
     * Get the resource path for a class file.
     * Example: java.lang.String → java/lang/String.class
     *
     * @param className The fully qualified class name
     * @return Resource path to class file
     */
    public static String getClassResourcePath(String className) {
        return toInternalName(className) + ".class";
    }

    /**
     * Get the descriptor for a class.
     * Example: String.class → Ljava/lang/String;
     *
     * @param klass The class
     * @return Type descriptor
     */
    public static String getDescriptor(Class<?> klass) {
        if (klass.isPrimitive()) {
            if (klass == void.class) return "V";
            if (klass == boolean.class) return "Z";
            if (klass == byte.class) return "B";
            if (klass == char.class) return "C";
            if (klass == short.class) return "S";
            if (klass == int.class) return "I";
            if (klass == long.class) return "J";
            if (klass == float.class) return "F";
            if (klass == double.class) return "D";
        }
        if (klass.isArray()) {
            return klass.getName().replace('.', '/');
        }
        return "L" + toInternalName(klass) + ";";
    }

    /**
     * Build method descriptor from parameter types and return type.
     * Example: (String, int) → String gives "(Ljava/lang/String;I)Ljava/lang/String;"
     *
     * @param returnType The return type
     * @param paramTypes The parameter types
     * @return Method descriptor
     */
    public static String getMethodDescriptor(Class<?> returnType, Class<?>... paramTypes) {
        StringBuilder desc = new StringBuilder("(");
        for (Class<?> paramType : paramTypes) {
            desc.append(getDescriptor(paramType));
        }
        desc.append(")");
        desc.append(getDescriptor(returnType));
        return desc.toString();
    }

    /**
     * Check if a class name represents a Java core class.
     *
     * @param internalName The internal class name
     * @return true if it's a Java core class
     */
    public static boolean isJavaCoreClass(String internalName) {
        return internalName.startsWith("java/") ||
                internalName.startsWith("javax/") ||
                internalName.startsWith("sun/") ||
                internalName.startsWith("jdk/");
    }

    /**
     * Check if a class name represents a MIDlet class.
     *
     * @param internalName The internal class name
     * @return true if it's a MIDlet-related class
     */
    public static boolean isMIDletClass(String internalName) {
        return internalName.startsWith("javax/microedition/") ||
                internalName.startsWith("org/microemu/");
    }

    /**
     * Extract package name from internal class name.
     * Example: java/lang/String → java/lang
     *
     * @param internalName The internal class name
     * @return Package name in internal format
     */
    public static String getPackageName(String internalName) {
        int lastSlash = internalName.lastIndexOf('/');
        return lastSlash > 0 ? internalName.substring(0, lastSlash) : "";
    }

    /**
     * Extract simple class name from internal name.
     * Example: java/lang/String → String
     *
     * @param internalName The internal class name
     * @return Simple class name
     */
    public static String getSimpleClassName(String internalName) {
        int lastSlash = internalName.lastIndexOf('/');
        return lastSlash >= 0 ? internalName.substring(lastSlash + 1) : internalName;
    }
}