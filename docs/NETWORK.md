# Network System

> **Purpose**: This document explains the network monitoring, redirection, and proxy system in extreme detail.

---

## 🎯 Overview

The network system:

1. **Intercepts all socket connections** from J2ME apps (via bytecode manipulation)
2. **Logs all connections** with metadata (timestamp, instance, host, port)
3. **Redirects connections** based on rules (e.g., game.server.com → localhost)
4. **Proxies connections** through SOCKS or HTTP proxies
5. **Captures all packet data** (sent and received bytes)
6. **Exposes packet data** for inspection and injection

---

## 📦 Components

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         NETWORK SYSTEM COMPONENTS                            │
│                                                                              │
│  ┌────────────────────────────────────────────────────────────────────────┐ │
│  │                        DOMAIN LAYER                                     │ │
│  │                                                                         │ │
│  │  ┌─────────────────────┐    ┌─────────────────────────────────────────┐│ │
│  │  │   NetworkService    │    │              MODELS                      ││ │
│  │  │     (Singleton)     │    │                                         ││ │
│  │  │                     │    │ • RedirectionRule (host:port → target)  ││ │
│  │  │ • Redirection rules │    │ • ProxyRule (SOCKS/HTTP proxy config)    ││ │
│  │  │ • Proxy rules       │    │ • ConnectionLog (connection metadata)   ││ │
│  │  │ • Connection logs   │    │ • SocketTap (per-socket tap streams)    ││ │
│  │  │ • Socket taps       │    │ • TapStream (expandable-buffer stream)  ││ │
│  │  │ • createSocket()    │    └─────────────────────────────────────────┘│ │
│  │  └─────────────────────┘                                                │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
│                                                                              │
│  ┌────────────────────────────────────────────────────────────────────────┐ │
│  │                     INFRASTRUCTURE LAYER                                │ │
│  │                                                                         │ │
│  │  ┌──────────────────┐ ┌──────────────────┐ ┌─────────────────────────┐ │ │
│  │  │  MonitoredSocket │ │MonitoredInput    │ │MonitoredOutput          │ │ │
│  │  │                  │ │    Stream        │ │    Stream               │ │ │
│  │  │ wraps real Socket│ │                  │ │                         │ │ │
│  │  │                  │ │ logs all reads   │ │ logs all writes         │ │ │
│  │  └──────────────────┘ └──────────────────┘ └─────────────────────────┘ │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 🔧 NetworkService

**Location**: `domain/network/service/NetworkService.java`

Central singleton that manages all network operations.

### Class Structure

```java
public class NetworkService {
    // Singleton
    private static final NetworkService INSTANCE = new NetworkService();
    public static NetworkService getInstance() { return INSTANCE; }
    
    // Rules
    private final List<RedirectionRule> redirectionRules = new CopyOnWriteArrayList<>();
    private final List<ProxyRule> proxyRules = new CopyOnWriteArrayList<>();
    
    // Logs
    private final List<ConnectionLog> connectionLogs = new CopyOnWriteArrayList<>();
    
    // Socket taps (stream-based, per-instance on/off)
    private final Map<Integer, SocketTap> socketTaps = new ConcurrentHashMap<>();
    private final Set<Integer> tappingEnabledInstances = ConcurrentHashMap.newKeySet();
    
    // Statistics
    private final AtomicLong totalBytesSent = new AtomicLong(0);
    private final AtomicLong totalBytesReceived = new AtomicLong(0);
}
```

### Key Method: `createSocket()`

This is the core method called when a J2ME app creates a socket.

```java
public Socket createSocket(int instanceId, String host, int port) throws IOException {
    String finalHost = host;
    int finalPort = port;
    
    // 1. APPLY REDIRECTION RULES
    for (RedirectionRule rule : redirectionRules) {
        if (rule.matches(instanceId, host, port)) {
            finalHost = rule.getTargetHost();
            finalPort = rule.getTargetPort();
            logger.info(String.format("Redirecting %s:%d → %s:%d", 
                host, port, finalHost, finalPort));
            break;  // Only apply first matching rule
        }
    }
    
    // 2. DETERMINE PROXY (if any)
    ProxyRule proxyRule = findApplicableProxy(instanceId);
    Socket socket;
    
    if (proxyRule != null) {
        // 3a. CREATE SOCKET VIA PROXY
        Proxy proxy = new Proxy(
            proxyRule.getJavaProxyType(),
            new InetSocketAddress(proxyRule.getProxyHost(), proxyRule.getProxyPort())
        );
        socket = new Socket(proxy);
        socket.connect(new InetSocketAddress(finalHost, finalPort));
    } else {
        // 3b. CREATE DIRECT SOCKET
        socket = new Socket(finalHost, finalPort);
    }
    
    // 4. LOG CONNECTION
    ConnectionLog log = new ConnectionLog(
        instanceId,
        host, port,           // Original
        finalHost, finalPort, // Final (after redirect)
        proxyRule != null     // Whether proxy was used
    );
    connectionLogs.add(log);
    
    return socket;
}
```

### Rule Management Methods

```java
// === REDIRECTION RULES ===
public void addRedirectionRule(RedirectionRule rule);
public void removeRedirectionRule(RedirectionRule rule);
public List<RedirectionRule> getRedirectionRules();
public void clearRedirectionRules();

// === PROXY RULES ===
public void addProxyRule(ProxyRule rule);
public void removeProxyRule(ProxyRule rule);
public List<ProxyRule> getProxyRules();
public void clearProxyRules();
```

### Tapping and Data Access

```java
// Enable tapping for an instance (off by default)
NetworkService.getInstance().enableTapping(instanceId);

// Get taps for an instance
List<SocketTap> taps = NetworkService.getInstance().getTapsByInstance(instanceId);

// Read data (blocking, like InputStream)
InputStream received = tap.getReceivedStream();

// Read data (non-blocking)
byte[] data = tap.drainSent();      // consume all buffered
byte[] peek = tap.peekReceived();   // peek without consuming

// Disable when done
NetworkService.getInstance().disableTapping(instanceId);
```

---

## 🔧 RedirectionRule

**Location**: `domain/network/model/RedirectionRule.java`

Model for redirection rules.

### Fields

```java
public class RedirectionRule {
    public static final int ALL_INSTANCES = -1;
    
    private final String originalHost;    // Host to match
    private final int originalPort;       // Port to match
    private final String targetHost;      // Redirect to this host
    private final int targetPort;         // Redirect to this port
    private final int instanceId;         // -1 = all instances
    private boolean enabled;
}
```

### Example Usage

```java
// Redirect all connections to game.server.com:8080 to localhost:9000
RedirectionRule rule = new RedirectionRule(
    "game.server.com", 8080,    // Match this
    "localhost", 9000,          // Redirect to this
    RedirectionRule.ALL_INSTANCES  // Apply to all instances
);
NetworkService.getInstance().addRedirectionRule(rule);
```

### Matching Logic

```java
public boolean matches(int instanceId, String host, int port) {
    // Must be enabled
    if (!enabled) return false;
    
    // Must match instance (or be ALL_INSTANCES)
    if (this.instanceId != ALL_INSTANCES && this.instanceId != instanceId)
        return false;
    
    // Must match host and port
    return originalHost.equalsIgnoreCase(host) && originalPort == port;
}
```

---

## 🔧 ProxyRule

**Location**: `domain/network/model/ProxyRule.java`

Model for proxy rules.

### Fields

```java
public class ProxyRule {
    public static final int ALL_INSTANCES = -1;
    
    public enum ProxyType { SOCKS, HTTP }
    
    private final ProxyType proxyType;
    private final String proxyHost;
    private final int proxyPort;
    private final int instanceId;
    private final String username;    // Nullable
    private final String password;    // Nullable
    private boolean enabled;
}
```

### Example Usage

```java
// SOCKS proxy for instance #1
ProxyRule rule = new ProxyRule(
    ProxyRule.ProxyType.SOCKS,
    "127.0.0.1", 1080,
    1,             // Only instance #1
    null, null     // No authentication
);
NetworkService.getInstance().addProxyRule(rule);
```

### Proxy Type Mapping

```java
public Proxy.Type getJavaProxyType() {
    return proxyType == ProxyType.SOCKS ? Proxy.Type.SOCKS : Proxy.Type.HTTP;
}
```

---

## 🔧 MonitoredSocket

**Location**: `infrastructure/network/MonitoredSocket.java`

Socket wrapper that intercepts all I/O.

### Class Structure

```java
public class MonitoredSocket extends Socket {
    private static final AtomicInteger SOCKET_ID_COUNTER = new AtomicInteger(1);

    private final Socket wrapped;          // Real socket
    private final int instanceId;          // Which emulator instance
    private final int socketId;            // Unique ID for this socket
    private final String host;             // Original target host
    private final int port;                // Original target port

    private MonitoredInputStream monitoredInputStream;
    private MonitoredOutputStream monitoredOutputStream;
}
```

### Key Methods

```java
@Override
public InputStream getInputStream() throws IOException {
    if (monitoredInputStream == null) {
        monitoredInputStream = new MonitoredInputStream(
            wrapped.getInputStream(), 
            instanceId, 
            socketId, 
            host, 
            port
        );
    }
    return monitoredInputStream;
}

@Override
public OutputStream getOutputStream() throws IOException {
    if (monitoredOutputStream == null) {
        monitoredOutputStream = new MonitoredOutputStream(
            wrapped.getOutputStream(), 
            instanceId, 
            socketId, 
            host,
            port
        );
    }
    return monitoredOutputStream;
}
```

All other methods delegate to the wrapped socket.

---

## 🔧 MonitoredInputStream

**Location**: `infrastructure/network/MonitoredInputStream.java`

InputStream wrapper that logs all received data.

```java
public class MonitoredInputStream extends InputStream {
    private final InputStream wrapped;
    private final int instanceId;
    private final int socketId;

    @Override
    public int read() throws IOException {
        int b = wrapped.read();
        if (b != -1) {
            // Push to SocketTap received stream
            SocketTap tap = NetworkService.getInstance().getTap(socketId);
            if (tap != null) {
                tap.getReceivedStream().push(new byte[]{(byte) b}, 0, 1);
            }
        }
        return b;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int bytesRead = wrapped.read(b, off, len);
        if (bytesRead > 0) {
            // Push to SocketTap received stream
            SocketTap tap = NetworkService.getInstance().getTap(socketId);
            if (tap != null) {
                tap.getReceivedStream().push(b, off, bytesRead);
            }
        }
        return bytesRead;
    }
}
```

---

## 🔧 MonitoredOutputStream

**Location**: `infrastructure/network/MonitoredOutputStream.java`

OutputStream wrapper that logs all sent data.

```java
public class MonitoredOutputStream extends OutputStream {
    private final OutputStream wrapped;
    private final int instanceId;
    private final int socketId;

    @Override
    public void write(int b) throws IOException {
        wrapped.write(b);
        // Push to SocketTap sent stream
        SocketTap tap = NetworkService.getInstance().getTap(socketId);
        if (tap != null) {
            tap.getSentStream().push(new byte[]{(byte) b}, 0, 1);
        }
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        wrapped.write(b, off, len);
        // Push to SocketTap sent stream
        SocketTap tap = NetworkService.getInstance().getTap(socketId);
        if (tap != null) {
            tap.getSentStream().push(b, off, len);
        }
    }
}
```

---

## 🔄 Complete Flow

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                     NETWORK INTERCEPTION FLOW                                │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  J2ME App Code                                                               │
│      │                                                                       │
│      │  new Socket("game.server.com", 8080)                                 │
│      │                                                                       │
│      ▼                                                                       │
│  [BYTECODE TRANSFORMED]                                                      │
│      │                                                                       │
│      │  SystemCallHandler.createSocket(1, "game.server.com", 8080)          │
│      │                                                                       │
│      ▼                                                                       │
│  SystemCallHandler                                                           │
│      │                                                                       │
│      │  1. Delegate to NetworkService                                        │
│      │  2. Wrap result in MonitoredSocket                                   │
│      │  3. Track socket in EmulatorInstance                                 │
│      │                                                                       │
│      ▼                                                                       │
│  NetworkService.createSocket(1, "game.server.com", 8080)                    │
│      │                                                                       │
│      ├── Check RedirectionRules                                             │
│      │       Rule: game.server.com:8080 → localhost:9000                    │
│      │       Result: finalHost=localhost, finalPort=9000                    │
│      │                                                                       │
│      ├── Check ProxyRules                                                   │
│      │       No proxy for instance #1                                       │
│      │                                                                       │
│      ├── Create Socket                                                      │
│      │       new Socket("localhost", 9000)                                  │
│      │                                                                       │
│      ├── Log Connection                                                     │
│      │       ConnectionLog: #1, game.server.com:8080 → localhost:9000       │
│      │                                                                       │
│      └── Return raw Socket                                                  │
│                                                                              │
│      ▼                                                                       │
│  MonitoredSocket wraps raw Socket                                           │
│      │                                                                       │
│      │  socketId = 1 (auto-incremented)                                     │
│      │                                                                       │
│      ▼                                                                       │
│  J2ME App gets MonitoredSocket (looks like normal Socket)                   │
│      │                                                                       │
│      │  socket.getOutputStream().write(data)                                │
│      │                                                                       │
│      ▼                                                                       │
│  MonitoredOutputStream                                                       │
│      │                                                                       │
│      ├── wrapped.write(data)     → Actually send data                       │
│      │                                                                       │
│      └── SocketTap.getSentStream().push(data)   → Store for inspection      │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

---

## 💾 Persistence

Rules are saved to `data/network_rules.properties`:

```properties
# Redirection rules
redirect.0.originalHost=game.server.com
redirect.0.originalPort=8080
redirect.0.targetHost=localhost
redirect.0.targetPort=9000
redirect.0.instanceId=-1
redirect.0.enabled=true

# Proxy rules
proxy.0.type=SOCKS
proxy.0.host=127.0.0.1
proxy.0.port=1080
proxy.0.instanceId=-1
proxy.0.enabled=true
```

---

## ⚠️ Important Notes

### Thread Safety

All collections use thread-safe implementations:
- `CopyOnWriteArrayList` for rules and logs
- `ConcurrentHashMap` for data pools
- `AtomicLong` for statistics

### Memory Considerations

Tap stream buffers accumulate in memory. For long-running sessions:
- Use `drain()` to consume and release buffered data regularly
- Disable tapping with `disableTapping(instanceId)` when not needed
- Large data transfers will consume significant memory

### Socket Cleanup

When an instance shuts down:
```java
// In InstanceLifecycleManager
for (Socket socket : instance.getSockets()) {
    try {
        socket.close();
    } catch (IOException e) {
        // Log and continue
    }
}
```

---

## 🔗 Related Documentation

- [BYTECODE.md](BYTECODE.md) - How socket creation is intercepted
- [INJECTION.md](INJECTION.md) - Java injection for runtime interaction
- [CONFIGURATION.md](CONFIGURATION.md) - Network rules configuration
