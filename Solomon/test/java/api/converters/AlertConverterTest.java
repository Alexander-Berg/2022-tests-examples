package ru.yandex.solomon.alert.api.converters;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.solomon.alert.EvaluationStatus;
import ru.yandex.solomon.alert.domain.Alert;
import ru.yandex.solomon.alert.domain.AlertTestSupport;
import ru.yandex.solomon.alert.domain.SubAlert;
import ru.yandex.solomon.alert.domain.template.AlertFromTemplatePersistent;
import ru.yandex.solomon.alert.domain.threshold.ThresholdAlert;
import ru.yandex.solomon.alert.protobuf.AlertTemplateStatus;
import ru.yandex.solomon.alert.protobuf.CreateAlertsFromTemplateRequest;
import ru.yandex.solomon.alert.protobuf.TAlert;
import ru.yandex.solomon.alert.protobuf.TDefaultChannelConfig;
import ru.yandex.solomon.alert.protobuf.TEvaluationState;
import ru.yandex.solomon.alert.protobuf.TEvaluationStatus;
import ru.yandex.solomon.alert.protobuf.TNotificationChannelOptions;
import ru.yandex.solomon.alert.protobuf.TSubAlert;
import ru.yandex.solomon.alert.protobuf.TThreshold;
import ru.yandex.solomon.alert.rule.AlertEvalStateTestSupport;
import ru.yandex.solomon.alert.rule.EvaluationState;
import ru.yandex.solomon.labels.protobuf.LabelSelectorConverter;
import ru.yandex.solomon.labels.query.Selectors;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static ru.yandex.solomon.alert.api.converters.AlertConverter.alertTemplateFromProto;
import static ru.yandex.solomon.alert.api.converters.AlertConverter.alertTemplateToProto;
import static ru.yandex.solomon.alert.api.converters.AlertConverter.alertToProto;
import static ru.yandex.solomon.alert.api.converters.AlertConverter.protoToAlert;
import static ru.yandex.solomon.alert.api.converters.AlertConverter.protoToState;
import static ru.yandex.solomon.alert.api.converters.AlertConverter.protoToStatus;
import static ru.yandex.solomon.alert.api.converters.AlertConverter.stateToProto;
import static ru.yandex.solomon.alert.api.converters.AlertConverter.statusToProto;
import static ru.yandex.solomon.alert.api.converters.AlertConverter.subAlertToProto;
import static ru.yandex.solomon.alert.domain.AlertTestSupport.alertFromTemplatePersistent;
import static ru.yandex.solomon.alert.domain.AlertTestSupport.randomAlert;
import static ru.yandex.solomon.alert.domain.AlertTestSupport.randomAlertWithConfiguredChannels;
import static ru.yandex.solomon.alert.domain.AlertTestSupport.randomExpressionAlert;
import static ru.yandex.solomon.alert.domain.AlertTestSupport.randomSubAlert;
import static ru.yandex.solomon.alert.domain.AlertTestSupport.randomThresholdAlert;
import static ru.yandex.solomon.alert.domain.AlertTestSupport.randomThresholdAlertWithManyRules;
import static ru.yandex.solomon.alert.util.CommonMatchers.reflectionEqualTo;

/**
 * @author Vladimir Gordiychuk
 */
public class AlertConverterTest {

    private ThreadLocalRandom random;

    @Before
    public void setUp() throws Exception {
        random = ThreadLocalRandom.current();
    }

    @Test
    public void thresholdAlert() throws Exception {
        Alert expected = randomThresholdAlert(random).toBuilder()
                .setObtainedFromTemplate(random.nextBoolean())
                .build();
        Alert converted = protoToAlert(alertToProto(expected));
        compare(expected, converted);
    }

    @Test
    public void expressionAlert() throws Exception {
        Alert expected = randomExpressionAlert(random).toBuilder()
                .setObtainedFromTemplate(random.nextBoolean())
                .build();
        Alert converted = protoToAlert(alertToProto(expected));
        compare(expected, converted);
    }

    @Test
    public void alertFromTemplate() {
        Alert expected = alertFromTemplatePersistent(random);
        Alert converted = protoToAlert(alertToProto(expected));
        compare(expected, converted);
    }

    @Test
    public void templateExpression() {
        var expected = AlertTestSupport.expressionTemplate();
        var converted = alertTemplateFromProto(alertTemplateToProto(expected, AlertTemplateStatus.ALERT_TEMPLATE_STATUS_DRAFT));
        compare(expected, converted);
    }

    @Test
    public void templateThreshold() {
        var expected = AlertTestSupport.thresholdTemplate();
        var converted = alertTemplateFromProto(alertTemplateToProto(expected, AlertTemplateStatus.ALERT_TEMPLATE_STATUS_PUBLISHED));
        compare(expected, converted);
    }

    @Test
    public void thresholdRulesAlert() throws Exception {
        Alert expected = randomThresholdAlertWithManyRules(random);
        Alert converted = protoToAlert(alertToProto(expected));
        compare(expected, converted);
    }

    @Test
    public void configuredChannelsAlert() throws Exception {
        Alert expected = randomAlertWithConfiguredChannels(random);
        Alert converted = protoToAlert(alertToProto(expected));
        compare(expected, converted);
    }

    @Test
    public void subAlert() throws Exception {
        SubAlert expected = randomSubAlert(random);
        TSubAlert proto = subAlertToProto(expected);
        assertThat(proto.getParent().getId(), equalTo(expected.getParent().getId()));
    }

    @Test
    public void stateToProtoAndBack() throws Exception {
        EvaluationState expected = AlertEvalStateTestSupport.randomState();
        TEvaluationState proto = stateToProto(expected);
        EvaluationState result = protoToState(proto);
        compare(expected, result);
    }

    @Test
    public void delaySeconds() {
        Alert expected = randomAlert(random)
                .toBuilder()
                .setDelaySeconds(random.nextInt())
                .build();

        Alert converted = protoToAlert(alertToProto(expected));
        compare(expected, converted);
    }

    @Test
    public void newSelectorsOverrideOld() {
        TAlert alert = alertToProto(randomThresholdAlert());
        TThreshold threshold = alert.getThreshold();
        TAlert patched = alert.toBuilder()
                .setThreshold(threshold.toBuilder()
                                .setNewSelectors(LabelSelectorConverter.selectorsToNewProto(Selectors.parse("metric{host='solomon'}"))))
                .build();
        ThresholdAlert result = (ThresholdAlert) protoToAlert(patched);
        assertThat(result.getSelectors().getNameSelector(), equalTo("metric"));
    }

    // Necessary to support new functionality
    @Test
    public void description() {
        Alert expected = randomAlert(random)
                .toBuilder()
                .setDescription("description: " + random.nextInt())
                .build();

        Alert converted = protoToAlert(alertToProto(expected));
        compare(expected, converted);
    }

    @Test
    public void deadlineToProtoAndBack() {
        var status = EvaluationStatus.DEADLINE.withDescription("oops");
        var proto = statusToProto(status);
        var result = protoToStatus(proto);

        assertEquals(status.getErrorCode(), result.getErrorCode());
        assertEquals(status.getDescription(), result.getDescription());
        assertEquals(status, result);
    }

    @Test
    public void evaluationStatusAnnotations() {
        var status = EvaluationStatus.OK.withAnnotations(Map.of("1", "2")).withServiceProviderAnnotations(Map.of("3", "4"));
        var proto = statusToProto(status);
        var result = protoToStatus(proto);

        assertEquals(status.getErrorCode(), result.getErrorCode());
        assertEquals(status.getAnnotations(), result.getAnnotations());
        assertEquals(status.getServiceProviderAnnotations(), result.getServiceProviderAnnotations());
        assertEquals(status, result);
    }

    @Test
    public void evaluationStatusWithScalars() {
        var status = EvaluationStatus.OK.withScalars(List.of(
                new EvaluationStatus.ScalarValue("1", "value", EvaluationStatus.ScalarType.STRING),
                new EvaluationStatus.ScalarValue("2", 1.2, EvaluationStatus.ScalarType.DOUBLE),
                new EvaluationStatus.ScalarValue("3", true, EvaluationStatus.ScalarType.BOOLEAN)
        ));
        var proto = statusToProto(status);

        assertEquals(proto.getScalarValues(0), TEvaluationStatus.ScalarValue.newBuilder()
                .setName("1")
                .setStringValue("value")
                .build());
        assertEquals(proto.getScalarValues(1), TEvaluationStatus.ScalarValue.newBuilder()
                .setName("2")
                .setDoubleValue(1.2)
                .build());
        assertEquals(proto.getScalarValues(2), TEvaluationStatus.ScalarValue.newBuilder()
                .setName("3")
                .setBoolValue(true)
                .build());
    }

    @Test
    public void protoToAlerts() {
        var cmd = CreateAlertsFromTemplateRequest.newBuilder()
                .setServiceProviderId("service")
                .setProjectId("project")
                .setCreatedBy("creator")
                .addAllTemplateIds(List.of("t1", "t2"))
                .addEscalations("e1")
                .addAllResources(List.of(
                        CreateAlertsFromTemplateRequest.Resource.newBuilder()
                                .putAllResourceParameters(Map.of("1", "2", "3", "4"))
                                .build(),
                        CreateAlertsFromTemplateRequest.Resource.newBuilder()
                                .putAllResourceParameters(Map.of("2", "3", "4", "5"))
                                .build()
                ))
                .build();
        var alerts = AlertConverter.protoToAlerts(1000, cmd);
        validateAlert(alerts.get(0), cmd,"t1", Map.of("1", "2", "3", "4"), Set.of("e1"), Set.of());
        validateAlert(alerts.get(1), cmd,"t1", Map.of("2", "3", "4", "5"), Set.of("e1"), Set.of());
        validateAlert(alerts.get(2), cmd,"t2", Map.of("1", "2", "3", "4"), Set.of("e1"), Set.of());
        validateAlert(alerts.get(3), cmd,"t2", Map.of("2", "3", "4", "5"), Set.of("e1"), Set.of());
    }

    @Test
    public void protoToAlerts_multipleConfigs() {
        var cmd = CreateAlertsFromTemplateRequest.newBuilder()
                .setServiceProviderId("service")
                .setProjectId("project")
                .setCreatedBy("creator")
                .addAllTemplateIds(List.of("t1", "t2"))
                .putChannels("c1", TNotificationChannelOptions.newBuilder().setDefaultChannelConfig(TDefaultChannelConfig.newBuilder().build()).build())
                .addEscalations("e1")
                .addAllTemplateConfigs(List.of(
                        CreateAlertsFromTemplateRequest.TemplateConfigs.newBuilder()
                                .setTemplateId("t1")
                                .addEscalations("e2")
                                .putChannels("c2", TNotificationChannelOptions.newBuilder().setDefaultChannelConfig(TDefaultChannelConfig.newBuilder().build()).build())
                                .build(),
                        CreateAlertsFromTemplateRequest.TemplateConfigs.newBuilder()
                                .setTemplateId("t2")
                                .build()
                ))
                .addAllResources(List.of(
                        CreateAlertsFromTemplateRequest.Resource.newBuilder()
                                .putAllResourceParameters(Map.of("1", "2", "3", "4"))
                                .build()
                ))
                .build();
        var alerts = AlertConverter.protoToAlerts(1000, cmd);
        validateAlert(alerts.get(0), cmd,"t1", Map.of("1", "2", "3", "4"), Set.of("e2"), Set.of("c2"));
        validateAlert(alerts.get(1), cmd,"t2", Map.of("1", "2", "3", "4"), Set.of("e1"), Set.of("c1"));
    }

    private void validateAlert(
            AlertFromTemplatePersistent fromTemplatePersistent,
            CreateAlertsFromTemplateRequest cmd,
            String templateId,
            Map<String, String> labels,
            Set<String> escalations,
            Set<String> channels)
    {
        assertNotNull(fromTemplatePersistent.getId());
        assertTrue(fromTemplatePersistent.getCreatedAt() > 0);
        assertTrue(fromTemplatePersistent.getUpdatedAt() > 0);
        assertTrue(fromTemplatePersistent.getVersion() > 0);
        assertEquals(fromTemplatePersistent.getProjectId(), cmd.getProjectId());
        assertEquals(fromTemplatePersistent.getTemplateId(), templateId);
        assertEquals(fromTemplatePersistent.getTemplateVersionTag(), "");
        assertEquals(fromTemplatePersistent.getName(), "");
        assertEquals(fromTemplatePersistent.getDescription(), "");
        assertEquals(fromTemplatePersistent.getCreatedBy(), cmd.getCreatedBy());
        assertEquals(fromTemplatePersistent.getUpdatedBy(), cmd.getCreatedBy());
        assertEquals(fromTemplatePersistent.getLabels(), labels);
        assertEquals(fromTemplatePersistent.getEscalations(), escalations);
        assertEquals(fromTemplatePersistent.getNotificationChannels().keySet(), channels);
    }

    private void compare(Object expected, Object converted) {
        assertThat("Original: " + expected + ", Converted: " + converted,
                converted, reflectionEqualTo(expected));
    }
}
