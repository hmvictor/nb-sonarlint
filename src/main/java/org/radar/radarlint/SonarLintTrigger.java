package org.radar.radarlint;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
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
import org.openide.cookies.EditorCookie;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.text.Annotation;
import org.openide.text.Line;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.NbPreferences;
import org.radar.radarlint.ui.SonarQubeOptionsPanel;
import org.sonarsource.sonarlint.core.ConnectedSonarLintEngineImpl;
import org.sonarsource.sonarlint.core.client.api.common.LogOutput;
import org.sonarsource.sonarlint.core.client.api.common.ProgressMonitor;
import org.sonarsource.sonarlint.core.client.api.common.analysis.ClientInputFile;
import org.sonarsource.sonarlint.core.client.api.common.analysis.Issue;
import org.sonarsource.sonarlint.core.client.api.common.analysis.IssueListener;
import org.sonarsource.sonarlint.core.client.api.connected.ConnectedAnalysisConfiguration;
import org.sonarsource.sonarlint.core.client.api.connected.ConnectedGlobalConfiguration;
import org.sonarsource.sonarlint.core.client.api.connected.ConnectedSonarLintEngine;
import org.sonarsource.sonarlint.core.client.api.connected.Language;
import org.sonarsource.sonarlint.core.client.api.connected.ServerConfiguration;

/**
 *
 * @author Víctor
 */
public class SonarLintTrigger implements OnSaveTask{
    private static final Logger LOGGER = Logger.getLogger(SonarLintTrigger.class.getName());
    
    public static final String SERVER_URL_PROPERTY="sonarqube.server.url";
    public static final String DEFAULT_SERVER_URL="http://localhost:9000";
    
    private Document document;
    private FileObject fileObject;
    private static Map<String, List<Annotation>> editorAnnotations=new HashMap<>();

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
        LOGGER.log(Level.INFO, "Number of editor annotations: {0}", editorAnnotations.getOrDefault(fileObject, new LinkedList<>()).size());
        try {
            LOGGER.log(Level.INFO, "Document content: {0}", document.getText(0, document.getLength()));
        } catch (BadLocationException ex) {
            Exceptions.printStackTrace(ex);
        }
        editorAnnotations.getOrDefault(fileObject.getPath(), new LinkedList<>()).forEach(editorAnnotation -> {
            editorAnnotation.detach();
        });
        Executors.newSingleThreadExecutor().submit(new SonarAnalyzer(ProgressHandle.createHandle("Sonar")));
    }

    @Override
    public void runLocked(Runnable r) {
        LOGGER.log(Level.INFO, "RunLocked saveTaskImpl");
        r.run();
    }

    @Override
    public boolean cancel() {
        LOGGER.log(Level.INFO, "Cancel saveTaskImpl");
        return true;
    }
    
    public boolean tryToAttachAnnotation(Issue issue, FileObject fileObject) {
        try {
            LOGGER.log(Level.INFO, "file object {0}", fileObject.getPath());
            DataObject dataObject=DataObject.find(fileObject);
            LOGGER.log(Level.INFO, "data object {0}", dataObject.getName());
            EditorCookie editorCookie = getEditorCookie(dataObject);
            if(editorCookie != null) {
                Line line = getLine(editorCookie, issue.getStartLine());
                if(line != null) {
                    Annotation editorAnnotation = new SonarQubeEditorAnnotation(Severity.valueOf(issue.getSeverity()), issue.getMessage());
                    editorAnnotation.attach(line);
                    List<Annotation> list;
                    LOGGER.log(Level.INFO, "put file object path: {0}", fileObject.getPath());
                    if(editorAnnotations.containsKey(fileObject.getPath())) {
                        list=editorAnnotations.get(fileObject.getPath());
                    }else{
                        list=new LinkedList<>();
                        editorAnnotations.put(fileObject.getPath(), list);
                    }
                    list.add(editorAnnotation);
                    LOGGER.log(Level.INFO, "Attached Issue: {0}, {1}, {2}, {3}, {4}, {5}, {6}, {7}, {8}", new Object[]{issue.getInputFile().relativePath(), issue.getStartLine(), issue.getStartLineOffset(), issue.getEndLine(), issue.getEndLineOffset(), issue.getSeverity(), issue.getRuleName(), issue.getType(), issue.getMessage()});
                    return true;
                }
            }
            return false;
        } catch (DataObjectNotFoundException ex) {
            throw new RuntimeException(ex);
        }
    }
    private class SonarAnalyzer implements Callable<List<Issue>>{
        private ProgressHandle handle;

        public SonarAnalyzer(ProgressHandle handle) {
            this.handle = handle;
        }

        @Override
        public List<Issue> call() throws Exception {
            SwingUtilities.invokeLater(() -> {
                handle.start();
                handle.switchToIndeterminate();
            });
            try{
                List<Issue> issues=new LinkedList<>();
                Language[] enabledLanguages={Language.JAVA};
                ConnectedGlobalConfiguration globalConfig=ConnectedGlobalConfiguration.builder()
                        .setServerId("123")
        //                .setWorkDir(StoragePathManager.getServerWorkDir(getId()))
        //                .setStorageRoot(StoragePathManager.getServerStorageRoot())
                        .setLogOutput(new LogOutput() {

                            @Override
                            public void log(String string, LogOutput.Level level) {

                            }

                        })
                        .addEnabledLanguages(enabledLanguages)
                        .build();
                LinkedList<ClientInputFile> inputFiles = new LinkedList<>();

                ConnectedAnalysisConfiguration.Builder builder = ConnectedAnalysisConfiguration.builder();

                AtomicReference<String> projectKey=new AtomicReference();
                boolean found=false;
                for (Language enabledLanguage : enabledLanguages) {
                    if(fileObject.getExt().equals(enabledLanguage.getLanguageKey())) {
                        found=true;
                        break;
                    }
                }
                if(found) {
                    LOGGER.log(Level.INFO, "File object to analyze: {0}", fileObject.getNameExt());
                    ClientInputFile clientInputFile=null;
                    try {
                        clientInputFile = new ClientInputFileImpl(fileObject, document.getText(0, document.getLength()), false);
                    } catch (BadLocationException ex) {
                        throw new RuntimeException(ex);
                    }
                    try {
                        LOGGER.log(Level.INFO, "File content: {0}", clientInputFile.contents());
                    } catch (IOException ex) {
                        Exceptions.printStackTrace(ex);
                    }
                    inputFiles.add(clientInputFile);
                    Project pro = FileOwnerQuery.getOwner(fileObject);
                    projectKey.set(ProjectUtils.getInformation(pro).getName());
                    builder.setBaseDir(Paths.get(pro.getProjectDirectory().getPath()));
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
                        .userAgent("RadarLint")
                        //.credentials(login, password)
                        .build();
                    ConnectedSonarLintEngine engine = new ConnectedSonarLintEngineImpl(globalConfig);
                    engine.update(serverConfiguration, new ProgressMonitor() { });

                    engine.updateProject(serverConfiguration, projectKey.get(), new ProgressMonitor() { });
                    engine.analyze(analysisConfig, new IssueListener() {

                        @Override
                        public void handle(Issue issue) {
                            LOGGER.log(Level.INFO, "Issue: {0}, {1}, {2}, {3}, {4}, {5}, {6}, {7}, {8}", new Object[]{issue.getInputFile().relativePath(), issue.getStartLine(), issue.getStartLineOffset(), issue.getEndLine(), issue.getEndLineOffset(), issue.getSeverity(), issue.getRuleName(), issue.getType(), issue.getMessage()});
                            FileObject fileObject=issue.getInputFile().getClientObject();
                            boolean attached = tryToAttachAnnotation(issue, fileObject);
                            issues.add(issue);
                        }

                    }, new LogOutput() {

                        @Override
                        public void log(String string, LogOutput.Level level) {

                        }

                    }, new ProgressMonitor() {});
                }
                return issues;
            }finally{
                SwingUtilities.invokeLater(() -> {
                    handle.finish();
                });
            }
        }
        
        
    }
    
    public static EditorCookie getEditorCookie(DataObject dataObject) throws DataObjectNotFoundException {
        Lookup lookup = dataObject.getLookup();
        return lookup.lookup(EditorCookie.class);
    }
    
    public static Line getLine(EditorCookie editorCookie, Integer lineNumber) {
        Line.Set lineSet = editorCookie.getLineSet();
        int effectiveLineNumber = lineNumber == null || lineNumber <= 0 ? 1 : lineNumber;
        int index = Math.min(effectiveLineNumber, lineSet.getLines().size()) - 1;
        return index < 0 ? null : lineSet.getCurrent(index);
    }
    
}
