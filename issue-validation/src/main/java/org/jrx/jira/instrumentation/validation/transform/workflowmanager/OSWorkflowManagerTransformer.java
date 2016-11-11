package org.jrx.jira.instrumentation.validation.transform.workflowmanager;

import com.atlassian.plugin.spring.scanner.annotation.export.ExportAsService;
import org.jrx.jira.instrumentation.provider.api.InstrumentationConsumer;
import org.jrx.jira.instrumentation.validation.transform.util.LogTransformListener;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.matcher.StringMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.instrument.Instrumentation;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.StringMatcher.Mode.EQUALS_FULLY;

/**
 * Created by dzharikhin (https://github.com/dzharikhin) on 02.11.2016.
 */
@Component
@ExportAsService
public class OSWorkflowManagerTransformer implements InstrumentationConsumer {
    private static final Logger log = LoggerFactory.getLogger(OSWorkflowManagerTransformer.class);
    private static final AgentBuilder.Listener listener = new LogTransformListener(log);
    private final String OS_WORKFLOW_MANAGER_CLASS_NAME = "com.atlassian.jira.workflow.OSWorkflowManager";

    @Override
    public void applyInstrumentation(Instrumentation instrumentation) {
        new AgentBuilder.Default().disableClassFormatChanges()
                .with(new AgentBuilder.Listener.Filtering(
                    new StringMatcher(OS_WORKFLOW_MANAGER_CLASS_NAME, EQUALS_FULLY),
                    listener
                ))
                .with(AgentBuilder.TypeStrategy.Default.REDEFINE)
                .with(AgentBuilder.RedefinitionStrategy.REDEFINITION)
                .with(AgentBuilder.InitializationStrategy.NoOp.INSTANCE)
                .type(named(OS_WORKFLOW_MANAGER_CLASS_NAME))
                .transform((builder, typeDescription, classloader) ->
                    builder
                        //transformation is idempotent!!! You can call it many times with same effect
                        //no way to add advice on advice if it applies to original class
                        //https://github.com/raphw/byte-buddy/issues/206
                        .visit(Advice.to(OSWorkflowManagerCreateIssueAdvice.class).on(named("createIssue").and(ElementMatchers.isPublic()))))
                .installOn(instrumentation);
    }
}
