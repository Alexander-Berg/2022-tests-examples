package ru.yandex.solomon.alert.cluster.broker.alert.activity.search;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.monlib.metrics.labels.Labels;
import ru.yandex.solomon.alert.EvaluationStatus;
import ru.yandex.solomon.alert.api.converters.NotificationConverter;
import ru.yandex.solomon.alert.cluster.broker.alert.activity.ActivityFactory;
import ru.yandex.solomon.alert.cluster.broker.alert.activity.AlertActivity;
import ru.yandex.solomon.alert.cluster.broker.alert.activity.MultiAlertActivity;
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
import ru.yandex.solomon.alert.domain.AlertType;
import ru.yandex.solomon.alert.domain.SubAlert;
import ru.yandex.solomon.alert.domain.expression.ExpressionAlert;
import ru.yandex.solomon.alert.domain.template.AlertFromTemplatePersistent;
import ru.yandex.solomon.alert.domain.threshold.ThresholdAlert;
import ru.yandex.solomon.alert.notification.channel.NotificationStatus;
import ru.yandex.solomon.alert.notification.channel.telegram.ChatIdResolverStub;
import ru.yandex.solomon.alert.protobuf.EAlertState;
import ru.yandex.solomon.alert.protobuf.TEvaluationStatus;
import ru.yandex.solomon.alert.protobuf.TListAlertRequest;
import ru.yandex.solomon.alert.protobuf.TListSubAlertRequest;
import ru.yandex.solomon.alert.protobuf.TNotificationStatus;
import ru.yandex.solomon.alert.template.MustacheTemplateFactory;
import ru.yandex.solomon.alert.unroll.UnrollExecutorStub;
import ru.yandex.solomon.balancer.AssignmentSeqNo;
import ru.yandex.solomon.labels.query.Selectors;
import ru.yandex.solomon.model.protobuf.MatchType;
import ru.yandex.solomon.model.protobuf.Selector;
import ru.yandex.solomon.ut.ManualClock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * @author Vladimir Gordiychuk
 */
public class ActivityFiltersTest {
    private ScheduledExecutorService executor;
    private ActivityFactory factory;
    private List<AlertActivity> source = new ArrayList<>();
    private ActivityFilters activityFilters;
    private AlertPersistStateSupport stateSupport;

    @Before
    public void setUp() throws Exception {
        executor = Executors.newSingleThreadScheduledExecutor();
        String projectId = "junk";
        var simpleActivitiesFactory = new SimpleActivitiesFactory(
                new ProjectAssignment(projectId, "localhost", AssignmentSeqNo.EMPTY),
                new UnrollExecutorStub(executor),
                new EvaluationAssignmentServiceStub(new ManualClock(), executor),
                new StatefulNotificationChannelFactoryStub(executor, projectId),
                new MuteMatcherStub());
        var templateActivityFactory = new TemplateActivityFactory(
                new InMemoryAlertTemplateDao(true),
                new TemplateAlertFactory(new MustacheTemplateFactory()),
                simpleActivitiesFactory);
        factory = new ActivityFactory(simpleActivitiesFactory, templateActivityFactory);
        NotificationConverter notificationConverter = new NotificationConverter(new ChatIdResolverStub());
        stateSupport = new AlertPersistStateSupport(notificationConverter);
        activityFilters = new ActivityFilters(notificationConverter);
    }

    @After
    public void tearDown() throws Exception {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    @Test
    public void byName() {
        for (int index = 0; index < 10; index++) {
            addAlert(AlertTestSupport.randomAlert());
        }
        Alert expected = AlertTestSupport.randomAlert()
                .toBuilder()
                .setName("fetcher-oom-per-shard")
                .build();
        addAlert(expected);

        List<Alert> result = source.stream()
                .filter(activityFilters.filterByName("oom"))
                .map(AlertActivity::getAlert)
                .collect(Collectors.toList());

        assertThat(result, iterableWithSize(1));
        assertThat(result, hasItem(expected));
    }

    @Test
    public void byName_failedActivity() {
        for (int index = 0; index < 10; index++) {
            addAlert(AlertTestSupport.randomAlert());
        }
        Alert expected = AlertTestSupport.randomAlert()
                .toBuilder()
                .setName("fetcher-oom-per-shard")
                .build();
        AlertActivity activity = factory.makeFailedActivity(expected, new IllegalArgumentException());
        source.add(activity);

        List<Alert> result = source.stream()
                .filter(activityFilters.filterByName("oom"))
                .map(AlertActivity::getAlert)
                .collect(Collectors.toList());

        assertThat(result, iterableWithSize(1));
        assertThat(result, hasItem(expected));
    }

    @Test
    public void byServiceProvider() {
        for (int index = 0; index < 10; index++) {
            addAlert(AlertTestSupport.randomAlert());
        }
        Alert expected = AlertTestSupport.alertFromTemplatePersistent(ThreadLocalRandom.current())
                .toBuilder()
                .setServiceProvider("provider")
                .build();
        addAlert(expected);

        List<Alert> result = source.stream()
                .filter(activityFilters.filterByCreatedServiceProvider("provider"))
                .map(AlertActivity::getAlert)
                .collect(Collectors.toList());

        assertThat(result, iterableWithSize(1));
        assertThat(result, hasItem(expected));
    }

    @Test
    public void byTemplateServiceProvider() {
        var threshold = AlertTestSupport.randomThresholdAlert(ThreadLocalRandom.current());
        var expression = AlertTestSupport.randomExpressionAlert(ThreadLocalRandom.current());
        var template = AlertTestSupport.alertFromTemplatePersistent(ThreadLocalRandom.current());
        addAlert(threshold);
        addAlert(expression);
        addAlert(template);

        List<Alert> result = source.stream()
                .filter(activityFilters.filterByTemplateServiceProvider("SERVICE_PROVIDER"))
                .map(AlertActivity::getAlert)
                .collect(Collectors.toList());

        assertThat(result, iterableWithSize(1));
        assertThat(result, hasItem(template));

        result = source.stream()
                .filter(activityFilters.filterByTemplateServiceProvider("-"))
                .map(AlertActivity::getAlert)
                .collect(Collectors.toList());

        assertThat(result, iterableWithSize(2));
        assertThat(result, hasItem(threshold));
        assertThat(result, hasItem(expression));
    }

    @Test
    public void byFolder() {
        for (int index = 0; index < 100; index++) {
            addAlert(AlertTestSupport.randomAlert().toBuilder()
                    .setFolderId("folder-" + index)
                    .build());
        }

        List<Alert> result = source.stream()
                .filter(activityFilters.filterBy(TListAlertRequest.newBuilder()
                        .setFolderId("folder-42")
                        .build()))
                .map(AlertActivity::getAlert)
                .collect(Collectors.toList());

        assertThat(result, iterableWithSize(1));
    }

    @Test
    public void byType() {
        ThresholdAlert threshold1 = AlertTestSupport.randomThresholdAlert(ThreadLocalRandom.current());
        ThresholdAlert threshold2 = AlertTestSupport.randomThresholdAlert(ThreadLocalRandom.current());
        ExpressionAlert expression = AlertTestSupport.randomExpressionAlert(ThreadLocalRandom.current());
        AlertFromTemplatePersistent alertFromTemplateExpression = AlertTestSupport.alertFromTemplatePersistent(ThreadLocalRandom.current(), true);
        AlertFromTemplatePersistent alertFromTemplateThreshold = AlertTestSupport.alertFromTemplatePersistent(ThreadLocalRandom.current(), false);

        addAlert(threshold1);
        addAlert(expression);
        addAlert(threshold2);
        addAlert(alertFromTemplateExpression);
        addAlert(alertFromTemplateThreshold);

        var all = source.stream()
                .filter(activityFilters.filterByType(EnumSet.of(AlertType.THRESHOLD, AlertType.EXPRESSION, AlertType.FROM_TEMPLATE)))
                .map(AlertActivity::getAlert)
                .collect(Collectors.toList());

        assertEquals(5, all.size());
        assertEquals(true, all.contains(threshold1));
        assertEquals(true, all.contains(expression));
        assertEquals(true, all.contains(threshold2));
        assertEquals(true, all.contains(alertFromTemplateExpression));
        assertEquals(true, all.contains(alertFromTemplateThreshold));

        var thresholds = source.stream()
                .filter(activityFilters.filterByType(EnumSet.of(AlertType.THRESHOLD)))
                .map(AlertActivity::getAlert)
                .collect(Collectors.toList());

        assertEquals(3, thresholds.size());
        assertEquals(true, thresholds.contains(threshold1));
        assertEquals(true, thresholds.contains(threshold2));
        assertEquals(true, thresholds.contains(alertFromTemplateThreshold));


        var expressions = source.stream()
                .filter(activityFilters.filterByType(EnumSet.of(AlertType.EXPRESSION)))
                .map(AlertActivity::getAlert)
                .collect(Collectors.toList());

        assertEquals(2, expressions.size());
        assertEquals(true, expressions.contains(expression));
        assertEquals(true, expressions.contains(alertFromTemplateExpression));

       var fromTemplate = source.stream()
                .filter(activityFilters.filterByType(EnumSet.of(AlertType.FROM_TEMPLATE)))
                .map(AlertActivity::getAlert)
                .collect(Collectors.toList());

        assertEquals(2, fromTemplate.size());
        assertEquals(true, fromTemplate.contains(alertFromTemplateThreshold));
        assertEquals(true, fromTemplate.contains(alertFromTemplateExpression));
    }

    @Test
    public void byState() {
        Alert mute1 = AlertTestSupport.randomAlert()
                .toBuilder()
                .setState(AlertState.MUTED)
                .build();
        Alert mute2 = AlertTestSupport.randomAlert()
                .toBuilder()
                .setState(AlertState.MUTED)
                .build();
        Alert active = AlertTestSupport.randomAlert()
                .toBuilder()
                .setState(AlertState.ACTIVE)
                .build();

        addAlert(mute1);
        addAlert(mute2);
        addAlert(active);

        Alert[] all = source.stream()
                .filter(activityFilters.filterByState(ImmutableList.of(EAlertState.ACTIVE, EAlertState.MUTED)))
                .map(AlertActivity::getAlert)
                .toArray(Alert[]::new);

        assertArrayEquals(new Alert[]{mute1, mute2, active}, all);

        Alert[] muted = source.stream()
                .filter(activityFilters.filterByState(ImmutableList.of(EAlertState.MUTED)))
                .map(AlertActivity::getAlert)
                .toArray(Alert[]::new);

        assertArrayEquals(new Alert[]{mute1, mute2}, muted);

        Alert[] actives = source.stream()
                .filter(activityFilters.filterByState(ImmutableList.of(EAlertState.ACTIVE)))
                .map(AlertActivity::getAlert)
                .toArray(Alert[]::new);

        assertArrayEquals(new Alert[]{active}, actives);
    }

    @Test
    public void byStatus() {
        Alert ok = AlertTestSupport.randomThresholdAlert(ThreadLocalRandom.current())
                .toBuilder()
                .setGroupByLabels(Collections.emptyList())
                .build();
        addAlert(ok).restore(stateSupport.state(ok, EvaluationStatus.OK));

        Alert error = AlertTestSupport.randomThresholdAlert(ThreadLocalRandom.current())
                .toBuilder()
                .setGroupByLabels(Collections.emptyList())
                .build();
        addAlert(error).restore(stateSupport.state(error, EvaluationStatus.ERROR));

        Alert multiOkAlarm = AlertTestSupport.randomThresholdAlert(ThreadLocalRandom.current())
                .toBuilder()
                .setSelectors("project=solomon, cluster=local, service=test, sensor=idleTime, host=*")
                .setGroupByLabel("host")
                .build();
        addAlert(multiOkAlarm)
                .restore(stateSupport.state(multiOkAlarm,
                        ImmutableMap.of(
                                Labels.of("host", "test-1"), EvaluationStatus.OK,
                                Labels.of("host", "test-2"), EvaluationStatus.OK,
                                Labels.of("host", "test-3"), EvaluationStatus.ALARM
                        )));

        {
            Alert[] oks = source.stream()
                    .filter(activityFilters.filterByEvaluationStatus(ImmutableList.of(TEvaluationStatus.ECode.OK)))
                    .map(AlertActivity::getAlert)
                    .toArray(Alert[]::new);

            assertArrayEquals(new Alert[]{ok, multiOkAlarm}, oks);
        }

        {
            Alert[] errors = source.stream()
                    .filter(activityFilters.filterByEvaluationStatus(ImmutableList.of(TEvaluationStatus.ECode.ERROR)))
                    .map(AlertActivity::getAlert)
                    .toArray(Alert[]::new);

            assertArrayEquals(new Alert[]{error}, errors);
        }

        {
            Alert[] alarms = source.stream()
                    .filter(activityFilters.filterByEvaluationStatus(ImmutableList.of(TEvaluationStatus.ECode.ALARM)))
                    .map(AlertActivity::getAlert)
                    .toArray(Alert[]::new);

            assertArrayEquals(new Alert[]{multiOkAlarm}, alarms);
        }
    }

    @Test
    public void byNotification() {
        Alert alice = AlertTestSupport.randomThresholdAlert(ThreadLocalRandom.current())
                .toBuilder()
                .setNotificationChannel("alice")
                .setGroupByLabels(Collections.emptyList())
                .build();

        Alert bob = AlertTestSupport.randomThresholdAlert(ThreadLocalRandom.current())
                .toBuilder()
                .setNotificationChannel("bob")
                .setGroupByLabels(Collections.emptyList())
                .build();

        addAlert(alice);
        addAlert(bob);

        {
            Alert[] result = source.stream()
                    .filter(activityFilters.filterByNotificationIds(ImmutableList.of("bob", "alice")))
                    .map(AlertActivity::getAlert)
                    .toArray(Alert[]::new);

            assertArrayEquals(new Alert[]{alice, bob}, result);
        }

        {
            Alert[] result = source.stream()
                    .filter(activityFilters.filterByNotificationIds(ImmutableList.of("alice")))
                    .map(AlertActivity::getAlert)
                    .toArray(Alert[]::new);

            assertArrayEquals(new Alert[]{alice}, result);
        }
    }

    @Test
    public void byNotificationStatus() {
        Alert alice = AlertTestSupport.randomThresholdAlert()
                .toBuilder()
                .setName("alice")
                .setNotificationChannel("channel-0")
                .setGroupByLabels(Collections.emptyList())
                .build();
        addAlert(alice).restore(stateSupport.state(alice, EvaluationStatus.OK, NotificationStatus.SUCCESS));

        Alert bob = AlertTestSupport.randomThresholdAlert()
                .toBuilder()
                .setName("bob")
                .setNotificationChannels(Arrays.asList("channel-0", "channel-1"))
                .setGroupByLabels(Collections.emptyList())
                .build();
        addAlert(bob).restore(stateSupport.state(bob, EvaluationStatus.OK, NotificationStatus.INVALID_REQUEST, NotificationStatus.RESOURCE_EXHAUSTED));

        Alert eva = AlertTestSupport.randomThresholdAlert()
                .toBuilder()
                .setName("eva")
                .setNotificationChannels(Collections.emptyList())
                .setGroupByLabels(Collections.emptyList())
                .build();
        addAlert(eva);

        {
            Alert[] result = source.stream()
                    .filter(activityFilters.filterByNotificationStatus(Collections.emptySet(), ImmutableList.of(TNotificationStatus.ECode.SUCCESS)))
                    .map(AlertActivity::getAlert)
                    .toArray(Alert[]::new);

            assertArrayEquals(new Alert[]{alice}, result);
        }

        {
            Alert[] result = source.stream()
                    .filter(activityFilters.filterByNotificationStatus(Collections.emptySet(), ImmutableList.of(TNotificationStatus.ECode.RESOURCE_EXHAUSTED)))
                    .map(AlertActivity::getAlert)
                    .toArray(Alert[]::new);

            assertArrayEquals(new Alert[]{bob}, result);
        }

        {
            Alert[] result = source.stream()
                    .filter(activityFilters.filterByNotificationStatus(Collections.emptySet(), ImmutableList.of(TNotificationStatus.ECode.SUCCESS, TNotificationStatus.ECode.INVALID_REQUEST)))
                    .map(AlertActivity::getAlert)
                    .toArray(Alert[]::new);

            assertArrayEquals(new Alert[]{alice, bob}, result);
        }

        {
            Alert[] result = source.stream()
                    .filter(activityFilters.filterByNotificationStatus(Collections.emptySet(), ImmutableList.of(TNotificationStatus.ECode.OBSOLETE)))
                    .map(AlertActivity::getAlert)
                    .toArray(Alert[]::new);

            assertArrayEquals(new Alert[0], result);
        }
    }

    @Test
    public void byNotificationStatusMultiAlert() {
        Alert alice = AlertTestSupport.randomThresholdAlert(ThreadLocalRandom.current())
                .toBuilder()
                .setSelectors("project=solomon, cluster=local, service=test, sensor=freeSpace, host=*")
                .setGroupByLabels(ImmutableList.of("host"))
                .setNotificationChannel("channel-0")
                .build();
        addAlert(alice).restore(stateSupport.stateNotification(alice, ImmutableMap.of(Labels.of("host", "test"), ImmutableList.of(NotificationStatus.SUCCESS))));

        Alert bob = AlertTestSupport.randomThresholdAlert(ThreadLocalRandom.current())
                .toBuilder()
                .setSelectors("project=solomon, cluster=local, service=test, sensor=freeMemory, host=*")
                .setGroupByLabels(ImmutableList.of("host"))
                .setNotificationChannels(ImmutableList.of("channel-0", "channel-1"))
                .build();

        addAlert(bob).restore(stateSupport.stateNotification(bob, ImmutableMap.of(
                Labels.of("host", "test-1"), ImmutableList.of(NotificationStatus.ERROR, NotificationStatus.SUCCESS),
                Labels.of("host", "test-2"), ImmutableList.of(NotificationStatus.SUCCESS, NotificationStatus.SUCCESS)
        )));

        Alert eva = AlertTestSupport.randomThresholdAlert(ThreadLocalRandom.current())
                .toBuilder()
                .setSelectors("project=solomon, cluster=local, service=test, sensor=utime, host=*")
                .setGroupByLabels(ImmutableList.of("host"))
                .build();
        addAlert(eva);

        {
            Alert[] result = source.stream()
                    .filter(activityFilters.filterByNotificationStatus(Collections.emptySet(), ImmutableList.of(TNotificationStatus.ECode.OBSOLETE)))
                    .map(AlertActivity::getAlert)
                    .toArray(Alert[]::new);

            assertArrayEquals(new Alert[0], result);
        }

        {
            Alert[] result = source.stream()
                    .filter(activityFilters.filterByNotificationStatus(Collections.emptySet(), ImmutableList.of(TNotificationStatus.ECode.ERROR)))
                    .map(AlertActivity::getAlert)
                    .toArray(Alert[]::new);

            assertArrayEquals(new Alert[]{bob}, result);
        }

        {
            Alert[] result = source.stream()
                    .filter(activityFilters.filterByNotificationStatus(ImmutableSet.of("channel-1"), ImmutableList.of(TNotificationStatus.ECode.ERROR)))
                    .map(AlertActivity::getAlert)
                    .toArray(Alert[]::new);

            assertArrayEquals(new Alert[0], result);
        }

        {
            Alert[] result = source.stream()
                    .filter(activityFilters.filterByNotificationStatus(ImmutableSet.of("channel-0"), ImmutableList.of(TNotificationStatus.ECode.ERROR)))
                    .map(AlertActivity::getAlert)
                    .toArray(Alert[]::new);

            assertArrayEquals(new Alert[]{bob}, result);
        }

        {
            Alert[] result = source.stream()
                    .filter(activityFilters.filterByNotificationStatus(Collections.emptySet(), ImmutableList.of(TNotificationStatus.ECode.SUCCESS)))
                    .map(AlertActivity::getAlert)
                    .toArray(Alert[]::new);

            assertArrayEquals(new Alert[]{alice, bob}, result);
        }
    }

    @Test
    public void subAlertBySelector() {
        Map<Labels, EvaluationStatus> child = ImmutableMap.<Labels, EvaluationStatus>builder()
                .put(Labels.of("host", "solomon-1", "disk", "/dev/sda1"), EvaluationStatus.OK)
                .put(Labels.of("host", "solomon-2", "disk", "/dev/sda2"), EvaluationStatus.OK)
                .put(Labels.of("host", "solomon-3", "disk", "/dev/sda2"), EvaluationStatus.OK)
                .build();

        Alert parent = AlertTestSupport.randomThresholdAlert(ThreadLocalRandom.current())
                .toBuilder()
                .setSelectors("project=solomon, cluster=local, service=test, sensor=freeSpace, host=*, disk=*")
                .setGroupByLabels(ImmutableList.of("host", "disk"))
                .build();

        MultiAlertActivity activity = (MultiAlertActivity) addAlert(parent);
        activity.restore(stateSupport.state(parent, child));

        {
            Labels[] result = activity.getSubActivities()
                    .stream()
                    .filter(activityFilters.filterSubAlertBySelector(Selectors.parse("disk=/dev/sda1")))
                    .map(SubAlertActivity::getAlert)
                    .map(SubAlert::getGroupKey)
                    .toArray(Labels[]::new);

            Labels[] expect = {Labels.of("host", "solomon-1", "disk", "/dev/sda1")};
            assertArrayEquals(expect, result);
        }

        {
            Labels[] result = activity.getSubActivities()
                    .stream()
                    .filter(activityFilters.filterSubAlertBySelector(Selectors.parse("host=solomon-2")))
                    .map(SubAlertActivity::getAlert)
                    .map(SubAlert::getGroupKey)
                    .toArray(Labels[]::new);

            Labels[] expect = {Labels.of("host", "solomon-2", "disk", "/dev/sda2")};
            assertArrayEquals(expect, result);
        }
    }

    @Test
    public void subAlertsByEvaluationStatus() {
        Map<Labels, EvaluationStatus> child = ImmutableMap.<Labels, EvaluationStatus>builder()
                .put(Labels.of("host", "ok"), EvaluationStatus.OK)
                .put(Labels.of("host", "alarm"), EvaluationStatus.ALARM)
                .put(Labels.of("host", "alarm-2"), EvaluationStatus.ALARM)
                .put(Labels.of("host", "error"), EvaluationStatus.ERROR)
                .build();

        Alert parent = AlertTestSupport.randomThresholdAlert(ThreadLocalRandom.current())
                .toBuilder()
                .setSelectors("project=solomon, cluster=local, service=test, sensor=freeSpace, host=*, disk=*")
                .setGroupByLabels(ImmutableList.of("host", "disk"))
                .build();

        MultiAlertActivity activity = (MultiAlertActivity) addAlert(parent);
        activity.restore(stateSupport.state(parent, child));

        {
            Labels[] result = activity.getSubActivities()
                    .stream()
                    .filter(activityFilters.filterSubAlertByEvaluationStatus(ImmutableList.of(TEvaluationStatus.ECode.OK)))
                    .map(SubAlertActivity::getAlert)
                    .map(SubAlert::getGroupKey)
                    .toArray(Labels[]::new);

            Labels[] expect = {Labels.of("host", "ok")};
            assertArrayEquals(expect, result);
        }

        {
            Labels[] result = activity.getSubActivities()
                    .stream()
                    .filter(activityFilters.filterSubAlertByEvaluationStatus(ImmutableList.of(TEvaluationStatus.ECode.ERROR)))
                    .map(SubAlertActivity::getAlert)
                    .map(SubAlert::getGroupKey)
                    .toArray(Labels[]::new);

            Labels[] expect = {Labels.of("host", "error")};
            assertArrayEquals(expect, result);
        }
    }

    @Test
    public void subAlertBySelectorViaRequest() {
        Map<Labels, EvaluationStatus> child = ImmutableMap.<Labels, EvaluationStatus>builder()
                .put(Labels.of("host", "solomon-1", "disk", "/dev/sda1"), EvaluationStatus.OK)
                .put(Labels.of("host", "solomon-2", "disk", "/dev/sda2"), EvaluationStatus.OK)
                .put(Labels.of("host", "solomon-3", "disk", "/dev/sda2"), EvaluationStatus.OK)
                .build();

        Alert parent = AlertTestSupport.randomThresholdAlert(ThreadLocalRandom.current())
                .toBuilder()
                .setSelectors("project=solomon, cluster=local, service=test, sensor=freeSpace, host=*, disk=*")
                .setGroupByLabels(ImmutableList.of("host", "disk"))
                .build();

        MultiAlertActivity activity = (MultiAlertActivity) addAlert(parent);
        activity.restore(stateSupport.state(parent, child));

        TListSubAlertRequest request = TListSubAlertRequest.newBuilder()
                .addFilterByLabels(Selector.newBuilder()
                        .setKey("disk")
                        .setMatchType(MatchType.GLOB)
                        .setPattern("/dev/sda*")
                        .build())
                .addFilterByLabels(Selector.newBuilder()
                        .setKey("host")
                        .setMatchType(MatchType.EXACT)
                        .setPattern("solomon-1")
                        .build())
                .build();

        Labels[] result = activity.getSubActivities()
                .stream()
                .filter(activityFilters.filterBy(request))
                .map(SubAlertActivity::getAlert)
                .map(SubAlert::getGroupKey)
                .toArray(Labels[]::new);

        Labels[] expect = {Labels.of("host", "solomon-1", "disk", "/dev/sda1")};
        assertArrayEquals(expect, result);
    }

    @Test
    public void subAlertByStatusCodeViaRequest() {
        Map<Labels, EvaluationStatus> child = ImmutableMap.<Labels, EvaluationStatus>builder()
                .put(Labels.of("host", "ok-2"), EvaluationStatus.OK)
                .put(Labels.of("host", "alarm"), EvaluationStatus.ALARM)
                .put(Labels.of("host", "error"), EvaluationStatus.ERROR)
                .build();

        Alert parent = AlertTestSupport.randomThresholdAlert(ThreadLocalRandom.current())
                .toBuilder()
                .setSelectors("project=solomon, cluster=local, service=test, sensor=freeSpace, host=*, disk=*")
                .setGroupByLabels(ImmutableList.of("host", "disk"))
                .build();

        MultiAlertActivity activity = (MultiAlertActivity) addAlert(parent);
        activity.restore(stateSupport.state(parent, child));

        TListSubAlertRequest request = TListSubAlertRequest.newBuilder()
                .addFilterByEvaluationStatus(TEvaluationStatus.ECode.ALARM)
                .build();

        Labels[] result = activity.getSubActivities()
                .stream()
                .filter(activityFilters.filterBy(request))
                .map(SubAlertActivity::getAlert)
                .map(SubAlert::getGroupKey)
                .toArray(Labels[]::new);

        Labels[] expect = {Labels.of("host", "alarm")};
        assertArrayEquals(expect, result);
    }

    @Test
    public void subAlertByStatusCodeAndSelector() {
        Map<Labels, EvaluationStatus> child = ImmutableMap.<Labels, EvaluationStatus>builder()
                .put(Labels.of("host", "ok-2"), EvaluationStatus.OK)
                .put(Labels.of("host", "alarm"), EvaluationStatus.ALARM)
                .put(Labels.of("host", "error"), EvaluationStatus.ERROR)
                .build();

        Alert parent = AlertTestSupport.randomThresholdAlert(ThreadLocalRandom.current())
                .toBuilder()
                .setSelectors("project=solomon, cluster=local, service=test, sensor=freeSpace, host=*, disk=*")
                .setGroupByLabels(ImmutableList.of("host", "disk"))
                .build();

        MultiAlertActivity activity = (MultiAlertActivity) addAlert(parent);
        activity.restore(stateSupport.state(parent, child));

        {
            TListSubAlertRequest request = TListSubAlertRequest.newBuilder()
                    .addFilterByLabels(Selector.newBuilder()
                            .setKey("disk")
                            .setMatchType(MatchType.EXACT)
                            .setPattern("/dev/sda1")
                            .build())
                    .addFilterByEvaluationStatus(TEvaluationStatus.ECode.ALARM)
                    .build();

            Labels[] result = activity.getSubActivities()
                    .stream()
                    .filter(activityFilters.filterBy(request))
                    .map(SubAlertActivity::getAlert)
                    .map(SubAlert::getGroupKey)
                    .toArray(Labels[]::new);

            assertArrayEquals(new Labels[0], result);
        }

        {
            TListSubAlertRequest request = TListSubAlertRequest.newBuilder()
                    .addFilterByLabels(Selector.newBuilder()
                            .setKey("host")
                            .setMatchType(MatchType.GLOB)
                            .setPattern("a*")
                            .build())
                    .addFilterByEvaluationStatus(TEvaluationStatus.ECode.ALARM)
                    .build();

            Labels[] result = activity.getSubActivities()
                    .stream()
                    .filter(activityFilters.filterBy(request))
                    .map(SubAlertActivity::getAlert)
                    .map(SubAlert::getGroupKey)
                    .toArray(Labels[]::new);

            Labels[] expect = {Labels.of("host", "alarm")};
            assertArrayEquals(expect, result);
        }
    }

    @Test
    public void subAlertsByNotificationStatus() {
        Map<Labels, List<NotificationStatus>> child = ImmutableMap.<Labels, List<NotificationStatus>>builder()
                .put(Labels.of("host", "alice"), ImmutableList.of(NotificationStatus.SUCCESS, NotificationStatus.SUCCESS))
                .put(Labels.of("host", "bob"), ImmutableList.of(NotificationStatus.ERROR, NotificationStatus.SUCCESS))
                .put(Labels.of("host", "eva"), ImmutableList.of(NotificationStatus.RESOURCE_EXHAUSTED, NotificationStatus.INVALID_REQUEST))
                .build();

        Alert parent = AlertTestSupport.randomThresholdAlert(ThreadLocalRandom.current())
                .toBuilder()
                .setSelectors("project=solomon, cluster=local, service=test, sensor=freeSpace, host=*")
                .setGroupByLabels(ImmutableList.of("host"))
                .setNotificationChannels(Arrays.asList("channel-0", "channel-1"))
                .build();

        MultiAlertActivity activity = (MultiAlertActivity) addAlert(parent);
        activity.restore(stateSupport.stateNotification(parent, child));

        {
            TListSubAlertRequest request = TListSubAlertRequest.newBuilder()
                    .addFilterByNotificationStatus(TNotificationStatus.ECode.ERROR_ABLE_TO_RETRY)
                    .build();

            Labels[] result = activity.getSubActivities()
                    .stream()
                    .filter(activityFilters.filterBy(request))
                    .map(SubAlertActivity::getAlert)
                    .map(SubAlert::getGroupKey)
                    .toArray(Labels[]::new);

            assertArrayEquals(new Labels[0], result);
        }

        {
            TListSubAlertRequest request = TListSubAlertRequest.newBuilder()
                    .addFilterByNotificationStatus(TNotificationStatus.ECode.ERROR)
                    .build();

            Labels[] result = activity.getSubActivities()
                    .stream()
                    .filter(activityFilters.filterBy(request))
                    .map(SubAlertActivity::getAlert)
                    .map(SubAlert::getGroupKey)
                    .toArray(Labels[]::new);

            Labels[] expect = {Labels.of("host", "bob")};
            assertArrayEquals(expect, result);
        }
    }

    @Test
    public void byLabels() {
        for (int index = 0; index < 10; index++) {
            addAlert(AlertTestSupport.randomAlert());
        }
        Alert expected = AlertTestSupport.alertFromTemplatePersistent(ThreadLocalRandom.current())
                .toBuilder()
                .setLabels(Map.of("k1", "v1", "k2", "v2"))
                .build();
        addAlert(expected);

        List<Alert> result = source.stream()
                .filter(activityFilters.filterByLabels("k1=v1"))
                .map(AlertActivity::getAlert)
                .collect(Collectors.toList());

        assertThat(result, iterableWithSize(1));
        assertThat(result, hasItem(expected));

        result = source.stream()
                .filter(activityFilters.filterByLabels("k1=v1, k2=v2"))
                .map(AlertActivity::getAlert)
                .collect(Collectors.toList());

        assertThat(result, iterableWithSize(1));
        assertThat(result, hasItem(expected));

        result = source.stream()
                .filter(activityFilters.filterByLabels("k1=v1, k2=v22"))
                .map(AlertActivity::getAlert)
                .collect(Collectors.toList());

        assertThat(result, iterableWithSize(0));

        result = source.stream()
                .filter(activityFilters.filterByLabels("k1=v1, k2=v2, k2=v3"))
                .map(AlertActivity::getAlert)
                .collect(Collectors.toList());

        assertThat(result, iterableWithSize(0));
    }

    private AlertActivity addAlert(Alert alert) {
        AlertActivity activity = factory.makeActivity(alert).join();
        source.add(activity);
        return activity;
    }
}
