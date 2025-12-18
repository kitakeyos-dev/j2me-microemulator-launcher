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
6. **Exposes data to Lua scripts** for automation

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
│  │  │ • Connection logs   │    │ • PacketLog (captured bytes)            ││ │
│  │  │ • Packet data pools │    │                                         ││ │
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
    
    // Packet data pools (socketId → byte data)
    private final Map<Integer, ByteArrayOutputStream> sentDataPools = new ConcurrentHashMap<>();
    private final Map<Integer, ByteArrayOutputStream> receivedDataPools = new ConcurrentHashMap<>();
    
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

### Packet Data Access (for Lua scripts)

```java
// Get all data SENT through a specific socket
public byte[] getSentData(int socketId) {
    ByteArrayOutputStream baos = sentDataPools.get(socketId);
    return baos != null ? baos.toByteArray() : new byte[0];
}

// Get all data RECEIVED through a specific socket
public byte[] getReceivedData(int socketId) {
    ByteArrayOutputStream baos = receivedDataPools.get(socketId);
    return baos != null ? baos.toByteArray() : new byte[0];
}

// Internal: Called by MonitoredOutputStream
void logSentData(int socketId, byte[] data, int offset, int length) {
    sentDataPools.computeIfAbsent(socketId, k -> new ByteArrayOutputStream())
        .write(data, offset, length);
    totalBytesSent.addAndGet(length);
}

// Internal: Called by MonitoredInputStream
void logReceivedData(int socketId, byte[] data, int offset, int length) {
    receivedDataPools.computeIfAbsent(socketId, k -> new ByteArrayOutputStream())
        .write(data, offset, length);
    totalBytesReceived.addAndGet(length);
}
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
            // Log single byte
            NetworkService.getInstance().logReceivedData(
                socketId, new byte[]{(byte) b}, 0, 1);
        }
        return b;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int bytesRead = wrapped.read(b, off, len);
        if (bytesRead > 0) {
            // Log received bytes
            NetworkService.getInstance().logReceivedData(socketId, b, off, bytesRead);
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
        // Log single byte
        NetworkService.getInstance().logSentData(
            socketId, new byte[]{(byte) b}, 0, 1);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        wrapped.write(b, off, len);
        // Log sent bytes
        NetworkService.getInstance().logSentData(socketId, b, off, len);
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
│      └── NetworkService.logSentData(1, data)   → Store for Lua access       │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 📜 Lua Script Access

```lua
-- Get data sent through socket #1
local sent = network.getSentData(1)
if sent then
    print("Sent " .. #sent .. " bytes")
    
    -- Parse as hex
    for i = 1, math.min(#sent, 50) do
        io.write(string.format("%02X ", sent:byte(i)))
    end
    print()
end

-- Get data received through socket #1
local received = network.getReceivedData(1)
if received then
    print("Received " .. #received .. " bytes")
end
```

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

Packet data accumulates in memory. For long-running sessions:
- Data pools grow as more data is transferred
- Consider clearing pools periodically
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
- [SCRIPTING.md](SCRIPTING.md) - Accessing network data from Lua
- [CONFIGURATION.md](CONFIGURATION.md) - Network rules configuration
