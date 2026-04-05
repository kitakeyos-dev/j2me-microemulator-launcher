# Architecture

> **Purpose**: Complete architecture documentation explaining every layer, package, and class relationship.

---

## 🎯 Overview

This project follows **Clean Architecture** with 4 layers:

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                                                                              │
│                         DEPENDENCY DIRECTION                                 │
│                                                                              │
│    ┌──────────────┐         ┌──────────────┐         ┌──────────────┐       │
│    │ PRESENTATION │───────▶│ APPLICATION  │───────▶│   DOMAIN     │       │
│    │   (UI)       │         │(Orchestration)│        │(Business)    │       │
│    └──────────────┘         └──────────────┘         └──────────────┘       │
│                                                             ▲               │
│                                                             │               │
│                             ┌──────────────┐                │               │
│                             │INFRASTRUCTURE│────────────────┘               │
│                             │(Implementation)                               │
│                             └──────────────┘                                │
│                                                                              │
│    Dependencies flow INWARD toward Domain (core business logic)             │
│    Domain has NO dependencies on other layers                               │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 📦 Layer Details

### 1. Presentation Layer

**Purpose**: User interface components (Swing)

**Package**: `me.kitakeyos.j2me.presentation`

```
presentation/
├── common/                      # Shared UI components
│   ├── builder/
│   │   └── ConfigurationPanelBuilder.java
│   ├── component/
│   │   ├── BaseTabPanel.java    # Base class for tab panels
│   │   ├── ScrollablePanel.java
│   │   ├── StatusBar.java
│   │   └── ToastNotification.java
│   ├── dialog/
│   │   ├── ConfirmDialog.java
│   │   ├── MessageDialog.java
│   │   └── SettingsDialog.java
│   └── layout/
│       └── WrapLayout.java      # Flow layout that wraps
│
├── emulator/panel/
│   ├── ApplicationsPanel.java   # Tab 1: Manage J2ME apps
│   ├── EmulatorsPanel.java      # Tab 2: Manage emulator configs
│   └── InstancesPanel.java      # Tab 3: Running instances
│
├── monitor/
│   └── SystemMonitorDialog.java # RAM/CPU/Thread monitor
│
├── network/
│   └── NetworkMonitorDialog.java # Network monitoring UI
│
└── injection/panel/
    └── InjectionPanel.java      # Java injection UI
```

**Key Classes**:

| Class | Purpose |
|-------|---------|
| `ApplicationsPanel` | Grid of installed J2ME apps with install/remove |
| `EmulatorsPanel` | Add/edit/remove emulator JAR configurations |
| `InstancesPanel` | Shows running emulator instances, start/stop controls |
| `NetworkMonitorDialog` | View connection logs, manage rules |
| `InjectionPanel` | Load JARs, execute code against running instances |

---

### 2. Application Layer

**Purpose**: Orchestration, coordination between layers

**Package**: `me.kitakeyos.j2me.application`

```
application/
├── MainApplication.java         # Entry point, JFrame, DI container
├── config/
│   └── ApplicationConfig.java   # Settings management
└── emulator/
    └── EmulatorLauncher.java    # Coordinates instance startup
```

**Key Classes**:

| Class | Purpose |
|-------|---------|
| `MainApplication` | Main JFrame, creates all services, DI container |
| `ApplicationConfig` | Load/save settings from properties file |
| `EmulatorLauncher` | Coordinates ClassLoader, reflection, instance creation |

#### MainApplication Details

```java
public class MainApplication extends JFrame {
    public static final MainApplication INSTANCE = new MainApplication();
    
    // Services (created in constructor)
    private final ApplicationConfig applicationConfig;
    private final ApplicationRepository applicationRepository;
    private final ApplicationService applicationService;
    public InstanceManager emulatorInstanceManager;
    
    // Panels
    private ApplicationsPanel applicationsPanel;
    private EmulatorsPanel emulatorsPanel;
    private InstancesPanel instancesPanel;
    private InjectionPanel injectionPanel;
    
    public MainApplication() {
        // Initialize config and load i18n bundle
        applicationConfig = new ApplicationConfig();
        Messages.loadBundle(applicationConfig.getLanguage());
        
        setTitle(Messages.get("app.title"));
        
        // Create services
        applicationRepository = new ApplicationRepositoryImpl(applicationConfig);
        applicationService = new ApplicationService(applicationRepository);
        
        // Create panels
        applicationsPanel = new ApplicationsPanel(this, applicationConfig, applicationService);
        emulatorsPanel = new EmulatorsPanel(this, applicationConfig, applicationService, ...);
        instancesPanel = new InstancesPanel(this, applicationConfig, applicationService);
        injectionPanel = new InjectionPanel(this, applicationConfig, applicationService);
        
        emulatorInstanceManager = instancesPanel.emulatorInstanceManager;
        initializeComponents();
    }
    
    private void initializeComponents() {
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab(Messages.get("tab.applications"), applicationsPanel);
        tabbedPane.addTab(Messages.get("tab.emulators"), emulatorsPanel);
        tabbedPane.addTab(Messages.get("tab.instances"), instancesPanel);
        tabbedPane.addTab(Messages.get("tab.injection"), injectionPanel);
        add(tabbedPane);
    }
    
    // Rebuilds the entire UI after language change (stops all running instances first)
    public void rebuildUI() { ... }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> INSTANCE.setVisible(true));
    }
}
```

---

### 3. Domain Layer

**Purpose**: Core business logic, entities, interfaces

**Package**: `me.kitakeyos.j2me.domain`

```
domain/
├── application/                 # J2ME app management
│   ├── model/
│   │   └── J2meApplication.java
│   ├── repository/
│   │   └── ApplicationRepository.java  # Interface
│   └── service/
│       └── ApplicationService.java
│
├── emulator/                    # Emulator instance management
│   ├── model/
│   │   ├── EmulatorConfig.java        # Emulator configuration
│   │   └── EmulatorInstance.java
│   ├── repository/
│   │   └── EmulatorConfigRepository.java  # Interface
│   ├── resource/
│   │   └── ResourceManager.java
│   ├── input/
│   │   └── InputSynchronizer.java      # Interface
│   └── service/
│       ├── InstanceManager.java
│       ├── InstanceIdPool.java
│       └── InstanceLifecycleManager.java
│
├── network/                     # Network monitoring
│   ├── model/
│   │   ├── ConnectionLog.java
│   │   ├── PacketLog.java
│   │   ├── ProxyRule.java
│   │   └── RedirectionRule.java
│   └── service/
│       └── NetworkService.java          # Singleton
│
├── injection/                   # Java injection
│   ├── model/
│   │   ├── InjectionEntry.java
│   │   └── InjectionLogger.java
│   └── service/
│       └── InjectionService.java
│
├── speed/                       # Speed control
│   └── service/
│       └── SpeedService.java            # Singleton
│
└── graphics/                    # Graphics optimization
    └── service/
        └── GraphicsOptimizationService.java  # Singleton
```

**Key Classes**:

| Class | Purpose |
|-------|---------|
| `EmulatorConfig` | Model representing an installed emulator configuration |
| `EmulatorInstance` | Model representing a running emulator |
| `NetworkService` | Singleton managing network rules and logging |
| `InstanceManager` | Manages lifecycle of all instances |
| `InjectionService` | Loads external JARs and executes code against instances |
| `SpeedService` | Manages per-instance speed multipliers |
| `GraphicsOptimizationService` | Toggles paint interception via dynamic proxy |

#### EmulatorInstance Details

```java
public class EmulatorInstance {
    public enum InstanceState { CREATED, STARTING, RUNNING, STOPPED }
    
    // Identification
    private final int instanceId;
    
    // Configuration
    private final String microemulatorPath;
    private final String j2meFilePath;
    private final int displayWidth;
    private final int displayHeight;
    private final boolean fullDisplayMode;
    
    // State
    private InstanceState state = InstanceState.CREATED;
    
    // UI Components (set after launch)
    private JPanel devicePanel;
    private JComponent emulatorDisplay;
    
    // ClassLoaders
    private EmulatorClassLoader emulatorClassLoader;  // MicroEmulator classes
    private ClassLoader appClassLoader;               // J2ME app classes
    
    // Resources (tracked for cleanup)
    private final ResourceManager resourceManager;
    // Contains: threads, sockets to close on shutdown
    
    public void shutdown() {
        InstanceLifecycleManager.shutdown(this);
    }
}
```

#### NetworkService Details

```java
public class NetworkService {
    private static final NetworkService INSTANCE = new NetworkService();
    
    // Rules
    private final List<RedirectionRule> redirectionRules;
    private final List<ProxyRule> proxyRules;
    
    // Logs
    private final List<ConnectionLog> connectionLogs;
    private final List<PacketLog> packetLogs;
    
    // Socket taps: socketId → SocketTap (stream-based data access)
    private final Map<Integer, SocketTap> socketTaps;
    
    // Per-instance tapping control (off by default)
    private final Set<Integer> tappingEnabledInstances;
    
    // Called when J2ME app creates a socket
    public Socket createSocket(int instanceId, String host, int port) {
        // Apply redirection rules
        // Apply proxy rules
        // Log connection
        // Return socket
    }
}
```

---

### 4. Infrastructure Layer

**Purpose**: Concrete implementations of interfaces, external integrations

**Package**: `me.kitakeyos.j2me.infrastructure`

```
infrastructure/
├── bytecode/                    # ASM bytecode manipulation
│   ├── ByteCodeHelper.java
│   ├── InstrumentationClassVisitor.java
│   ├── ModificationTracker.java
│   ├── SystemCallHandler.java
│   └── SystemCallInterceptor.java
│
├── classloader/                 # Custom ClassLoader
│   ├── ClassPreprocessor.java
│   └── EmulatorClassLoader.java
│
├── input/
│   └── InputSynchronizerImpl.java
│
├── monitoring/
│   └── SystemMonitorService.java
│
├── network/                     # Network I/O wrappers
│   ├── MonitoredInputStream.java
│   ├── MonitoredOutputStream.java
│   └── MonitoredSocket.java
│
├── persistence/                 # File storage
│   ├── application/
│   │   └── ApplicationRepositoryImpl.java
│   └── emulator/
│       └── EmulatorConfigRepositoryImpl.java
│
├── resource/
│   └── ManifestReader.java      # Read JAR manifest
│
└── thread/
    └── XThread.java             # Custom Thread for tracking
```

**Key Classes**:

| Class | Purpose |
|-------|---------|
| `EmulatorClassLoader` | Custom ClassLoader with bytecode transformation |
| `ClassPreprocessor` | ASM transformation pipeline |
| `SystemCallHandler` | Receives intercepted system calls |
| `MonitoredSocket` | Socket wrapper for packet capture |
| `ApplicationRepositoryImpl` | Saves apps to properties file |
| `EmulatorConfigRepositoryImpl` | Saves emulator configs to properties file |

---

## 🔄 Class Relationships

### Emulator Launch Sequence

```
┌───────────────────────────────────────────────────────────────────┐
│                    CLASS COLLABORATION                             │
├───────────────────────────────────────────────────────────────────┤
│                                                                    │
│  InstancesPanel (Presentation)                                     │
│      │                                                             │
│      │ createInstances()                                           │
│      ▼                                                             │
│  InstanceManager (Domain)                                          │
│      │                                                             │
│      │ createInstance() → returns EmulatorInstance                │
│      │                                                             │
│      ▼                                                             │
│  EmulatorLauncher (Application)                                    │
│      │                                                             │
│      │ startEmulatorInstance(instance)                            │
│      │                                                             │
│      ├───▶ EmulatorClassLoader (Infrastructure)                   │
│      │         │                                                   │
│      │         │ loadClass() → transform bytecode                 │
│      │         │                                                   │
│      │         └───▶ ClassPreprocessor                            │
│      │                   │                                         │
│      │                   └───▶ InstrumentationClassVisitor        │
│      │                             │                               │
│      │                             └───▶ SystemCallInterceptor    │
│      │                                                             │
│      └───▶ ReflectionHelper / EmulatorReflectionHelper (Util)     │
│                │                                                   │
│                │ Create and configure MicroEmulator via reflection│
│                │                                                   │
│                └───▶ org.microemu.app.Main (external)             │
│                                                                    │
└───────────────────────────────────────────────────────────────────┘
```

### Network Interception

```
┌───────────────────────────────────────────────────────────────────┐
│                    NETWORK CLASS COLLABORATION                     │
├───────────────────────────────────────────────────────────────────┤
│                                                                    │
│  J2ME App Code (external)                                          │
│      │                                                             │
│      │ new Socket(host, port)                                     │
│      │ [Bytecode transformed at load time]                        │
│      ▼                                                             │
│  SystemCallHandler.createSocket() (Infrastructure)                │
│      │                                                             │
│      │                                                             │
│      ├───▶ NetworkService.createSocket() (Domain)                 │
│      │         │                                                   │
│      │         ├── Check RedirectionRule matches                  │
│      │         ├── Check ProxyRule applies                        │
│      │         ├── Create actual java.net.Socket                  │
│      │         └── Log ConnectionLog                              │
│      │                                                             │
│      └───▶ new MonitoredSocket(realSocket) (Infrastructure)       │
│                │                                                   │
│                │ getInputStream() → MonitoredInputStream          │
│                │ getOutputStream() → MonitoredOutputStream        │
│                │                                                   │
│                └── All I/O logged to NetworkService               │
│                                                                    │
└───────────────────────────────────────────────────────────────────┘
```

---

## 📊 Data Flow

### Configuration Loading

```
ApplicationConfig
      │
      │ load()
      ▼
data/j2me_launcher.properties
      │
      ├── microemulatorPath
      ├── defaultDisplayWidth
      ├── defaultDisplayHeight
      └── ui.language
```

### Application Storage

```
J2meApplication
      │
      │ save via ApplicationRepository
      ▼
ApplicationRepositoryImpl
      │
      │ write to
      ▼
data/j2me_apps.properties
      │
      ├── app.0.name=Game
      ├── app.0.path=apps/game.jar
      └── app.0.iconPath=icons/game.png
```

### Network Rules

```
RedirectionRule / ProxyRule
      │
      │ addRule()
      ▼
NetworkService
      │
      │ saveRules()
      ▼
data/network_rules.properties
```

### Emulator Storage

```
EmulatorConfig
      │
      │ save via EmulatorConfigRepository
      ▼
EmulatorConfigRepositoryImpl
      │
      │ write to
      ▼
data/emulators.properties
      │
      ├── emulator.0.id=uuid
      ├── emulator.0.name=MicroEmulator Default
      ├── emulator.0.jarPath=microemulator.jar
      ├── emulator.0.displayWidth=240
      └── emulator.0.displayHeight=320
```

---

## 🎨 Design Decisions

### 1. Singleton for NetworkService

**Why**: 
- All instances share the same rules
- Centralized logging
- Rules persist across sessions

**Trade-off**: 
- Harder to test in isolation
- Global state

### 2. Reverse Delegation in ClassLoader

**Why**:
- MIDlet classes must be loaded from J2ME JAR first
- System classes should be fallback
- Prevents class conflicts

### 3. Bytecode Modification vs. Source Modification

**Why bytecode**:
- No access to J2ME app source code
- Works with any J2ME app
- Transformation happens at load time

### 4. Reflection for MicroEmulator Access

**Why reflection**:
- MicroEmulator loaded at runtime
- Cannot compile against MicroEmulator classes
- Need to access internal fields/methods

---

## 🔗 Related Documentation

- [CLASSLOADER.md](CLASSLOADER.md) - ClassLoader internals
- [BYTECODE.md](BYTECODE.md) - Bytecode transformation
- [REFLECTION.md](REFLECTION.md) - Reflection usage
- [NETWORK.md](NETWORK.md) - Network system
