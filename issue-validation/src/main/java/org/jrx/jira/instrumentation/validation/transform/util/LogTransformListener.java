package org.jrx.jira.instrumentation.validation.transform.util;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.utility.JavaModule;
import org.slf4j.Logger;

/**
 * Created by dzharikhin (https://github.com/dzharikhin) on 02.11.2016.
 */
public class LogTransformListener implements AgentBuilder.Listener {

    private final Logger log;

    public LogTransformListener(Logger log) {
        this.log = log;
    }

    @Override
    public void onTransformation(TypeDescription typeDescription, ClassLoader classLoader, JavaModule javaModule, DynamicType dynamicType) {
        log.debug(
                "Transformed type {}, dynamicType={}, with classloader={}",
                typeDescription,
                dynamicType,
                classLoader
        );
    }

    @Override
    public void onIgnored(TypeDescription typeDescription, ClassLoader classLoader, JavaModule javaModule) {

    }

    @Override
    public void onError(String s, ClassLoader classLoader, JavaModule javaModule, Throwable throwable) {
        log.warn("Error on class {} transform. Classloader: {}", s, classLoader, throwable);
    }

    @Override
    public void onComplete(String s, ClassLoader classLoader, JavaModule javaModule) {
    }
}
