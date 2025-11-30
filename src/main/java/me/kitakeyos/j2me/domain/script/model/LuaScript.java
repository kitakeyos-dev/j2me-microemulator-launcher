package me.kitakeyos.j2me.domain.script.model;

import java.nio.file.Path;

/**
 * Represents a Lua script with its name and code content.
 * This is a simple data model class for managing Lua scripts in the application.
 */
public class LuaScript {
    private String name;
    private String code;
    private Path path;

    public LuaScript(String name, String code, Path path) {
        this.name = name;
        this.code = code;
        this.path = path;
    }

    // Getters and setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Path getPath() {
        return path;
    }

    @Override
    public String toString() {
        return "LuaScript{" +
                "name='" + name + '\'' +
                ", codeLength=" + (code != null ? code.length() : 0) +
                '}';
    }
}