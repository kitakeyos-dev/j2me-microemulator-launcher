package me.kitakeyos.j2me.script.ui;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Enhanced CodeCompletionProvider with:
 * - Detection of user-defined functions in the current script
 * - Detection of local variables
 * - Improved context-aware suggestions
 * - Better performance through caching
 */
public class CodeCompletionProvider {

    // Lua keywords
    private static final Set<String> LUA_KEYWORDS = new HashSet<>(Arrays.asList(
            "and", "break", "do", "else", "elseif", "end", "false", "for", "function",
            "if", "in", "local", "nil", "not", "or", "repeat", "return", "then",
            "true", "until", "while", "goto"
    ));

    // Lua built-in functions
    private static final Set<String> LUA_BUILTIN_FUNCTIONS = new HashSet<>(Arrays.asList(
            "print", "type", "tostring", "tonumber", "pairs", "ipairs", "next",
            "require", "assert", "error", "pcall", "xpcall", "select", "unpack",
            "setmetatable", "getmetatable", "rawget", "rawset", "rawequal",
            "collectgarbage", "dofile", "load", "loadfile", "loadstring"
    ));

    // Lua standard libraries
    private static final Set<String> LUA_LIBRARIES = new HashSet<>(Arrays.asList(
            "string", "table", "math", "io", "os", "coroutine", "debug", "package"
    ));

    // Library methods
    private static final Map<String, Set<String>> LIBRARY_FUNCTIONS = new HashMap<>();
    static {
        LIBRARY_FUNCTIONS.put("string", new HashSet<>(Arrays.asList(
                "byte", "char", "dump", "find", "format", "gmatch", "gsub",
                "len", "lower", "match", "rep", "reverse", "sub", "upper"
        )));

        LIBRARY_FUNCTIONS.put("table", new HashSet<>(Arrays.asList(
                "concat", "insert", "maxn", "remove", "sort", "unpack", "pack"
        )));

        LIBRARY_FUNCTIONS.put("math", new HashSet<>(Arrays.asList(
                "abs", "acos", "asin", "atan", "atan2", "ceil", "cos", "cosh",
                "deg", "exp", "floor", "fmod", "frexp", "huge", "ldexp", "log",
                "log10", "max", "min", "modf", "pi", "pow", "rad", "random",
                "randomseed", "sin", "sinh", "sqrt", "tan", "tanh"
        )));

        LIBRARY_FUNCTIONS.put("io", new HashSet<>(Arrays.asList(
                "close", "flush", "input", "lines", "open", "output", "popen",
                "read", "stderr", "stdin", "stdout", "tmpfile", "type", "write"
        )));

        LIBRARY_FUNCTIONS.put("os", new HashSet<>(Arrays.asList(
                "clock", "date", "difftime", "execute", "exit", "getenv",
                "remove", "rename", "setlocale", "time", "tmpname"
        )));

        LIBRARY_FUNCTIONS.put("coroutine", new HashSet<>(Arrays.asList(
                "create", "resume", "running", "status", "wrap", "yield"
        )));

        LIBRARY_FUNCTIONS.put("debug", new HashSet<>(Arrays.asList(
                "debug", "getfenv", "gethook", "getinfo", "getlocal",
                "getmetatable", "getregistry", "getupvalue", "setfenv",
                "sethook", "setlocal", "setmetatable", "setupvalue", "traceback"
        )));
    }

    // Java classes for import completion
    private static final Set<String> JAVA_CLASSES = new HashSet<>(Arrays.asList(
            "java.util.ArrayList", "java.util.HashMap", "java.util.HashSet",
            "java.util.LinkedList", "java.util.List", "java.util.Map", "java.util.Set",
            "java.io.File", "java.io.FileReader", "java.io.FileWriter",
            "java.io.BufferedReader", "java.io.BufferedWriter",
            "java.lang.String", "java.lang.Integer", "java.lang.Double",
            "java.lang.Boolean", "java.lang.Math", "java.lang.System",
            "java.util.Arrays", "java.util.Collections",
            "java.util.Date", "java.util.Random", "java.util.Scanner",
            "java.net.URL", "java.net.HttpURLConnection"
    ));

    // Common object methods
    private static final Set<String> COMMON_METHODS = new HashSet<>(Arrays.asList(
            "add", "remove", "size", "get", "put", "set", "clear", "contains",
            "isEmpty", "toString", "length", "substring", "indexOf", "lastIndexOf",
            "equals", "hashCode", "clone", "toArray", "iterator"
    ));

    // Code templates/snippets
    private static final Map<String, String> CODE_SNIPPETS = new LinkedHashMap<>();
    static {
        CODE_SNIPPETS.put("function", "function name()\n\t\nend");
        CODE_SNIPPETS.put("if", "if condition then\n\t\nend");
        CODE_SNIPPETS.put("ifelse", "if condition then\n\t\nelse\n\t\nend");
        CODE_SNIPPETS.put("for", "for i = 1, n do\n\t\nend");
        CODE_SNIPPETS.put("fori", "for i, v in ipairs(tbl) do\n\t\nend");
        CODE_SNIPPETS.put("forp", "for k, v in pairs(tbl) do\n\t\nend");
        CODE_SNIPPETS.put("while", "while condition do\n\t\nend");
        CODE_SNIPPETS.put("repeat", "repeat\n\t\nuntil condition");
        CODE_SNIPPETS.put("local", "local name = value");
        CODE_SNIPPETS.put("localfn", "local function name()\n\t\nend");
    }

    // Pre-compiled patterns
    private static final Pattern LIBRARY_PATTERN = Pattern.compile("\\b(string|table|math|io|os|coroutine|debug)\\.([a-zA-Z_]*)$");
    private static final Pattern DOT_PATTERN = Pattern.compile("\\b([a-zA-Z_][a-zA-Z0-9_]*)\\.$");
    private static final Pattern COLON_PATTERN = Pattern.compile("\\b([a-zA-Z_][a-zA-Z0-9_]*):([a-zA-Z_]*)$");
    private static final Pattern FUNCTION_DEF_PATTERN = Pattern.compile(
            "(?:local\\s+)?function\\s+([a-zA-Z_][a-zA-Z0-9_]*)\\s*\\(([^)]*)\\)"
    );
    private static final Pattern LOCAL_VAR_PATTERN = Pattern.compile(
            "\\blocal\\s+([a-zA-Z_][a-zA-Z0-9_]*)\\s*="
    );
    private static final Pattern GLOBAL_VAR_PATTERN = Pattern.compile(
            "^([a-zA-Z_][a-zA-Z0-9_]*)\\s*=\\s*(?!.*function)",
            Pattern.MULTILINE
    );

    // Cache for user-defined symbols
    private Set<String> cachedUserFunctions = new HashSet<>();
    private Set<String> cachedLocalVariables = new HashSet<>();
    private String cachedScriptHash = "";

    /**
     * Provides completion suggestions based on context
     */
    public List<String> getSuggestions(String text, int caretOffset) {
        List<String> suggestions = new ArrayList<>();

        if (caretOffset < 0 || caretOffset > text.length()) {
            return suggestions;
        }

        String prefix = text.substring(0, caretOffset);
        String lastLine = getLastLine(prefix);
        String partialWord = getPartialWord(prefix);

        // Update cache of user-defined symbols
        updateUserSymbolsCache(text);

        // Handle specific completion contexts
        if (handleImportCompletion(lastLine, suggestions)) {
            return limitAndSort(suggestions);
        }

        if (handleLibraryMethodCompletion(lastLine, suggestions)) {
            return limitAndSort(suggestions);
        }

        if (handleDotNotation(lastLine, text, suggestions)) {
            return limitAndSort(suggestions);
        }

        if (handleColonNotation(lastLine, suggestions)) {
            return limitAndSort(suggestions);
        }

        // General completions
        if (partialWord.isEmpty()) {
            // Show common templates and keywords when no partial word
            addSnippetSuggestions("", suggestions);
            suggestions.addAll(Arrays.asList("if", "for", "while", "function", "local", "return"));
        } else {
            // Filter by partial word
            addSnippetSuggestions(partialWord, suggestions);
            addKeywordSuggestions(partialWord, suggestions);
            addBuiltinFunctionSuggestions(partialWord, suggestions);
            addLibrarySuggestions(partialWord, suggestions);
            addUserFunctionSuggestions(partialWord, suggestions);
            addLocalVariableSuggestions(partialWord, suggestions);
        }

        return limitAndSort(suggestions);
    }

    /**
     * Updates the cache of user-defined functions and variables
     */
    private void updateUserSymbolsCache(String text) {
        String hash = String.valueOf(text.hashCode());
        if (hash.equals(cachedScriptHash)) {
            return; // Cache is still valid
        }

        cachedUserFunctions.clear();
        cachedLocalVariables.clear();

        // Find function definitions
        Matcher funcMatcher = FUNCTION_DEF_PATTERN.matcher(text);
        while (funcMatcher.find()) {
            cachedUserFunctions.add(funcMatcher.group(1));
        }

        // Find local variables
        Matcher localMatcher = LOCAL_VAR_PATTERN.matcher(text);
        while (localMatcher.find()) {
            cachedLocalVariables.add(localMatcher.group(1));
        }

        // Find global assignments
        Matcher globalMatcher = GLOBAL_VAR_PATTERN.matcher(text);
        while (globalMatcher.find()) {
            String varName = globalMatcher.group(1);
            // Exclude common keywords and built-ins
            if (!LUA_KEYWORDS.contains(varName) && !LUA_BUILTIN_FUNCTIONS.contains(varName)) {
                cachedLocalVariables.add(varName);
            }
        }

        cachedScriptHash = hash;
    }

    /**
     * Gets user-defined functions in the script
     */
    public Set<String> getUserFunctions() {
        return new HashSet<>(cachedUserFunctions);
    }

    /**
     * Gets local variables in the script
     */
    public Set<String> getLocalVariables() {
        return new HashSet<>(cachedLocalVariables);
    }

    private List<String> limitAndSort(List<String> suggestions) {
        // Remove duplicates while preserving order
        Set<String> seen = new LinkedHashSet<>(suggestions);
        suggestions = new ArrayList<>(seen);

        // Sort alphabetically
        Collections.sort(suggestions);

        // Limit to 25 suggestions
        if (suggestions.size() > 25) {
            suggestions = suggestions.subList(0, 25);
        }

        return suggestions;
    }

    private boolean handleImportCompletion(String lastLine, List<String> suggestions) {
        if (lastLine.trim().startsWith("import ")) {
            String partial = lastLine.replaceFirst("^\\s*import\\s+", "").trim().toLowerCase();

            for (String javaClass : JAVA_CLASSES) {
                if (partial.isEmpty() || javaClass.toLowerCase().contains(partial)) {
                    suggestions.add(javaClass);
                }
            }
            return true;
        }
        return false;
    }

    private boolean handleLibraryMethodCompletion(String lastLine, List<String> suggestions) {
        Matcher libMatcher = LIBRARY_PATTERN.matcher(lastLine);
        if (libMatcher.find()) {
            String libraryName = libMatcher.group(1);
            String partialMethod = libMatcher.group(2).toLowerCase();
            Set<String> methods = LIBRARY_FUNCTIONS.get(libraryName);

            if (methods != null) {
                for (String method : methods) {
                    if (partialMethod.isEmpty() || method.toLowerCase().startsWith(partialMethod)) {
                        suggestions.add(method + "()");
                    }
                }
            }
            return true;
        }
        return false;
    }

    private boolean handleDotNotation(String lastLine, String fullText, List<String> suggestions) {
        Matcher dotMatcher = DOT_PATTERN.matcher(lastLine);
        if (dotMatcher.find()) {
            String objectName = dotMatcher.group(1);

            // Check if it's a known library
            if (LIBRARY_FUNCTIONS.containsKey(objectName)) {
                Set<String> methods = LIBRARY_FUNCTIONS.get(objectName);
                for (String method : methods) {
                    suggestions.add(method + "()");
                }
            } else {
                // Add common methods for unknown objects
                for (String method : COMMON_METHODS) {
                    suggestions.add(method + "()");
                }
            }
            return true;
        }
        return false;
    }

    private boolean handleColonNotation(String lastLine, List<String> suggestions) {
        Matcher colonMatcher = COLON_PATTERN.matcher(lastLine);
        if (colonMatcher.find()) {
            String partialMethod = colonMatcher.group(2).toLowerCase();

            for (String method : COMMON_METHODS) {
                if (partialMethod.isEmpty() || method.toLowerCase().startsWith(partialMethod)) {
                    suggestions.add(method + "()");
                }
            }
            return true;
        }
        return false;
    }

    private void addSnippetSuggestions(String partialWord, List<String> suggestions) {
        String lower = partialWord.toLowerCase();
        for (String snippetKey : CODE_SNIPPETS.keySet()) {
            if (snippetKey.startsWith(lower)) {
                suggestions.add(snippetKey + " [snippet]");
            }
        }
    }

    private void addKeywordSuggestions(String partialWord, List<String> suggestions) {
        String lower = partialWord.toLowerCase();
        for (String keyword : LUA_KEYWORDS) {
            if (keyword.startsWith(lower)) {
                suggestions.add(keyword);
            }
        }
    }

    private void addBuiltinFunctionSuggestions(String partialWord, List<String> suggestions) {
        String lower = partialWord.toLowerCase();
        for (String builtin : LUA_BUILTIN_FUNCTIONS) {
            if (builtin.toLowerCase().startsWith(lower)) {
                suggestions.add(builtin + "()");
            }
        }
    }

    private void addLibrarySuggestions(String partialWord, List<String> suggestions) {
        String lower = partialWord.toLowerCase();
        for (String library : LUA_LIBRARIES) {
            if (library.startsWith(lower)) {
                suggestions.add(library);
            }
        }
    }

    private void addUserFunctionSuggestions(String partialWord, List<String> suggestions) {
        String lower = partialWord.toLowerCase();
        for (String funcName : cachedUserFunctions) {
            if (funcName.toLowerCase().startsWith(lower)) {
                suggestions.add(funcName + "()");
            }
        }
    }

    private void addLocalVariableSuggestions(String partialWord, List<String> suggestions) {
        String lower = partialWord.toLowerCase();
        for (String varName : cachedLocalVariables) {
            if (varName.toLowerCase().startsWith(lower)) {
                suggestions.add(varName);
            }
        }
    }

    /**
     * Gets the snippet template for a given key
     */
    public String getSnippet(String key) {
        // Remove the " [snippet]" suffix if present
        String cleanKey = key.replace(" [snippet]", "");
        return CODE_SNIPPETS.get(cleanKey);
    }

    /**
     * Checks if the suggestion is a snippet
     */
    public boolean isSnippet(String suggestion) {
        return suggestion != null && suggestion.endsWith(" [snippet]");
    }

    /**
     * Gets the last line of text up to the cursor
     */
    private String getLastLine(String text) {
        int lastNewline = text.lastIndexOf('\n');
        if (lastNewline == -1) {
            return text;
        }
        return text.substring(lastNewline + 1);
    }

    /**
     * Gets the partial word before the cursor
     */
    public String getPartialWord(String text) {
        // Check if we're in an import statement
        String lastLine = getLastLine(text);
        if (lastLine.trim().startsWith("import ")) {
            String importPrefix = "import ";
            int importIndex = lastLine.indexOf(importPrefix);
            if (importIndex != -1) {
                return lastLine.substring(importIndex + importPrefix.length()).trim();
            }
        }

        // Default behavior - find word boundary
        int end = text.length();
        int start = end;
        while (start > 0) {
            char c = text.charAt(start - 1);
            if (Character.isLetterOrDigit(c) || c == '_') {
                start--;
            } else {
                break;
            }
        }
        return text.substring(start, end);
    }

    /**
     * Analyzes the script and returns information about defined symbols
     */
    public ScriptAnalysis analyzeScript(String text) {
        updateUserSymbolsCache(text);

        ScriptAnalysis analysis = new ScriptAnalysis();
        analysis.setFunctions(new ArrayList<>(cachedUserFunctions));
        analysis.setVariables(new ArrayList<>(cachedLocalVariables));

        // Count lines
        analysis.setLineCount(text.isEmpty() ? 0 : text.split("\n").length);

        return analysis;
    }

    /**
     * Inner class to hold script analysis results
     */
    public static class ScriptAnalysis {
        private List<String> functions = new ArrayList<>();
        private List<String> variables = new ArrayList<>();
        private int lineCount;

        public List<String> getFunctions() {
            return functions;
        }

        public void setFunctions(List<String> functions) {
            this.functions = functions;
        }

        public List<String> getVariables() {
            return variables;
        }

        public void setVariables(List<String> variables) {
            this.variables = variables;
        }

        public int getLineCount() {
            return lineCount;
        }

        public void setLineCount(int lineCount) {
            this.lineCount = lineCount;
        }

        @Override
        public String toString() {
            return "ScriptAnalysis{" +
                    "functions=" + functions.size() +
                    ", variables=" + variables.size() +
                    ", lines=" + lineCount +
                    '}';
        }
    }
}