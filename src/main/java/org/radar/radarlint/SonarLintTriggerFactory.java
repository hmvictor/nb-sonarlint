package org.radar.radarlint;

import org.netbeans.api.editor.mimelookup.MimeRegistration;
import org.netbeans.spi.editor.document.OnSaveTask;

/**
 *
 * @author Víctor
 */
@MimeRegistration(mimeType = "", service = OnSaveTask.Factory.class, position = 1500)
public class SonarLintTriggerFactory implements OnSaveTask.Factory{

    @Override
    public OnSaveTask createTask(OnSaveTask.Context context) {
        return new SonarLintTrigger(context.getDocument());
    }
    
}
