package ru.yandex.solomon.alert.cluster.broker.alert;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.solomon.alert.dao.memory.InMemoryAlertTemplateDao;
import ru.yandex.solomon.alert.dao.memory.InMemoryAlertTemplateLastVersionDao;
import ru.yandex.solomon.alert.domain.AlertTestSupport;
import ru.yandex.solomon.alert.domain.template.AlertFromTemplatePersistent;
import ru.yandex.solomon.alert.domain.template.AlertParameter;
import ru.yandex.solomon.alert.template.domain.AlertTemplateLastVersion;
import ru.yandex.solomon.alert.template.domain.AlertTemplateParameter;
import ru.yandex.solomon.alert.template.domain.threshold.ThresholdAlertTemplate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static ru.yandex.solomon.alert.cluster.broker.alert.ProjectAlertServiceValidatorTest.thresholdTemplate;

/**
 * @author Alexey Trushkin
 */
public class AlertPostInitializerTest {

    private InMemoryAlertTemplateDao dao;
    private AlertPostInitializer initializer;
    private InMemoryAlertTemplateLastVersionDao versionsDao;

    @Before
    public void setUp() {
        initializer = new AlertPostInitializerImpl(
                dao = new InMemoryAlertTemplateDao(false),
                versionsDao = new InMemoryAlertTemplateLastVersionDao());
    }

    @Test
    public void postCreate() {
        var template = thresholdTemplate("id").toBuilder()
                .setAnnotations(Map.of("v1", "k1", "v3", "k3"))
                .build();
        var alert = AlertTestSupport.alertFromTemplatePersistent(ThreadLocalRandom.current(), false).toBuilder()
                .setTemplateId(template.getId())
                .setTemplateVersionTag(template.getTemplateVersionTag())
                .setServiceProviderAnnotations(Map.of("1", "2"))
                .build();
        dao.create(template).join();

        var initializedAlert = initializer.initializeCreate(alert).join();
        assertTrue(alert.toBuilder()
                .setServiceProviderAnnotations(template.getAnnotations())
                .build().equalContent(initializedAlert));
    }

    @Test
    public void postVersionUpdate() {
        var template = thresholdTemplate("id").toBuilder()
                .setAnnotations(Map.of("v1", "k1", "v3", "k3"))
                .build();
        var alert = AlertTestSupport.alertFromTemplatePersistent(ThreadLocalRandom.current(), false).toBuilder()
                .setTemplateId(template.getId())
                .setTemplateVersionTag(template.getTemplateVersionTag())
                .setServiceProviderAnnotations(Map.of("1", "2"))
                .build();
        dao.create(template).join();

        var initializedAlert = initializer.initializeVersionUpdate(alert).join();
        assertTrue(alert.toBuilder()
                .setServiceProviderAnnotations(template.getAnnotations())
                .build().equalContent(initializedAlert));
    }


    @Test
    public void postInitializeTemplateAlerts() {
        var template = thresholdTemplate("id").toBuilder()
                .setParameters(List.of(
                        new AlertTemplateParameter.TextParameterValue("", "p1", "p1", "descr"),
                        new AlertTemplateParameter.LabelListParameterValue("", "", List.of(), "p3", "p3", "descr", "", false)
                ))
                .setAnnotations(Map.of("v1", "k1", "v3", "k3"))
                .build();
        var template2 = thresholdTemplate("id2").toBuilder()
                .setAnnotations(Map.of("v1", "k1", "v3", "k3"))
                .setParameters(List.of(
                        new AlertTemplateParameter.TextParameterValue("", "p4", "p4", "descr")
                ))
                .setServiceProviderId(template.getServiceProviderId())
                .build();
        dao.create(template).join();
        dao.create(template2).join();
        versionsDao.create(new AlertTemplateLastVersion(template.getId(), template.getTemplateVersionTag(), template.getServiceProviderId(), template.getName(), 0, ""));
        versionsDao.create(new AlertTemplateLastVersion(template2.getId(), template2.getTemplateVersionTag(), template2.getServiceProviderId(), template2.getName(), 0, ""));

        var alerts = List.of(
                AlertTestSupport.alertFromTemplatePersistent(ThreadLocalRandom.current(), false).toBuilder()
                        .setLabels(Map.of("p1", "1", "p3", "4", "p4", "5"))
                        .setTemplateId(template.getId())
                        .build(),
                AlertTestSupport.alertFromTemplatePersistent(ThreadLocalRandom.current(), false).toBuilder()
                        .setLabels(Map.of("p1", "2", "p3", "4", "p4", "5"))
                        .setTemplateId(template.getId())
                        .build(),
                AlertTestSupport.alertFromTemplatePersistent(ThreadLocalRandom.current(), false).toBuilder()
                        .setLabels(Map.of("p1", "3", "p3", "4", "p4", "5"))
                        .setTemplateId(template2.getId())
                        .build(),
                AlertTestSupport.alertFromTemplatePersistent(ThreadLocalRandom.current(), false).toBuilder()
                        .setLabels(Map.of("p1", "4", "p3", "4", "p4", "5"))
                        .setTemplateId(template2.getId())
                        .build()
        );

        var initializedAlert = initializer.initializeTemplateAlertsFromPublishedTemplates(alerts, template.getServiceProviderId()).join();
        assertAlert(template, initializedAlert.get(0));
        assertAlert(template, initializedAlert.get(1));
        assertAlert(template2, initializedAlert.get(2));
        assertAlert(template2, initializedAlert.get(3));
    }

    @Test
    public void postInitializeTemplateAlerts_resourceType() {
        var template = thresholdTemplate("id").toBuilder()
                .setParameters(List.of(
                        new AlertTemplateParameter.TextParameterValue("", "p1", "p1", "descr"),
                        new AlertTemplateParameter.LabelListParameterValue("", "", List.of(), "p3", "p3", "descr", "", false)
                ))
                .setAnnotations(Map.of("v1", "k1", "v3", "k3"))
                .setLabels(Map.of("resourceType", "dedicated"))
                .build();
        var template2 = thresholdTemplate("id2").toBuilder()
                .setAnnotations(Map.of("v1", "k1", "v3", "k3"))
                .setParameters(List.of(
                        new AlertTemplateParameter.TextParameterValue("", "p4", "p4", "descr")
                ))
                .setServiceProviderId(template.getServiceProviderId())
                .build();
        dao.create(template).join();
        dao.create(template2).join();
        versionsDao.create(new AlertTemplateLastVersion(template.getId(), template.getTemplateVersionTag(), template.getServiceProviderId(), template.getName(), 0, ""));
        versionsDao.create(new AlertTemplateLastVersion(template2.getId(), template2.getTemplateVersionTag(), template2.getServiceProviderId(), template2.getName(), 0, ""));

        var alerts = List.of(
                AlertTestSupport.alertFromTemplatePersistent(ThreadLocalRandom.current(), false).toBuilder()
                        .setLabels(Map.of("p1", "1", "p3", "4", "p4", "5", "resourceType", "dedicated"))
                        .setTemplateId(template.getId())
                        .build(),
                AlertTestSupport.alertFromTemplatePersistent(ThreadLocalRandom.current(), false).toBuilder()
                        .setLabels(Map.of("p1", "2", "p3", "4", "p4", "5"))
                        .setTemplateId(template.getId())
                        .build(),
                AlertTestSupport.alertFromTemplatePersistent(ThreadLocalRandom.current(), false).toBuilder()
                        .setLabels(Map.of("p1", "2", "p3", "4", "p4", "5", "resourceType", "serverless"))
                        .setTemplateId(template.getId())
                        .build(),
                AlertTestSupport.alertFromTemplatePersistent(ThreadLocalRandom.current(), false).toBuilder()
                        .setLabels(Map.of("p1", "3", "p3", "4", "p4", "5"))
                        .setTemplateId(template2.getId())
                        .build(),
                AlertTestSupport.alertFromTemplatePersistent(ThreadLocalRandom.current(), false).toBuilder()
                        .setLabels(Map.of("p1", "4", "p3", "4", "p4", "5"))
                        .setTemplateId(template2.getId())
                        .build()
        );

        var initializedAlert = initializer.initializeTemplateAlertsFromPublishedTemplates(alerts, template.getServiceProviderId()).join();
        assertAlert(template, initializedAlert.get(0));
        assertAlert(template2, initializedAlert.get(1));
        assertAlert(template2, initializedAlert.get(2));
    }

    private void assertAlert(ThresholdAlertTemplate template, AlertFromTemplatePersistent initializedAlert) {
        assertEquals(template.getTemplateVersionTag(), initializedAlert.getTemplateVersionTag());
        assertEquals(template.getAnnotations(), initializedAlert.getServiceProviderAnnotations());
        assertEquals(template.getDescription(), initializedAlert.getDescription());
        assertFalse(template.getName().isEmpty());

        var map = initializedAlert.getParameters().stream().collect(Collectors.toMap(AlertParameter::getName, Function.identity()));

        for (AlertTemplateParameter parameter : template.getParameters()) {
            var param = map.get(parameter.getName());
            if (param instanceof AlertParameter.LabelListParameterValue l) {
                assertEquals(List.of(initializedAlert.getLabels().get(parameter.getName())), l.getValues());
            } else if (param instanceof AlertParameter.TextParameterValue l) {
                assertEquals(initializedAlert.getLabels().get(parameter.getName()), l.getValue());
            }
        }
    }
}
