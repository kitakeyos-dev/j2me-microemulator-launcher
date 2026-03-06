# Java Injection

## Overview

The **Injection** tab allows developers to load external Java classes and execute them against running emulator instances at runtime. This is a developer tool for debugging, testing, and interacting with J2ME MIDlet internals.

## How It Works

```
Dev writes Java class → Builds JAR in IDE → Loads into app → Selects instance → Executes
```

### ClassLoader Chain

When executing an injection class, a custom classloader resolves classes in this order:

1. **Injection JAR** (child-first): Your `MyScript.class`, etc.
2. **App classloader**: `InjectionEntry`, `InjectionLogger`, app internals
3. **MIDlet classloader**: J2ME game classes (`Class_CZ`, etc.)

This means your injection code can directly reference MIDlet classes via imports.

## Quick Start

### 1. Add J2MELauncher as dependency

Run `mvn install -DskipTests` on the J2MELauncher project, then in your inject project's `pom.xml`:

```xml
<dependency>
    <groupId>me.kitakeyos.j2me</groupId>
    <artifactId>J2MELauncher</artifactId>
    <version>1.0.2</version>
    <scope>provided</scope>
</dependency>
```

> **Important**: Use `<scope>provided</scope>` to avoid bundling J2MELauncher classes into your JAR.

### 2. Implement `InjectionEntry`

```java
import me.kitakeyos.j2me.domain.injection.model.InjectionEntry;
import me.kitakeyos.j2me.domain.injection.model.InjectionLogger;

public class MyScript implements InjectionEntry {

    @Override
    public void execute(ClassLoader appClassLoader, InjectionLogger logger) {
        logger.info("Starting injection...");

        try {
            // Access MIDlet classes directly
            Class<?> gameClass = appClassLoader.loadClass("com.game.MainCanvas");
            logger.success("Found: " + gameClass.getName());

            // Use reflection to read/modify fields
            Object fieldValue = gameClass.getDeclaredField("score").get(null);
            logger.info("Score: " + fieldValue);
        } catch (Exception e) {
            logger.error("Failed", e);
        }
    }
}
```

### 3. Build and load

```bash
mvn package
```

In the app:
1. Go to **Injection** tab
2. Click **Load JAR** → select your built JAR
3. Select a running instance from the dropdown
4. Click **Execute** on the discovered entry class

## API Reference

### `InjectionEntry`

Interface that all injection classes must implement.

```java
public interface InjectionEntry {
    void execute(ClassLoader appClassLoader, InjectionLogger logger);
}
```

| Parameter | Description |
|-----------|-------------|
| `appClassLoader` | The MIDlet's ClassLoader. Use to load and interact with game classes. |
| `logger` | Logger that outputs to the app's log panel. |

### `InjectionLogger`

Logger with 4 levels:

| Method | Description |
|--------|-------------|
| `logger.info(String)` | General information |
| `logger.success(String)` | Success messages |
| `logger.warn(String)` | Warnings |
| `logger.error(String)` | Error messages |
| `logger.error(String, Throwable)` | Error with exception details |

Log output format: `[HH:mm:ss] [LEVEL] message`

## UI Features

| Button | Action |
|--------|--------|
| **Load JAR** | Browse and load a JAR file |
| **Reload JAR** | Hot-reload current JAR (after rebuilding) |
| **Refresh Instances** | Update the running instances dropdown |
| **Clear Log** | Clear the log output panel |

## Technical Details

- **File locking**: JAR is cloned to a temp file before loading, so the original is never locked by the JVM. You can rebuild freely.
- **Hot-reload**: Click "Reload JAR" to pick up changes after rebuilding. Old classloader is closed and temp file deleted.
- **Thread safety**: Injection code runs in a separate thread to avoid blocking the UI.
