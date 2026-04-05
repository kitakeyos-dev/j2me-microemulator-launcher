package me.kitakeyos.j2me.infrastructure.bytecode;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Generates bytecode for SpeedHelper class that will be injected into J2ME
 * JARs. The helper is loaded by MicroEmulator's MIDlet classloader, which
 * restricts visibility to standard {@code java.*} APIs — so the generated
 * code must not reference any launcher-side class directly.
 *
 * Speed multipliers are carried across the classloader boundary via the
 * Thread's own name: XThread encodes {@code <baseName> + '\u0001' + milli}
 * when speed != 1.0. The fast path (no override) is just {@code getName()}
 * + {@code indexOf} + one branch — no lock, no allocation, no reflection.
 *
 * The generated class is equivalent to:
 *
 * <pre>
 * package j2me_speed_helper;
 * public class SpeedHelper {
 *     public static void sleep(long millis) throws InterruptedException {
 *         String name = Thread.currentThread().getName();
 *         int idx = name.indexOf(1); // '\u0001'
 *         if (idx >= 0) {
 *             // Manual decimal parse — zero allocation on the hot path.
 *             int len = name.length();
 *             int milli = 0;
 *             for (int i = idx + 1; i < len; i++) {
 *                 milli = milli * 10 + name.charAt(i) - '0';
 *             }
 *             if (milli > 0 && milli != 1000) {
 *                 millis = millis * 1000L / milli;
 *             }
 *         }
 *         if (millis > 0) {
 *             Thread.sleep(millis);
 *         }
 *     }
 * }
 * </pre>
 */
public class SpeedHelperGenerator {

    public static final String CLASS_NAME = "j2me_speed_helper/SpeedHelper";
    public static final String CLASS_FILE_NAME = "j2me_speed_helper/SpeedHelper.class";

    /**
     * Generate bytecode for SpeedHelper class.
     */
    public static byte[] generateClass() {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);

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

        generateSleepMethod(cw);

        cw.visitEnd();
        return cw.toByteArray();
    }

    /**
     * Generate: public static void sleep(long millis) throws InterruptedException
     *
     * Local slots:
     *   0-1: millis (long) — reused to hold adjusted value
     *   2  : String name
     *   3  : int idx
     *   4  : int len
     *   5  : int milli (accumulator)
     *   6  : int i (loop index)
     */
    private static void generateSleepMethod(ClassWriter cw) {
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC,
                "sleep",
                "(J)V",
                null,
                new String[] { "java/lang/InterruptedException" });
        mv.visitCode();

        Label doSleep = new Label();
        Label end = new Label();

        // String name = Thread.currentThread().getName();
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Thread", "currentThread", "()Ljava/lang/Thread;");
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Thread", "getName", "()Ljava/lang/String;");
        mv.visitVarInsn(Opcodes.ASTORE, 2);

        // int idx = name.indexOf(1);
        mv.visitVarInsn(Opcodes.ALOAD, 2);
        mv.visitInsn(Opcodes.ICONST_1);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "indexOf", "(I)I");
        mv.visitVarInsn(Opcodes.ISTORE, 3);

        // if (idx < 0) goto doSleep;
        mv.visitVarInsn(Opcodes.ILOAD, 3);
        mv.visitJumpInsn(Opcodes.IFLT, doSleep);

        // int len = name.length();
        mv.visitVarInsn(Opcodes.ALOAD, 2);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "length", "()I");
        mv.visitVarInsn(Opcodes.ISTORE, 4);

        // int milli = 0;
        mv.visitInsn(Opcodes.ICONST_0);
        mv.visitVarInsn(Opcodes.ISTORE, 5);

        // int i = idx + 1;
        mv.visitVarInsn(Opcodes.ILOAD, 3);
        mv.visitInsn(Opcodes.ICONST_1);
        mv.visitInsn(Opcodes.IADD);
        mv.visitVarInsn(Opcodes.ISTORE, 6);

        // loop: if (i >= len) goto afterLoop;
        Label loopStart = new Label();
        Label afterLoop = new Label();
        mv.visitLabel(loopStart);
        mv.visitVarInsn(Opcodes.ILOAD, 6);
        mv.visitVarInsn(Opcodes.ILOAD, 4);
        mv.visitJumpInsn(Opcodes.IF_ICMPGE, afterLoop);

        // milli = milli * 10 + name.charAt(i) - '0';
        mv.visitVarInsn(Opcodes.ILOAD, 5);
        mv.visitIntInsn(Opcodes.BIPUSH, 10);
        mv.visitInsn(Opcodes.IMUL);
        mv.visitVarInsn(Opcodes.ALOAD, 2);
        mv.visitVarInsn(Opcodes.ILOAD, 6);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "charAt", "(I)C");
        mv.visitInsn(Opcodes.IADD);
        mv.visitIntInsn(Opcodes.BIPUSH, 48); // '0'
        mv.visitInsn(Opcodes.ISUB);
        mv.visitVarInsn(Opcodes.ISTORE, 5);

        // i++;
        mv.visitIincInsn(6, 1);

        // goto loopStart;
        mv.visitJumpInsn(Opcodes.GOTO, loopStart);

        mv.visitLabel(afterLoop);

        // if (milli <= 0) goto doSleep;
        mv.visitVarInsn(Opcodes.ILOAD, 5);
        mv.visitJumpInsn(Opcodes.IFLE, doSleep);

        // if (milli == 1000) goto doSleep;
        mv.visitVarInsn(Opcodes.ILOAD, 5);
        mv.visitIntInsn(Opcodes.SIPUSH, 1000);
        mv.visitJumpInsn(Opcodes.IF_ICMPEQ, doSleep);

        // millis = millis * 1000L / milli;
        mv.visitVarInsn(Opcodes.LLOAD, 0);
        mv.visitLdcInsn(Long.valueOf(1000L));
        mv.visitInsn(Opcodes.LMUL);
        mv.visitVarInsn(Opcodes.ILOAD, 5);
        mv.visitInsn(Opcodes.I2L);
        mv.visitInsn(Opcodes.LDIV);
        mv.visitVarInsn(Opcodes.LSTORE, 0);

        mv.visitLabel(doSleep);

        // if (millis <= 0) goto end;
        mv.visitVarInsn(Opcodes.LLOAD, 0);
        mv.visitInsn(Opcodes.LCONST_0);
        mv.visitInsn(Opcodes.LCMP);
        mv.visitJumpInsn(Opcodes.IFLE, end);

        // Thread.sleep(millis);
        mv.visitVarInsn(Opcodes.LLOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Thread", "sleep", "(J)V");

        mv.visitLabel(end);
        mv.visitInsn(Opcodes.RETURN);

        mv.visitMaxs(4, 7);
        mv.visitEnd();
    }
}
