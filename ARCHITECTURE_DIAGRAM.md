# J2ME Launcher - Layered Architecture Diagram

## 🏗️ Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                     🚀 APPLICATION LAYER                        │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │  application/                                             │  │
│  │  ├── MainApplication.java    (Entry Point)                │  │
│  │  └── config/                                              │  │
│  │  │   └── ApplicationConfig.java                           │  │
│  │  └── script/                                              │  │
│  │      └── state/    (Editor State Management)              │  │
│  └───────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                              ↓ depends on
┌─────────────────────────────────────────────────────────────────┐
│                    🎨 PRESENTATION LAYER (UI)                   │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │  presentation/                                            │  │
│  │  ├── common/          (Shared UI Components)              │  │
│  │  │   ├── component/  (StatusBar, BaseTabPanel, etc.)      │  │
│  │  │   ├── dialog/     (MessageDialog, ConfirmDialog)       │  │
│  │  │   ├── builder/    (UI Builders)                        │  │
│  │  │   └── layout/     (Custom Layouts)                     │  │
│  │  ├── emulator/                                            │  │
│  │  │   └── panel/      (ApplicationsPanel, InstancesPanel)  │  │
│  │  └── script/                                              │  │
│  │      ├── LuaScriptManager.java                            │  │
│  │      ├── editor/     (Code Editor Components)             │  │
│  │      ├── completion/ (Auto-completion)                    │  │
│  │      ├── syntax/     (Syntax Highlighting)                │  │
│  │      └── component/  (Script UI Components)               │  │
│  └───────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                              ↓ depends on
┌─────────────────────────────────────────────────────────────────┐
│              🏛️ DOMAIN LAYER (Business Logic)                   │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │  domain/                                                  │  │
│  │  ├── emulator/        (Emulator Domain)                   │  │
│  │  │   ├── model/       (EmulatorInstance)                  │  │
│  │  │   ├── service/     (InstanceManager, Lifecycle)        │  │
│  │  │   └── launcher/    (EmulatorLauncher)                  │  │
│  │  ├── application/     (Application Domain)                │  │
│  │  │   ├── model/       (J2meApplication)                   │  │
│  │  │   └── service/     (ApplicationService)                │  │
│  │  └── script/          (Script Domain)                     │  │
│  │      ├── model/       (LuaScript)                         │  │
│  │      ├── executor/    (LuaScriptExecutor)                 │  │
│  │      └── library/     (Java-Lua Bridge)                   │  │
│  └───────────────────────────────────────────────────────────┘  │
│                                                                 │
│  ⚠️ NO DEPENDENCIES ON OTHER LAYERS                             │
│  (Framework-independent, Pure Business Logic)                   │
└─────────────────────────────────────────────────────────────────┘
                              ↑ used by
┌─────────────────────────────────────────────────────────────────┐
│            🔧 INFRASTRUCTURE LAYER (Technical)                  │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │  infrastructure/                                          │  │
│  │  ├── bytecode/       (ASM Bytecode Manipulation)          │  │
│  │  ├── classloader/    (Custom ClassLoader)                 │  │
│  │  ├── thread/         (Thread Management)                  │  │
│  │  ├── resource/       (Resource & Manifest Loading)        │  │
│  │  ├── input/          (Input Synchronization)              │  │
│  │  └── persistence/    (File Storage)                       │  │
│  │      ├── script/     (Script File Manager)                │  │
│  │      └── application/(App Persistence)                    │  │
│  └───────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                              ↓ depends on
┌─────────────────────────────────────────────────────────────────┐
│               📚 EXTERNAL LIBRARIES                             │
│  ├── ASM (Bytecode)                                             │
│  ├── LuaJ (Lua Interpreter)                                     │
│  ├── Gson (JSON)                                                │
│  └── Swing (UI Framework)                                       │
└─────────────────────────────────────────────────────────────────┘
```

---

## 📐 Dependency Rules

### ✅ Allowed Dependencies

```
app            →  presentation, domain, infrastructure
presentation   →  application, domain
application    →  domain
infrastructure →  external libraries only
domain         →  NOTHING (pure business logic)
```

### ❌ Forbidden Dependencies

```
domain         ⤫  infrastructure   (NEVER)
domain         ⤫  presentation     (NEVER)
infrastructure ⤫  domain           (NEVER)
infrastructure ⤫  application      (NEVER)
```

---

## 🎯 Layer Responsibilities

### 🚀 Application Layer
**Responsibility**: Application bootstrap and global configuration
- Entry point (`MainApplication`)
- Global configuration
- Application-level initialization

### 🎨 Presentation Layer
**Responsibility**: User interface and user interaction
- Swing components
- Panels, dialogs, buttons
- Event handlers
- View logic ONLY (no business logic)

### 💼 Application Layer
**Responsibility**: Use cases and workflows
- Orchestrate business operations
- Coordinate between domain and infrastructure
- Transaction boundaries
- Application-specific state management

### 🏛️ Domain Layer
**Responsibility**: Core business logic
- Business entities and models
- Business rules and validations
- Domain services
- **Framework-independent** (no Swing, no file I/O)

### 🔧 Infrastructure Layer
**Responsibility**: Technical implementation
- Database/file access
- External API calls
- Bytecode manipulation
- Threading and concurrency
- Framework-specific code

---

## 🔄 Data Flow Example

### Example: "User Launches an Emulator Instance"

```
1. USER CLICKS BUTTON
   ↓
2. PRESENTATION Layer
   InstancesPanel handles button click
   ↓
3. APPLICATION Layer (Use Case)
   LaunchInstanceUseCase orchestrates
   ↓
4. DOMAIN Layer
   InstanceManager.createInstance()
   EmulatorLauncher.launch()
   ↓
5. INFRASTRUCTURE Layer
   EmulatorClassLoader loads classes
   ResourceManager loads resources
   ↓
6. PRESENTATION Layer
   UI updated with new instance
```

---

## 🗂️ Package Organization

### Domain Packages (Business Logic)
```
domain/
├── emulator/     → Emulator business logic
├── application/  → Application management
└── script/       → Scripting functionality
```

### Infrastructure Packages (Technical)
```
infrastructure/
├── bytecode/     → ASM bytecode manipulation
├── classloader/  → Custom classloading
├── thread/       → Threading utilities
├── resource/     → Resource loading
├── input/        → Input synchronization
└── persistence/  → Data storage
```

### Presentation Packages (UI)
```
presentation/
├── common/       → Shared UI components
├── emulator/     → Emulator-specific UI
└── script/       → Script editor UI
```

---

## 🧪 Testing Strategy

### Unit Tests
- **Domain Layer**: Pure unit tests (no mocks needed)
- **Application Layer**: Mock domain and infrastructure
- **Infrastructure Layer**: Integration tests with real dependencies

### Integration Tests
- Test layer interactions
- Verify dependency rules
- End-to-end workflows

---

## 📈 Benefits Achieved

| Benefit | Description |
|---------|-------------|
| **Testability** | Domain logic testable without UI or infrastructure |
| **Maintainability** | Clear code organization, easy to find and modify |
| **Scalability** | Easy to add new features without breaking existing code |
| **Flexibility** | Can swap implementations (e.g., file → database) |
| **Understanding** | New developers quickly understand the structure |
| **Reusability** | Domain logic reusable in different contexts |

---

## 🚀 Future Enhancements

With this architecture, you can easily:
1. ✅ Add new emulator features in `domain/emulator/`
2. ✅ Swap file storage with database in `infrastructure/persistence/`
3. ✅ Add new UI components in `presentation/`
4. ✅ Implement new use cases in `application/`
5. ✅ Keep domain logic independent and testable

---

**Architecture Type**: Layered Architecture + Domain-Driven Design
**Pattern**: Clean Architecture inspired
**Status**: ✅ Fully Implemented
