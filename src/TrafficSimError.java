/**
 * Custom error handling class for the Traffic Simulator application.
 * Provides standardized error messages with severity levels and categories.
 */
public class TrafficSimError {
    
    // Error severity levels
    public enum Severity {
        INFO,       // Informational message, not an error
        WARNING,    // Potential issue that doesn't stop execution
        ERROR,      // Serious issue that may affect functionality
        CRITICAL    // Fatal error that prevents proper operation
    }
    
    // Error categories for better organization
    public enum Category {
        NETWORK,    // Road network structure issues
        JUNCTION,   // Junction-related errors
        VEHICLE,    // Vehicle behavior or positioning errors
        TRAFFIC,    // Traffic flow or congestion issues
        RENDERING,  // Visual display problems
        SYSTEM      // General system or application errors
    }
    
    /**
     * Logs an error message with the specified severity and category.
     * 
     * @param severity The error severity level
     * @param category The error category
     * @param message The detailed error message
     */
    public static void log(Severity severity, Category category, String message) {
        String prefix = "[" + severity + "][" + category + "] ";
        
        switch (severity) {
            case INFO:
                System.out.println(prefix + message);
                break;
            case WARNING:
                System.out.println("\u001B[33m" + prefix + message + "\u001B[0m"); // Yellow
                break;
            case ERROR:
                System.err.println(prefix + message);
                break;
            case CRITICAL:
                System.err.println("\u001B[31m" + prefix + message + "\u001B[0m"); // Red
                // Optionally add stack trace for critical errors
                new Exception("Stack trace for critical error").printStackTrace();
                break;
        }
    }
    
    /**
     * Convenience method for logging info messages.
     */
    public static void info(Category category, String message) {
        log(Severity.INFO, category, message);
    }
    
    /**
     * Convenience method for logging warning messages.
     */
    public static void warning(Category category, String message) {
        log(Severity.WARNING, category, message);
    }
    
    /**
     * Convenience method for logging error messages.
     */
    public static void error(Category category, String message) {
        log(Severity.ERROR, category, message);
    }
    
    /**
     * Convenience method for logging critical messages.
     */
    public static void critical(Category category, String message) {
        log(Severity.CRITICAL, category, message);
    }
} 