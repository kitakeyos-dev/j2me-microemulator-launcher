package me.kitakeyos.j2me.infrastructure.bytecode;

import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.logging.Logger;

/**
 * ASM ClassVisitor that transforms J2ME app classes for speed control.
 * 
 * Replaces Thread.sleep(millis) with a call to SpeedHelper.sleep(millis)
 * where SpeedHelper is a class we inject into the transformed JAR.
 * 
 * NOTE: This visitor does NOT hardcode instanceId. SpeedHelper will get
 * the instanceId from the current Thread at runtime.
 */
public class J2meSpeedClassVisitor extends ClassAdapter {

    private static final Logger logger = Logger.getLogger(J2meSpeedClassVisitor.class.getName());

    private final ModificationTracker tracker;
    private String className;

    // The helper class we inject into the JAR
    public static final String SPEED_HELPER_CLASS = "j2me_speed_helper/SpeedHelper";

    public J2meSpeedClassVisitor(ClassVisitor cv, ModificationTracker tracker) {
        super(cv);
        this.tracker = tracker;
    }

    // Backwards compatibility constructor
    public J2meSpeedClassVisitor(ClassVisitor cv, int instanceId, ModificationTracker tracker) {
        this(cv, tracker);
    }

    @Override
    public void visit(int version, int access, String name, String signature,
            String superName, String[] interfaces) {
        this.className = name;
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc,
            String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        return new SleepInterceptor(mv, tracker, className);
    }

    /**
     * Method visitor that intercepts Thread.sleep() calls.
     */
    private static class SleepInterceptor extends MethodAdapter {

        private final ModificationTracker tracker;
        private final String className;

        public SleepInterceptor(MethodVisitor mv, ModificationTracker tracker, String className) {
            super(mv);
            this.tracker = tracker;
            this.className = className;
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc) {
            // Intercept Thread.sleep(long)
            if (opcode == Opcodes.INVOKESTATIC &&
                    owner.equals("java/lang/Thread") &&
                    name.equals("sleep") &&
                    desc.equals("(J)V")) {

                logger.info("J2ME JAR: Intercepting Thread.sleep(long) in " + className);

                // Stack: [millis (long)]
                // Call our helper class - it will get instanceId from current Thread
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, SPEED_HELPER_CLASS, "sleep", "(J)V");

                tracker.setModified(true);
                return;
            }

            // Intercept Thread.sleep(long, int)
            if (opcode == Opcodes.INVOKESTATIC &&
                    owner.equals("java/lang/Thread") &&
                    name.equals("sleep") &&
                    desc.equals("(JI)V")) {

                logger.info("J2ME JAR: Intercepting Thread.sleep(long,int) in " + className);

                // Stack: [millis (long), nanos (int)]
                // Pop nanos
                mv.visitInsn(Opcodes.POP);
                // Stack: [millis (long)]

                mv.visitMethodInsn(Opcodes.INVOKESTATIC, SPEED_HELPER_CLASS, "sleep", "(J)V");

                tracker.setModified(true);
                return;
            }

            // Pass through other method calls
            super.visitMethodInsn(opcode, owner, name, desc);
        }
    }
}
