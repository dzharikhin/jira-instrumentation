package org.jrx.jira.instrumentation.validation.transform.issueservice;

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
 * Created by dzharikhin (https://github.com/dzharikhin) on 27.10.2016.
 */
@Component
@ExportAsService
public class DefaultIssueServiceTransformer implements InstrumentationConsumer {

    private static final Logger log = LoggerFactory.getLogger(DefaultIssueServiceTransformer.class);
    private static final AgentBuilder.Listener listener = new LogTransformListener(log);
    private final String DEFAULT_ISSUE_SERVICE_CLASS_NAME = "com.atlassian.jira.bc.issue.DefaultIssueService";

    @Override
    public void applyInstrumentation(Instrumentation instrumentation) {
        new AgentBuilder.Default().disableClassFormatChanges()
            .with(new AgentBuilder.Listener.Filtering(
                new StringMatcher(DEFAULT_ISSUE_SERVICE_CLASS_NAME, EQUALS_FULLY),
                listener
            ))
            .with(AgentBuilder.TypeStrategy.Default.REDEFINE)
            .with(AgentBuilder.RedefinitionStrategy.REDEFINITION)
            .with(AgentBuilder.InitializationStrategy.NoOp.INSTANCE)
            .type(named(DEFAULT_ISSUE_SERVICE_CLASS_NAME))
            .transform((builder, typeDescription, classloader) ->
                    builder
                //transformation is idempotent!!! You can call it many times with same effect
                //no way to add advice on advice if it applies to original class
                //https://github.com/raphw/byte-buddy/issues/206
                .visit(Advice.to(DefaultIssueServiceValidateCreateAdvice.class).on(named("validateCreate").and(ElementMatchers.isPublic())))
                .visit(Advice.to(DefaultIssueServiceValidateUpdateAdvice.class).on(named("validateUpdate").and(ElementMatchers.isPublic()))))
            .installOn(instrumentation);
    }
}
