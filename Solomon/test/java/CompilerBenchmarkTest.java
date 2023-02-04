package ru.yandex.solomon.expression;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.simple.SimpleLogger;

import ru.yandex.devtools.test.Metrics;
import ru.yandex.solomon.expression.CompilerCanonSupport.AlertProgram;

/**
 * @author Ivan Tsybulin
 */
@ParametersAreNonnullByDefault
public class CompilerBenchmarkTest {

    private List<AlertProgram> canonicalSources;

    @Before
    public void setUp() throws IOException {
        System.setProperty(SimpleLogger.LOG_KEY_PREFIX + "ru.yandex", "info");
        Locale.setDefault(Locale.ROOT); // to_fixed is locale-sensitive

        String prefix = "classpath:";

        canonicalSources = CompilerCanonSupport.readAlertPrograms(prefix + CompilerCanonSupport.ALERT_PROGRAMS);
    }

    private static class SimpleStats {
        private int count = 0;
        private double mean = 0d;
        private double M2 = 0d;

        void accept(double value) {
            // https://en.wikipedia.org/wiki/Algorithms_for_calculating_variance#Welford's_online_algorithm
            count++;
            double delta = value - mean;
            mean += delta / count;
            double deltaNew = value - mean;
            M2 += delta * deltaNew;
        }

        double getMean() {
            return mean;
        }

        double getStd() {
            return Math.sqrt(M2 / (count - 1));
        }
    }

    @Test
    public void benchmarkCompile() {
        int each = 5;
        List<AlertProgram> sample = new ArrayList<>((canonicalSources.size() + each - 1) / each);

        for (int i = 0; i < canonicalSources.size(); i += 5) {
            sample.add(canonicalSources.get(i));
        }

        // Warmup
        for (int i = 0; i < 2; i++) {
            CompilerCanonSupport.compileSources(sample);
        }

        final int ITERATIONS = 5;
        SimpleStats statistics = new SimpleStats();
        for (int i = 0; i < ITERATIONS; i++) {
            long startNanos = System.nanoTime();
            CompilerCanonSupport.compileSources(sample);
            statistics.accept(System.nanoTime() - startNanos);
        }

        Metrics.set("timeMicrosAvg", (int)(statistics.getMean() / TimeUnit.MICROSECONDS.toNanos(1)));
        Metrics.set("timeMicrosStd", (int)(statistics.getStd() / TimeUnit.MICROSECONDS.toNanos(1)));
    }
}
