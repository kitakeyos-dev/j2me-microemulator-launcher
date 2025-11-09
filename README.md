# J2ME MicroEmulator Launcher

[English](README.md) | [Tiếng Việt](README_VI.md)

## Overview

J2ME MicroEmulator Launcher is a desktop application built in Java that allows users to launch and manage multiple instances of J2ME (Java 2 Micro Edition) applications using the MicroEmulator. It provides an intuitive GUI for configuring, running, and arranging emulator windows, making it easier to test or run legacy mobile Java applications on modern desktops.

This project is ideal for developers, enthusiasts, or anyone working with J2ME apps who needs to simulate multiple devices simultaneously.

## Features

- **Multi-Instance Support**: Create and run multiple emulator instances from a single J2ME JAR or JAD file.
- **GUI Management**: User-friendly interface to add, remove, start, stop, and rearrange instances.
- **Automatic Window Arrangement**: Organizes emulator windows in a grid layout for better visibility.
- **Configuration Options**: Easily set the path to MicroEmulator JAR and select J2ME files.
- **State Tracking**: Monitors instance states (Created, Starting, Running, Stopped) with color-coded UI.
- **Cross-Platform**: Runs on any system with Java installed (tested on Windows, macOS, Linux).

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

## Usage

1. **Set MicroEmulator Path**: Go to Settings and browse for your `microemulator.jar` file.
2. **Select J2ME File**: Browse for your .jar or .jad file.
3. **Create Instances**: Choose the number of instances and click "Create Instances".
4. **Run Instances**: Click "Run All" or individual "Run" buttons.
5. **Manage Instances**: Stop, remove, or rearrange windows as needed.

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