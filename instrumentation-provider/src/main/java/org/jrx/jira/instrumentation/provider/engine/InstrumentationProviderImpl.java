package org.jrx.jira.instrumentation.provider.engine;

import org.jrx.jira.instrumentation.provider.api.InstrumentationAgentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;

/**
 * Created by dzharikhin (https://github.com/dzharikhin) on 27.10.2016.
 */
@Component
public class InstrumentationProviderImpl implements InstrumentationProvider {

    private static final Logger log = LoggerFactory.getLogger(InstrumentationProviderImpl.class);

    @Override
    public Instrumentation getInstrumentation() throws InstrumentationAgentException {
        try {
            final Class<?> agentClass = ClassLoader.getSystemClassLoader().loadClass(INSTRUMENTATION_CLASS_NAME);
            log.debug("Agent class loaded from system classloader", agentClass);
            final Field instrumentation = agentClass.getDeclaredField(INSTRUMENTATION_FIELD_NAME);
            log.debug("Instrumentation field: {}", instrumentation);
            final Object instrumentationValue = instrumentation.get(null);
            if (instrumentationValue == null) {
                throw new NullPointerException("instrumentation data is null. Seems agent is not installed");
            }
            return (Instrumentation) instrumentationValue;
        } catch (Throwable e) {
            String msg = "Error getting instrumentation";
            log.error(msg, e);
            throw new InstrumentationAgentException("Error getting instrumentation", e);
        }
    }
}
