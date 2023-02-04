package ru.yandex.qe.logging.turbo;

import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.event.Level;

/**
 * @author intr13
 */
public class MDCLogThresholdFilterIntegrationTest {

    @Test
    public void enable_filter() throws Exception {
        calculate(createLogger("/mdc-log-threshold-filter-enable.xml"));
    }

    @Test
    public void enable_filter_with_threshold() throws Exception {
        Logger logger = createLogger("/mdc-log-threshold-filter-enable.xml");
        MDC.put(MDCLogThresholdFilter.MDC_THRESHOLD_KEY, Level.DEBUG.name());
        try {
            calculate(logger);
        } finally {
            MDC.remove(MDCLogThresholdFilter.MDC_THRESHOLD_KEY);
        }
    }

    @Test
    public void enable_filter_with_threshold_and_pattern() throws Exception {
        Logger logger = createLogger("/mdc-log-threshold-filter-enable.xml");
        MDC.put(MDCLogThresholdFilter.MDC_THRESHOLD_KEY, Level.DEBUG.name());
        MDC.put(MDCLogThresholdFilter.MDC_PATTERN_KEY, MDCLogThresholdFilterIntegrationTest.class.getSimpleName());
        try {
            calculate(logger);
        } finally {
            MDC.remove(MDCLogThresholdFilter.MDC_THRESHOLD_KEY);
            MDC.remove(MDCLogThresholdFilter.MDC_PATTERN_KEY);
        }
    }

    @Test
    public void disable_filter() throws Exception {
        calculate(createLogger("/mdc-log-threshold-filter-disable.xml"));
    }

    private void calculate(Logger logger) {
        for (int j = 0; j < 1; j++) {
            long time = System.nanoTime();
            for (int i = 1_000_000; i < 2_000_000; i++) {
                logger.info(Integer.toString(i));
            }
            time = System.nanoTime() - time;
            System.out.print(new DecimalFormat("#,###,##0").format(time / 1000 / 1000) + " ms; ");
        }
    }

    private Logger createLogger(String file) throws IOException, JoranException {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        loggerContext.reset();
        JoranConfigurator configurator = new JoranConfigurator();
        try (InputStream configStream = getClass().getResourceAsStream(file)) {
            configurator.setContext(loggerContext);
            configurator.doConfigure(configStream);
        }
        return loggerContext.getLogger(MDCLogThresholdFilterIntegrationTest.class);
    }
}
