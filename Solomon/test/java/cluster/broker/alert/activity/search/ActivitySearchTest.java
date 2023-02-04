package ru.yandex.solomon.alert.cluster.broker.alert.activity.search;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.google.common.collect.ImmutableList;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.monlib.metrics.labels.Labels;
import ru.yandex.solomon.alert.EvaluationStatus;
import ru.yandex.solomon.alert.api.converters.NotificationConverter;
import ru.yandex.solomon.alert.cluster.broker.alert.activity.ActivityFactory;
import ru.yandex.solomon.alert.cluster.broker.alert.activity.AlertActivity;
import ru.yandex.solomon.alert.cluster.broker.alert.activity.SimpleActivitiesFactory;
import ru.yandex.solomon.alert.cluster.broker.alert.activity.TemplateActivityFactory;
import ru.yandex.solomon.alert.cluster.broker.alert.activity.TemplateAlertFactory;
import ru.yandex.solomon.alert.cluster.broker.evaluation.EvaluationAssignmentServiceStub;
import ru.yandex.solomon.alert.cluster.broker.mute.MuteMatcherStub;
import ru.yandex.solomon.alert.cluster.broker.notification.StatefulNotificationChannelFactoryStub;
import ru.yandex.solomon.alert.cluster.project.ProjectAssignment;
import ru.yandex.solomon.alert.dao.memory.InMemoryAlertTemplateDao;
import ru.yandex.solomon.alert.domain.Alert;
import ru.yandex.solomon.alert.domain.AlertTestSupport;
import ru.yandex.solomon.alert.domain.expression.ExpressionAlert;
import ru.yandex.solomon.alert.domain.threshold.ThresholdAlert;
import ru.yandex.solomon.alert.notification.channel.NotificationStatus;
import ru.yandex.solomon.alert.notification.channel.telegram.ChatIdResolverStub;
import ru.yandex.solomon.alert.protobuf.EAlertType;
import ru.yandex.solomon.alert.protobuf.EOrderDirection;
import ru.yandex.solomon.alert.protobuf.ERequestStatusCode;
import ru.yandex.solomon.alert.protobuf.TEvaluationStats;
import ru.yandex.solomon.alert.protobuf.TEvaluationStatus;
import ru.yandex.solomon.alert.protobuf.TListAlert;
import ru.yandex.solomon.alert.protobuf.TListAlertList;
import ru.yandex.solomon.alert.protobuf.TListAlertRequest;
import ru.yandex.solomon.alert.protobuf.TListAlertResponse;
import ru.yandex.solomon.alert.protobuf.TListSubAlertRequest;
import ru.yandex.solomon.alert.protobuf.TListSubAlertResponse;
import ru.yandex.solomon.alert.protobuf.TNotificationStats;
import ru.yandex.solomon.alert.protobuf.TNotificationStatus;
import ru.yandex.solomon.alert.template.MustacheTemplateFactory;
import ru.yandex.solomon.alert.unroll.UnrollExecutorStub;
import ru.yandex.solomon.balancer.AssignmentSeqNo;
import ru.yandex.solomon.model.protobuf.MatchType;
import ru.yandex.solomon.model.protobuf.Selector;
import ru.yandex.solomon.ut.ManualClock;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertThat;

/**
 * @author Vladimir Gordiychuk
 */
public class ActivitySearchTest {
    private ScheduledExecutorService executor;
    private ActivityFactory factory;
    private List<AlertActivity> source = new ArrayList<>();
    private ActivitySearch activitySearch;
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
        activitySearch = new ActivitySearch(new ActivityFilters(notificationConverter));
        stateSupport = new AlertPersistStateSupport(notificationConverter);
    }

    @After
    public void tearDown() throws Exception {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    @Test
    public void searchOnEmpty() {
        TListAlertResponse response = activitySearch.listAlerts(source, TListAlertRequest.newBuilder().build());
        assertThat(response, equalTo(TListAlertResponse.newBuilder()
            .setRequestStatus(ERequestStatusCode.OK)
            .setListAlertList(TListAlertList.newBuilder().build())
            .build()));
    }

    @Test
    public void listByDefaultLimited() {
        for (int index = 0; index < 1000; index++) {
            Alert alert = AlertTestSupport.randomAlert()
                .toBuilder()
                .setName(String.format("%04d", index))
                .build();

            addAlert(alert);
        }

        TListAlertResponse response =
            activitySearch.listAlerts(source, TListAlertRequest.newBuilder().build());
        assertThat(response.getAlertsList(), iterableWithSize(10));
    }

    @Test
    public void pagingIterateBySource() {
        for (int index = 0; index < 100; index++) {
            Alert alert = AlertTestSupport.randomAlert()
                .toBuilder()
                .setName(String.format("%04d", index))
                .build();

            addAlert(alert);
        }

        TListAlertRequest init = TListAlertRequest.newBuilder()
            .setPageSize(10)
            .build();

        List<String> buffer = new ArrayList<>(source.size());
        String pageToken = "";
        do {
            TListAlertResponse response = activitySearch.listAlerts(source, init.toBuilder()
                .setPageToken(pageToken)
                .build());

            response.getAlertsList()
                .stream()
                .map(TListAlert::getName)
                .forEach(buffer::add);

            pageToken = response.getNextPageToken();
        } while (!pageToken.isEmpty());

        String[] expected = source.stream()
            .map(activity -> activity.getAlert().getName())
            .toArray(String[]::new);

        String[] result = buffer.toArray(new String[0]);

        assertArrayEquals(expected, result);
    }

    @Test
    public void orderByNameDescAndFilterByType() {
        ThresholdAlert threshold1 = AlertTestSupport.randomThresholdAlert(ThreadLocalRandom.current())
            .toBuilder()
            .setName("01")
            .build();
        addAlert(threshold1);

        ExpressionAlert expression = AlertTestSupport.randomExpressionAlert(ThreadLocalRandom.current())
            .toBuilder()
            .setName("02")
            .build();
        addAlert(expression);

        ThresholdAlert threshold2 = AlertTestSupport.randomThresholdAlert(ThreadLocalRandom.current())
            .toBuilder()
            .setName("03")
            .build();
        addAlert(threshold2);

        Collections.shuffle(source);

        String[] expected = {"03", "01"};

        String[] result = activitySearch.listAlerts(source, TListAlertRequest.newBuilder()
            .setOrderByName(EOrderDirection.DESC)
            .addFilterByType(EAlertType.THRESHOLD)
            .build())
            .getAlertsList()
            .stream()
            .map(TListAlert::getName)
            .toArray(String[]::new);

        assertArrayEquals(expected, result);
    }

    @Test
    public void simpleAlertStats() {
        addThreshold("1", EvaluationStatus.OK);
        addThreshold("2", EvaluationStatus.ALARM);
        addThreshold("3", EvaluationStatus.DEADLINE);
        addThreshold("4", EvaluationStatus.NO_DATA);
        addThreshold("5", EvaluationStatus.ERROR);
        addThreshold("6", EvaluationStatus.WARN);

        Collections.shuffle(source);
        List<TEvaluationStats> result = activitySearch.listAlerts(source, TListAlertRequest.newBuilder()
            .setOrderByName(EOrderDirection.ASC)
            .addFilterByType(EAlertType.THRESHOLD)
            .build())
            .getAlertsList()
            .stream()
            .map(TListAlert::getEvaluationStats)
            .collect(Collectors.toList());

        assertThat(result.get(0), equalTo(TEvaluationStats.newBuilder()
            .setCountOk(1)
            .build()));

        assertThat(result.get(1), equalTo(TEvaluationStats.newBuilder()
            .setCountAlarm(1)
            .build()));

        assertThat(result.get(2), equalTo(TEvaluationStats.newBuilder()
            .setCountError(1)
            .build()));

        assertThat(result.get(3), equalTo(TEvaluationStats.newBuilder()
            .setCountNoData(1)
            .build()));

        assertThat(result.get(4), equalTo(TEvaluationStats.newBuilder()
            .setCountError(1)
            .build()));

        assertThat(result.get(5), equalTo(TEvaluationStats.newBuilder()
            .setCountWarn(1)
            .build()));
    }

    @Test
    public void multiAlertStats() {
        addMultiThreshold("test",
            EvaluationStatus.ERROR,

            EvaluationStatus.DEADLINE,
            EvaluationStatus.DEADLINE,

            EvaluationStatus.NO_DATA,
            EvaluationStatus.NO_DATA,
            EvaluationStatus.NO_DATA,

            EvaluationStatus.OK,
            EvaluationStatus.OK,
            EvaluationStatus.OK,
            EvaluationStatus.OK,

            EvaluationStatus.WARN,
            EvaluationStatus.WARN,

            EvaluationStatus.ALARM,
            EvaluationStatus.ALARM,
            EvaluationStatus.ALARM,
            EvaluationStatus.ALARM,
            EvaluationStatus.ALARM,
            EvaluationStatus.ALARM
        );

        TEvaluationStats stats = activitySearch.listAlerts(source, TListAlertRequest.newBuilder()
            .setFilterByName("test")
            .build())
            .getAlertsList()
            .stream()
            .map(TListAlert::getEvaluationStats)
            .findFirst()
            .get();

        TEvaluationStats expected = TEvaluationStats.newBuilder()
            .setCountError(3)
            .setCountNoData(3)
            .setCountOk(4)
            .setCountAlarm(6)
            .setCountWarn(2)
            .build();

        assertThat(stats, equalTo(expected));
    }

    @Test
    public void simpleAlertNotificationStats() {
        addThreshold("1", EvaluationStatus.OK,
                NotificationStatus.SUCCESS,
                NotificationStatus.SUCCESS);
        addThreshold("2", EvaluationStatus.OK,
                NotificationStatus.SUCCESS,
                NotificationStatus.ERROR);
        addThreshold("3", EvaluationStatus.OK,
                NotificationStatus.ERROR_ABLE_TO_RETRY,
                NotificationStatus.ERROR_ABLE_TO_RETRY,
                NotificationStatus.ERROR_ABLE_TO_RETRY);
        addThreshold("4", EvaluationStatus.OK,
                NotificationStatus.RESOURCE_EXHAUSTED,
                NotificationStatus.RESOURCE_EXHAUSTED,
                NotificationStatus.ERROR_ABLE_TO_RETRY);

        Collections.shuffle(source);
        List<TNotificationStats> result = activitySearch.listAlerts(source, TListAlertRequest.newBuilder()
                .setOrderByName(EOrderDirection.ASC)
                .addFilterByType(EAlertType.THRESHOLD)
                .build())
                .getAlertsList()
                .stream()
                .map(TListAlert::getNotificationStats)
                .collect(Collectors.toList());

        assertThat(result.get(0), equalTo(TNotificationStats.newBuilder()
                .setCountSuccess(2)
                .build()));

        assertThat(result.get(1), equalTo(TNotificationStats.newBuilder()
                .setCountSuccess(1)
                .setCountError(1)
                .build()));

        assertThat(result.get(2), equalTo(TNotificationStats.newBuilder()
                .setCountRetryError(3)
                .build()));

        assertThat(result.get(3), equalTo(TNotificationStats.newBuilder()
                .setCountRetryError(1)
                .setCountResourceExhausted(2)
                .build()));
    }

    @Test
    public void simpleAlertNotificationStatsFiltered() {
        addThreshold("1", EvaluationStatus.OK,
                NotificationStatus.SUCCESS,
                NotificationStatus.SUCCESS);
        addThreshold("2", EvaluationStatus.OK,
                NotificationStatus.SUCCESS,
                NotificationStatus.ERROR);

        Collections.shuffle(source);
        List<TNotificationStats> result = activitySearch.listAlerts(source, TListAlertRequest.newBuilder()
                .setOrderByName(EOrderDirection.ASC)
                .addFilterByNotificationId("channel-1")
                .build())
                .getAlertsList()
                .stream()
                .map(TListAlert::getNotificationStats)
                .collect(Collectors.toList());

        assertThat(result.get(0), equalTo(TNotificationStats.newBuilder()
                .setCountSuccess(1)
                .build()));

        assertThat(result.get(1), equalTo(TNotificationStats.newBuilder()
                .setCountError(1)
                .build()));
    }

    @Test
    public void multiSubAlertNotificationStats() {
        addMultiThreshold("test",
                new NotificationStatus[][]{
                {NotificationStatus.SUCCESS, NotificationStatus.SUCCESS},
                {NotificationStatus.SUCCESS, NotificationStatus.ERROR},
                {NotificationStatus.ERROR, NotificationStatus.ERROR},
                {NotificationStatus.INVALID_REQUEST, NotificationStatus.ERROR},
                {NotificationStatus.RESOURCE_EXHAUSTED, NotificationStatus.RESOURCE_EXHAUSTED},
                {NotificationStatus.RESOURCE_EXHAUSTED, NotificationStatus.RESOURCE_EXHAUSTED},
                {NotificationStatus.ERROR_ABLE_TO_RETRY, NotificationStatus.ERROR}
        });

        TNotificationStats stats = activitySearch.listAlerts(source, TListAlertRequest.newBuilder()
                .setFilterByName("test")
                .build())
                .getAlertsList()
                .stream()
                .map(TListAlert::getNotificationStats)
                .findFirst()
                .get();

        TNotificationStats expected = TNotificationStats.newBuilder()
                .setCountSuccess(3)
                .setCountError(5)
                .setCountInvalidRequest(1)
                .setCountResourceExhausted(4)
                .setCountRetryError(1)
                .build();

        assertThat(stats, equalTo(expected));
    }

    @Test
    public void multiSubAlertNotificationStatsFiltered() {
        addMultiThreshold("test",
                new NotificationStatus[][]{
                        {NotificationStatus.SUCCESS, NotificationStatus.SUCCESS},
                        {NotificationStatus.SUCCESS, NotificationStatus.ERROR},
                        {NotificationStatus.ERROR, NotificationStatus.ERROR},
                        {NotificationStatus.INVALID_REQUEST, NotificationStatus.ERROR},
                        {NotificationStatus.RESOURCE_EXHAUSTED, NotificationStatus.RESOURCE_EXHAUSTED},
                        {NotificationStatus.RESOURCE_EXHAUSTED, NotificationStatus.RESOURCE_EXHAUSTED},
                        {NotificationStatus.ERROR_ABLE_TO_RETRY, NotificationStatus.ERROR}
                });

        TNotificationStats stats = activitySearch.listAlerts(source, TListAlertRequest.newBuilder()
                .setFilterByName("test")
                .addFilterByNotificationId("channel-1")
                .build())
                .getAlertsList()
                .stream()
                .map(TListAlert::getNotificationStats)
                .findFirst()
                .get();

        TNotificationStats expected = TNotificationStats.newBuilder()
                .setCountSuccess(1)
                .setCountError(4)
                .setCountResourceExhausted(2)
                .build();

        assertThat(stats, equalTo(expected));
    }

    @Test
    public void listSubAlertByDefaultLimited() {
        EvaluationStatus[] subAlerts = new EvaluationStatus[3000];
        Arrays.fill(subAlerts, EvaluationStatus.OK);
        addMultiThreshold("test", subAlerts);
        TListSubAlertResponse response =
            activitySearch.listSubAlerts(source.get(0), TListSubAlertRequest.newBuilder().build());
        assertThat(response.getAlertsList(), iterableWithSize(10));
    }

    @Test
    public void listSubAlertsPagingIterate() {
        ThresholdAlert parent = AlertTestSupport.randomThresholdAlert()
                .toBuilder()
                .setSelectors("project=solomon, cluster=foo, service=misc, sensor=idleTime, host=*")
                .setName("test")
                .setGroupByLabels(ImmutableList.of("host"))
                .build();
        LinkedHashMap<Labels, EvaluationStatus> child = new LinkedHashMap<>();
        for (int index = 0; index < 100; index++) {
            child.put(Labels.of("host", String.format("%04d", index)), EvaluationStatus.OK);
        }

        AlertActivity activity = addAlert(parent);
        activity.restore(stateSupport.state(parent, child));

        TListSubAlertRequest init = TListSubAlertRequest.newBuilder()
                .setPageSize(10)
                .build();

        List<String> buffer = new ArrayList<>(100);
        String pageToken = "";
        do {
            TListSubAlertResponse response = activitySearch.listSubAlerts(activity, init.toBuilder()
                    .setPageToken(pageToken)
                    .build());

            response.getAlertsList()
                    .stream()
                    .map(alert -> alert.getLabels(0).getValue())
                    .forEach(buffer::add);

            pageToken = response.getNextPageToken();
        } while (!pageToken.isEmpty());

        String[] expected = child.keySet().stream()
                .map(labels -> labels.at(0).getValue())
                .sorted()
                .toArray(String[]::new);

        String[] result = buffer.toArray(new String[0]);
        assertArrayEquals(expected, result);
    }

    @Test
    public void filterSubAlerts() {
        Map<String, String> annotations = new HashMap<>();
        annotations.put("ann1", "Annotation #1");
        annotations.put("ann2", "Annotation #2");

        ThresholdAlert parent = AlertTestSupport.randomThresholdAlert()
                .toBuilder()
                .setSelectors("project=solomon, cluster=foo, service=misc, sensor=idleTime, host=*")
                .setName("test")
                .setAnnotations(annotations)
                .setGroupByLabels(ImmutableList.of("host"))
                .build();
        LinkedHashMap<Labels, EvaluationStatus> child = new LinkedHashMap<>();
        for (int index = 0; index < 100; index++) {
            child.put(Labels.of("host", String.format("%04d", index)), EvaluationStatus.OK.withAnnotations(annotations));
        }

        AlertActivity activity = addAlert(parent);
        activity.restore(stateSupport.state(parent, child));

        TListSubAlertResponse response = activitySearch.listSubAlerts(activity, TListSubAlertRequest.newBuilder()
                .addFilterByLabels(Selector.newBuilder()
                        .setKey("host")
                        .setMatchType(MatchType.EXACT)
                        .setPattern("0042")
                        .build())
                .addFilterByEvaluationStatus(TEvaluationStatus.ECode.OK)
                .addAnnotationKeys("ann1")
                .addAnnotationKeys("ann3")
                .build());

        String[] result = response.getAlertsList()
                .stream()
                .map(alert -> alert.getLabels(0).getValue())
                .toArray(String[]::new);

        assertArrayEquals(new String[]{"0042"}, result);

        List<Set<String>> resultAnnotationKeys =
            response.getAlertsList()
                .stream()
                .map(alert -> alert.getAnnotationsMap().keySet())
                .collect(Collectors.toList());

        assertThat(resultAnnotationKeys, allOf(hasItem(Collections.singleton("ann1")), iterableWithSize(1)));
    }

    @Test
    public void sortSubAlerts() {
        ThresholdAlert parent = AlertTestSupport.randomThresholdAlert()
                .toBuilder()
                .setSelectors("project=solomon, cluster=foo, service=misc, sensor=idleTime, host=*")
                .setName("test")
                .setGroupByLabels(ImmutableList.of("host"))
                .build();
        LinkedHashMap<Labels, EvaluationStatus> child = new LinkedHashMap<>();
        for (int index = 0; index < 100; index++) {
            child.put(Labels.of("host", String.format("%04d", index)), EvaluationStatus.OK);
        }

        AlertActivity activity = addAlert(parent);
        activity.restore(stateSupport.state(parent, child));

        TListSubAlertResponse response = activitySearch.listSubAlerts(activity, TListSubAlertRequest.newBuilder()
                .setOrderByLabels(EOrderDirection.ASC)
                .setPageSize(5)
                .build());

        String[] result = response.getAlertsList()
                .stream()
                .map(alert -> alert.getLabels(0).getValue())
                .toArray(String[]::new);

        String[] expected = {"0000", "0001", "0002", "0003", "0004"};
        assertArrayEquals(expected, result);
    }

    @Test
    public void filterByNotificationStatus() {
        addThreshold("1", EvaluationStatus.ALARM, NotificationStatus.SUCCESS);
        addThreshold("2", EvaluationStatus.ALARM, NotificationStatus.ERROR);
        addThreshold("3", EvaluationStatus.ALARM, NotificationStatus.SUCCESS);
        addThreshold("4", EvaluationStatus.ALARM, NotificationStatus.RESOURCE_EXHAUSTED);
        addThreshold("5", EvaluationStatus.ALARM, NotificationStatus.SUCCESS);

        {
            String[] result = activitySearch.listAlerts(source, TListAlertRequest.newBuilder()
                    .addFilterByNotificationStatus(TNotificationStatus.ECode.SUCCESS)
                    .build())
                    .getAlertsList()
                    .stream()
                    .map(TListAlert::getName)
                    .toArray(String[]::new);

            String[] expected = {"1", "3", "5"};
            assertArrayEquals(expected, result);
        }

        {
            String[] result = activitySearch.listAlerts(source, TListAlertRequest.newBuilder()
                    .addFilterByNotificationStatus(TNotificationStatus.ECode.ERROR)
                    .build())
                    .getAlertsList()
                    .stream()
                    .map(TListAlert::getName)
                    .toArray(String[]::new);

            String[] expected = {"2"};
            assertArrayEquals(expected, result);
        }
    }

    @Test
    public void filterByNotificationIdAndStatus() {
        addThreshold("1", EvaluationStatus.ALARM, NotificationStatus.SUCCESS, NotificationStatus.SUCCESS);
        addThreshold("2", EvaluationStatus.ALARM, NotificationStatus.ERROR, NotificationStatus.SUCCESS);
        addThreshold("3", EvaluationStatus.ALARM, NotificationStatus.SUCCESS, NotificationStatus.ERROR);
        addThreshold("4", EvaluationStatus.ALARM, NotificationStatus.RESOURCE_EXHAUSTED, NotificationStatus.SUCCESS);
        addThreshold("5", EvaluationStatus.ALARM, NotificationStatus.SUCCESS, NotificationStatus.ERROR);

        {
            String[] result = activitySearch.listAlerts(source, TListAlertRequest.newBuilder()
                    .addFilterByNotificationStatus(TNotificationStatus.ECode.ERROR)
                    .addFilterByNotificationId("channel-0")
                    .build())
                    .getAlertsList()
                    .stream()
                    .map(TListAlert::getName)
                    .toArray(String[]::new);

            String[] expected = {"2"};
            assertArrayEquals(expected, result);
        }

        {
            String[] result = activitySearch.listAlerts(source, TListAlertRequest.newBuilder()
                    .addFilterByNotificationStatus(TNotificationStatus.ECode.ERROR)
                    .addFilterByNotificationId("channel-1")
                    .build())
                    .getAlertsList()
                    .stream()
                    .map(TListAlert::getName)
                    .toArray(String[]::new);

            String[] expected = {"3", "5"};
            assertArrayEquals(expected, result);
        }
    }

    private void addThreshold(String name, EvaluationStatus status) {
        ThresholdAlert alert = AlertTestSupport.randomThresholdAlert()
            .toBuilder()
            .setName(name)
            .setGroupByLabels(Collections.emptyList())
            .build();
        addAlert(alert).restore(stateSupport.state(alert, status));
    }

    private void addThreshold(String name, EvaluationStatus status, NotificationStatus... notifications) {
        ThresholdAlert alert = AlertTestSupport.randomThresholdAlert()
                .toBuilder()
                .setName(name)
                .setGroupByLabels(Collections.emptyList())
                .setNotificationChannels(IntStream.range(0, notifications.length)
                        .mapToObj(index -> "channel-" + index)
                        .collect(Collectors.toList()))
                .build();
        addAlert(alert).restore(stateSupport.state(alert, status, notifications));
    }

    private void addMultiThreshold(String name, EvaluationStatus... stats) {
        ThresholdAlert alert = AlertTestSupport.randomThresholdAlert()
            .toBuilder()
            .setSelectors("project=solomon, cluster=foo, service=misc, sensor=idleTime, host=*")
            .setName(name)
            .setGroupByLabels(ImmutableList.of("host"))
            .build();
        Map<Labels, EvaluationStatus> map = new HashMap<>();
        for (EvaluationStatus stat : stats) {
            map.put(Labels.of("host", "solomon-" + map.size()), stat);
        }

        addAlert(alert).restore(stateSupport.state(alert, map));
    }

    private void addMultiThreshold(String name, NotificationStatus[][] stats) {
        ThresholdAlert alert = AlertTestSupport.randomThresholdAlert()
                .toBuilder()
                .setSelectors("project=solomon, cluster=foo, service=misc, sensor=idleTime, host=*")
                .setName(name)
                .setGroupByLabels(ImmutableList.of("host"))
                .setNotificationChannels(IntStream.range(0, stats[0].length)
                        .mapToObj(index -> "channel-" + index)
                        .collect(Collectors.toList()))
                .build();
        Map<Labels, List<NotificationStatus>> map = new HashMap<>();
        for (NotificationStatus[] stat : stats) {
            map.put(Labels.of("host", "solomon-" + map.size()), Arrays.asList(stat));
        }

        addAlert(alert).restore(stateSupport.stateNotification(alert, map));
    }

    private AlertActivity addAlert(Alert alert) {
        AlertActivity activity = factory.makeActivity(alert).join();
        source.add(activity);
        return activity;
    }

}
