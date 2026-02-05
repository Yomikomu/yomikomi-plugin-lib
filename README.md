# Yomikomu Plugin System Documentation

## Overview
Yomikomu includes a plugin system that allows developers to extend the application's functionality. Plugins can provide custom manga sources, image processing, UI extensions, analytics, export features, and more.

## Directory Structure

```
~/.yomikomi/plugins/           # User plugins directory (in home folder)
├── lib/                     # Shared libraries for all plugins
│   └── *.jar               # JAR files available to all plugins
├── plugin_name/            # Individual plugin directory
│   ├── plugin.yaml         # Plugin manifest (YAML format)
│   ├── plugin.jar          # Plugin JAR file
│   └── lib/               # Plugin-specific libraries
│       └── *.jar          # JAR files for this plugin only
```

## Plugin Manifest (plugin.yaml)

Each plugin must have a `plugin.yaml` manifest file:

```yaml
id: com.example.myplugin
name: My Plugin
version: 1.0.0
author: Your Name
description: Description of what your plugin does

capability: GENERAL          # See Capability Types below
main.class: com.example.MyPluginClass

license: MIT
website: https://github.com/yourusername/plugin
dependencies: []
api.versions: 1.0
```

## Capability Types

| Capability | Description |
|------------|-------------|
| `DATA_SOURCE` | Custom manga/data source |
| `IMAGE_PROCESSING` | Image filters and transformations |
| `UI_EXTENSION` | Additional UI elements |
| `ANALYTICS` | Reading statistics |
| `EXPORT` | Export functionality |
| `NOTIFICATION` | Alerts and notifications |
| `SYNC` | External service sync |
| `GENERAL` | General-purpose extension |

## Plugin Interface

All plugins must implement the `YomikomuPlugin` interface:

```java
package plugin;

import model.Manga;
import model.Chapter;

public interface YomikomuPlugin {
    // Required methods
    String getId();
    String getName();
    String getVersion();
    String getAuthor();
    String getDescription();
    PluginCapability getCapability();
    
    // Optional lifecycle methods
    default void init(PluginContext context) {}
    default void onMangaLoaded(Manga manga) {}
    default void onChapterLoaded(Chapter chapter, Manga manga) {}
    default byte[] onPageLoaded(byte[] imageData, int pageIndex) { return imageData; }
    default void onReadingComplete(Chapter chapter, Manga manga) {}
    default void destroy() {}
    default boolean isEnabled() { return true; }
}
```

## PluginContext API

Plugins receive a `PluginContext` that provides access to application APIs:

```java
public class PluginContext {
    // API Access
    public MangaDexClient getApiClient();
    public BookmarkStore getBookmarkStore();
    public ReadingProgressStore getReadingProgressStore();
    public RecentMangasStore getRecentMangasStore();
    public CacheManager getCacheManager();
    public PluginManager getPluginManager();
    
    // UI Integration
    public void addMenu(JMenu menu);
    public void addMenuItem(String menuName, JMenuItem menuItem);
    
    // Event Registration
    public void onMangaLoaded(Consumer<Manga> callback);
    public void onChapterLoaded(ChapterCallback callback);
    public void onPageLoaded(PageCallback callback);
}
```

## Building a Plugin

### 1. Create Project Structure

```
my-plugin/
├── pom.xml
└── src/main/java/
    └── com/example/
        └── MyPlugin.java
```

### 2. Add Dependencies to pom.xml

```xml
<dependencies>
    <dependency>
        <groupId>site.meowcat</groupId>
        <artifactId>Yomikomu</artifactId>
        <version>preview</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

### 3. Implement Plugin Class

```java
package com.example;

import plugin.*;

public class MyPlugin implements YomikomuPlugin {
    @Override
    public String getId() { return "com.example.myplugin"; }
    @Override
    public String getName() { return "My Plugin"; }
    @Override
    public String getVersion() { return "1.0.0"; }
    @Override
    public String getAuthor() { return "Your Name"; }
    @Override
    public String getDescription() { return "My plugin description"; }
    @Override
    public PluginCapability getCapability() { return PluginCapability.GENERAL; }
    
    @Override
    public void init(PluginContext context) {
        // Initialize plugin
    }
}
```

### 4. Create Manifest File

Create `src/main/resources/plugin.yaml`:

```yaml
id: com.example.myplugin
name: My Plugin
version: 1.0.0
author: Your Name
description: My plugin description
capability: GENERAL
main.class: com.example.MyPlugin
```

### 5. Build and Install

```bash
# Build JAR
mvn package

# Install to Yomikomu plugins directory
cp target/my-plugin.jar ~/.yomikomi/plugins/my_plugin/
```

## JAR Manifest Alternative

Instead of `plugin.yaml`, you can include plugin metadata in the JAR's `META-INF/MANIFEST.MF`:

```
Plugin-Id: com.example.myplugin
Plugin-Name: My Plugin
Plugin-Version: 1.0.0
Plugin-Author: Your Name
Plugin-Description: My plugin description
Plugin-Capability: GENERAL
Plugin-Main-Class: com.example.MyPlugin
Plugin-License: MIT
Plugin-Website: https://github.com/example
Plugin-Dependencies:
Plugin-Api-Versions: 1.0
```

## Event Hooks

### Manga Loading
```java
@Override
public void onMangaLoaded(Manga manga) {
    // Called when user selects a manga
    System.out.println("Loaded: " + manga.title());
}
```

### Chapter Loading
```java
@Override
public void onChapterLoaded(Chapter chapter, Manga manga) {
    // Called when chapter is loaded
    System.out.println("Reading: " + chapter.title());
}
```

### Image Processing
```java
@Override
public byte[] onPageLoaded(byte[] imageData, int pageIndex) {
    // Modify page image data
    // Return modified data or null to keep original
    return imageData;
}
```

### Reading Complete
```java
@Override
public void onReadingComplete(Chapter chapter, Manga manga) {
    // Called when user finishes a chapter
    System.out.println("Completed: " + chapter.title());
}
```

## Using Shared Libraries

Plugins can use libraries from the shared `lib/` folder:

```
~/.yomikomi/plugins/
├── lib/
│   ├── commons-io.jar
│   └── gson.jar
└── my_plugin/
    └── plugin.jar
```

Plugin-specific libraries go in the plugin's own `lib/` folder:

```
~/.yomikomi/plugins/my_plugin/
├── plugin.jar
└── lib/
    └── custom-library.jar
```

## UI Integration

### Adding Menu Items

```java
@Override
public void init(PluginContext context) {
    JMenuItem myItem = new JMenuItem("My Action");
    myItem.addActionListener(e -> {
        // Handle action
    });
    context.addMenuItem("Plugins", myItem);
}
```

### Adding Custom Menus

```java
@Override
public void init(PluginContext context) {
    JMenu myMenu = new JMenu("My Plugin");
    myMenu.add(new JMenuItem("Action 1"));
    myMenu.add(new JMenuItem("Action 2"));
    context.addMenu(myMenu);
}
```

## Plugin Manager UI

Access the Plugin Manager from the application menu:
`Plugins → Plugin Manager`

Features:
- View installed plugins
- Enable/disable plugins
- Remove plugins
- Manage shared libraries
- Add new plugins

## Troubleshooting

### Plugin Not Loading
1. Check `~/.yomikomi/logs/` for errors
2. Verify plugin.yaml syntax
3. Ensure main class exists and implements YomikomuPlugin
4. Check API version compatibility

### Class Not Found Errors
1. Ensure dependencies are in `lib/` folder
2. Check library JARs are valid
3. Verify classpath includes all required JARs

### Permission Issues
1. Ensure plugins directory is writable
2. Check file permissions on plugin JARs

## Example Plugin

See `plugins/example_plugin/` for a complete working example.

## API Version Compatibility

The plugin system supports API version 1.0. Future versions will maintain backward compatibility.

```
api.versions: 1.0,2.0  # Plugin supports both versions
```

## Security Considerations

- Only load plugins from trusted sources
- Review plugin code before installation
- Plugin JARs are loaded with limited permissions
- User can disable plugins at any time

