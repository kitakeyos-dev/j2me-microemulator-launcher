/**
 *  MicroEmulator
 *  Copyright (C) 2006-2007 Bartek Teodorczyk <barteo@barteo.net>
 *  Copyright (C) 2006-2007 Vlad Skarzhevskyy
 *
 *  It is licensed under the following two licenses as alternatives:
 *    1. GNU Lesser General Public License (the "LGPL") version 2.1 or any newer version
 *    2. Apache License (the "AL") Version 2.0
 *
 *  You may not use this file except in compliance with at least one of
 *  the above two licenses.
 *
 *  You may obtain a copy of the LGPL at
 *      http://www.gnu.org/licenses/old-licenses/lgpl-2.1.txt
 *
 *  You may obtain a copy of the AL at
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the LGPL or the AL for the specific language governing permissions and
 *  limitations.
 *
 *  @version $Id: ChangeCallsClassVisitor.java 2092 2009-06-13 10:14:45Z barteo $
 */
package me.kitakeyos.j2me.infrastructure.bytecode;

import me.kitakeyos.j2me.infrastructure.thread.XThread;
import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.logging.Logger;

/**
 * @author vlads
 *
 */
public class InstrumentationClassVisitor extends ClassAdapter {

	private static final Logger logger = Logger.getLogger(InstrumentationClassVisitor.class.getName());

	private final int instanceId;
	private final String oldSuperclass;
	private final String newSuperclass;
	private boolean shouldChangeSuperCalls = false;

	public InstrumentationClassVisitor(ClassVisitor cv, int instanceId) {
		super(cv);
		this.instanceId = instanceId;
		this.oldSuperclass = ByteCodeHelper.toInternalName(Thread.class);
		this.newSuperclass = ByteCodeHelper.toInternalName(XThread.class);
	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		// Check if this class extends the old superclass
		if (superName.equals(this.oldSuperclass)) {
			shouldChangeSuperCalls = true;
			logger.info("Changing superclass:");
			logger.info("  Class: " + name);
			logger.info("  From: " + superName);
			logger.info("  To:   " + newSuperclass);

			// Change to new superclass
			superName = newSuperclass;
		}

		cv.visit(version, access, name, signature, superName, interfaces);
	}

	@Override
	public MethodVisitor visitMethod(final int access, final String name, final String desc, final String signature,
			final String[] exceptions) {

		MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

		// Wrap with SystemCallInterceptorMethodVisitor first
		mv = new SystemCallInterceptor(mv);

		// If we changed the superclass and this is a constructor, also redirect super()
		// calls
		if (shouldChangeSuperCalls && name.equals("<init>")) {
			mv = new SuperCallRedirector(mv, oldSuperclass, newSuperclass, instanceId);
		}

		return mv;
	}

	/**
	 * Method visitor to redirect super() constructor calls
	 */
	private static class SuperCallRedirector extends MethodAdapter {
		private final String oldSuperclass;
		private final String newSuperclass;
		private final int instanceId;

		public SuperCallRedirector(MethodVisitor mv, String oldSuperclass, String newSuperclass, int instanceId) {
			super(mv);
			this.oldSuperclass = oldSuperclass;
			this.newSuperclass = newSuperclass;
			this.instanceId = instanceId;
		}

		@Override
		public void visitMethodInsn(int opcode, String owner, String name, String desc) {
			// Redirect super() constructor calls from old superclass to new superclass
			if (opcode == Opcodes.INVOKESPECIAL &&
					owner.equals(oldSuperclass) &&
					name.equals("<init>")) {

				logger.info("  → Redirecting super() call: " + desc);

				// Push instanceId to stack
				mv.visitLdcInsn(instanceId);

				// Modify descriptor to accept int at the end
				// We need to insert 'I' before the closing parenthesis ')'
				int closingParenIndex = desc.lastIndexOf(')');
				String newDesc = desc.substring(0, closingParenIndex) + "I" + desc.substring(closingParenIndex);

				// Call new superclass constructor instead
				mv.visitMethodInsn(opcode, newSuperclass, name, newDesc);
				return;
			}

			// Pass through all other method calls
			super.visitMethodInsn(opcode, owner, name, desc);
		}
	}
}