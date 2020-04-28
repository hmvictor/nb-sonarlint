package org.radar.radarlint.settings;

/**
 *
 * @author Víctor
 */
public interface SettingsAccessor<T> {
    
    void setValue(T value);
    
    T getValue();
        
}
