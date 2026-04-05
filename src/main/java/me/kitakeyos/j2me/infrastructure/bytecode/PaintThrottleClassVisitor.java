package me.kitakeyos.j2me.infrastructure.bytecode;

import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.logging.Logger;

/**
 * ASM ClassVisitor that caps the paint rate of MicroEmulator's display
 * component to 30 FPS, regardless of how fast the MIDlet's game loop runs.
 *
 * <p>Why: with many concurrent instances, each MIDlet's {@code Canvas.repaint()}
 * funnels through {@code SwingDisplayComponent.repaintRequest(IIII)V}, which
 * does a full back-buffer render and schedules a Swing repaint on the EDT.
 * At 100 instances × 30-60 fps the EDT drowns in paint events.
 *
 * <p>What this transform does:
 * <ol>
 *   <li>Adds a {@code private long __lastPaintTime} field to
 *       {@code org/microemu/app/ui/swing/SwingDisplayComponent}.</li>
 *   <li>Prepends a guard at the entry of {@code repaintRequest(IIII)V} that
 *       drops the call if {@code now - __lastPaintTime < 33 ms}.</li>
 * </ol>
 *
 * <p>Game logic speed is unaffected — it's controlled by
 * {@code Thread.sleep()} which is intercepted separately by SpeedHelper.
 * Only the visual refresh rate is capped.
 */
public class PaintThrottleClassVisitor extends ClassAdapter {

    private static final Logger logger = Logger.getLogger(PaintThrottleClassVisitor.class.getName());

    private static final String TARGET_CLASS = "org/microemu/app/ui/swing/SwingDisplayComponent";
    private static final String TARGET_METHOD = "repaintRequest";
    private static final String TARGET_DESC = "(IIII)V";
    private static final String FIELD_NAME = "__lastPaintTime";
    private static final String CONFIG_CLASS = "me/kitakeyos/j2me/infrastructure/bytecode/PaintThrottleConfig";
    private static final String CONFIG_FIELD = "intervalMs";

    private final ModificationTracker tracker;
    private boolean isTargetClass = false;
    private String owner;

    public PaintThrottleClassVisitor(ClassVisitor cv, ModificationTracker tracker) {
        super(cv);
        this.tracker = tracker;
    }

    @Override
    public void visit(int version, int access, String name, String signature,
            String superName, String[] interfaces) {
        this.owner = name;
        this.isTargetClass = TARGET_CLASS.equals(name);
        super.visit(version, access, name, signature, superName, interfaces);

        if (isTargetClass) {
            // Add: private long __lastPaintTime;
            cv.visitField(Opcodes.ACC_PRIVATE, FIELD_NAME, "J", null, null).visitEnd();
            tracker.setModified(true);
            logger.info("Adding paint throttle to " + name);
        }
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc,
            String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        if (isTargetClass && TARGET_METHOD.equals(name) && TARGET_DESC.equals(desc)) {
            logger.info("Injecting throttle guard into " + owner + "." + name + desc);
            return new ThrottleGuardInjector(mv, owner);
        }
        return mv;
    }

    /**
     * Prepends a rate-limit guard to repaintRequest(IIII)V.
     *
     * <p>Equivalent source:
     * <pre>
     * void repaintRequest(int x, int y, int w, int h) {
     *     long now = System.currentTimeMillis();
     *     if (now - this.__lastPaintTime &lt; 33L) return;
     *     this.__lastPaintTime = now;
     *     // ... original body ...
     * }
     * </pre>
     *
     * <p>Local slots used:
     * <ul>
     *   <li>0: this</li>
     *   <li>1-4: x, y, w, h (original params)</li>
     *   <li>5-6: long now (inserted temp)</li>
     * </ul>
     */
    private static class ThrottleGuardInjector extends MethodAdapter {

        private final String owner;

        ThrottleGuardInjector(MethodVisitor mv, String owner) {
            super(mv);
            this.owner = owner;
        }

        @Override
        public void visitCode() {
            mv.visitCode();

            // long now = System.currentTimeMillis();
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/System",
                    "currentTimeMillis", "()J");
            mv.visitVarInsn(Opcodes.LSTORE, 5);

            // if (now - this.__lastPaintTime >= 33) goto proceed;
            mv.visitVarInsn(Opcodes.LLOAD, 5);
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitFieldInsn(Opcodes.GETFIELD, owner, FIELD_NAME, "J");
            mv.visitInsn(Opcodes.LSUB);
            mv.visitFieldInsn(Opcodes.GETSTATIC, CONFIG_CLASS, CONFIG_FIELD, "J");
            mv.visitInsn(Opcodes.LCMP);
            org.objectweb.asm.Label proceed = new org.objectweb.asm.Label();
            mv.visitJumpInsn(Opcodes.IFGE, proceed);

            // drop frame
            mv.visitInsn(Opcodes.RETURN);

            mv.visitLabel(proceed);

            // this.__lastPaintTime = now;
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitVarInsn(Opcodes.LLOAD, 5);
            mv.visitFieldInsn(Opcodes.PUTFIELD, owner, FIELD_NAME, "J");

            // fall through into original method body
        }

        @Override
        public void visitMaxs(int maxStack, int maxLocals) {
            // We introduced a long-sized local (slots 5-6) and added up to
            // 4 stack slots (2 longs for LSUB). Pad both.
            super.visitMaxs(Math.max(maxStack, 4), Math.max(maxLocals, 7));
        }
    }
}
