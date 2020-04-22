package org.radar.radarlint;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.sonarsource.sonarlint.core.ConnectedSonarLintEngineImpl;
import org.sonarsource.sonarlint.core.client.api.common.LogOutput;
import org.sonarsource.sonarlint.core.client.api.connected.ConnectedGlobalConfiguration;
import org.sonarsource.sonarlint.core.client.api.connected.ConnectedSonarLintEngine;
import org.sonarsource.sonarlint.core.client.api.connected.Language;

/**
 *
 * @author Víctor
 */
public class SonarLintEngineFactory {
    private static final Logger LOGGER = Logger.getLogger(SonarLintEngineFactory.class.getName());
    
    private static ConnectedSonarLintEngine ENGINE;
    
    public static synchronized ConnectedSonarLintEngine getOrCreateEngine(Language... enabledLanguages) {
        if(ENGINE == null) {
            long startTime=System.currentTimeMillis();
            ConnectedGlobalConfiguration globalConfig=ConnectedGlobalConfiguration.builder()
                .setServerId("123")
    //            .setWorkDir(StoragePathManager.getServerWorkDir(getId()))
    //            .setStorageRoot(StoragePathManager.getServerStorageRoot())
                .setLogOutput((String string, LogOutput.Level level) -> {
                    LOGGER.log(Level.INFO, "{0} Global config {1}", new Object[]{(System.currentTimeMillis()-startTime)/1000f, string});
                })
                .addEnabledLanguages(enabledLanguages)
                .build();
            ENGINE = new ConnectedSonarLintEngineImpl(globalConfig);
        }
        return ENGINE;
    }
    
}
