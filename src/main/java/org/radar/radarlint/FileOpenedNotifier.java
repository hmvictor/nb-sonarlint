package org.radar.radarlint;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

/**
 * A component to notify when files are opened.
 * 
 * Used for attaching editor annotations because the data object is not available if it is not opened in the editor.
 * 
 * @author Victor
 */
public class FileOpenedNotifier implements PropertyChangeListener {

    public void init() {
        WindowManager.getDefault().getRegistry().addPropertyChangeListener(this);
    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {
        if(TopComponent.Registry.PROP_TC_OPENED.equals(event.getPropertyName())) {
            getFileObject((TopComponent) event.getNewValue()).ifPresent(fileObject -> {
                try{
                    SonarLintScanner.of(fileObject).ifPresent(scanner -> scanner.runAsync());
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            });
        } else if(TopComponent.Registry.PROP_TC_CLOSED.equals(event.getPropertyName())) {
            getFileObject((TopComponent) event.getNewValue())
                .ifPresent(fileObject -> EditorAnnotator.getInstance().cleanEditorAnnotations(fileObject));
        }
    }

    public static Optional<FileObject> getFileObject(TopComponent topComponent) {
        Objects.requireNonNull(topComponent);
        FileObject fileObject = null;
        DataObject dataObject = topComponent.getLookup().lookup(DataObject.class);
        if (dataObject != null) {
            fileObject = dataObject.getPrimaryFile();
        }
        return Optional.ofNullable(fileObject);
    }

}
