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
- **Tabbed Interface**: Two specialized tabs for Applications and Instances management
- **Configurable Display Size**: Set custom screen dimensions (width: 128-800px, height: 128-1000px) for each instance batch with default 240x320
- **Auto-Sorted Display**: Running instances are automatically sorted by instance ID in a responsive wrap layout
- **Input Synchronization**: Optionally sync mouse clicks and keyboard inputs across all running instances for parallel testing
- **Individual Control**: Start, stop, and manage each instance independently with dedicated controls

### Performance Optimizations ⚡
- **Bytecode Caching**: Instrumented classes are cached and shared across instances, eliminating redundant processing
- **ClassLoader Pre-warming**: Emulator classes are pre-loaded at startup for instant first launch
- **ThreadLocal Context**: Dynamic instance ID injection enables bytecode sharing without memory conflicts
- **Smart Memory Management**: Proper resource cleanup and garbage collection prevent memory leaks

### User Experience
- **Portable Data Storage**: All configuration and application data stored in local `./data/` directory
- **Responsive UI**: Background operations ensure the interface never freezes, with wrap layout that adapts to window resizing
- **Automatic Cleanup**: Instances are properly disposed when stopped, freeing system resources
- **Toast Notifications**: Non-intrusive notifications for actions like instance creation and synchronization toggle
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

3. **Configure and Create Instances** (Instances tab):
   - Select an installed application from the dropdown menu
   - Choose the number of instances you want to create (1-100)
   - Set custom display dimensions:
     - Width: 128-800 pixels (default: 240)
     - Height: 128-1000 pixels (default: 320)
   - Click "Create & Run" to create and automatically start instances
   - Instances appear in the running instances panel below with sorted order

4. **Enable Input Synchronization** (optional):
   - Check "Sync Mouse & Keyboard Input" to enable parallel testing
   - When enabled, any mouse click or keyboard input on one instance will be replicated to all other running instances
   - Useful for testing the same interaction across multiple device configurations simultaneously
   - Uncheck to disable synchronization and control instances independently

5. **View Running Instances**:
   - All running instances are displayed in sorted order (1, 2, 3...) in a responsive wrap layout
   - Each instance shows its ID with a dedicated "Stop" button
   - Layout automatically adjusts when resizing the window
   - Instances wrap to fill available horizontal space efficiently

6. **Manage Instances**:
   - Stop instances individually with the "Stop" button on each instance
   - Click "Stop All" to stop all running instances at once
   - When stopped, instances are properly disposed and all resources are freed

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

#### Input Synchronization
- `InputSynchronizer` service manages event broadcasting across all running instances
- Mouse and keyboard listeners are attached recursively to all components in each instance's display
- Event dispatching uses `ConcurrentHashMap`-based tracking to prevent infinite loops
- Coordinate conversion ensures mouse events are dispatched to the correct relative position
- Component hierarchy matching ensures keyboard events target the corresponding component
- All events are dispatched asynchronously via `SwingUtilities.invokeLater()` to prevent UI blocking
- Listeners are automatically attached/detached when instances start/stop

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