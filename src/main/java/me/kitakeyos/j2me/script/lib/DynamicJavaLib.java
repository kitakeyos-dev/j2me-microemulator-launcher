package me.kitakeyos.j2me.script.lib;

import org.luaj.vm2.*;
import org.luaj.vm2.lib.VarArgFunction;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;
import org.luaj.vm2.lib.jse.LuajavaLib;

public class DynamicJavaLib extends LuajavaLib {

    public DynamicJavaLib() {
    }

    @Override
    public LuaValue call(LuaValue modname, LuaValue env) {
        LuaTable luajava = new LuaTable();

        // Implement các function cơ bản của luajava
        luajava.set("bindClass", new BindClassFunction());
        luajava.set("new", new NewFunction());
        luajava.set("createProxy", new CreateProxyFunction());
        luajava.set("loadLib", new LoadLibFunction());

        // Set vào environment
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

                // Nếu arg đầu tiên là class
                if (args.arg1() instanceof LuaUserdata) {
                    Object obj = args.arg1().touserdata();
                    if (obj instanceof Class) {
                        Class<?> clazz = (Class<?>) obj;
                        if (args.narg() == 1) {
                            // No-arg constructor
                            Object instance = clazz.getDeclaredConstructor().newInstance();
                            return CoerceJavaToLua.coerce(instance);
                        }
                        // TODO: Implement constructor with arguments
                    }
                }

                // Nếu arg đầu tiên là string (class name)
                String className = args.checkjstring(1);
                Class<?> clazz = loadClass(className);

                if (args.narg() == 1) {
                    Object instance = clazz.getDeclaredConstructor().newInstance();
                    return CoerceJavaToLua.coerce(instance);
                }

                return LuaValue.NIL;
            } catch (Exception e) {
                throw new LuaError("Error creating instance: " + e.getMessage());
            }
        }
    }

    class CreateProxyFunction extends VarArgFunction {
        @Override
        public Varargs invoke(Varargs args) {
            // Simplified proxy creation - có thể implement sau
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

    private Class<?> loadClass(String className) throws ClassNotFoundException {
//        try {
//            return MIDletResourceLoader.classLoader.loadClass(className);
//        } catch (ClassNotFoundException e) {
//            try {
//                return Thread.currentThread().getContextClassLoader().loadClass(className);
//            } catch (ClassNotFoundException e2) {
//                return Class.forName(className);
//            }
//        }
        return null;
    }
}