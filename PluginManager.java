package plugin;

import model.Manga;
import model.Chapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.JMenuItem;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Manages all loaded plugins in the application.
 * Handles plugin lifecycle, callbacks, and communication between plugins.
 */
public class PluginManager {
    
    private static final Logger logger = LogManager.getLogger(PluginManager.class);
    
    private final Map<String, ShioriPlugin> plugins = new ConcurrentHashMap<>();
    private final Map<String, PluginDescriptor> descriptors = new ConcurrentHashMap<>();
    private final Map<String, Boolean> enabledPlugins = new ConcurrentHashMap<>();
    private final Set<Consumer<Manga>> mangaCallbacks = ConcurrentHashMap.newKeySet();
    private final Set<Consumer<PluginContext.ChapterCallback>> chapterCallbacks = ConcurrentHashMap.newKeySet();
    private final Set<PluginContext.PageCallback> pageCallbacks = ConcurrentHashMap.newKeySet();
    private final PluginLoader loader;
    private boolean initialized = false;
    
    public PluginManager() {
        this.loader = new PluginLoader(this);
    }
    
    /**
     * Get the plugin loader.
     * @return PluginLoader instance
     */
    public PluginLoader getPluginLoader() {
        return loader;
    }
    
    /**
     * Initialize the plugin system and load all plugins.
     * @return true if plugins were loaded successfully
     */
    public boolean initialize() {
        if (initialized) {
            logger.warn("PluginManager already initialized");
            return true;
        }
        
        logger.info("Initializing plugin system...");
        boolean success = loader.loadAllPlugins();
        
        if (success) {
            initialized = true;
            logger.info("Plugin system initialized with {} plugins", plugins.size());
        } else {
            logger.error("Failed to initialize plugin system");
        }
        
        return success;
    }
    
    /**
     * Register a plugin with the manager.
     * @param plugin The plugin to register
     * @param descriptor The plugin's descriptor
     */
    public void registerPlugin(ShioriPlugin plugin, PluginDescriptor descriptor) {
        String pluginId = plugin.getId();
        
        if (plugins.containsKey(pluginId)) {
            logger.warn("Plugin {} already registered, skipping", pluginId);
            return;
        }
        
        plugins.put(pluginId, plugin);
        descriptors.put(pluginId, descriptor);
        enabledPlugins.put(pluginId, true);
        
        logger.info("Registered plugin: {} v{} by {}", 
                plugin.getName(), plugin.getVersion(), plugin.getAuthor());
    }
    
    /**
     * Get a plugin by its ID.
     * @param pluginId The plugin ID
     * @return Optional containing the plugin, or empty if not found
     */
    public Optional<ShioriPlugin> getPlugin(String pluginId) {
        return Optional.ofNullable(plugins.get(pluginId));
    }
    
    /**
     * Get all registered plugins.
     * @return Collection of all plugins
     */
    public Collection<ShioriPlugin> getAllPlugins() {
        return Collections.unmodifiableCollection(plugins.values());
    }
    
    /**
     * Get all enabled plugins.
     * @return Collection of enabled plugins
     */
    public Collection<ShioriPlugin> getEnabledPlugins() {
        List<ShioriPlugin> enabled = new ArrayList<>();
        for (Map.Entry<String, ShioriPlugin> entry : plugins.entrySet()) {
            if (enabledPlugins.getOrDefault(entry.getKey(), true)) {
                ShioriPlugin plugin = entry.getValue();
                if (plugin.isEnabled()) {
                    enabled.add(plugin);
                }
            }
        }
        return enabled;
    }
    
    /**
     * Get a plugin's descriptor by ID.
     * @param pluginId The plugin ID
     * @return Optional containing the descriptor, or empty if not found
     */
    public Optional<PluginDescriptor> getDescriptor(String pluginId) {
        return Optional.ofNullable(descriptors.get(pluginId));
    }
    
    /**
     * Get all plugin descriptors.
     * @return Collection of all descriptors
     */
    public Collection<PluginDescriptor> getAllDescriptors() {
        return Collections.unmodifiableCollection(descriptors.values());
    }
    
    /**
     * Enable a plugin by ID.
     * @param pluginId The plugin to enable
     * @return true if plugin was found and enabled
     */
    public boolean enablePlugin(String pluginId) {
        if (!plugins.containsKey(pluginId)) {
            logger.warn("Cannot enable unknown plugin: {}", pluginId);
            return false;
        }
        
        enabledPlugins.put(pluginId, true);
        logger.info("Enabled plugin: {}", pluginId);
        
        // Re-initialize the plugin
        ShioriPlugin plugin = plugins.get(pluginId);
        try {
            plugin.init(null); // Will be re-initialized with proper context later
        } catch (Exception e) {
            logger.error("Failed to re-initialize plugin {}: {}", pluginId, e.getMessage());
        }
        
        return true;
    }
    
    /**
     * Disable a plugin by ID.
     * @param pluginId The plugin to disable
     * @return true if plugin was found and disabled
     */
    public boolean disablePlugin(String pluginId) {
        if (!plugins.containsKey(pluginId)) {
            logger.warn("Cannot disable unknown plugin: {}", pluginId);
            return false;
        }
        
        enabledPlugins.put(pluginId, false);
        logger.info("Disabled plugin: {}", pluginId);
        
        // Destroy the plugin
        ShioriPlugin plugin = plugins.get(pluginId);
        try {
            plugin.destroy();
        } catch (Exception e) {
            logger.error("Failed to destroy plugin {}: {}", pluginId, e.getMessage());
        }
        
        return true;
    }
    
    /**
     * Check if a plugin is enabled.
     * @param pluginId The plugin ID
     * @return true if plugin is enabled
     */
    public boolean isEnabled(String pluginId) {
        return enabledPlugins.getOrDefault(pluginId, false);
    }
    
    /**
     * Notify all enabled plugins that a manga was loaded.
     * @param manga The loaded manga
     */
    public void notifyMangaLoaded(Manga manga) {
        for (ShioriPlugin plugin : getEnabledPlugins()) {
            try {
                plugin.onMangaLoaded(manga);
            } catch (Exception e) {
                logger.error("Plugin {} failed in onMangaLoaded: {}", plugin.getId(), e.getMessage());
            }
        }
    }
    
    /**
     * Notify all enabled plugins that a chapter was loaded.
     * @param chapter The loaded chapter
     * @param manga The parent manga
     */
    public void notifyChapterLoaded(Chapter chapter, Manga manga) {
        for (ShioriPlugin plugin : getEnabledPlugins()) {
            try {
                plugin.onChapterLoaded(chapter, manga);
            } catch (Exception e) {
                logger.error("Plugin {} failed in onChapterLoaded: {}", plugin.getId(), e.getMessage());
            }
        }
    }
    
    /**
     * Process page data through enabled plugins.
     * @param imageData The raw image data
     * @param pageIndex The page number (0-based)
     * @param chapter The current chapter
     * @param manga The current manga
     * @return Processed image data after all plugins
     */
    public byte[] processPage(byte[] imageData, int pageIndex, Chapter chapter, Manga manga) {
        byte[] result = imageData;
        
        for (ShioriPlugin plugin : getEnabledPlugins()) {
            try {
                byte[] processed = plugin.onPageLoaded(result, pageIndex);
                if (processed != null) {
                    result = processed;
                }
            } catch (Exception e) {
                logger.error("Plugin {} failed in onPageLoaded: {}", plugin.getId(), e.getMessage());
            }
        }
        
        // Also process through registered page callbacks
        for (PluginContext.PageCallback callback : pageCallbacks) {
            try {
                byte[] processed = callback.accept(result, pageIndex, chapter, manga);
                if (processed != null) {
                    result = processed;
                }
            } catch (Exception e) {
                logger.error("Page callback failed: {}", e.getMessage());
            }
        }
        
        return result;
    }
    
    /**
     * Notify all enabled plugins that reading is complete.
     * @param chapter The completed chapter
     * @param manga The parent manga
     */
    public void notifyReadingComplete(Chapter chapter, Manga manga) {
        for (ShioriPlugin plugin : getEnabledPlugins()) {
            try {
                plugin.onReadingComplete(chapter, manga);
            } catch (Exception e) {
                logger.error("Plugin {} failed in onReadingComplete: {}", plugin.getId(), e.getMessage());
            }
        }
    }
    
    /**
     * Register a callback for manga loading.
     * @param callback The callback to register
     */
    public void registerMangaCallback(Consumer<Manga> callback) {
        mangaCallbacks.add(callback);
    }
    
    /**
     * Register a callback for chapter loading.
     * @param callback The callback to register
     */
    public void registerChapterCallback(Consumer<PluginContext.ChapterCallback> callback) {
        chapterCallbacks.add(callback);
    }
    
    /**
     * Register a callback for page processing.
     * @param callback The callback to register
     */
    public void registerPageCallback(PluginContext.PageCallback callback) {
        pageCallbacks.add(callback);
    }
    
    /**
     * Get the number of loaded plugins.
     * @return Number of registered plugins
     */
    public int getPluginCount() {
        return plugins.size();
    }
    
    /**
     * Get the number of enabled plugins.
     * @return Number of enabled plugins
     */
    public int getEnabledCount() {
        return (int) enabledPlugins.values().stream().filter(v -> v).count();
    }
    
    /**
     * Shutdown all plugins and cleanup.
     */
    public void shutdown() {
        logger.info("Shutting down plugin system...");
        
        for (ShioriPlugin plugin : plugins.values()) {
            try {
                plugin.destroy();
                logger.info("Destroyed plugin: {}", plugin.getId());
            } catch (Exception e) {
                logger.error("Failed to destroy plugin {}: {}", plugin.getId(), e.getMessage());
            }
        }
        
        plugins.clear();
        descriptors.clear();
        enabledPlugins.clear();
        mangaCallbacks.clear();
        chapterCallbacks.clear();
        pageCallbacks.clear();
        
        initialized = false;
        logger.info("Plugin system shut down");
    }
}

