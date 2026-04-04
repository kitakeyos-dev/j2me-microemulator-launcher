# Configuration

> **Purpose**: Complete documentation of all configuration files and options.

---

## 📁 Configuration Files

All configuration files are stored in the `data/` directory (created automatically):

```
data/
├── j2me_launcher.properties     # Main application settings
├── j2me_apps.properties         # Installed J2ME applications
└── network_rules.properties     # Network redirection/proxy rules
```

---

## 🔧 j2me_launcher.properties

Main application configuration.

### File Location

```
data/j2me_launcher.properties
```

### Properties

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `microemulatorPath` | String | `""` | Path to microemulator.jar |
| `defaultDisplayWidth` | Integer | `240` | Default emulator width |
| `defaultDisplayHeight` | Integer | `320` | Default emulator height |

### Example File

```properties
# Path to MicroEmulator JAR
microemulatorPath=C:/tools/microemulator.jar

# Default display dimensions
defaultDisplayWidth=240
defaultDisplayHeight=320

```

### Code Reference

```java
public class ApplicationConfig {
    private String microemulatorPath;
    private int defaultDisplayWidth = 240;
    private int defaultDisplayHeight = 320;
    
    public void load() {
        Properties props = new Properties();
        props.load(new FileInputStream(CONFIG_PATH));
        
        microemulatorPath = props.getProperty("microemulatorPath", "");
        defaultDisplayWidth = Integer.parseInt(
            props.getProperty("defaultDisplayWidth", "240"));
        // ...
    }
}
```

---

## 🔧 j2me_apps.properties

Installed J2ME applications.

### File Location

```
data/j2me_apps.properties
```

### Properties Per App

| Property | Type | Description |
|----------|------|-------------|
| `app.<index>.name` | String | Application name |
| `app.<index>.path` | String | Path to JAR file (in data/apps/) |
| `app.<index>.iconPath` | String | Path to icon (in data/icons/) |
| `app.<index>.vendor` | String | Vendor/developer |
| `app.<index>.version` | String | Version string |

### Example File

```properties
# Application count
app.count=2

# App 0
app.0.name=Mobile Game
app.0.path=apps/mobile_game.jar
app.0.iconPath=icons/mobile_game.png
app.0.vendor=Game Company
app.0.version=1.0

# App 1
app.1.name=Chat App
app.1.path=apps/chat_app.jar
app.1.iconPath=icons/chat_app.png
app.1.vendor=Unknown
app.1.version=2.1
```

### Code Reference

```java
public class ApplicationRepositoryImpl implements ApplicationRepository {
    
    @Override
    public List<J2meApplication> findAll() {
        Properties props = new Properties();
        props.load(new FileInputStream(APPS_PATH));
        
        int count = Integer.parseInt(props.getProperty("app.count", "0"));
        List<J2meApplication> apps = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            J2meApplication app = new J2meApplication();
            app.setName(props.getProperty("app." + i + ".name"));
            app.setPath(props.getProperty("app." + i + ".path"));
            // ...
            apps.add(app);
        }
        
        return apps;
    }
}
```

---

## 🔧 network_rules.properties

Network redirection and proxy rules.

### File Location

```
data/network_rules.properties
```

### Redirection Rules

| Property | Type | Description |
|----------|------|-------------|
| `redirect.count` | Integer | Number of redirection rules |
| `redirect.<index>.originalHost` | String | Host to match |
| `redirect.<index>.originalPort` | Integer | Port to match |
| `redirect.<index>.targetHost` | String | Redirect to this host |
| `redirect.<index>.targetPort` | Integer | Redirect to this port |
| `redirect.<index>.instanceId` | Integer | Instance ID (-1 = all) |
| `redirect.<index>.enabled` | Boolean | Rule enabled |

### Proxy Rules

| Property | Type | Description |
|----------|------|-------------|
| `proxy.count` | Integer | Number of proxy rules |
| `proxy.<index>.type` | String | `SOCKS` or `HTTP` |
| `proxy.<index>.host` | String | Proxy server host |
| `proxy.<index>.port` | Integer | Proxy server port |
| `proxy.<index>.instanceId` | Integer | Instance ID (-1 = all) |
| `proxy.<index>.username` | String | Auth username (optional) |
| `proxy.<index>.password` | String | Auth password (optional) |
| `proxy.<index>.enabled` | Boolean | Rule enabled |

### Example File

```properties
# Redirection rules
redirect.count=2

redirect.0.originalHost=game.server.com
redirect.0.originalPort=8080
redirect.0.targetHost=localhost
redirect.0.targetPort=9000
redirect.0.instanceId=-1
redirect.0.enabled=true

redirect.1.originalHost=api.server.com
redirect.1.originalPort=443
redirect.1.targetHost=127.0.0.1
redirect.1.targetPort=8443
redirect.1.instanceId=1
redirect.1.enabled=true

# Proxy rules
proxy.count=1

proxy.0.type=SOCKS
proxy.0.host=127.0.0.1
proxy.0.port=1080
proxy.0.instanceId=-1
proxy.0.username=
proxy.0.password=
proxy.0.enabled=true
```

---

## 📁 Data Directories

### Directory Structure

```
data/
├── apps/                        # Copied J2ME JAR files
│   ├── game1.jar
│   └── game2.jar
│
├── icons/                       # Application icons
│   ├── game1.png
│   └── game2.png
│
├── rms/                         # Per-instance RMS data
│   ├── 1/                       # Instance #1 data
│   │   └── ...
│   └── 2/                       # Instance #2 data
│       └── ...
│
```

### apps/ Directory

When you install a J2ME app:
1. JAR file is COPIED to `data/apps/`
2. Icon is extracted and saved to `data/icons/`
3. Metadata is saved to `j2me_apps.properties`

This ensures the original file can be moved/deleted without breaking the launcher.

### rms/ Directory

Each emulator instance has its own RMS (Record Management System) directory:

```
data/rms/<instanceId>/
```

This is where MIDlet data is persisted (game saves, settings, etc.).

The directory is created automatically when:
- `SystemCallHandler.initMEHomePath(instanceId)` is called
- (Intercepted from `Config.initMEHomePath()` in MicroEmulator)

---

## 🔧 Environment Variables

### DATA_DIR Override

Set environment variable to override data directory location:

```bash
# Windows
set J2ME_LAUNCHER_DATA=C:\my\custom\path
java -jar j2me-launcher.jar

# Linux/macOS
export J2ME_LAUNCHER_DATA=/my/custom/path
java -jar j2me-launcher.jar
```

### Code Reference

```java
public class ApplicationConfig {
    public static final String DATA_DIR;
    
    static {
        String envDir = System.getenv("J2ME_LAUNCHER_DATA");
        if (envDir != null && !envDir.isEmpty()) {
            DATA_DIR = envDir;
        } else {
            DATA_DIR = "data";
        }
    }
}
```

---

## 💡 Configuration Tips

### First-Time Setup

1. **Set MicroEmulator path**:
   - Open Settings dialog
   - Browse to microemulator.jar
   - Click Save

2. **Configure display size**:
   - Default is 240x320 (QVGA)
   - Common sizes: 176x220, 240x320, 320x480

### Multiple Configurations

Create separate `data/` directories for different setups:

```
my-launcher/
├── j2me-launcher.jar
├── data-games/
│   └── ...
└── data-apps/
    └── ...
```

Run with environment variable:
```bash
set J2ME_LAUNCHER_DATA=data-games
java -jar j2me-launcher.jar
```

### Backup Configuration

Important files to backup:
- `j2me_apps.properties` - installed apps list
- `network_rules.properties` - network rules
- `apps/` - J2ME JAR files
- `rms/` - instance data (game saves)

---

## 🔗 Related Documentation

- [GETTING_STARTED.md](GETTING_STARTED.md) - Initial setup
- [NETWORK.md](NETWORK.md) - Network rules details
- [INJECTION.md](INJECTION.md) - Java injection
