---
description: Debug and fix issues in J2ME Launcher
---

# Debug Issue

Workflow to guide debugging issues in the project.

## Step 1: Identify error type

### ClassNotFoundException / NoSuchMethodException
→ Read `docs/CLASSLOADER.md` and `docs/REFLECTION.md`

### Socket / Network issues
→ Read `docs/NETWORK.md`

### Bytecode transformation issues
→ Read `docs/BYTECODE.md`

### Lua script errors
→ Read `docs/SCRIPTING.md`

## Step 2: Check common error points

### ClassLoader issues
- Does EmulatorClassLoader have correct URLs?
- Is reverse delegation working correctly?
- Is bytecode cache stale?

### Reflection issues
- Does field/method name match MicroEmulator version?
- Has setAccessible(true) been called?

### Network issues
- Is RedirectionRule enabled?
- Does InstanceId match (-1 = all)?
- Is MonitoredSocket wrapping correctly?

### Bytecode issues
- Is ModificationTracker tracking correctly?
- Is stack manipulation in correct order?

## Step 3: Add logging
```java
private static final Logger logger = Logger.getLogger(ClassName.class.getName());
logger.info("Debug: " + variable);
logger.log(Level.WARNING, "Error", exception);
```

## Step 4: Test isolation
Create separate test case to reproduce the error
