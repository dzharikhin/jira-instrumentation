package org.jrx.jira.instrumentation.provider.api;

/**
 * Created by dzharikhin (https://github.com/dzharikhin) on 27.10.2016.
 */
public class InstrumentationAgentException extends Exception {
    public InstrumentationAgentException(String message, Throwable cause) {
        super(message, cause, true, false);
    }
}
