package org.jrx.jira.instrumentation.validation.transform.issueservice;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.IssueInputParameters;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugin.Plugin;
import net.bytebuddy.asm.Advice;

import java.lang.reflect.Method;

import static com.atlassian.jira.bc.issue.IssueService.*;

/**
 * Created by dzharikhin (https://github.com/dzharikhin) on 27.10.2016.
 */
public class DefaultIssueServiceValidateCreateAdvice {

    @Advice.OnMethodExit(onThrowable = IllegalArgumentException.class)
    public static void intercept(
        @Advice.Return(readOnly = false) CreateValidationResult originalResult,
        @Advice.Thrown Throwable throwable,
        @Advice.Argument(0) ApplicationUser user,
        @Advice.Argument(1) IssueInputParameters issueInputParameters
    ) {
        try {
            if (throwable == null) {
                //current plugin key
                final Plugin plugin = ComponentAccessor.getPluginAccessor().getEnabledPlugin("org.jrx.jira.instrumentation.issue-validation");
                //related aggregator class
                final Class<?> issueValidatorClass = plugin != null ? plugin.getClassLoader().loadClass("org.jrx.jira.instrumentation.validation.spi.issueservice.IssueServiceValidateCreateValidatorAggregator") : null;
                final Object issueValidator = issueValidatorClass != null ? ComponentAccessor.getOSGiComponentInstanceOfType(issueValidatorClass) : null;
                if (issueValidator != null) {
                    final Method validate = issueValidator.getClass().getMethod("validate", CreateValidationResult.class, ApplicationUser.class, IssueInputParameters.class);
                    if (validate != null) {
                        final CreateValidationResult validationResult = (CreateValidationResult) validate
                            .invoke(issueValidator, originalResult, user, issueInputParameters);
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
            System.err.println("==**Warn: Exception on additional logic of validateCreate " + e + "**==");
        }
    }
}
