package org.radar.radarlint.ui;

import java.awt.event.ActionEvent;
import java.util.prefs.Preferences;
import javax.swing.JComponent;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.spi.project.ui.support.ProjectCustomizer;
import org.openide.util.Lookup;
import org.radar.radarlint.EditorAnnotator;
import org.radar.radarlint.PreferenceAccessor;
import org.radar.radarlint.SonarLintTrigger;
import org.radar.radarlint.ExcludedFilePatterns;
import org.radar.radarlint.SonarLintActivePreference;

/**
 *
 * @author Víctor
 */
public class SonarLintCustomizerTab implements ProjectCustomizer.CompositeCategoryProvider {
    private final String name;

    public SonarLintCustomizerTab(String name) {
        this.name = name;
    }
    
    @Override
    public ProjectCustomizer.Category createCategory(Lookup lookup) {
        return ProjectCustomizer.Category.create(name, name, null);
    }

    @Override
    public JComponent createComponent(ProjectCustomizer.Category category, Lookup lookup) {
        SonarLintPropertiesComponent component = new SonarLintPropertiesComponent();
        Project currentProject = lookup.lookup(Project.class);
        Preferences preferences = ProjectUtils.getPreferences(currentProject, SonarLintPropertiesComponent.class, false);
        
        PreferenceAccessor<Boolean> sonarLintActivePreference=new SonarLintActivePreference(preferences);
        PreferenceAccessor<String> excludedFilePatternsPreference=new ExcludedFilePatterns(preferences);
        
        category.setOkButtonListener((ActionEvent e) -> {
            if(category.isValid()) {
                //save current properties
                sonarLintActivePreference.setValue(component.isSonarLintActive());
                excludedFilePatternsPreference.setValue(component.getExcludedFilePatterns());
                
                EditorAnnotator editorAnnotator = EditorAnnotator.getInstance();
                if(!component.isSonarLintActive()) {
                    editorAnnotator.cleanEditorAnnotations(currentProject);
                }else{
                    editorAnnotator.getFileObjects(currentProject).forEach((fileObject) -> {
                        if(SonarLintTrigger.isExcludedFile(preferences, fileObject)) {
                            editorAnnotator.cleanEditorAnnotations(fileObject);
                        }
                    });
                }
            }
        });
        /* Load current properties*/
        component.setSonarLintActive(sonarLintActivePreference.getValue());
        component.setExcludedFilePatterns(excludedFilePatternsPreference.getValue());
        
        return component;
    }
    
    @ProjectCustomizer.CompositeCategoryProvider.Registrations({
        @ProjectCustomizer.CompositeCategoryProvider.Registration(projectType = "org-netbeans-modules-java-j2seproject"),
        @ProjectCustomizer.CompositeCategoryProvider.Registration(projectType = "org-netbeans-modules-web-project"),
        @ProjectCustomizer.CompositeCategoryProvider.Registration(projectType = "org-netbeans-modules-maven")
    })
    public static SonarLintCustomizerTab createPropertiesComponent() {
        return new SonarLintCustomizerTab("SonarLint");
    }
    
}
