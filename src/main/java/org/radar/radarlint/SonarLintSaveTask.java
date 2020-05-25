package org.radar.radarlint;

import org.netbeans.spi.editor.document.OnSaveTask;

/**
 *
 * @author Víctor
 */
public class SonarLintSaveTask implements OnSaveTask {
    private final SonarLintScanner sonarLintScanner;

    public SonarLintSaveTask(SonarLintScanner sonarLintScanner) {
        this.sonarLintScanner = sonarLintScanner;
    }
    
    @Override
    public void performTask() {
        sonarLintScanner.runAsync();
    }

    @Override
    public void runLocked(Runnable r) {
        r.run();
    }

    @Override
    public boolean cancel() {
        return true;
    }
    
}
