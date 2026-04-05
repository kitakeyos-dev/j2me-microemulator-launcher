# Getting Started

> **Purpose**: Complete installation and setup guide.

---

## 📋 System Requirements

| Requirement | Minimum | Recommended |
|-------------|---------|-------------|
| **Java** | JRE 8 | JDK 11+ |
| **RAM** | 256 MB | 512 MB+ |
| **OS** | Windows 7+ / Linux / macOS | Windows 10+ |
| **MicroEmulator** | v2.0.4 | v2.0.4 |

---

## 📥 Installation

### Option 1: Download Release

1. Go to [Releases](https://github.com/kitakeyos-dev/j2me-microemulator-launcher/releases)
2. Download `j2me-microemulator-launcher.jar`
3. Download [MicroEmulator 2.0.4](https://sourceforge.net/projects/microemulator/files/microemulator/2.0.4/)
4. Extract `microemulator.jar` from the download

### Option 2: Build from Source

```bash
# Clone repository
git clone https://github.com/kitakeyos-dev/j2me-microemulator-launcher.git
cd j2me-microemulator-launcher

# Build with Maven
mvn clean package

# Output: target/j2me-microemulator-launcher-*-jar-with-dependencies.jar
```

---

## 🚀 First-Time Setup

### Step 1: Run the Application

```bash
java -jar j2me-microemulator-launcher.jar
```

### Step 2: Configure MicroEmulator

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  1. Go to the "Emulators" tab                                               │
│                                                                              │
│  2. The default emulator (microemulator.jar) is auto-configured             │
│                                                                              │
│  3. To add more emulators, enter a name, browse for the JAR, click "Add"   │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Step 3: Install a J2ME Application

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  1. Go to "Applications" tab                                                │
│                                                                              │
│  2. Click "Install"                                                         │
│                                                                              │
│  3. Browse to a J2ME .jar or .jad file                                      │
│                                                                              │
│  4. App appears in grid with icon                                           │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Step 4: Run an Emulator Instance

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  1. Go to "Instances" tab                                                   │
│                                                                              │
│  2. Select an application from dropdown                                     │
│                                                                              │
│  3. Set number of instances (1-10)                                          │
│                                                                              │
│  4. Set display size (default: 240x320)                                     │
│                                                                              │
│  5. Click "Create Instances"                                                │
│                                                                              │
│  6. Emulator appears as new tab                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 🖥️ User Interface Overview

### Main Window Layout

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  File   Settings   Help                                    [_] [□] [X]      │
├─────────────────────────────────────────────────────────────────────────────┤
│  Menu bar: Settings (language selection)                                     │
│  ┌──────────────┬──────────┬───────────────┬────────────┬────────────┐      │
│  │ Applications │ Emulators│   Instances   │ Instance 1 │ Instance 2 │      │
│  └──────────────┴──────────┴───────────────┴────────────┴────────────┘      │
│                                                                              │
│  ┌─────────────────────────────────────────────────────────────────────────┐│
│  │                                                                          ││
│  │                         [ Tab Content Here ]                             ││
│  │                                                                          ││
│  └─────────────────────────────────────────────────────────────────────────┘│
│                                                                              │
│  ┌─────────────────────────────────────────────────────────────────────────┐│
│  │  Status: Ready                                          CPU: 5%  RAM: 50M││
│  └─────────────────────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────────────────────┘
```

### Applications Tab

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  Applications Tab                                                            │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐                     │
│  │  [Icon]  │  │  [Icon]  │  │  [Icon]  │  │  [Icon]  │                     │
│  │          │  │          │  │          │  │          │                     │
│  │  Game 1  │  │  Game 2  │  │  Chat    │  │  [+Add]  │                     │
│  │  v1.0    │  │  v2.1    │  │  v1.5    │  │          │                     │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘                     │
│                                                                              │
│  [Install]  [Remove]  [Refresh]                                             │
└─────────────────────────────────────────────────────────────────────────────┘

Right-click app → Context menu:
  - Run (1 instance)
  - Run multiple...
  - Remove
  - Show details
```

### Instances Tab

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  Instances Tab                                                               │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  Application: [Game 1            ▼]                                          │
│                                                                              │
│  Instances:   [1    ] [+] [-]                                                │
│                                                                              │
│  Display:     Width: [240  ]  Height: [320  ]                               │
│               □ Full display mode                                            │
│                                                                              │
│  [Create Instances]    [Network Monitor]    [System Monitor]                │
│                                                                              │
├─────────────────────────────────────────────────────────────────────────────┤
│  Running Instances:                                                          │
│                                                                              │
│  ┌─────────────────┬─────────────────┬─────────────────┐                    │
│  │ Instance #1     │ Instance #2     │ Instance #3     │                    │
│  │ Game 1          │ Game 1          │ Chat            │                    │
│  │ State: RUNNING  │ State: RUNNING  │ State: RUNNING  │                    │
│  │ [Stop] [Focus]  │ [Stop] [Focus]  │ [Stop] [Focus]  │                    │
│  └─────────────────┴─────────────────┴─────────────────┘                    │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## ⚡ Quick Actions

### Run Single Instance

1. Right-click app in Applications tab
2. Select "Run"
3. Emulator opens in new tab

### Run Multiple Instances

1. Right-click app in Applications tab
2. Select "Run multiple..."
3. Enter number (1-10)
4. Each instance opens in separate tab

### Monitor Network

1. Click "Network Monitor" button in Instances tab
2. View connection logs
3. Add redirection rules
4. Add proxy rules

> **Note**: Packet capture must be enabled per-instance via the Actions menu first.

---

## 🔧 Troubleshooting

### "MicroEmulator path not set"

**Solution**: Go to the Emulators tab and ensure an emulator JAR is configured.

### "Class not found" when starting instance

**Possible causes**:
1. Wrong MicroEmulator version (need 2.0.4)
2. Corrupted microemulator.jar
3. Java version incompatibility

**Solution**: Re-download MicroEmulator 2.0.4

### Instance won't start

**Check**:
1. Is the J2ME JAR file valid?
2. Does the JAR contain a MIDlet class?
3. Check console for error messages

### Network rules not working

**Check**:
1. Is the rule enabled?
2. Is instanceId set to -1 (all) or correct instance number?
3. Does the host/port exactly match?

---

## 📁 Data Directory Structure

After first run, the `data/` directory is created:

```
data/
├── j2me_launcher.properties     # Settings
├── j2me_apps.properties         # Installed apps
├── network_rules.properties     # Network rules
├── apps/                        # Copied JARs
├── icons/                       # App icons
├── rms/                         # Instance data
│   ├── 1/                       # Instance #1
│   └── 2/                       # Instance #2
```

---

## 🔗 Next Steps

After installation:

1. [CONFIGURATION.md](CONFIGURATION.md) - Detailed configuration options
2. [NETWORK.md](NETWORK.md) - Network monitoring and proxy
3. [INJECTION.md](INJECTION.md) - Java injection guide
4. [ARCHITECTURE.md](ARCHITECTURE.md) - Understanding the codebase
