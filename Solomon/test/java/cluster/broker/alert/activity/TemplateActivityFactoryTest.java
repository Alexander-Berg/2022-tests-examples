package ru.yandex.solomon.alert.cluster.broker.alert.activity;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.solomon.alert.cluster.broker.evaluation.EvaluationAssignmentServiceStub;
import ru.yandex.solomon.alert.cluster.broker.mute.MuteMatcherStub;
import ru.yandex.solomon.alert.cluster.broker.notification.StatefulNotificationChannelFactoryStub;
import ru.yandex.solomon.alert.cluster.project.ProjectAssignment;
import ru.yandex.solomon.alert.dao.AlertTemplateDaoTest;
import ru.yandex.solomon.alert.dao.CachedAlertTemplateDao;
import ru.yandex.solomon.alert.dao.memory.InMemoryAlertTemplateDao;
import ru.yandex.solomon.alert.domain.AlertTestSupport;
import ru.yandex.solomon.alert.domain.template.AlertFromTemplatePersistent;
import ru.yandex.solomon.alert.template.MustacheTemplateFactory;
import ru.yandex.solomon.alert.template.domain.AlertTemplate;
import ru.yandex.solomon.alert.template.domain.AlertTemplateId;
import ru.yandex.solomon.alert.unroll.UnrollExecutorStub;
import ru.yandex.solomon.balancer.AssignmentSeqNo;
import ru.yandex.solomon.ut.ManualClock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Alexey Trushkin
 */
public class TemplateActivityFactoryTest {

    private CachedAlertTemplateDao alertTemplateDao;
    private TemplateActivityFactory templateActivityFactory;

    @Before
    public void setUp() throws Exception {
        var executor = Executors.newSingleThreadScheduledExecutor();
        String projectId = "junk";
        var simpleActivitiesFactory = new SimpleActivitiesFactory(
                new ProjectAssignment(projectId, "localhost", AssignmentSeqNo.EMPTY),
                new UnrollExecutorStub(executor),
                new EvaluationAssignmentServiceStub(new ManualClock(), executor),
                new StatefulNotificationChannelFactoryStub(executor, projectId),
                new MuteMatcherStub());
        templateActivityFactory = new TemplateActivityFactory(
                alertTemplateDao = new CachedAlertTemplateDao(new InMemoryAlertTemplateDao(true)),
                new TemplateAlertFactory(new MustacheTemplateFactory()),
                simpleActivitiesFactory);
    }

    @Test
    public void makeActivity_fetchedTemplate() {
        AlertTemplate template = AlertTemplateDaoTest.expressionTemplate();
        alertTemplateDao.create(template).join();
        alertTemplateDao.getAll().join();

        var activity = templateActivityFactory.makeActivity(AlertTestSupport.alertFromTemplatePersistent(ThreadLocalRandom.current()).toBuilder()
                .setTemplateId(template.getId())
                .setTemplateVersionTag(template.getTemplateVersionTag())
                .build()).join();

        assertEquals(template.getId(), ((AlertFromTemplatePersistent) activity.getAlert()).getTemplateId());
        assertEquals(template.getTemplateVersionTag(), ((AlertFromTemplatePersistent) activity.getAlert()).getTemplateVersionTag());

        Map<AlertTemplateId, AlertTemplate> alertTemplatesState = alertTemplateDao.getCache();
        assertEquals(1, alertTemplatesState.size());
        assertTrue(template.equalContent(alertTemplatesState.get(template.getCompositeId())));
    }

    @Test
    public void fetchAll() {
        alertTemplateDao.create(AlertTemplateDaoTest.thresholdTemplate()).join();
        alertTemplateDao.create(AlertTemplateDaoTest.expressionTemplate()).join();

        alertTemplateDao.getAll().join();

        Map<AlertTemplateId, AlertTemplate> alertTemplatesState = alertTemplateDao.getCache();
        assertEquals(2, alertTemplatesState.size());
        assertTrue(AlertTemplateDaoTest.thresholdTemplate().equalContent(alertTemplatesState.get(AlertTemplateDaoTest.thresholdTemplate().getCompositeId())));
        assertTrue(AlertTemplateDaoTest.expressionTemplate().equalContent(alertTemplatesState.get(AlertTemplateDaoTest.expressionTemplate().getCompositeId())));
    }
}
