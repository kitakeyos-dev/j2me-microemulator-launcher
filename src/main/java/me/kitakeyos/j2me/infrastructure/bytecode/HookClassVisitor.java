package me.kitakeyos.j2me.infrastructure.bytecode;

import me.kitakeyos.j2me.domain.hook.model.MethodSignature;
import me.kitakeyos.j2me.domain.hook.repository.HookLookup;
import me.kitakeyos.j2me.infrastructure.hook.HookDispatcher;

import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * ASM visitor that turns any registered method into a hook point, using the
 * classic rename-and-trampoline transform.
 *
 * <p>Given a hook on {@code void keyPressed(int)}, the class comes out as:
 * <pre>
 * // renamed, untouched body — private so reflection can call exactly this one
 * private synthetic void keyPressed$orig(int p0) { ...original... }
 *
 * // generated replacement under the original name and descriptor
 * void keyPressed(int p0) {
 *     HookDispatcher.dispatch(instanceId, "com/game/Canvas", "keyPressed", "(I)V",
 *                             this, new Object[]{ Integer.valueOf(p0) });
 * }
 * </pre>
 *
 * <p>Everything that called {@code keyPressed} — including virtual and
 * {@code super} calls from elsewhere in the MIDlet — now lands in the
 * trampoline, and {@code HookInvocation.proceed()} reaches the preserved body.
 *
 * <h3>Why the generated code is this plain</h3>
 * It contains no branches, so no stack map frames are ever required and the
 * transform is safe on both the ancient class files inside J2ME JARs and on
 * modern ones. Casting and null handling live in {@link HookDispatcher}'s
 * {@code toXxx} helpers rather than in emitted instructions.
 *
 * <h3>Limits</h3>
 * Constructors, abstract and native methods are skipped — a constructor's
 * body cannot be moved without breaking the mandatory {@code super()} call.
 * Hooks must be registered before the target class is loaded, since the
 * rewrite happens once, in {@code EmulatorClassLoader.findClass}.
 */
public class HookClassVisitor extends ClassAdapter {

    private static final Logger logger = Logger.getLogger(HookClassVisitor.class.getName());

    /** Suffix appended to the preserved original method. */
    public static final String ORIGINAL_SUFFIX = "$orig";

    private static final String DISPATCHER = ByteCodeHelper.toInternalName(HookDispatcher.class);
    private static final String OBJECT = "java/lang/Object";

    /**
     * Upper bound on the trampoline's operand stack: 6 fixed dispatch
     * arguments, plus (array dup + index + a two-slot argument) while filling
     * the argument array. Over-estimating is legal; under-estimating is not.
     */
    private static final int TRAMPOLINE_MAX_STACK = 10;

    private final int instanceId;
    private final ModificationTracker tracker;
    private final HookLookup lookup;
    private final List<HookedMethod> pending = new ArrayList<>();

    private String owner;
    private boolean classHasHooks;

    public HookClassVisitor(ClassVisitor cv, int instanceId, ModificationTracker tracker, HookLookup lookup) {
        super(cv);
        this.instanceId = instanceId;
        this.tracker = tracker;
        this.lookup = lookup;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName,
            String[] interfaces) {
        this.owner = name;
        this.classHasHooks = lookup.hasHooksForClass(instanceId, name);
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature,
            String[] exceptions) {
        if (!classHasHooks || !isRewritable(access, name)) {
            return super.visitMethod(access, name, desc, signature, exceptions);
        }
        if (lookup.find(instanceId, MethodSignature.of(owner, name, desc)) == null) {
            return super.visitMethod(access, name, desc, signature, exceptions);
        }

        pending.add(new HookedMethod(access, name, desc, signature, exceptions));
        tracker.setModified(true);
        logger.info("Hooking " + owner + "." + name + desc + " (instance #" + instanceId + ")");

        // The original body carries on into the writer under its new name.
        return super.visitMethod(originalAccess(access), name + ORIGINAL_SUFFIX, desc, signature, exceptions);
    }

    @Override
    public void visitEnd() {
        // Emitted here rather than from visitMethod so no two method visitors
        // of this class are ever open at the same time.
        for (HookedMethod method : pending) {
            emitTrampoline(method);
        }
        pending.clear();
        super.visitEnd();
    }

    /**
     * Constructors cannot be renamed (the verifier requires {@code super()} in
     * {@code <init>}), and abstract/native methods have no body to preserve.
     */
    private static boolean isRewritable(int access, String name) {
        if ((access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)) != 0) {
            return false;
        }
        return !"<init>".equals(name) && !"<clinit>".equals(name);
    }

    /**
     * Access flags for the preserved body. Forcing {@code private} is what
     * stops {@link java.lang.reflect.Method#invoke} from re-dispatching
     * virtually into a subclass's own preserved body.
     */
    private static int originalAccess(int access) {
        int cleared = access & ~(Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED | Opcodes.ACC_FINAL);
        return cleared | Opcodes.ACC_PRIVATE | Opcodes.ACC_SYNTHETIC;
    }

    private void emitTrampoline(HookedMethod method) {
        boolean isStatic = (method.access & Opcodes.ACC_STATIC) != 0;
        Type[] argumentTypes = Type.getArgumentTypes(method.desc);
        Type returnType = Type.getReturnType(method.desc);

        // Locking, if any, stays on the preserved body.
        int access = method.access & ~Opcodes.ACC_SYNCHRONIZED;
        MethodVisitor mv = cv.visitMethod(access, method.name, method.desc, method.signature, method.exceptions);
        mv.visitCode();

        pushInt(mv, instanceId);
        mv.visitLdcInsn(owner);
        mv.visitLdcInsn(method.name);
        mv.visitLdcInsn(method.desc);
        if (isStatic) {
            mv.visitInsn(Opcodes.ACONST_NULL);
        } else {
            mv.visitVarInsn(Opcodes.ALOAD, 0);
        }

        // new Object[]{ boxed arguments... }
        pushInt(mv, argumentTypes.length);
        mv.visitTypeInsn(Opcodes.ANEWARRAY, OBJECT);
        int slot = isStatic ? 0 : 1;
        for (int i = 0; i < argumentTypes.length; i++) {
            Type argument = argumentTypes[i];
            mv.visitInsn(Opcodes.DUP);
            pushInt(mv, i);
            mv.visitVarInsn(argument.getOpcode(Opcodes.ILOAD), slot);
            box(mv, argument);
            mv.visitInsn(Opcodes.AASTORE);
            slot += argument.getSize();
        }

        mv.visitMethodInsn(Opcodes.INVOKESTATIC, DISPATCHER, "dispatch", HookDispatcher.DISPATCH_DESCRIPTOR);
        emitReturn(mv, returnType);

        mv.visitMaxs(TRAMPOLINE_MAX_STACK, slot);
        mv.visitEnd();
    }

    /**
     * Convert the dispatcher's {@code Object} result into the declared return
     * type and return it.
     */
    private static void emitReturn(MethodVisitor mv, Type returnType) {
        switch (returnType.getSort()) {
            case Type.VOID:
                mv.visitInsn(Opcodes.POP);
                mv.visitInsn(Opcodes.RETURN);
                break;
            case Type.BOOLEAN:
                unbox(mv, "toBoolean", "Z");
                mv.visitInsn(Opcodes.IRETURN);
                break;
            case Type.BYTE:
                unbox(mv, "toByte", "B");
                mv.visitInsn(Opcodes.IRETURN);
                break;
            case Type.CHAR:
                unbox(mv, "toChar", "C");
                mv.visitInsn(Opcodes.IRETURN);
                break;
            case Type.SHORT:
                unbox(mv, "toShort", "S");
                mv.visitInsn(Opcodes.IRETURN);
                break;
            case Type.INT:
                unbox(mv, "toInt", "I");
                mv.visitInsn(Opcodes.IRETURN);
                break;
            case Type.LONG:
                unbox(mv, "toLong", "J");
                mv.visitInsn(Opcodes.LRETURN);
                break;
            case Type.FLOAT:
                unbox(mv, "toFloat", "F");
                mv.visitInsn(Opcodes.FRETURN);
                break;
            case Type.DOUBLE:
                unbox(mv, "toDouble", "D");
                mv.visitInsn(Opcodes.DRETURN);
                break;
            default:
                // Object or array: getInternalName() already yields the right
                // CHECKCAST operand for both ("java/lang/String", "[I").
                mv.visitTypeInsn(Opcodes.CHECKCAST, returnType.getInternalName());
                mv.visitInsn(Opcodes.ARETURN);
                break;
        }
    }

    private static void unbox(MethodVisitor mv, String helper, String returnDescriptor) {
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, DISPATCHER, helper,
                "(Ljava/lang/Object;)" + returnDescriptor);
    }

    private static void box(MethodVisitor mv, Type type) {
        switch (type.getSort()) {
            case Type.BOOLEAN:
                valueOf(mv, "java/lang/Boolean", "Z");
                break;
            case Type.BYTE:
                valueOf(mv, "java/lang/Byte", "B");
                break;
            case Type.CHAR:
                valueOf(mv, "java/lang/Character", "C");
                break;
            case Type.SHORT:
                valueOf(mv, "java/lang/Short", "S");
                break;
            case Type.INT:
                valueOf(mv, "java/lang/Integer", "I");
                break;
            case Type.LONG:
                valueOf(mv, "java/lang/Long", "J");
                break;
            case Type.FLOAT:
                valueOf(mv, "java/lang/Float", "F");
                break;
            case Type.DOUBLE:
                valueOf(mv, "java/lang/Double", "D");
                break;
            default:
                // Already a reference.
                break;
        }
    }

    private static void valueOf(MethodVisitor mv, String boxType, String primitiveDescriptor) {
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, boxType, "valueOf",
                "(" + primitiveDescriptor + ")L" + boxType + ";");
    }

    private static void pushInt(MethodVisitor mv, int value) {
        if (value >= -1 && value <= 5) {
            mv.visitInsn(Opcodes.ICONST_0 + value);
        } else if (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE) {
            mv.visitIntInsn(Opcodes.BIPUSH, value);
        } else if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE) {
            mv.visitIntInsn(Opcodes.SIPUSH, value);
        } else {
            mv.visitLdcInsn(value);
        }
    }

    /**
     * A method seen during the visit that still needs its trampoline emitted.
     */
    private static final class HookedMethod {

        final int access;
        final String name;
        final String desc;
        final String signature;
        final String[] exceptions;

        HookedMethod(int access, String name, String desc, String signature, String[] exceptions) {
            this.access = access;
            this.name = name;
            this.desc = desc;
            this.signature = signature;
            this.exceptions = exceptions;
        }
    }
}
