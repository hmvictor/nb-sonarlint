package org.radar.radarlint;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;
import javax.swing.SwingUtilities;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.spi.editor.document.OnSaveTask;
import org.openide.filesystems.FileObject;
import org.sonarsource.sonarlint.core.client.api.common.analysis.Issue;
import org.radar.radarlint.ui.SonarLintPropertiesComponent;

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
