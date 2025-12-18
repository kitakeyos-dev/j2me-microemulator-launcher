# API Reference

> **Purpose**: Complete API reference for all key classes and interfaces.

---

## 📦 Domain Layer

### EmulatorInstance

Represents a running emulator instance.

**Package**: `me.kitakeyos.j2me.domain.emulator.model`

```java
public class EmulatorInstance {
    
    // === ENUMS ===
    
    public enum InstanceState {
        CREATED,    // Instance created, not started
        STARTING,   // Starting up
        RUNNING,    // Running normally
        STOPPED     // Shutdown
    }
    
    // === CONSTRUCTORS ===
    
    public EmulatorInstance(
        int instanceId,
        String microemulatorPath,
        String j2meFilePath,
        int displayWidth,
        int displayHeight,
        boolean fullDisplayMode
    );
    
    // === IDENTIFICATION ===
    
    int getInstanceId();
    
    // === CONFIGURATION ===
    
    String getMicroemulatorPath();
    String getJ2meFilePath();
    int getDisplayWidth();
    int getDisplayHeight();
    boolean isFullDisplayMode();
    
    // === STATE ===
    
    InstanceState getState();
    void setState(InstanceState state);
    boolean canRun();  // true if CREATED
    
    // === UI COMPONENTS ===
    
    JPanel getDevicePanel();
    void setDevicePanel(JPanel panel);
    
    JComponent getEmulatorDisplay();
    void setEmulatorDisplay(JComponent display);
    
    ActionListener getMenuExitListener();
    void setMenuExitListener(ActionListener listener);
    
    // === CLASSLOADERS ===
    
    EmulatorClassLoader getEmulatorClassLoader();
    void setEmulatorClassLoader(EmulatorClassLoader classLoader);
    
    ClassLoader getAppClassLoader();  // J2ME app ClassLoader
    void setAppClassLoader(ClassLoader classLoader);
    
    // === RESOURCE MANAGEMENT ===
    
    ResourceManager getResourceManager();
    
    void addThread(Thread thread);
    void removeThread(Thread thread);
    void addSocket(Socket socket);
    
    // === LIFECYCLE ===
    
    void shutdown();
}
```

---

### NetworkService

Singleton managing network rules and logging.

**Package**: `me.kitakeyos.j2me.domain.network.service`

```java
public class NetworkService {
    
    // === SINGLETON ===
    
    public static NetworkService getInstance();
    
    // === REDIRECTION RULES ===
    
    void addRedirectionRule(RedirectionRule rule);
    void removeRedirectionRule(RedirectionRule rule);
    List<RedirectionRule> getRedirectionRules();
    void clearRedirectionRules();
    
    // === PROXY RULES ===
    
    void addProxyRule(ProxyRule rule);
    void removeProxyRule(ProxyRule rule);
    List<ProxyRule> getProxyRules();
    void clearProxyRules();
    
    // === CONNECTION LOGGING ===
    
    List<ConnectionLog> getConnectionLogs();
    void clearConnectionLogs();
    
    // === PACKET DATA ACCESS ===
    
    byte[] getSentData(int socketId);
    byte[] getReceivedData(int socketId);
    
    long getTotalBytesSent();
    long getTotalBytesReceived();
    
    // === SOCKET CREATION (called by SystemCallHandler) ===
    
    Socket createSocket(int instanceId, String host, int port) throws IOException;
    
    // === PERSISTENCE ===
    
    void saveRules();
    void loadRules();
    
    // === INTERNAL (called by MonitoredInputStream/OutputStream) ===
    
    void logSentData(int socketId, byte[] data, int offset, int length);
    void logReceivedData(int socketId, byte[] data, int offset, int length);
}
```

---

### RedirectionRule

Model for network redirection.

**Package**: `me.kitakeyos.j2me.domain.network.model`

```java
public class RedirectionRule {
    
    public static final int ALL_INSTANCES = -1;
    
    // === CONSTRUCTORS ===
    
    public RedirectionRule(
        String originalHost,
        int originalPort,
        String targetHost,
        int targetPort,
        int instanceId  // -1 for all
    );
    
    // === GETTERS ===
    
    String getOriginalHost();
    int getOriginalPort();
    String getTargetHost();
    int getTargetPort();
    int getInstanceId();
    
    // === ENABLED STATE ===
    
    boolean isEnabled();
    void setEnabled(boolean enabled);
    
    // === MATCHING ===
    
    boolean matches(int instanceId, String host, int port);
}
```

---

### ProxyRule

Model for proxy configuration.

**Package**: `me.kitakeyos.j2me.domain.network.model`

```java
public class ProxyRule {
    
    public static final int ALL_INSTANCES = -1;
    
    public enum ProxyType { SOCKS, HTTP }
    
    // === CONSTRUCTORS ===
    
    public ProxyRule(
        ProxyType proxyType,
        String proxyHost,
        int proxyPort,
        int instanceId
    );
    
    public ProxyRule(
        ProxyType proxyType,
        String proxyHost,
        int proxyPort,
        int instanceId,
        String username,
        String password
    );
    
    // === GETTERS ===
    
    ProxyType getProxyType();
    String getProxyHost();
    int getProxyPort();
    int getInstanceId();
    String getUsername();
    String getPassword();
    
    // === STATE ===
    
    boolean isEnabled();
    void setEnabled(boolean enabled);
    boolean hasAuthentication();
    
    // === MATCHING ===
    
    boolean appliesTo(int instanceId);
    
    // === CONVERSION ===
    
    Proxy.Type getJavaProxyType();
}
```

---

### LuaScriptExecutor

Executes Lua scripts.

**Package**: `me.kitakeyos.j2me.domain.script.executor`

```java
public class LuaScriptExecutor {
    
    // === CONSTRUCTORS ===
    
    public LuaScriptExecutor(
        Path scriptsDirectory,
        Consumer<String> outputConsumer,
        Consumer<String> errorConsumer,
        Consumer<String> successConsumer,
        Consumer<String> infoConsumer
    );
    
    // === CLASSLOADER ===
    
    void setInstanceClassLoader(ClassLoader classLoader);
    
    // === EXECUTION ===
    
    void executeScript(Path scriptPath);
}
```

---

## 📦 Infrastructure Layer

### EmulatorClassLoader

Custom ClassLoader with bytecode transformation.

**Package**: `me.kitakeyos.j2me.infrastructure.classloader`

```java
public class EmulatorClassLoader extends URLClassLoader {
    
    // === CONSTRUCTORS ===
    
    public EmulatorClassLoader(int instanceId, URL[] urls, ClassLoader parent);
    
    // === IDENTIFICATION ===
    
    int getInstanceId();
    
    // === URL MANAGEMENT ===
    
    void addClassURL(String className) throws MalformedURLException;
    
    // === CLASS LOADING (overridden) ===
    
    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException;
    
    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException;
    
    // === RESOURCE ACCESS (overridden) ===
    
    @Override
    public URL getResource(String name);
    
    @Override
    public InputStream getResourceAsStream(String name);
}
```

---

### ClassPreprocessor

ASM bytecode transformation pipeline.

**Package**: `me.kitakeyos.j2me.infrastructure.classloader`

```java
public class ClassPreprocessor {
    
    // === RESULT CLASS ===
    
    public static class InstrumentationResult {
        public final byte[] bytecode;
        public final boolean isModified;
    }
    
    // === TRANSFORMATION ===
    
    public static InstrumentationResult instrumentAndModifyBytecode(
        InputStream classInputStream,
        int instanceId
    );
}
```

---

### SystemCallHandler

Receives intercepted system calls.

**Package**: `me.kitakeyos.j2me.infrastructure.bytecode`

```java
public final class SystemCallHandler {
    
    // === INTERCEPTED CALLS ===
    
    // Called instead of System.exit(status)
    public static void exit(int instanceId, int status);
    
    // Called instead of Config.initMEHomePath()
    public static File initMEHomePath(int instanceId);
    
    // Called instead of new Socket(host, port)
    public static Socket createSocket(int instanceId, String host, int port) 
        throws IOException;
}
```

---

### MonitoredSocket

Socket wrapper for packet capture.

**Package**: `me.kitakeyos.j2me.infrastructure.network`

```java
public class MonitoredSocket extends Socket {
    
    // === CONSTRUCTORS ===
    
    public MonitoredSocket(Socket wrapped, int instanceId, String host, int port);
    
    // === STREAM ACCESS (monitored) ===
    
    @Override
    public InputStream getInputStream() throws IOException;
    
    @Override
    public OutputStream getOutputStream() throws IOException;
    
    // All other methods delegate to wrapped socket
}
```

---

## 📦 Application Layer

### ApplicationConfig

Application settings.

**Package**: `me.kitakeyos.j2me.application.config`

```java
public class ApplicationConfig {
    
    // === CONSTANTS ===
    
    public static final String DATA_DIR = "data";
    public static final String APPS_DIR = "apps";
    public static final String ICONS_DIR = "icons";
    public static final String RMS_DIR = "rms";
    public static final String SCRIPTS_DIR = "scripts";
    
    // === CONSTRUCTORS ===
    
    public ApplicationConfig();  // Loads settings
    
    // === PATHS ===
    
    String getMicroemulatorPath();
    void setMicroemulatorPath(String path);
    
    // === DISPLAY ===
    
    int getDefaultDisplayWidth();
    int getDefaultDisplayHeight();
    
    // === FEATURES ===
    
    boolean isScriptTabEnabled();
    void setScriptTabEnabled(boolean enabled);
    
    // === PERSISTENCE ===
    
    void save();
    void load();
}
```

---

### EmulatorLauncher

Coordinates emulator startup.

**Package**: `me.kitakeyos.j2me.application.emulator`

```java
public class EmulatorLauncher {
    
    // === CLASSLOADER ===
    
    public static EmulatorClassLoader initializeEmulatorClassLoader(
        int instanceId,
        String microemulatorJarPath
    ) throws IOException;
    
    // === LAUNCH ===
    
    public static void startEmulatorInstance(
        EmulatorInstance instance,
        Runnable onComplete
    ) throws Exception;
    
    public static JFrame launchMicroEmulator(
        List<String> params,
        ClassLoader classLoader
    ) throws Exception;
}
```

---

## 📦 Utility Classes

### ReflectionHelper

Generic reflection operations.

**Package**: `me.kitakeyos.j2me.util.reflection`

```java
public class ReflectionHelper {
    
    // === FIELD ACCESS ===
    
    static <T> T getFieldValue(Object obj, String fieldName, Class<T> type);
    static Object getFieldValue(Object obj, String fieldName);
    static boolean setFieldValue(Object obj, String fieldName, Object value);
    static Object getStaticFieldValue(Class<?> clazz, String fieldName);
    static void setStaticFieldValue(Class<?> clazz, String fieldName, Object value);
    
    // === METHOD INVOCATION ===
    
    static Object invokeMethod(Object obj, String methodName);
    static Object invokeMethod(Object obj, String methodName, 
                               Class<?>[] paramTypes, Object... args);
    static Object invokeDeclaredMethod(Object obj, String methodName);
    static Object invokeDeclaredMethod(Object obj, String methodName,
                                       Class<?>[] paramTypes, Object... args);
    static Object invokeStaticMethod(Class<?> clazz, String methodName,
                                     Class<?>[] paramTypes, Object... args);
    
    // === CLASS LOADING ===
    
    static Class<?> loadClass(ClassLoader classLoader, String className);
    static Object createInstance(ClassLoader classLoader, String className);
}
```

---

### ByteCodeHelper

Bytecode utilities.

**Package**: `me.kitakeyos.j2me.infrastructure.bytecode`

```java
public class ByteCodeHelper {
    
    // === NAME CONVERSIONS ===
    
    static String toInternalName(Class<?> klass);    // java.lang.String → java/lang/String
    static String toInternalName(String className);
    static String toClassName(String internalName);  // java/lang/String → java.lang.String
    static String getClassResourcePath(String className);  // → java/lang/String.class
    
    // === DESCRIPTORS ===
    
    static String getDescriptor(Class<?> klass);  // String.class → Ljava/lang/String;
    static String getMethodDescriptor(Class<?> returnType, Class<?>... paramTypes);
    
    // === TYPE CHECKS ===
    
    static boolean isJavaCoreClass(String internalName);
    static boolean isMIDletClass(String internalName);
    
    // === NAME EXTRACTION ===
    
    static String getPackageName(String internalName);
    static String getSimpleClassName(String internalName);
}
```

---

## 🔗 Related Documentation

- [ARCHITECTURE.md](ARCHITECTURE.md) - Architecture overview
- [CLASSLOADER.md](CLASSLOADER.md) - ClassLoader details
- [BYTECODE.md](BYTECODE.md) - Bytecode manipulation
- [NETWORK.md](NETWORK.md) - Network system
