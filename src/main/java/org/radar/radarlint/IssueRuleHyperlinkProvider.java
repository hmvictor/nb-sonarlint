package org.radar.radarlint;

import java.awt.Toolkit;
import java.util.EnumSet;
import java.util.ResourceBundle;
import java.util.Set;
import javax.swing.text.Document;
import javax.swing.text.Element;
import org.netbeans.api.editor.mimelookup.MimeRegistration;
import org.netbeans.lib.editor.hyperlink.spi.HyperlinkProviderExt;
import org.netbeans.lib.editor.hyperlink.spi.HyperlinkType;
import org.netbeans.modules.parsing.api.Source;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.filesystems.FileObject;
import org.openide.util.NbBundle;
import org.openide.windows.WindowManager;
import org.radar.radarlint.ui.RuleDetailsAction;
import org.radar.radarlint.ui.RuleDialog;
import org.sonarsource.sonarlint.core.client.api.connected.Language;

/**
 *
 * @author Víctor
 */
@MimeRegistration(mimeType = "", service = HyperlinkProviderExt.class)
public class IssueRuleHyperlinkProvider implements HyperlinkProviderExt {

    @Override
    public Set<HyperlinkType> getSupportedHyperlinkTypes() {
        return EnumSet.of(HyperlinkType.GO_TO_DECLARATION, HyperlinkType.ALT_HYPERLINK);
    }

    @Override
    public boolean isHyperlinkPoint(Document document, int offset, HyperlinkType ht) {
        Element root = document.getDefaultRootElement();
        int lineNumber = root.getElementIndex(offset)+1;
        Source source = Source.create(document);
        FileObject fileObject = source.getFileObject();
        return EditorAnnotator.getInstance().getIssueAnnotation(fileObject, lineNumber).isPresent();
    }

    @Override
    public int[] getHyperlinkSpan(Document document, int offset, HyperlinkType ht) {
        Element root = document.getDefaultRootElement();
        int lineNumber = root.getElementIndex(offset)+1;
        Source source = Source.create(document);
        FileObject fileObject = source.getFileObject();
        if(EditorAnnotator.getInstance().getIssueAnnotation(fileObject, lineNumber).isPresent()) {
            Element lineElement = root.getElement(root.getElementIndex(offset));
            return new int[]{lineElement.getStartOffset(), lineElement.getEndOffset()};
        }else{
            return null;
        }
    }

    @Override
    public void performClickAction(Document document, int offset, HyperlinkType ht) {
        Element root = document.getDefaultRootElement();
        int lineNumber = root.getElementIndex(offset)+1;
        Source source = Source.create(document);
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

    @Override
    public String getTooltipText(Document dcmnt, int i, HyperlinkType ht) {
        return "Show Rule Details";
    }
    
}
