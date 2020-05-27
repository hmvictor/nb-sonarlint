package org.radar.radarlint;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.openide.cookies.EditorCookie;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.text.Line;
import org.openide.util.Lookup;
import org.sonarsource.sonarlint.core.client.api.common.analysis.Issue;

/**
 *
 * @author Víctor
 */
public final class EditorAnnotator {
    private static final Logger LOGGER = Logger.getLogger(EditorAnnotator.class.getName());
    private static Map<String, Map<String, List<IssueAnnotation>>> editorAnnotationsMap=new HashMap<>();
    private static final EditorAnnotator INSTANCE=new EditorAnnotator();
    
    private EditorAnnotator() {
        
    }
    
    public Optional<EditorCookie> getEditorCookie(FileObject fileObject) throws DataObjectNotFoundException {
        DataObject dataObject=DataObject.find(fileObject);
        if(dataObject != null) {
            Lookup lookup = dataObject.getLookup();
            return Optional.ofNullable(lookup.lookup(EditorCookie.class));
        } else {
            return Optional.empty();
        }
    }

    private EditorCookie getEditorCookie(DataObject dataObject) throws DataObjectNotFoundException {
        Lookup lookup = dataObject.getLookup();
        return lookup.lookup(EditorCookie.class);
    }

    private Line getLine(EditorCookie editorCookie, Integer lineNumber) {
        Line.Set lineSet = editorCookie.getLineSet();
        int effectiveLineNumber = lineNumber == null || lineNumber <= 0 ? 1 : lineNumber;
        int index = Math.min(effectiveLineNumber, lineSet.getLines().size()) - 1;
        return index < 0 ? null : lineSet.getCurrent(index);
    }
    
    public boolean tryToAttachAnnotation(Issue issue, FileObject fileObject) {
        try {
            Project ownerProject = FileOwnerQuery.getOwner(fileObject);
            LOGGER.log(Level.INFO, "file object {0}", fileObject.getPath());
            DataObject dataObject=DataObject.find(fileObject);
            LOGGER.log(Level.INFO, "data object {0}", dataObject.getName());
            EditorCookie editorCookie = getEditorCookie(dataObject);
            if(editorCookie != null) {
                Line line = getLine(editorCookie, issue.getStartLine());
                if(line != null) {
                    IssueAnnotation editorAnnotation = new IssueAnnotation(issue);
                    editorAnnotation.attach(line);
                    LOGGER.log(Level.INFO, "put file object path: {0}", fileObject.getPath());
                    
                    editorAnnotationsMap.putIfAbsent(ownerProject.getProjectDirectory().getPath(), new HashMap<>());
                    Map<String, List<IssueAnnotation>> editorAnnotationsByFile = editorAnnotationsMap.get(ownerProject.getProjectDirectory().getPath());
                    editorAnnotationsByFile.putIfAbsent(fileObject.getPath(), new LinkedList<>());
                    List<IssueAnnotation> editorAnnotations = editorAnnotationsByFile.get(fileObject.getPath());
                    editorAnnotations.add(editorAnnotation);
                    
                    LOGGER.log(Level.INFO, "Attached Issue: {0}, {1}, {2}, {3}, {4}, {5}, {6}, {7}, {8}", new Object[]{issue.getInputFile().relativePath(), issue.getStartLine(), issue.getStartLineOffset(), issue.getEndLine(), issue.getEndLineOffset(), issue.getSeverity(), issue.getRuleName(), issue.getType(), issue.getMessage()});
                    return true;
                }
            }
            return false;
        } catch (DataObjectNotFoundException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    public void cleanEditorAnnotations(Project project) {
        Map<String, List<IssueAnnotation>> editorAnnotationsByFile = editorAnnotationsMap.getOrDefault(project.getProjectDirectory().getPath(), new HashMap<>());
        editorAnnotationsByFile.entrySet().forEach((entry) -> {
            entry.getValue().forEach((annotation) -> {
                annotation.detach();
            });
        });
        editorAnnotationsMap.remove(project.getProjectDirectory().getPath());
    }

    public void cleanEditorAnnotations(FileObject fileObject) {
        Project ownerProject = FileOwnerQuery.getOwner(fileObject);
        Map<String, List<IssueAnnotation>> editorAnnotationsByFile = editorAnnotationsMap.getOrDefault(ownerProject.getProjectDirectory().getPath(), new HashMap<>());
        editorAnnotationsByFile.getOrDefault(fileObject.getPath(), new LinkedList<>()).forEach((annotation) -> {
            annotation.detach();
        });
        editorAnnotationsByFile.remove(fileObject.getPath());
    }
    
    public List<FileObject> getFileObjects(Project project) {
        List<FileObject> paths=new LinkedList<>();
        Map<String, List<IssueAnnotation>> editorAnnotationsByFile = editorAnnotationsMap.getOrDefault(project.getProjectDirectory().getPath(), new HashMap<>());
        editorAnnotationsByFile.keySet().forEach((filePath) -> {
            paths.add(FileUtil.toFileObject(new File(filePath)));
        });
        return paths;
    }
    
    public Optional<IssueAnnotation> getIssueAnnotation(FileObject fileObject, int line) {
        Project ownerProject = FileOwnerQuery.getOwner(fileObject);
        if(ownerProject != null) {
            Map<String, List<IssueAnnotation>> editorAnnotationsByFile = editorAnnotationsMap.getOrDefault(ownerProject.getProjectDirectory().getPath(), new HashMap<>());
            for (IssueAnnotation annotation : editorAnnotationsByFile.getOrDefault(fileObject.getPath(), new LinkedList<>())) {
                if(annotation.getIssue().getStartLine() != null && annotation.getIssue().getStartLine() == line) {
                    return Optional.of(annotation);
                }
            }
        }
        return Optional.empty();
    }
    
    /**
     * Gets the singleton instance.
     * 
     * @return 
     */
    public static EditorAnnotator getInstance() {
        return INSTANCE;
    }

}
