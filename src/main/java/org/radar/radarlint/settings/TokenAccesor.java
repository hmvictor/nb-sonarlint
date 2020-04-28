package org.radar.radarlint.settings;

import org.netbeans.api.keyring.Keyring;

/**
 *
 * @author Víctor
 */
public class TokenAccesor implements SettingsAccessor<char[]>{

    @Override
    public void setValue(char[] value) {
        if(value == null || value.length == 0) {
            Keyring.delete("sonarLint.server.token");
        } else {
            Keyring.save("sonarLint.server.token", value, "SonarLint server token");
        }
    }

    @Override
    public char[] getValue() {
        return Keyring.read("sonarLint.server.token");
    }
    
}
