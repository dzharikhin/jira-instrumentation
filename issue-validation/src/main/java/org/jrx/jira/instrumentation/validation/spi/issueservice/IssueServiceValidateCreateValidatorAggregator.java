package org.jrx.jira.instrumentation.validation.spi.issueservice;

import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.issue.IssueInputParameters;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugin.spring.scanner.annotation.export.ExportAsService;
import org.jrx.jira.instrumentation.validation.api.issueservice.IssueServiceValidateCreateValidator;
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
@ExportAsService(IssueServiceValidateCreateValidatorAggregator.class)
public class IssueServiceValidateCreateValidatorAggregator implements IssueServiceValidateCreateValidator {

    private static final Logger log = LoggerFactory.getLogger(IssueServiceValidateCreateValidatorAggregator.class);

    private final BundleContext bundleContext;

    @Autowired
    public IssueServiceValidateCreateValidatorAggregator(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    @Nonnull
    @Override
    public IssueService.CreateValidationResult validate(@Nonnull final IssueService.CreateValidationResult originalResult, final ApplicationUser user, final IssueInputParameters issueInputParameters) {
        try {
            log.trace("Executing validate of IssueServiceValidateCreateValidatorAggregator");
            final Collection<ServiceReference<IssueServiceValidateCreateValidator>> serviceReferences = bundleContext.getServiceReferences(IssueServiceValidateCreateValidator.class, null);
            log.debug("Found services: {}", serviceReferences);
            IssueService.CreateValidationResult result = originalResult;
            for (ServiceReference<IssueServiceValidateCreateValidator> serviceReference : serviceReferences) {
                final IssueServiceValidateCreateValidator service = bundleContext.getService(serviceReference);
                if (service != null) {
                    result = service.validate(result, user, issueInputParameters);
                } else {
                    log.debug("Failed to get service from {}", serviceReference);
                }
            }
            return result;
        } catch (InvalidSyntaxException e) {
            log.warn("Exception on getting IssueServiceValidateCreateValidator", e);
            return originalResult;
        }
    }
}
