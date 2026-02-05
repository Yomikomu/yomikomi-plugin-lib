package plugin;

import java.net.URL;
import java.util.List;
import java.util.Set;

/**
 * Metadata descriptor for a plugin.
 * Contains all information about a plugin that's used for display and management.
 */
public class PluginDescriptor {
    
    private final String id;
    private final String name;
    private final String version;
    private final String author;
    private final String description;
    private final PluginCapability capability;
    private final String mainClass;
    private final URL jarLocation;
    private final List<String> dependencies;
    private final Set<String> supportedApiVersions;
    private final String license;
    private final String website;
    private final boolean builtIn;
    
    public PluginDescriptor(
            String id,
            String name,
            String version,
            String author,
            String description,
            PluginCapability capability,
            String mainClass,
            URL jarLocation,
            List<String> dependencies,
            Set<String> supportedApiVersions,
            String license,
            String website,
            boolean builtIn) {
        this.id = id;
        this.name = name;
        this.version = version;
        this.author = author;
        this.description = description;
        this.capability = capability;
        this.mainClass = mainClass;
        this.jarLocation = jarLocation;
        this.dependencies = dependencies;
        this.supportedApiVersions = supportedApiVersions;
        this.license = license;
        this.website = website;
        this.builtIn = builtIn;
    }
    
    /**
     * Get the unique identifier for this plugin.
     */
    public String getId() {
        return id;
    }
    
    /**
     * Get the display name of this plugin.
     */
    public String getName() {
        return name;
    }
    
    /**
     * Get the version of this plugin.
     */
    public String getVersion() {
        return version;
    }
    
    /**
     * Get the author of this plugin.
     */
    public String getAuthor() {
        return author;
    }
    
    /**
     * Get a description of what this plugin does.
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Get the capability this plugin provides.
     */
    public PluginCapability getCapability() {
        return capability;
    }
    
    /**
     * Get the main class name of this plugin.
     */
    public String getMainClass() {
        return mainClass;
    }
    
    /**
     * Get the URL location of the plugin JAR.
     */
    public URL getJarLocation() {
        return jarLocation;
    }
    
    /**
     * Get the list of dependencies required by this plugin.
     */
    public List<String> getDependencies() {
        return dependencies;
    }
    
    /**
     * Get the supported API versions for this plugin.
     */
    public Set<String> getSupportedApiVersions() {
        return supportedApiVersions;
    }
    
    /**
     * Get the license of this plugin.
     */
    public String getLicense() {
        return license;
    }
    
    /**
     * Get the website URL for this plugin.
     */
    public String getWebsite() {
        return website;
    }
    
    /**
     * Check if this is a built-in plugin.
     */
    public boolean isBuiltIn() {
        return builtIn;
    }
    
    /**
     * Get a formatted string representation of this descriptor.
     */
    public String toDisplayString() {
        return String.format("%s v%s by %s", name, version, author);
    }
    
    /**
     * Builder class for creating PluginDescriptor instances.
     */
    public static class Builder {
        private String id;
        private String name;
        private String version = "1.0.0";
        private String author = "Unknown";
        private String description = "";
        private PluginCapability capability = PluginCapability.GENERAL;
        private String mainClass;
        private URL jarLocation;
        private List<String> dependencies = List.of();
        private Set<String> supportedApiVersions = Set.of("1.0");
        private String license = "MIT";
        private String website = "";
        private boolean builtIn = false;
        
        public Builder setId(String id) {
            this.id = id;
            return this;
        }
        
        public Builder setName(String name) {
            this.name = name;
            return this;
        }
        
        public Builder setVersion(String version) {
            this.version = version;
            return this;
        }
        
        public Builder setAuthor(String author) {
            this.author = author;
            return this;
        }
        
        public Builder setDescription(String description) {
            this.description = description;
            return this;
        }
        
        public Builder setCapability(PluginCapability capability) {
            this.capability = capability;
            return this;
        }
        
        public Builder setMainClass(String mainClass) {
            this.mainClass = mainClass;
            return this;
        }
        
        public Builder setJarLocation(URL jarLocation) {
            this.jarLocation = jarLocation;
            return this;
        }
        
        public Builder setDependencies(List<String> dependencies) {
            this.dependencies = dependencies;
            return this;
        }
        
        public Builder setSupportedApiVersions(Set<String> versions) {
            this.supportedApiVersions = versions;
            return this;
        }
        
        public Builder setLicense(String license) {
            this.license = license;
            return this;
        }
        
        public Builder setWebsite(String website) {
            this.website = website;
            return this;
        }
        
        public Builder setBuiltIn(boolean builtIn) {
            this.builtIn = builtIn;
            return this;
        }
        
        public PluginDescriptor build() {
            if (id == null || name == null || mainClass == null) {
                throw new IllegalStateException("PluginDescriptor requires id, name, and mainClass");
            }
            return new PluginDescriptor(
                    id, name, version, author, description, capability,
                    mainClass, jarLocation, dependencies, supportedApiVersions,
                    license, website, builtIn
            );
        }
    }
}

