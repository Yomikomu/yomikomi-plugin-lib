package plugin;

/**
 * Defines the capabilities a plugin can provide to the application.
 * Plugins can offer one or more of these capabilities.
 */
public enum PluginCapability {
    /**
     * Plugin provides a custom manga/data source.
     */
    DATA_SOURCE,
    
    /**
     * Plugin provides image processing/filtering.
     */
    IMAGE_PROCESSING,
    
    /**
     * Plugin adds UI elements to the application.
     */
    UI_EXTENSION,
    
    /**
     * Plugin provides reading statistics or analytics.
     */
    ANALYTICS,
    
    /**
     * Plugin provides export functionality.
     */
    EXPORT,
    
    /**
     * Plugin provides notifications or alerts.
     */
    NOTIFICATION,
    
    /**
     * Plugin is a general-purpose extension with multiple features.
     */
    GENERAL,
    
    /**
     * Plugin provides sync functionality with external services.
     */
    SYNC
}

