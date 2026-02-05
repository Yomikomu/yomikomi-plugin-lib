package plugin;

import model.Manga;
import model.Chapter;

/**
 * Main interface for Shiori plugins.
 * All plugins must implement this interface to be loaded by the application.
 */
public interface ShioriPlugin {
    
    /**
     * Get the unique identifier for this plugin.
     * @return unique plugin ID
     */
    String getId();
    
    /**
     * Get the human-readable name of this plugin.
     * @return plugin name
     */
    String getName();
    
    /**
     * Get the version of this plugin.
     * @return plugin version string
     */
    String getVersion();
    
    /**
     * Get the author of this plugin.
     * @return plugin author
     */
    String getAuthor();
    
    /**
     * Get a description of what this plugin does.
     * @return plugin description
     */
    String getDescription();
    
    /**
     * Get the capabilities this plugin provides.
     * @return capability type
     */
    PluginCapability getCapability();
    
    /**
     * Initialize the plugin. Called when the plugin is loaded.
     * @param context The plugin context providing access to app APIs
     */
    default void init(PluginContext context) {}
    
    /**
     * Called when a manga is loaded/selected.
     * @param manga The manga that was loaded
     */
    default void onMangaLoaded(Manga manga) {}
    
    /**
     * Called when a chapter is loaded.
     * @param chapter The chapter that was loaded
     * @param manga The parent manga
     */
    default void onChapterLoaded(Chapter chapter, Manga manga) {}
    
    /**
     * Called for each page image loaded. Can be used to transform images.
     * @param imageData The raw image data
     * @param pageIndex The page number (0-based)
     * @return Modified image data, or null to use original
     */
    default byte[] onPageLoaded(byte[] imageData, int pageIndex) {
        return imageData;
    }
    
    /**
     * Called when reading a chapter is complete.
     * @param chapter The chapter that was completed
     * @param manga The parent manga
     */
    default void onReadingComplete(Chapter chapter, Manga manga) {}
    
    /**
     * Called when the application is shutting down.
     * Perform cleanup operations here.
     */
    default void destroy() {}
    
    /**
     * Check if this plugin is enabled.
     * @return true if plugin should be active
     */
    default boolean isEnabled() {
        return true;
    }
}

