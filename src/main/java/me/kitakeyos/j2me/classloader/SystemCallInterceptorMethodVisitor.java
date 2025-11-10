/**
 * MicroEmulator
 * Copyright (C) 2006-2007 Bartek Teodorczyk <barteo@barteo.net>
 * Copyright (C) 2006-2007 Vlad Skarzhevskyy
 * <p>
 * It is licensed under the following two licenses as alternatives:
 * 1. GNU Lesser General Public License (the "LGPL") version 2.1 or any newer version
 * 2. Apache License (the "AL") Version 2.0
 * <p>
 * You may not use this file except in compliance with at least one of
 * the above two licenses.
 * <p>
 * You may obtain a copy of the LGPL at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.txt
 * <p>
 * You may obtain a copy of the AL at
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the LGPL or the AL for the specific language governing permissions and
 * limitations.
 *
 * @version $Id: ChangeCallsMethodVisitor.java 2092 2009-06-13 10:14:45Z barteo $
 */
package me.kitakeyos.j2me.classloader;

import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * @author vlads
 *
 */
public class SystemCallInterceptorMethodVisitor extends MethodAdapter implements Opcodes {

    private static final String INJECTED_CLASS = codeName(SystemCallHandler.class);
    private final int instanceId;

    public SystemCallInterceptorMethodVisitor(MethodVisitor mv, int instanceId) {
        super(mv);
        this.instanceId = instanceId;
    }

    public static String codeName(Class klass) {
        return klass.getName().replace('.', '/');
    }

    public void visitMethodInsn(int opcode, String owner, String name, String desc) {
        if (opcode == INVOKESTATIC) {
            // System.exit(int status) -> Injected.exit(instanceId, status)
            if ((name.equals("exit")) && (owner.equals("java/lang/System"))) {
                // Current stack: [status]
                // Push instanceId from field
                mv.visitLdcInsn(instanceId);
                // Stack: [status, instanceId]
                // Swap to: [instanceId, status]
                mv.visitInsn(SWAP);
                // Call Injected.exit(instanceId, status)
                mv.visitMethodInsn(opcode, INJECTED_CLASS, name, "(II)V");
                return;
            }

            // Config.initMEHomePath() -> Injected.initMEHomePath(instanceId)
            if ((name.equals("initMEHomePath")) && (owner.equals("org/microemu/app/Config"))) {
                // Push instanceId from field
                mv.visitLdcInsn(instanceId);
                // Call Injected.initMEHomePath(instanceId)
                mv.visitMethodInsn(opcode, INJECTED_CLASS, name, "(I)Ljava/io/File;");
                return;
            }
        }

        mv.visitMethodInsn(opcode, owner, name, desc);
    }
}