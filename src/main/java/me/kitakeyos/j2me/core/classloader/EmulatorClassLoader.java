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
 * @version $Id: MIDletClassLoader.java 2052 2009-05-13 14:31:06Z barteo $
 */
package me.kitakeyos.j2me.core.classloader;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * Main features of this class loader Security aware - enables load and run app in Webstart. Proper class loading order.
 * MIDlet classes loaded first then system and MicroEmulator classes Proper resource loading order. MIDlet resources
 * only can be loaded. MIDlet Bytecode preprocessing/instrumentation
 *
 * @author vlads
 */
public class EmulatorClassLoader extends URLClassLoader {

    // TODO make this configurable

    public static boolean instrumentMIDletClasses = true;

    private final boolean delegatingToParent = false;

    private final boolean shouldSearchPathInParent = false;

    private final int instanceId;

    public EmulatorClassLoader(int instanceId, URL[] urls, ClassLoader parent) {
        super(urls, parent);
        this.instanceId = instanceId;
    }


    /**
     * Appends the Class Location URL to the list of URLs to search for classes and resources.
     */
    public void addClassURL(String className) throws MalformedURLException {
        String resource = getClassResourceName(className);
        URL url = getParent().getResource(resource);
        if (url == null) {
            url = this.getResource(resource);
        }
        if (url == null) {
            throw new MalformedURLException("Unable to find class " + className + " URL");
        }
        String path = url.toExternalForm();
        addURL(new URL(path.substring(0, path.length() - resource.length())));
    }

    static URL getClassURL(ClassLoader parent, String className) throws MalformedURLException {
        String resource = getClassResourceName(className);
        URL url = parent.getResource(resource);
        if (url == null) {
            throw new MalformedURLException("Unable to find class " + className + " URL");
        }
        String path = url.toExternalForm();
        return new URL(path.substring(0, path.length() - resource.length()));
    }

    /**
     * Loads the class with the specified <a href="#name">binary name</a>.
     *
     * <p>
     * Search order is reverse to standard implemenation
     * </p>
     * <p>
     * This implementation of this method searches for classes in the following order:
     *
     * <p>
     * <ol>
     *
     * <li>
     * <p>
     * Invoke {@link #findLoadedClass(String)} to check if the class has already been loaded.
     * </p>
     * </li>
     *
     * <li>
     * <p>
     * Invoke the {@link #findClass(String)} method to find the class in this class loader URLs.
     * </p>
     * </li>
     *
     * <li>
     * <p>
     * Invoke the {@link #loadClass(String) <tt>loadClass</tt>} method on the parent class loader. If the parent is
     * <tt>null</tt> the class loader built-in to the virtual machine is used, instead.
     * </p>
     * </li>
     *
     * </ol>
     */
    protected synchronized Class loadClass(String name, boolean resolve) throws ClassNotFoundException {
        // First, check if the class has already been loaded
        Class result = findLoadedClass(name);
        if (result == null) {
            try {
                result = findClass(name);
            } catch (ClassNotFoundException e) {
                // This will call our findClass again if Class is not found
                // in parent
                result = super.loadClass(name, false);
                if (result == null) {
                    throw new ClassNotFoundException(name);
                }
            }
        }
        if (resolve) {
            resolveClass(result);
        }
        return result;
    }

    /**
     * Finds the resource with the given name. A resource is some data (images, audio, text, etc) that can be accessed
     * by class code in a way that is independent of the location of the code.
     *
     * <p>
     * The name of a resource is a '<tt>/</tt>'-separated path name that identifies the resource.
     *
     * <p>
     * Search order is reverse to standard implementation
     * </p>
     *
     * <p>
     * This method will first use {@link #findResource(String)} to find the resource. That failing, this method will NOT
     * invoke the parent class loader if delegatingToParent=false.
     * </p>
     *
     * @param name The resource name
     * @return A <tt>URL</tt> object for reading the resource, or <tt>null</tt> if the resource could not be found or
     * the invoker doesn't have adequate privileges to get the resource.
     */

    public URL getResource(final String name) {
        URL url = findResource(name);
        if ((url == null) && delegatingToParent && (getParent() != null)) {
            url = getParent().getResource(name);
        }
        return url;
    }

    /**
     * Allow access to resources
     */
    public InputStream getResourceAsStream(String name) {
        final URL url = getResource(name);
        if (url == null) {
            return null;
        }

        try {
            return url.openStream();
        } catch (IOException e) {
            return null;
        }

    }

    public static String getClassResourceName(String className) {
        return className.replace('.', '/').concat(".class");
    }

    protected Class findClass(final String name) throws ClassNotFoundException {
        InputStream is;
        is = getResourceAsStream(getClassResourceName(name));

        // Relax ClassLoader behavior
        if ((is == null) && (this.shouldSearchPathInParent)) {
            boolean classFound;
            try {
                addClassURL(name);
                classFound = true;
            } catch (MalformedURLException e) {
                classFound = false;
            }
            if (classFound) {
                is = getResourceAsStream(getClassResourceName(name));
            }
        }

        if (is == null) {
            throw new ClassNotFoundException(name);
        }
        byte[] byteCode;
        int byteCodeLength;
        try {
            if (instrumentMIDletClasses) {
                byteCode = ClassPreprocessor.instrumentAndModifyBytecode(is, instanceId);
                byteCodeLength = byteCode.length;
            } else {
                final int chunkSize = 1024 * 2;
                // No class or data object must be bigger than 16 Kilobyte
                final int maxClassSizeSize = 1024 * 16;
                byteCode = new byte[chunkSize];
                byteCodeLength = 0;
                do {
                    int retrived;
                    try {
                        retrived = is.read(byteCode, byteCodeLength, byteCode.length - byteCodeLength);
                    } catch (IOException e) {
                        throw new ClassNotFoundException(name, e);
                    }
                    if (retrived == -1) {
                        break;
                    }
                    if (byteCode.length + chunkSize > maxClassSizeSize) {
                        throw new ClassNotFoundException(name, new ClassFormatError(
                                "Class object is bigger than 16 Kilobyte"));
                    }
                    byteCodeLength += retrived;
                    if (byteCode.length == byteCodeLength) {
                        byte[] newData = new byte[byteCode.length + chunkSize];
                        System.arraycopy(byteCode, 0, newData, 0, byteCode.length);
                        byteCode = newData;
                    } else if (byteCode.length < byteCodeLength) {
                        throw new ClassNotFoundException(name, new ClassFormatError("Internal read error"));
                    }
                } while (true);
            }
        } finally {
            try {
                is.close();
            } catch (IOException ignore) {
            }
        }
        return defineClass(name, byteCode, 0, byteCodeLength);
    }
}
