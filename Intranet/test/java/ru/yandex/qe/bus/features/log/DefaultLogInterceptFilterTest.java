package ru.yandex.qe.bus.features.log;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.core.OutputStreamAppender;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import ru.yandex.qe.logging.security.PrivateHeaderSecurityGuard;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DefaultLogInterceptFilterTest {
    private static ByteArrayOutputStream logOutStream;

    @Test
    public void testDefaultFilter() {
        LogFeature logFeature = new LogFeature("testApp", Collections.<PrivateHeaderSecurityGuard>emptyList());
        final DefaultLogInterceptFilter defaultLogInterceptFilter = new DefaultLogInterceptFilter();
        defaultLogInterceptFilter.setIgnoredUserAgents(Arrays.asList("nagios-plugins"));
        HashSet<LogInterceptFilter> filters = Sets.<LogInterceptFilter>newHashSet(defaultLogInterceptFilter);

        OnReceiveInLogInterceptor onReceiveInLogInterceptor = new OnReceiveInLogInterceptor(logFeature, filters);
        PostLogicOutLogInterceptor postLogicOutLogInterceptor = new PostLogicOutLogInterceptor(logFeature, filters);

        Message msg = new MessageImpl();
        msg.setExchange(new ExchangeImpl());

        Map<String, List<String>> protocolHeaders = Maps.newHashMap(ImmutableMap.of(
                DefaultLogInterceptFilter.USER_AGENT,
                Arrays.asList("check_http/v1.4.15 (nagios-plugins 1.4.15)")));
        msg.put(Message.PROTOCOL_HEADERS, protocolHeaders);
        msg.put(Message.REQUEST_URL, "http://127.0.0.1:8080/api/version");
        assertFalse(msg.getExchange().containsKey(LogInterceptFilter.SKIP_LOGGING));

        configureLogInterceptor();

        onReceiveInLogInterceptor.handleMessage(msg);

        assertNotLogged();
        assertTrue(msg.getExchange().containsKey(LogInterceptFilter.SKIP_LOGGING));

        postLogicOutLogInterceptor.handleMessage(msg);
        assertNotLogged();
    }

    public static ByteArrayOutputStream configureLogInterceptor() {
        logOutStream = new ByteArrayOutputStream(4096);

        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(loggerContext);
        encoder.setPattern("%-4relative [%thread] %-5level %logger{35} - %msg%n");
        encoder.start();

        OutputStreamAppender interceptingAppender = new OutputStreamAppender();
        interceptingAppender.setEncoder(encoder);
        interceptingAppender.setOutputStream(logOutStream);

        interceptingAppender.setContext(loggerContext);

        interceptingAppender.start();

        Logger logbackLogger = loggerContext.getLogger("ru.yandex.qe.bus.features.log.OnReceiveInLogInterceptor");
        logbackLogger.addAppender(interceptingAppender);

        return logOutStream;
    }

    public static void assertNotLogged() {
        String fullLog = logOutStream.toString();

        assertTrue(fullLog.isEmpty());
    }

}