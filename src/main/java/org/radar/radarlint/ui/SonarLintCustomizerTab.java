package org.radar.radarlint.ui;

import javax.swing.JComponent;
import org.netbeans.spi.project.ui.support.ProjectCustomizer;
import org.openide.util.Lookup;

/**
 *
 * @author Víctor
 */
public class SonarLintCustomizerTab implements ProjectCustomizer.CompositeCategoryProvider {
    private String name;

    public SonarLintCustomizerTab(String name) {
        this.name = name;
    }
    
    @Override
    public ProjectCustomizer.Category createCategory(Lookup lkp) {
        return ProjectCustomizer.Category.create(name, name, null);
    }

    @Override
    public JComponent createComponent(ProjectCustomizer.Category ctgr, Lookup lkp) {
        return new SonarLintPropertiesComponent();
    }
    
    @ProjectCustomizer.CompositeCategoryProvider.Registrations({
        @ProjectCustomizer.CompositeCategoryProvider.Registration(projectType = "org-netbeans-modules-java-j2seproject"),
        @ProjectCustomizer.CompositeCategoryProvider.Registration(projectType = "org-netbeans-modules-web-project"),
        @ProjectCustomizer.CompositeCategoryProvider.Registration(projectType = "org-netbeans-modules-maven")
    })
    public static SonarLintCustomizerTab createMyDemoConfigurationTab() {
        return new SonarLintCustomizerTab("SonarLint");
    }
    
}
