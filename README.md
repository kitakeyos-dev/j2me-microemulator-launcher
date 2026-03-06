# J2ME MicroEmulator Launcher

[English](README.md) | [Tiếng Việt](README_VI.md)

## Overview

J2ME MicroEmulator Launcher is a powerful desktop application that enables running and managing multiple J2ME (Java 2 Micro Edition) application instances using MicroEmulator. Built with advanced bytecode caching and optimized memory management for fast startup and smooth operation.

## Features

### 📱 Application Management
- Smart installation with automatic metadata extraction from JAR/JAD manifests
- File cloning to prevent data loss
- Icon caching for instant display

### 🖥️ Multi-Instance Support
- Run unlimited emulator instances simultaneously
- Configurable display size (128-800px width, 128-1000px height)
- Input synchronization across all instances
- Individual instance control

### ⚙️ Emulator Management
- Install and manage multiple emulator JARs
- Per-emulator default display size configuration
- Select emulator from dropdown when creating instances
- Auto-creates default emulator from bundled `microemulator.jar`

### 🌐 Network Monitor
- **Connection Logging**: Monitor all socket connections with timestamps
- **Host/Port Redirection**: Redirect connections from one host:port to another
- **Proxy Support**: SOCKS and HTTP proxy with authentication (username/password)
- **Instance Filtering**: Apply rules to all instances or specific ones
- **Auto-persistence**: Rules are automatically saved and loaded

### 📜 Lua Scripting
- Automate interactions with running instances
- Built-in code editor with syntax highlighting
- Folder organization for scripts

### 🔌 Java Injection
- Load external JAR files to execute code against running instances
- Direct access to MIDlet classes via custom classloader chain
- Built-in logging system with real-time output
- Hot-reload support for rapid development
- See [docs/INJECTION.md](docs/INJECTION.md) for full guide

### ⚡ Performance
- Bytecode caching for instant subsequent startups
- Smart memory management with proper cleanup
- Cross-platform support (Windows, macOS, Linux)

## Requirements

- Java Runtime Environment (JRE) 8 or higher
- MicroEmulator JAR file ([Download](https://sourceforge.net/projects/microemulator/files/microemulator/2.0.4/))
- J2ME application files (.jar or .jad)

## Installation

### From Release
1. Download the latest release JAR from [Releases](https://github.com/kitakeyos-dev/j2me-microemulator-launcher/releases)
2. Run: `java -jar j2me-microemulator-launcher.jar`

### From Source
```bash
git clone https://github.com/kitakeyos-dev/j2me-microemulator-launcher.git
cd j2me-microemulator-launcher
mvn package
java -jar target/j2me-microemulator-launcher-*-jar-with-dependencies.jar
```

## Usage

### First-Time Setup
1. Go to the **Emulators** tab
2. The default `microemulator.jar` is auto-configured
3. To add more emulators, enter a name, browse for the JAR file, and click **Add Emulator**

### Managing Applications
1. Go to **Applications** tab
2. Click **Add Application** to install a .jar or .jad file
3. Apps are automatically saved and persist across sessions

### Running Instances
1. Go to **Instances** tab
2. Select an application and an emulator from the dropdowns
3. Set the number of instances and display size (auto-populated from emulator defaults)
4. Click **Create & Run**

### Network Monitor
1. Click **Network Monitor** button in Instances tab
2. **Connection Logs**: View all connection attempts
3. **Redirection Rules**: Add rules to redirect host:port
4. **Proxy Rules**: Configure SOCKS/HTTP proxy with optional authentication
5. Rules are auto-saved and loaded on startup

## Data Directory

```
./data/
├── j2me_launcher.properties    # Main configuration
├── j2me_apps.properties        # Installed applications
├── emulators.properties        # Emulator configurations
├── network_rules.properties    # Network redirection/proxy rules
├── apps/                       # Cloned JAR/JAD files
├── icons/                      # Application icons
├── rms/                        # Per-instance data
├── emulators/                  # Cloned emulator JARs
└── scripts/                    # Lua scripts
```

## License

MIT License - see [LICENSE](LICENSE) file.

## Acknowledgments

- Built using Java Swing for GUI
- Relies on MicroEmulator for J2ME emulation
- Inspired by the need to revive legacy Java mobile apps