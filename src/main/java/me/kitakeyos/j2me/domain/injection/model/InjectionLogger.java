package me.kitakeyos.j2me.domain.injection.model;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.function.Consumer;

/**
 * Logger for injection scripts.
 * Messages are forwarded to the UI via a callback.
 *
 * Usage in InjectionEntry:
 * <pre>
 * logger.info("Loading class...");
 * logger.success("Done!");
 * logger.warn("Something unexpected");
 * logger.error("Failed: " + e.getMessage());
 * </pre>
 */
public class InjectionLogger {

    public enum Level {
        INFO, SUCCESS, WARN, ERROR
    }

    public static class LogEntry {
        public final Level level;
        public final String message;
        public final String timestamp;

        public LogEntry(Level level, String message) {
            this.level = level;
            this.message = message;
            this.timestamp = new SimpleDateFormat("HH:mm:ss").format(new Date());
        }

        @Override
        public String toString() {
            return "[" + timestamp + "] [" + level + "] " + message;
        }
    }

    private final Consumer<LogEntry> callback;

    public InjectionLogger(Consumer<LogEntry> callback) {
        this.callback = callback;
    }

    public void info(String message) {
        callback.accept(new LogEntry(Level.INFO, message));
    }

    public void success(String message) {
        callback.accept(new LogEntry(Level.SUCCESS, message));
    }

    public void warn(String message) {
        callback.accept(new LogEntry(Level.WARN, message));
    }

    public void error(String message) {
        callback.accept(new LogEntry(Level.ERROR, message));
    }

    public void error(String message, Throwable throwable) {
        callback.accept(new LogEntry(Level.ERROR, message + ": " + throwable.getMessage()));
    }
}
