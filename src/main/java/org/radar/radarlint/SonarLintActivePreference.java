package org.radar.radarlint;

import java.util.prefs.Preferences;

/**
 *
 * @author Víctor
 */
public class SonarLintActivePreference extends AbstractPreferenceAccessor<Boolean> {
    
    public SonarLintActivePreference(Preferences preferences) {
        super(preferences, "sonarLint.active");
    }

    @Override
    public void setValue(Boolean value) {
        getPreferences().putBoolean(getPreferenceKey(), true);
    }

    @Override
    public Boolean getValue() {
        return getPreferences().getBoolean(getPreferenceKey(), false);
    }
    
}
