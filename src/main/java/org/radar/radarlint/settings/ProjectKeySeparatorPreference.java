package org.radar.radarlint.settings;

import org.openide.util.NbPreferences;
import org.radar.radarlint.SonarLintSaveTask;

/**
 * Separator to use when building a default project key.
 *
 * Default is ':' so that project keys end up as groupId:artifactId, but can be modified as needed.
 *
 * @author Aaron Watry
 */
public class ProjectKeySeparatorPreference extends AbstractPreferenceAccessor<String>{
    private static final String DEFAULT_SEPARATOR = ":";

    public ProjectKeySeparatorPreference() {
        super(NbPreferences.forModule(SonarLintSaveTask.class), "sonarqube.project.key.separator");
    }
    
    @Override
    public void setValue(String value) {
        getPreferences().put(getPreferenceKey(), value);
    }

    @Override
    public String getValue() {
        return getPreferences().get(getPreferenceKey(), DEFAULT_SEPARATOR);
    }
    
}
