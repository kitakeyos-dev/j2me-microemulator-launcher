# J2ME MicroEmulator Launcher - Technical Documentation

> **Purpose**: This documentation is designed to provide complete understanding of the project for AI agents and developers. Every component, class, and interaction is explained in detail.

---

## 📋 Project Summary

| Attribute | Value |
|-----------|-------|
| **Name** | J2ME MicroEmulator Launcher |
| **Type** | Desktop Application (Java Swing) |
| **Language** | Java 8 |
| **Build System** | Maven |
| **Architecture** | Clean Architecture (4 layers) |
| **Main Entry Point** | `me.kitakeyos.j2me.application.MainApplication` |
| **Key Dependencies** | ASM 3.1, Gson 2.10.1 |

---

## 🎯 What This Project Does

This application allows users to:

1. **Run J2ME (Java ME) applications** on a modern desktop using MicroEmulator
2. **Run multiple instances simultaneously** - each instance is isolated with its own ClassLoader
3. **Monitor network traffic** - intercept, log, redirect, and proxy all socket connections
4. **Inject Java code at runtime** - load external JARs and execute against running instances
6. **Manage J2ME apps** - install, store, and organize J2ME JAR/JAD files

### Why Bytecode Manipulation is Needed

J2ME apps make system calls that need to be intercepted:
- `System.exit()` → Must close only the specific instance, not the entire launcher
- `new Socket()` → Must be monitored and potentially redirected/proxied
- `Thread` subclasses → Must be tracked for cleanup when instance shuts down

---

## 📚 Documentation Index

### Core Documentation

| Document | Purpose | Audience |
|----------|---------|----------|
| [ARCHITECTURE.md](ARCHITECTURE.md) | Complete architecture with all layers, packages, and class relationships | Understanding codebase structure |
| [GETTING_STARTED.md](GETTING_STARTED.md) | Installation, setup, and basic usage | First-time users |
| [CONFIGURATION.md](CONFIGURATION.md) | All configuration files and properties | Configuring the application |

### Feature Documentation

| Document | Purpose | Audience |
|----------|---------|----------|
| [NETWORK.md](NETWORK.md) | Network monitoring, redirection, proxy system | Using/extending network features |
| [INJECTION.md](INJECTION.md) | Java injection API and guides | Runtime class injection |

### Technical Deep-Dives

| Document | Purpose | Audience |
|----------|---------|----------|
| [CLASSLOADER.md](CLASSLOADER.md) | Custom ClassLoader implementation details | Understanding class loading |
| [BYTECODE.md](BYTECODE.md) | ASM bytecode manipulation system | Understanding bytecode interception |
| [REFLECTION.md](REFLECTION.md) | Reflection for emulator initialization | Understanding emulator startup |
| [API.md](API.md) | Complete API reference for all key classes | Developers extending the project |

### Contribution

| Document | Purpose |
|----------|---------|
| [CONTRIBUTING.md](CONTRIBUTING.md) | How to contribute to the project |

---

## 🏗️ Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           PRESENTATION LAYER                                 │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐              │
│  │ ApplicationsPanel│  │ InstancesPanel  │  │ NetworkMonitor  │              │
│  │   (Tab 1)       │  │   (Tab 2)       │  │   Dialog        │              │
│  └────────┬────────┘  └────────┬────────┘  └────────┬────────┘              │
└───────────┼─────────────────────┼───────────────────┼───────────────────────┘
            │                     │                   │
            ▼                     ▼                   ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                           APPLICATION LAYER                                  │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐              │
│  │ MainApplication │  │ EmulatorLauncher│  │ ApplicationConfig│             │
│  │   (Entry Point) │  │   (Starts EMU)  │  │   (Settings)    │              │
│  └────────┬────────┘  └────────┬────────┘  └────────┬────────┘              │
└───────────┼─────────────────────┼───────────────────┼───────────────────────┘
            │                     │                   │
            ▼                     ▼                   ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                             DOMAIN LAYER                                     │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐              │
│  │ EmulatorInstance│  │ NetworkService  │  │ ApplicationServ.│              │
│  │   (Model)       │  │   (Singleton)   │  │   (CRUD)        │              │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘              │
│  ┌─────────────────┐  ┌─────────────────┐                                   │
│  ┌─────────────────┐  ┌─────────────────┐                                   │
│  │ InjectionService│  │ InstanceManager │                                   │
│  │   (Injection)   │  │   (Lifecycle)   │                                   │
│  └─────────────────┘  └─────────────────┘                                   │
└─────────────────────────────────────────────────────────────────────────────┘
            ▲                     ▲                   ▲
            │                     │                   │
┌───────────┼─────────────────────┼───────────────────┼───────────────────────┐
│           │       INFRASTRUCTURE LAYER              │                        │
│  ┌────────┴────────┐  ┌────────┴────────┐  ┌───────┴─────────┐              │
│  │EmulatorClassLoad│  │ SystemCallHandler│ │ApplicationRepoImpl│            │
│  │  (Custom CL)    │  │ (Intercepts)    │  │   (Persistence) │              │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘              │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐              │
│  │ClassPreprocessor│  │ MonitoredSocket │                                    │
│  │   (ASM)         │  │ (Network I/O)   │                                    │
│  └─────────────────┘  └─────────────────┘                                    │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 📁 Directory Structure

```
j2me-microemulator-launcher/
├── pom.xml                          # Maven build configuration
├── microemulator.jar                # MicroEmulator library (external)
├── src/main/java/me/kitakeyos/j2me/
│   ├── application/                 # APPLICATION LAYER
│   │   ├── MainApplication.java     # Entry point, DI container
│   │   ├── config/
│   │   │   └── ApplicationConfig.java
│   │   └── emulator/
│   │       └── EmulatorLauncher.java
│   │
│   ├── domain/                      # DOMAIN LAYER (Business Logic)
│   │   ├── application/
│   │   │   ├── model/J2meApplication.java
│   │   │   ├── repository/ApplicationRepository.java  # Interface
│   │   │   └── service/ApplicationService.java
│   │   ├── emulator/
│   │   │   ├── model/EmulatorInstance.java
│   │   │   ├── resource/ResourceManager.java
│   │   │   ├── input/InputSynchronizer.java           # Interface
│   │   │   └── service/
│   │   │       ├── InstanceManager.java
│   │   │       ├── InstanceIdPool.java
│   │   │       └── InstanceLifecycleManager.java
│   │   ├── network/
│   │   │   ├── model/
│   │   │   │   ├── ConnectionLog.java
│   │   │   │   ├── PacketLog.java
│   │   │   │   ├── ProxyRule.java
│   │   │   │   └── RedirectionRule.java
│   │   │   └── service/NetworkService.java            # Singleton
│   │   └── injection/
│   │       ├── model/
│   │       │   ├── InjectionEntry.java
│   │       │   └── InjectionLogger.java
│   │       └── service/InjectionService.java
│   │
│   ├── infrastructure/              # INFRASTRUCTURE LAYER
│   │   ├── bytecode/
│   │   │   ├── ByteCodeHelper.java
│   │   │   ├── ClassPreprocessor.java  → in classloader/
│   │   │   ├── InstrumentationClassVisitor.java
│   │   │   ├── ModificationTracker.java
│   │   │   ├── SystemCallHandler.java
│   │   │   └── SystemCallInterceptor.java
│   │   ├── classloader/
│   │   │   ├── ClassPreprocessor.java
│   │   │   └── EmulatorClassLoader.java
│   │   ├── input/
│   │   │   └── InputSynchronizerImpl.java
│   │   ├── monitoring/
│   │   │   └── SystemMonitorService.java
│   │   ├── network/
│   │   │   ├── MonitoredInputStream.java
│   │   │   ├── MonitoredOutputStream.java
│   │   │   └── MonitoredSocket.java
│   │   ├── persistence/
│   │   │   └── application/ApplicationRepositoryImpl.java
│   │   ├── resource/
│   │   │   └── ManifestReader.java
│   │   └── thread/
│   │       └── XThread.java
│   │
│   ├── presentation/                # PRESENTATION LAYER
│   │   ├── common/
│   │   │   ├── builder/ConfigurationPanelBuilder.java
│   │   │   ├── component/
│   │   │   │   ├── BaseTabPanel.java
│   │   │   │   ├── ScrollablePanel.java
│   │   │   │   ├── StatusBar.java
│   │   │   │   └── ToastNotification.java
│   │   │   ├── dialog/
│   │   │   │   ├── ConfirmDialog.java
│   │   │   │   ├── MessageDialog.java
│   │   │   │   └── SettingsDialog.java
│   │   │   └── layout/
│   │   │       └── WrapLayout.java
│   │   ├── emulator/panel/
│   │   │   ├── ApplicationsPanel.java
│   │   │   └── InstancesPanel.java
│   │   ├── monitor/
│   │   │   └── SystemMonitorDialog.java
│   │   ├── network/
│   │   │   └── NetworkMonitorDialog.java
│   │   └── injection/panel/
│   │       └── InjectionPanel.java
│   │
│   └── util/
│       └── reflection/
│           ├── ReflectionHelper.java
│           └── EmulatorReflectionHelper.java
│
└── data/                            # RUNTIME DATA (created at runtime)
    ├── j2me_launcher.properties
    ├── j2me_apps.properties
    ├── network_rules.properties
    ├── apps/
    ├── icons/
    └── rms/
```

---

## 🔄 Key Flows

### Flow 1: Starting an Emulator Instance

```
User clicks "Create & Run"
        │
        ▼
InstancesPanel.createInstances()
        │
        ▼
InstanceManager.createInstance() ──► EmulatorInstance created with CREATED state
        │
        ▼
EmulatorLauncher.startEmulatorInstance()
        │
        ├──► EmulatorClassLoader created (custom ClassLoader)
        │           │
        │           ▼
        │    Classes loaded from microemulator.jar
        │           │
        │           ▼
        │    ClassPreprocessor.instrumentAndModifyBytecode()
        │           │
        │           ▼
        │    InstrumentationClassVisitor transforms bytecode:
        │        - Thread → XThread
        │        - System.exit() → SystemCallHandler.exit()
        │        - new Socket() → SystemCallHandler.createSocket()
        │
        ├──► ReflectionHelper.createInstance("org.microemu.app.Main")
        │           │
        │           ▼
        │    EmulatorReflectionHelper configures emulator:
        │        - initializeEmulatorParams()
        │        - configureDisplaySize()
        │        - initializeMIDlet()
        │
        └──► Instance state set to RUNNING
                    │
                    ▼
            Device panel extracted and added to launcher UI
```

### Flow 2: Network Connection Interception

```
J2ME App: new Socket("game.server.com", 8080)
        │
        ▼
[Bytecode was transformed to call SystemCallHandler.createSocket()]
        │
        ▼
SystemCallHandler.createSocket(instanceId, "game.server.com", 8080)
        │
        ▼
NetworkService.getInstance().createSocket(instanceId, host, port)
        │
        ├──► Check RedirectionRules (host:port → target:port)
        │
        ├──► Check ProxyRules (SOCKS/HTTP proxy)
        │
        ├──► Create actual Socket (with redirection/proxy applied)
        │
        ├──► Wrap in MonitoredSocket (logs all I/O)
        │
        └──► Return wrapped socket to J2ME app

All data sent/received is logged via MonitoredInputStream/MonitoredOutputStream
```

---

## 📦 Maven Dependencies

```xml
<dependencies>
    <!-- ASM for bytecode manipulation -->
    <dependency>
        <groupId>asm</groupId>
        <artifactId>asm</artifactId>
        <version>3.1</version>
    </dependency>
    
    <!-- Gson for JSON handling -->
    <dependency>
        <groupId>com.google.code.gson</groupId>
        <artifactId>gson</artifactId>
        <version>2.10.1</version>
    </dependency>
</dependencies>
```

---

## 🔑 Key Technical Decisions

### 1. Why Custom ClassLoader?

Each emulator instance needs:
- **Isolation**: Classes from one instance don't interfere with another
- **Bytecode modification**: Intercept system calls before class loading
- **Reverse delegation**: Load MIDlet classes before system classes

### 2. Why ASM for Bytecode Manipulation?

- Lightweight and fast
- Works at class load time
- Can modify any class, including third-party code

### 3. Why Reflection for Emulator Initialization?

- MicroEmulator is loaded at runtime via ClassLoader
- Cannot use direct imports for MicroEmulator classes
- Need to access private fields and methods

### 4. Why Singleton for NetworkService?

- All instances share the same network rules
- Need centralized logging and monitoring
- Rules persist across sessions

---

## 🔗 External Resources

- [MicroEmulator](http://www.microemu.org/) - The emulator used
- [ASM Library](https://asm.ow2.io/) - Bytecode manipulation
---

## 📝 License

MIT License - see [LICENSE](../LICENSE) file.
