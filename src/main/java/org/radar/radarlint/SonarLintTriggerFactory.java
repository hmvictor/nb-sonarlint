package org.radar.radarlint;

import org.netbeans.api.editor.mimelookup.MimeRegistration;
import org.netbeans.spi.editor.document.OnSaveTask;

/**
 *
 * @author Víctor
 */
@MimeRegistration(mimeType = "", service = OnSaveTask.Factory.class, position = 1500)
public class SonarLintTriggerFactory implements OnSaveTask.Factory{
//    private static final Logger LOGGER = Logger.getLogger(SonarLintTriggerFactory.class.getName());

    @Override
    public OnSaveTask createTask(OnSaveTask.Context context) {
//        LOGGER.log(Level.INFO, "Creating task");
//        Source source = Source.create(context.getDocument());
//        FileObject fileObject = source.getFileObject();
//        LOGGER.log(Level.INFO, "Return task");
        return new SonarLintTrigger(context.getDocument());
    }
    
}
