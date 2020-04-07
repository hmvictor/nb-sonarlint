package org.radar.radarlint;

/**
 *
 * @author Victor
 */
public enum Severity {
    BLOCKER("Blocker", "/qubexplorer/ui/images/blocker.png"),
    CRITICAL("Critical", "/qubexplorer/ui/images/critical.png"),
    MAJOR("Major", "/qubexplorer/ui/images/major.png"),
    MINOR("Minor", "/qubexplorer/ui/images/minor.png"),
    INFO("Info", "/qubexplorer/ui/images/info.png");
    
    private final String userDescription;
    private final String resourcePath;

    private Severity(String userDescription, String resourcePath) {
        this.userDescription = userDescription;
        this.resourcePath = resourcePath;
    }

    public String getUserDescription() {
        return userDescription;
    }

    public String getResourcePath() {
        return resourcePath;
    }
    
}
