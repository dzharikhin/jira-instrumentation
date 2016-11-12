package org.jrx.jira.instrumentation.validation.transform.issueupdater;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.util.IssueUpdateBean;
import com.atlassian.plugin.Plugin;
import net.bytebuddy.asm.Advice;

import java.lang.reflect.Method;

/**
 * Created by dzharikhin (https://github.com/dzharikhin) on 02.11.2016.
 */
public class DefaultIssueUpdaterAdvice {

    @Advice.OnMethodEnter
    public static void intercept(
        @Advice.Argument(0) IssueUpdateBean issueUpdateBean,
        @Advice.Argument(1) boolean generateChangeItems
    ) {
        try {
            final Plugin plugin = ComponentAccessor.getPluginAccessor().getEnabledPlugin("org.jrx.jira.instrumentation.issue-validation");
            final Class<?> issueUpdaterValidatorClass = plugin != null ? plugin.getClassLoader().loadClass("org.jrx.jira.instrumentation.validation.spi.issueupdater.IssueUpdaterValidatorAggregator") : null;
            final Object issueUpdaterValidator = issueUpdaterValidatorClass != null ? ComponentAccessor.getOSGiComponentInstanceOfType(issueUpdaterValidatorClass) : null;
            if (issueUpdaterValidator != null) {
                final Method validate = issueUpdaterValidator.getClass().getMethod("validate", IssueUpdateBean.class, boolean.class);
                if (validate != null) {
                    final Object exception = validate.invoke(issueUpdaterValidator, issueUpdateBean, generateChangeItems);
                    if (exception != null) {
                        throw (IllegalArgumentException) exception;
                    }
                } else {
                    System.err.println("==**Warn: method validate is not found on aggregator" + "**==");
                }
            }
        } catch (ReflectiveOperationException e) {
            System.err.println("==**Warn: Exception on additional logic of doUpdate onEnter " + e.getMessage() + "**==");
        }
    }
}
