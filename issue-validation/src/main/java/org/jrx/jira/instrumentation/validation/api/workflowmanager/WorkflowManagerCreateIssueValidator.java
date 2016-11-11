package org.jrx.jira.instrumentation.validation.api.workflowmanager;

import com.atlassian.jira.workflow.WorkflowException;

import java.util.Map;

/**
 * Created by dzharikhin (https://github.com/dzharikhin) on 02.11.2016.
 * * Published via OSGi - implement this interface and publish as a service to apply validation on
 * @see com.atlassian.jira.workflow.OSWorkflowManager#createIssue(String, Map)
 */
public interface WorkflowManagerCreateIssueValidator {

    WorkflowException validate(String remoteUserName, Map<String, Object> fields);
}
