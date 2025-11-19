# J2ME MicroEmulator Launcher

[English](README.md) | [Tiếng Việt](README_VI.md)

[![Java](https://img.shields.io/badge/Java-8%2B-blue.svg)](https://www.oracle.com/java/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)
[![Maven](https://img.shields.io/badge/Maven-3.6%2B-red.svg)](https://maven.apache.org/)

## Overview

J2ME MicroEmulator Launcher is a high-performance desktop application built in Java that allows users to launch and manage multiple instances of J2ME (Java 2 Micro Edition) applications using the MicroEmulator. It features advanced bytecode instrumentation caching, classloader pre-warming, intelligent memory management, and **built-in Lua scripting** to deliver blazing-fast instance startup and optimal resource utilization.

This project is ideal for developers, enthusiasts, or anyone working with J2ME apps who needs to simulate multiple devices simultaneously with professional-grade performance and automation capabilities.

## Table of Contents

- [Key Features](#key-features)
- [Requirements](#requirements)
- [Installation](#installation)
- [Building from Source](#building-from-source)
- [Data Directory Structure](#data-directory-structure)
- [Usage](#usage)
- [Lua Scripting](#lua-scripting)
- [Technical Details](#technical-details)
- [Troubleshooting](#troubleshooting)
- [Development](#development)
- [Contributing](#contributing)
- [License](#license)
- [Acknowledgments](#acknowledgments)

## Key Features

### Application Management
- **Smart Installation**: Install and manage J2ME applications with automatic extraction of app name, icon, vendor, and version from JAR/JAD manifests
- **File Cloning**: JAR/JAD files are automatically copied to the application's data directory, preventing data loss if original files are deleted
- **Icon Caching**: Application icons are automatically extracted and cached locally for instant display

### Multi-Instance Support
- **Unlimited Instances**: Create and run unlimited emulator instances from installed J2ME applications
- **Tabbed Interface**: Three specialized tabs for Applications, Instances, and **Script** management
- **Configurable Display Size**: Set custom screen dimensions (width: 128-800px, height: 128-1000px) for each instance batch with default 240x320
- **Auto-Sorted Display**: Running instances are automatically sorted by instance ID in a responsive wrap layout
- **Input Synchronization**: Optionally sync mouse clicks and keyboard inputs across all running instances for parallel testing
- **Individual Control**: Start, stop, and manage each instance independently with dedicated controls

### Lua Scripting ✨ NEW
- **Integrated Lua Environment**: Built-in Lua interpreter (LuaJ 3.0.1) for automation and testing
- **Code Editor**: Syntax-highlighted Lua editor with code completion and line numbers
- **Java Integration**: Access Java classes and emulator instances directly from Lua scripts via `luajava` library
- **Script Management**: Save, load, and organize multiple Lua scripts with persistent storage
- **Instance Control**: Programmatically control emulator instances using Lua scripts
- **Real-time Output**: View script execution results in color-coded output panel
- **Auto-completion**: Context-aware code completion for Lua keywords, functions, and libraries

### Performance Optimizations ⚡
- **Bytecode Caching**: Instrumented classes are cached and shared across instances, eliminating redundant processing
- **ClassLoader Pre-warming**: Emulator classes are pre-loaded at startup for instant first launch
- **ThreadLocal Context**: Dynamic instance ID injection enables bytecode sharing without memory conflicts
- **Smart Memory Management**: Proper resource cleanup and garbage collection prevent memory leaks

### User Experience
- **Portable Data Storage**: All configuration, application data, and scripts stored in local `./data/` and `./lua_scripts/` directories
- **Responsive UI**: Background operations ensure the interface never freezes, with wrap layout that adapts to window resizing
- **Automatic Cleanup**: Instances are properly disposed when stopped, freeing system resources
- **Toast Notifications**: Non-intrusive notifications for actions like instance creation and synchronization toggle
- **Cross-Platform**: Runs seamlessly on Windows, macOS, and Linux

## Requirements

- **Java Runtime Environment (JRE)**: Version 8 or higher
- **MicroEmulator JAR**: Download from [SourceForge](https://sourceforge.net/projects/microemulator/files/microemulator/2.0.4/) or official sources
- **J2ME Application Files**: `.jar` or `.jad` files
- **Maven** (for building from source): Version 3.6 or higher

## Installation

### Option 1: Download Pre-built JAR (Recommended)

1. Download the latest release from the [Releases](https://github.com/kitakeyos-dev/j2me-microemulator-launcher/releases) page
2. Run the application:
   ```bash
   java -jar j2me-launcher-with-dependencies.jar
   ```

### Option 2: Build from Source

See [Building from Source](#building-from-source) section below.

## Building from Source

### Prerequisites

- Java Development Kit (JDK) 8 or higher
- Maven 3.6 or higher
- Git

### Build Steps

1. **Clone the repository**:
   ```bash
   git clone https://github.com/kitakeyos-dev/j2me-microemulator-launcher.git
   cd j2me-microemulator-launcher
   ```

2. **Build with Maven**:
   ```bash
   mvn clean package
   ```

   This will:
   - Compile all source code
   - Run tests (if any)
   - Create a fat JAR with all dependencies included
   - Output file: `target/J2MELauncher-1.0-SNAPSHOT-jar-with-dependencies.jar`

3. **Run the application**:
   ```bash
   java -jar target/J2MELauncher-1.0-SNAPSHOT-jar-with-dependencies.jar
   ```

### Development Build

For development with auto-compilation:
```bash
mvn clean compile exec:java -Dexec.mainClass="me.kitakeyos.j2me.MainApplication"
```

## Data Directory Structure

The launcher stores all data in local directories:

```
./
├── data/                          # Application data
│   ├── j2me_launcher.properties   # MicroEmulator configuration
│   ├── j2me_apps.properties       # Installed applications list
│   ├── apps/                      # Cloned JAR/JAD files
│   │   ├── <app-id>.jar
│   │   └── <app-id>.jad
│   ├── icons/                     # Extracted application icons
│   │   └── <app-id>.png
│   └── rms/                       # Record Management System (per-instance data)
│       ├── 1/                     # Instance #1 data
│       ├── 2/                     # Instance #2 data
│       └── ...
└── lua_scripts/                   # Lua scripts
    ├── example.lua
    ├── automation.lua
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
   - Click "Add Application" to browse for your `.jar` or `.jad` file
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

## Lua Scripting

### Overview

The Script tab provides a powerful Lua scripting environment for automation and testing. You can write Lua scripts to interact with emulator instances, automate repetitive tasks, and perform advanced testing scenarios.

### Getting Started with Lua Scripts

1. **Navigate to Script Tab**: Click on the "Script" tab in the main window

2. **Create a New Script**: Click the "New" button to create a new script

3. **Write Your Script**: Use the code editor with syntax highlighting and auto-completion

4. **Save Your Script**: Click "Save" to persist your script (saved in `./lua_scripts/` directory)

5. **Run Your Script**: Click "Run" to execute the script and view output in the output panel

### Basic Lua Script Example

```lua
-- Print a simple message
print("Hello from Lua!")

-- Access Java classes
local ArrayList = luajava.bindClass("java.util.ArrayList")
local list = luajava.new("java.util.ArrayList")

-- Check type
print("List type: " .. type(list))
```

### Accessing Emulator Instances

You can access running emulator instances from Lua scripts:

```lua
-- Get instance by ID (requires instance selector in UI)
-- Note: Instance integration is done via the UI dropdown

-- Example: Working with Java classes
local String = luajava.bindClass("java.lang.String")
local str = luajava.new("java.lang.String", "Hello from Lua!")
print(str:toString())
```

### Available Lua Libraries

- **Standard Lua Libraries**: `string`, `table`, `math`, `io`, `os`
- **LuaJava Integration**: `luajava.bindClass()`, `luajava.new()`
- **Built-in Functions**: `print()`, `type()`, `tostring()`, `pairs()`, `ipairs()`

### Code Completion

Press `Ctrl+Space` to trigger auto-completion:
- Lua keywords: `if`, `then`, `end`, `for`, `while`, `function`, `local`
- Built-in functions: `print()`, `type()`, `tostring()`, `pairs()`, `ipairs()`
- Library methods: `string.len()`, `table.insert()`, `math.abs()`
- Java class imports when typing `import`

### Script Management

- **New**: Create a new empty script
- **Save**: Save current script to disk
- **Delete**: Remove selected script
- **Load**: Scripts are automatically loaded from `./lua_scripts/` directory on startup

### Output Panel

The output panel displays:
- **[INFO]**: Informational messages (blue)
- **[SUCCESS]**: Successful operations (green)
- **[ERROR]**: Error messages (red)
- **Normal output**: Script print statements (black)

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

### Project Structure

```
src/main/java/me/kitakeyos/j2me/
├── config/                 # Application configuration
├── core/
│   ├── bytecode/          # Bytecode instrumentation
│   ├── classloader/       # Custom classloaders
│   ├── lifecycle/         # Instance lifecycle management
│   ├── resource/          # Resource management
│   └── thread/            # Thread management
├── model/                 # Data models
├── script/                # Lua scripting engine
│   ├── core/             # Script execution
│   ├── lib/              # Java-Lua bridge
│   ├── model/            # Script models
│   ├── storage/          # Script persistence
│   └── ui/               # Script editor UI
│       └── component/    # UI components
├── service/               # Business logic services
├── ui/                    # User interface
│   ├── builder/          # UI builders
│   ├── component/        # Reusable components
│   ├── dialog/           # Dialogs
│   ├── layout/           # Custom layouts
│   └── panel/            # Main panels
└── util/                  # Utility classes
```

### Dependencies

- **ASM 3.1**: Bytecode manipulation and instrumentation
- **LuaJ 3.0.1**: Lua interpreter for Java
- **Gson 2.10.1**: JSON serialization/deserialization
- **Swing**: GUI framework (built-in)

## Troubleshooting

### Common Issues

#### 1. "Failed to load MicroEmulator"
- **Cause**: Invalid or missing `microemulator.jar` file
- **Solution**: Go to Settings and select a valid MicroEmulator JAR file

#### 2. OutOfMemoryError
- **Cause**: Too many instances running simultaneously
- **Solution**:
  - Increase JVM heap size: `java -Xmx2G -jar j2me-launcher.jar`
  - Stop unused instances
  - Reduce number of instances created at once

#### 3. "ClassNotFoundException" in Lua scripts
- **Cause**: Trying to access a class that doesn't exist or isn't in the classpath
- **Solution**: Check class name spelling and ensure the class is available

#### 4. Script execution hangs
- **Cause**: Infinite loop or blocking operation in Lua script
- **Solution**: Review script logic and avoid blocking operations

#### 5. Instances not responding
- **Cause**: Instance may have crashed or frozen
- **Solution**: Click "Stop" button and recreate the instance

### Getting Help

If you encounter issues not listed here:
1. Check the [Issues](https://github.com/kitakeyos-dev/j2me-microemulator-launcher/issues) page
2. Create a new issue with:
   - Your Java version (`java -version`)
   - Operating system
   - Steps to reproduce the problem
   - Error messages or logs

## Development

### Setting Up Development Environment

1. **Clone the repository**:
   ```bash
   git clone https://github.com/kitakeyos-dev/j2me-microemulator-launcher.git
   ```

2. **Import into IDE**:
   - IntelliJ IDEA: Open as Maven project
   - Eclipse: Import as Existing Maven Project
   - NetBeans: Open Project

3. **Install dependencies**:
   ```bash
   mvn clean install
   ```

4. **Run from IDE**: Execute `me.kitakeyos.j2me.MainApplication.main()`

### Code Style

- Follow standard Java naming conventions
- Use meaningful variable and method names
- Add Javadoc comments for public classes and methods
- Keep methods focused and concise
- Use proper exception handling with descriptive error messages

### Testing

Run tests with Maven:
```bash
mvn test
```

### Building Release

To create a release build:
```bash
mvn clean package
```

The fat JAR will be created in `target/J2MELauncher-1.0-SNAPSHOT-jar-with-dependencies.jar`

## Contributing

Contributions are welcome! Please follow these steps:

1. **Fork the repository**
2. **Create a feature branch**: `git checkout -b feature/your-feature-name`
3. **Make your changes**
4. **Test thoroughly**
5. **Commit your changes**: `git commit -am 'Add new feature'`
6. **Push to the branch**: `git push origin feature/your-feature-name`
7. **Create a Pull Request**

### Contribution Guidelines

- Report bugs or suggest features via [GitHub Issues](https://github.com/kitakeyos-dev/j2me-microemulator-launcher/issues)
- Ensure code follows project coding standards
- Add tests for new features
- Update documentation as needed
- Keep pull requests focused on a single feature or bug fix

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.

## Acknowledgments

- Built using **Swing** for GUI
- Relies on **MicroEmulator** for J2ME emulation
- Uses **LuaJ** for Lua scripting integration
- Bytecode manipulation powered by **ASM**
- Inspired by the need to revive legacy Java mobile apps

## Contact

- **Repository**: [github.com/kitakeyos-dev/j2me-microemulator-launcher](https://github.com/kitakeyos-dev/j2me-microemulator-launcher)
- **Issues**: [github.com/kitakeyos-dev/j2me-microemulator-launcher/issues](https://github.com/kitakeyos-dev/j2me-microemulator-launcher/issues)

---

Made with ❤️ for the J2ME community
