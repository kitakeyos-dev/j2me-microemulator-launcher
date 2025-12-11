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
 *  @version $Id: ClassPreprocessor.java 1605 2008-02-25 21:07:14Z barteo $
 */
package me.kitakeyos.j2me.infrastructure.classloader;

import me.kitakeyos.j2me.infrastructure.bytecode.InstrumentationClassVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author vlads
 *
 */
public class ClassPreprocessor {

	public static class InstrumentationResult {
		public final byte[] bytecode;
		public final boolean isModified;

		public InstrumentationResult(byte[] bytecode, boolean isModified) {
			this.bytecode = bytecode;
			this.isModified = isModified;
		}
	}

	/**
	 * Instrument bytecode without baking instanceId into the code.
	 * The instrumented code will call InstanceContext.getInstanceId() dynamically.
	 * This allows the same instrumented bytecode to be shared across multiple
	 * instances.
	 */
	public static InstrumentationResult instrumentAndModifyBytecode(final InputStream classInputStream,
			int instanceId) {
		try {
			// Read original bytecode
			byte[] originalBytes = readAllBytes(classInputStream);

			ClassReader cr = new ClassReader(originalBytes);
			ClassWriter cw = new ClassWriter(0);
			me.kitakeyos.j2me.infrastructure.bytecode.ModificationTracker tracker = new me.kitakeyos.j2me.infrastructure.bytecode.ModificationTracker();
			ClassVisitor cv = new InstrumentationClassVisitor(cw, instanceId, tracker);
			cr.accept(cv, 0);

			if (tracker.isModified()) {
				return new InstrumentationResult(cw.toByteArray(), true);
			} else {
				return new InstrumentationResult(originalBytes, false);
			}
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	private static byte[] readAllBytes(InputStream inputStream) throws IOException {
		java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
		int nRead;
		byte[] data = new byte[16384];
		while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
			buffer.write(data, 0, nRead);
		}
		return buffer.toByteArray();
	}

}
