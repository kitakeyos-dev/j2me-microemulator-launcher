# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

J2ME MicroEmulator Launcher is a Java Swing desktop app for running and managing multiple J2ME (Java 2 Micro Edition) application instances via MicroEmulator. Key capabilities: multi-instance emulation, bytecode instrumentation (ASM), network interception, and runtime Java injection.

## Build & Run

```bash
mvn clean package                    # Build fat JAR
java -jar target/j2me-microemulator-launcher-*-jar-with-dependencies.jar  # Run
mvn test                             # Run tests (no tests exist yet)
```

Requires JDK 8+. Targets Java 8 bytecode. Maven 3.6+.

## Dependencies

- **ASM 3.1** - Bytecode manipulation (transforms J2ME JARs at load time)
- **Gson 2.10.1** - JSON processing

## Architecture

Clean Architecture with 4 layers. Dependencies flow inward toward Domain:

```
Presentation (Swing UI) -> Application (Orchestration) -> Domain (Business Logic)
                                                              ^
                                Infrastructure (Implementations) -+
```

### Layer packages under `me.kitakeyos.j2me`:

- **`domain/`** - Core business logic, no external dependencies. Contains models, repository interfaces, and services for: application management, emulator instances, network rules/logging, Java injection, speed control, graphics optimization.
- **`application/`** - Orchestration layer. `MainApplication` is the entry point and DI container (singleton JFrame). `EmulatorLauncher` coordinates instance startup via reflection. `ApplicationConfig` manages settings.
- **`infrastructure/`** - Concrete implementations. Custom `EmulatorClassLoader` with reverse delegation (MIDlet classes first). ASM bytecode visitors (`InstrumentationClassVisitor`, `SystemCallInterceptor`) intercept Thread.sleep() and Socket creation. `MonitoredSocket`/streams wrap I/O for packet capture. Properties-file persistence for all repositories.
- **`presentation/`** - Swing UI organized by feature. 4 tab panels: Applications, Emulators, Instances, Injection. `BaseTabPanel` is the common base class. Dialogs for network monitoring and system monitoring.

### Key architectural patterns

- **Reverse-delegation ClassLoader**: `EmulatorClassLoader` loads MIDlet classes before delegating to parent, opposite of standard Java classloading. Each emulator instance gets its own classloader for isolation.
- **Load-time bytecode transformation**: J2ME app classes are instrumented via ASM when loaded. `SystemCallInterceptor` redirects `Thread.sleep()` through `SpeedHelper` (for speed control) and `new Socket()` through `SystemCallHandler` (for network monitoring/redirection).
- **Reflection-based MicroEmulator integration**: MicroEmulator is loaded at runtime via classloader; all access to its internals uses reflection since the launcher can't compile against it directly.
- **Singleton services**: `NetworkService`, `SpeedService`, `GraphicsOptimizationService` are singletons shared across all instances.

## Entry Point

`me.kitakeyos.j2me.application.MainApplication.main()` - creates the singleton JFrame, initializes all services and UI tabs.

## Data Directory

Runtime state lives in `./data/`: properties files for apps, emulators, network rules, and launcher config; subdirectories for cloned JARs, icons, RMS data, and emulator JARs.

## Conventions

- **Commits**: Conventional commits (`feat:`, `fix:`, `docs:`, `refactor:`, `test:`, `chore:`)
- **Code style**: 4-space indent, UTF-8, LF line endings, 120 char max line length, same-line braces
- **Layer boundaries**: Domain must not depend on any other layer. Presentation must not depend on Infrastructure directly.
- **Branching**: `feature/name`, `fix/description`, `docs/topic`
