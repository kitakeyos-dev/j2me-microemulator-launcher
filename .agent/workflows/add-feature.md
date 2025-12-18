---
description: Add new feature to J2ME Launcher with correct architecture
---

# Add Feature

Workflow to guide adding new features following the project's Clean Architecture.

## Step 1: Understand Architecture
// turbo
Read `docs/ARCHITECTURE.md` to understand 4 layers:
- Presentation (UI) → Application → Domain ← Infrastructure

## Step 2: Identify layer
Which layer does the new feature belong to?

| Layer | Code Type |
|-------|-----------|
| Domain | Models, Services, Interfaces, Business logic |
| Infrastructure | Repository implementations, ByteCode, Network I/O |
| Application | Orchestration, Coordinators, Config |
| Presentation | UI Panels, Dialogs, Components |

## Step 3: Create files following package structure
```
src/main/java/me/kitakeyos/j2me/
├── domain/<feature>/
│   ├── model/       # Data classes
│   ├── service/     # Business logic
│   └── repository/  # Interfaces
├── infrastructure/<feature>/
│   └── ...          # Implementations
├── application/<feature>/
│   └── ...          # Coordination
└── presentation/<feature>/
    └── ...          # UI
```

## Step 4: Follow existing patterns
- Models: See `EmulatorInstance.java`
- Services: See `NetworkService.java`
- Repositories: See `ApplicationRepository.java` interface + `ApplicationRepositoryImpl.java`
- UI Panels: See `ApplicationsPanel.java`, `InstancesPanel.java`

## Step 5: Update documentation
After completion, update related docs:
- `docs/API.md` - Add new API
- `docs/ARCHITECTURE.md` - If structure changed
