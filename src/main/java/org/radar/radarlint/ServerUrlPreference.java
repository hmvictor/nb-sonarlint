package org.radar.radarlint;

import org.openide.util.NbPreferences;

/**
 *
 * @author Víctor
 */
public class ServerUrlPreference extends AbstractPreferenceAccessor<String>{
    private static final String DEFAULT_SERVER_URL="http://localhost:9000";

    public ServerUrlPreference() {
        super(NbPreferences.forModule(SonarLintSaveTask.class), "sonarqube.server.url");
    }
    
    @Override
    public void setValue(String value) {
        getPreferences().put(getPreferenceKey(), value);
    }

    @Override
    public String getValue() {
        return getPreferences().get(getPreferenceKey(), DEFAULT_SERVER_URL);
    }
    
}
