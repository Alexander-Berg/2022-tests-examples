package ru.yandex.solomon.alert.cluster.health;

import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ScheduledExecutorService;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.jns.client.JnsClientStub;
import ru.yandex.monlib.metrics.registry.MetricRegistry;
import ru.yandex.solomon.alert.api.converters.MuteConverter;
import ru.yandex.solomon.alert.api.converters.NotificationConverter;
import ru.yandex.solomon.alert.cluster.balancer.AlertingLocalShardsStub;
import ru.yandex.solomon.alert.cluster.broker.AlertingProjectShard;
import ru.yandex.solomon.alert.cluster.broker.alert.AlertPostInitializerStub;
import ru.yandex.solomon.alert.cluster.broker.alert.ProjectAlertService;
import ru.yandex.solomon.alert.cluster.broker.alert.ProjectAlertServiceValidatorStub;
import ru.yandex.solomon.alert.cluster.broker.alert.SnapshotProcess;
import ru.yandex.solomon.alert.cluster.broker.alert.activity.ActivityFactory;
import ru.yandex.solomon.alert.cluster.broker.alert.activity.SimpleActivitiesFactory;
import ru.yandex.solomon.alert.cluster.broker.alert.activity.TemplateActivityFactory;
import ru.yandex.solomon.alert.cluster.broker.alert.activity.TemplateAlertFactory;
import ru.yandex.solomon.alert.cluster.broker.alert.activity.search.ActivityFilters;
import ru.yandex.solomon.alert.cluster.broker.alert.activity.search.ActivitySearch;
import ru.yandex.solomon.alert.cluster.broker.evaluation.EvaluationAssignmentServiceStub;
import ru.yandex.solomon.alert.cluster.broker.mute.MuteMatcherStub;
import ru.yandex.solomon.alert.cluster.broker.mute.ProjectMuteService;
import ru.yandex.solomon.alert.cluster.broker.mute.search.MuteSearch;
import ru.yandex.solomon.alert.cluster.broker.notification.ProjectNotificationService;
import ru.yandex.solomon.alert.cluster.broker.notification.StatefulNotificationChannelFactoryStub;
import ru.yandex.solomon.alert.cluster.broker.notification.search.NotificationSearch;
import ru.yandex.solomon.alert.cluster.project.ProjectAssignment;
import ru.yandex.solomon.alert.dao.memory.InMemoryAlertStatesDao;
import ru.yandex.solomon.alert.dao.memory.InMemoryAlertTemplateDao;
import ru.yandex.solomon.alert.dao.memory.InMemoryAlertsDao;
import ru.yandex.solomon.alert.dao.memory.InMemoryMutesDao;
import ru.yandex.solomon.alert.dao.memory.InMemoryNotificationsDao;
import ru.yandex.solomon.alert.notification.RetryOptions;
import ru.yandex.solomon.alert.notification.channel.NotificationChannelStubFactory;
import ru.yandex.solomon.alert.notification.channel.telegram.ChatIdResolverStub;
import ru.yandex.solomon.alert.notification.state.StatefulNotificationChannelFactoryImpl;
import ru.yandex.solomon.alert.template.MustacheTemplateFactory;
import ru.yandex.solomon.alert.unroll.UnrollExecutorStub;
import ru.yandex.solomon.balancer.AssignmentSeqNo;
import ru.yandex.solomon.core.db.dao.memory.InMemoryQuotasDao;
import ru.yandex.solomon.idempotency.IdempotentOperationServiceImpl;
import ru.yandex.solomon.idempotency.dao.memory.InMemoryIdempotentOperationDao;
import ru.yandex.solomon.quotas.watcher.QuotaWatcher;
import ru.yandex.solomon.staffOnly.manager.ok.OkProvider.Status;
import ru.yandex.solomon.ut.ManualClock;
import ru.yandex.solomon.util.host.HostUtils;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.fail;

/**
 * @author Vladimir Gordiychuk
 */
public class ProjectReadinessCheckerTest {

    private AlertingLocalShardsStub localShards;
    private MetricRegistry registry;
    private ProjectReadinessChecker checker;
    private ProjectNotificationService notificationService;
    private ProjectAlertService alertService;
    private ProjectMuteService muteService;
    private SnapshotProcess snapshotProcess;
    private ScheduledExecutorService timer;

    @Before
    public void setUp() throws Exception {
        registry = new MetricRegistry();
        localShards = new AlertingLocalShardsStub();
        checker = new ProjectReadinessChecker(localShards, registry);

        var projectId = "solomon";
        var clock = new ManualClock();
        var notificationConverter = new NotificationConverter(new ChatIdResolverStub());
        var notificationSearch = new NotificationSearch(notificationConverter);
        var notificationChannelFactory = new NotificationChannelStubFactory();
        timer = Executors.newSingleThreadScheduledExecutor();
        var statefulNotificationFactory = new StatefulNotificationChannelFactoryImpl(timer, RetryOptions.newBuilder().build(), notificationConverter);
        notificationService = new ProjectNotificationService(
                projectId, clock, new InMemoryNotificationsDao(), notificationChannelFactory,
                statefulNotificationFactory, notificationConverter, notificationSearch,
                new JnsClientStub());
        var projectAssignment = new ProjectAssignment(projectId, "localhost", AssignmentSeqNo.EMPTY);
        var simpleActivitiesFactory = new SimpleActivitiesFactory(
                projectAssignment,
                new UnrollExecutorStub(timer),
                new EvaluationAssignmentServiceStub(clock, timer),
                new StatefulNotificationChannelFactoryStub(timer, projectId),
                new MuteMatcherStub());
        var templateActivityFactory = new TemplateActivityFactory(
                new InMemoryAlertTemplateDao(true),
                new TemplateAlertFactory(new MustacheTemplateFactory()),
                simpleActivitiesFactory);
        var activityFactory = new ActivityFactory(simpleActivitiesFactory, templateActivityFactory);
        var activitySearch = new ActivitySearch(new ActivityFilters(notificationConverter));
        var alertStatesDao = new InMemoryAlertStatesDao();
        alertService = new ProjectAlertService(projectId, clock, new InMemoryAlertsDao(), alertStatesDao,
                activityFactory, timer, notificationConverter, activitySearch,
                new QuotaWatcher(null, new InMemoryQuotasDao(), "alerting", timer),
                new ProjectAlertServiceValidatorStub(),
                new AlertPostInitializerStub(),
                timer,
                registry,
                new IdempotentOperationServiceImpl(new InMemoryIdempotentOperationDao()), ForkJoinPool.commonPool());
        muteService = new ProjectMuteService(projectId, clock, new InMemoryMutesDao(), new MuteSearch(MuteConverter.INSTANCE),
                MuteConverter.INSTANCE);
        snapshotProcess = new SnapshotProcess(clock, timer, timer, projectAssignment, alertStatesDao,
                () -> { throw new IllegalStateException("broken"); }, Duration.ofMinutes(2));
    }

    @After
    public void tearDown() {
        timer.shutdown();
    }

    @Test
    public void readyWhenNoAssignedProjects() {
        assertStatus(true);
        assertArrayEquals(new Status[0], checker.statuses());
    }

    @Test
    public void notReady() {
        assign("junk");
        assertStatus(false);
    }

    @Test
    public void ready() {
        assign("junk");
        localShards.setReady("junk", true);
        assertStatus(true);
    }

    @Test
    public void notReadyWhenAtLeastOneNotReady() {
        assign("junk");
        assign("solomon");

        localShards.setReady("junk", true);
        assertStatus(false);
    }

    @Test
    public void notReadyWhenAllReady() {
        assign("junk");
        assign("solomon");

        localShards.setReady("junk", true);
        localShards.setReady("solomon", true);
        assertStatus(true);
    }

    private void assertStatus(boolean expected) {
        boolean allReady = true;
        Status[] statuses = checker.statuses();
        for (Status status : statuses) {
            allReady &= status.ok;
        }

        if (allReady != expected) {
            fail(Arrays.toString(statuses) + " expected " + expected);
        }
    }

    private void assign(String projectId) {
        ProjectAssignment assignment = new ProjectAssignment(projectId, HostUtils.getFqdn(), new AssignmentSeqNo(System.nanoTime(), System.nanoTime() + 1));
        localShards.addShard(projectId, new AlertingProjectShard(
                assignment,
                notificationService,
                alertService,
                muteService,
                snapshotProcess));
    }
}
