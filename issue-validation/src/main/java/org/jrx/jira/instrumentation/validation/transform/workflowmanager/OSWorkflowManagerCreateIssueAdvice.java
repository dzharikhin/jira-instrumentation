package org.jrx.jira.instrumentation.validation.transform.workflowmanager;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.util.IssueUpdateBean;
import com.atlassian.jira.workflow.WorkflowException;
import com.atlassian.plugin.Plugin;
import net.bytebuddy.asm.Advice;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * Created by dzharikhin (https://github.com/dzharikhin) on 02.11.2016.
 */
public class OSWorkflowManagerCreateIssueAdvice {

    @Advice.OnMethodEnter
    public static void intercept(
            @Advice.Argument(0) String remoteUserName,
            @Advice.Argument(1) Map<String, Object> fields
    ) {
        try {
            final Plugin plugin = ComponentAccessor.getPluginAccessor().getEnabledPlugin("org.jrx.jira.instrumentation.issue-validation");
            final Class<?> workflowManagerCreateIssueValidatorClass = plugin != null ? plugin.getClassLoader().loadClass("WorkflowManagerCreateIssueValidatorAggregator") : null;
            final Object workflowManagerCreateIssueValidator = workflowManagerCreateIssueValidatorClass != null ? ComponentAccessor.getOSGiComponentInstanceOfType(workflowManagerCreateIssueValidatorClass) : null;
            if (workflowManagerCreateIssueValidator != null) {
                final Method validate = workflowManagerCreateIssueValidator.getClass().getMethod("validate", IssueUpdateBean.class, boolean.class);
                if (validate != null) {
                    final Object exception = validate.invoke(workflowManagerCreateIssueValidator, remoteUserName, fields);
                    if (exception != null) {
                        throw (WorkflowException) exception;
                    }
                } else {
                    System.err.println("==**Warn: method validate is not found on aggregator" + "**==");
                }
            }
        } catch (ReflectiveOperationException e) {
            System.err.println("==**Warn: Exception on additional logic of createIssue " + e + "**==");
        }
    }
}
