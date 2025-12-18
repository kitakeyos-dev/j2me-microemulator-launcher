package me.kitakeyos.j2me.infrastructure.bytecode;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Label;

/**
 * Generates bytecode for SpeedHelper class that will be injected into J2ME
 * JARs.
 * 
 * This class is designed to be included in the transformed JAR so that
 * J2ME app classes can call it without ClassLoader issues.
 * 
 * The generated class is equivalent to:
 * 
 * package j2me_speed_helper;
 * public class SpeedHelper {
 * public static void sleep(long millis) throws InterruptedException {
 * int instanceId = getInstanceIdFromThread();
 * String speedStr = System.getProperty("j2me.speed." + instanceId);
 * if (speedStr == null) speedStr = "1.0";
 * double speed = Double.parseDouble(speedStr);
 * long adjusted = (long)(millis / speed);
 * if (adjusted > 0) {
 * Thread.sleep(adjusted);
 * }
 * }
 * 
 * private static int getInstanceIdFromThread() {
 * Thread t = Thread.currentThread();
 * try {
 * // Use reflection to call t.getInstanceId() if it's XThread
 * return
 * ((Integer)t.getClass().getMethod("getInstanceId").invoke(t)).intValue();
 * } catch (Exception e) {
 * return 0; // Default instance
 * }
 * }
 * }
 */
public class SpeedHelperGenerator {

    public static final String CLASS_NAME = "j2me_speed_helper/SpeedHelper";
    public static final String CLASS_FILE_NAME = "j2me_speed_helper/SpeedHelper.class";

    /**
     * Generate bytecode for SpeedHelper class.
     */
    public static byte[] generateClass() {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);

        // Class header
        cw.visit(Opcodes.V1_5,
                Opcodes.ACC_PUBLIC + Opcodes.ACC_SUPER,
                CLASS_NAME,
                null,
                "java/lang/Object",
                null);

        // Default constructor
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V");
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();

        // Generate getInstanceIdFromThread() method
        generateGetInstanceIdMethod(cw);

        // Generate sleep(long millis) method
        generateSleepMethod(cw);

        cw.visitEnd();
        return cw.toByteArray();
    }

    /**
     * Generate: private static int getInstanceIdFromThread()
     */
    private static void generateGetInstanceIdMethod(ClassWriter cw) {
        MethodVisitor mv = cw.visitMethod(
                Opcodes.ACC_PRIVATE + Opcodes.ACC_STATIC,
                "getInstanceIdFromThread",
                "()I",
                null,
                null);
        mv.visitCode();

        Label tryStart = new Label();
        Label tryEnd = new Label();
        Label catchHandler = new Label();
        mv.visitTryCatchBlock(tryStart, tryEnd, catchHandler, "java/lang/Exception");

        mv.visitLabel(tryStart);

        // Thread t = Thread.currentThread()
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Thread", "currentThread", "()Ljava/lang/Thread;");
        mv.visitVarInsn(Opcodes.ASTORE, 0);

        // t.getClass()
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;");

        // .getMethod("getInstanceId")
        mv.visitLdcInsn("getInstanceId");
        mv.visitInsn(Opcodes.ICONST_0);
        mv.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Class");
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Class", "getMethod",
                "(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;");

        // .invoke(t)
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitInsn(Opcodes.ICONST_0);
        mv.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/reflect/Method", "invoke",
                "(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;");

        // Cast to Integer and get intValue
        mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Integer");
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I");

        mv.visitLabel(tryEnd);
        mv.visitInsn(Opcodes.IRETURN);

        mv.visitLabel(catchHandler);
        // On exception, just return 0
        mv.visitInsn(Opcodes.POP); // pop exception
        mv.visitInsn(Opcodes.ICONST_0);
        mv.visitInsn(Opcodes.IRETURN);

        mv.visitMaxs(4, 1);
        mv.visitEnd();
    }

    /**
     * Generate: public static void sleep(long millis) throws InterruptedException
     */
    private static void generateSleepMethod(ClassWriter cw) {
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC,
                "sleep",
                "(J)V",
                null,
                new String[] { "java/lang/InterruptedException" });
        mv.visitCode();

        // int instanceId = getInstanceIdFromThread()
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, CLASS_NAME, "getInstanceIdFromThread", "()I");
        mv.visitVarInsn(Opcodes.ISTORE, 2);

        // String key = "j2me.speed." + instanceId
        mv.visitTypeInsn(Opcodes.NEW, "java/lang/StringBuilder");
        mv.visitInsn(Opcodes.DUP);
        mv.visitLdcInsn("j2me.speed.");
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V");
        mv.visitVarInsn(Opcodes.ILOAD, 2); // instanceId
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(I)Ljava/lang/StringBuilder;");
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;");
        // Stack: [key]

        // String speedStr = System.getProperty(key) - WITHOUT default
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/System", "getProperty",
                "(Ljava/lang/String;)Ljava/lang/String;");
        // Stack: [speedStr] (may be null)

        // if (speedStr == null) speedStr = "1.0"
        mv.visitInsn(Opcodes.DUP);
        Label notNull = new Label();
        mv.visitJumpInsn(Opcodes.IFNONNULL, notNull);
        mv.visitInsn(Opcodes.POP); // pop null
        mv.visitLdcInsn("1.0"); // push default
        mv.visitLabel(notNull);
        // Stack: [speedStr]

        // double speed = Double.parseDouble(speedStr)
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Double", "parseDouble",
                "(Ljava/lang/String;)D");
        mv.visitVarInsn(Opcodes.DSTORE, 3); // Store speed in local var 3
        // Stack: []

        // long adjusted = (long)(millis / speed)
        mv.visitVarInsn(Opcodes.LLOAD, 0); // millis
        mv.visitInsn(Opcodes.L2D); // convert to double
        mv.visitVarInsn(Opcodes.DLOAD, 3); // speed
        mv.visitInsn(Opcodes.DDIV); // millis / speed
        mv.visitInsn(Opcodes.D2L); // convert back to long
        mv.visitVarInsn(Opcodes.LSTORE, 5); // Store adjusted in local var 5
        // Stack: []

        // if (adjusted > 0) Thread.sleep(adjusted)
        mv.visitVarInsn(Opcodes.LLOAD, 5); // adjusted
        mv.visitInsn(Opcodes.LCONST_0);
        mv.visitInsn(Opcodes.LCMP);
        Label skipSleep = new Label();
        mv.visitJumpInsn(Opcodes.IFLE, skipSleep);

        // Thread.sleep(adjusted)
        mv.visitVarInsn(Opcodes.LLOAD, 5);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Thread", "sleep", "(J)V");

        mv.visitLabel(skipSleep);
        mv.visitInsn(Opcodes.RETURN);

        mv.visitMaxs(4, 7);
        mv.visitEnd();
    }
}
