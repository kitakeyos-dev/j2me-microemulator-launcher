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

            org.objectweb.asm.Label dropFrame = new org.objectweb.asm.Label();

            // Guard 1: launcher window minimized → drop every frame
            // if (PaintThrottleConfig.windowMinimized) return;
            mv.visitFieldInsn(Opcodes.GETSTATIC, CONFIG_CLASS, "windowMinimized", "Z");
            mv.visitJumpInsn(Opcodes.IFNE, dropFrame);

            // Guard 2: component scrolled out of viewport
            // Rectangle r = this.getVisibleRect();
            // if (r.width <= 0 || r.height <= 0) return;
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "javax/swing/JComponent",
                    "getVisibleRect", "()Ljava/awt/Rectangle;");
            mv.visitVarInsn(Opcodes.ASTORE, 5);
            mv.visitVarInsn(Opcodes.ALOAD, 5);
            mv.visitFieldInsn(Opcodes.GETFIELD, "java/awt/Rectangle", "width", "I");
            mv.visitJumpInsn(Opcodes.IFLE, dropFrame);
            mv.visitVarInsn(Opcodes.ALOAD, 5);
            mv.visitFieldInsn(Opcodes.GETFIELD, "java/awt/Rectangle", "height", "I");
            mv.visitJumpInsn(Opcodes.IFLE, dropFrame);

            // long now = System.currentTimeMillis();
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/System",
                    "currentTimeMillis", "()J");
            mv.visitVarInsn(Opcodes.LSTORE, 5); // reuse slot 5-6 (Rectangle ref no longer needed)

            // Guard 3: idle timeout
            // long idle = PaintThrottleConfig.idleTimeoutMs;
            // if (idle > 0 && now - PaintThrottleConfig.lastActivityMs > idle) return;
            mv.visitFieldInsn(Opcodes.GETSTATIC, CONFIG_CLASS, "idleTimeoutMs", "J");
            mv.visitInsn(Opcodes.DUP2); // keep idle for the comparison below
            mv.visitInsn(Opcodes.LCONST_0);
            mv.visitInsn(Opcodes.LCMP);
            org.objectweb.asm.Label idleDisabled = new org.objectweb.asm.Label();
            mv.visitJumpInsn(Opcodes.IFLE, idleDisabled); // idle <= 0 → skip check
            // stack: [idle(long)]
            mv.visitVarInsn(Opcodes.LLOAD, 5);
            mv.visitFieldInsn(Opcodes.GETSTATIC, CONFIG_CLASS, "lastActivityMs", "J");
            mv.visitInsn(Opcodes.LSUB); // now - lastActivity
            // stack: [idle, elapsed]
            mv.visitInsn(Opcodes.LCMP); // compare: v1=idle, v2=elapsed → -1 if idle<elapsed (exceeded)
            mv.visitJumpInsn(Opcodes.IFLT, dropFrame); // cmp<0 means elapsed>idle → drop
            org.objectweb.asm.Label afterIdle = new org.objectweb.asm.Label();
            mv.visitJumpInsn(Opcodes.GOTO, afterIdle);

            mv.visitLabel(idleDisabled);
            // stack: [idle(long)] still present (from DUP2) — discard
            mv.visitInsn(Opcodes.POP2);

            mv.visitLabel(afterIdle);

            // Guard 4: rate limit
            // if (now - this.__lastPaintTime < intervalMs) return;
            mv.visitVarInsn(Opcodes.LLOAD, 5);
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitFieldInsn(Opcodes.GETFIELD, owner, FIELD_NAME, "J");
            mv.visitInsn(Opcodes.LSUB);
            mv.visitFieldInsn(Opcodes.GETSTATIC, CONFIG_CLASS, CONFIG_FIELD, "J");
            mv.visitInsn(Opcodes.LCMP);
            org.objectweb.asm.Label proceed = new org.objectweb.asm.Label();
            mv.visitJumpInsn(Opcodes.IFGE, proceed);

            mv.visitLabel(dropFrame);
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
            // slots 5-6 used for temp (Rectangle ref or long now).
            // Max stack: 4 for LSUB with two longs.
            super.visitMaxs(Math.max(maxStack, 4), Math.max(maxLocals, 7));
        }
    }
}
