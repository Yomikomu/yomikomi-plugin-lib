package plugin;

import api.MangaDexClient;
import bookmark.BookmarkStore;
import reading.ReadingProgressStore;
import recent.RecentMangasStore;
import api.CacheManager;

import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import java.util.function.Consumer;
import java.util.List;
import model.Manga;
import model.Chapter;

/**
 * Provides plugins with access to application APIs and functionality.
 * This context is passed to plugins during initialization and throughout their lifecycle.
 */
public class PluginContext {
    
    private final MangaDexClient apiClient;
    private final BookmarkStore bookmarkStore;
    private final ReadingProgressStore readingProgressStore;
    private final RecentMangasStore recentMangasStore;
    private final CacheManager cacheManager;
    private final PluginManager pluginManager;
    private final JMenuBar menuBar;
    
    public PluginContext(
            MangaDexClient apiClient,
            BookmarkStore bookmarkStore,
            ReadingProgressStore readingProgressStore,
            RecentMangasStore recentMangasStore,
            CacheManager cacheManager,
            PluginManager pluginManager,
            JMenuBar menuBar) {
        this.apiClient = apiClient;
        this.bookmarkStore = bookmarkStore;
        this.readingProgressStore = readingProgressStore;
        this.recentMangasStore = recentMangasStore;
        this.cacheManager = cacheManager;
        this.pluginManager = pluginManager;
        this.menuBar = menuBar;
    }
    
    /**
     * Get the MangaDex API client for making API requests.
     * @return MangaDexClient instance
     */
    public MangaDexClient getApiClient() {
        return apiClient;
    }
    
    /**
     * Get the bookmark store for managing bookmarks.
     * @return BookmarkStore instance
     */
    public BookmarkStore getBookmarkStore() {
        return bookmarkStore;
    }
    
    /**
     * Get the reading progress store for tracking reading position.
     * @return ReadingProgressStore instance
     */
    public ReadingProgressStore getReadingProgressStore() {
        return readingProgressStore;
    }
    
    /**
     * Get the recent mangas store.
     * @return RecentMangasStore instance
     */
    public RecentMangasStore getRecentMangasStore() {
        return recentMangasStore;
    }
    
    /**
     * Get the cache manager for image caching.
     * @return CacheManager instance
     */
    public CacheManager getCacheManager() {
        return cacheManager;
    }
    
    /**
     * Get the plugin manager for interacting with other plugins.
     * @return PluginManager instance
     */
    public PluginManager getPluginManager() {
        return pluginManager;
    }
    
    /**
     * Add a menu to the application's menu bar.
     * @param menu The menu to add
     */
    public void addMenu(JMenu menu) {
        if (menuBar != null) {
            menuBar.add(menu);
        }
    }
    
    /**
     * Add a menu item to the specified existing menu.
     * @param menuName The name of the parent menu
     * @param menuItem The menu item to add
     */
    public void addMenuItem(String menuName, JMenuItem menuItem) {
        if (menuBar != null) {
            for (int i = 0; i < menuBar.getMenuCount(); i++) {
                JMenu menu = menuBar.getMenu(i);
                if (menu != null && menu.getText().equals(menuName)) {
                    menu.add(menuItem);
                    break;
                }
            }
        }
    }
    
    /**
     * Register a callback for when a manga is loaded.
     * @param callback The callback to execute when manga is loaded
     */
    public void onMangaLoaded(Consumer<Manga> callback) {
        if (pluginManager != null) {
            pluginManager.registerMangaCallback(callback);
        }
    }
    
    /**
     * Register a callback for when a chapter is loaded.
     * @param callback The callback to execute when chapter is loaded
     */
    public void onChapterLoaded(Consumer<ChapterCallback> callback) {
        if (pluginManager != null) {
            pluginManager.registerChapterCallback(callback);
        }
    }
    
    /**
     * Register a callback for when a page is loaded (for image processing).
     * @param callback The callback to execute for each page
     */
    public void onPageLoaded(PageCallback callback) {
        if (pluginManager != null) {
            pluginManager.registerPageCallback(callback);
        }
    }
    
    /**
     * Callback interface for chapter loading.
     */
    @FunctionalInterface
    public interface ChapterCallback {
        void accept(Chapter chapter, Manga manga);
    }
    
    /**
     * Callback interface for page loading.
     */
    @FunctionalInterface
    public interface PageCallback {
        byte[] accept(byte[] imageData, int pageIndex, Chapter chapter, Manga manga);
    }
}

