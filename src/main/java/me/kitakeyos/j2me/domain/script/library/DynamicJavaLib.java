package me.kitakeyos.j2me.domain.script.library;

import org.luaj.vm2.*;
import org.luaj.vm2.lib.VarArgFunction;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;
import org.luaj.vm2.lib.jse.LuajavaLib;

public class DynamicJavaLib extends LuajavaLib {

    private ClassLoader instanceClassLoader;

    public DynamicJavaLib() {
    }

    public DynamicJavaLib(ClassLoader classLoader) {
        this.instanceClassLoader = classLoader;
    }

    public void setInstanceClassLoader(ClassLoader classLoader) {
        this.instanceClassLoader = classLoader;
    }

    @Override
    public LuaValue call(LuaValue modname, LuaValue env) {
        LuaTable luajava = new LuaTable();

        // Implement basic luajava functions
        luajava.set("bindClass", new BindClassFunction());
        luajava.set("new", new NewFunction());
        luajava.set("createProxy", new CreateProxyFunction());
        luajava.set("loadLib", new LoadLibFunction());

        // Set into environment
        env.set("luajava", luajava);
        env.get("package").get("loaded").set("luajava", luajava);

        return luajava;
    }

    class BindClassFunction extends VarArgFunction {
        @Override
        public Varargs invoke(Varargs args) {
            try {
                String className = args.checkjstring(1);
                Class<?> clazz = loadClass(className);
                return CoerceJavaToLua.coerce(clazz);
            } catch (ClassNotFoundException e) {
                return LuaValue.NIL;
            }
        }
    }

    class NewFunction extends VarArgFunction {
        @Override
        public Varargs invoke(Varargs args) {
            try {
                if (args.narg() < 1) {
                    throw new LuaError("luajava.new requires at least 1 argument");
                }

                // If the first arg is a class
                if (args.arg1() instanceof LuaUserdata) {
                    Object obj = args.arg1().touserdata();
                    if (obj instanceof Class) {
                        Class<?> clazz = (Class<?>) obj;
                        if (args.narg() == 1) {
                            // No-arg constructor
                            Object instance = clazz.getDeclaredConstructor().newInstance();
                            return CoerceJavaToLua.coerce(instance);
                        } else {
                            // Constructor with arguments
                            Object[] javaArgs = convertLuaArgsToJava(args, 2);
                            Object instance = createInstanceWithArgs(clazz, javaArgs);
                            return CoerceJavaToLua.coerce(instance);
                        }
                    }
                }

                // If the first arg is a string (class name)
                String className = args.checkjstring(1);
                Class<?> clazz = loadClass(className);

                if (args.narg() == 1) {
                    Object instance = clazz.getDeclaredConstructor().newInstance();
                    return CoerceJavaToLua.coerce(instance);
                } else {
                    // Constructor with arguments
                    Object[] javaArgs = convertLuaArgsToJava(args, 2);
                    Object instance = createInstanceWithArgs(clazz, javaArgs);
                    return CoerceJavaToLua.coerce(instance);
                }
            } catch (Exception e) {
                throw new LuaError("Error creating instance: " + e.getMessage());
            }
        }
    }

    static class CreateProxyFunction extends VarArgFunction {
        @Override
        public Varargs invoke(Varargs args) {
            // Simplified proxy creation - can be implemented later
            return LuaValue.NIL;
        }
    }

    class LoadLibFunction extends VarArgFunction {
        @Override
        public Varargs invoke(Varargs args) {
            try {
                String className = args.checkjstring(1);
                String methodName = args.checkjstring(2);

                Class<?> clazz = loadClass(className);
                java.lang.reflect.Method method = clazz.getMethod(methodName);
                Object result = method.invoke(null);

                return CoerceJavaToLua.coerce(result);
            } catch (Exception e) {
                throw new LuaError("Error loading library: " + e.getMessage());
            }
        }
    }

    private Object[] convertLuaArgsToJava(Varargs args, int startIndex) {
        int argCount = args.narg() - startIndex + 1;
        Object[] javaArgs = new Object[argCount];

        for (int i = 0; i < argCount; i++) {
            LuaValue luaArg = args.arg(startIndex + i);
            javaArgs[i] = coerceLuaToJava(luaArg);
        }

        return javaArgs;
    }

    private Object coerceLuaToJava(LuaValue luaValue) {
        if (luaValue.isnil()) {
            return null;
        } else if (luaValue.isboolean()) {
            return luaValue.toboolean();
        } else if (luaValue.isint()) {
            return luaValue.toint();
        } else if (luaValue.isnumber()) {
            return luaValue.todouble();
        } else if (luaValue.isstring()) {
            return luaValue.tojstring();
        } else if (luaValue.isuserdata()) {
            return luaValue.touserdata();
        } else {
            return luaValue;
        }
    }

    private Object createInstanceWithArgs(Class<?> clazz, Object[] args) throws Exception {
        // Try to find a matching constructor in public constructors
        for (java.lang.reflect.Constructor<?> constructor : clazz.getConstructors()) {
            if (constructor.getParameterCount() == args.length) {
                if (parametersMatch(constructor.getParameterTypes(), args)) {
                    return constructor.newInstance(args);
                }
            }
        }

        // If no exact match, try declared constructors (including private ones)
        for (java.lang.reflect.Constructor<?> constructor : clazz.getDeclaredConstructors()) {
            if (constructor.getParameterCount() == args.length) {
                if (parametersMatch(constructor.getParameterTypes(), args)) {
                    constructor.setAccessible(true);
                    return constructor.newInstance(args);
                }
            }
        }

        throw new NoSuchMethodException("No matching constructor found for " + clazz.getName());
    }

    private boolean parametersMatch(Class<?>[] paramTypes, Object[] args) {
        for (int i = 0; i < paramTypes.length; i++) {
            if (args[i] == null) {
                if (paramTypes[i].isPrimitive()) {
                    return false;
                }
                continue;
            }

            Class<?> paramType = paramTypes[i];
            Class<?> argType = args[i].getClass();

            // Handle primitive types and their wrappers
            if (paramType.isPrimitive()) {
                if (!isPrimitiveCompatible(paramType, argType)) {
                    return false;
                }
            } else if (!paramType.isAssignableFrom(argType)) {
                return false;
            }
        }
        return true;
    }

    private boolean isPrimitiveCompatible(Class<?> primitiveType, Class<?> wrapperType) {
        if (primitiveType == int.class) return wrapperType == Integer.class;
        if (primitiveType == long.class) return wrapperType == Long.class;
        if (primitiveType == double.class) return wrapperType == Double.class;
        if (primitiveType == float.class) return wrapperType == Float.class;
        if (primitiveType == boolean.class) return wrapperType == Boolean.class;
        if (primitiveType == byte.class) return wrapperType == Byte.class;
        if (primitiveType == short.class) return wrapperType == Short.class;
        if (primitiveType == char.class) return wrapperType == Character.class;
        return false;
    }

    private Class<?> loadClass(String className) throws ClassNotFoundException {
        if (instanceClassLoader != null) {
            try {
                return instanceClassLoader.loadClass(className);
            } catch (ClassNotFoundException e) {
                // Fall through to other loaders
            }
        }

        try {
            return Thread.currentThread().getContextClassLoader().loadClass(className);
        } catch (ClassNotFoundException e) {
            return Class.forName(className);
        }
    }
}