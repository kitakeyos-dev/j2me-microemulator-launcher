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
1. In the Instances tab, click **Browse** to select your `microemulator.jar` file

### Managing Applications
1. Go to **Applications** tab
2. Click **Add Application** to install a .jar or .jad file
3. Apps are automatically saved and persist across sessions

### Running Instances
1. Go to **Instances** tab
2. Select an application and set the number of instances
3. Configure display size (default: 240x320)
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
├── network_rules.properties    # Network redirection/proxy rules
├── apps/                       # Cloned JAR/JAD files
├── icons/                      # Application icons
├── rms/                        # Per-instance data
└── scripts/                    # Lua scripts
```

## License

MIT License - see [LICENSE](LICENSE) file.

## Acknowledgments

- Built using Java Swing for GUI
- Relies on MicroEmulator for J2ME emulation
- Inspired by the need to revive legacy Java mobile apps