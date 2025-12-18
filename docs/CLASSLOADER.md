# ClassLoader System

> **Purpose**: This document explains the custom ClassLoader system that enables multi-instance emulation with bytecode modification.

---

## 🎯 Overview

The ClassLoader system is the foundation of the launcher. It enables:

1. **Multi-instance isolation** - Each emulator instance has its own ClassLoader
2. **Bytecode modification** - Classes are transformed before loading
3. **Reverse delegation** - MIDlet classes are loaded before system classes
4. **Shared caching** - Unmodified bytecode is cached across instances

---

## 📦 Components

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         CLASS LOADING PIPELINE                               │
│                                                                              │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐    ┌────────────┐ │
│  │ MIDlet JAR   │───▶│ InputStream  │───▶│ClassPreproc. │───▶│  ASM       │ │
│  │ (J2ME App)   │    │ (raw bytes)  │    │(orchestrator)│    │(transform) │ │
│  └──────────────┘    └──────────────┘    └──────────────┘    └─────┬──────┘ │
│                                                                     │        │
│                                          ┌──────────────────────────┘        │
│                                          ▼                                   │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐                   │
│  │ Loaded Class │◀───│ defineClass  │◀───│Transformed   │                   │
│  │  (ready)     │    │ (JVM)        │    │  bytecode    │                   │
│  └──────────────┘    └──────────────┘    └──────────────┘                   │
│                                                 │                            │
│                                                 ▼                            │
│                                          ┌──────────────┐                   │
│                                          │Shared Cache  │ (if not modified) │
│                                          │(ConcurrentMap)│                   │
│                                          └──────────────┘                   │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 🔧 EmulatorClassLoader

**Location**: `infrastructure/classloader/EmulatorClassLoader.java`

### Class Definition

```java
public class EmulatorClassLoader extends URLClassLoader {
    // Shared cache for UNMODIFIED bytecode (thread-safe)
    private static final Map<String, byte[]> sharedBytecodeCache = new ConcurrentHashMap<>();
    
    // Instance ID for this ClassLoader
    private final int instanceId;
    
    public EmulatorClassLoader(int instanceId, URL[] urls, ClassLoader parent) {
        super(urls, parent);
        this.instanceId = instanceId;
    }
}
```

### Why Extend URLClassLoader?

- URLClassLoader can load classes from JAR files via URLs
- We need to load from `microemulator.jar`
- Custom class loading logic can be added via overriding

### Key Methods

#### `loadClass(String name, boolean resolve)`

This method implements **reverse delegation**:

```java
@Override
protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
    // 1. Check if already loaded
    Class<?> loadedClass = findLoadedClass(name);

    if (loadedClass == null) {
        try {
            // 2. TRY OUR CLASSLOADER FIRST (reverse delegation!)
            // This is different from standard Java which delegates to parent first
            loadedClass = findClass(name);
        } catch (ClassNotFoundException e) {
            // 3. Not found in MIDlet JAR, delegate to parent
            loadedClass = super.loadClass(name, false);
        }
    }

    if (resolve) {
        resolveClass(loadedClass);
    }

    return loadedClass;
}
```

**Standard delegation**: Parent → Child  
**Reverse delegation**: Child → Parent

This ensures MIDlet classes are loaded from the J2ME JAR, not from system.

#### `findClass(String name)`

This method loads and transforms bytecode:

```java
@Override
protected Class<?> findClass(String name) throws ClassNotFoundException {
    // 1. Try shared cache first (for unmodified classes)
    byte[] cachedBytes = sharedBytecodeCache.get(name);
    if (cachedBytes != null) {
        return defineClass(name, cachedBytes, 0, cachedBytes.length);
    }

    // 2. Read raw bytecode from JAR
    String resourcePath = ByteCodeHelper.getClassResourcePath(name);
    InputStream is = getResourceAsStream(resourcePath);

    if (is == null) {
        throw new ClassNotFoundException(name);
    }

    try {
        // 3. Transform bytecode via ClassPreprocessor
        ClassPreprocessor.InstrumentationResult result = 
            ClassPreprocessor.instrumentAndModifyBytecode(is, instanceId);

        // 4. Cache unmodified bytecode for other instances
        if (!result.isModified) {
            sharedBytecodeCache.put(name, result.bytecode);
        }

        // 5. Define class from transformed bytecode
        return defineClass(name, result.bytecode, 0, result.bytecode.length);
    } finally {
        closeQuietly(is);
    }
}
```

### Caching Strategy

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           CACHING LOGIC                                      │
│                                                                              │
│   Class loaded first time                                                    │
│          │                                                                   │
│          ▼                                                                   │
│   ┌──────────────┐                                                          │
│   │ Transform    │                                                          │
│   │ bytecode     │                                                          │
│   └──────┬───────┘                                                          │
│          │                                                                   │
│          ▼                                                                   │
│   ┌──────────────────────────────┐                                          │
│   │ Was bytecode modified?       │                                          │
│   │ (Thread→XThread, Socket, etc)│                                          │
│   └──────────┬───────────────────┘                                          │
│              │                                                               │
│     ┌────────┴────────┐                                                     │
│     ▼                 ▼                                                     │
│   [YES]             [NO]                                                    │
│     │                 │                                                     │
│     ▼                 ▼                                                     │
│   DO NOT cache    CACHE in sharedBytecodeCache                              │
│   (instanceId is  (can be reused by all instances)                          │
│   baked in)                                                                  │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

**Why not cache modified bytecode?**

Modified bytecode contains the `instanceId` baked into the code:
```java
// Transformed code contains literal instanceId
mv.visitLdcInsn(instanceId);  // e.g., 1, 2, 3...
```

So each instance needs its own transformed bytecode.

---

## 🔧 ClassPreprocessor

**Location**: `infrastructure/classloader/ClassPreprocessor.java`

### Purpose

Orchestrates the ASM transformation pipeline.

### Class Definition

```java
public class ClassPreprocessor {
    
    public static class InstrumentationResult {
        public final byte[] bytecode;
        public final boolean isModified;  // True if bytecode was changed
        
        public InstrumentationResult(byte[] bytecode, boolean isModified) {
            this.bytecode = bytecode;
            this.isModified = isModified;
        }
    }
    
    public static InstrumentationResult instrumentAndModifyBytecode(
            InputStream classInputStream, int instanceId) {
        // ...
    }
}
```

### Transformation Pipeline

```java
public static InstrumentationResult instrumentAndModifyBytecode(
        InputStream classInputStream, int instanceId) {
    try {
        // 1. Read original bytecode into byte array
        byte[] originalBytes = readAllBytes(classInputStream);

        // 2. Create ASM ClassReader to parse bytecode
        ClassReader cr = new ClassReader(originalBytes);
        
        // 3. Create ASM ClassWriter to output transformed bytecode
        ClassWriter cw = new ClassWriter(0);
        
        // 4. Create tracker to know if modifications were made
        ModificationTracker tracker = new ModificationTracker();
        
        // 5. Create our custom ClassVisitor that does the transformations
        ClassVisitor cv = new InstrumentationClassVisitor(cw, instanceId, tracker);
        
        // 6. Run the transformation
        cr.accept(cv, 0);

        // 7. Return result with modification flag
        if (tracker.isModified()) {
            return new InstrumentationResult(cw.toByteArray(), true);
        } else {
            // Return original bytes (no changes needed)
            return new InstrumentationResult(originalBytes, false);
        }
    } catch (IOException e) {
        return null;
    }
}
```

### ASM Pipeline Visualization

```
          ┌───────────────────────────────────────────────────────────────────┐
          │                    ASM TRANSFORMATION PIPELINE                     │
          │                                                                    │
          │  ┌────────────┐    ┌────────────────────┐    ┌────────────┐       │
          │  │ClassReader │───▶│InstrumentationClass│───▶│ClassWriter │       │
          │  │            │    │      Visitor       │    │            │       │
          │  │ (parses    │    │                    │    │ (outputs   │       │
          │  │  bytecode) │    │  ┌──────────────┐  │    │  bytecode) │       │
          │  │            │    │  │SystemCall    │  │    │            │       │
          │  │            │    │  │Interceptor   │  │    │            │       │
          │  │            │    │  │(per method)  │  │    │            │       │
          │  │            │    │  └──────────────┘  │    │            │       │
          │  └────────────┘    └────────────────────┘    └────────────┘       │
          │                                                    │              │
          │                                                    ▼              │
          │                                           ┌────────────────┐      │
          │                                           │ byte[] result  │      │
          │                                           │ (transformed)  │      │
          │                                           └────────────────┘      │
          └───────────────────────────────────────────────────────────────────┘
```

---

## 🔄 Complete Class Loading Sequence

```
┌─────────────────────────────────────────────────────────────────────────────┐
│ SEQUENCE: Loading "com.game.MainMIDlet" for Instance #1                     │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  1. EmulatorClassLoader.loadClass("com.game.MainMIDlet")                    │
│     │                                                                        │
│     ▼                                                                        │
│  2. findLoadedClass("com.game.MainMIDlet") → null (not loaded yet)          │
│     │                                                                        │
│     ▼                                                                        │
│  3. findClass("com.game.MainMIDlet")                                        │
│     │                                                                        │
│     ├── Check sharedBytecodeCache → miss                                    │
│     │                                                                        │
│     ├── getResourceAsStream("com/game/MainMIDlet.class")                    │
│     │   │                                                                    │
│     │   └── Read from MIDlet JAR                                            │
│     │                                                                        │
│     ├── ClassPreprocessor.instrumentAndModifyBytecode(stream, 1)            │
│     │   │                                                                    │
│     │   ├── ClassReader parses bytecode                                     │
│     │   │                                                                    │
│     │   ├── InstrumentationClassVisitor visits each element:               │
│     │   │   │                                                                │
│     │   │   ├── visit() - Check if extends Thread                          │
│     │   │   │             If yes: change to XThread, set modified=true     │
│     │   │   │                                                                │
│     │   │   └── visitMethod() for each method:                              │
│     │   │       │                                                            │
│     │   │       └── SystemCallInterceptor.visitMethodInsn()                 │
│     │   │           │                                                        │
│     │   │           ├── System.exit() → redirect to SystemCallHandler      │
│     │   │           ├── new Socket() → redirect to SystemCallHandler       │
│     │   │           └── other calls → pass through unchanged               │
│     │   │                                                                    │
│     │   └── Return InstrumentationResult(bytecode, isModified)             │
│     │                                                                        │
│     ├── If not modified: cache in sharedBytecodeCache                       │
│     │                                                                        │
│     └── defineClass("com.game.MainMIDlet", bytecode, ...)                   │
│                                                                              │
│  4. Return loaded Class object                                               │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 📊 Instance Isolation

Each instance has its own ClassLoader, creating class isolation:

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          INSTANCE ISOLATION                                  │
│                                                                              │
│  ┌───────────────────────────────────┐  ┌───────────────────────────────────┐
│  │         INSTANCE #1               │  │         INSTANCE #2               │
│  │                                   │  │                                   │
│  │  ┌─────────────────────────────┐  │  │  ┌─────────────────────────────┐  │
│  │  │ EmulatorClassLoader #1      │  │  │  │ EmulatorClassLoader #2      │  │
│  │  │ instanceId = 1              │  │  │  │ instanceId = 2              │  │
│  │  └─────────────────────────────┘  │  │  └─────────────────────────────┘  │
│  │              │                    │  │              │                    │
│  │              ▼                    │  │              ▼                    │
│  │  ┌─────────────────────────────┐  │  │  ┌─────────────────────────────┐  │
│  │  │ com.game.MainMIDlet        │  │  │  │ com.game.MainMIDlet        │  │
│  │  │ (transformed for ID=1)      │  │  │  │ (transformed for ID=2)      │  │
│  │  └─────────────────────────────┘  │  │  └─────────────────────────────┘  │
│  │              │                    │  │              │                    │
│  │              ▼                    │  │              ▼                    │
│  │  ┌─────────────────────────────┐  │  │  ┌─────────────────────────────┐  │
│  │  │ Static fields are SEPARATE │  │  │  │ Static fields are SEPARATE │  │
│  │  │ for each instance!         │  │  │  │ for each instance!         │  │
│  │  └─────────────────────────────┘  │  │  └─────────────────────────────┘  │
│  │                                   │  │                                   │
│  └───────────────────────────────────┘  └───────────────────────────────────┘
│                                                                              │
│                    ▼                              ▼                          │
│           ┌─────────────────────────────────────────────────────┐           │
│           │              SHARED BYTECODE CACHE                   │           │
│           │  (Only for UNMODIFIED classes)                       │           │
│           │                                                      │           │
│           │  java/util/ArrayList.class → byte[]                  │           │
│           │  org/json/JSONObject.class → byte[]                  │           │
│           │  ... (classes with no Thread/Socket/exit usage)      │           │
│           └─────────────────────────────────────────────────────┘           │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## ⚠️ Important Notes

### 1. ClassLoader Cleanup

When an instance shuts down:
```java
// In InstanceLifecycleManager
emulatorClassLoader.close();  // Release resources
emulatorClassLoader = null;   // Allow GC
```

### 2. Thread Context ClassLoader

Before running emulator code:
```java
Thread.currentThread().setContextClassLoader(emulatorClassLoader);
```

This ensures classes loaded via `Class.forName()` use the correct ClassLoader.

### 3. Memory Considerations

- Shared cache grows with number of unique classes loaded
- Modified bytecode is NOT cached → each instance uses separate memory
- ClassLoader closure releases instance-specific classes

---

## 🔗 Related Documentation

- [BYTECODE.md](BYTECODE.md) - What transformations are applied
- [REFLECTION.md](REFLECTION.md) - How emulator is initialized via reflection
- [ARCHITECTURE.md](ARCHITECTURE.md) - Overall architecture
