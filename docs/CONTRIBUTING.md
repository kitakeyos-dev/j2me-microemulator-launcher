# Contributing Guide

> **Purpose**: How to contribute to the project.

---

## 🚀 Getting Started

### 1. Fork the Repository

1. Go to [GitHub repository](https://github.com/kitakeyos-dev/j2me-microemulator-launcher)
2. Click **Fork** button
3. Clone your fork:

```bash
git clone https://github.com/YOUR_USERNAME/j2me-microemulator-launcher.git
cd j2me-microemulator-launcher
```

### 2. Set Up Development Environment

**Requirements:**
- JDK 8 or higher
- Maven 3.6+
- IDE (IntelliJ IDEA, Eclipse, VS Code)

**Build:**
```bash
mvn clean install
```

**Run:**
```bash
java -jar target/j2me-microemulator-launcher-*-jar-with-dependencies.jar
```

### 3. Create a Branch

```bash
# For features
git checkout -b feature/your-feature-name

# For bug fixes
git checkout -b fix/bug-description

# For documentation
git checkout -b docs/topic-name
```

---

## 📝 Coding Guidelines

### Code Style

| Aspect | Standard |
|--------|----------|
| Indent | 4 spaces |
| Encoding | UTF-8 |
| Line endings | LF (Unix) |
| Max line length | 120 characters |
| Braces | Same line |

### Naming Conventions

| Type | Convention | Example |
|------|------------|---------|
| Classes | PascalCase | `EmulatorInstance` |
| Interfaces | PascalCase | `ApplicationRepository` |
| Methods | camelCase | `getInstance()` |
| Variables | camelCase | `instanceId` |
| Constants | UPPER_SNAKE | `MAX_INSTANCES` |
| Packages | lowercase | `me.kitakeyos.j2me.domain` |

### Architecture

Follow Clean Architecture:

```
presentation → application → domain ← infrastructure
```

| Layer | Can Depend On | Cannot Depend On |
|-------|---------------|------------------|
| Presentation | Application, Domain | Infrastructure |
| Application | Domain | Infrastructure, Presentation |
| Domain | Nothing | All other layers |
| Infrastructure | Domain | Presentation, Application |

### Javadoc

Add Javadoc for public classes and methods:

```java
/**
 * Represents an emulator instance with its configuration and state.
 * 
 * <p>Each instance is isolated with its own ClassLoader and can be
 * started, stopped, and configured independently.</p>
 * 
 * @author Kitakeyos
 * @since 1.0
 * @see EmulatorLauncher
 */
public class EmulatorInstance {
    
    /**
     * Shuts down the instance and releases all resources.
     * 
     * <p>This method:
     * <ul>
     *   <li>Stops all threads created by this instance</li>
     *   <li>Closes all sockets</li>
     *   <li>Closes the ClassLoader</li>
     * </ul>
     * 
     * @throws IllegalStateException if already shut down
     */
    public void shutdown() {
        // ...
    }
}
```

---

## 🔀 Pull Request Process

### 1. Sync with Upstream

```bash
git fetch upstream
git rebase upstream/main
```

### 2. Commit Changes

Use conventional commit messages:

```
<type>: <subject>

<body>
```

**Types:**
| Type | Description |
|------|-------------|
| `feat` | New feature |
| `fix` | Bug fix |
| `docs` | Documentation |
| `refactor` | Code refactoring |
| `test` | Adding/fixing tests |
| `chore` | Maintenance |

**Examples:**
```
feat: Add HTTP proxy support

- Add ProxyRule.ProxyType.HTTP enum value
- Update NetworkService to handle HTTP CONNECT
- Add UI for HTTP proxy configuration

Closes #123
```

```
fix: Fix socket cleanup on instance shutdown

Previously, sockets were not closed when an instance was shut down,
leading to resource leaks.

This fix ensures all sockets are tracked and closed in shutdown().

Fixes #456
```

### 3. Push Changes

```bash
git push origin feature/your-feature-name
```

### 4. Create Pull Request

1. Go to your fork on GitHub
2. Click "New Pull Request"
3. Select your branch
4. Fill in template:

```markdown
## Description
Brief description of changes.

## Changes
- Added ...
- Fixed ...
- Updated ...

## Testing
- [ ] Tested locally
- [ ] Added unit tests
- [ ] Tested with multiple instances

## Screenshots (if UI changes)
[Add screenshots]

## Related Issues
Closes #123
```

### 5. PR Checklist

- [ ] Code follows style guidelines
- [ ] Self-reviewed code
- [ ] Added Javadoc for public APIs
- [ ] No breaking changes (or documented)
- [ ] Tests pass (`mvn test`)
- [ ] Build succeeds (`mvn package`)

---

## 🐛 Bug Reports

### Create an Issue

Use this template:

```markdown
## Bug Description
Clear description of the bug.

## Steps to Reproduce
1. Go to '...'
2. Click on '...'
3. See error

## Expected Behavior
What should happen.

## Actual Behavior
What actually happens.

## Screenshots
If applicable.

## Environment
- OS: [Windows 10 / Ubuntu 22.04 / macOS 13]
- Java version: [output of `java -version`]
- Launcher version: [version number]
- MicroEmulator version: [2.0.4]

## Additional Context
Any other information.
```

---

## 💡 Feature Requests

### Create an Issue

```markdown
## Feature Description
Brief description of the feature.

## Motivation
Why is this feature needed?
What problem does it solve?

## Proposed Solution
How you think it should work.

## Alternatives Considered
Other approaches considered.

## Additional Context
Screenshots, mockups, examples.
```

---

## 📋 Good First Issues

Looking for something to contribute? Check these areas:

- [ ] **Documentation** - Improve docs, fix typos
- [ ] **Unit tests** - Add test coverage
- [ ] **UI improvements** - Better styling, accessibility
- [ ] **Error messages** - More helpful error messages
- [ ] **Localization** - Translate to other languages

---

## 🏗️ Wanted Features

Major features that need contributors:

- [ ] Plugin system for extensions
- [ ] Enhanced Java injection IDE (autocomplete, debugging)
- [ ] Screenshot/video recording
- [ ] Performance profiling tools
- [ ] Cross-platform installer

---

## 🤝 Code of Conduct

- Be respectful and constructive
- Welcome newcomers
- Focus on code, not people
- No harassment or discrimination

---

## 📞 Getting Help

- **GitHub Issues** - Bug reports and feature requests
- **Pull Requests** - Code contributions
- **Discussions** - Questions and ideas

---

Thank you for contributing! 🙏
