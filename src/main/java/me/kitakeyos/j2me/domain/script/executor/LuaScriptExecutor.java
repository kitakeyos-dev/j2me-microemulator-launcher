package me.kitakeyos.j2me.domain.script.executor;

import me.kitakeyos.j2me.domain.script.library.DynamicJavaLib;
import me.kitakeyos.j2me.infrastructure.persistence.script.ScriptFileManager;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.*;
import org.luaj.vm2.lib.jse.*;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.function.Consumer;

/**
 * LuaScriptExecutor with require support for cross-script communication
 */
public class LuaScriptExecutor {
    private final Consumer<String> outputConsumer;
    private final Consumer<String> errorConsumer;
    private final Consumer<String> successConsumer;
    private final Consumer<String> infoConsumer;
    private final DynamicJavaLib dynamicJavaLib;

    public LuaScriptExecutor(Consumer<String> outputConsumer, Consumer<String> errorConsumer,
                             Consumer<String> successConsumer, Consumer<String> infoConsumer) {
        this.outputConsumer = outputConsumer;
        this.errorConsumer = errorConsumer;
        this.successConsumer = successConsumer;
        this.infoConsumer = infoConsumer;
        this.dynamicJavaLib = new DynamicJavaLib();
    }

    /**
     * Set ClassLoader from selected emulator instance
     */
    public void setInstanceClassLoader(ClassLoader classLoader) {
        dynamicJavaLib.setInstanceClassLoader(classLoader);
    }

    public void executeScript(String scriptName) {
        String separator = new String(new char[50]).replace('\0', '-');
        infoConsumer.accept("Time: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));

        try {

            outputConsumer.accept(separator);

            // Create Lua globals
            Globals globals = JsePlatform.standardGlobals();

            globals.load(new JseBaseLib());
            globals.load(new PackageLib());
            globals.load(new Bit32Lib());
            globals.load(new TableLib());
            globals.load(new StringLib());
            globals.load(new CoroutineLib());
            globals.load(new JseMathLib());
            globals.load(new JseIoLib());
            globals.load(new JseOsLib());
            globals.load(dynamicJavaLib);

            String currentPath = globals.get("package").get("path").tojstring();

            // Set the new path using the correct method signature
            globals.get("package").set("path", currentPath + ";./" + ScriptFileManager.SCRIPTS_DIR + "/?.lua");

            // Redirect Lua print to output consumer
            globals.set("print", new VarArgFunction() {
                @Override
                public Varargs invoke(Varargs args) {
                    StringBuilder output = new StringBuilder();
                    for (int i = 1; i <= args.narg(); i++) {
                        if (i > 1) output.append("\t");
                        output.append(args.arg(i).tojstring());
                    }
                    outputConsumer.accept(output.toString());
                    return NONE;
                }
            });

            // Execute Lua script
            LuaValue chunk = globals.loadfile(new File(ScriptFileManager.SCRIPTS_DIR, scriptName + ".lua").getPath());
            chunk.call();

            outputConsumer.accept(separator);
            successConsumer.accept("Script execution completed successfully");

        } catch (Exception e) {
            outputConsumer.accept(separator);
            errorConsumer.accept("Error: " + e.getMessage());
        }
    }
}