package plugin;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.*;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

/**
 * Manages the shared library folder that plugins can use.
 * Libraries in this folder are available to all plugins.
 */
public class LibraryManager {
    
    private static final Logger logger = LogManager.getLogger(LibraryManager.class);
    
    private static final String LIB_DIR = "lib";
    private static final String PLUGIN_LIB_DIR = "lib";
    
    private final Path pluginsRoot;
    private final Path sharedLibDir;
    private final List<URL> sharedLibraryURLs = new ArrayList<>();
    private ClassLoader sharedLibraryClassLoader;
    
    public LibraryManager(Path pluginsRoot) {
        this.pluginsRoot = pluginsRoot;
        this.sharedLibDir = pluginsRoot.resolve(LIB_DIR);
        initialize();
    }
    
    /**
     * Initialize the library manager and load shared libraries.
     */
    private void initialize() {
        try {
            // Create the shared library directory if it doesn't exist
            if (!Files.exists(sharedLibDir)) {
                Files.createDirectories(sharedLibDir);
                logger.info("Created shared library directory: {}", sharedLibDir);
            }
            
            // Scan for JAR files in the shared library directory
            loadSharedLibraries();
            
        } catch (IOException e) {
            logger.error("Failed to initialize library manager: {}", e.getMessage());
        }
    }
    
    /**
     * Load all JAR files from the shared library directory.
     */
    private void loadSharedLibraries() {
        try {
            List<Path> jarFiles = Files.list(sharedLibDir)
                    .filter(p -> p.toString().endsWith(".jar"))
                    .filter(Files::isRegularFile)
                    .collect(Collectors.toList());
            
            for (Path jarPath : jarFiles) {
                try {
                    URL jarURL = jarPath.toUri().toURL();
                    sharedLibraryURLs.add(jarURL);
                    logger.debug("Added shared library: {}", jarPath.getFileName());
                } catch (Exception e) {
                    logger.warn("Failed to load shared library {}: {}", jarPath, e.getMessage());
                }
            }
            
            if (!sharedLibraryURLs.isEmpty()) {
                logger.info("Loaded {} shared libraries", sharedLibraryURLs.size());
                updateClassLoader();
            }
            
        } catch (IOException e) {
            logger.error("Error scanning shared library directory: {}", e.getMessage());
        }
    }
    
    /**
     * Update the shared library class loader.
     */
    private void updateClassLoader() {
        if (!sharedLibraryURLs.isEmpty()) {
            sharedLibraryClassLoader = new URLClassLoader(
                    sharedLibraryURLs.toArray(new URL[0]),
                    ClassLoader.getSystemClassLoader()
            );
        }
    }
    
    /**
     * Get the shared library directory path.
     */
    public Path getSharedLibraryDirectory() {
        return sharedLibDir;
    }
    
    /**
     * Get the URL of a library by name.
     */
    public Optional<URL> getLibraryURL(String libraryName) {
        return sharedLibraryURLs.stream()
                .filter(url -> url.toString().contains(libraryName))
                .findFirst();
    }
    
    /**
     * Get a class loader that includes all shared libraries.
     */
    public ClassLoader getSharedLibraryClassLoader() {
        if (sharedLibraryClassLoader == null) {
            sharedLibraryClassLoader = ClassLoader.getSystemClassLoader();
        }
        return sharedLibraryClassLoader;
    }
    
    /**
     * Load a class from the shared libraries.
     */
    public Optional<Class<?>> loadClass(String className) {
        try {
            Class<?> clazz = getSharedLibraryClassLoader().loadClass(className);
            return Optional.of(clazz);
        } catch (ClassNotFoundException e) {
            return Optional.empty();
        }
    }
    
    /**
     * Add a new library to the shared folder.
     * @param sourcePath Path to the source JAR file
     * @return true if library was added successfully
     */
    public boolean addLibrary(Path sourcePath) {
        try {
            Path destPath = sharedLibDir.resolve(sourcePath.getFileName());
            Files.copy(sourcePath, destPath, StandardCopyOption.REPLACE_EXISTING);
            
            // Reload libraries
            URL jarURL = destPath.toUri().toURL();
            sharedLibraryURLs.add(jarURL);
            updateClassLoader();
            
            logger.info("Added library: {}", destPath.getFileName());
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to add library {}: {}", sourcePath, e.getMessage());
            return false;
        }
    }
    
    /**
     * Remove a library from the shared folder.
     * @param libraryName Name of the library to remove
     * @return true if library was removed successfully
     */
    public boolean removeLibrary(String libraryName) {
        Optional<Path> libraryPath = findLibraryPath(libraryName);
        if (libraryPath.isEmpty()) {
            logger.warn("Library not found: {}", libraryName);
            return false;
        }
        
        try {
            Files.delete(libraryPath.get());
            
            // Rebuild library URLs
            loadSharedLibraries();
            
            logger.info("Removed library: {}", libraryName);
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to remove library {}: {}", libraryName, e.getMessage());
            return false;
        }
    }
    
    /**
     * Find the path of a library by name.
     */
    private Optional<Path> findLibraryPath(String libraryName) {
        return sharedLibraryURLs.stream()
                .map(url -> Paths.get(url.getPath()))
                .filter(path -> path.getFileName().toString().contains(libraryName))
                .findFirst();
    }
    
    /**
     * Get list of all shared libraries.
     */
    public List<String> listLibraries() {
        return sharedLibraryURLs.stream()
                .map(url -> Paths.get(url.getPath()).getFileName().toString())
                .collect(Collectors.toList());
    }
    
    /**
     * Get list of all libraries in the shared folder (including not loaded).
     */
    public List<String> listAvailableLibraries() {
        try {
            return Files.list(sharedLibDir)
                    .filter(p -> p.toString().endsWith(".jar"))
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            logger.error("Error listing libraries: {}", e.getMessage());
            return List.of();
        }
    }
    
    /**
     * Extract a library from resources to the shared folder.
     */
    public boolean extractResourceLibrary(String resourcePath, String fileName) {
        try {
            Path destPath = sharedLibDir.resolve(fileName);
            
            // Check if already exists
            if (Files.exists(destPath)) {
                logger.debug("Library {} already exists, skipping extraction", fileName);
                return true;
            }
            
            InputStream resourceStream = getClass().getResourceAsStream(resourcePath);
            if (resourceStream == null) {
                logger.warn("Resource not found: {}", resourcePath);
                return false;
            }
            
            Files.copy(resourceStream, destPath, StandardCopyOption.REPLACE_EXISTING);
            
            // Reload libraries
            loadSharedLibraries();
            
            logger.info("Extracted library: {}", fileName);
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to extract library {}: {}", resourcePath, e.getMessage());
            return false;
        }
    }
    
    /**
     * Get information about all available libraries.
     */
    public String getLibraryInfo() {
        List<String> libs = listAvailableLibraries();
        if (libs.isEmpty()) {
            return "No shared libraries available.\n" +
                   "Add JAR files to: " + sharedLibDir.toAbsolutePath();
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("Shared Libraries (").append(libs.size()).append("):\n");
        sb.append("Location: ").append(sharedLibDir.toAbsolutePath()).append("\n\n");
        
        for (String lib : libs) {
            sb.append("  - ").append(lib).append("\n");
        }
        
        return sb.toString();
    }
}

