package com.example;

import plugin.PluginContext;
import plugin.ShioriPlugin;
import model.Manga;
import model.Chapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;

/**
 * Example plugin that demonstrates the Shiori plugin system.
 * This plugin adds a simple "Hello World" feature with logging.
 */
public class HelloWorldPlugin implements ShioriPlugin {
    
    private static final Logger logger = LogManager.getLogger(HelloWorldPlugin.class);
    
    private PluginContext context;
    private boolean initialized = false;

    @Override
    public String getId() {
        return "com.example.helloworld";
    }

    @Override
    public String getName() {
        return "Hello World Plugin";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String getAuthor() {
        return "Example Author";
    }

    @Override
    public String getDescription() {
        return "A simple example plugin that demonstrates the Shiori plugin system. " +
               "Logs manga and chapter events to the console.";
    }

    @Override
    public plugin.PluginCapability getCapability() {
        return plugin.PluginCapability.GENERAL;
    }

    @Override
    public void init(PluginContext context) {
        this.context = context;
        this.initialized = true;
        logger.info("Hello World Plugin initialized!");
        
        // Example: Add a menu item to the application
        if (context != null) {
            JMenuItem helloItem = new JMenuItem("Say Hello");
            helloItem.addActionListener(e -> {
                JOptionPane.showMessageDialog(
                        null,
                        "Hello from the Hello World Plugin!",
                        "Plugin Message",
                        JOptionPane.INFORMATION_MESSAGE
                );
            });
            context.addMenuItem("Plugins", helloItem);
        }
    }

    @Override
    public void onMangaLoaded(Manga manga) {
        if (initialized) {
            logger.info("Hello World Plugin: Manga loaded - {} (ID: {})", 
                    manga.title(), manga.id());
        }
    }

    @Override
    public void onChapterLoaded(Chapter chapter, Manga manga) {
        if (initialized) {
            logger.info("Hello World Plugin: Chapter loaded - {} from {}", 
                    chapter.title(), manga.title());
        }
    }

    @Override
    public byte[] onPageLoaded(byte[] imageData, int pageIndex) {
        // Example: Log page loading (without modifying the image)
        if (initialized && pageIndex == 0) {
            logger.debug("Hello World Plugin: First page loaded, {} bytes", 
                    imageData != null ? imageData.length : 0);
        }
        // Return null to keep the original image unchanged
        return null;
    }

    @Override
    public void onReadingComplete(Chapter chapter, Manga manga) {
        if (initialized) {
            logger.info("Hello World Plugin: Finished reading {} - {}", 
                    manga.title(), chapter.title());
        }
    }

    @Override
    public void destroy() {
        logger.info("Hello World Plugin destroyed!");
        this.initialized = false;
        this.context = null;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}

