package org.radar.radarlint.ui;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.util.ResourceBundle;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;
import org.netbeans.api.editor.EditorActionRegistration;
import org.netbeans.editor.BaseAction;
import org.netbeans.modules.parsing.api.Source;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.filesystems.FileObject;
import org.openide.util.NbBundle;
import org.openide.windows.WindowManager;
import org.radar.radarlint.EditorAnnotator;
import org.radar.radarlint.SonarLintEngineFactory;
import org.sonarsource.sonarlint.core.client.api.connected.Language;

/**
 *
 * @author Víctor
 */
@NbBundle.Messages({
    "CTL_SomeAction=Show Rule Details",
    "show-rule-details=Show Rule Details"
})
@EditorActionRegistration(
    category = "Navigate",
    name = "show-rule-details",
    popupText = "Show Rule Details",
    popupPosition = 10000
)
public class RuleDetailsAction extends BaseAction{

    @Override
    public void actionPerformed(ActionEvent ae, JTextComponent component) {
        int caretPosition = component.getCaretPosition();
        Element root = component.getDocument().getDefaultRootElement();
        int lineNumber = root.getElementIndex(caretPosition) + 1;
        Source source = Source.create(component.getDocument());
        FileObject fileObject = source.getFileObject();
        EditorAnnotator.getInstance().getIssueAnnotation(fileObject, lineNumber)
            .ifPresentOrElse(issueAnnotation -> {
                RuleDialog.showRule(WindowManager.getDefault().getMainWindow(), SonarLintEngineFactory.getOrCreateEngine(Language.values()).getRuleDetails(issueAnnotation.getIssue().getRuleKey()));
            }, () -> {
                Toolkit.getDefaultToolkit().beep();
                ResourceBundle bundle = NbBundle.getBundle(RuleDetailsAction.class);
                DialogDisplayer.getDefault().notify(new NotifyDescriptor.Message(bundle.getString("RuleDetailsAction.noIssueAtLocation"), NotifyDescriptor.WARNING_MESSAGE));
            });
    }
    
}
