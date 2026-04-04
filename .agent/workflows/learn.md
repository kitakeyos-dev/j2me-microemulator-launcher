---
description: Read documentation to understand J2ME Launcher project
---

# Learn Project Context

This workflow helps AI Agent understand the project through pre-created documentation.

## When to use
- When starting to work with the project
- When needing to understand a specific part of the codebase
- When needing context to write new code

## Documentation Files

### Core Documentation
1. **docs/README.md** - Project overview, architecture diagram, data flow
2. **docs/ARCHITECTURE.md** - 4 layers detail, package structure, class relationships

### Technical Deep-Dives
3. **docs/CLASSLOADER.md** - Custom ClassLoader, reverse delegation, bytecode caching
4. **docs/BYTECODE.md** - ASM transformation pipeline, stack manipulation, interceptions
5. **docs/REFLECTION.md** - Launch sequence via reflection, MicroEmulator classes accessed
6. **docs/NETWORK.md** - Socket interception, redirection/proxy rules, MonitoredSocket

### Feature Documentation
7. **docs/INJECTION.md** - Java injection API and guides
8. **docs/API.md** - Complete API reference for all key classes

### User Guides
9. **docs/GETTING_STARTED.md** - Installation, UI layout, troubleshooting
10. **docs/CONFIGURATION.md** - Properties files, data directories

## Instructions for AI Agent

### To understand project overview:
// turbo
Read `docs/README.md` and `docs/ARCHITECTURE.md`

### To understand ClassLoader and Bytecode:
// turbo
Read `docs/CLASSLOADER.md` and `docs/BYTECODE.md`

### To understand Network system:
// turbo
Read `docs/NETWORK.md`

### To understand Java injection:
// turbo
Read `docs/INJECTION.md`

### To understand emulator initialization:
// turbo
Read `docs/REFLECTION.md`

## Key Classes Quick Reference

| Component | Main Classes |
|-----------|-------------|
| Entry Point | `MainApplication` |
| Instance Model | `EmulatorInstance` |
| Class Loading | `EmulatorClassLoader`, `ClassPreprocessor` |
| Bytecode | `InstrumentationClassVisitor`, `SystemCallInterceptor`, `SystemCallHandler` |
| Network | `NetworkService`, `MonitoredSocket`, `RedirectionRule`, `ProxyRule` |
| Injection | `InjectionService`, `InjectionEntry`, `InjectionPanel` |
| Reflection | `ReflectionHelper`, `EmulatorReflectionHelper` |
