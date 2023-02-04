package ru.yandex.solomon.alert.cluster.broker.alert.activity.search;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;

import com.google.common.collect.ImmutableList;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.monlib.metrics.labels.Labels;
import ru.yandex.solomon.alert.cluster.broker.alert.activity.ActivityFactory;
import ru.yandex.solomon.alert.cluster.broker.alert.activity.AlertActivity;
import ru.yandex.solomon.alert.cluster.broker.alert.activity.SimpleActivitiesFactory;
import ru.yandex.solomon.alert.cluster.broker.alert.activity.SubAlertActivity;
import ru.yandex.solomon.alert.cluster.broker.alert.activity.TemplateActivityFactory;
import ru.yandex.solomon.alert.cluster.broker.alert.activity.TemplateAlertFactory;
import ru.yandex.solomon.alert.cluster.broker.evaluation.EvaluationAssignmentServiceStub;
import ru.yandex.solomon.alert.cluster.broker.mute.MuteMatcherStub;
import ru.yandex.solomon.alert.cluster.broker.notification.StatefulNotificationChannelFactoryStub;
import ru.yandex.solomon.alert.cluster.project.ProjectAssignment;
import ru.yandex.solomon.alert.dao.memory.InMemoryAlertTemplateDao;
import ru.yandex.solomon.alert.domain.Alert;
import ru.yandex.solomon.alert.domain.AlertState;
import ru.yandex.solomon.alert.domain.AlertTestSupport;
import ru.yandex.solomon.alert.domain.SubAlert;
import ru.yandex.solomon.alert.domain.expression.ExpressionAlert;
import ru.yandex.solomon.alert.domain.template.AlertFromTemplatePersistent;
import ru.yandex.solomon.alert.domain.threshold.ThresholdAlert;
import ru.yandex.solomon.alert.protobuf.EOrderDirection;
import ru.yandex.solomon.alert.protobuf.TListAlertRequest;
import ru.yandex.solomon.alert.protobuf.TListSubAlertRequest;
import ru.yandex.solomon.alert.template.MustacheTemplateFactory;
import ru.yandex.solomon.alert.unroll.UnrollExecutorStub;
import ru.yandex.solomon.balancer.AssignmentSeqNo;
import ru.yandex.solomon.ut.ManualClock;

import static org.junit.Assert.assertArrayEquals;

/**
 * @author Vladimir Gordiychuk
 */
public class ActivitySortsTest {
    private ScheduledExecutorService executor;
    private ActivityFactory factory;
    private List<AlertActivity> source = new ArrayList<>();
    private List<SubAlertActivity> subAlertSource = new ArrayList<>();
    private SimpleActivitiesFactory simpleActivitiesFactory;

    @Before
    public void setUp() throws Exception {
        executor = Executors.newSingleThreadScheduledExecutor();
        String projectId = "junk";
        simpleActivitiesFactory = new SimpleActivitiesFactory(
            new ProjectAssignment("junk", "localhost", AssignmentSeqNo.EMPTY),
            new UnrollExecutorStub(executor),
            new EvaluationAssignmentServiceStub(new ManualClock(), executor),
            new StatefulNotificationChannelFactoryStub(executor, projectId),
            new MuteMatcherStub());
        var templateActivityFactory = new TemplateActivityFactory(
                new InMemoryAlertTemplateDao(true),
                new TemplateAlertFactory(new MustacheTemplateFactory()),
                simpleActivitiesFactory);
        factory = new ActivityFactory(simpleActivitiesFactory, templateActivityFactory);
    }

    @After
    public void tearDown() throws Exception {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    @Test
    public void byName() {
        Alert a = AlertTestSupport.randomAlert()
            .toBuilder()
            .setName("a")
            .build();
        addAlert(a);

        Alert b = AlertTestSupport.randomAlert()
            .toBuilder()
            .setName("b")
            .build();
        addAlert(b);

        Alert c = AlertTestSupport.randomAlert()
            .toBuilder()
            .setName("c")
            .build();
        addAlert(c);

        Alert d = AlertTestSupport.randomAlert()
            .toBuilder()
            .setName("d")
            .build();
        addAlert(d);

        Collections.shuffle(source);

        Alert[] asc = source.stream()
            .sorted(ActivitySorts.orderByName(EOrderDirection.ASC))
            .map(AlertActivity::getAlert)
            .toArray(Alert[]::new);

        Alert[] desc = source.stream()
            .sorted(ActivitySorts.orderByName(EOrderDirection.DESC))
            .map(AlertActivity::getAlert)
            .toArray(Alert[]::new);

        assertArrayEquals(new Alert[]{a, b, c, d}, asc);
        assertArrayEquals(new Alert[]{d, c, b, a}, desc);
    }

    @Test
    public void byType() {
        ThresholdAlert threshold = AlertTestSupport.randomThresholdAlert(ThreadLocalRandom.current());
        addAlert(threshold);

        ExpressionAlert expression = AlertTestSupport.randomExpressionAlert(ThreadLocalRandom.current());
        addAlert(expression);

        AlertFromTemplatePersistent alertFromTemplatePersistent = AlertTestSupport.alertFromTemplatePersistent(ThreadLocalRandom.current());
        addAlert(alertFromTemplatePersistent);

        Collections.shuffle(source);

        Alert[] asc = source.stream()
            .sorted(ActivitySorts.orderByType(EOrderDirection.ASC))
            .map(AlertActivity::getAlert)
            .toArray(Alert[]::new);

        Alert[] desc = source.stream()
            .sorted(ActivitySorts.orderByType(EOrderDirection.DESC))
            .map(AlertActivity::getAlert)
            .toArray(Alert[]::new);

        assertArrayEquals(new Alert[]{threshold, expression, alertFromTemplatePersistent}, asc);
        assertArrayEquals(new Alert[]{alertFromTemplatePersistent, expression, threshold}, desc);
    }

    @Test
    public void byState() {
        ThresholdAlert active = AlertTestSupport.randomThresholdAlert(ThreadLocalRandom.current())
            .toBuilder()
            .setState(AlertState.ACTIVE)
            .build();
        addAlert(active);

        ExpressionAlert muted = AlertTestSupport.randomExpressionAlert(ThreadLocalRandom.current())
            .toBuilder()
            .setState(AlertState.MUTED)
            .build();
        addAlert(muted);

        Collections.shuffle(source);

        Alert[] asc = source.stream()
            .sorted(ActivitySorts.orderByState(EOrderDirection.ASC))
            .map(AlertActivity::getAlert)
            .toArray(Alert[]::new);

        Alert[] desc = source.stream()
            .sorted(ActivitySorts.orderByState(EOrderDirection.DESC))
            .map(AlertActivity::getAlert)
            .toArray(Alert[]::new);

        assertArrayEquals(new Alert[]{active, muted}, asc);
        assertArrayEquals(new Alert[]{muted, active}, desc);
    }

    @Test
    public void defaultSortByName() {
        Alert a = AlertTestSupport.randomAlert()
            .toBuilder()
            .setName("a")
            .build();
        addAlert(a);

        Alert b = AlertTestSupport.randomAlert()
            .toBuilder()
            .setName("b")
            .build();
        addAlert(b);

        Alert c = AlertTestSupport.randomAlert()
            .toBuilder()
            .setName("c")
            .build();
        addAlert(c);

        Collections.shuffle(source);
        Alert[] result = orderBy(TListAlertRequest.getDefaultInstance());
        assertArrayEquals(new Alert[]{a, b, c}, result);
    }

    @Test
    public void byNameViaRequest() {
        Alert a = AlertTestSupport.randomAlert()
            .toBuilder()
            .setName("a")
            .build();
        addAlert(a);

        Alert b = AlertTestSupport.randomAlert()
            .toBuilder()
            .setName("b")
            .build();
        addAlert(b);

        Alert c = AlertTestSupport.randomAlert()
            .toBuilder()
            .setName("c")
            .build();
        addAlert(c);

        Collections.shuffle(source);
        Alert[] asc = orderBy(TListAlertRequest.newBuilder()
            .setOrderByName(EOrderDirection.ASC)
            .build());

        Alert[] desc = orderBy(TListAlertRequest.newBuilder()
            .setOrderByName(EOrderDirection.DESC)
            .build());

        assertArrayEquals(new Alert[]{a, b, c}, asc);
        assertArrayEquals(new Alert[]{c, b, a}, desc);
    }

    @Test
    public void byLabels() {
        Alert parent = AlertTestSupport.randomThresholdAlert(ThreadLocalRandom.current())
                .toBuilder()
                .setSelectors("project=solomon, cluster=local, service=test, name=*")
                .setGroupByLabels(ImmutableList.of("name"))
                .build();

        AlertActivity parentActivity = factory.makeActivity(parent).join();
        SubAlert a = SubAlert.newBuilder()
                .setParent(parent)
                .setGroupKey(Labels.of("name", "a"))
                .build();
        addSubAlert(parentActivity, a);

        SubAlert b = SubAlert.newBuilder()
                .setParent(parent)
                .setGroupKey(Labels.of("name", "b"))
                .build();
        addSubAlert(parentActivity, b);

        SubAlert c = SubAlert.newBuilder()
                .setParent(parent)
                .setGroupKey(Labels.of("name", "c"))
                .build();
        addSubAlert(parentActivity, c);

        Collections.shuffle(subAlertSource);
        SubAlert[] asc = orderBy(TListSubAlertRequest.newBuilder()
                .setOrderByLabels(EOrderDirection.ASC)
                .build());

        SubAlert[] desc = orderBy(TListSubAlertRequest.newBuilder()
                .setOrderByLabels(EOrderDirection.DESC)
                .build());

        assertArrayEquals(new SubAlert[]{a, b, c}, asc);
        assertArrayEquals(new SubAlert[]{c, b, a}, desc);
    }

    @Test
    public void subAlertsSortByLabelsNotViolateCompareContract() {
        Alert parent = AlertTestSupport.randomThresholdAlert(ThreadLocalRandom.current())
                .toBuilder()
                .setSelectors("project=rorewillo-test, cluster=cluster, service=push_service")
                .setGroupByLabels(ImmutableList.of("Os", "TestId"))
                .build();

        AlertActivity parentActivity = factory.makeActivity(parent).join();
        SubAlert a = SubAlert.newBuilder()
                .setParent(parent)
                .setGroupKey(Labels.of("TestId", "a"))
                .build();
        addSubAlert(parentActivity, a);

        SubAlert b = SubAlert.newBuilder()
                .setParent(parent)
                .setGroupKey(Labels.of("TestId", "b"))
                .build();
        addSubAlert(parentActivity, b);

        SubAlert c = SubAlert.newBuilder()
                .setParent(parent)
                .setGroupKey(Labels.of("Os", "Linux", "TestId", "a"))
                .build();
        addSubAlert(parentActivity, c);

        SubAlert d = SubAlert.newBuilder()
                .setParent(parent)
                .setGroupKey(Labels.of("Os", "Linux", "TestId", "b"))
                .build();
        addSubAlert(parentActivity, d);

        SubAlert e = SubAlert.newBuilder()
                .setParent(parent)
                .setGroupKey(Labels.of("Os", "MaxOs", "TestId", "a"))
                .build();
        addSubAlert(parentActivity, e);

        SubAlert[] asc = orderBy(TListSubAlertRequest.newBuilder()
                .setOrderByLabels(EOrderDirection.ASC)
                .build());

        SubAlert[] desc = orderBy(TListSubAlertRequest.newBuilder()
                .setOrderByLabels(EOrderDirection.DESC)
                .build());

        assertArrayEquals(new SubAlert[]{a, b, c, d, e}, asc);
        assertArrayEquals(new SubAlert[]{e, d, c, b, a}, desc);
    }

    @Test
    public void byDefaultSubAlertOrderedByLabels() {
        Alert parent = AlertTestSupport.randomThresholdAlert(ThreadLocalRandom.current())
                .toBuilder()
                .setSelectors("project=solomon, cluster=local, service=test, name=*")
                .setGroupByLabels(ImmutableList.of("name"))
                .build();

        AlertActivity parentActivity = factory.makeActivity(parent).join();
        SubAlert a = SubAlert.newBuilder()
                .setParent(parent)
                .setGroupKey(Labels.of("name", "a"))
                .build();
        addSubAlert(parentActivity, a);

        SubAlert b = SubAlert.newBuilder()
                .setParent(parent)
                .setGroupKey(Labels.of("name", "b"))
                .build();
        addSubAlert(parentActivity, b);

        SubAlert c = SubAlert.newBuilder()
                .setParent(parent)
                .setGroupKey(Labels.of("name", "c"))
                .build();
        addSubAlert(parentActivity, c);

        Collections.shuffle(source);
        SubAlert[] defAsc = orderBy(TListSubAlertRequest.newBuilder().build());
        assertArrayEquals(new SubAlert[]{a, b, c}, defAsc);
    }

    private Alert[] orderBy(TListAlertRequest request) {
        return source.stream()
            .sorted(ActivitySorts.orderBy(request))
            .map(AlertActivity::getAlert)
            .toArray(Alert[]::new);
    }

    private SubAlert[] orderBy(TListSubAlertRequest request) {
        return subAlertSource.stream()
                .sorted(ActivitySorts.orderBy(request))
                .map(SubAlertActivity::getAlert)
                .toArray(SubAlert[]::new);
    }

    private void addAlert(Alert alert) {
        source.add(factory.makeActivity(alert).join());
    }

    private void addSubAlert(AlertActivity parent, SubAlert alert) {
        subAlertSource.add(new SubAlertActivity(alert, parent, simpleActivitiesFactory));
    }
}
