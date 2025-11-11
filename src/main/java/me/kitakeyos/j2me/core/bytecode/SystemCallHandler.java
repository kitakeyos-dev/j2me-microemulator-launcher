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
 *  @version $Id: Injected.java 1605 2008-02-25 21:07:14Z barteo $
 */
package me.kitakeyos.j2me.core.bytecode;


import me.kitakeyos.j2me.MainApplication;
import me.kitakeyos.j2me.config.ApplicationConfig;
import me.kitakeyos.j2me.model.EmulatorInstance;

import java.io.File;
import java.io.Serializable;
import java.nio.file.Paths;

/**
 * @author vlads
 *
 * This code is added to MIDlet application to solve problems with security policy  while running in Applet and Webstart.
 * Also solves resource loading paterns commonly used in MIDlet and not aceptable in Java SE application
 * The calls to this code is injected by ClassLoader or "Save for Web...".
 * <p>
 * This class is used instead injected one when application is running in Applet with MicroEmulator. 
 *
 * Serializable is just internal flag to verify tha proper class is loaded by application.
 */
@SuppressWarnings("unused")
public final class SystemCallHandler implements Serializable {

	private static final long serialVersionUID = -1L;
	
	/**
	 * We don't need to instantiate the class, all access is static
	 */
	private SystemCallHandler() {
		
	}
	
    public static void exit(int instanceId, int status) {
        EmulatorInstance emulatorInstance = MainApplication.INSTANCE.emulatorInstanceManager.findInstance(instanceId);
        if (emulatorInstance != null) {

            // Update UI and remove from running instances tab
            javax.swing.SwingUtilities.invokeLater(() -> {
                MainApplication.INSTANCE.removeEmulatorInstanceTab(emulatorInstance);
                MainApplication.INSTANCE.updateInstanceUI(emulatorInstance);
                // Shutdown the instance and release all resources
                emulatorInstance.shutdown();
            });
        }
    }

    public static File initMEHomePath(int instanceId) {
        return Paths.get(ApplicationConfig.DATA_DIR, ApplicationConfig.RMS_DIR, String.valueOf(instanceId)).toFile();
    }
}
