package org.radar.radarlint;

import java.util.prefs.Preferences;

/**
 *
 * @author Víctor
 */
public abstract class AbstractPreferenceAccessor<T> implements PreferenceAccessor<T> {  
    
    private Preferences preferences;
    private String key;

    public AbstractPreferenceAccessor(Preferences preferences, String key) {
        this.preferences = preferences;
        this.key = key;
    }

    public Preferences getPreferences() {
        return preferences;
    }

    public String getPreferenceKey() {
        return key;
    }
    
}
