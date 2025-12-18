# Reflection (Emulator Instance Initialization)

> **Purpose**: This document explains how reflection is used to launch and configure MicroEmulator instances at runtime.

---

## 🎯 Overview

Reflection is required because:

1. **MicroEmulator is loaded at runtime** via custom ClassLoader
2. **Cannot use direct imports** - compilation would fail
3. **Need to access private fields** - internal configuration
4. **Need to call internal methods** - initialization sequence

---

## 📦 Components

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                       REFLECTION SYSTEM                                      │
│                                                                              │
│  ┌────────────────────────────────────────────────────────────────────────┐ │
│  │                    UTILITY CLASSES                                      │ │
│  │                                                                         │ │
│  │  ┌─────────────────────┐     ┌─────────────────────────────────────┐   │ │
│  │  │   ReflectionHelper  │     │   EmulatorReflectionHelper          │   │ │
│  │  │   (Generic)         │     │   (MicroEmulator-specific)          │   │ │
│  │  │                     │     │                                      │   │ │
│  │  │ • getFieldValue()   │     │ • initializeEmulatorParams()        │   │ │
│  │  │ • setFieldValue()   │     │ • configureDisplaySize()             │   │ │
│  │  │ • invokeMethod()    │     │ • initializeMIDlet()                 │   │ │
│  │  │ • loadClass()       │     │ • setupComponentListener()           │   │ │
│  │  │ • createInstance()  │     │ • notifyStateChanged()               │   │ │
│  │  └─────────────────────┘     └─────────────────────────────────────┘   │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
│                                                                              │
│  ┌────────────────────────────────────────────────────────────────────────┐ │
│  │                    CALLING CODE                                         │ │
│  │                                                                         │ │
│  │  EmulatorLauncher.launchMicroEmulator()                                │ │
│  │      │                                                                  │ │
│  │      ├── ReflectionHelper.createInstance("org.microemu.app.Main")      │ │
│  │      ├── EmulatorReflectionHelper.initializeEmulatorParams(...)        │ │
│  │      ├── EmulatorReflectionHelper.configureDisplaySize(...)            │ │
│  │      └── EmulatorReflectionHelper.initializeMIDlet(...)                │ │
│  │                                                                         │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 🔧 ReflectionHelper

**Location**: `util/reflection/ReflectionHelper.java`

Generic reflection utility class.

### Field Operations

```java
// Get field value with type casting
public static <T> T getFieldValue(Object obj, String fieldName, Class<T> type) 
        throws NoSuchFieldException, IllegalAccessException {
    Field field = obj.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);  // Access private fields
    return type.cast(field.get(obj));
}

// Get field value as Object
public static Object getFieldValue(Object obj, String fieldName) 
        throws NoSuchFieldException, IllegalAccessException {
    Field field = obj.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    return field.get(obj);
}

// Set field value
public static boolean setFieldValue(Object obj, String fieldName, Object value) 
        throws NoSuchFieldException, IllegalAccessException {
    Field field = obj.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(obj, value);
    return true;
}

// Get static field value
public static Object getStaticFieldValue(Class<?> clazz, String fieldName) 
        throws Exception {
    Field field = clazz.getDeclaredField(fieldName);
    field.setAccessible(true);
    return field.get(null);  // null for static fields
}

// Set static field value
public static void setStaticFieldValue(Class<?> clazz, String fieldName, Object value) 
        throws Exception {
    Field field = clazz.getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(null, value);
}
```

### Method Invocation

```java
// Invoke public method without parameters
public static Object invokeMethod(Object obj, String methodName) 
        throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    Method method = obj.getClass().getMethod(methodName);
    return method.invoke(obj);
}

// Invoke public method with parameters
public static Object invokeMethod(Object obj, String methodName, 
                                   Class<?>[] paramTypes, Object... args) 
        throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    Method method = obj.getClass().getMethod(methodName, paramTypes);
    return method.invoke(obj, args);
}

// Invoke private/declared method without parameters
public static Object invokeDeclaredMethod(Object obj, String methodName) 
        throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    Method method = obj.getClass().getDeclaredMethod(methodName);
    method.setAccessible(true);  // Access private methods
    return method.invoke(obj);
}

// Invoke private/declared method with parameters
public static Object invokeDeclaredMethod(Object obj, String methodName, 
                                          Class<?>[] paramTypes, Object... args) 
        throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    Method method = obj.getClass().getDeclaredMethod(methodName, paramTypes);
    method.setAccessible(true);
    return method.invoke(obj, args);
}

// Invoke static method
public static Object invokeStaticMethod(Class<?> clazz, String methodName, 
                                        Class<?>[] paramTypes, Object... args) 
        throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    Method method = clazz.getMethod(methodName, paramTypes);
    return method.invoke(null, args);  // null for static methods
}
```

### Class Loading

```java
// Load class from custom ClassLoader
public static Class<?> loadClass(ClassLoader classLoader, String className) 
        throws ClassNotFoundException {
    return classLoader.loadClass(className);
}

// Create instance using no-arg constructor
public static Object createInstance(ClassLoader classLoader, String className) 
        throws ClassNotFoundException, NoSuchMethodException, 
               InvocationTargetException, InstantiationException, IllegalAccessException {
    Class<?> clazz = classLoader.loadClass(className);
    return clazz.getDeclaredConstructor().newInstance();
}
```

---

## 🔧 EmulatorReflectionHelper

**Location**: `util/reflection/EmulatorReflectionHelper.java`

MicroEmulator-specific reflection operations.

### initializeEmulatorParams()

Initializes emulator with launch parameters.

```java
public static Object initializeEmulatorParams(JFrame app, List<String> params, 
                                               ClassLoader classLoader)
        throws ClassNotFoundException, NoSuchFieldException, NoSuchMethodException,
               InvocationTargetException, IllegalAccessException {

    // 1. Get internal fields from Main
    Object common = ReflectionHelper.getFieldValue(app, "common");
    Object selectDevicePanel = ReflectionHelper.getFieldValue(app, "selectDevicePanel");

    // 2. Get selected device entry
    Object deviceEntry = ReflectionHelper.invokeMethod(selectDevicePanel, "getSelectedDeviceEntry");

    // 3. Load J2SEDevice class
    Class<?> j2seDeviceClass = ReflectionHelper.loadClass(classLoader, 
        "org.microemu.device.j2se.J2SEDevice");

    // 4. Call common.initParams(params, deviceEntry, J2SEDevice.class)
    boolean initResult = (boolean) ReflectionHelper.invokeMethod(
        common,
        "initParams",
        new Class<?>[] { List.class, deviceEntry.getClass(), Class.class },
        params, deviceEntry, j2seDeviceClass
    );

    // 5. Set deviceEntry field if successful
    if (initResult) {
        ReflectionHelper.setFieldValue(app, "deviceEntry", deviceEntry);
    }

    return deviceEntry;
}
```

### configureDisplaySize()

Configures display dimensions for resizable devices.

```java
public static void configureDisplaySize(Object deviceEntry, ClassLoader classLoader)
        throws ClassNotFoundException, NoSuchMethodException, 
               InvocationTargetException, IllegalAccessException {

    // 1. Get DeviceFactory and current device
    Class<?> deviceFactoryClass = ReflectionHelper.loadClass(classLoader, 
        "org.microemu.device.DeviceFactory");
    Object device = ReflectionHelper.invokeStaticMethod(deviceFactoryClass, 
        "getDevice", new Class<?>[] {});
    
    // 2. Get device display
    Object deviceDisplay = ReflectionHelper.invokeMethod(device, "getDeviceDisplay");

    // 3. Check if resizable
    boolean isResizable = (boolean) ReflectionHelper.invokeMethod(deviceDisplay, "isResizable");

    if (isResizable) {
        // 4. Get device entry display size from Config
        Class<?> configClass = ReflectionHelper.loadClass(classLoader, 
            "org.microemu.app.Config");
        Object size = ReflectionHelper.invokeStaticMethod(
            configClass,
            "getDeviceEntryDisplaySize",
            new Class<?>[] { deviceEntry.getClass() },
            deviceEntry
        );

        // 5. Set display rectangle
        if (size != null) {
            ReflectionHelper.invokeMethod(
                deviceDisplay,
                "setDisplayRectangle",
                new Class<?>[] { Rectangle.class },
                size
            );
        }
    }
}
```

### initializeMIDlet()

Starts the MIDlet.

```java
public static void initializeMIDlet(Object common)
        throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    // Call common.initMIDlet(true)
    ReflectionHelper.invokeMethod(common, "initMIDlet", 
        new Class<?>[] { boolean.class }, true);
}
```

### setupComponentListener()

Sets up resize handling.

```java
public static void setupComponentListener(JFrame app)
        throws NoSuchFieldException, NoSuchMethodException, 
               InvocationTargetException, IllegalAccessException {
    Object componentListener = ReflectionHelper.getFieldValue(app, "componentListener");
    ReflectionHelper.invokeMethod(
        app,
        "addComponentListener",
        new Class<?>[] { ComponentListener.class },
        componentListener
    );
}
```

### notifyStateChanged()

Notifies that emulator state has changed.

```java
public static void notifyStateChanged(JFrame app)
        throws NoSuchFieldException, NoSuchMethodException, 
               InvocationTargetException, IllegalAccessException {
    Object responseInterfaceListener = ReflectionHelper.getFieldValue(app, 
        "responseInterfaceListener");
    ReflectionHelper.invokeDeclaredMethod(
        responseInterfaceListener,
        "stateChanged",
        new Class<?>[] { boolean.class },
        true
    );
}
```

### updateDevice()

Updates device after configuration.

```java
public static void updateDevice(JFrame app)
        throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    ReflectionHelper.invokeDeclaredMethod(app, "updateDevice");
}
```

---

## 🔄 Complete Launch Sequence

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                   EMULATOR LAUNCH VIA REFLECTION                             │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  EmulatorLauncher.launchMicroEmulator(params, classLoader)                  │
│      │                                                                       │
│      │                                                                       │
│      ▼                                                                       │
│  STEP 1: Create Main instance                                               │
│  ─────────────────────────────                                              │
│  JFrame app = (JFrame) ReflectionHelper.createInstance(                     │
│      classLoader, "org.microemu.app.Main");                                 │
│                                                                              │
│  // Under the hood:                                                          │
│  // Class<?> clazz = classLoader.loadClass("org.microemu.app.Main");        │
│  // return clazz.getDeclaredConstructor().newInstance();                    │
│      │                                                                       │
│      ▼                                                                       │
│  STEP 2: Initialize parameters                                              │
│  ────────────────────────────                                               │
│  Object deviceEntry = EmulatorReflectionHelper.initializeEmulatorParams(    │
│      app, params, classLoader);                                             │
│                                                                              │
│  // Under the hood:                                                          │
│  // - Get app.common field                                                  │
│  // - Get app.selectDevicePanel field                                       │
│  // - Call selectDevicePanel.getSelectedDeviceEntry()                       │
│  // - Call common.initParams(params, deviceEntry, J2SEDevice.class)         │
│  // - Set app.deviceEntry = deviceEntry                                     │
│      │                                                                       │
│      ▼                                                                       │
│  STEP 3: Configure display size                                             │
│  ──────────────────────────────                                             │
│  EmulatorReflectionHelper.configureDisplaySize(deviceEntry, classLoader);   │
│                                                                              │
│  // Under the hood:                                                          │
│  // - Get device from DeviceFactory.getDevice()                             │
│  // - Get deviceDisplay from device.getDeviceDisplay()                      │
│  // - Check if deviceDisplay.isResizable()                                  │
│  // - If yes, get size from Config.getDeviceEntryDisplaySize()              │
│  // - Call deviceDisplay.setDisplayRectangle(size)                          │
│      │                                                                       │
│      ▼                                                                       │
│  STEP 4: Update device                                                      │
│  ─────────────────────                                                      │
│  EmulatorReflectionHelper.updateDevice(app);                                │
│                                                                              │
│  // Under the hood:                                                          │
│  // - Call app.updateDevice() (private method)                              │
│      │                                                                       │
│      ▼                                                                       │
│  STEP 5: Validate frame                                                     │
│  ──────────────────────                                                     │
│  app.validate();                                                            │
│      │                                                                       │
│      ▼                                                                       │
│  STEP 6: Initialize MIDlet                                                  │
│  ─────────────────────────                                                  │
│  Object common = ReflectionHelper.getFieldValue(app, "common");             │
│  EmulatorReflectionHelper.initializeMIDlet(common);                         │
│                                                                              │
│  // Under the hood:                                                          │
│  // - Call common.initMIDlet(true)                                          │
│  // - This starts the J2ME application                                      │
│      │                                                                       │
│      ▼                                                                       │
│  STEP 7: Setup component listener                                           │
│  ────────────────────────────────                                           │
│  EmulatorReflectionHelper.setupComponentListener(app);                      │
│                                                                              │
│  // Under the hood:                                                          │
│  // - Get app.componentListener field                                       │
│  // - Call app.addComponentListener(componentListener)                      │
│      │                                                                       │
│      ▼                                                                       │
│  STEP 8: Notify state changed                                               │
│  ────────────────────────────                                               │
│  EmulatorReflectionHelper.notifyStateChanged(app);                          │
│                                                                              │
│  // Under the hood:                                                          │
│  // - Get app.responseInterfaceListener field                               │
│  // - Call responseInterfaceListener.stateChanged(true)                     │
│      │                                                                       │
│      ▼                                                                       │
│  RESULT: Return configured JFrame                                           │
│  return app;                                                                 │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 📊 MicroEmulator Classes Accessed

| Class | Purpose |
|-------|---------|
| `org.microemu.app.Main` | Main application JFrame |
| `org.microemu.app.Common` | Core initialization logic |
| `org.microemu.device.j2se.J2SEDevice` | J2SE device implementation |
| `org.microemu.device.DeviceFactory` | Factory to get current device |
| `org.microemu.app.Config` | Configuration utilities |
| `org.microemu.app.util.MIDletResourceLoader` | MIDlet resource loading |

---

## 📊 Fields Accessed via Reflection

| Object | Field | Type | Purpose |
|--------|-------|------|---------|
| `Main` | `common` | `Common` | Core emulator logic |
| `Main` | `selectDevicePanel` | `JPanel` | Device selection panel |
| `Main` | `deviceEntry` | `DeviceEntry` | Current device entry |
| `Main` | `devicePanel` | `JPanel` | Emulator display panel |
| `Main` | `menuExitListener` | `ActionListener` | Exit menu handler |
| `Main` | `componentListener` | `ComponentListener` | Resize handler |
| `Main` | `responseInterfaceListener` | `Object` | State callback |
| `MIDletResourceLoader` | `classLoader` (static) | `ClassLoader` | J2ME app ClassLoader |

---

## ⚠️ Error Handling

Common exceptions:

| Exception | Cause |
|-----------|-------|
| `ClassNotFoundException` | MicroEmulator class not found in ClassLoader |
| `NoSuchFieldException` | Field name changed in different MicroEmulator version |
| `NoSuchMethodException` | Method signature changed |
| `IllegalAccessException` | Cannot access even with `setAccessible(true)` |
| `InvocationTargetException` | Target method threw an exception |

```java
try {
    JFrame app = launchMicroEmulator(params, classLoader);
    // ...
} catch (ClassNotFoundException e) {
    throw new Exception("MicroEmulator class not found: " + e.getMessage());
} catch (NoSuchFieldException | NoSuchMethodException e) {
    throw new Exception("MicroEmulator API changed: " + e.getMessage());
} catch (InvocationTargetException e) {
    throw new Exception("MicroEmulator error: " + e.getTargetException().getMessage());
}
```

---

## 🔗 Related Documentation

- [CLASSLOADER.md](CLASSLOADER.md) - How MicroEmulator is loaded
- [BYTECODE.md](BYTECODE.md) - Bytecode transformation
- [ARCHITECTURE.md](ARCHITECTURE.md) - Overall architecture
