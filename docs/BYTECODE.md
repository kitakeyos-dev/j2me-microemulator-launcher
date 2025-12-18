# Bytecode Manipulation (ASM)

> **Purpose**: This document explains the ASM bytecode manipulation system in extreme detail. Every class, method, and transformation is documented.

---

## 🎯 Overview

The bytecode manipulation system intercepts and modifies J2ME application classes at load time. This is necessary because:

1. **`System.exit()`** - Must close only the specific instance, not the entire JVM
2. **`new Socket()`** - Must be monitored, logged, and potentially redirected
3. **`Thread` subclasses** - Must be tracked for cleanup when instance shuts down

Without bytecode manipulation, multiple instances would interfere with each other.

---

## 📦 Components

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                     BYTECODE MANIPULATION COMPONENTS                         │
│                                                                              │
│  ┌────────────────────┐     ┌─────────────────────────────────────────────┐ │
│  │   ByteCodeHelper   │     │ InstrumentationClassVisitor                 │ │
│  │   (Utilities)      │     │ (ASM ClassVisitor)                          │ │
│  │                    │     │                                              │ │
│  │ • toInternalName() │     │ • Detects Thread subclasses                 │ │
│  │ • getDescriptor()  │     │ • Changes superclass Thread → XThread       │ │
│  │ • getMethodDesc()  │     │ • Creates SystemCallInterceptor per method  │ │
│  └────────────────────┘     └─────────────────────────────────────────────┘ │
│                                              │                               │
│                                              ▼                               │
│  ┌────────────────────┐     ┌─────────────────────────────────────────────┐ │
│  │ ModificationTracker│     │ SystemCallInterceptor                       │ │
│  │                    │     │ (ASM MethodVisitor)                         │ │
│  │ • isModified: bool │     │                                              │ │
│  │ • setModified()    │     │ • Intercepts System.exit()                  │ │
│  │                    │     │ • Intercepts new Socket(host, port)          │ │
│  └────────────────────┘     │ • Intercepts Config.initMEHomePath()         │ │
│                             └──────────────────────────────────────────────┘ │
│                                              │                               │
│                                              ▼                               │
│  ┌────────────────────────────────────────────────────────────────────────┐ │
│  │ SystemCallHandler                                                       │ │
│  │ (Runtime Handler - receives all intercepted calls)                      │ │
│  │                                                                         │ │
│  │ • exit(instanceId, status) → Gracefully shutdown specific instance     │ │
│  │ • createSocket(instanceId, host, port) → Create monitored socket       │ │
│  │ • initMEHomePath(instanceId) → Return per-instance data directory      │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 🔧 ByteCodeHelper

**Location**: `infrastructure/bytecode/ByteCodeHelper.java`

Utility class for bytecode-related conversions.

### Methods

```java
public class ByteCodeHelper {

    // === Name Conversions ===
    
    // java.lang.String → java/lang/String
    public static String toInternalName(Class<?> klass);
    public static String toInternalName(String className);
    
    // java/lang/String → java.lang.String
    public static String toClassName(String internalName);
    
    // java.lang.String → java/lang/String.class
    public static String getClassResourcePath(String className);
    
    // === Descriptors ===
    
    // String.class → Ljava/lang/String;
    // int.class → I
    // void.class → V
    public static String getDescriptor(Class<?> klass);
    
    // (String, int) → void gives "(Ljava/lang/String;I)V"
    public static String getMethodDescriptor(Class<?> returnType, Class<?>... paramTypes);
    
    // === Class Type Checks ===
    
    // Returns true for java/*, javax/*, sun/*, jdk/*
    public static boolean isJavaCoreClass(String internalName);
    
    // Returns true for javax/microedition/*, org/microemu/*
    public static boolean isMIDletClass(String internalName);
    
    // === Package/Class Name Extraction ===
    
    // java/lang/String → java/lang
    public static String getPackageName(String internalName);
    
    // java/lang/String → String
    public static String getSimpleClassName(String internalName);
}
```

### Descriptor Reference

| Java Type | Descriptor |
|-----------|------------|
| `void` | `V` |
| `boolean` | `Z` |
| `byte` | `B` |
| `char` | `C` |
| `short` | `S` |
| `int` | `I` |
| `long` | `J` |
| `float` | `F` |
| `double` | `D` |
| `String` | `Ljava/lang/String;` |
| `int[]` | `[I` |
| `Object[][]` | `[[Ljava/lang/Object;` |

---

## 🔧 InstrumentationClassVisitor

**Location**: `infrastructure/bytecode/InstrumentationClassVisitor.java`

ASM ClassVisitor that transforms class structure.

### Class Definition

```java
public class InstrumentationClassVisitor extends ClassAdapter {
    private static final Logger logger = Logger.getLogger(...);
    
    private final int instanceId;
    private final String oldSuperclass;      // "java/lang/Thread"
    private final String newSuperclass;      // XThread internal name
    private final ModificationTracker modificationTracker;
    private boolean shouldChangeSuperCalls = false;
    
    public InstrumentationClassVisitor(ClassVisitor cv, int instanceId, 
                                        ModificationTracker modificationTracker) {
        super(cv);
        this.instanceId = instanceId;
        this.modificationTracker = modificationTracker;
        this.oldSuperclass = ByteCodeHelper.toInternalName(Thread.class);
        this.newSuperclass = ByteCodeHelper.toInternalName(XThread.class);
    }
}
```

### Method: `visit()`

Called when visiting class header. Checks and changes superclass.

```java
@Override
public void visit(int version, int access, String name, String signature, 
                  String superName, String[] interfaces) {
    
    // Check if this class extends Thread
    if (superName.equals(this.oldSuperclass)) {
        shouldChangeSuperCalls = true;
        
        logger.info("Changing superclass:");
        logger.info("  Class: " + name);
        logger.info("  From: " + superName);
        logger.info("  To:   " + newSuperclass);

        // Change superclass from Thread to XThread
        superName = newSuperclass;
        modificationTracker.setModified(true);
    }

    cv.visit(version, access, name, signature, superName, interfaces);
}
```

### Method: `visitMethod()`

Called for each method in the class. Wraps with interceptors.

```java
@Override
public MethodVisitor visitMethod(int access, String name, String desc, 
                                  String signature, String[] exceptions) {
    
    MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

    // 1. ALWAYS wrap with SystemCallInterceptor
    mv = new SystemCallInterceptor(mv, instanceId, modificationTracker);

    // 2. If we changed superclass AND this is constructor, redirect super()
    if (shouldChangeSuperCalls && name.equals("<init>")) {
        mv = new SuperCallRedirector(mv, oldSuperclass, newSuperclass, instanceId);
    }

    return mv;
}
```

### Inner Class: `SuperCallRedirector`

Redirects `super()` calls in constructors when superclass was changed.

```java
private static class SuperCallRedirector extends MethodAdapter {
    private final String oldSuperclass;
    private final String newSuperclass;
    private final int instanceId;

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc) {
        // Detect: INVOKESPECIAL Thread.<init>(...)
        if (opcode == Opcodes.INVOKESPECIAL &&
            owner.equals(oldSuperclass) &&
            name.equals("<init>")) {

            logger.info("  → Redirecting super() call: " + desc);

            // Push instanceId to stack
            mv.visitLdcInsn(instanceId);

            // Modify descriptor: add 'I' (int) parameter at end
            // "(Ljava/lang/String;)V" → "(Ljava/lang/String;I)V"
            int closingParenIndex = desc.lastIndexOf(')');
            String newDesc = desc.substring(0, closingParenIndex) + "I" + 
                             desc.substring(closingParenIndex);

            // Call XThread constructor instead of Thread
            mv.visitMethodInsn(opcode, newSuperclass, name, newDesc);
            return;
        }

        // Pass through all other method calls
        super.visitMethodInsn(opcode, owner, name, desc);
    }
}
```

---

## 🔧 SystemCallInterceptor

**Location**: `infrastructure/bytecode/SystemCallInterceptor.java`

ASM MethodVisitor that intercepts specific method calls.

### Class Definition

```java
public class SystemCallInterceptor extends MethodAdapter {
    private static final Logger logger = Logger.getLogger(...);
    
    private final int instanceId;
    private final ModificationTracker modificationTracker;
    private static final String INJECTED_CLASS = 
        ByteCodeHelper.toInternalName(SystemCallHandler.class);

    // Track if we just saw NEW Socket
    private boolean foundNewSocket = false;
}
```

### Interception 1: `new Socket(host, port)`

This is the most complex interception because it needs to:
1. Remove `NEW` and `DUP` instructions
2. Reorder stack to add `instanceId`
3. Call static method instead of constructor

```java
@Override
public void visitTypeInsn(int opcode, String type) {
    // Detect: NEW java/net/Socket
    if (opcode == Opcodes.NEW && type.equals("java/net/Socket")) {
        foundNewSocket = true;
        // SKIP this instruction (don't pass to next visitor)
        return;
    }
    super.visitTypeInsn(opcode, type);
}

@Override
public void visitInsn(int opcode) {
    // Detect: DUP after NEW Socket
    if (foundNewSocket && opcode == Opcodes.DUP) {
        // SKIP this instruction
        return;
    }
    super.visitInsn(opcode);
}

@Override
public void visitMethodInsn(int opcode, String owner, String name, String desc) {
    // Detect: Socket.<init>(String, int)
    if (foundNewSocket &&
        opcode == Opcodes.INVOKESPECIAL &&
        owner.equals("java/net/Socket") &&
        name.equals("<init>") &&
        desc.equals("(Ljava/lang/String;I)V")) {

        logger.info("Intercepting Socket constructor: " + owner + "." + name + desc);

        // Stack manipulation to reorder [host, port] → [instanceId, host, port]
        // ... (detailed stack manipulation below)

        // Call static method instead
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, INJECTED_CLASS, "createSocket",
            "(ILjava/lang/String;I)Ljava/net/Socket;");

        foundNewSocket = false;
        modificationTracker.setModified(true);
        return;
    }
    
    // ... other interceptions
}
```

#### Stack Manipulation Detail

Original bytecode:
```
NEW java/net/Socket
DUP
ALOAD "host"
BIPUSH 8080
INVOKESPECIAL java/net/Socket.<init>(Ljava/lang/String;I)V
```

After interception (no NEW/DUP):
```
ALOAD "host"           ; Stack: [host]
BIPUSH 8080            ; Stack: [host, port]
LDC instanceId         ; Stack: [host, port, instanceId]
SWAP                   ; Stack: [host, instanceId, port]
DUP_X2                 ; Stack: [port, host, instanceId, port]
POP                    ; Stack: [port, host, instanceId]
DUP_X2                 ; Stack: [instanceId, port, host, instanceId]
POP                    ; Stack: [instanceId, port, host]
SWAP                   ; Stack: [instanceId, host, port]
INVOKESTATIC SystemCallHandler.createSocket(ILjava/lang/String;I)Ljava/net/Socket;
```

### Interception 2: `System.exit(int)`

```java
// Handle System.exit
if (opcode == Opcodes.INVOKESTATIC) {
    if (name.equals("exit") && owner.equals("java/lang/System")) {
        // Original: INVOKESTATIC System.exit(I)V
        // Stack before: [status]
        
        mv.visitLdcInsn(instanceId);  // Stack: [status, instanceId]
        mv.visitInsn(Opcodes.SWAP);    // Stack: [instanceId, status]
        mv.visitMethodInsn(opcode, INJECTED_CLASS, name, "(II)V");
        
        modificationTracker.setModified(true);
        return;
    }
}
```

### Interception 3: `Config.initMEHomePath()`

```java
if (name.equals("initMEHomePath") && owner.equals("org/microemu/app/Config")) {
    // Original: INVOKESTATIC Config.initMEHomePath()File
    // Stack before: []
    
    mv.visitLdcInsn(instanceId);  // Stack: [instanceId]
    mv.visitMethodInsn(opcode, INJECTED_CLASS, name, "(I)Ljava/io/File;");
    
    modificationTracker.setModified(true);
    return;
}
```

---

## 🔧 SystemCallHandler

**Location**: `infrastructure/bytecode/SystemCallHandler.java`

Runtime handler that receives all intercepted calls.

### Class Definition

```java
public final class SystemCallHandler implements Serializable {
    private static final long serialVersionUID = -1L;

    private SystemCallHandler() {} // No instantiation
}
```

### Method: `exit(int instanceId, int status)`

```java
public static void exit(int instanceId, int status) {
    EmulatorInstance emulatorInstance = 
        MainApplication.INSTANCE.emulatorInstanceManager.findInstance(instanceId);
    
    if (emulatorInstance != null) {
        // Run on Swing thread
        SwingUtilities.invokeLater(() -> {
            // Remove from UI
            MainApplication.INSTANCE.removeEmulatorInstanceTab(emulatorInstance);
            // Shutdown instance (stop threads, close sockets, etc.)
            emulatorInstance.shutdown();
        });
    }
}
```

### Method: `initMEHomePath(int instanceId)`

```java
public static File initMEHomePath(int instanceId) {
    // Returns: ./data/rms/<instanceId>/
    return Paths.get(
        ApplicationConfig.DATA_DIR,      // "data"
        ApplicationConfig.RMS_DIR,       // "rms"
        String.valueOf(instanceId)       // "1", "2", etc.
    ).toFile();
}
```

### Method: `createSocket(int instanceId, String host, int port)`

```java
public static Socket createSocket(int instanceId, String host, int port) 
        throws IOException {
    
    // 1. Delegate to NetworkService for redirection/proxy/logging
    Socket realSocket = NetworkService.getInstance()
        .createSocket(instanceId, host, port);

    // 2. Wrap with MonitoredSocket for packet capture
    Socket socket = new MonitoredSocket(realSocket, instanceId, host, port);

    // 3. Track socket in emulator instance for cleanup
    EmulatorInstance emulatorInstance = 
        MainApplication.INSTANCE.emulatorInstanceManager.findInstance(instanceId);
    if (emulatorInstance != null) {
        emulatorInstance.addSocket(socket);
    }
    
    return socket;
}
```

---

## 🔧 ModificationTracker

**Location**: `infrastructure/bytecode/ModificationTracker.java`

Simple flag to track if any modifications were made.

```java
public class ModificationTracker {
    private boolean modified = false;
    
    public boolean isModified() {
        return modified;
    }
    
    public void setModified(boolean modified) {
        this.modified = modified;
    }
}
```

**Why needed?**

If no modifications were made, the original bytecode can be cached and reused by all instances. This optimization is important for performance with many instances.

---

## 🔧 XThread

**Location**: `infrastructure/thread/XThread.java`

Custom Thread subclass that tracks which instance created it.

```java
public class XThread extends Thread {
    private final int instanceId;
    
    public XThread(int instanceId) {
        super();
        this.instanceId = instanceId;
        registerThread();
    }
    
    public XThread(Runnable target, int instanceId) {
        super(target);
        this.instanceId = instanceId;
        registerThread();
    }
    
    // ... other constructors with instanceId at the end
    
    private void registerThread() {
        EmulatorInstance instance = 
            MainApplication.INSTANCE.emulatorInstanceManager.findInstance(instanceId);
        if (instance != null) {
            instance.addThread(this);
        }
    }
    
    @Override
    public void run() {
        try {
            super.run();
        } finally {
            unregisterThread();
        }
    }
    
    private void unregisterThread() {
        EmulatorInstance instance = 
            MainApplication.INSTANCE.emulatorInstanceManager.findInstance(instanceId);
        if (instance != null) {
            instance.removeThread(this);
        }
    }
}
```

---

## 📊 Summary of Transformations

| Original Code | Transformed Code | Purpose |
|---------------|------------------|---------|
| `extends Thread` | `extends XThread` | Track threads per instance |
| `super()` in Thread subclass | `super(..., instanceId)` | Pass instanceId to XThread |
| `System.exit(status)` | `SystemCallHandler.exit(instanceId, status)` | Graceful instance shutdown |
| `new Socket(host, port)` | `SystemCallHandler.createSocket(instanceId, host, port)` | Network monitoring |
| `Config.initMEHomePath()` | `SystemCallHandler.initMEHomePath(instanceId)` | Per-instance data dir |

---

## 🔗 Related Documentation

- [CLASSLOADER.md](CLASSLOADER.md) - How classes are loaded and cached
- [NETWORK.md](NETWORK.md) - Network monitoring details
- [REFLECTION.md](REFLECTION.md) - Emulator initialization
