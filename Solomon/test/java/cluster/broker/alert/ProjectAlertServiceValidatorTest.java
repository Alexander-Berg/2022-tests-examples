package ru.yandex.solomon.alert.cluster.broker.alert;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.jns.client.JnsClientStub;
import ru.yandex.jns.dto.JnsListEscalationPolicy;
import ru.yandex.solomon.alert.cluster.broker.alert.activity.AlertActivity;
import ru.yandex.solomon.alert.cluster.broker.alert.activity.SimpleActivitiesFactory;
import ru.yandex.solomon.alert.cluster.broker.alert.activity.TemplateAlertActivity;
import ru.yandex.solomon.alert.cluster.broker.notification.StatefulNotificationChannelFactoryStub;
import ru.yandex.solomon.alert.dao.memory.InMemoryAlertTemplateDao;
import ru.yandex.solomon.alert.dao.memory.InMemoryAlertTemplateLastVersionDao;
import ru.yandex.solomon.alert.domain.template.AlertFromTemplatePersistent;
import ru.yandex.solomon.alert.domain.template.AlertParameter;
import ru.yandex.solomon.alert.domain.threshold.Compare;
import ru.yandex.solomon.alert.domain.threshold.TargetStatus;
import ru.yandex.solomon.alert.domain.threshold.ThresholdAlert;
import ru.yandex.solomon.alert.domain.threshold.ThresholdType;
import ru.yandex.solomon.alert.protobuf.CreateAlertsFromTemplateRequest;
import ru.yandex.solomon.alert.protobuf.TDeleteAlertRequest;
import ru.yandex.solomon.alert.template.domain.AlertTemplateLastVersion;
import ru.yandex.solomon.alert.template.domain.AlertTemplateParameter;
import ru.yandex.solomon.alert.template.domain.threshold.TemplatePredicateRule;
import ru.yandex.solomon.alert.template.domain.threshold.ThresholdAlertTemplate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Alexey Trushkin
 */
@ParametersAreNonnullByDefault
public class ProjectAlertServiceValidatorTest {

    private ProjectAlertServiceValidator validator;
    private InMemoryAlertTemplateDao dao;
    private InMemoryAlertTemplateLastVersionDao varsionsDao;
    private JnsClientStub jns;

    @Before
    public void setUp() {
        validator = new ProjectAlertServiceValidatorImpl(
                dao = new InMemoryAlertTemplateDao(false),
                varsionsDao = new InMemoryAlertTemplateLastVersionDao(),
                jns = new JnsClientStub());
    }

    @Test
    public void validateCreateEscalations() {

        var alert = alert("id", List.of(), List.of()).toBuilder()
                .setEscalations(new LinkedHashSet<>(List.of("e1", "e2")))
                .build();
        dao.create(thresholdTemplate("id").toBuilder()
                .setParameters(List.of())
                .build()).join();
        assertEquals("No escalation 'e1' in project 'myProject'", validator.validateCreate(alert).join());
        jns.escalations = new JnsListEscalationPolicy("", "", List.of(new JnsListEscalationPolicy.JnsEscalationPolicy("e1", "e1")));
        assertEquals("No escalation 'e2' in project 'myProject'", validator.validateCreate(alert).join());
        jns.escalations = new JnsListEscalationPolicy("", "", List.of(
                new JnsListEscalationPolicy.JnsEscalationPolicy("e1", "e1"),
                new JnsListEscalationPolicy.JnsEscalationPolicy("e2", "e2")
        ));
        assertEquals(null, validator.validateCreate(alert).join());
        jns.escalations = new JnsListEscalationPolicy("error", "error", List.of());
        assertEquals(null, validator.validateCreate(alert).join());

        jns.setFail(true);
        assertEquals(null, validator.validateCreate(alert).join());
    }

    @Test
    public void validateCreateFailed() {
        assertEquals("Hasn't alert template with id id and version idTag", validator.validateCreate(alert("id", List.of(), List.of())).join());

        dao.create(thresholdTemplate("id")).join();
        assertEquals("Alert template hasn't parameter or parameter duplicated: noParam", validator.validateCreate(alert("id", List.of(
                new AlertParameter.DoubleParameterValue(2.1, "noParam")
        ), List.of())).join());
        assertEquals("Alert template hasn't parameter or parameter duplicated: p3", validator.validateCreate(alert("id", List.of(
                new AlertParameter.DoubleParameterValue(2.1, "p3"),
                new AlertParameter.DoubleParameterValue(2.1, "p3")
        ), List.of())).join());
        assertEquals("Alert template hasn't threshold or threshold duplicated: noParam", validator.validateCreate(alert("id", List.of(), List.of(
                new AlertParameter.DoubleParameterValue(2.1, "noParam")
        ))).join());
        assertEquals("Alert template hasn't threshold or threshold duplicated: p3", validator.validateCreate(alert("id", List.of(), List.of(
                new AlertParameter.DoubleParameterValue(2.1, "p3"),
                new AlertParameter.DoubleParameterValue(2.1, "p3")
        ))).join());
        assertEquals("Alert template has parameter type TEXT, but here another type DOUBLE", validator.validateCreate(alert("id", List.of(
                new AlertParameter.DoubleParameterValue(2.1, "p1")
        ), List.of())).join());
        assertEquals("Alert template has parameter type INTEGER, but here another type DOUBLE", validator.validateCreate(alert("id", List.of(
                new AlertParameter.DoubleParameterValue(2.1, "p2")
        ), List.of())).join());
        assertEquals("Alert template has parameter type DOUBLE, but here another type INTEGER", validator.validateCreate(alert("id", List.of(
                new AlertParameter.IntegerParameterValue(2, "p3")
        ), List.of())).join());
        assertEquals("Alert template has threshold type TEXT_LIST, but here another type INTEGER", validator.validateCreate(alert("id", List.of(), List.of(
                new AlertParameter.IntegerParameterValue(2, "p4")
        ))).join());
        assertEquals("Alert template has threshold type LABEL_LIST, but here another type INTEGER", validator.validateCreate(alert("id", List.of(), List.of(
                new AlertParameter.IntegerParameterValue(2, "p5")
        ))).join());
        assertEquals("Alert template hasn't threshold or threshold duplicated: p3", validator.validateCreate(alert("id", List.of(), List.of(
                new AlertParameter.IntegerParameterValue(2, "p3")
        ))).join());
        assertEquals("Alert template hasn't parameter or parameter duplicated: p4", validator.validateCreate(alert("id", List.of(
                new AlertParameter.IntegerParameterValue(2, "p4")
        ), List.of())).join());
    }

    @Test
    public void validateCreate_defaultsEmpty() {
        dao.create(thresholdTemplate("id").toBuilder()
                .setParameters(List.of(
                        new AlertTemplateParameter.TextParameterValue("", "p1", "p1", "descr"),
                        new AlertTemplateParameter.TextListParameterValue(List.of(), "p2", "p2", "descr"),
                        new AlertTemplateParameter.LabelListParameterValue("", "", List.of(), "p3", "p3", "descr", "", false)
                ))
                .build()).join();
        assertEquals("Alert parameter p1 value must be specified", validator.validateCreate(alert("id", List.of(
        ), List.of())).join());
        assertEquals("Alert parameter p1 value must be specified", validator.validateCreate(alert("id", List.of(
                new AlertParameter.TextParameterValue("", "p1")
        ), List.of())).join());
        assertEquals("Alert parameter p2 value must be specified", validator.validateCreate(alert("id", List.of(
                new AlertParameter.TextParameterValue("1", "p1")
        ), List.of())).join());
        assertEquals("Alert parameter p2 value must be specified", validator.validateCreate(alert("id", List.of(
                new AlertParameter.TextParameterValue("1", "p1"),
                new AlertParameter.TextListParameterValue(List.of(), "p2")
        ), List.of())).join());
        assertEquals("Alert parameter p3 value must be specified", validator.validateCreate(alert("id", List.of(
                new AlertParameter.TextParameterValue("1", "p1"),
                new AlertParameter.TextListParameterValue(List.of("123"), "p2")
        ), List.of())).join());
        assertEquals("Alert parameter p3 value must be specified", validator.validateCreate(alert("id", List.of(
                new AlertParameter.TextParameterValue("1", "p1"),
                new AlertParameter.TextListParameterValue(List.of("123"), "p2"),
                new AlertParameter.LabelListParameterValue(List.of(), "p3")
        ), List.of())).join());

        assertEquals(null, validator.validateCreate(alert("id", List.of(
                new AlertParameter.TextParameterValue("1", "p1"),
                new AlertParameter.TextListParameterValue(List.of("123"), "p2"),
                new AlertParameter.LabelListParameterValue(List.of("123"), "p3")

        ), List.of())).join());
    }


    @Test
    public void validateCreate_defaultsSpecified() {
        dao.create(thresholdTemplate("id").toBuilder()
                .setParameters(List.of(
                        new AlertTemplateParameter.TextParameterValue("1", "p1", "p1", "descr"),
                        new AlertTemplateParameter.TextListParameterValue(List.of("2"), "p2", "p2", "descr"),
                        new AlertTemplateParameter.LabelListParameterValue("", "", List.of("3"), "p3", "p3", "descr", "", false)
                ))
                .build()).join();
        assertEquals(null, validator.validateCreate(alert("id", List.of(
        ), List.of())).join());
    }

    @Test
    public void validateUpdate_canUpdateNotSpecified() {
        dao.create(thresholdTemplate("id").toBuilder()
                .setParameters(List.of(
                        new AlertTemplateParameter.TextParameterValue("1", "p1", "p1", "descr"),
                        new AlertTemplateParameter.TextParameterValue("1", "p2", "p2", "descr")
                ))
                .build()).join();
        var factory = new SimpleActivitiesFactory(
                null,
                null,
                null,
                new StatefulNotificationChannelFactoryStub(Executors.newSingleThreadScheduledExecutor(), "junk"),
                null);
        var activity = new TemplateAlertActivity(alertFromTemplatePersistent("id", "idTag").toBuilder()
                .setParameters(List.of(new AlertParameter.TextParameterValue("", "p1")))
                .build(), ThresholdAlert.newBuilder()
                .setId(UUID.randomUUID().toString())
                .setName("Name with random")
                .setProjectId("junk")
                .setSelectors("")
                .build(), factory,
                null);
        assertEquals(null, validator.validateUpdate(alert("id", List.of(
                new AlertParameter.TextParameterValue("12", "p1"),
                new AlertParameter.TextParameterValue("11", "p2")
        ), List.of()), activity, true).join());

    }

    @Test
    public void validateUpdateFailed() {
        assertEquals("Hasn't alert template with id id and version idTag", validator.validateUpdate(alert("id", List.of(), List.of()), activity("id", "idTag"), true).join());

        dao.create(thresholdTemplate("id")).join();
        assertEquals("Can't change template id, original was id2", validator.validateUpdate(alert("id", List.of(), List.of()), activity("id2", "idTag"), true).join());
        assertEquals("Can't change template version tag, original was idTag2", validator.validateUpdate(alert("id", List.of(), List.of()), activity("id", "idTag2"), true).join());

        assertEquals("Can't remove parameters from alert, instead change alert template.", validator.validateUpdate(alert("id", List.of(
                new AlertParameter.TextParameterValue("1", "p1")
        ), List.of()), activity("id", "idTag"), true).join());

        assertEquals("Parameter p3 can't be changed, instead change alert template.", validator.validateUpdate(alert("id", List.of(
                new AlertParameter.TextParameterValue("1", "p1"),
                new AlertParameter.DoubleParameterValue(3.0, "p3")// changed
        ), List.of()), activity("id", "idTag"), true).join());

        assertEquals("Parameter p2 can't be changed, instead change alert template.", validator.validateUpdate(alert("id", List.of(
                new AlertParameter.TextParameterValue("1", "p1"),
                new AlertParameter.IntegerParameterValue(1, "p2")// changed
        ), List.of()), activity("id", "idTag"), true).join());

        assertEquals("Alert template hasn't parameter or parameter duplicated: p4", validator.validateUpdate(alert("id", List.of(
                new AlertParameter.TextParameterValue("1", "p1"),
                new AlertParameter.IntegerParameterValue(2, "p2"),
                new AlertParameter.DoubleParameterValue(1.1, "p3"),
                new AlertParameter.DoubleParameterValue(4.0, "p4")// added
        ), List.of()), activity("id", "idTag"), true).join());
        assertEquals("Can't change alert of another service provider", validator.validateUpdate(alert("id", List.of(
                new AlertParameter.TextParameterValue("1", "p1"),
                new AlertParameter.IntegerParameterValue(2, "p2"),
                new AlertParameter.DoubleParameterValue(1.1, "p3")
        ), List.of()).toBuilder().setServiceProvider("anotherService").build(), activity("id", "idTag"), true).join());
    }

    @Test
    public void validateServiceProviderAnnotationsUpdate() {
        dao.create(thresholdTemplate("id")).join();
        var newAlert = alert("id", List.of(
                new AlertParameter.TextParameterValue("1", "p1"),
                new AlertParameter.IntegerParameterValue(2, "p2"),
                new AlertParameter.DoubleParameterValue(1.1, "p3")
        ), List.of());
        assertNull(validator.validateUpdate(newAlert, activity("id", "idTag"), false).join());
        assertNull(validator.validateUpdate(newAlert.toBuilder()
                .setServiceProviderAnnotations(Map.of("k1", "v11", "k22", "v2"))
                .build(), activity("id", "idTag"), false).join());
        assertEquals("Can't change service provider annotations", validator.validateUpdate(newAlert.toBuilder()
                .setServiceProviderAnnotations(Map.of("k1", "v11", "k22", "v2"))
                .build(), activity("id", "idTag"), true).join());
    }

    @Test
    public void validateCreate() {
        dao.create(thresholdTemplate("id")).join();
        assertNull(validator.validateCreate(alert("id", List.of(
                new AlertParameter.DoubleParameterValue(2.1, "p3")
        ), List.of())).join());
        assertNull(validator.validateCreate(alert("id", List.of(
                new AlertParameter.TextParameterValue("value", "p1"),
                new AlertParameter.DoubleParameterValue(2.1, "p3")
        ), List.of())).join());
        assertNull(validator.validateCreate(alert("id", List.of(
                new AlertParameter.TextParameterValue("value", "p1"),
                new AlertParameter.DoubleParameterValue(2.1, "p3")
        ), List.of(
                new AlertParameter.TextListParameterValue(List.of(""), "p4")
        ))).join());
        assertNull(validator.validateCreate(alert("id", List.of(
                new AlertParameter.TextParameterValue("value", "p1"),
                new AlertParameter.IntegerParameterValue(2, "p2"),
                new AlertParameter.DoubleParameterValue(2.1, "p3")
        ), List.of(
                new AlertParameter.TextListParameterValue(List.of(""), "p4"),
                new AlertParameter.LabelListParameterValue(List.of(""), "p5")
        ))).join());
    }

    @Test
    public void validateUpdate() {
        dao.create(thresholdTemplate("id")).join();
        assertNull(validator.validateUpdate(alert("id", List.of(
                new AlertParameter.TextParameterValue("1", "p1"),
                new AlertParameter.IntegerParameterValue(2, "p2"),
                new AlertParameter.DoubleParameterValue(1.1, "p3")
        ), List.of()), activity("id", "idTag"), true).join());
        assertNull(validator.validateUpdate(alert("id", List.of(
                new AlertParameter.TextParameterValue("1", "p1"),
                new AlertParameter.IntegerParameterValue(2, "p2"),
                new AlertParameter.DoubleParameterValue(1.1, "p3")
        ), List.of(
                new AlertParameter.LabelListParameterValue(List.of(""), "p5")
        )), activity("id", "idTag"), true).join());
    }

    @Test
    public void validateDelete() {
        dao.create(thresholdTemplate("id")).join();
        assertTrue(validator.validateDelete(TDeleteAlertRequest.newBuilder()
                .setServiceProvider("another one")
                .build(), activity("id2", "idTag")));
        assertTrue(validator.validateDelete(TDeleteAlertRequest.newBuilder()
                .setServiceProvider("service")
                .build(), activity("id2", "idTag")));
    }

    @Test
    public void validateUpdateVersion() {
        dao.create(thresholdTemplate("id")).join();
        assertNull(validator.validateAlertVersionUpdate(alert("id", List.of(
                new AlertParameter.TextParameterValue("1", "p1"),
                new AlertParameter.IntegerParameterValue(2, "p2"),
                new AlertParameter.DoubleParameterValue(1.1, "p3")
        ), List.of())).join());
    }

    @Test
    public void validateCreateAlertsFromTemplate_failed() {
        assertEquals("Hasn't published alert template with id template1",
                validator.validateCreateAlertsFromTemplate(CreateAlertsFromTemplateRequest.newBuilder()
                        .addAllTemplateIds(List.of("template1"))
                        .build()).join());

        var template = thresholdTemplate("template1").toBuilder()
                .setId("template1")
                .build();
        dao.create(template).join();
        varsionsDao.create(new AlertTemplateLastVersion(template.getId(), template.getTemplateVersionTag(), template.getServiceProviderId(), template.getName(), 0, ""));

        assertEquals("Resource '{}' hasn't parameter 'p1' for template 'template1'",
                validator.validateCreateAlertsFromTemplate(CreateAlertsFromTemplateRequest.newBuilder()
                        .addAllTemplateIds(List.of("template1"))
                        .addResources(CreateAlertsFromTemplateRequest.Resource.newBuilder()
                                .putAllResourceParameters(Map.of())
                                .build())
                        .build()).join());

        assertEquals("Resource '{p1=2}' hasn't parameter 'p2' for template 'template1'",
                validator.validateCreateAlertsFromTemplate(CreateAlertsFromTemplateRequest.newBuilder()
                        .addAllTemplateIds(List.of("template1"))
                        .addResources(CreateAlertsFromTemplateRequest.Resource.newBuilder()
                                .putAllResourceParameters(Map.of("p1", "2"))
                                .build())
                        .build()).join());
    }

    @Test
    public void validateCreateAlertsFromTemplate() {
        var template = thresholdTemplate("template1").toBuilder()
                .setId("template1")
                .build();
        dao.create(template).join();
        varsionsDao.create(new AlertTemplateLastVersion(template.getId(), template.getTemplateVersionTag(), template.getServiceProviderId(), template.getName(), 0, ""));

        assertNull(validator.validateCreateAlertsFromTemplate(CreateAlertsFromTemplateRequest.newBuilder()
                        .addAllTemplateIds(List.of("template1"))
                        .addResources(CreateAlertsFromTemplateRequest.Resource.newBuilder()
                                .putAllResourceParameters(Map.of("p1", "2", "p2", "2", "p3", "2"))
                                .build())
                        .build()).join());
    }

    @Test
    public void validateUpdateVersionFailed() {
        assertEquals("Hasn't alert template with id id and version idTag", validator.validateAlertVersionUpdate(alert("id", List.of(), List.of())).join());
    }

    private AlertActivity activity(String id, String idTag) {
        var factory = new SimpleActivitiesFactory(
                null,
                null,
                null,
                new StatefulNotificationChannelFactoryStub(Executors.newSingleThreadScheduledExecutor(), "junk"),
                null);
        return new TemplateAlertActivity(alertFromTemplatePersistent(id, idTag), ThresholdAlert.newBuilder()
                .setId(UUID.randomUUID().toString())
                .setName("Name with random")
                .setProjectId("junk")
                .setSelectors("")
                .build(), factory,
                null);
    }

    public static AlertFromTemplatePersistent alertFromTemplatePersistent(String id, String idTag) {
        return AlertFromTemplatePersistent.newBuilder()
                .setId(UUID.randomUUID().toString())
                .setName("Name with random")
                .setProjectId("junk")
                .setFolderId("myfolder")
                .setVersion(0)
                .setTemplateId(id)
                .setTemplateVersionTag(idTag)
                .setParameters(List.of(
                        new ru.yandex.solomon.alert.domain.template.AlertParameter.TextParameterValue("1", "p1"),
                        new ru.yandex.solomon.alert.domain.template.AlertParameter.IntegerParameterValue(2, "p2"),
                        new ru.yandex.solomon.alert.domain.template.AlertParameter.DoubleParameterValue(1.1, "p3")
                ))
                .setServiceProvider("service")
                .setServiceProviderAnnotations(Map.of("k1", "v1", "k2", "v2"))
                .build();
    }

    private AlertFromTemplatePersistent alert(String id, List<AlertParameter> params, List<AlertParameter> thresholds) {
        return AlertFromTemplatePersistent.newBuilder()
                .setName("test")
                .setProjectId("myProject")
                .setId("id")
                .setTemplateId(id)
                .setTemplateVersionTag("idTag")
                .setThresholds(thresholds)
                .setParameters(params)
                .setServiceProvider("service")
                .setServiceProviderAnnotations(Map.of("k1", "v1", "k2", "v2"))
                .build();
    }

    public static ThresholdAlertTemplate thresholdTemplate(String id) {
        return ThresholdAlertTemplate.newBuilder()
                .setId(id)
                .setTemplateVersionTag("idTag")
                .setServiceProviderId("service")
                .setName("name")
                .setDescription("descr")
                .setPeriodMillis(1000)
                .setDelaySeconds(1)
                .setPredicateRules(Stream.of(
                        TemplatePredicateRule.onThreshold(10)
                                .onThresholdParameter("p6")
                                .withThresholdType(ThresholdType.AT_ALL_TIMES)
                                .withComparison(Compare.EQ)
                                .withTargetStatus(TargetStatus.OK),
                        TemplatePredicateRule.onThreshold(10)
                                .withThresholdType(ThresholdType.AT_ALL_TIMES)
                                .withComparison(Compare.EQ)
                                .withTargetStatus(TargetStatus.OK)
                ))
                .setTransformations("AVG")
                .setSelectors("project=solomon, cluster=local, service=test, sensor=idleTime, host=solomon-1")
                .setUpdatedBy("user")
                .setUpdatedAt(Instant.now())
                .setCreatedBy("user2")
                .setCreatedAt(Instant.now())
                .setParameters(getTemplateParameters())
                .setThresholds(getTemplateThresholds())
                .build();
    }

    private static List<AlertTemplateParameter> getTemplateParameters() {
        return List.of(
                new AlertTemplateParameter.TextParameterValue("value", "p1", "p1Title", "descr"),
                new AlertTemplateParameter.IntegerParameterValue(1, "p2", "p2Title", "descr", "unit1"),
                new AlertTemplateParameter.DoubleParameterValue(1.1, "p3", "p3Title", "descr", "unit1")
        );
    }

    private static List<AlertTemplateParameter> getTemplateThresholds() {
        return List.of(
                new AlertTemplateParameter.TextListParameterValue(List.of("value"), "p4", "p4Title", "descr"),
                new AlertTemplateParameter.LabelListParameterValue("", "", List.of("value"), "p5", "p5Title", "descr", "", false)
        );
    }

}
