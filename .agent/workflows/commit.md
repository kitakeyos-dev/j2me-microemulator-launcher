---
description: View git status and write commit message
---

# Git Commit

## Step 1: View change status
// turbo
```bash
git status
```

## Step 2: View change details
// turbo
```bash
git diff --stat
```

## Step 3: Analyze and suggest commit message
Based on changes, I will suggest a commit message following format:
- `feat:` - New feature
- `fix:` - Bug fix
- `docs:` - Documentation update
- `refactor:` - Code refactoring

## Step 4: Stage and commit (wait for user approval)
```bash
git add .
git commit -m "message"
```
