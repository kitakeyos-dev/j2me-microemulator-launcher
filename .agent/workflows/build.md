---
description: Build project with Maven
---

# Build Project

## Step 1: Clean and compile
// turbo
```bash
mvn clean compile
```

## Step 2: Package JAR with dependencies
// turbo
```bash
mvn package -DskipTests
```

## Step 3: Check output
JAR file will be at `target/j2me-microemulator-launcher-*-jar-with-dependencies.jar`
