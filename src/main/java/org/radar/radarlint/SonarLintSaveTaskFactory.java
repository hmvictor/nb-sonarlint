package org.radar.radarlint;

import java.util.Optional;
import javax.swing.text.BadLocationException;
import org.netbeans.api.editor.mimelookup.MimeRegistration;
import org.netbeans.spi.editor.document.OnSaveTask;

/**
 *
 * @author Víctor
 */
@MimeRegistration(mimeType = "", service = OnSaveTask.Factory.class, position = 1500)
public class SonarLintSaveTaskFactory implements OnSaveTask.Factory {

    @Override
    public OnSaveTask createTask(OnSaveTask.Context context) {
        try{
            Optional<SonarLintScanner> optionalScanner = SonarLintScanner.of(context.getDocument());
            if(optionalScanner.isPresent()) {
                return new SonarLintSaveTask(optionalScanner.get());
            }else{
                return null;
            }
        } catch (BadLocationException ex) {
            throw new RuntimeException(ex);
        }
    }
    
}
