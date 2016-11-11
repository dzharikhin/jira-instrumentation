package org.jrx.jira.instrumentation.provider.engine;

import org.jrx.jira.instrumentation.provider.api.InstrumentationAgentException;

import java.lang.instrument.Instrumentation;

/**
 * Created by dzharikhin (https://github.com/dzharikhin) on 27.10.2016.
 */
public interface InstrumentationProvider {
    String INSTRUMENTATION_CLASS_NAME = "org.jrx.jira.instrumentation.InstrumentationSupplierAgent";
    String INSTRUMENTATION_FIELD_NAME = "instrumentation";
    Instrumentation getInstrumentation() throws InstrumentationAgentException;

}
