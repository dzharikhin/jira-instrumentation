package org.jrx.jira.instrumentation.validation.api.issueupdater;

import com.atlassian.jira.issue.util.IssueUpdateBean;

/**
 * Created by dzharikhin (https://github.com/dzharikhin) on 02.11.2016.
 * * Published via OSGi - implement this interface and publish as a service to apply validation on
 * @see com.atlassian.jira.issue.util.DefaultIssueUpdater#doUpdate(IssueUpdateBean, boolean)
 */
public interface IssueUpdaterValidator {

    IllegalArgumentException validate(IssueUpdateBean issueUpdateBean, boolean generateChangeItems);
}
