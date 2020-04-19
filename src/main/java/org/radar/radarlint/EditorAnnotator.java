package org.radar.radarlint;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openide.cookies.EditorCookie;
import org.openide.filesystems.FileObject;
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
public class EditorAnnotator {
    private static final Logger LOGGER = Logger.getLogger(EditorAnnotator.class.getName());
    private static Map<String, List<Annotation>> EDITOR_ANNOTATIONS=new HashMap<>();

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
                    List<Annotation> list;
                    LOGGER.log(Level.INFO, "put file object path: {0}", fileObject.getPath());
                    if(EDITOR_ANNOTATIONS.containsKey(fileObject.getPath())) {
                        list=EDITOR_ANNOTATIONS.get(fileObject.getPath());
                    }else{
                        list=new LinkedList<>();
                        EDITOR_ANNOTATIONS.put(fileObject.getPath(), list);
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

    public void cleanEditorAnnotations(FileObject fileObject) {
        LOGGER.log(Level.INFO, "Number of editor annotations: {0}", EDITOR_ANNOTATIONS.getOrDefault(fileObject, new LinkedList<>()).size());
        EDITOR_ANNOTATIONS.getOrDefault(fileObject.getPath(), new LinkedList<>()).forEach(editorAnnotation -> {
            editorAnnotation.detach();
        });
    }
    
}
