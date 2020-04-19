package org.radar.radarlint;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.modules.parsing.api.Source;
import org.netbeans.spi.editor.document.OnSaveTask;
import org.openide.filesystems.FileObject;
import org.openide.util.NbPreferences;
import org.sonarsource.sonarlint.core.ConnectedSonarLintEngineImpl;
import org.sonarsource.sonarlint.core.client.api.common.LogOutput;
import org.sonarsource.sonarlint.core.client.api.common.ProgressMonitor;
import org.sonarsource.sonarlint.core.client.api.common.analysis.ClientInputFile;
import org.sonarsource.sonarlint.core.client.api.common.analysis.Issue;
import org.sonarsource.sonarlint.core.client.api.connected.ConnectedAnalysisConfiguration;
import org.sonarsource.sonarlint.core.client.api.connected.ConnectedGlobalConfiguration;
import org.sonarsource.sonarlint.core.client.api.connected.ConnectedSonarLintEngine;
import org.sonarsource.sonarlint.core.client.api.connected.Language;
import org.sonarsource.sonarlint.core.client.api.connected.ServerConfiguration;
import org.apache.maven.model.Model;

/**
 *
 * @author Víctor
 */
public class SonarLintTrigger implements OnSaveTask, Supplier<List<Issue>> {
    private static final Logger LOGGER = Logger.getLogger(SonarLintTrigger.class.getName());
    
    public static final String SERVER_URL_PROPERTY="sonarqube.server.url";
    public static final String DEFAULT_SERVER_URL="http://localhost:9000";
    private static final String USER_AGENT = "RadarLint";
    
    private Document document;
    private FileObject fileObject;
    private Language[] enabledLanguages={Language.JAVA};
    private ProgressHandle handle;

    public SonarLintTrigger(Document document) {
        this.document=document;
        Source source = Source.create(document);
        fileObject = source.getFileObject();
    }
    
    @Override
    public void performTask() {
        LOGGER.log(Level.INFO, "Perform saveTaskImpl {0}", fileObject == null ? fileObject:fileObject.getNameExt());
        LOGGER.log(Level.INFO, "Thread name: {0}", Thread.currentThread().getName());
        LOGGER.log(Level.INFO, "perform on file object path: {0}", fileObject.getPath());
        new EditorAnnotator().cleanEditorAnnotations(fileObject);
        handle=ProgressHandle.createHandle("SonarLint");
        CompletableFuture.supplyAsync(this)
            .exceptionallyAsync((Throwable t) -> {
                LOGGER.log(Level.WARNING, t.toString(), t);
                return null;
            }).whenComplete((List<Issue> t, Throwable u) -> {
                SwingUtilities.invokeLater(() -> {
                    handle.finish();
                });
            });
    }
    
    @Override
    public List<Issue> get() {
        SwingUtilities.invokeLater(() -> {
            handle.start();
            handle.switchToIndeterminate();
        });
        try{
            List<Issue> issues=new LinkedList<>();

            ConnectedGlobalConfiguration globalConfig=ConnectedGlobalConfiguration.builder()
                    .setServerId("123")
    //                .setWorkDir(StoragePathManager.getServerWorkDir(getId()))
    //                .setStorageRoot(StoragePathManager.getServerStorageRoot())
                    .setLogOutput((String string, LogOutput.Level level) -> {})
                    .addEnabledLanguages(enabledLanguages)
                    .build();
            LinkedList<ClientInputFile> inputFiles = new LinkedList<>();

            ConnectedAnalysisConfiguration.Builder builder = ConnectedAnalysisConfiguration.builder();

            AtomicReference<String> projectKey=new AtomicReference();

            if(isSupportedLanguage(fileObject)) {
                try {
                LOGGER.log(Level.INFO, "File object to analyze: {0}", fileObject.getNameExt());
                    ClientInputFile clientInputFile=new ContentClientInputFile(fileObject, document.getText(0, document.getLength()), false);
                    inputFiles.add(clientInputFile);
                    Project pro = FileOwnerQuery.getOwner(fileObject);
                    projectKey.set(getProjectKey(pro));
                    builder.setBaseDir(Paths.get(pro.getProjectDirectory().getPath()));
                } catch (BadLocationException ex) {
                    throw new RuntimeException(ex);
                }
            }

            if(!inputFiles.isEmpty()) {
                Map<String, String> extraProperties=new HashMap<>();

                ConnectedAnalysisConfiguration analysisConfig=builder
                    .setProjectKey(projectKey.get())
                    .addInputFiles(inputFiles)
                    .putAllExtraProperties(extraProperties)
                    .build();

                ServerConfiguration serverConfiguration=ServerConfiguration.builder()
                    .url(NbPreferences.forModule(SonarLintTrigger.class).get(SERVER_URL_PROPERTY, DEFAULT_SERVER_URL))
                    .userAgent(USER_AGENT)
                    //.credentials(login, password)
                    .build();
                ConnectedSonarLintEngine engine = new ConnectedSonarLintEngineImpl(globalConfig);
                engine.update(serverConfiguration, new ProgressMonitor() { });

                engine.updateProject(serverConfiguration, projectKey.get(), new ProgressMonitor() { });
                engine.analyze(analysisConfig, (Issue issue) -> {
                    LOGGER.log(Level.INFO, "Issue: {0}, {1}, {2}, {3}, {4}, {5}, {6}, {7}, {8}", new Object[]{issue.getInputFile().relativePath(), issue.getStartLine(), issue.getStartLineOffset(), issue.getEndLine(), issue.getEndLineOffset(), issue.getSeverity(), issue.getRuleName(), issue.getType(), issue.getMessage()});
                    FileObject fileObject1 = issue.getInputFile().getClientObject();
                    boolean attached = new EditorAnnotator().tryToAttachAnnotation(issue, fileObject1);
                    issues.add(issue);
                }, (String string, LogOutput.Level level) -> {
                }, new ProgressMonitor() {});
            }
            return issues;
        }finally{
            SwingUtilities.invokeLater(() -> {
                handle.finish();
            });
        }
    }

    private boolean isSupportedLanguage(FileObject fileObject) {
        boolean isEnabledLanguage=false;
        for (Language enabledLanguage : enabledLanguages) {
            if(fileObject.getExt().equals(enabledLanguage.getLanguageKey())) {
                isEnabledLanguage=true;
                break;
            }
        }
        return isEnabledLanguage;
    }

    public String getProjectKey(Project project) {
        MvnProjectAnalyzer mvnProjectAnalyzer = new MvnProjectAnalyzer();
        Model model=mvnProjectAnalyzer.createModel(project);
        if(model != null) {
            return mvnProjectAnalyzer.getProjectKey(model);
        }else{
            return ProjectUtils.getInformation(project).getName();
        }
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
