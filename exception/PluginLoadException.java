package plugin.exception;

/**
 * Exception thrown when a plugin fails to load.
 */
public class PluginLoadException extends PluginException {
    
    private final String pluginPath;
    private final String failureReason;
    
    public PluginLoadException(String message, String pluginPath) {
        super(message, pluginPath);
        this.pluginPath = pluginPath;
        this.failureReason = message;
    }
    
    public PluginLoadException(String message, String pluginPath, Throwable cause) {
        super(message, pluginPath, cause);
        this.pluginPath = pluginPath;
        this.failureReason = message;
    }
    
    /**
     * Get the path to the plugin that failed to load.
     */
    public String getPluginPath() {
        return pluginPath;
    }
    
    /**
     * Get the reason the plugin failed to load.
     */
    public String getFailureReason() {
        return failureReason;
    }
    
    @Override
    public String getMessage() {
        return String.format("Failed to load plugin from %s: %s", pluginPath, failureReason);
    }
}

