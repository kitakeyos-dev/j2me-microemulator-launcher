package me.kitakeyos.j2me.script.storage;

import me.kitakeyos.j2me.script.model.LuaScript;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * ScriptFileManager - Handles loading and saving of Lua scripts and their metadata
 */
public class ScriptFileManager {
    public static final String SCRIPTS_DIR = "lua_scripts";

    public ScriptFileManager() {
        // Create scripts directory if it doesn't exist
        new File(SCRIPTS_DIR).mkdirs();
    }

    /**
     * Loads all Lua scripts and their metadata from the scripts directory
     * @return Map of script names to LuaScript objects
     */
    public Map<String, LuaScript> loadScripts() {
        Map<String, LuaScript> scripts = new HashMap<>();

        File scriptsDir = new File(SCRIPTS_DIR);
        if (!scriptsDir.exists()) {
            return scripts;
        }

        File[] luaFiles = scriptsDir.listFiles((dir, name) -> name.endsWith(".lua"));
        if (luaFiles != null) {
            for (File luaFile : luaFiles) {
                String scriptName = luaFile.getName().replace(".lua", "");

                // Load script code
                String code = "";
                try (BufferedReader reader = new BufferedReader(new FileReader(luaFile))) {
                    StringBuilder codeBuilder = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        codeBuilder.append(line).append("\n");
                    }
                    code = codeBuilder.toString();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                LuaScript script = new LuaScript(scriptName, code);
                scripts.put(scriptName, script);
            }
        }

        return scripts;
    }

    /**
     * Saves a Lua script and its metadata to files
     * @param script The LuaScript object to save
     */
    public void saveScriptToFile(LuaScript script) {
        // Save script code
        File luaFile = new File(SCRIPTS_DIR, script.getName() + ".lua");
        try (PrintWriter writer = new PrintWriter(new FileWriter(luaFile))) {
            writer.print(script.getCode());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Deletes a script and its metadata file
     * @param scriptName The name of the script to delete
     */
    public void deleteScriptFiles(String scriptName) {
        File scriptFile = new File(SCRIPTS_DIR, scriptName + ".lua");
        if (scriptFile.exists()) {
            scriptFile.delete();
        }
    }
}