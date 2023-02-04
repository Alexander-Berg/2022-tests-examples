package ru.yandex.solomon.alert;

import java.io.IOException;
import java.time.Instant;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.logging.log4j.core.LoggerContext;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.bolts.collection.Try;
import ru.yandex.monlib.metrics.labels.validate.InvalidLabelException;
import ru.yandex.monlib.metrics.labels.validate.LabelsValidator;
import ru.yandex.solomon.alert.canon.Check;
import ru.yandex.solomon.alert.canon.Explainer;
import ru.yandex.solomon.alert.canon.IgnoredTests;
import ru.yandex.solomon.alert.canon.ReplayerCanonSupport;
import ru.yandex.solomon.alert.canon.ResultMatcher;
import ru.yandex.solomon.alert.canon.protobuf.ArchiveConverterImpl;
import ru.yandex.solomon.alert.domain.Alert;
import ru.yandex.solomon.alert.domain.AlertKey;
import ru.yandex.solomon.alert.domain.AlertType;
import ru.yandex.solomon.alert.domain.SubAlert;
import ru.yandex.solomon.alert.domain.expression.ExpressionAlert;
import ru.yandex.solomon.alerting.canon.protobuf.TExplainResult;
import ru.yandex.solomon.config.protobuf.ELevel;
import ru.yandex.solomon.config.protobuf.TLogger;
import ru.yandex.solomon.config.protobuf.TLoggingConfig;
import ru.yandex.solomon.main.logger.LoggerConfigurationUtils;
import ru.yandex.solomon.metrics.client.ReplayMetricsClient;

/**
 * @author Ivan Tsybulin
 */
@ParametersAreNonnullByDefault
@RunWith(Parameterized.class)
public class ParametrizedReplayTest {
    @Rule
    public TestRule timeout = new DisableOnDebug(Timeout.builder()
            .withLookingForStuckThread(true)
            .withTimeout(15, TimeUnit.SECONDS)
            .build());

    @Parameterized.Parameter
    public AlertKey alertKey;

    @Parameterized.Parameter(value = 1)
    public Try<Alert> maybeAlert;

    @Parameterized.Parameter(value = 2)
    public Instant when;

    @Parameterized.Parameter(value = 3)
    public Explainer explainer;

    @Parameterized.Parameter(value = 4)
    public TExplainResult expected;

    static {
        try {
            LabelsValidator.checkKeyValid("foo-bar");
        } catch (InvalidLabelException e) {
            Assert.fail("Run with -Dru.yandex.solomon.LabelValidator=skip");
        }
    }

    private static void ensureLoggingConfiguredOnce() {
        var ctx = LoggerContext.getContext(false);
        var conf = ctx.getConfiguration();
        if (conf.getAppenders().size() == 1) {
            LoggerConfigurationUtils.configureLogger(TLoggingConfig.newBuilder()
                    .addLoggers(TLogger.newBuilder().setName("root").setLevel(ELevel.ERROR).build())
                    .addLoggers(TLogger.newBuilder().setName(ResultMatcher.class.getName()).setLevel(ELevel.INFO).build())
                    .build());
        }
    }

    @Parameterized.Parameters(name = "{index}: alertKey={0}")
    public static Collection<Object[]> data() throws IOException {
        ensureLoggingConfiguredOnce();

        String prefix = "classpath:";

        var capture = ReplayerCanonSupport.readMetricsClientCapture(prefix);
        var canonResults = ReplayerCanonSupport.readCanonicalResults(prefix);

        Explainer explainer = new Explainer(new ReplayMetricsClient(capture, ArchiveConverterImpl.I, true));
        Instant when = canonResults.evaluationMoment;

        return canonResults.alertEvaluationRecords.stream()
                .map(record -> {
                    var key = ReplayerCanonSupport.keyFromRecord(record);
                    var alert = Try.tryCatchException(() -> ReplayerCanonSupport.extractAlert(record));
                    return new Object[] {key, alert, when, explainer, record.getExplainResult()};
                })
                .collect(Collectors.toList());
    }

    @Test
    public void compareWithCanonResults() {
        System.out.println("new AlertKey(\"" + alertKey.getProjectId() + "\", \"" + alertKey.getParentId() + "\", \"" +
                alertKey.getAlertId() + "\"),");
        String id = alertKey.getParentId().isEmpty() ? alertKey.getAlertId() : alertKey.getParentId();
        String url = "https://solomon.yandex-team.ru/admin/projects/" + alertKey.getProjectId() + "/alerts/" + id;
        System.out.println(url);

        Check checkMethod = IgnoredTests.IGNORED_KEYS.getOrDefault(alertKey, Check.FULL_CHECK);
        Assume.assumeFalse(checkMethod.explanation(), checkMethod.method() == Check.Method.IGNORED);

        if (maybeAlert.isFailure()) {
            Throwable t = maybeAlert.getThrowable();
            t.printStackTrace();
            Assert.fail("Failed to extract alert");
        }

        Alert alert = maybeAlert.get();

        if (alert.getAlertType() == AlertType.EXPRESSION) {
            System.out.println(((ExpressionAlert) alert).getCombinedSource());
        }
        if (alert.getAlertType() == AlertType.SUB_ALERT) {
            Alert parent = ((SubAlert) alert).getParent();
            if (parent.getAlertType() == AlertType.EXPRESSION) {
                System.out.println(((ExpressionAlert) parent).getCombinedSource());
            }
        }
        var computed = ReplayerCanonSupport.evaluate(alert, explainer, when);
        var matcher = ResultMatcher.newBuilder()
                .strictSeriesCheck(false)
                .build();

        matcher.assertEqualsIgnoringStackTrace(expected, computed, ReplayerCanonSupport.computeFlappingAnnotationKeys(alert));
    }
}
