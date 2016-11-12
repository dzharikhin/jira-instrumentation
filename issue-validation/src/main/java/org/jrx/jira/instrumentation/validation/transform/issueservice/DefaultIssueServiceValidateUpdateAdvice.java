package org.jrx.jira.instrumentation.validation.transform.issueservice;

import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.IssueInputParameters;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugin.Plugin;
import net.bytebuddy.asm.Advice;

import java.lang.reflect.Method;

/**
 * Created by dzharikhin (https://github.com/dzharikhin) on 31.10.2016.
 */
public class DefaultIssueServiceValidateUpdateAdvice {

    @Advice.OnMethodExit(onThrowable = IllegalArgumentException.class)
    public static void intercept(
            @Advice.Return(readOnly = false) IssueService.UpdateValidationResult originalResult,
            @Advice.Thrown Throwable throwable,
            @Advice.Argument(0) ApplicationUser user,
            @Advice.Argument(1) Long issueId,
            @Advice.Argument(2) IssueInputParameters issueInputParameters
    ) {
        try {
            if (throwable == null) {
                final Plugin plugin = ComponentAccessor.getPluginAccessor().getEnabledPlugin("org.jrx.jira.instrumentation.issue-validation");
                final Class<?> issueValidatorClass = plugin != null ? plugin.getClassLoader().loadClass("org.jrx.jira.instrumentation.validation.spi.issueservice.IssueServiceValidateUpdateValidatorAggregator") : null;
                final Object issueValidator = issueValidatorClass != null ? ComponentAccessor.getOSGiComponentInstanceOfType(issueValidatorClass) : null;
                if (issueValidator != null) {
                    final Method validate = issueValidator.getClass().getMethod("validate", IssueService.UpdateValidationResult.class, ApplicationUser.class, Long.class, IssueInputParameters.class);
                    if (validate != null) {
                        final IssueService.UpdateValidationResult validationResult = (IssueService.UpdateValidationResult) validate
                            .invoke(issueValidator, originalResult, user, issueId, issueInputParameters);
                        if (validationResult != null) {
                            originalResult = validationResult;
                        }
                    } else {
                        System.err.println("==**Warn: method validate is not found on aggregator " + "**==");
                    }
                }
            }
        //Nothing should break service
        } catch (Throwable e) {
            System.err.println("==**Warn: Exception on additional logic of validateUpdate " + e + "**==");
        }
    }
}
