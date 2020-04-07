package org.radar.radarlint;

/**
 *
 * @author Victor
 */
public enum Severity /* implements Classifier */{
    
    BLOCKER("Blocker", "/qubexplorer/ui/images/blocker.png"),
    CRITICAL("Critical", "/qubexplorer/ui/images/critical.png"),
    MAJOR("Major", "/qubexplorer/ui/images/major.png"),
    MINOR("Minor", "/qubexplorer/ui/images/minor.png"),
    INFO("Info", "/qubexplorer/ui/images/info.png");
    
    private final String userDescription;
    private final String resourcePath;
    private long a=5;

    private Severity(String userDescription, String resourcePath) {
        this.userDescription = userDescription;
        this.resourcePath = resourcePath;
    }
    
    public boolean  method()  {
        return true;
    }
    
//    @Override
//    public IssueFilter createFilter() {
//        return new SeverityFilter(this); 
//    }
    
    
//
//    @Override
//    public Icon getIcon() {
//        return new ImageIcon(getClass().getResource(resourcePath));
//    }
//
//    @Override
//    public String getUserDescription() {
//        return userDescription;
//    }
//    
//    public static ClassifierType<Severity> getType() {
//        return TYPE;
//    }
//    
//    private static final SeverityType TYPE=new SeverityType();
//
//    private static class SeverityType implements ClassifierType {
//
//        @Override
//        public Severity valueOf(RadarIssue issue) {
//            return Severity.valueOf(issue.severity().toUpperCase());
//        }
//
//        @Override
//        public List<Severity> getValues() {
//            return Arrays.asList(Severity.values());
//        }
//        
//    }
    
}
