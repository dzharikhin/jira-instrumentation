package org.jrx.jira.instrumentation.provider.engine;

import org.jrx.jira.instrumentation.provider.api.InstrumentationAgentException;
import org.jrx.jira.instrumentation.provider.api.InstrumentationConsumer;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.instrument.Instrumentation;

/**
 * Created by dzharikhin (https://github.com/dzharikhin) on 26.10.2016.
 */
@Component
public class Loader implements InitializingBean, DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(Loader.class);

    private final AgentInstaller agentInstaller;
    private final ServiceTracker<InstrumentationConsumer, Void> serviceTracker;

    @Autowired
    public Loader(
        BundleContext bundleContext,
        AgentInstaller agentInstaller,
        InstrumentationProvider instrumentationProvider
    ) {
        this.agentInstaller = agentInstaller;
        this.serviceTracker = initTracker(bundleContext, instrumentationProvider);
    }

    private ServiceTracker<InstrumentationConsumer, Void> initTracker(final BundleContext bundleContext, final InstrumentationProvider instrumentationProvider) {
        return new ServiceTracker<>(bundleContext, InstrumentationConsumer.class, new ServiceTrackerCustomizer<InstrumentationConsumer, Void>() {
            @Override
            public Void addingService(ServiceReference<InstrumentationConsumer> serviceReference) {
                try {
                    log.trace("addingService called");
                    final InstrumentationConsumer consumer = bundleContext.getService(serviceReference);
                    log.debug("Consumer: {}", consumer);
                    if (consumer != null) {
                        final Instrumentation instrumentation;
                        try {
                            instrumentation = instrumentationProvider.getInstrumentation();
                            consumer.applyInstrumentation(instrumentation);
                        } catch (InstrumentationAgentException e) {
                            log.error("Error on getting insrumentation", e);
                        }
                    }
                } catch (Throwable t) {
                    log.error("Error on 'addingService'", t);
                }
                return null;
            }

            @Override
            public void modifiedService(ServiceReference<InstrumentationConsumer> serviceReference, Void aVoid) {

            }

            @Override
            public void removedService(ServiceReference<InstrumentationConsumer> serviceReference, Void aVoid) {

            }
        });
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.agentInstaller.install();
        this.serviceTracker.open();
    }

    @Override
    public void destroy() throws Exception {
        this.serviceTracker.close();
    }
}
