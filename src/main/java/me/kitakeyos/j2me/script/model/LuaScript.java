package me.kitakeyos.j2me.script.model;

/**
 * Represents a Lua script with its name and code content.
 * This is a simple data model class for managing Lua scripts in the application.
 */
public class LuaScript {
    private String name;
    private String code;

    public LuaScript(String name, String code) {
        this.name = name;
        this.code = code;
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

    @Override
    public String toString() {
        return "LuaScript{" +
                "name='" + name + '\'' +
                ", codeLength=" + (code != null ? code.length() : 0) +
                '}';
    }
}