package org.radar.radarlint;

/**
 *
 * @author Víctor
 */
public interface PreferenceAccessor<T> {
    
    void setValue(T value);
    
    T getValue();
        
}
