# J2ME MicroEmulator Launcher

[English](README.md) | [Tiếng Việt](README_VI.md)

## Overview

J2ME MicroEmulator Launcher is a high-performance desktop application built in Java that allows users to launch and manage multiple instances of J2ME (Java 2 Micro Edition) applications using the MicroEmulator. It features advanced bytecode instrumentation caching, classloader pre-warming, and intelligent memory management to deliver blazing-fast instance startup and optimal resource utilization.

This project is ideal for developers, enthusiasts, or anyone working with J2ME apps who needs to simulate multiple devices simultaneously with professional-grade performance.

## Key Features

### Application Management
- **Smart Installation**: Install and manage J2ME applications with automatic extraction of app name, icon, vendor, and version from JAR/JAD manifests
- **File Cloning**: JAR/JAD files are automatically copied to the application's data directory, preventing data loss if original files are deleted
- **Icon Caching**: Application icons are automatically extracted and cached locally for instant display

### Multi-Instance Support
- **Unlimited Instances**: Create and run unlimited emulator instances from installed J2ME applications
- **Tabbed Interface**: Three specialized tabs for Applications, Instances, and Running Instances management
- **Auto-Sorted Display**: Running instances are automatically sorted by instance ID for easy navigation
- **State Tracking**: Real-time monitoring of instance states (Created, Starting, Running, Stopped) with color-coded UI
- **Individual Control**: Start, stop, and manage each instance independently

### Performance Optimizations ⚡
- **Bytecode Caching**: Instrumented classes are cached and shared across instances, eliminating redundant processing
- **ClassLoader Pre-warming**: Emulator classes are pre-loaded at startup for instant first launch
- **ThreadLocal Context**: Dynamic instance ID injection enables bytecode sharing without memory conflicts
- **Smart Memory Management**: Proper resource cleanup and garbage collection prevent memory leaks

### User Experience
- **Portable Data Storage**: All configuration and application data stored in local `./data/` directory
- **Responsive UI**: Background operations ensure the interface never freezes
- **Automatic Cleanup**: Instances are properly disposed when stopped, freeing system resources
- **Cross-Platform**: Runs seamlessly on Windows, macOS, and Linux

## Requirements

- Java Runtime Environment (JRE) 8 or higher.
- MicroEmulator JAR file (download from [SourceForge](https://sourceforge.net/projects/microemulator/files/microemulator/2.0.4/) or official sources).
- J2ME application files (.jar or .jad).

## Installation

1. Clone the repository:
   ```
   git clone https://github.com/kitakeyos-dev/j2me-microemulator-launcher.git
   ```

2. Build the project using your preferred Java IDE (e.g., IntelliJ IDEA, Eclipse) or via command line with Maven/Gradle if configured. (Note: The provided code is plain Java; add build tools if needed.)

3. Run the main class: `me.kitakeyos.j2me.MainApplication`.

Alternatively, package it into a JAR and run:
```
java -jar j2me-launcher.jar
```

## Data Directory Structure

The launcher stores all data in a local `./data/` directory:

```
./data/
├── j2me_launcher.properties  # MicroEmulator configuration
├── j2me_apps.properties      # Installed applications list
├── apps/                      # Cloned JAR/JAD files
│   ├── <app-id>.jar
│   └── <app-id>.jad
├── icons/                     # Extracted application icons
│   └── <app-id>.png
└── rms/                       # Record Management System (per-instance data)
    ├── 1/                     # Instance #1 data
    ├── 2/                     # Instance #2 data
    └── ...
```

This structure ensures:
- **Portability**: Move the entire application folder anywhere
- **Safety**: Original files can be deleted without affecting installed apps
- **Isolation**: Each instance has its own data directory
- **Organization**: All data in one clean, manageable location

## Usage

### First-Time Setup

1. **Set MicroEmulator Path**:
   - Go to Settings (⚙️ icon in Instances tab)
   - Browse for your `microemulator.jar` file
   - The classloader will automatically pre-warm common classes for faster startup

### Managing Applications

2. **Install Applications** (Applications tab):
   - Click "Add Application" to browse for your .jar or .jad file
   - The app's name, icon, vendor, and version are automatically extracted from the manifest
   - Installed applications are saved and will persist across sessions
   - View all installed applications with their details and icons
   - Click "Remove" to uninstall any application

### Creating and Running Instances

3. **Create Instances** (Instances tab):
   - Select an installed application from the dropdown menu
   - Choose the number of instances you want to create
   - Click "Create Instances"
   - Instances appear in the list with color-coded status

4. **Run Instances**:
   - Click "Run All" to start all created instances
   - Or click individual "Run" buttons for specific instances
   - First instance startup is fast due to pre-warming
   - Subsequent instances start even faster with cached bytecode

5. **View Running Instances** (Running Instances tab):
   - All running instances are displayed in sorted order (1, 2, 3...)
   - Instances automatically arrange in a responsive grid layout
   - Window resizing automatically adjusts the layout

6. **Manage Instances**:
   - Stop instances individually with the "Stop" button or click "Stop All"
   - Remove instances you no longer need with the "Remove" button
   - Move instances up/down to change their order in the Instances tab
   - When stopped, instances are automatically removed from the Running Instances tab and all resources are freed

## Technical Details

### Performance Architecture

#### Bytecode Instrumentation Caching
- Emulator classes are instrumented once and cached in memory
- Instrumented bytecode is shared across all instances via `InstrumentedClassCache`
- `ThreadLocal<Integer>` stores the current instance ID for dynamic runtime lookup
- First instance: Instruments and caches classes
- Subsequent instances: Instant startup using cached bytecode

#### ClassLoader Pre-warming
- When the application starts, 10 critical emulator classes are pre-loaded
- Pre-warming runs in a background thread to avoid blocking the UI
- Common classes (Main, Config, DeviceFactory, etc.) are ready before the first instance launches
- Typical pre-warming time: 100-300ms

#### Memory Management
- Stopped instances trigger comprehensive cleanup:
  - JFrame disposal releases native window resources
  - Component hierarchies are cleared to break circular references
  - ThreadLocal contexts are properly cleaned
  - All object references are nulled for garbage collection
- System.gc() is called after cleanup to suggest immediate collection
- No memory leaks even after running/stopping hundreds of instances

#### Instance Isolation
- Each instance has its own `EmulatorClassLoader` for class isolation
- Each instance has its own RMS (Record Management System) directory
- System calls (System.exit, Config.initMEHomePath) are intercepted and routed per-instance
- Static fields are NOT shared between instances (separate class namespaces)

For detailed code structure, refer to the source files in the `src` directory.

## Contributing

Contributions are welcome! Please fork the repo and submit a pull request. For major changes, open an issue first to discuss.

- Report bugs or suggest features via GitHub Issues.
- Ensure code follows standard Java conventions.

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.

## Acknowledgments

- Built using Swing for GUI.
- Relies on MicroEmulator for J2ME emulation.
- Inspired by the need to revive legacy Java mobile apps.