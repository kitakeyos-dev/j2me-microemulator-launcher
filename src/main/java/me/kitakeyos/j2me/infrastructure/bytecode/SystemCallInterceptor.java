package me.kitakeyos.j2me.infrastructure.bytecode;

import me.kitakeyos.j2me.infrastructure.classloader.InstanceContext;
import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.logging.Logger;

public class SystemCallInterceptor extends MethodAdapter {

    private static final Logger logger = Logger.getLogger(SystemCallInterceptor.class.getName());

    private static final String INJECTED_CLASS = ByteCodeHelper.toInternalName(SystemCallHandler.class);
    private static final String INSTANCE_CONTEXT_CLASS = ByteCodeHelper.toInternalName(InstanceContext.class);

    // Track if we just saw NEW Socket
    private boolean foundNewSocket = false;

    public SystemCallInterceptor(MethodVisitor mv) {
        super(mv);
    }

    @Override
    public void visitTypeInsn(int opcode, String type) {
        // Detect NEW Socket
        if (opcode == Opcodes.NEW && type.equals("java/net/Socket")) {
            foundNewSocket = true;
            // Skip NEW instruction - don't pass it to next visitor
            return;
        }
        super.visitTypeInsn(opcode, type);
    }

    @Override
    public void visitInsn(int opcode) {
        // Detect DUP after NEW Socket
        if (foundNewSocket && opcode == Opcodes.DUP) {
            // Skip DUP instruction
            return;
        }
        super.visitInsn(opcode);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc) {
        // Intercept Socket constructor
        if (foundNewSocket &&
                opcode == Opcodes.INVOKESPECIAL &&
                owner.equals("java/net/Socket") &&
                name.equals("<init>") &&
                desc.equals("(Ljava/lang/String;I)V")) {

            logger.info("Intercepting Socket constructor: " + owner + "." + name + desc);

            // Current stack after skipping NEW and DUP: [host, port]
            // We need: [instanceId, host, port]

            // Get instance ID and insert it at the bottom of the stack
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, INSTANCE_CONTEXT_CLASS, "getInstanceId", "()I");
            // Stack: [host, port, instanceId]

            // Swap to get: [host, instanceId, port]
            mv.visitInsn(Opcodes.SWAP);
            // Stack: [host, instanceId, port]

            // Rotate to get: [instanceId, host, port]
            mv.visitInsn(Opcodes.DUP_X2);
            // Stack: [port, host, instanceId, port]

            mv.visitInsn(Opcodes.POP);
            // Stack: [port, host, instanceId]

            mv.visitInsn(Opcodes.DUP_X2);
            // Stack: [instanceId, port, host, instanceId]

            mv.visitInsn(Opcodes.POP);
            // Stack: [instanceId, port, host]

            mv.visitInsn(Opcodes.SWAP);
            // Stack: [instanceId, host, port]

            // Call static method
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, INJECTED_CLASS, "createSocket",
                    "(ILjava/lang/String;I)Ljava/net/Socket;");
            // Stack: [socket]

            foundNewSocket = false;
            return;
        }

        // Handle System.exit
        if (opcode == Opcodes.INVOKESTATIC) {
            if ((name.equals("exit")) && (owner.equals("java/lang/System"))) {
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, INSTANCE_CONTEXT_CLASS, "getInstanceId", "()I");
                mv.visitInsn(Opcodes.SWAP);
                mv.visitMethodInsn(opcode, INJECTED_CLASS, name, "(II)V");
                return;
            }

            if ((name.equals("initMEHomePath")) && (owner.equals("org/microemu/app/Config"))) {
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, INSTANCE_CONTEXT_CLASS, "getInstanceId", "()I");
                mv.visitMethodInsn(opcode, INJECTED_CLASS, name, "(I)Ljava/io/File;");
                return;
            }
        }

        mv.visitMethodInsn(opcode, owner, name, desc);
    }
}