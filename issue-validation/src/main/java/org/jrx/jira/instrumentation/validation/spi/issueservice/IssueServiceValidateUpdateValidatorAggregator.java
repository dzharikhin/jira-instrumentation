package org.jrx.jira.instrumentation.validation.spi.issueservice;

import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.issue.IssueInputParameters;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugin.spring.scanner.annotation.export.ExportAsService;
import org.jrx.jira.instrumentation.validation.api.issueservice.IssueServiceValidateUpdateValidator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import java.util.Collection;

/**
 * Created by dzharikhin (https://github.com/dzharikhin) on 31.10.2016.
 */
@Component
@ExportAsService(IssueServiceValidateUpdateValidatorAggregator.class)
public class IssueServiceValidateUpdateValidatorAggregator implements IssueServiceValidateUpdateValidator {

    private static final Logger log = LoggerFactory.getLogger(IssueServiceValidateUpdateValidatorAggregator.class);

    private final BundleContext bundleContext;

    @Autowired
    public IssueServiceValidateUpdateValidatorAggregator(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    @Nonnull
    @Override
    public IssueService.UpdateValidationResult validate(@Nonnull final IssueService.UpdateValidationResult originalResult, final ApplicationUser user, final Long issueId, final IssueInputParameters issueInputParameters) {
        try {
            log.trace("Executing validate of IssueServiceValidateUpdateValidatorAggregator");
            final Collection<ServiceReference<IssueServiceValidateUpdateValidator>> serviceReferences = bundleContext.getServiceReferences(IssueServiceValidateUpdateValidator.class, null);
            log.debug("Found services: {}", serviceReferences);
            IssueService.UpdateValidationResult result = originalResult;
            for (ServiceReference<IssueServiceValidateUpdateValidator> serviceReference : serviceReferences) {
                final IssueServiceValidateUpdateValidator service = bundleContext.getService(serviceReference);
                if (service != null) {
                    result = service.validate(result, user, issueId, issueInputParameters);
                } else {
                    log.debug("Failed to get service from {}", serviceReference);
                }
            }
            return result;
        } catch (InvalidSyntaxException e) {
            log.warn("Exception on getting IssueServiceValidateUpdateValidator", e);
            return originalResult;
        }
    }
}
