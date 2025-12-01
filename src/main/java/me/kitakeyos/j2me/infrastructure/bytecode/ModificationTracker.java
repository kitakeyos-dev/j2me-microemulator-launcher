package me.kitakeyos.j2me.infrastructure.bytecode;

public class ModificationTracker {
    private boolean modified = false;

    public void setModified(boolean modified) {
        this.modified = modified;
    }

    public boolean isModified() {
        return modified;
    }
}
