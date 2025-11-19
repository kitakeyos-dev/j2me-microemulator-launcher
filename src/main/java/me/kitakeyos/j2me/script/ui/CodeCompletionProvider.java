package me.kitakeyos.j2me.script.ui;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Optimized CodeCompletionProvider with improved performance and simplified logic
 */
public class CodeCompletionProvider {
    private static final Set<String> LUA_KEYWORDS = new HashSet<>(Arrays.asList(
            "and", "break", "do", "else", "elseif", "end", "false", "for", "function",
            "if", "in", "local", "nil", "not", "or", "repeat", "return", "then",
            "true", "until", "while", "goto"
    ));

    private static final Set<String> LUA_BUILTIN_FUNCTIONS = new HashSet<>(Arrays.asList(
            "print", "type", "tostring", "tonumber", "pairs", "ipairs", "next",
            "require", "assert", "error", "pcall", "xpcall"
    ));

    private static final Set<String> LUA_LIBRARIES = new HashSet<>(Arrays.asList(
            "string", "table", "math", "io", "os"
    ));

    // Reduced library functions for better performance
    private static final Map<String, Set<String>> LIBRARY_FUNCTIONS = new HashMap<>();
    static {
        LIBRARY_FUNCTIONS.put("string", new HashSet<>(Arrays.asList(
                "len", "sub", "find", "gsub", "lower", "upper", "format"
        )));

        LIBRARY_FUNCTIONS.put("table", new HashSet<>(Arrays.asList(
                "insert", "remove", "concat", "sort"
        )));

        LIBRARY_FUNCTIONS.put("math", new HashSet<>(Arrays.asList(
                "abs", "max", "min", "floor", "ceil", "sqrt", "random"
        )));

        LIBRARY_FUNCTIONS.put("io", new HashSet<>(Arrays.asList(
                "open", "close", "read", "write"
        )));

        LIBRARY_FUNCTIONS.put("os", new HashSet<>(Arrays.asList(
                "time", "date", "execute", "exit"
        )));
    }

    // Reduced Java classes for essential imports only
    private static final Set<String> JAVA_CLASSES = new HashSet<>(Arrays.asList(
            "java.util.ArrayList", "java.util.HashMap", "java.util.List",
            "java.util.Map", "java.io.File", "java.lang.String",
            "java.lang.Integer", "java.lang.Double", "java.util.Arrays",
            "java.util.Date", "java.util.Random"
    ));

    private static final Set<String> COMMON_METHODS = new HashSet<>(Arrays.asList(
            "add", "remove", "size", "get", "put", "contains", "isEmpty",
            "toString", "length", "substring", "indexOf"
    ));

    // Pre-compiled patterns
    private static final Pattern LIBRARY_PATTERN = Pattern.compile("\\b(string|table|math|io|os)\\.([a-zA-Z_]*)$");
    private static final Pattern DOT_PATTERN = Pattern.compile("\\b([a-zA-Z_][a-zA-Z0-9_]*)\\.$");
    private static final Pattern COLON_PATTERN = Pattern.compile("\\b([a-zA-Z_][a-zA-Z0-9_]*):([a-zA-Z_]*)$");

    /**
     * Provides completion suggestions with optimized performance
     */
    public List<String> getSuggestions(String text, int caretOffset) {
        List<String> suggestions = new ArrayList<>();
        if (caretOffset < 0 || caretOffset > text.length()) {
            return suggestions;
        }

        String prefix = text.substring(0, caretOffset);
        String lastLine = getLastLine(prefix);
        String partialWord = getPartialWord(prefix);

        System.out.println("Getting suggestions for partialWord: '" + partialWord + "'"); // Debug

        // Handle specific completion contexts
        if (handleImportCompletion(lastLine, suggestions)) {
            System.out.println("Found import completions: " + suggestions.size()); // Debug
            return suggestions;
        }

        if (handleLibraryMethodCompletion(lastLine, suggestions)) {
            System.out.println("Found library method completions: " + suggestions.size()); // Debug
            return suggestions;
        }

        if (handleDotNotation(lastLine, suggestions)) {
            System.out.println("Found dot notation completions: " + suggestions.size()); // Debug
            return suggestions;
        }

        if (handleColonNotation(lastLine, suggestions)) {
            System.out.println("Found colon notation completions: " + suggestions.size()); // Debug
            return suggestions;
        }

        // General completions - show all if partial word is empty or very short
        if (partialWord.isEmpty()) {
            // Show common keywords and functions when no partial word
            suggestions.addAll(Arrays.asList("if", "then", "end", "for", "do", "while", "function", "local"));
            suggestions.addAll(Arrays.asList("print()", "type()", "tostring()", "pairs()", "ipairs()"));
        } else {
            // Show filtered suggestions
            addKeywordSuggestions(partialWord, suggestions);
            addBuiltinFunctionSuggestions(partialWord, suggestions);
            addLibrarySuggestions(partialWord, suggestions);
        }

        // Limit suggestions to prevent overwhelming the user
        if (suggestions.size() > 20) {
            suggestions = suggestions.subList(0, 20);
        }

        // Simple alphabetical sort
        Collections.sort(suggestions);

        System.out.println("Final suggestions count: " + suggestions.size()); // Debug
        return suggestions;
    }

    private boolean handleImportCompletion(String lastLine, List<String> suggestions) {
        if (lastLine.trim().startsWith("import ")) {
            String partial = lastLine.replaceFirst("^\\s*import\\s+", "").trim();

            for (String javaClass : JAVA_CLASSES) {
                if (partial.isEmpty() || javaClass.toLowerCase().contains(partial.toLowerCase())) {
                    suggestions.add(javaClass);
                }
            }

            Collections.sort(suggestions);
            return true;
        }
        return false;
    }

    private boolean handleLibraryMethodCompletion(String lastLine, List<String> suggestions) {
        Matcher libMatcher = LIBRARY_PATTERN.matcher(lastLine);
        if (libMatcher.find()) {
            String libraryName = libMatcher.group(1);
            String partialMethod = libMatcher.group(2);
            Set<String> methods = LIBRARY_FUNCTIONS.get(libraryName);
            if (methods != null) {
                for (String method : methods) {
                    if (method.startsWith(partialMethod)) {
                        suggestions.add(method + "()");
                    }
                }
            }
            return true;
        }
        return false;
    }

    private boolean handleDotNotation(String lastLine, List<String> suggestions) {
        Matcher dotMatcher = DOT_PATTERN.matcher(lastLine);
        if (dotMatcher.find()) {
            // Add common methods
            for (String method : COMMON_METHODS) {
                suggestions.add(method + "()");
            }
            return true;
        }
        return false;
    }

    private boolean handleColonNotation(String lastLine, List<String> suggestions) {
        Matcher colonMatcher = COLON_PATTERN.matcher(lastLine);
        if (colonMatcher.find()) {
            String partialMethod = colonMatcher.group(2);
            for (String method : COMMON_METHODS) {
                if (method.startsWith(partialMethod)) {
                    suggestions.add(method + "()");
                }
            }
            return true;
        }
        return false;
    }

    private void addKeywordSuggestions(String partialWord, List<String> suggestions) {
        for (String keyword : LUA_KEYWORDS) {
            if (keyword.startsWith(partialWord)) {
                suggestions.add(keyword);
            }
        }
    }

    private void addBuiltinFunctionSuggestions(String partialWord, List<String> suggestions) {
        for (String builtin : LUA_BUILTIN_FUNCTIONS) {
            if (builtin.startsWith(partialWord)) {
                suggestions.add(builtin + "()");
            }
        }
    }

    private void addLibrarySuggestions(String partialWord, List<String> suggestions) {
        for (String library : LUA_LIBRARIES) {
            if (library.startsWith(partialWord)) {
                suggestions.add(library);
            }
        }
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
     * Gets the partial word before the cursor - optimized version
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
        while (start > 0 && (Character.isLetterOrDigit(text.charAt(start - 1)) || text.charAt(start - 1) == '_')) {
            start--;
        }
        return text.substring(start, end);
    }
}