---
description: Run J2ME Launcher application
---

# Run Application

## Step 1: Build if JAR doesn't exist
// turbo
```bash
mvn package -DskipTests
```

## Step 2: Run application
```bash
java -jar target/j2me-microemulator-launcher-1.0-SNAPSHOT-jar-with-dependencies.jar
```
