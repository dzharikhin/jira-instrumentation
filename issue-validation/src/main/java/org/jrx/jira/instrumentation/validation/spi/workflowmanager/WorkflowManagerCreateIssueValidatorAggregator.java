package org.jrx.jira.instrumentation.validation.spi.workflowmanager;

import com.atlassian.jira.workflow.WorkflowException;
import com.atlassian.plugin.spring.scanner.annotation.export.ExportAsService;
import org.jrx.jira.instrumentation.validation.api.workflowmanager.WorkflowManagerCreateIssueValidator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;

/**
 * Created by dzharikhin (https://github.com/dzharikhin) on 02.11.2016.
 */
@Component
@ExportAsService(WorkflowManagerCreateIssueValidatorAggregator.class)
public class WorkflowManagerCreateIssueValidatorAggregator implements WorkflowManagerCreateIssueValidator {

    private static final Logger log = LoggerFactory.getLogger(WorkflowManagerCreateIssueValidatorAggregator.class);

    private final BundleContext bundleContext;

    @Autowired
    public WorkflowManagerCreateIssueValidatorAggregator(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    @Override
    public WorkflowException validate(String remoteUserName, Map<String, Object> fields) throws WorkflowException {
        try {
            log.trace("Executing validate of WorkflowCreateIssueValidatorAggregator");
            final Collection<ServiceReference<WorkflowManagerCreateIssueValidator>> serviceReferences = bundleContext.getServiceReferences(WorkflowManagerCreateIssueValidator.class, null);
            log.debug("Found services: {}", serviceReferences);
            return serviceReferences.stream().map(reference -> {
                final WorkflowManagerCreateIssueValidator service = bundleContext.getService(reference);
                if (service != null) {
                    return service.validate(remoteUserName, fields);
                } else {
                    log.debug("Failed to get service from {}", reference);
                    return null;
                }
            }).filter(Objects::nonNull).findAny().orElse(null);
        } catch (InvalidSyntaxException e) {
            log.warn("Exception on getting WorkflowCreateIssueValidatorAggregator", e);
            return null;
        }
    }
}
