package org.jrx.jira.instrumentation;

import java.lang.instrument.Instrumentation;

/**
 * Created by dzharikhin (https://github.com/dzharikhin) on 26.10.2016.
 */
public class InstrumentationSupplierAgent {

    public static volatile Instrumentation instrumentation;

    public static void agentmain(String args, Instrumentation inst) throws Exception {
        System.out.println("==**agent started**==");
        InstrumentationSupplierAgent.instrumentation = inst;
        System.out.println("==**agent execution complete**==");
    }
}
