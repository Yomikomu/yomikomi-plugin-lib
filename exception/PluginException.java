package plugin.exception;

/**
 * Base exception class for plugin-related errors.
 */
public class PluginException extends RuntimeException {
    
    private final String pluginId;
    
    public PluginException(String message) {
        super(message);
        this.pluginId = null;
    }
    
    public PluginException(String message, String pluginId) {
        super(message);
        this.pluginId = pluginId;
    }
    
    public PluginException(String message, Throwable cause) {
        super(message, cause);
        this.pluginId = null;
    }
    
    public PluginException(String message, String pluginId, Throwable cause) {
        super(message, cause);
        this.pluginId = pluginId;
    }
    
    /**
     * Get the ID of the plugin that caused this exception.
     * @return plugin ID, or null if not applicable
     */
    public String getPluginId() {
        return pluginId;
    }
}

