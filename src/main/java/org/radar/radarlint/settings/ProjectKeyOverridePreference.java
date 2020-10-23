package org.radar.radarlint.settings;

import java.util.prefs.Preferences;

/**
 * Allows setting project key override in Netbeans project preferences.
 *
 * @author Aaron Watry
 */
public class ProjectKeyOverridePreference extends AbstractPreferenceAccessor<String> {

    public ProjectKeyOverridePreference(Preferences preferences) {
        super(preferences, "sonarLint.project.key.override");
    }

    @Override
    public void setValue(String value) {
        getPreferences().put(getPreferenceKey(), value);
    }

    @Override
    public String getValue() {
        return getPreferences().get(getPreferenceKey(), null);
    }

}
