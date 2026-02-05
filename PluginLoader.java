package plugin;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.io.*;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.*;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Loads plugins from the plugins directory.
 * Handles discovery, class loading, and initialization of plugins.
 */
public class PluginLoader {
    
    private static final Logger logger = LogManager.getLogger(PluginLoader.class);
    
    private static final String PLUGINS_DIR = "plugins";
    private static final String LIB_DIR = "lib";
    private static final String PLUGIN_MANIFEST = "plugin.yaml";
    private static final String PLUGIN_CLASS_PROPERTY = "plugin.main.class";
    private static final String SHIORI_API_VERSION = "1.0";
    
    private final PluginManager pluginManager;
    private Path pluginsDirectory;
    private Path libraryDirectory;
    
    public PluginLoader(PluginManager pluginManager) {
        this.pluginManager = pluginManager;
        initializeDirectories();
    }
    
    /**
     * Initialize the plugins and library directories.
     */
    private void initializeDirectories() {
        try {
            // Set up plugins directory in user home
            Path userHome = Path.of(System.getProperty("user.home"));
            Path shioriHome = userHome.resolve(".shiori");
            
            pluginsDirectory = shioriHome.resolve(PLUGINS_DIR);
            libraryDirectory = pluginsDirectory.resolve(LIB_DIR);
            
            // Create directories if they don't exist
            Files.createDirectories(pluginsDirectory);
            Files.createDirectories(libraryDirectory);
            
            logger.info("Plugins directory: {}", pluginsDirectory);
            logger.info("Library directory: {}", libraryDirectory);
            
        } catch (IOException e) {
            logger.error("Failed to initialize plugin directories: {}", e.getMessage());
            // Fallback to current directory
            pluginsDirectory = Path.of(PLUGINS_DIR);
            libraryDirectory = pluginsDirectory.resolve(LIB_DIR);
        }
    }
    
    /**
     * Load all plugins from the plugins directory.
     * @return true if all plugins loaded successfully
     */
    public boolean loadAllPlugins() {
        logger.info("Loading plugins from: {}", pluginsDirectory);
        
        if (!Files.exists(pluginsDirectory)) {
            logger.info("Plugins directory does not exist, skipping plugin loading");
            return true;
        }
        
        try {
            // First, check for and extract any bundled plugins from resources
            extractBundledPlugins();
            
            // Scan for plugin directories
            List<Path> pluginDirs = Files.list(pluginsDirectory)
                    .filter(Files::isDirectory)
                    .filter(this::isValidPluginDirectory)
                    .collect(Collectors.toList());
            
            logger.info("Found {} potential plugin directories", pluginDirs.size());
            
            boolean allSuccess = true;
            for (Path pluginDir : pluginDirs) {
                try {
                    loadPluginFromDirectory(pluginDir);
                } catch (Exception e) {
                    logger.error("Failed to load plugin from {}: {}", pluginDir, e.getMessage());
                    allSuccess = false;
                }
            }
            
            // Also check for JAR files directly in plugins directory
            List<Path> pluginJars = Files.list(pluginsDirectory)
                    .filter(p -> p.toString().endsWith(".jar"))
                    .filter(Files::isRegularFile)
                    .collect(Collectors.toList());
            
            for (Path jarPath : pluginJars) {
                try {
                    loadPluginJar(jarPath);
                } catch (Exception e) {
                    logger.error("Failed to load plugin JAR {}: {}", jarPath, e.getMessage());
                    allSuccess = false;
                }
            }
            
            return allSuccess;
            
        } catch (IOException e) {
            logger.error("Error scanning plugins directory: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Check if a directory is a valid plugin directory.
     */
    private boolean isValidPluginDirectory(Path dir) {
        // A valid plugin directory should contain either:
        // - A plugin.yaml manifest file
        // - A plugin.jar file
        
        Path manifestPath = dir.resolve(PLUGIN_MANIFEST);
        Path jarPath = dir.resolve("plugin.jar");
        
        return Files.exists(manifestPath) || Files.exists(jarPath);
    }
    
    /**
     * Extract bundled plugins from resources.
     */
    private void extractBundledPlugins() {
        // This would be used if plugins are bundled in the JAR
        // For now, just a placeholder
    }
    
    /**
     * Load a plugin from a directory.
     */
    private void loadPluginFromDirectory(Path pluginDir) throws Exception {
        String pluginName = pluginDir.getFileName().toString();
        logger.info("Loading plugin: {}", pluginName);
        
        // Check for manifest
        Path manifestPath = pluginDir.resolve(PLUGIN_MANIFEST);
        PluginDescriptor descriptor;
        
        if (Files.exists(manifestPath)) {
            descriptor = loadFromManifest(pluginDir, manifestPath);
        } else {
            // Try to load from JAR in directory
            Path jarPath = pluginDir.resolve("plugin.jar");
            if (Files.exists(jarPath)) {
                descriptor = loadFromJar(jarPath, null);
            } else {
                throw new IllegalStateException("No manifest or JAR found in plugin directory");
            }
        }
        
        // Create class loader with plugin and library paths
        List<URL> classpathURLs = buildClassPath(pluginDir);
        ClassLoader parentLoader = PluginLoader.class.getClassLoader();
        if (parentLoader == null) {
            parentLoader = ClassLoader.getSystemClassLoader();
        }
        ClassLoader classLoader = new PluginClassLoader(
                classpathURLs.toArray(new URL[0]),
                parentLoader
        );
        
        // Load and instantiate the plugin
        ShioriPlugin plugin = instantiatePlugin(descriptor, classLoader);
        
        if (plugin != null) {
            pluginManager.registerPlugin(plugin, descriptor);
            logger.info("Successfully loaded plugin: {}", descriptor.getName());
        }
    }
    
    /**
     * Load a plugin from a standalone JAR file.
     */
    private void loadPluginJar(Path jarPath) throws Exception {
        logger.info("Loading plugin JAR: {}", jarPath);
        
        PluginDescriptor descriptor = loadFromJar(jarPath, jarPath.getParent());
        
        // Create class loader
        URL jarURL = jarPath.toUri().toURL();
        List<URL> classpathURLs = List.of(jarURL);
        ClassLoader parentLoader = PluginLoader.class.getClassLoader();
        if (parentLoader == null) {
            parentLoader = ClassLoader.getSystemClassLoader();
        }
        ClassLoader classLoader = new PluginClassLoader(
                classpathURLs.toArray(new URL[0]),
                parentLoader
        );
        
        // Load and instantiate the plugin
        ShioriPlugin plugin = instantiatePlugin(descriptor, classLoader);
        
        if (plugin != null) {
            pluginManager.registerPlugin(plugin, descriptor);
            logger.info("Successfully loaded plugin JAR: {}", descriptor.getName());
        }
    }
    
    /**
     * Build the classpath for a plugin.
     */
    private List<URL> buildClassPath(Path pluginDir) throws Exception {
        List<URL> urls = new ArrayList<>();
        
        // Add the plugin's own JAR if exists
        Path pluginJar = pluginDir.resolve("plugin.jar");
        if (Files.exists(pluginJar)) {
            urls.add(pluginJar.toUri().toURL());
        }
        
        // Add the main library directory
        if (Files.exists(libraryDirectory)) {
            Files.list(libraryDirectory)
                    .filter(p -> p.toString().endsWith(".jar"))
                    .forEach(p -> {
                        try {
                            urls.add(p.toUri().toURL());
                        } catch (Exception e) {
                            logger.warn("Failed to add library to classpath: {}", p);
                        }
                    });
        }
        
        // Add the plugin's own lib directory
        Path pluginLibDir = pluginDir.resolve(LIB_DIR);
        if (Files.exists(pluginLibDir)) {
            Files.list(pluginLibDir)
                    .filter(p -> p.toString().endsWith(".jar"))
                    .forEach(p -> {
                        try {
                            urls.add(p.toUri().toURL());
                        } catch (Exception e) {
                            logger.warn("Failed to add plugin library to classpath: {}", p);
                        }
                    });
        }
        
        return urls;
    }
    
    /**
     * Load plugin descriptor from a YAML manifest.
     */
    private PluginDescriptor loadFromManifest(Path pluginDir, Path manifestPath) throws Exception {
        String manifestContent = Files.readString(manifestPath);
        
        // Simple YAML parser for plugin manifest
        Properties props = new Properties();
        try (StringReader reader = new StringReader(manifestContent)) {
            props.load(reader);
        }
        
        String id = props.getProperty("id");
        String name = props.getProperty("name");
        String version = props.getProperty("version", "1.0.0");
        String author = props.getProperty("author", "Unknown");
        String description = props.getProperty("description", "");
        String capabilityStr = props.getProperty("capability", "GENERAL");
        String mainClass = props.getProperty("main.class");
        String license = props.getProperty("license", "MIT");
        String website = props.getProperty("website", "");
        String dependenciesStr = props.getProperty("dependencies", "");
        
        // Parse capability
        PluginCapability capability;
        try {
            capability = PluginCapability.valueOf(capabilityStr);
        } catch (IllegalArgumentException e) {
            capability = PluginCapability.GENERAL;
        }
        
        // Parse dependencies
        List<String> dependencies = dependenciesStr.isEmpty() 
                ? List.of()
                : Arrays.asList(dependenciesStr.split(","));
        
        // Parse supported API versions
        String apiVersionsStr = props.getProperty("api.versions", "1.0");
        Set<String> apiVersions = new HashSet<>(Arrays.asList(apiVersionsStr.split(",")));
        
        Path jarPath = pluginDir.resolve("plugin.jar");
        URL jarLocation = Files.exists(jarPath) ? jarPath.toUri().toURL() : null;
        
        return new PluginDescriptor.Builder()
                .setId(id)
                .setName(name)
                .setVersion(version)
                .setAuthor(author)
                .setDescription(description)
                .setCapability(capability)
                .setMainClass(mainClass)
                .setJarLocation(jarLocation)
                .setDependencies(dependencies)
                .setSupportedApiVersions(apiVersions)
                .setLicense(license)
                .setWebsite(website)
                .setBuiltIn(false)
                .build();
    }
    
    /**
     * Load plugin descriptor from a JAR file's manifest.
     */
    private PluginDescriptor loadFromJar(Path jarPath, Path baseDir) throws Exception {
        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            JarEntry manifestEntry = jarFile.getJarEntry("META-INF/MANIFEST.MF");
            
            if (manifestEntry == null) {
                throw new IllegalArgumentException("JAR has no MANIFEST.MF");
            }
            
            Properties props = new Properties();
            try (InputStream manifestStream = jarFile.getInputStream(manifestEntry)) {
                props.load(manifestStream);
            }
            
            String id = props.getProperty("Plugin-Id");
            String name = props.getProperty("Plugin-Name");
            String version = props.getProperty("Plugin-Version", "1.0.0");
            String author = props.getProperty("Plugin-Author", "Unknown");
            String description = props.getProperty("Plugin-Description", "");
            String capabilityStr = props.getProperty("Plugin-Capability", "GENERAL");
            String mainClass = props.getProperty("Plugin-Main-Class");
            String license = props.getProperty("Plugin-License", "MIT");
            String website = props.getProperty("Plugin-Website", "");
            String dependenciesStr = props.getProperty("Plugin-Dependencies", "");
            
            if (id == null || name == null || mainClass == null) {
                throw new IllegalArgumentException("JAR manifest missing required plugin fields");
            }
            
            // Parse capability
            PluginCapability capability;
            try {
                capability = PluginCapability.valueOf(capabilityStr);
            } catch (IllegalArgumentException e) {
                capability = PluginCapability.GENERAL;
            }
            
            // Parse dependencies
            List<String> dependencies = dependenciesStr.isEmpty() 
                    ? List.of()
                    : Arrays.asList(dependenciesStr.split(","));
            
            // Parse supported API versions
            String apiVersionsStr = props.getProperty("Plugin-Api-Versions", "1.0");
            Set<String> apiVersions = new HashSet<>(Arrays.asList(apiVersionsStr.split(",")));
            
            return new PluginDescriptor.Builder()
                    .setId(id)
                    .setName(name)
                    .setVersion(version)
                    .setAuthor(author)
                    .setDescription(description)
                    .setCapability(capability)
                    .setMainClass(mainClass)
                    .setJarLocation(jarPath.toUri().toURL())
                    .setDependencies(dependencies)
                    .setSupportedApiVersions(apiVersions)
                    .setLicense(license)
                    .setWebsite(website)
                    .setBuiltIn(false)
                    .build();
        }
    }
    
    /**
     * Instantiate the plugin class.
     */
    private ShioriPlugin instantiatePlugin(PluginDescriptor descriptor, ClassLoader classLoader) {
        try {
            Class<?> pluginClass = classLoader.loadClass(descriptor.getMainClass());
            
            // Check if it implements ShioriPlugin
            if (!ShioriPlugin.class.isAssignableFrom(pluginClass)) {
                throw new IllegalArgumentException(
                        "Plugin class " + descriptor.getMainClass() + 
                        " does not implement ShioriPlugin interface"
                );
            }
            
            // Check if it's not abstract
            if (Modifier.isAbstract(pluginClass.getModifiers())) {
                throw new IllegalArgumentException(
                        "Plugin class " + descriptor.getMainClass() + " is abstract"
                );
            }
            
            // Instantiate the plugin
            @SuppressWarnings("unchecked")
            Class<? extends ShioriPlugin> concreteClass = 
                    (Class<? extends ShioriPlugin>) pluginClass;
            
            ShioriPlugin plugin = concreteClass.getDeclaredConstructor().newInstance();
            
            // Validate plugin ID matches descriptor
            if (!plugin.getId().equals(descriptor.getId())) {
                logger.warn("Plugin ID mismatch: {} vs {}", 
                        plugin.getId(), descriptor.getId());
            }
            
            return plugin;
            
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Plugin class not found: " + descriptor.getMainClass(), e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to instantiate plugin: " + descriptor.getName(), e);
        }
    }
    
    /**
     * Get the plugins directory path.
     */
    public Path getPluginsDirectory() {
        return pluginsDirectory;
    }
    
    /**
     * Get the library directory path.
     */
    public Path getLibraryDirectory() {
        return libraryDirectory;
    }
    
    /**
     * Custom class loader for plugins.
     */
    private static class PluginClassLoader extends URLClassLoader {
        public PluginClassLoader(URL[] urls, ClassLoader parent) {
            super(urls, parent);
        }
        
        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            // Try to load from parent first (for system classes)
            try {
                return getParent().loadClass(name);
            } catch (ClassNotFoundException e) {
                // Then try our own URLs
                return super.loadClass(name);
            }
        }
    }
}

