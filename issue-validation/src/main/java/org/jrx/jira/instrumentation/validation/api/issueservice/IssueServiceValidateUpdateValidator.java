package org.jrx.jira.instrumentation.validation.api.issueservice;

import com.atlassian.jira.issue.IssueInputParameters;
import com.atlassian.jira.user.ApplicationUser;

import javax.annotation.Nonnull;

import static com.atlassian.jira.bc.issue.IssueService.*;

/**
 * Created by dzharikhin (https://github.com/dzharikhin) on 31.10.2016.
 * * Published via OSGi - implement this interface and publish as a service to apply validation on
 * @see com.atlassian.jira.bc.issue.DefaultIssueService#validateUpdate(ApplicationUser, Long, IssueInputParameters)
 */
public interface IssueServiceValidateUpdateValidator {

    @Nonnull UpdateValidationResult validate(
        final @Nonnull UpdateValidationResult originalResult,
        final ApplicationUser user,
        final Long issueId,
        final IssueInputParameters issueInputParameters
    );
}
