package ru.yandex.solomon.alert.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.RandomStringUtils;

import ru.yandex.monlib.metrics.labels.Labels;
import ru.yandex.solomon.alert.EvaluationStatus;
import ru.yandex.solomon.alert.domain.expression.ExpressionAlert;
import ru.yandex.solomon.alert.domain.template.AlertFromTemplatePersistent;
import ru.yandex.solomon.alert.domain.template.AlertParameter;
import ru.yandex.solomon.alert.domain.threshold.Compare;
import ru.yandex.solomon.alert.domain.threshold.FluentPredicate;
import ru.yandex.solomon.alert.domain.threshold.PredicateRule;
import ru.yandex.solomon.alert.domain.threshold.TargetStatus;
import ru.yandex.solomon.alert.domain.threshold.ThresholdAlert;
import ru.yandex.solomon.alert.domain.threshold.ThresholdType;
import ru.yandex.solomon.alert.template.domain.AlertTemplate;
import ru.yandex.solomon.alert.template.domain.AlertTemplateParameter;
import ru.yandex.solomon.alert.template.domain.expression.ExpressionAlertTemplate;
import ru.yandex.solomon.alert.template.domain.threshold.TemplatePredicateRule;
import ru.yandex.solomon.alert.template.domain.threshold.ThresholdAlertTemplate;
import ru.yandex.solomon.alert.unroll.MultiAlertUtils;

/**
 * @author Vladimir Gordiychuk
 */
public final class AlertTestSupport {
    private AlertTestSupport() {
    }

    public static Alert randomAlert() {
        return randomAlert(ThreadLocalRandom.current());
    }

    public static Alert randomActiveAlert() {
        return randomAlert()
                .toBuilder()
                .setState(AlertState.ACTIVE)
                .build();
    }

    public static Alert randomAlert(ThreadLocalRandom random) {
        return randomAlert(random, true);
    }

    public static Alert randomAlert(ThreadLocalRandom random, boolean template) {
        int i = random.nextInt(template ? 3 : 2);
        return switch (i) {
            case 0 -> randomThresholdAlert(random);
            case 1 -> randomExpressionAlert(random);
            default -> alertFromTemplatePersistent(random);
        };
    }

    public static ThresholdAlert randomThresholdAlert() {
        return randomThresholdAlert(ThreadLocalRandom.current());
    }

    public static ThresholdAlert randomThresholdAlert(ThreadLocalRandom random) {
        return ThresholdAlert.newBuilder()
                .setId(UUID.randomUUID().toString())
                .setName("Name with random: " + random.nextInt())
                .setProjectId("junk")
                .setFolderId("myfolder")
                .setVersion(random.nextInt(0, 1000))
                .setPredicateRule(randomAlarmRule(random))
                .setPeriod(Duration.ofMillis(random.nextLong(1000, 1_000_00)))
                .setSelectors("project=solomon, cluster=local, service=test, sensor=idleTime, host=solomon-" + random.nextInt(1, 100))
                .setCreatedBy(randomUser(random))
                .setUpdatedAt(System.currentTimeMillis() + random.nextLong(0, 1_000_000))
                .setUpdatedBy(randomUser(random))
                .setCreatedAt(System.currentTimeMillis() - random.nextLong(0, 1_000_000))
                .setState(AlertState.values()[random.nextInt(0, AlertState.values().length)])
                .setSeverity(AlertSeverity.values()[random.nextInt(0, AlertSeverity.values().length)])
                .setNotificationChannel("notify-" + UUID.randomUUID())
                .setEscalation(UUID.randomUUID().toString())
                .setGroupByLabels(random.nextBoolean() ? Collections.emptyList() : Collections.singleton("host"))
                .setAnnotations(randomAnnotations(random))
                .setServiceProviderAnnotations(randomAnnotations(random))
                .setLabels(Map.of("label1", "value1"))
                .build();
    }


    public static AlertFromTemplatePersistent alertFromTemplatePersistent(ThreadLocalRandom random) {
        return alertFromTemplatePersistent(random, random.nextBoolean());
    }

    public static AlertFromTemplatePersistent alertFromTemplatePersistent(ThreadLocalRandom random, boolean type) {
        return AlertFromTemplatePersistent.newBuilder()
                .setId(UUID.randomUUID().toString())
                .setName("Name with random: " + random.nextInt())
                .setProjectId("junk")
                .setFolderId("myfolder")
                .setVersion(random.nextInt(0, 1000))
                .setCreatedBy(randomUser(random))
                .setUpdatedAt(System.currentTimeMillis() + random.nextLong(0, 1_000_000))
                .setUpdatedBy(randomUser(random))
                .setCreatedAt(System.currentTimeMillis() - random.nextLong(0, 1_000_000))
                .setState(AlertState.values()[random.nextInt(0, AlertState.values().length)])
                .setNotificationChannel("notify-" + UUID.randomUUID())
                .setEscalation(UUID.randomUUID().toString())
                .setTemplateId("template " + type)
                .setTemplateVersionTag("templateVersionTag " + random.nextInt())
                .setServiceProvider("service1")
                .setParameters(getParameters())
                .setThresholds(getThresholds())
                .setServiceProviderAnnotations(randomAnnotations(random))
                .setAnnotations(randomAnnotations(random))
                .setLabels(Map.of("label1", "value1"))
                .build();
    }

    private static List<AlertParameter> getThresholds() {
        return List.of(
                new AlertParameter.TextParameterValue("text", "name"),
                new AlertParameter.IntegerParameterValue(1, "name"),
                new AlertParameter.DoubleParameterValue(1.1, "name")
        );
    }

    private static List<AlertParameter> getParameters() {
        return List.of(
                new AlertParameter.TextListParameterValue(List.of("t1", "t2"), "p3"),
                new AlertParameter.LabelListParameterValue(List.of("v1"), "p4")
        );
    }

    public static ThresholdAlert randomCloudThresholdAlert() {
        return randomCloudThresholdAlert(ThreadLocalRandom.current());
    }

    private static final ThresholdType[] CLOUD_ALERT_AGGREGATIONS = new ThresholdType[] {
        ThresholdType.AVG, ThresholdType.MAX, ThresholdType.MIN, ThresholdType.LAST_NON_NAN,
        ThresholdType.SUM, ThresholdType.COUNT
    };

    public static ThresholdAlert randomCloudThresholdAlert(ThreadLocalRandom random) {
        ThresholdType aggr = CLOUD_ALERT_AGGREGATIONS[random.nextInt(0, CLOUD_ALERT_AGGREGATIONS.length)];
        Compare comp = Compare.values()[random.nextInt(0, Compare.values().length)];
        double alarmLevel = 100 * random.nextDouble();
        Double warnLevel = switch (comp) {
            case EQ -> 100 * random.nextDouble();
            case GT, GTE -> 0.75 * alarmLevel;
            case LT, LTE -> 1.5 * alarmLevel;
            case NE  -> null;
        };
        PredicateRule alarmRule = FluentPredicate.when(aggr).is(comp).than(alarmLevel).signal(TargetStatus.ALARM);
        PredicateRule warnRule = (random.nextBoolean() || warnLevel == null) ? null :
            FluentPredicate.when(aggr).is(comp).than(warnLevel).signal(TargetStatus.WARN);
        Stream<PredicateRule> rules = warnRule != null ? Stream.of(alarmRule, warnRule) : Stream.of(alarmRule);
        return randomThresholdAlert(random).toBuilder()
            .setPredicateRules(rules)
            .build();
    }


    private static PredicateRule randomAlarmRule(ThreadLocalRandom random) {
        return PredicateRule.onThreshold(random.nextDouble())
                .withThresholdType(ThresholdType.values()[random.nextInt(0, ThresholdType.values().length)])
                .withComparison(Compare.values()[random.nextInt(0, Compare.values().length)]);
    }

    private static PredicateRule randomRule(ThreadLocalRandom random) {
        return PredicateRule.onThreshold(random.nextDouble())
                .withThresholdType(ThresholdType.values()[random.nextInt(0, ThresholdType.values().length)])
                .withComparison(Compare.values()[random.nextInt(0, Compare.values().length)])
                .withTargetStatus(TargetStatus.values()[random.nextInt(0, TargetStatus.values().length)]);
    }

    public static ThresholdAlert randomThresholdAlertWithManyRules() {
        return randomThresholdAlertWithManyRules(ThreadLocalRandom.current());
    }

    public static ThresholdAlert randomThresholdAlertWithManyRules(ThreadLocalRandom random) {
        return ThresholdAlert.newBuilder()
                .setId(UUID.randomUUID().toString())
                .setName("Name with random: " + random.nextInt())
                .setProjectId("junk")
                .setFolderId("myfolder")
                .setVersion(random.nextInt(0, 1000))
                .setTransformations(IntStream.range(0, random.nextInt(2, 5))
                        .mapToObj((ignore) -> randomTransformation(random))
                        .collect(Collectors.joining(".")))
                .setPredicateRules(IntStream.range(0, random.nextInt(1, 5)).mapToObj((ignore) -> randomRule(random)))
                .setPeriod(Duration.ofMillis(random.nextLong(1000, 1_000_00)))
                .setSelectors("project=solomon, cluster=local, service=test, sensor=idleTime, host=solomon-" + random.nextInt(1, 100))
                .setCreatedBy(randomUser(random))
                .setUpdatedAt(System.currentTimeMillis() + random.nextLong(0, 1_000_000))
                .setUpdatedBy(randomUser(random))
                .setCreatedAt(System.currentTimeMillis() - random.nextLong(0, 1_000_000))
                .setState(AlertState.values()[random.nextInt(0, AlertState.values().length)])
                .setNotificationChannel("notify-" + UUID.randomUUID())
                .setEscalation(UUID.randomUUID().toString())
                .setGroupByLabels(random.nextBoolean() ? Collections.emptyList() : Collections.singleton("host"))
                .setAnnotations(randomAnnotations(random))
                .setServiceProviderAnnotations(randomAnnotations(random))
                .setSeverity(AlertSeverity.DISASTER)
                .setLabels(Map.of("label1", "value2"))
                .build();
    }

    public static ExpressionAlert randomExpressionAlert(ThreadLocalRandom random) {
        return ExpressionAlert.newBuilder()
                .setId(UUID.randomUUID().toString())
                .setName("Expression alert with random " + random.nextInt())
                .setProjectId("junk")
                .setFolderId("myfolder")
                .setVersion(random.nextInt(0, 1000))
                .setUpdatedBy(randomUser(random))
                .setUpdatedAt(System.currentTimeMillis() + random.nextLong(0, 1_000_000))
                .setCreatedBy(randomUser(random))
                .setCreatedAt(System.currentTimeMillis() - random.nextLong(0, 1_000_000))
                .setState(AlertState.values()[random.nextInt(0, AlertState.values().length)])
                .setPeriod(Duration.ofMillis(random.nextLong(1000, 1_000_00)))
                .setSeverity(AlertSeverity.values()[random.nextInt(0, AlertSeverity.values().length)])
                .setProgram("let rr = random01() < " + random.nextDouble(1, 10) + ";")
                .setCheckExpression("rr")
                .setGroupByLabels(random.nextBoolean() ? Collections.emptyList() : Collections.singleton("host"))
                .setNotificationChannel("notify-" + UUID.randomUUID())
                .setEscalation(UUID.randomUUID().toString())
                .setAnnotations(randomAnnotations(random))
                .setServiceProviderAnnotations(randomAnnotations(random))
                .setLabels(Map.of("label1", "value2"))
                .build();
    }

    public static Alert randomAlertWithConfiguredChannels() {
        return randomAlertWithConfiguredChannels(ThreadLocalRandom.current());
    }

    public static Alert randomAlertWithConfiguredChannels(ThreadLocalRandom random) {
        int size = random.nextInt(1, 5);
        Map<String, ChannelConfig> channels = new HashMap<>(size);

        Set<String> escalations = new HashSet<>();
        for (int i = 0; i < size; i++) {
            if (random.nextBoolean()) {
                channels.put("default-channel-" + UUID.randomUUID(), ChannelConfig.EMPTY);
            } else {
                channels.put("configured-channel-" + UUID.randomUUID(), new ChannelConfig(
                    Arrays.stream(EvaluationStatus.Code.values()).filter(ignore -> random.nextBoolean()).collect(Collectors.toSet()),
                    Duration.ofMinutes(random.nextInt(0, 30))
                ));
            }
            escalations.add(UUID.randomUUID().toString());
        }

        return randomAlert().toBuilder()
            .setNotificationChannels(channels)
            .setEscalations(escalations)
            .build();
    }

    public static SubAlert randomSubAlert(ThreadLocalRandom random) {
        Labels labels = Labels.of("host", "solomon-" + random.nextInt(1, 100));
        Alert parent = randomAlert(random)
                .toBuilder()
                .setGroupByLabel("host")
                .build();

        return SubAlert.newBuilder()
                .setId(MultiAlertUtils.getAlertId(parent, labels))
                .setParent(parent)
                .setGroupKey(labels)
                .build();
    }

    public static String randomUser(ThreadLocalRandom random) {
        return switch (random.nextInt(5)) {
            case 0 -> "";
            case 1 -> "Catalina Daniels";
            case 2 -> "Patricia Chavez";
            default -> RandomStringUtils.randomAlphanumeric(10);
        };
    }

    public static String randomTransformation(ThreadLocalRandom random) {
        return switch (random.nextInt(5)) {
            case 0 -> "derivative";
            case 1 -> "intergrate";
            case 2 -> "percentille(95)";
            case 3 -> "group_lines";
            default -> RandomStringUtils.randomAlphanumeric(8);
        };
    }

    public static Map<String, String> randomAnnotations(ThreadLocalRandom random) {
        if (random.nextBoolean()) {
            return ImmutableMap.of();
        } else {
            return ImmutableMap.of("summary", "{{#isAlarm}}Everything broken!{{/isAlarm}}{{#isOk}}Good night!{{/isOk}}");
        }
    }

    public static AlertTemplate thresholdTemplate() {
        return ThresholdAlertTemplate.newBuilder()
                .setId("id")
                .setTemplateVersionTag("tag")
                .setServiceProviderId("service provider")
                .setPeriodMillis(1000)
                .setDelaySeconds(2000)
                .setName("name")
                .setDescription("descr")
                .setUpdatedAt(Instant.now())
                .setUpdatedAt(Instant.now())
                .setCreatedBy("user")
                .setUpdatedBy("user")
                .setAnnotations(Map.of("a", "b"))
                .setLabels(Map.of("c", "d"))
                .setSelectors("{{templateParameter.paramA}}={{templateParameter.paramB}}, {{templateParameter.paramC}}={{templateParameter.paramK}}")
                .setTransformations("{{templateParameter.paramI}}={{templateParameter.paramJ}}")
                .setPredicateRules(Stream.of(
                        TemplatePredicateRule.onThreshold(10)
                                .withThresholdType(ThresholdType.AVG)
                                .withComparison(Compare.GT)
                                .withTargetStatus(TargetStatus.ALARM),
                        TemplatePredicateRule.onThreshold(10)
                                .onThresholdParameter("{{templateParameter.thresholdOnly}}")
                                .withThresholdType(ThresholdType.AT_ALL_TIMES)
                                .withComparison(Compare.EQ)
                                .withTargetStatus(TargetStatus.OK),
                        TemplatePredicateRule.onThreshold(10)
                                .onThresholdParameter("{{templateParameter.justDefaultValue}}")
                                .withThresholdType(ThresholdType.AT_ALL_TIMES)
                                .withComparison(Compare.EQ)
                                .withTargetStatus(TargetStatus.OK),
                        TemplatePredicateRule.onThreshold(10)
                                .onThresholdParameter("abcd")
                                .withThresholdType(ThresholdType.AT_ALL_TIMES)
                                .withComparison(Compare.EQ)
                                .withTargetStatus(TargetStatus.OK)
                ))
                .setGroupByLabels(List.of("host", "abc_{{templateParameter.paramD}} {{skippedParam}}"))
                .setResolvedEmptyPolicy(ResolvedEmptyPolicy.ALARM)
                .setNoPointsPolicy(NoPointsPolicy.MANUAL)
                .setAlertSeverity(AlertSeverity.DISASTER)
                .setParameters(getParameterParams())
                .setThresholds(getThresholdParams())
                .setDefaultTemplate(true)
                .build();
    }

    public static AlertTemplate expressionTemplate() {
        return ExpressionAlertTemplate.newBuilder()
                .setId("id")
                .setTemplateVersionTag("tag")
                .setServiceProviderId("service provider")
                .setPeriodMillis(1000)
                .setDelaySeconds(2000)
                .setName("name")
                .setDescription("descr")
                .setUpdatedAt(Instant.now())
                .setUpdatedAt(Instant.now())
                .setCreatedBy("user")
                .setUpdatedBy("user")
                .setAnnotations(Map.of("a", "b"))
                .setLabels(Map.of("c", "d"))
                .setProgram("let {{templateParameter.paramA}}_{{templateParameter.paramB}} = random01() < {{templateParameter.paramC}}; {{alert.projectId}}")
                .setGroupByLabels(List.of("host", "abc_{{templateParameter.paramD}} {{templateParameter.skippedParam}}"))
                .setResolvedEmptyPolicy(ResolvedEmptyPolicy.ALARM)
                .setNoPointsPolicy(NoPointsPolicy.MANUAL)
                .setAlertSeverity(AlertSeverity.DISASTER)
                .setParameters(getParameterParams())
                .setThresholds(getThresholdParams())
                .setDefaultTemplate(true)
                .build();
    }

    private static List<AlertTemplateParameter> getThresholdParams() {
        return List.of(
                new AlertTemplateParameter.TextParameterValue("paramAValueDefault", "paramA", "title", "descr"),
                new AlertTemplateParameter.TextParameterValue("paramFValueDefault", "paramF", "title", "descr"),
                new AlertTemplateParameter.TextListParameterValue(List.of("paramGValue1Default", "paramGValue2Default"), "paramG", "title", "descr"),
                new AlertTemplateParameter.IntegerParameterValue(2, "paramI", "title", "descr", "UNIT_FORMAT_UNSPECIFIED"),
                new AlertTemplateParameter.DoubleParameterValue(2.2, "paramJ", "title", "descr", "UNIT_FORMAT_UNSPECIFIED"),
                new AlertTemplateParameter.LabelListParameterValue("", "", List.of("paramKValue1Default", "paramKValue2Default"), "paramK", "title", "descr", "", false),
                new AlertTemplateParameter.IntegerParameterValue(100, "paramC", "title", "descr", "UNIT_FORMAT_UNSPECIFIED"),
                new AlertTemplateParameter.DoubleParameterValue(10.1, "paramD", "title", "descr", "UNIT_FORMAT_UNSPECIFIED"),
                new AlertTemplateParameter.DoubleParameterValue(23.22, "thresholdOnly", "title", "descr", "UNIT_FORMAT_UNSPECIFIED"),
                new AlertTemplateParameter.DoubleParameterValue(3.3, "justDefaultValue", "title", "descr", "UNIT_FORMAT_UNSPECIFIED")
        );
    }

    private static List<AlertTemplateParameter> getParameterParams() {
        return List.of(
                new AlertTemplateParameter.TextParameterValue("thresholdAValueDefault", "thresholdA", "title", "descr"),
                new AlertTemplateParameter.TextParameterValue("thresholdFValueDefault", "thresholdF", "title", "descr"),
                new AlertTemplateParameter.TextListParameterValue(List.of("thresholdGValue1Default", "thresholdGValue2Default"), "thresholdG", "title", "descr"),
                new AlertTemplateParameter.IntegerParameterValue(2, "thresholdI", "title", "descr", "UNIT_FORMAT_UNSPECIFIED"),
                new AlertTemplateParameter.DoubleParameterValue(2.2, "thresholdJ", "title", "descr", "UNIT_FORMAT_UNSPECIFIED"),
                new AlertTemplateParameter.LabelListParameterValue("", "", List.of("thresholdKValue1Default", "thresholdKValue2Default"), "thresholdK", "title", "descr", "", false),
                new AlertTemplateParameter.IntegerParameterValue(100, "thresholdC", "title", "descr", "UNIT_FORMAT_UNSPECIFIED"),
                new AlertTemplateParameter.DoubleParameterValue(10.1, "thresholdD", "title", "descr", "UNIT_FORMAT_UNSPECIFIED"),
                new AlertTemplateParameter.DoubleParameterValue(23.22, "1thresholdOnly", "title", "descr", "UNIT_FORMAT_UNSPECIFIED"),
                new AlertTemplateParameter.DoubleParameterValue(3.3, "2justDefaultValue", "title", "descr", "UNIT_FORMAT_UNSPECIFIED")
        );
    }
}
