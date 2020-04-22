package org.radar.radarlint;

import java.util.prefs.Preferences;

/**
 *
 * @author Víctor
 */
public class ExcludedFilePatterns extends AbstractPreferenceAccessor<String> {
    
    public ExcludedFilePatterns(Preferences preferences) {
        super(preferences, "sonarLint.excludedFilePatterns");
    }

    @Override
    public void setValue(String value) {
        getPreferences().put(getPreferenceKey(), value);
    }

    @Override
    public String getValue() {
        return getPreferences().get(getPreferenceKey(), "");
    }
    
}
