package org.radar.radarlint;

import org.radar.radarlint.settings.SonarLintActivePreference;
import org.radar.radarlint.settings.ServerUrlPreference;
import org.radar.radarlint.settings.ExcludedFilePatterns;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import org.apache.maven.model.Model;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.modules.parsing.api.Source;
import org.openide.filesystems.FileObject;
import org.radar.radarlint.ui.SonarLintPropertiesComponent;
import org.sonarsource.sonarlint.core.client.api.common.LogOutput;
import org.sonarsource.sonarlint.core.client.api.common.ProgressMonitor;
import org.sonarsource.sonarlint.core.client.api.common.analysis.ClientInputFile;
import org.sonarsource.sonarlint.core.client.api.common.analysis.Issue;
import org.sonarsource.sonarlint.core.client.api.connected.ConnectedAnalysisConfiguration;
import org.sonarsource.sonarlint.core.client.api.connected.ConnectedSonarLintEngine;
import org.sonarsource.sonarlint.core.client.api.connected.Language;
import org.sonarsource.sonarlint.core.client.api.connected.ServerConfiguration;
import org.radar.radarlint.settings.SettingsAccessor;
import org.radar.radarlint.settings.TokenAccesor;

/**
 *
 * @author Víctor
 */
public class SonarLintScanner implements Supplier<List<Issue>>  {
    private static final Logger LOGGER = Logger.getLogger(SonarLintSaveTask.class.getName());
    
    private static final String USER_AGENT = "SonarLint Netbeans";
    
    private final Language[] enabledLanguages=Language.values();
    private final FileObject fileObject;
    private final String text;
    
    private SonarLintScanner(FileObject fileObject, String content) {
        this.fileObject = fileObject;
        this.text=content;
    }
    
    public FileObject getFileObject() {
        return fileObject;
    }
    
    public void runAsync() {
        LOGGER.log(Level.INFO, "perform on file object path: {0}", fileObject.getPath());
        
        EditorAnnotator.getInstance().cleanEditorAnnotations(fileObject);
        CompletableFuture.supplyAsync(this)
            .exceptionally((Throwable t) -> {
                LOGGER.log(Level.WARNING, t.toString(), t);
                return null;
            });
    }
    
    @Override
    public List<Issue> get() {
        ProgressHandle handle = ProgressHandle.createHandle("SonarLint");
        SwingUtilities.invokeLater(() -> {
            handle.start();
            handle.switchToIndeterminate();
        });
        try{
            List<Issue> issues=new LinkedList<>();
            long startTime=System.currentTimeMillis();
            LinkedList<ClientInputFile> inputFiles = new LinkedList<>();

            ConnectedAnalysisConfiguration.Builder builder = ConnectedAnalysisConfiguration.builder();

            AtomicReference<String> projectKey=new AtomicReference();

            if(isSupportedLanguage(fileObject)) {
                LOGGER.log(Level.INFO, "{0} File object to analyze: {1}", new Object[]{(System.currentTimeMillis()-startTime)/1000f, fileObject.getNameExt()});
                ClientInputFile clientInputFile=new ContentClientInputFile(fileObject, text, false);
                inputFiles.add(clientInputFile);
                Project pro = FileOwnerQuery.getOwner(fileObject);
                projectKey.set(getProjectKey(pro));
                builder.setBaseDir(Paths.get(pro.getProjectDirectory().getPath()));
            }

            if(!inputFiles.isEmpty()) {
                Map<String, String> extraProperties=new HashMap<>();

                ConnectedAnalysisConfiguration analysisConfig=builder
                    .setProjectKey(projectKey.get())
                    .addInputFiles(inputFiles)
                    .putAllExtraProperties(extraProperties)
                    .build();
                ServerConfiguration.Builder serverConfigurationBuilder = ServerConfiguration.builder();
                serverConfigurationBuilder
                    .url(new ServerUrlPreference().getValue())
                    .userAgent(USER_AGENT);
                
                char[] token = new TokenAccesor().getValue();
                if(token != null) {
                    serverConfigurationBuilder.token(new String(token));
                    Arrays.fill(token, (char)0);
                }
                
                ServerConfiguration serverConfiguration=serverConfigurationBuilder.build();
                ConnectedSonarLintEngine engine = SonarLintEngineFactory.getOrCreateEngine(enabledLanguages);
                if(engine.checkIfGlobalStorageNeedUpdate(serverConfiguration, new ProgressMonitor() {}).needUpdate()) {
                    LOGGER.log(Level.INFO, "{0} Updating global", new Object[]{(System.currentTimeMillis()-startTime)/1000f});
                    engine.update(serverConfiguration, new ProgressMonitor() { });
                }
                
                if(engine.checkIfProjectStorageNeedUpdate(serverConfiguration, projectKey.get(), new ProgressMonitor() { }).needUpdate()) {
                    LOGGER.log(Level.INFO, "{0} Updating project", new Object[]{(System.currentTimeMillis()-startTime)/1000f});
                    engine.updateProject(serverConfiguration, projectKey.get(), new ProgressMonitor() { });
                }
                LOGGER.log(Level.INFO, "{0} Start analisys", new Object[]{(System.currentTimeMillis()-startTime)/1000f});
                engine.analyze(analysisConfig, (Issue issue) -> {
                    LOGGER.log(Level.INFO, "Issue: {0}, {1}, {2}, {3}, {4}, {5}, {6}, {7}, {8}", new Object[]{issue.getInputFile().relativePath(), issue.getStartLine(), issue.getStartLineOffset(), issue.getEndLine(), issue.getEndLineOffset(), issue.getSeverity(), issue.getRuleName(), issue.getType(), issue.getMessage()});
                    FileObject fileObject1 = issue.getInputFile().getClientObject();
                    boolean attached = EditorAnnotator.getInstance().tryToAttachAnnotation(issue, fileObject1);
                    issues.add(issue);
                }, (String string, LogOutput.Level level) -> {
                    LOGGER.log(Level.INFO, "{0} {1}", new Object[]{(System.currentTimeMillis()-startTime)/1000f, string});
                }, new ProgressMonitor() {
                    
                    @Override
                    public void setFraction(float fraction) {
                        LOGGER.log(Level.INFO, "{0} fraction: {1}", new Object[]{(System.currentTimeMillis()-startTime)/1000f, fraction});
                    }

                    @Override
                    public void setMessage(String msg) {
                        LOGGER.log(Level.INFO, "{0} message: {1}", new Object[]{(System.currentTimeMillis()-startTime)/1000f, msg});
                    }
                    
                });
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
    
    private static boolean isScanningNeeded(FileObject fileObject) {
        Project ownerProject = FileOwnerQuery.getOwner(fileObject);
        Preferences preferences = ProjectUtils.getPreferences(ownerProject, SonarLintPropertiesComponent.class, false);
        SettingsAccessor<Boolean> sonarLintActivePreference=new SonarLintActivePreference(preferences);
        return sonarLintActivePreference.getValue() && !isExcludedFile(preferences, fileObject);
    }
    
    public static boolean isExcludedFile(Preferences preferences, FileObject fileObject) {
        SettingsAccessor<String> excludedFilePatternsPreference=new ExcludedFilePatterns(preferences);
        String patterns[] = excludedFilePatternsPreference.getValue().split("\\s*,\\s*");
        for (String pattern : patterns) {
            if (Pattern.compile(pattern).matcher(fileObject.getNameExt()).matches()) {
                return true;
            }
        }
        return false;
    }
    
    public static Optional<SonarLintScanner> of(Document document) throws BadLocationException {
        Source source = Source.create(document);
        FileObject fileObject = source.getFileObject();
        if(isScanningNeeded(fileObject)) {
            return Optional.of(new SonarLintScanner(fileObject, document.getText(0, document.getLength())));
        }else{
            return Optional.empty();
        }
    }
    
    public static Optional<SonarLintScanner> of(FileObject fileObject) throws IOException {
        if(isScanningNeeded(fileObject)) {
            return Optional.of(new SonarLintScanner(fileObject, fileObject.asText()));
        }else{
            return Optional.empty();
        }
    }
    
}
