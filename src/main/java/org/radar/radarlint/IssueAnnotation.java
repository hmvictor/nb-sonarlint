package org.radar.radarlint;

import org.openide.text.Annotation;
import org.sonarsource.sonarlint.core.client.api.common.analysis.Issue;

/**
 *
 * @author Victor
 */
public class IssueAnnotation extends Annotation{
    private Issue issue;
    private final Severity severity;
    private final String description;

    public IssueAnnotation(Issue issue) {
        this.issue=issue;
        this.severity = Severity.valueOf(issue.getSeverity());
        this.description=severity+": "+issue.getMessage();
    }

    public Issue getIssue() {
        return issue;
    }
    
    @Override
    public String getAnnotationType() {
        return "sonarqube-"+severity.name().toLowerCase()+"-annotation";
    }

    @Override
    public String getShortDescription() {
        return description;
    }
    
}
