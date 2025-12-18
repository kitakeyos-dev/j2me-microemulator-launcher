# Lua Scripting

> **Purpose**: Complete guide to Lua scripting with all available APIs and examples.

---

## 🎯 Overview

Lua scripting allows you to:

1. **Automate tasks** - repetitive actions, testing
2. **Access J2ME app classes** - via reflection
3. **Read network data** - sent/received packets
4. **Interact with Java** - create instances, call methods

---

## 📦 Components

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        LUA SCRIPTING SYSTEM                                  │
│                                                                              │
│  ┌────────────────────────────────────────────────────────────────────────┐ │
│  │                     LuaScriptExecutor                                   │ │
│  │                                                                         │ │
│  │  • Creates Lua Globals environment                                      │ │
│  │  • Loads standard Lua libraries                                         │ │
│  │  • Loads custom libraries (DynamicJavaLib, NetworkLib)                  │ │
│  │  • Sets ClassLoader from selected emulator instance                     │ │
│  │  • Executes .lua scripts                                                │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
│                                                                              │
│  ┌────────────────────────────────────────────────────────────────────────┐ │
│  │                     CUSTOM LIBRARIES                                    │ │
│  │                                                                         │ │
│  │  ┌─────────────────────┐     ┌─────────────────────────────────────┐   │ │
│  │  │   DynamicJavaLib    │     │   NetworkLib                        │   │ │
│  │  │   (luajava.*)       │     │   (network.*)                       │   │ │
│  │  │                     │     │                                      │   │ │
│  │  │ • bindClass()       │     │ • getSentData(socketId)             │   │ │
│  │  │ • new()             │     │ • getReceivedData(socketId)         │   │ │
│  │  │ • loadLib()         │     │                                      │   │ │
│  │  │ • createProxy()     │     │                                      │   │ │
│  │  └─────────────────────┘     └─────────────────────────────────────┘   │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 🖥️ Using Script Manager

### UI Layout

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  Scripts Tab                                                                 │
├─────────────────────────────────────────────────────────────────────────────┤
│  ┌────────────┐  ┌──────────────────────────────────────────────────────┐  │
│  │ File Tree  │  │ Code Editor                                          │  │
│  │            │  │                                                       │  │
│  │ scripts/   │  │  -- my_script.lua                                    │  │
│  │ ├─ init.lua│  │  print("Hello!")                                     │  │
│  │ ├─ auto/   │  │                                                       │  │
│  │ │  └─ ...  │  │  local sent = network.getSentData(1)                 │  │
│  │ └─ utils/  │  │  if sent then                                        │  │
│  │            │  │      print("Sent: " .. #sent .. " bytes")            │  │
│  │            │  │  end                                                  │  │
│  └────────────┘  └──────────────────────────────────────────────────────┘  │
│                  ┌──────────────────────────────────────────────────────┐  │
│                  │ Output Console                                        │  │
│                  │                                                       │  │
│                  │ > Hello!                                              │  │
│                  │ > Sent: 128 bytes                                     │  │
│                  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Actions

| Action | Description |
|--------|-------------|
| Right-click → New Script | Create new .lua file |
| Click script | Load in editor |
| Ctrl+Enter | Run selected script |
| Click "Run" | Execute current script |

---

## 📖 Lua Basics

### Hello World

```lua
print("Hello from J2ME Launcher!")
```

### Variables

```lua
local name = "J2ME"           -- String
local count = 10              -- Integer
local enabled = true          -- Boolean
local ratio = 3.14            -- Float
local nothing = nil           -- Nil
```

### Functions

```lua
function greet(name)
    return "Hello, " .. name
end

print(greet("User"))  -- "Hello, User"

-- Anonymous function
local add = function(a, b)
    return a + b
end
```

### Tables

```lua
-- Array-like
local fruits = {"apple", "banana", "cherry"}
print(fruits[1])  -- "apple" (1-indexed!)

-- Dictionary-like
local config = {
    width = 240,
    height = 320,
    name = "MyApp"
}
print(config.width)  -- 240
print(config["name"])  -- "MyApp"
```

### Control Flow

```lua
-- If/else
if x > 10 then
    print("big")
elseif x > 5 then
    print("medium")
else
    print("small")
end

-- For loop (numeric)
for i = 1, 10 do
    print(i)
end

-- For loop (table)
for key, value in pairs(config) do
    print(key .. " = " .. tostring(value))
end

-- While loop
while condition do
    -- ...
end
```

---

## 🔌 Standard Libraries

| Library | Functions |
|---------|-----------|
| `base` | `print`, `type`, `tonumber`, `tostring`, `pairs`, `ipairs`, `pcall`, `error` |
| `string` | `string.sub`, `string.find`, `string.gsub`, `string.format`, `string.len` |
| `table` | `table.insert`, `table.remove`, `table.sort`, `table.concat` |
| `math` | `math.random`, `math.floor`, `math.ceil`, `math.abs`, `math.min`, `math.max` |
| `io` | `io.open`, `io.read`, `io.write`, `io.lines` |
| `os` | `os.date`, `os.time`, `os.clock` |

---

## 🔧 luajava Library (DynamicJavaLib)

Access Java classes from Lua.

### luajava.bindClass(className)

Load a Java class.

```lua
-- Load standard Java class
local StringBuilder = luajava.bindClass("java.lang.StringBuilder")

-- Load J2ME app class (requires ClassLoader from selected instance)
local GameManager = luajava.bindClass("com.game.GameManager")
```

### luajava.new(class, ...)

Create instance of a Java class.

```lua
-- No-arg constructor
local sb = luajava.new(StringBuilder)

-- With arguments
local ArrayList = luajava.bindClass("java.util.ArrayList")
local list = luajava.new(ArrayList, 10)  -- Initial capacity 10

-- Can also use class name string
local map = luajava.new("java.util.HashMap")
```

### Calling Methods

```lua
local StringBuilder = luajava.bindClass("java.lang.StringBuilder")
local sb = luajava.new(StringBuilder)

-- Instance methods (use colon :)
sb:append("Hello")
sb:append(" ")
sb:append("World")
local result = sb:toString()
print(result)  -- "Hello World"

-- Static methods (use dot .)
local System = luajava.bindClass("java.lang.System")
local time = System.currentTimeMillis()
print("Time: " .. time)
```

### Accessing Fields

```lua
-- Static fields
local Integer = luajava.bindClass("java.lang.Integer")
print(Integer.MAX_VALUE)  -- 2147483647

local System = luajava.bindClass("java.lang.System")
local out = System.out
out:println("Hello from Lua!")
```

### luajava.loadLib(className, methodName)

Call static method and return result.

```lua
-- Call System.currentTimeMillis()
local time = luajava.loadLib("java.lang.System", "currentTimeMillis")
print("Timestamp: " .. time)
```

---

## 🔧 network Library (NetworkLib)

Access network packet data.

### network.getSentData(socketId)

Get all data SENT through a socket.

```lua
local sent = network.getSentData(1)

if sent and #sent > 0 then
    print("Sent " .. #sent .. " bytes")
    
    -- Print as hex
    for i = 1, math.min(#sent, 20) do
        io.write(string.format("%02X ", sent:byte(i)))
    end
    print()
end
```

### network.getReceivedData(socketId)

Get all data RECEIVED through a socket.

```lua
local received = network.getReceivedData(1)

if received and #received > 0 then
    print("Received " .. #received .. " bytes")
    
    -- Parse first byte as opcode
    local opcode = received:byte(1)
    print(string.format("Opcode: 0x%02X", opcode))
end
```

---

## 📊 Practical Examples

### Example 1: Hex Dump

```lua
-- Utility function to format bytes as hex
function hexDump(data, maxBytes)
    maxBytes = maxBytes or 100
    if not data then return "(no data)" end
    
    local result = {}
    for i = 1, math.min(#data, maxBytes) do
        table.insert(result, string.format("%02X", data:byte(i)))
        if i % 16 == 0 then
            table.insert(result, "\n")
        end
    end
    
    if #data > maxBytes then
        table.insert(result, string.format("... (%d more bytes)", #data - maxBytes))
    end
    
    return table.concat(result, " ")
end

-- Usage
local sent = network.getSentData(1)
print("Sent data:")
print(hexDump(sent))
```

### Example 2: Parse Packet

```lua
-- Parse network packet
function parsePacket(data)
    if not data or #data < 4 then
        return nil, "Packet too short"
    end
    
    local opcode = data:byte(1)
    local length = data:byte(2) * 256 + data:byte(3) + data:byte(4)
    
    return {
        opcode = opcode,
        length = length,
        payload = data:sub(5)
    }
end

-- Usage
local received = network.getReceivedData(1)
local packet, err = parsePacket(received)

if packet then
    print(string.format("Opcode: 0x%02X", packet.opcode))
    print("Length: " .. packet.length)
    print("Payload size: " .. #packet.payload)
else
    print("Error: " .. err)
end
```

### Example 3: Access J2ME App State

```lua
-- Access game state from J2ME app
-- (Requires selecting emulator instance first)

-- Load game class
local GameState = luajava.bindClass("com.game.GameState")

-- Get singleton instance
local state = GameState:getInstance()

-- Read game values
print("Level: " .. state:getCurrentLevel())
print("Score: " .. state:getScore())
print("Lives: " .. state:getLives())

-- Modify values (use with caution!)
state:setLives(99)
state:setScore(999999)
```

### Example 4: Create Java Objects

```lua
-- Create and use Java collections
local ArrayList = luajava.bindClass("java.util.ArrayList")
local list = luajava.new(ArrayList)

list:add("Item 1")
list:add("Item 2")
list:add("Item 3")

print("List size: " .. list:size())

-- Iterate
for i = 0, list:size() - 1 do
    print("  " .. list:get(i))
end
```

### Example 5: File Operations

```lua
-- Read file
local file = io.open("data/config.txt", "r")
if file then
    local content = file:read("*all")
    file:close()
    print(content)
end

-- Write file
local file = io.open("data/output.txt", "w")
if file then
    file:write("Result: " .. os.date())
    file:close()
end
```

---

## 📁 Organizing Scripts

### Recommended Structure

```
data/scripts/
├── init.lua               # Auto-run on startup
├── common/
│   ├── utils.lua          # Utility functions
│   ├── hex.lua            # Hex formatting
│   └── packet.lua         # Packet parsing
├── automation/
│   ├── login.lua          # Auto login
│   ├── farming.lua        # Auto farming
│   └── quest.lua          # Quest automation
└── analysis/
    ├── traffic.lua        # Network traffic analysis
    └── protocol.lua       # Protocol parsing
```

### Module Pattern

**common/utils.lua:**
```lua
local M = {}

function M.hexDump(data)
    -- ... implementation
end

function M.timestamp()
    return os.date("%Y-%m-%d %H:%M:%S")
end

return M
```

**main.lua:**
```lua
local utils = require("common.utils")

print(utils.timestamp())
print(utils.hexDump(data))
```

---

## ⚠️ Type Coercion

### Lua → Java

| Lua Type | Java Type |
|----------|-----------|
| `nil` | `null` |
| `boolean` | `Boolean` |
| `number` (integer) | `Integer` |
| `number` (float) | `Double` |
| `string` | `String` |
| `userdata` (Java object) | Original Java type |

### Java → Lua

| Java Type | Lua Type |
|-----------|----------|
| `null` | `nil` |
| `Boolean` | `boolean` |
| All numbers | `number` |
| `String` | `string` |
| Other objects | `userdata` |

---

## ⚠️ Error Handling

```lua
-- Use pcall for protected calls
local success, result = pcall(function()
    local Class = luajava.bindClass("com.nonexistent.Class")
    return luajava.new(Class)
end)

if success then
    print("Created: " .. tostring(result))
else
    print("Error: " .. tostring(result))
end
```

---

## ⚠️ Security Considerations

> [!WARNING]
> Lua scripts have full access to:
> - Java runtime (via luajava)
> - File system (via io)
> - Network data
> - J2ME app internals

**Best practices:**
- Don't store passwords in scripts
- Review scripts before running
- Be careful with luajava.bindClass on sensitive classes
- Use pcall to handle errors gracefully

---

## 🔧 Debugging Tips

### Print Debug Info

```lua
-- Debug print with timestamp
function debug(msg)
    print("[" .. os.date("%H:%M:%S") .. "] " .. tostring(msg))
end

-- Debug table contents
function debugTable(t, indent)
    indent = indent or ""
    for k, v in pairs(t) do
        if type(v) == "table" then
            print(indent .. k .. ":")
            debugTable(v, indent .. "  ")
        else
            print(indent .. k .. " = " .. tostring(v))
        end
    end
end
```

### Check Types

```lua
print(type(variable))  -- "nil", "boolean", "number", "string", "table", "function", "userdata"

-- Check if Java object
if type(obj) == "userdata" then
    print("Java object")
end
```

---

## 🔗 Related Documentation

- [NETWORK.md](NETWORK.md) - Network system (where packet data comes from)
- [API.md](API.md) - Java API reference
- [CONFIGURATION.md](CONFIGURATION.md) - Enable scripts tab
