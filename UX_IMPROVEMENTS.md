# UX/UI Improvements

## Summary
This update significantly improves the user experience by reducing complexity, saving time, and adding convenient shortcuts.

## Changes Made

### 1. Persistent Browse Location ✅
- **Feature**: The application now remembers the last directory you browsed for JAR/JAD files
- **Benefit**: No need to navigate from home directory every time
- **Implementation**: Uses Java Preferences API
- **Files Changed**:
  - New: `FileChooserHelper.java` - Utility class for managing file chooser preferences
  - Modified: `ApplicationsPanel.java`, `SettingsDialog.java`

### 2. Drag & Drop Support ✅
- **Feature**: Drag JAR/JAD files directly into the Applications panel to add them
- **Benefit**: Faster workflow, no need to click "Add Application" and browse
- **Implementation**: DropTarget with file list support
- **Files Changed**: `ApplicationsPanel.java`

### 3. Double-Click to Run ✅
- **Feature**: Double-click any application in the list to create and run an instance immediately
- **Benefit**: One action instead of multiple: select app → switch tab → create → run
- **Implementation**: MouseListener with click count detection
- **Files Changed**: `ApplicationsPanel.java`, `MainApplication.java`

### 4. Simplified 2-Tab Layout ✅
- **Before**: 3 separate tabs (Applications | Instances | Running Instances)
- **After**: 2 tabs with split pane
  - **Tab 1**: "Applications & Instances" (merged, with resizable split)
  - **Tab 2**: "Running Instances"
- **Benefit**:
  - See applications and instances at the same time
  - Less tab switching
  - Adjustable split to customize view
- **Implementation**: JSplitPane with 40/60 split ratio
- **Files Changed**: `MainApplication.java`

### 5. Keyboard Shortcuts ✅
Global shortcuts for common actions:
- **Ctrl+O**: Add Application
- **Ctrl+N**: Create Instances
- **Ctrl+R**: Run All
- **Ctrl+Shift+S**: Stop All
- **Ctrl+Shift+C**: Clear All
- **Ctrl+,**: Open Settings

- **Benefit**: Power users can work faster without mouse
- **Implementation**: Swing KeyBindings (InputMap/ActionMap)
- **Files Changed**: `MainApplication.java`, `ApplicationsPanel.java`

## User Experience Comparison

### Before:
```
1. Click "Add Application"
2. Browse from home directory every time
3. Navigate to JAR location
4. Select file
5. Switch to "Instances" tab
6. Select application from dropdown
7. Click "Create Instances"
8. Click "Run All"
9. Switch to "Running Instances" tab to see result

Total: 9+ steps, 3 tab switches
```

### After:
```
Option A (Drag & Drop):
1. Drag JAR file into window
2. Double-click the app
Total: 2 steps, 0 tab switches

Option B (Keyboard):
1. Ctrl+O (opens at last location)
2. Select file
3. Ctrl+N (create)
4. Ctrl+R (run)
Total: 4 steps, 0 tab switches
```

## Technical Details

### New Classes
- `me.kitakeyos.j2me.util.FileChooserHelper`: Manages persistent file chooser preferences

### Modified Classes
- `me.kitakeyos.j2me.ui.panel.ApplicationsPanel`: Added drag-drop, double-click, preferences
- `me.kitakeyos.j2me.ui.dialog.SettingsDialog`: Uses FileChooserHelper
- `me.kitakeyos.j2me.MainApplication`: 2-tab layout, keyboard shortcuts, split pane

### Backwards Compatibility
- All existing functionality preserved
- No breaking changes
- Configuration files remain compatible
- Preferences stored in user node, won't conflict

## Future Enhancements (Not Implemented)
- Context menu (right-click) on applications
- Recent apps list
- Auto-detect MicroEmulator path
- Filter/search in applications list
- Multi-select applications
