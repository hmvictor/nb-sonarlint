package org.radar.radarlint;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.openide.cookies.EditorCookie;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.text.Annotation;
import org.openide.text.Line;
import org.openide.util.Lookup;
import org.sonarsource.sonarlint.core.client.api.common.analysis.Issue;

/**
 *
 * @author Víctor
 */
public final class EditorAnnotator {
    private static final Logger LOGGER = Logger.getLogger(EditorAnnotator.class.getName());
    private static Map<String, Map<String, List<Annotation>>> editorAnnotationsMap=new HashMap<>();
    private static final EditorAnnotator INSTANCE=new EditorAnnotator();
    
    private EditorAnnotator() {
        
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
            EditorAnnotator editorAnnotator=new EditorAnnotator();
            EditorCookie editorCookie = editorAnnotator.getEditorCookie(dataObject);
            if(editorCookie != null) {
                Line line = editorAnnotator.getLine(editorCookie, issue.getStartLine());
                if(line != null) {
                    Annotation editorAnnotation = new SonarQubeEditorAnnotation(Severity.valueOf(issue.getSeverity()), issue.getMessage());
                    editorAnnotation.attach(line);
                    LOGGER.log(Level.INFO, "put file object path: {0}", fileObject.getPath());
                    
                    editorAnnotationsMap.putIfAbsent(ownerProject.getProjectDirectory().getPath(), new HashMap<>());
                    Map<String, List<Annotation>> editorAnnotationsByFile = editorAnnotationsMap.get(ownerProject.getProjectDirectory().getPath());
                    editorAnnotationsByFile.putIfAbsent(fileObject.getPath(), new LinkedList<>());
                    List<Annotation> editorAnnotations = editorAnnotationsByFile.get(fileObject.getPath());
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
        Map<String, List<Annotation>> editorAnnotationsByFile = editorAnnotationsMap.getOrDefault(project.getProjectDirectory().getPath(), new HashMap<>());
        editorAnnotationsByFile.entrySet().forEach((entry) -> {
            entry.getValue().forEach((annotation) -> {
                annotation.detach();
            });
        });
        editorAnnotationsMap.remove(project.getProjectDirectory().getPath());
    }

    public void cleanEditorAnnotations(FileObject fileObject) {
        Project ownerProject = FileOwnerQuery.getOwner(fileObject);
        Map<String, List<Annotation>> editorAnnotationsByFile = editorAnnotationsMap.getOrDefault(ownerProject.getProjectDirectory().getPath(), new HashMap<>());
        editorAnnotationsByFile.getOrDefault(fileObject.getPath(), new LinkedList<>()).forEach((annotation) -> {
            annotation.detach();
        });
        editorAnnotationsByFile.remove(fileObject.getPath());
    }
    
    public List<FileObject> getFileObjects(Project project) {
        List<FileObject> paths=new LinkedList<>();
        Map<String, List<Annotation>> editorAnnotationsByFile = editorAnnotationsMap.getOrDefault(project.getProjectDirectory().getPath(), new HashMap<>());
        editorAnnotationsByFile.keySet().forEach((filePath) -> {
            paths.add(FileUtil.toFileObject(new File(filePath)));
        });
        return paths;
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
