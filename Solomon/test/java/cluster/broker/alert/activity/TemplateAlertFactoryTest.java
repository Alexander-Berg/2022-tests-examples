package ru.yandex.solomon.alert.cluster.broker.alert.activity;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.solomon.alert.domain.Alert;
import ru.yandex.solomon.alert.domain.AlertSeverity;
import ru.yandex.solomon.alert.domain.AlertState;
import ru.yandex.solomon.alert.domain.NoPointsPolicy;
import ru.yandex.solomon.alert.domain.ResolvedEmptyPolicy;
import ru.yandex.solomon.alert.domain.expression.ExpressionAlert;
import ru.yandex.solomon.alert.domain.template.AlertFromTemplatePersistent;
import ru.yandex.solomon.alert.domain.template.AlertParameter;
import ru.yandex.solomon.alert.domain.threshold.Compare;
import ru.yandex.solomon.alert.domain.threshold.PredicateRule;
import ru.yandex.solomon.alert.domain.threshold.TargetStatus;
import ru.yandex.solomon.alert.domain.threshold.ThresholdAlert;
import ru.yandex.solomon.alert.domain.threshold.ThresholdType;
import ru.yandex.solomon.alert.template.MustacheTemplateFactory;
import ru.yandex.solomon.alert.template.domain.AlertTemplate;
import ru.yandex.solomon.alert.template.domain.AlertTemplateParameter;
import ru.yandex.solomon.alert.template.domain.expression.ExpressionAlertTemplate;
import ru.yandex.solomon.alert.template.domain.threshold.TemplatePredicateRule;
import ru.yandex.solomon.alert.template.domain.threshold.ThresholdAlertTemplate;
import ru.yandex.solomon.labels.query.Selectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Alexey Trushkin
 */
public class TemplateAlertFactoryTest {

    private TemplateAlertFactory factory;

    @Before
    public void setUp() throws Exception {
        factory = new TemplateAlertFactory(new MustacheTemplateFactory());
    }

    @Test
    public void createExpression() {
        var alertFromTemplatePersistent = alertFromTemplatePersistent();
        var template = expressionTemplate();
        ExpressionAlert alertFromTemplate = (ExpressionAlert) factory.createAlertFromTemplate(alertFromTemplatePersistent, template);

        assertBase(alertFromTemplatePersistent, alertFromTemplate, template);

        assertEquals(Duration.ofMillis(template.getPeriodMillis()), alertFromTemplate.getPeriod());
        assertEquals(template.getDelaySeconds(), alertFromTemplate.getDelaySeconds());
        assertEquals("let paramAValue_paramBValue1|paramBValue2 = random01() < 10; {{alert.projectId}}", alertFromTemplate.getProgram());
        assertEquals(List.of("host", "abc_1.1 {{templateParameter.skippedParam}}"), alertFromTemplate.getGroupByLabels());
        assertEquals(Map.of("paramFValueDefault", "paramGValue1Default|paramGValue2Default",
                "a", "paramEValue1|paramEValue2",
                "2", "2.2_paramKValue1Default|paramKValue2Default",
                "paramEValue1|paramEValue2", "paramFValueDefault"), alertFromTemplate.getAnnotations());
        assertEquals(Map.of("paramFValueDefault", "paramGValue1Default|paramGValue2Default",
                "aa", "paramEValue1|paramEValue2",
                "2", "2.2_paramKValue1Default|paramKValue2Default",
                "paramEValue1|paramEValue2", "paramFValueDefault"), alertFromTemplate.getServiceProviderAnnotations());
        assertEquals(ResolvedEmptyPolicy.ALARM, alertFromTemplate.getResolvedEmptyPolicy());
        assertEquals(NoPointsPolicy.MANUAL, alertFromTemplate.getNoPointsPolicy());
        assertEquals(AlertSeverity.CRITICAL, alertFromTemplate.getSeverity());
    }

    @Test
    public void createThreshold() {
        var alertFromTemplatePersistent = alertFromTemplatePersistent()
                .toBuilder()
                .setAnnotations(Map.of("a", "{{templateParameter.paramE}}", "{{templateParameter.paramF}}", "{{templateParameter.paramG}}"))
                .setServiceProviderAnnotations(Map.of("aa", "{{templateParameter.paramE}}", "{{templateParameter.paramF}}", "{{templateParameter.paramG}}"))
                .build();
        var template = thresholdTemplate();
        ThresholdAlert alertFromTemplate = (ThresholdAlert) factory.createAlertFromTemplate(alertFromTemplatePersistent, template);

        assertBase(alertFromTemplatePersistent, alertFromTemplate, template);

        assertEquals(Selectors.parse("{paramAValue='paramBValue1|paramBValue2', 10='paramKValue1Default|paramKValue2Default'}"), alertFromTemplate.getSelectors());
        assertEquals("2=2.2", alertFromTemplate.getTransformations());
        assertTrue(alertFromTemplate.getPredicateRules().contains(PredicateRule.onThreshold(10)
                .withThresholdType(ThresholdType.AVG)
                .withComparison(Compare.GT)
                .withTargetStatus(TargetStatus.ALARM)));
        assertTrue(alertFromTemplate.getPredicateRules().contains(PredicateRule.onThreshold(2.22)
                .withThresholdType(ThresholdType.AT_ALL_TIMES)
                .withComparison(Compare.EQ)
                .withTargetStatus(TargetStatus.OK)));
        assertTrue(alertFromTemplate.getPredicateRules().contains(PredicateRule.onThreshold(3.3)
                .withThresholdType(ThresholdType.AT_ALL_TIMES)
                .withComparison(Compare.EQ)
                .withTargetStatus(TargetStatus.OK)));
        assertTrue(alertFromTemplate.getPredicateRules().contains(PredicateRule.onThreshold(10)
                .withThresholdType(ThresholdType.AT_ALL_TIMES)
                .withComparison(Compare.EQ)
                .withTargetStatus(TargetStatus.OK)));
        assertEquals(List.of("host", "abc_1.1 {{skippedParam}}"), alertFromTemplate.getGroupByLabels());
        assertEquals(Map.of("paramFValueDefault", "paramGValue1Default|paramGValue2Default",
                "a", "paramEValue1|paramEValue2"), alertFromTemplate.getAnnotations());
        assertEquals(Map.of("paramFValueDefault", "paramGValue1Default|paramGValue2Default",
                "aa", "paramEValue1|paramEValue2"), alertFromTemplate.getServiceProviderAnnotations());
    }

    private void assertBase(AlertFromTemplatePersistent alertFromTemplatePersistent, Alert alertFromTemplate, AlertTemplate template) {
        assertTrue(alertFromTemplate.isObtainedFromTemplate());
        assertEquals(alertFromTemplatePersistent.getId(), alertFromTemplate.getId());
        assertEquals(alertFromTemplatePersistent.getName(), alertFromTemplate.getName());
        assertEquals(alertFromTemplatePersistent.getDescription(), alertFromTemplate.getDescription());
        assertEquals(alertFromTemplatePersistent.getProjectId(), alertFromTemplate.getProjectId());
        assertEquals(alertFromTemplatePersistent.getFolderId(), alertFromTemplate.getFolderId());
        assertEquals(alertFromTemplatePersistent.getCreatedAt(), alertFromTemplate.getCreatedAt());
        assertEquals(alertFromTemplatePersistent.getCreatedBy(), alertFromTemplate.getCreatedBy());
        assertEquals(alertFromTemplatePersistent.getUpdatedAt(), alertFromTemplate.getUpdatedAt());
        assertEquals(alertFromTemplatePersistent.getUpdatedBy(), alertFromTemplate.getUpdatedBy());
        assertEquals(alertFromTemplatePersistent.getVersion(), alertFromTemplate.getVersion());
        assertEquals(alertFromTemplatePersistent.getState(), alertFromTemplate.getState());
        assertEquals(alertFromTemplatePersistent.getEscalations(), alertFromTemplate.getEscalations());
        assertEquals(alertFromTemplatePersistent.getNotificationChannels(), alertFromTemplate.getNotificationChannels());
        var labels = new HashMap<>(alertFromTemplatePersistent.getLabels());
        labels.put("templateId", template.getId());
        labels.put("templateVersionTag", template.getTemplateVersionTag());
        labels.put("serviceProviderId", template.getServiceProviderId());
        assertEquals(labels, alertFromTemplate.getLabels());

        assertEquals(Duration.ofMillis(template.getPeriodMillis()), alertFromTemplate.getPeriod());
        assertEquals(template.getDelaySeconds(), alertFromTemplate.getDelaySeconds());
        assertEquals(ResolvedEmptyPolicy.ALARM, alertFromTemplate.getResolvedEmptyPolicy());
        assertEquals(NoPointsPolicy.MANUAL, alertFromTemplate.getNoPointsPolicy());
        assertEquals(AlertSeverity.CRITICAL, alertFromTemplate.getSeverity());
    }

    private AlertTemplate thresholdTemplate() {
        return ThresholdAlertTemplate.newBuilder()
                .setId("")
                .setTemplateVersionTag("")
                .setServiceProviderId("")
                .setPeriodMillis(1000)
                .setDelaySeconds(2000)
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
                .setAlertSeverity(AlertSeverity.CRITICAL)
                .setParameters(getParameterParams())
                .setThresholds(getThresholdParams())
                .build();
    }

    private AlertTemplate expressionTemplate() {
        return ExpressionAlertTemplate.newBuilder()
                .setId("")
                .setTemplateVersionTag("")
                .setServiceProviderId("")
                .setPeriodMillis(1000)
                .setDelaySeconds(2000)
                .setProgram("let {{templateParameter.paramA}}_{{templateParameter.paramB}} = random01() < {{templateParameter.paramC}}; {{alert.projectId}}")
                .setGroupByLabels(List.of("host", "abc_{{templateParameter.paramD}} {{templateParameter.skippedParam}}"))
                .setResolvedEmptyPolicy(ResolvedEmptyPolicy.ALARM)
                .setNoPointsPolicy(NoPointsPolicy.MANUAL)
                .setAlertSeverity(AlertSeverity.CRITICAL)
                .setParameters(getParameterParams())
                .setThresholds(getThresholdParams())
                .build();
    }

    private static List<AlertTemplateParameter> getThresholdParams() {
        return List.of(
                new AlertTemplateParameter.TextParameterValue("paramAValueDefault", "paramA", "title", "descr"),
                new AlertTemplateParameter.TextParameterValue("paramFValueDefault", "paramF", "title", "descr"),
                new AlertTemplateParameter.TextListParameterValue(List.of("paramGValue1Default", "paramGValue2Default"), "paramG", "title", "descr"),
                new AlertTemplateParameter.IntegerParameterValue(2, "paramI", "title", "descr", "unit1"),
                new AlertTemplateParameter.DoubleParameterValue(2.2, "paramJ", "title", "descr", "unit1"),
                new AlertTemplateParameter.LabelListParameterValue("", "", List.of("paramKValue1Default", "paramKValue2Default"), "paramK", "title", "descr", "", false),
                new AlertTemplateParameter.IntegerParameterValue(100, "paramC", "title", "descr", "unit1"),
                new AlertTemplateParameter.DoubleParameterValue(10.1, "paramD", "title", "descr", "unit1"),
                new AlertTemplateParameter.DoubleParameterValue(23.22, "thresholdOnly", "title", "descr", "unit1"),
                new AlertTemplateParameter.DoubleParameterValue(3.3, "justDefaultValue", "title", "descr", "unit1")
        );
    }

    private static List<AlertTemplateParameter> getParameterParams() {
        return List.of(
                new AlertTemplateParameter.TextListParameterValue(List.of("paramBValue1Default", "paramBValue2Default"), "paramB", "title", "descr"),
                new AlertTemplateParameter.LabelListParameterValue("", "", List.of("paramEValue1Default", "paramEValue2Default"), "paramE", "title", "descr", "", false)
        );
    }

    public static AlertFromTemplatePersistent alertFromTemplatePersistent() {
        return AlertFromTemplatePersistent.newBuilder()
                .setId("id")
                .setName("name")
                .setDescription("descr")
                .setProjectId("junk")
                .setFolderId("myfolder")
                .setVersion(11)
                .setCreatedBy("user1")
                .setUpdatedAt(System.nanoTime())
                .setUpdatedBy("user2")
                .setCreatedAt(System.nanoTime())
                .setState(AlertState.ACTIVE)
                .setNotificationChannel("notify-" + UUID.randomUUID())
                .setTemplateId("templateId")
                .setTemplateVersionTag("templateVersionTag")
                .setLabels(Map.of("label1", "label2"))
                .setParameters(getParameters())
                .setThresholds(getThresholds())
                .setEscalation("e1")
                .setAnnotations(Map.of("a", "{{templateParameter.paramE}}", "{{templateParameter.paramF}}",
                        "{{templateParameter.paramG}}", "{{templateParameter.paramI}}",
                        "{{templateParameter.paramJ}}_{{templateParameter.paramK}}",
                        "{{templateParameters.paramE}}", "{{templateParameters.paramF}}"))
                .setServiceProviderAnnotations(Map.of("aa", "{{templateParameter.paramE}}", "{{templateParameter.paramF}}",
                        "{{templateParameter.paramG}}", "{{templateParameter.paramI}}",
                        "{{templateParameter.paramJ}}_{{templateParameter.paramK}}",
                        "{{templateParameters.paramE}}", "{{templateParameters.paramF}}"))
                .build();
    }

    private static List<AlertParameter> getThresholds() {
        return List.of(
                new AlertParameter.TextParameterValue("paramAValue", "paramA"),
                new AlertParameter.DoubleParameterValue(2.22, "thresholdOnly"),
                new AlertParameter.IntegerParameterValue(10, "paramC"),
                new AlertParameter.DoubleParameterValue(1.1, "paramD"),
                new AlertParameter.DoubleParameterValue(1.1, "paramFromOldVersion")
        );
    }

    private static List<AlertParameter> getParameters() {
        return List.of(
                new AlertParameter.TextListParameterValue(List.of("paramBValue1", "paramBValue2"), "paramB"),
                new AlertParameter.LabelListParameterValue(List.of("paramEValue1", "paramEValue2"), "paramE")
        );
    }
}
