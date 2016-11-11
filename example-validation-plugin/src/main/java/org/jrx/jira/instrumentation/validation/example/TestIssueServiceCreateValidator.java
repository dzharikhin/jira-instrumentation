package org.jrx.jira.instrumentation.validation.example;

import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.issue.IssueFieldConstants;
import com.atlassian.jira.issue.IssueInputParameters;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.ErrorCollection;
import com.atlassian.plugin.spring.scanner.annotation.export.ExportAsService;
import org.jrx.jira.instrumentation.validation.api.issueservice.IssueServiceValidateCreateValidator;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;

/**
 * Created by dzharikhin (https://github.com/dzharikhin) on 03.11.2016.
 */
@Component
@ExportAsService
public class TestIssueServiceCreateValidator implements IssueServiceValidateCreateValidator {
    @Nonnull
    @Override
    public IssueService.CreateValidationResult validate(@Nonnull IssueService.CreateValidationResult originalResult, ApplicationUser user, IssueInputParameters issueInputParameters) {
        originalResult.getErrorCollection().addError(IssueFieldConstants.ASSIGNEE, "This validation works", ErrorCollection.Reason.VALIDATION_FAILED);
        return originalResult;
    }
}
