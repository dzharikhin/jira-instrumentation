package org.jrx.jira.instrumentation.validation.spi.issueupdater;

import com.atlassian.jira.issue.util.IssueUpdateBean;
import com.atlassian.plugin.spring.scanner.annotation.export.ExportAsService;
import org.jrx.jira.instrumentation.validation.api.issueupdater.IssueUpdaterValidator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Objects;

/**
 * Created by dzharikhin (https://github.com/dzharikhin) on 02.11.2016.
 */
@Component
@ExportAsService(IssueUpdaterValidatorAggregator.class)
public class IssueUpdaterValidatorAggregator implements IssueUpdaterValidator {

    private static final Logger log = LoggerFactory.getLogger(IssueUpdaterValidatorAggregator.class);

    private final BundleContext bundleContext;

    @Autowired
    public IssueUpdaterValidatorAggregator(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    @Override
    public IllegalArgumentException validate(IssueUpdateBean issueUpdateBean, boolean generateChangeItems) throws IllegalArgumentException {
        try {
            log.trace("Executing validate of IssueUpdaterValidatorAggregator");
            final Collection<ServiceReference<IssueUpdaterValidator>> serviceReferences = bundleContext.getServiceReferences(IssueUpdaterValidator.class, null);
            log.debug("Found services: {}", serviceReferences);
            return serviceReferences.stream().map(reference -> {
                final IssueUpdaterValidator service = bundleContext.getService(reference);
                if (service != null) {
                    return service.validate(issueUpdateBean, generateChangeItems);
                } else {
                    log.debug("Failed to get service from {}", reference);
                    return null;
                }
            }).filter(Objects::nonNull).findAny().orElse(null);
        } catch (InvalidSyntaxException e) {
            log.warn("Exception on getting IssueUpdaterValidatorAggregator", e);
            return null;
        }
    }
}
