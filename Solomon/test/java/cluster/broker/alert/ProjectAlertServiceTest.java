package ru.yandex.solomon.alert.cluster.broker.alert;

import java.io.StringWriter;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.google.common.base.Throwables;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.protobuf.Any;
import com.google.protobuf.StringValue;
import io.grpc.StatusRuntimeException;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.logging.log4j.core.LoggerContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.misc.concurrent.CompletableFutures;
import ru.yandex.monlib.metrics.encode.text.MetricTextEncoder;
import ru.yandex.monlib.metrics.labels.Labels;
import ru.yandex.solomon.alert.EvaluationStatus;
import ru.yandex.solomon.alert.api.converters.AlertConverter;
import ru.yandex.solomon.alert.api.converters.MuteConverter;
import ru.yandex.solomon.alert.cluster.broker.alert.activity.ActivityFactory;
import ru.yandex.solomon.alert.cluster.broker.alert.activity.AlertStatusCodec;
import ru.yandex.solomon.alert.cluster.broker.alert.activity.SimpleActivitiesFactory;
import ru.yandex.solomon.alert.cluster.broker.alert.activity.TemplateActivityFactory;
import ru.yandex.solomon.alert.cluster.broker.alert.activity.TemplateAlertFactory;
import ru.yandex.solomon.alert.cluster.broker.evaluation.EvaluationAssignmentServiceStub;
import ru.yandex.solomon.alert.cluster.broker.mute.ProjectMuteService;
import ru.yandex.solomon.alert.cluster.broker.notification.StatefulNotificationChannelFactoryStub;
import ru.yandex.solomon.alert.cluster.project.ProjectAssignment;
import ru.yandex.solomon.alert.dao.memory.InMemoryAlertStatesDao;
import ru.yandex.solomon.alert.dao.memory.InMemoryAlertTemplateDao;
import ru.yandex.solomon.alert.dao.memory.InMemoryAlertsDao;
import ru.yandex.solomon.alert.domain.AlertState;
import ru.yandex.solomon.alert.domain.AlertTestSupport;
import ru.yandex.solomon.alert.mute.domain.Mute;
import ru.yandex.solomon.alert.mute.domain.MuteType;
import ru.yandex.solomon.alert.mute.domain.SelectorsMute;
import ru.yandex.solomon.alert.notification.channel.MessageBoxChannel;
import ru.yandex.solomon.alert.notification.channel.NotificationChannelStub;
import ru.yandex.solomon.alert.notification.channel.NotificationStatus;
import ru.yandex.solomon.alert.notification.domain.NotificationTestSupport;
import ru.yandex.solomon.alert.protobuf.AlertMuteStatus;
import ru.yandex.solomon.alert.protobuf.CreateAlertsFromTemplateRequest;
import ru.yandex.solomon.alert.protobuf.EAlertState;
import ru.yandex.solomon.alert.protobuf.EAlertType;
import ru.yandex.solomon.alert.protobuf.ECompare;
import ru.yandex.solomon.alert.protobuf.EOrderDirection;
import ru.yandex.solomon.alert.protobuf.ERequestStatusCode;
import ru.yandex.solomon.alert.protobuf.ListMutesRequest;
import ru.yandex.solomon.alert.protobuf.MuteStatus;
import ru.yandex.solomon.alert.protobuf.TAlert;
import ru.yandex.solomon.alert.protobuf.TCreateAlertRequest;
import ru.yandex.solomon.alert.protobuf.TCreateAlertResponse;
import ru.yandex.solomon.alert.protobuf.TDeleteAlertRequest;
import ru.yandex.solomon.alert.protobuf.TDeleteAlertResponse;
import ru.yandex.solomon.alert.protobuf.TEvaluationState;
import ru.yandex.solomon.alert.protobuf.TEvaluationStats;
import ru.yandex.solomon.alert.protobuf.TEvaluationStatus;
import ru.yandex.solomon.alert.protobuf.TExpression;
import ru.yandex.solomon.alert.protobuf.TListAlert;
import ru.yandex.solomon.alert.protobuf.TListAlertRequest;
import ru.yandex.solomon.alert.protobuf.TListAlertResponse;
import ru.yandex.solomon.alert.protobuf.TListSubAlert;
import ru.yandex.solomon.alert.protobuf.TListSubAlertRequest;
import ru.yandex.solomon.alert.protobuf.TNotificationState;
import ru.yandex.solomon.alert.protobuf.TNotificationStats;
import ru.yandex.solomon.alert.protobuf.TNotificationStatus;
import ru.yandex.solomon.alert.protobuf.TReadAlertInterpolatedRequest;
import ru.yandex.solomon.alert.protobuf.TReadAlertRequest;
import ru.yandex.solomon.alert.protobuf.TReadAlertResponse;
import ru.yandex.solomon.alert.protobuf.TReadEvaluationStateRequest;
import ru.yandex.solomon.alert.protobuf.TReadEvaluationStateResponse;
import ru.yandex.solomon.alert.protobuf.TReadNotificationStateRequest;
import ru.yandex.solomon.alert.protobuf.TReadProjectStatsRequest;
import ru.yandex.solomon.alert.protobuf.TReadProjectStatsResponse;
import ru.yandex.solomon.alert.protobuf.TSubAlert;
import ru.yandex.solomon.alert.protobuf.TUpdateAlertRequest;
import ru.yandex.solomon.alert.protobuf.TUpdateAlertResponse;
import ru.yandex.solomon.alert.protobuf.UpdateAlertTemplateVersionRequest;
import ru.yandex.solomon.alert.protobuf.UpdateAlertTemplateVersionResponse;
import ru.yandex.solomon.alert.template.MustacheTemplateFactory;
import ru.yandex.solomon.alert.unroll.UnrollExecutorStub;
import ru.yandex.solomon.balancer.AssignmentSeqNo;
import ru.yandex.solomon.config.protobuf.ELevel;
import ru.yandex.solomon.config.protobuf.TLogger;
import ru.yandex.solomon.config.protobuf.TLoggingConfig;
import ru.yandex.solomon.core.container.ContainerType;
import ru.yandex.solomon.core.db.dao.memory.InMemoryQuotasDao;
import ru.yandex.solomon.idempotency.IdempotentOperation;
import ru.yandex.solomon.labels.protobuf.LabelConverter;
import ru.yandex.solomon.labels.query.SelectorsFormat;
import ru.yandex.solomon.main.logger.LoggerConfigurationUtils;
import ru.yandex.solomon.model.protobuf.MatchType;
import ru.yandex.solomon.model.protobuf.Selector;
import ru.yandex.solomon.model.protobuf.Selectors;
import ru.yandex.solomon.quotas.watcher.QuotaWatcher;
import ru.yandex.solomon.ut.ManualClock;
import ru.yandex.solomon.ut.ManualScheduledExecutorService;
import ru.yandex.solomon.util.collection.enums.EnumMapToInt;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyArray;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static ru.yandex.solomon.alert.mute.domain.MuteTestSupport.randomMute;

/**
 * @author Vladimir Gordiychuk
 */
public class ProjectAlertServiceTest extends ProjectAlertServiceTestBase {
    private static final Logger logger = LoggerFactory.getLogger(ProjectAlertServiceTest.class);

    @Rule
    public TestRule timeoutRule = new DisableOnDebug(Timeout.builder()
        .withLookingForStuckThread(true)
        .withTimeout(15, TimeUnit.SECONDS)
        .build());

    private static void ensureLoggingConfiguredOnce() {
        var ctx = LoggerContext.getContext(false);
        var conf = ctx.getConfiguration();
        if (conf.getAppenders().size() == 1) {
            LoggerConfigurationUtils.configureLogger(TLoggingConfig.newBuilder()
                    .addLoggers(TLogger.newBuilder().setName("root").setLevel(ELevel.ERROR).build())
                    .addLoggers(TLogger.newBuilder().setName(ProjectMuteService.class.getName()).setLevel(ELevel.DEBUG).build())
                    .build());
        }
    }

    @Before
    public void setUp() throws Exception {
        ensureLoggingConfiguredOnce();
        this.projectId = "junk" + ThreadLocalRandom.current().nextInt(0, 100);
        AssignmentSeqNo seqNo = new AssignmentSeqNo(ThreadLocalRandom.current().nextInt(10, 10000), ThreadLocalRandom.current().nextInt(10, 100));
        this.assignment = new ProjectAssignment(projectId, "localhost", seqNo);
        this.clock = new ManualClock();
        this.executorService = new ManualScheduledExecutorService(2, clock);
        var alertDao = new InMemoryAlertsDao();
        this.alertDao = alertDao;
        this.idempotentDao = alertDao;
        this.statesDao = new InMemoryAlertStatesDao();
        this.channelStub = new StatefulNotificationChannelFactoryStub(executorService, projectId);
        this.assignmentService = new EvaluationAssignmentServiceStub(clock, executorService);
        this.unrollExecutor = new UnrollExecutorStub(executorService);
        createAndRunMuteService();
        this.simpleActivitiesFactory = new SimpleActivitiesFactory(
                assignment,
                unrollExecutor,
                assignmentService,
                channelStub,
                muteService);
        this.templateActivityFactory = new TemplateActivityFactory(
                alertTemplateDao = new InMemoryAlertTemplateDao(true, true),
                templateAlertFactory = new TemplateAlertFactory(new MustacheTemplateFactory()),
                simpleActivitiesFactory);
        this.activityFactory = new ActivityFactory(
                simpleActivitiesFactory,
                templateActivityFactory);
        this.quotasDao = new InMemoryQuotasDao();
        quotasDao.createSchemaForTests();
        quotaWatcher = new QuotaWatcher(null, quotasDao, "alerting", executorService);
        restartProjectAssignment();
    }

    @After
    public void tearDown() throws Exception {
        if (executorService != null) {
            executorService.shutdownNow();
        }
    }

    @Test
    public void readNotExistAlert() {
        TReadAlertResponse response = service.readAlert(TReadAlertRequest.newBuilder()
                .setProjectId(projectId)
                .setAlertId("notExistAlert")
                .build())
                .join();

        assertThat(response.getRequestStatus(), equalTo(ERequestStatusCode.NOT_FOUND));
    }

    @Test
    public void createAlert() {
        TAlert source = randomAlert();
        TAlert result = successCreate(source)
                .toBuilder()
                .clearVersion()
                .clearUpdatedAt()
                .clearCreatedAt()
                .build();

        assertThat(result, equalTo(source));
    }

    @Test
    public void createAndRead() {
        TAlert create = successCreate(randomAlert());
        TAlert read = successRead(create.getId());
        assertThat(read, equalTo(create));
    }

    @Test
    public void notAbleCreateAlertWithSameIdTwice() {
        TAlert v1 = successCreate(randomAlert());
        TCreateAlertResponse response = service.createAlert(TCreateAlertRequest.newBuilder()
                .setAlert(randomAlert()
                        .toBuilder()
                        .setId(v1.getId())
                        .build())
                .build())
                .join();

        assertThat(response.getRequestStatus(), equalTo(ERequestStatusCode.INVALID_REQUEST));
    }

    @Test
    public void ableCreateAlertWithSameIdTwice_sameIdempotentId() {
        var alert = randomAlert();
        service.createAlert(TCreateAlertRequest.newBuilder()
                .setAlert(alert)
                .setIdempotentOperationId("1")
                .build())
                .join();

        TCreateAlertResponse response = service.createAlert(TCreateAlertRequest.newBuilder()
                .setAlert(randomAlert().toBuilder().setId(alert.getId()).build())
                .setIdempotentOperationId("1")
                .build())
                .join();

        var op = idempotentDao.get("1", alert.getProjectId(), ContainerType.PROJECT, AlertingIdempotency.CREATE_OPERATION_TYPE).join();
        assertTrue(op.isPresent());
        assertEquals(alert.getId(), op.get().entityId());
        assertThat(response.getRequestStatus(), equalTo(ERequestStatusCode.OK));
    }

    @Test
    public void idempotentCreate_failedConversion() {
        try {
            var alert = randomAlert();
            idempotentDao.complete(new IdempotentOperation("op1",
                    alert.getProjectId(),
                    ContainerType.PROJECT,
                    AlertingIdempotency.CREATE_OPERATION_TYPE,
                    alert.getId(),
                    Any.getDefaultInstance(),
                    0));
            var response = service.createAlert(TCreateAlertRequest.newBuilder()
                    .setAlert(alert)
                    .setIdempotentOperationId("op1")
                    .build())
                    .join();
        } catch (CompletionException e) {
            assertTrue(Throwables.getRootCause(e) instanceof StatusRuntimeException);
        }
    }

    @Test
    public void idempotentUpdate_failedConversion() {
            var alert = randomAlert();
            idempotentDao.complete(new IdempotentOperation("op1",
                    alert.getProjectId(),
                    ContainerType.PROJECT,
                    AlertingIdempotency.UPDATE_OPERATION_TYPE,
                    alert.getId(),
                    Any.getDefaultInstance(),
                    0));
            service.createAlert(TCreateAlertRequest.newBuilder()
                    .setAlert(alert)
                    .build())
                    .join();
        try {
            service.updateAlert(TUpdateAlertRequest.newBuilder()
                    .setAlert(alert)
                    .setIdempotentOperationId("op1")
                    .build())
                    .join();
        } catch (CompletionException e) {
            assertTrue(Throwables.getRootCause(e) instanceof StatusRuntimeException);
        }
    }

    @Test
    public void idempotentDelete_noFailedConversion() {
        var alert = randomAlert();
        idempotentDao.complete(new IdempotentOperation("fakeOp",
                alert.getProjectId(),
                ContainerType.PROJECT,
                AlertingIdempotency.DELETE_OPERATION_TYPE,
                alert.getId(),
                Any.pack(StringValue.of("123")),
                0)).join();
        var response = service.deleteAlert(TDeleteAlertRequest.newBuilder()
                .setAlertId(alert.getId())
                .setProjectId(alert.getProjectId())
                .setIdempotentOperationId("fakeOp")
                .build())
                .join();
        assertThat(response.getRequestStatus(), equalTo(ERequestStatusCode.OK));
    }

    @Test
    public void updateAlert() {
        TAlert v1 = successCreate(randomAlert());
        TAlert v2 = successUpdate(v1.toBuilder()
                .setName("More pretty name for alert!")
                .build());

        assertThat(v2, not(equalTo(v1)));
        assertThat(v2.getName(), equalTo("More pretty name for alert!"));

        TAlert v3 = successUpdate(v2.toBuilder()
                .setName("Prev name was not so good")
                .build());
        assertThat(v3, not(equalTo(v2)));
        assertThat(v3.getName(), equalTo("Prev name was not so good"));
    }

    @Test
    public void updateAlertWithoutVersion() {
        TAlert v1 = successCreate(randomAlert());
        for (int version = 1; version < 10; ++version) {
            successUpdate(v1.toBuilder()
                    .setName("Alert (updated #" + version + ")")
                    .setVersion(version)
                    .build());
        }
        TAlert v11 = successUpdate(v1.toBuilder()
                .setName("Alert (updated #11)")
                .setVersion(-1)
                .build());

        assertThat(v11.getName(), equalTo("Alert (updated #11)"));
        assertThat(v11.getVersion(), equalTo(11));
    }

    @Test
    public void readUpdatedAlert() {
        TAlert v1 = successCreate(randomAlert());
        TAlert v2 = successUpdate(v1.toBuilder()
                .setName("partition")
                .build());

        TAlert read = successRead(v2.getId());
        assertThat(read, equalTo(v2));
    }

    @Test
    public void optimisticLockViaVersion() {
        TAlert v1 = successCreate(randomAlert());
        TAlert v2 = successUpdate(v1.toBuilder()
                .setName("partition")
                .build());

        TUpdateAlertResponse response = service.updateAlert(TUpdateAlertRequest.newBuilder()
                .setAlert(v1.toBuilder()
                        .setName("This alert never use partition aggregation")
                        .build())
                .build())
                .join();

        assertThat(response.getRequestStatus(), equalTo(ERequestStatusCode.CONCURRENT_MODIFICATION));

        TAlert read = successRead(v2.getId());
        assertThat(read, equalTo(v2));
    }

    @Test
    public void notAbleUpdateNotExistsAlert() {
        TUpdateAlertResponse response = service.updateAlert(TUpdateAlertRequest.newBuilder()
                .setAlert(randomAlert())
                .build())
                .join();

        assertThat(response.getRequestStatus(), equalTo(ERequestStatusCode.NOT_FOUND));
    }

    @Test
    public void updateIncrementUpdateTime() {
        TAlert v1 = successCreate(randomAlert());

        clock.passedTime(1, TimeUnit.MINUTES);
        TAlert v2 = successUpdate(v1.toBuilder()
                .setName("v2")
                .build());

        assertThat(v2.getUpdatedAt(), greaterThan(v1.getUpdatedAt()));

        clock.passedTime(5, TimeUnit.MINUTES);
        TAlert v3 = successUpdate(v2.toBuilder()
                .setName("v3")
                .build());

        assertThat(v3.getUpdatedAt(), greaterThan(v2.getUpdatedAt()));
    }

    @Test
    public void notAbleDeleteNotExistAlert() {
        TDeleteAlertResponse response = service.deleteAlert(TDeleteAlertRequest.newBuilder()
                .setAlertId("notExistsAlertId")
                .build())
                .join();

        assertThat(response.getRequestStatus(), equalTo(ERequestStatusCode.NOT_FOUND));
    }

    @Test
    public void deleteAlertById() {
        TAlert created = successCreate(randomAlert());
        successDelete(created.getId());

        TReadAlertResponse response = service.readAlert(TReadAlertRequest.newBuilder()
                .setAlertId(created.getId())
                .build())
                .join();

        assertThat(response.getRequestStatus(), equalTo(ERequestStatusCode.NOT_FOUND));
    }

    private void listAlerts(Predicate<TAlert> filter, Comparator<TAlert> sorted, TListAlertRequest.Builder request) throws InterruptedException {
        final int size = 10;
        List<TAlert> source = createManyAlerts();
        TListAlert[] expected = source.stream()
                .sorted(sorted)
                .filter(filter)
                .limit(size)
                .map(alert -> {
                    if (alert.getGroupByLabelsCount() > 0 && alert.getState() == EAlertState.ACTIVE) {
                        return alertToListAlert(alert);
                    } else {
                        return alertToListAlert(alert, TEvaluationStatus.ECode.OK, AlertMuteStatus.Code.NOT_MUTED);
                    }
                })
                .toArray(TListAlert[]::new);

        List<Semaphore> syncs = Arrays.stream(expected)
                .filter(a -> !a.getMultiAlert())
                .map(a -> this.assignmentService.getSyncEval(a.getId()))
                .collect(Collectors.toUnmodifiableList());

        for (var sync : syncs) {
            sync.drainPermits();
        }
        clock.passedTime(1, TimeUnit.MINUTES);
        for (var sync : syncs) {
            sync.acquire();
        }

        TListAlert[] result = successList(request
                .setProjectId(projectId)
                .setPageSize(size)
                .build());

        assertArrayEquals(expected, result);
    }

    @Test
    public void listAlertsByTypeThresholdOrExpression() throws InterruptedException {
        listAlerts(
                alert -> alert.getTypeCase() == TAlert.TypeCase.THRESHOLD || alert.getTypeCase() == TAlert.TypeCase.EXPRESSION
                    || alert.getTypeCase() == TAlert.TypeCase.ALERT_FROM_TEMPLATE,
                Comparator.comparing(TAlert::getName),
                TListAlertRequest.newBuilder()
                        .addFilterByType(EAlertType.EXPRESSION)
                        .addFilterByType(EAlertType.THRESHOLD)
        );
    }

    @Test
    public void listAlertsByTypeThresholds() throws InterruptedException {
        listAlerts(
                alert -> alert.getTypeCase() == TAlert.TypeCase.THRESHOLD
                    || (alert.getTypeCase() == TAlert.TypeCase.ALERT_FROM_TEMPLATE && alert.getAlertFromTemplate().getTemplateId().endsWith("false")),
                Comparator.comparing(TAlert::getName),
                TListAlertRequest.newBuilder()
                        .addFilterByType(EAlertType.THRESHOLD)
        );
    }

    @Test
    public void listAlertsByTypeEvaluationStatusOKAndNoData() throws InterruptedException {
        final int size = 10;

        List<TAlert> source = createManySimpleAlerts();
        Map<String, TEvaluationStatus> evalResult = source.stream()
                .map(TAlert::getId)
                .collect(Collectors.toMap(Function.identity(), ignore -> randomEvaluationStatus()));
        for (Map.Entry<String, TEvaluationStatus> entry : evalResult.entrySet()) {
            assignmentService.predefineStatus(entry.getKey(), () -> AlertConverter.protoToStatus(entry.getValue()));
        }

        TListAlert[] expected = source.stream()
                .filter(alert -> {
                    TEvaluationStatus.ECode code = evalResult.get(alert.getId()).getCode();
                    return code == TEvaluationStatus.ECode.OK || code == TEvaluationStatus.ECode.NO_DATA;
                })
                .filter(alert -> alert.getState() == EAlertState.ACTIVE)
                .sorted(Comparator.comparing(TAlert::getName))
                .limit(size)
                .map(alert -> alertToListAlert(alert, evalResult.get(alert.getId()).getCode(), AlertMuteStatus.Code.NOT_MUTED))
                .toArray(TListAlert[]::new);

        // await at last one evaluation
        List<Semaphore> syncs = Stream.of(expected)
                .map(TListAlert::getId)
                .map(assignmentService::getSyncEval)
                .collect(toList());

        for (var sync : syncs) {
            sync.drainPermits();
        }
        clock.passedTime(2, TimeUnit.MINUTES);
        for (var sync : syncs) {
            sync.acquire();
        }

        TListAlert[] result = successList(TListAlertRequest.newBuilder()
                .setProjectId(projectId)
                .setPageSize(size)
                .addFilterByState(EAlertState.ACTIVE)
                .addFilterByEvaluationStatus(TEvaluationStatus.ECode.OK)
                .addFilterByEvaluationStatus(TEvaluationStatus.ECode.NO_DATA)
                .build());

        assertArrayEquals(expected, result);
    }

    @Test
    public void listAlertsByTypeExpressions() throws InterruptedException {
        listAlerts(
                alert -> alert.getTypeCase() == TAlert.TypeCase.EXPRESSION  ||
                    (alert.getTypeCase() == TAlert.TypeCase.ALERT_FROM_TEMPLATE && alert.getAlertFromTemplate().getTemplateId().endsWith("true")),
                Comparator.comparing(TAlert::getName),
                TListAlertRequest.newBuilder()
                        .setProjectId(projectId)
                        .addFilterByType(EAlertType.EXPRESSION)
        );
    }

    @Test
    public void listAlertsByStateActiveAndMuted() throws InterruptedException {
        listAlerts(
                alert -> alert.getState() == EAlertState.ACTIVE || alert.getState() == EAlertState.MUTED,
                Comparator.comparing(TAlert::getName),
                TListAlertRequest.newBuilder()
                        .addFilterByState(EAlertState.ACTIVE)
                        .addFilterByState(EAlertState.MUTED)
        );
    }

    @Test
    public void listAlertsByStateMuted() throws InterruptedException {
        listAlerts(
                alert -> alert.getState() == EAlertState.MUTED,
                Comparator.comparing(TAlert::getName),
                TListAlertRequest.newBuilder()
                        .addFilterByState(EAlertState.MUTED)
        );
    }

    @Test
    public void listAlertsByStateActive() throws InterruptedException {
        listAlerts(
                alert -> alert.getState() == EAlertState.ACTIVE,
                Comparator.comparing(TAlert::getName),
                TListAlertRequest.newBuilder()
                        .addFilterByState(EAlertState.ACTIVE)
        );
    }

    @Test
    public void listAlertsNameSortAsc() throws InterruptedException {
        listAlerts(
                alert -> true,
                Comparator.comparing(TAlert::getName),
                TListAlertRequest.newBuilder()
                        .setOrderByName(EOrderDirection.ASC)
        );
    }

    @Test
    public void listAlertsMuteStats() throws InterruptedException {
        var alert1 = randomNonTemplateAlert().toBuilder()
                .setId("foo")
                .setName("foo")
                .setProjectId(projectId)
                .clearGroupByLabels()
                .addGroupByLabels("host")
                .build();

        var alert2 = randomNonTemplateAlert().toBuilder()
                .setId("bar")
                .setName("bar")
                .setProjectId(projectId)
                .clearGroupByLabels()
                .build();

        SelectorsMute dt = ((SelectorsMute) randomMute(MuteType.BY_SELECTORS)).toBuilder()
                .setAlertSelector(SelectorsFormat.parseSelector("alert='foo|bar'"))
                .setLabelSelectors(SelectorsFormat.parse("host='solomon-2|solomon-1|-'"))
                .setFrom(clock.instant())
                .setTo(clock.instant().plusSeconds(3600))
                .build();

        // This mute is not ACTIVE thus not counted in stats
        SelectorsMute otherDt = ((SelectorsMute) randomMute(MuteType.BY_SELECTORS)).toBuilder()
                .setAlertSelector(SelectorsFormat.parseSelector("alert='*'"))
                .setLabelSelectors(SelectorsFormat.parse(""))
                .setFrom(clock.instant().minusSeconds(3600))
                .setTo(clock.instant().minusSeconds(1800))
                .build();

        unrollExecutor.predefineUnroll("foo", Set.of(
                Labels.of("host", "solomon-1"),
                Labels.of("host", "solomon-2"),
                Labels.of("host", "solomon-3")
        ));

        createMute(dt);
        createMute(otherDt);
        var multiAlert = successCreate(alert1);
        successCreate(alert2);

        awaitUnroll(multiAlert);

        awaitEval(alert2);
        var subAlerts = Arrays.stream(successList(TListSubAlertRequest.newBuilder()
                .setParentId(alert1.getId())
                .build()))
                .collect(toList());

        var predefined = Map.of(
                "solomon-1", EvaluationStatus.OK,
                "solomon-2", EvaluationStatus.ALARM,
                "solomon-3", EvaluationStatus.OK
        );

        for (var subAlert : subAlerts) {
            predefineAlertStatus(subAlert.getId(), predefined.get(subAlert.getLabels(0).getValue()));
        }

        for (var subAlert : subAlerts) {
            awaitEval(subAlert.getId());
        }

        TListAlertResponse response = service.listAlerts(TListAlertRequest.getDefaultInstance()).join();
        assertThat(response.getStatusMessage(), response.getRequestStatus(), equalTo(ERequestStatusCode.OK));

        var fooMutedStats = TEvaluationStats.newBuilder()
                .setCountOk(1)
                .setCountAlarm(1)
                .build();
        var barMutedStats = TEvaluationStats.newBuilder()
                .setCountOk(1)
                .build();
        assertThat(response.getAlerts(0).getMuteStatusCode(), equalTo(AlertMuteStatus.Code.MUTED));
        assertThat(response.getAlerts(0).getMutedStats(), equalTo(barMutedStats));

        assertThat(response.getAlerts(1).getMuteStatusCode(), equalTo(AlertMuteStatus.Code.UNKNOWN));
        assertThat(response.getAlerts(1).getMutedStats(), equalTo(fooMutedStats));
    }

    @Test
    public void listAlertsMuteStatsFilter() throws InterruptedException {
        var alert1 = randomNonTemplateAlert().toBuilder()
                .setId("foo")
                .setName("foo")
                .setProjectId(projectId)
                .clearGroupByLabels()
                .addGroupByLabels("host")
                .build();

        var alert2 = randomNonTemplateAlert().toBuilder()
                .setId("bar")
                .setName("bar")
                .setProjectId(projectId)
                .clearGroupByLabels()
                .build();

        SelectorsMute dt = ((SelectorsMute) randomMute(MuteType.BY_SELECTORS)).toBuilder()
                .setAlertSelector(SelectorsFormat.parseSelector("alert='foo|bar'"))
                .setLabelSelectors(SelectorsFormat.parse("host='solomon-2|solomon-1|-'"))
                .setFrom(clock.instant())
                .setTo(clock.instant().plusSeconds(3600))
                .build();

        SelectorsMute otherDt = ((SelectorsMute) randomMute(MuteType.BY_SELECTORS)).toBuilder()
                .setAlertSelector(SelectorsFormat.parseSelector("alert='foo'"))
                .setLabelSelectors(SelectorsFormat.parse("host='solomon-1'"))
                .setFrom(clock.instant().minusSeconds(3600))
                .setTo(clock.instant().minusSeconds(1800))
                .build();

        unrollExecutor.predefineUnroll("foo", Set.of(
                Labels.of("host", "solomon-1"),
                Labels.of("host", "solomon-2"),
                Labels.of("host", "solomon-3")
        ));

        createMute(dt);
        createMute(otherDt);
        var multiAlert = successCreate(alert1);
        successCreate(alert2);

        awaitUnroll(multiAlert);

        awaitEval(alert2);
        var subAlerts = Arrays.stream(successList(TListSubAlertRequest.newBuilder()
                        .setParentId(alert1.getId())
                        .build()))
                .collect(toList());

        var predefined = Map.of(
                "solomon-1", EvaluationStatus.NO_DATA,
                "solomon-2", EvaluationStatus.ALARM,
                "solomon-3", EvaluationStatus.OK
        );

        for (var subAlert : subAlerts) {
            predefineAlertStatus(subAlert.getId(), predefined.get(subAlert.getLabels(0).getValue()));
        }

        for (var subAlert : subAlerts) {
            awaitEval(subAlert.getId());
        }

        TListAlertResponse response = service.listAlerts(TListAlertRequest.newBuilder()
                .setOrderByName(EOrderDirection.ASC)
                .setFilterByMuteReference(TListAlertRequest.MuteReference.newBuilder().addIds(otherDt.getId()))
                .build()
        ).join();
        assertThat(response.getStatusMessage(), response.getRequestStatus(), equalTo(ERequestStatusCode.OK));

        var fooMutedStats = TEvaluationStats.newBuilder()
                .setCountNoData(1)
                .build();
        assertThat(response.getAlertsCount(), equalTo(1));

        assertThat(response.getAlerts(0).getMuteStatusCode(), equalTo(AlertMuteStatus.Code.UNKNOWN));
        assertThat(response.getAlerts(0).getMutedStats(), equalTo(fooMutedStats));
    }

    @Test
    public void listAlertsMuteStatsFilterInline() throws InterruptedException {
        var alert1 = randomNonTemplateAlert().toBuilder()
                .setId("foo")
                .setName("foo")
                .setProjectId(projectId)
                .clearGroupByLabels()
                .addGroupByLabels("host")
                .build();

        var alert2 = randomNonTemplateAlert().toBuilder()
                .setId("bar")
                .setName("bar")
                .setProjectId(projectId)
                .clearGroupByLabels()
                .build();

        SelectorsMute dt = ((SelectorsMute) randomMute(MuteType.BY_SELECTORS)).toBuilder()
                .setAlertSelector(SelectorsFormat.parseSelector("alert='*'"))
                .setLabelSelectors(SelectorsFormat.parse("host='solomon-2|solomon-1'"))
                .setFrom(clock.instant())
                .setTo(clock.instant().plusSeconds(3600))
                .build();

        unrollExecutor.predefineUnroll("foo", Set.of(
                Labels.of("host", "solomon-1"),
                Labels.of("host", "solomon-2"),
                Labels.of("host", "solomon-3")
        ));

        createMute(dt);
        var multiAlert = successCreate(alert1);
        successCreate(alert2);

        awaitUnroll(multiAlert);

        awaitEval(alert2);
        var subAlerts = Arrays.stream(successList(TListSubAlertRequest.newBuilder()
                        .setParentId(alert1.getId())
                        .build()))
                .collect(toList());

        var predefined = Map.of(
                "solomon-1", EvaluationStatus.NO_DATA,
                "solomon-2", EvaluationStatus.ALARM,
                "solomon-3", EvaluationStatus.OK
        );

        for (var subAlert : subAlerts) {
            predefineAlertStatus(subAlert.getId(), predefined.get(subAlert.getLabels(0).getValue()));
        }

        for (var subAlert : subAlerts) {
            awaitEval(subAlert.getId());
        }

        SelectorsMute otherDt = ((SelectorsMute) randomMute(MuteType.BY_SELECTORS)).toBuilder()
                .setAlertSelector(SelectorsFormat.parseSelector("alert='*'"))
                .setLabelSelectors(SelectorsFormat.parse("host='solomon-3|-'"))
                .setFrom(clock.instant().minusSeconds(3600))
                .setTo(clock.instant().minusSeconds(1800))
                .build();

        TListAlertResponse response = service.listAlerts(TListAlertRequest.newBuilder()
                .setFilterByInlinedMute(MuteConverter.INSTANCE.muteToProto(otherDt, ru.yandex.solomon.alert.mute.domain.MuteStatus.UNKNOWN))
                .setOrderByName(EOrderDirection.ASC)
                .build()
        ).join();
        assertThat(response.getStatusMessage(), response.getRequestStatus(), equalTo(ERequestStatusCode.OK));

        var fooMutedStats = TEvaluationStats.newBuilder()
                .setCountOk(1)
                .build();
        var barMutedStats = TEvaluationStats.newBuilder()
                .setCountOk(1)
                .build();
        assertThat(response.getAlertsCount(), equalTo(2));

        assertThat(response.getAlerts(0).getMuteStatusCode(), equalTo(AlertMuteStatus.Code.NOT_MUTED));
        assertThat(response.getAlerts(0).getMutedStats(), equalTo(barMutedStats));

        assertThat(response.getAlerts(1).getMuteStatusCode(), equalTo(AlertMuteStatus.Code.UNKNOWN));
        assertThat(response.getAlerts(1).getMutedStats(), equalTo(fooMutedStats));
    }

    @Test
    public void listFullAlerts() {
        final int size = 10;
        List<TAlert> source = createManyAlerts();

        TAlert[] expected = source.stream()
                .sorted(Comparator.comparing(TAlert::getName))
                .limit(size)
                .toArray(TAlert[]::new);

        TAlert[] result = successFullList(TListAlertRequest.newBuilder()
                .setProjectId(projectId)
                .setPageSize(size)
                .setFullResultModel(true)
                .build());

        assertArrayEquals(expected, result);
    }

    @Test
    public void listAlertsNameSortDesc() throws InterruptedException {
        listAlerts(
                alert -> true,
                Comparator.comparing(TAlert::getName).reversed(),
                TListAlertRequest.newBuilder()
                        .setOrderByName(EOrderDirection.DESC)
        );
    }

    @Test
    public void createAlertPersisted() {
        TAlert create = successCreate(randomAlert());
        restartProjectAssignment();

        TAlert read = successRead(create.getId());
        assertThat(read, equalTo(create));
    }

    @Test
    public void updateAlertPersisted() {
        TAlert create = successCreate(randomAlert());
        TAlert updated = successUpdate(create.toBuilder()
                .setName("changed alert")
                .build());
        restartProjectAssignment();

        TAlert read = successRead(create.getId());
        assertThat(read, equalTo(updated));

        TListAlert list = successList(TListAlertRequest.newBuilder()
                .setProjectId(projectId)
                .setPageSize(10)
                .build())[0];

        assertThat(list, equalTo(alertToListAlert(read)));
    }

    @Test
    public void saveCreatedFieldsImmutable() {
        Instant now = clock.instant().truncatedTo(ChronoUnit.SECONDS);
        String createdBy = "alice";

        TAlert create = successCreate(randomAlert()
                .toBuilder()
                .setCreatedBy(createdBy)
                .build());
        assertThat(create.getCreatedBy(), equalTo(createdBy));
        assertThat(create.getCreatedAt(), greaterThanOrEqualTo(now.toEpochMilli()));

        TAlert updated = successUpdate(create.toBuilder()
                .setName("changed name")
                .setCreatedBy("bob")
                .setCreatedAt(now.toEpochMilli() + TimeUnit.SECONDS.toMillis(10))
                .build());

        assertThat(updated.getCreatedBy(), equalTo(createdBy));
        assertThat(updated.getCreatedAt(), equalTo(create.getCreatedAt()));
    }

    @Test
    public void deleteAlertPersisted() {
        TAlert created = successCreate(randomAlert());
        successDelete(created.getId());
        restartProjectAssignment();

        TReadAlertResponse response = service.readAlert(TReadAlertRequest.newBuilder()
                .setProjectId(projectId)
                .setAlertId(created.getId())
                .build())
                .join();

        assertThat(response.getRequestStatus(), equalTo(ERequestStatusCode.NOT_FOUND));
    }

    @Test
    public void createNotAbleWhenServiceNotReady() {
        createBrokerService();
        TCreateAlertResponse response = service.createAlert(TCreateAlertRequest.newBuilder()
                .setAlert(randomAlert())
                .build())
                .join();

        assertThat(response.getRequestStatus(), equalTo(ERequestStatusCode.SHARD_NOT_INITIALIZED));
    }

    @Test
    public void updateNotAbleWhenServiceNotReady() {
        createBrokerService();
        TUpdateAlertResponse response = service.updateAlert(TUpdateAlertRequest.newBuilder()
                .setAlert(randomAlert())
                .build())
                .join();

        assertThat(response.getRequestStatus(), equalTo(ERequestStatusCode.SHARD_NOT_INITIALIZED));
    }

    @Test
    public void readNotAbleWhenServiceNotReady() {
        createBrokerService();
        TReadAlertResponse response = service.readAlert(TReadAlertRequest.newBuilder()
                .setProjectId(projectId)
                .setAlertId("myId")
                .build())
                .join();

        assertThat(response.getRequestStatus(), equalTo(ERequestStatusCode.SHARD_NOT_INITIALIZED));
    }

    @Test
    public void listNotAbleWhenServiceNotReady() {
        createBrokerService();
        TListAlertResponse response = service.listAlerts(TListAlertRequest.newBuilder()
                .setProjectId(projectId)
                .setPageSize(10)
                .build())
                .join();

        assertThat(response.getRequestStatus(), equalTo(ERequestStatusCode.SHARD_NOT_INITIALIZED));
    }

    @Test
    public void deleteNotAbleWhenServiceNotReady() {
        createBrokerService();
        TDeleteAlertResponse response = service.deleteAlert(TDeleteAlertRequest.newBuilder()
                .setProjectId(projectId)
                .setAlertId("myId")
                .build())
                .join();

        assertThat(response.getRequestStatus(), equalTo(ERequestStatusCode.SHARD_NOT_INITIALIZED));
    }

    @Test
    public void createdAlertPeriodicallyEvaluate() throws InterruptedException {
        TAlert created = successCreate(randomAlert()
                .toBuilder()
                .clearGroupByLabels()
                .build());

        awaitEval(created);
        TEvaluationState stateOne = successReadEvalState(created.getId());
        assertThat(stateOne.getAlertId(), equalTo(created.getId()));
        assertThat(stateOne.getAlertVersion(), equalTo(created.getVersion()));
        assertThat(stateOne.getProjectId(), equalTo(projectId));
        assertThat(stateOne.getStatus().getCode(), equalTo(TEvaluationStatus.ECode.OK));

        awaitEval(created);
        TEvaluationState stateTwo = successReadEvalState(created.getId());
        assertThat(stateTwo.getStatus().getCode(), equalTo(TEvaluationStatus.ECode.OK));
        assertThat(stateTwo.getSinceMillis(), equalTo(stateOne.getSinceMillis()));
        assertThat(stateTwo.getLatestEvalMillis(), greaterThan(stateOne.getLatestEvalMillis()));

        assignmentService.predefineStatus(created.getId(), () -> EvaluationStatus.ALARM);
        awaitEval(created);

        TEvaluationState stateThree = successReadEvalState(created.getId());
        assertThat(stateThree.getStatus().getCode(), equalTo(TEvaluationStatus.ECode.ALARM));
        assertThat(stateThree.getSinceMillis(), greaterThan(stateTwo.getSinceMillis()));
        assertThat(stateThree.getLatestEvalMillis(), greaterThan(stateTwo.getLatestEvalMillis()));
    }

    @Test
    public void updateAlertResetEvaluationState() throws InterruptedException {
        TAlert created = successCreate(randomAlert()
                .toBuilder()
                .clearGroupByLabels()
                .build());

        awaitEval(created);
        TEvaluationState stateOne = successReadEvalState(created.getId());
        assertThat(stateOne.getStatus().getCode(), equalTo(TEvaluationStatus.ECode.OK));

        TAlert updated = successUpdate(created.toBuilder()
                .clearType()
                .setExpression(TExpression.newBuilder()
                        .setCheckExpression("true")
                        .build())
                .build());

        for (int index = 0; index < 3; index++) {
            awaitEval(updated);
        }

        TEvaluationState stateTwo = successReadEvalState(created.getId());
        assertThat(stateTwo.getAlertVersion(), greaterThan(stateOne.getAlertVersion()));
        assertThat(stateTwo.getSinceMillis(), greaterThan(stateOne.getSinceMillis()));
        assertThat(stateTwo.getStatus().getCode(), equalTo(TEvaluationStatus.ECode.OK));
    }

    @Test
    public void deleteAlertCancelEvaluation() throws InterruptedException {
        TAlert created = successCreate(randomAlert()
                .toBuilder()
                .clearGroupByLabels().build());

        awaitEval(created);
        TEvaluationState stateOne = successReadEvalState(created.getId());
        assertThat(stateOne.getStatus().getCode(), equalTo(TEvaluationStatus.ECode.OK));

        successDelete(created.getId());
        boolean result = awaitEval(created, 20, TimeUnit.MILLISECONDS);
        assertThat(result, equalTo(false));

        TReadEvaluationStateResponse response = service.readEvaluationState(TReadEvaluationStateRequest.newBuilder()
                .setProjectId(projectId)
                .setAlertId(created.getId())
                .build())
                .join();

        assertThat(response.getRequestStatus(), equalTo(ERequestStatusCode.NOT_FOUND));
    }

    @Test
    public void closeServiceStopEvaluation() throws InterruptedException {
        TAlert created = successCreate(randomAlert()
                .toBuilder()
                .clearGroupByLabels()
                .build());

        awaitEval(created);
        service.close();
        boolean result = awaitEval(created, 20, TimeUnit.MILLISECONDS);
        assertThat(result, equalTo(false));
    }

    @Test
    public void evaluationStatePersisted() throws InterruptedException {
        TAlert created = successCreate(randomAlert()
                .toBuilder()
                .clearGroupByLabels()
                .build());

        assignmentService.predefineStatus(created.getId(), () -> EvaluationStatus.ALARM.withDescription("Everything broken!"));
        for (int index = 0; index < 3; index++) {
            awaitEval(created);
        }
        TEvaluationState stateOne = successReadEvalState(created.getId());
        assertThat(stateOne.getStatus().getCode(), equalTo(TEvaluationStatus.ECode.ALARM));

        TimeUnit.MILLISECONDS.sleep(ThreadLocalRandom.current().nextLong(10, 100));
        restartProjectAssignment();
        awaitEval(created);

        TEvaluationState stateTwo = successReadEvalState(created.getId());
        assertThat(stateTwo.getStatus().getCode(), equalTo(TEvaluationStatus.ECode.ALARM));
        assertThat(stateTwo.getSinceMillis(), equalTo(stateOne.getSinceMillis()));
        assertThat(stateTwo.getLatestEvalMillis(), greaterThan(stateOne.getLatestEvalMillis()));
    }

    @Test
    public void listSubAlerts() throws InterruptedException {
        final String parentId = "Root";
        unrollExecutor.predefineUnroll(parentId, Collections::emptySet);
        TAlert parent = successCreate(convert(AlertTestSupport.randomThresholdAlert()
                .toBuilder()
                .setId(parentId)
                .setProjectId(projectId)
                .setState(AlertState.ACTIVE)
                .setSelectors("project=solomon, cluster=local, service=test, sensor=idleTime, host=*")
                .setGroupByLabel("host")
                .build()));

        TListSubAlert[] empty = successList(TListSubAlertRequest.newBuilder()
                .setParentId(parentId)
                .setProjectId(projectId)
                .build());
        assertThat(empty, emptyArray());

        awaitUnroll(parent);
        TListSubAlert[] stillEmpty = successList(TListSubAlertRequest.newBuilder()
                .setParentId(parentId)
                .setProjectId(projectId)
                .build());
        assertThat(stillEmpty, emptyArray());

        unrollExecutor.predefineUnroll(parentId,
                () -> ImmutableSet.of(
                        Labels.of("host", "solomon-1"),
                        Labels.of("host", "solomon-2")
                )
        );
        awaitUnroll(parent);

        TListSubAlert[] unrolled = successList(TListSubAlertRequest.newBuilder()
                .setProjectId(projectId)
                .setParentId(parentId)
                .build());

        assertThat(unrolled, arrayWithSize(2));
        // By default sub alerts sorted by labels, in our case expected order solomon-1, solomon-2
        assertThat(LabelConverter.protoToLabels(unrolled[0].getLabelsList()), equalTo(Labels.of("host", "solomon-1")));
        assertThat(LabelConverter.protoToLabels(unrolled[1].getLabelsList()), equalTo(Labels.of("host", "solomon-2")));
        assertThat("Each unrolled alert have own unique id", unrolled[0].getId(), not(equalTo(unrolled[1].getId())));

        TListSubAlert[] solomon2 = successList(TListSubAlertRequest.newBuilder()
                .setParentId(projectId)
                .setParentId(parentId)
                .addFilterByLabels(Selector.newBuilder()
                        .setKey("host")
                        .setMatchType(MatchType.EXACT)
                        .setPattern("solomon-2")
                        .build())
                .build());
        assertThat(solomon2, arrayWithSize(1));
        assertThat(solomon2[0], equalTo(unrolled[1]));
    }

    @Test
    public void multiAlertChangeResetSubAlertsUnrolling() throws InterruptedException {
        final String parentId = "Root";
        unrollExecutor.predefineUnroll(parentId,
                () -> ImmutableSet.of(
                        Labels.of("host", "solomon-1"),
                        Labels.of("host", "solomon-2"),
                        Labels.of("host", "solomon-3")
                )
        );

        TAlert created = successCreate(convert(AlertTestSupport.randomThresholdAlert()
                .toBuilder()
                .setId(parentId)
                .setProjectId(projectId)
                .setState(AlertState.ACTIVE)
                .setSelectors("project=solomon, cluster=local, service=test, sensor=idleTime, host=*")
                .setGroupByLabel("host")
                .build()));
        awaitUnroll(created);

        TListSubAlert[] allHosts = successList(TListSubAlertRequest.newBuilder()
                .setParentId(parentId)
                .setProjectId(projectId)
                .build());
        assertThat(allHosts, arrayWithSize(3));

        unrollExecutor.predefineUnroll(parentId, () -> ImmutableSet.of(
                Labels.of("host", "solomon-1"),
                Labels.of("host", "solomon-3")
        ));

        TAlert updated = successUpdate(created.toBuilder()
                .setThreshold(created.getThreshold()
                        .toBuilder()
                        .setNewSelectors(Selectors.newBuilder()
                                .addLabelSelectors(Selector.newBuilder()
                                        .setKey("host")
                                        .setPattern("solomon-1|solomon-3")
                                        .setMatchType(MatchType.GLOB)
                                )
                        )
                )
                .build());
        awaitUnroll(updated);

        TListSubAlert[] updatedHosts = successList(TListSubAlertRequest.newBuilder()
                .setParentId(parentId)
                .setProjectId(projectId)
                .build());
        assertThat(updatedHosts, arrayWithSize(2));
        // By default sub alerts sorted by labels, in our case expected order solomon-1, solomon-3
        assertThat(LabelConverter.protoToLabels(updatedHosts[0].getLabelsList()), equalTo(Labels.of("host", "solomon-1")));
        assertThat(LabelConverter.protoToLabels(updatedHosts[1].getLabelsList()), equalTo(Labels.of("host", "solomon-3")));
    }

    @Test
    public void updateFromMultiAlertToRegularAlert() throws InterruptedException {
        final String parentId = "Root";
        unrollExecutor.predefineUnroll(parentId,
                () -> ImmutableSet.of(
                        Labels.of("host", "solomon-1"),
                        Labels.of("host", "solomon-2"),
                        Labels.of("host", "solomon-3")
                )
        );

        TAlert created = successCreate(convert(AlertTestSupport.randomThresholdAlert()
                .toBuilder()
                .setId(parentId)
                .setProjectId(projectId)
                .setState(AlertState.ACTIVE)
                .setSelectors("project=solomon, cluster=local, service=test, sensor=idleTime, host=*")
                .setGroupByLabel("host")
                .build()));
        awaitUnroll(created);

        assignmentService.predefineStatus(created.getId(), () -> EvaluationStatus.ALARM.withDescription("My alarm"));
        TAlert updated = successUpdate(created.toBuilder()
                .clearGroupByLabels()
                .build());
        awaitEval(updated);

        TListSubAlert[] empty = successList(TListSubAlertRequest.newBuilder()
                .setProjectId(projectId)
                .setParentId(parentId)
                .build());
        assertThat(empty, emptyArray());

        TEvaluationState eval = successReadEvalState(updated.getId());
        assertThat(eval.getStatus().getCode(), equalTo(TEvaluationStatus.ECode.ALARM));
    }

    @Test
    public void updateFromRegularAlertToMultiAlert() throws InterruptedException {
        final String id = "myPredefineId";
        unrollExecutor.predefineUnroll(id,
                () -> ImmutableSet.of(
                        Labels.of("host", "solomon-1"),
                        Labels.of("host", "solomon-2")
                )
        );

        TAlert created = successCreate(convert(AlertTestSupport.randomThresholdAlert()
                .toBuilder()
                .setId(id)
                .setProjectId(projectId)
                .setState(AlertState.ACTIVE)
                .setSelectors("project=solomon, cluster=local, service=test, sensor=idleTime, host=*")
                .setGroupByLabels(Collections.emptyList())
                .build()));
        assignmentService.predefineStatus(created.getId(), () -> EvaluationStatus.ERROR.withDescription("Something go wrong"));
        awaitEval(created);

        TEvaluationState eval = successReadEvalState(id);
        assertThat(eval.getStatus().getCode(), equalTo(TEvaluationStatus.ECode.ERROR));

        TListSubAlert[] emptySubAlerts = successList(TListSubAlertRequest.newBuilder()
                .setParentId(id)
                .setProjectId(projectId)
                .build());
        assertThat(emptySubAlerts, emptyArray());

        TAlert updated = successUpdate(created.toBuilder()
                .addGroupByLabels("host")
                .build());
        awaitUnroll(updated);

        TListSubAlert[] subAlerts = successList(TListSubAlertRequest.newBuilder()
                .setParentId(id)
                .setProjectId(projectId)
                .build());
        assertThat(subAlerts, arrayWithSize(2));
        // By default sub alerts sorted by labels, in our case expected order solomon-1, solomon-3
        assertThat(LabelConverter.protoToLabels(subAlerts[0].getLabelsList()), equalTo(Labels.of("host", "solomon-1")));
        assertThat(LabelConverter.protoToLabels(subAlerts[1].getLabelsList()), equalTo(Labels.of("host", "solomon-2")));
    }

    @Test
    public void readSubAlert() throws InterruptedException {
        final String parentId = "Root";
        unrollExecutor.predefineUnroll(parentId,
                () -> ImmutableSet.of(
                        Labels.of("host", "solomon-1"),
                        Labels.of("host", "solomon-2")
                )
        );

        TAlert created = successCreate(convert(AlertTestSupport.randomThresholdAlert()
                .toBuilder()
                .setId(parentId)
                .setProjectId(projectId)
                .setState(AlertState.ACTIVE)
                .setSelectors("project=solomon, cluster=local, service=test, sensor=idleTime, host=*")
                .setGroupByLabel("host")
                .build()));
        awaitUnroll(created);

        TListSubAlert[] subAlerts = successList(TListSubAlertRequest.newBuilder()
                .setParentId(parentId)
                .setProjectId(projectId)
                .build());
        assertThat(subAlerts, arrayWithSize(2));

        for (TListSubAlert subAlert : subAlerts) {
            TSubAlert expected = TSubAlert.newBuilder()
                    .setId(subAlert.getId())
                    .setProjectId(projectId)
                    .setParent(created)
                    .addAllGroupKey(subAlert.getLabelsList())
                    .build();

            TSubAlert read = successReadSubAlert(parentId, subAlert.getId());
            assertThat(read, equalTo(expected));
        }
    }

    @Test
    public void subAlertsPeriodicallyEvaluates() throws InterruptedException {
        final String parentId = "root";
        unrollExecutor.predefineUnroll(parentId,
                () -> ImmutableSet.of(
                        Labels.of("host", "solomon-1"),
                        Labels.of("host", "solomon-2")
                )
        );

        TAlert created = successCreate(convert(AlertTestSupport.randomThresholdAlert()
                .toBuilder()
                .setId(parentId)
                .setProjectId(projectId)
                .setState(AlertState.ACTIVE)
                .setSelectors("project=solomon, cluster=local, service=test, sensor=idleTime, host=*")
                .setGroupByLabel("host")
                .build()));
        awaitUnroll(created);

        TListSubAlert[] subAlerts = successList(TListSubAlertRequest.newBuilder()
                .setParentId(parentId)
                .setProjectId(projectId)
                .build());
        assertThat(subAlerts, arrayWithSize(2));

        String subId = subAlerts[0].getId();
        EvaluationStatus expectAlarm = EvaluationStatus.ALARM.withDescription("expected alarm");
        assignmentService.predefineStatus(subId, () -> expectAlarm);
        awaitEval(subId);

        TEvaluationState alarm = successReadEvalState(parentId, subId);
        assertThat(AlertConverter.protoToStatus(alarm.getStatus()), equalTo(expectAlarm));
        awaitEval(subId);
        awaitEval(subId);
        awaitEval(subId);
        TEvaluationState stillAlarm = successReadEvalState(parentId, subId);
        assertThat(stillAlarm.getStatus(), equalTo(alarm.getStatus()));
        assertThat(stillAlarm.getSinceMillis(), equalTo(alarm.getSinceMillis()));
        assertThat(stillAlarm.getLatestEvalMillis(), greaterThan(alarm.getLatestEvalMillis()));

        assignmentService.predefineStatus(subId, () -> EvaluationStatus.OK);
        awaitEval(subId);
        TEvaluationState ok = successReadEvalState(parentId, subId);
        assertThat(ok.getStatus().getCode(), equalTo(TEvaluationStatus.ECode.OK));
        assertThat(ok.getSinceMillis(), greaterThan(stillAlarm.getLatestEvalMillis()));
        assertThat(ok.getLatestEvalMillis(), greaterThan(stillAlarm.getLatestEvalMillis()));
    }

    @Test
    public void subAlertEvaluateStatePersisted() throws InterruptedException {
        final String parentId = "root";
        unrollExecutor.predefineUnroll(parentId,
                () -> ImmutableSet.of(
                        Labels.of("host", "solomon-1", "disk", "/dev/sda1"),
                        Labels.of("host", "solomon-2", "disk", "tmp")
                )
        );

        TAlert created = successCreate(convert(AlertTestSupport.randomThresholdAlert()
                .toBuilder()
                .setId(parentId)
                .setProjectId(projectId)
                .setState(AlertState.ACTIVE)
                .setSelectors("project=solomon, cluster=local, service=test, sensor=freeSpace")
                .setGroupByLabels(Arrays.asList("host", "disk"))
                .build()));
        awaitUnroll(created);

        TListSubAlert[] subAlerts = successList(TListSubAlertRequest.newBuilder()
                .setParentId(parentId)
                .setProjectId(projectId)
                .build());
        assertThat(subAlerts, arrayWithSize(2));

        String subId = subAlerts[0].getId();
        EvaluationStatus expectAlarm = EvaluationStatus.ALARM.withDescription("expected alarm");
        assignmentService.predefineStatus(subId, () -> expectAlarm);
        awaitEval(subId);
        awaitEval(subId);

        TEvaluationState stateOne = successReadEvalState(parentId, subId);
        restartProjectAssignment();
        awaitEval(subId);
        TEvaluationState stateTwo = successReadEvalState(parentId, subId);

        assertThat(stateTwo.getStatus(), equalTo(stateOne.getStatus()));
        assertThat(stateTwo.getSinceMillis(), equalTo(stateOne.getSinceMillis()));
        assertThat(stateTwo.getLatestEvalMillis(), greaterThan(stateOne.getLatestEvalMillis()));
    }

    @Test
    public void resetSubAlertEvaluationStateWhenParentAlertChanged() throws InterruptedException {
        final String parentId = "root";
        unrollExecutor.predefineUnroll(parentId,
                () -> ImmutableSet.of(
                        Labels.of("host", "solomon-1", "disk", "/dev/sda1"),
                        Labels.of("host", "solomon-2", "disk", "tmp")
                )
        );

        TAlert created = successCreate(convert(AlertTestSupport.randomThresholdAlert()
                .toBuilder()
                .setId(parentId)
                .setProjectId(projectId)
                .setState(AlertState.ACTIVE)
                .setSelectors("project=solomon, cluster=local, service=test, sensor=freeSpace")
                .setGroupByLabels(Arrays.asList("host", "disk"))
                .build()));
        awaitUnroll(created);

        TListSubAlert[] subAlerts = successList(TListSubAlertRequest.newBuilder()
                .setParentId(parentId)
                .setProjectId(projectId)
                .build());
        assertThat(subAlerts, arrayWithSize(2));

        String subId = subAlerts[0].getId();
        EvaluationStatus expectAlarm = EvaluationStatus.ALARM.withDescription("expected alarm");
        assignmentService.predefineStatus(subId, () -> expectAlarm);
        awaitEval(subId);

        TEvaluationState stateOne = successReadEvalState(parentId, subId);
        assertThat(stateOne.getAlertVersion(), equalTo(created.getVersion()));
        assertThat(AlertConverter.protoToStatus(stateOne.getStatus()), equalTo(expectAlarm));

        logger.debug("Updating alert {}", created.getId());
        TAlert updated = successUpdate(created.toBuilder()
                .setThreshold(created.getThreshold()
                        .toBuilder()
                        .setThreshold(42)
                        .setComparison(ECompare.EQ))
                .setName("diff name")
                .build());
        awaitUnroll(updated);
        awaitEval(subId);

        TEvaluationState stateTwo = successReadEvalState(parentId, subId);
        assertThat(stateTwo.getStatus(), equalTo(stateOne.getStatus()));
        assertThat(stateTwo.getAlertVersion(), equalTo(updated.getVersion()));
        assertThat(stateTwo.getSinceMillis(), greaterThan(stateOne.getSinceMillis()));
        assertThat(stateTwo.getLatestEvalMillis(), greaterThan(stateOne.getLatestEvalMillis()));
    }

    @Test
    public void readAlertNotificationStateWhenItAbsent() throws InterruptedException {
        TAlert alert = successCreate(randomAlert()
                .toBuilder()
                .clearGroupByLabels()
                .clearConfiguredNotificationChannels()
                .clearNotificationChannelIds()
                .build());
        assignmentService.predefineStatus(alert.getId(), () -> EvaluationStatus.ALARM);
        awaitEval(alert);

        List<TNotificationState> result = successReadNotificationState(TReadNotificationStateRequest.newBuilder()
                .setAlertId(alert.getId())
                .setProjectId(projectId)
                .build());

        assertThat(result, emptyIterable());
    }

    @Test
    public void readAlertNotificationState() throws InterruptedException {
        NotificationChannelStub channel = new NotificationChannelStub(NotificationTestSupport.randomNotification()
                .toBuilder()
                .setProjectId(projectId)
                .setRepeatNotifyDelay(Duration.ZERO)
                .setNotifyAboutStatus(EvaluationStatus.Code.ALARM, EvaluationStatus.Code.OK)
                .build());
        channel.predefineStatuses(NotificationStatus.SUCCESS);
        channelStub.setNotificationChannel(channel);

        TAlert alert = successCreate(randomAlert()
                .toBuilder()
                .clearGroupByLabels()
                .clearConfiguredNotificationChannels()
                .clearNotificationChannelIds()
                .addNotificationChannelIds(channel.getId())
                .build());
        assignmentService.predefineStatus(alert.getId(), () -> EvaluationStatus.ALARM);
        awaitNotify(alert.getId(), channel);

        TNotificationState stateOne = Iterables.getOnlyElement(successReadNotificationState(alert.getId()));
        assertThat(stateOne.getStatus().getCode(), equalTo(TNotificationStatus.ECode.SUCCESS));
        assertThat(stateOne.getLatestEvalMillis(), equalTo(stateOne.getLatestSuccessMillis()));

        channel.predefineStatuses(NotificationStatus.ERROR.withDescription("Some details"));
        assignmentService.predefineStatus(alert.getId(), () -> EvaluationStatus.OK);
        awaitNotify(alert.getId(), channel);

        TNotificationState stateTwo = Iterables.getOnlyElement(successReadNotificationState(alert.getId()));
        assertThat(stateTwo.getStatus(), equalTo(TNotificationStatus.newBuilder()
                .setCode(TNotificationStatus.ECode.ERROR)
                .setDesctiption("Some details")
                .build()));
        assertThat(stateTwo.getLatestSuccessMillis(), equalTo(stateOne.getLatestSuccessMillis()));
        assertThat(stateTwo.getLatestEvalMillis(), greaterThan(stateOne.getLatestEvalMillis()));

        channel.predefineStatuses(NotificationStatus.SUCCESS);
        awaitNotify(alert.getId(), channel);
        TNotificationState stateTree = Iterables.getOnlyElement(successReadNotificationState(alert.getId()));
        assertThat(stateTree.getStatus().getCode(), equalTo(TNotificationStatus.ECode.SUCCESS));
        assertThat(stateTree.getLatestSuccessMillis(), greaterThan(stateTwo.getLatestSuccessMillis()));
        assertThat(stateTree.getLatestEvalMillis(), greaterThan(stateTwo.getLatestEvalMillis()));
    }

    @Test
    public void readSubAlertNotificationState() throws InterruptedException {
        NotificationChannelStub channel = new NotificationChannelStub(NotificationTestSupport.randomNotification()
                .toBuilder()
                .setProjectId(projectId)
                .setRepeatNotifyDelay(Duration.ZERO)
                .setNotifyAboutStatus(EvaluationStatus.Code.ALARM, EvaluationStatus.Code.OK)
                .build());
        channel.predefineStatuses(NotificationStatus.ERROR.withDescription("Expected notify error"));
        channelStub.setNotificationChannel(channel);

        final String parentId = "root";
        unrollExecutor.predefineUnroll(parentId,
                () -> ImmutableSet.of(
                        Labels.of("host", "solomon-1")
                )
        );

        TAlert created = successCreate(convert(AlertTestSupport.randomThresholdAlert()
                .toBuilder()
                .setId(parentId)
                .setProjectId(projectId)
                .setState(AlertState.ACTIVE)
                .setSelectors("project=solomon, cluster=local, service=test, sensor=idleTime, host=*")
                .setGroupByLabel("host")
                .setNotificationChannel(channel.getId())
                .build()));
        awaitUnroll(created);

        TListSubAlert[] subAlerts = successList(TListSubAlertRequest.newBuilder()
                .setParentId(parentId)
                .setProjectId(projectId)
                .build());
        assertThat(subAlerts, arrayWithSize(1));

        String subId = subAlerts[0].getId();
        awaitNotify(parentId, subId, channel);

        TNotificationState state = Iterables.getOnlyElement(successReadNotificationState(parentId, subId));
        assertThat(state.getStatus(), equalTo(TNotificationStatus.newBuilder()
                .setCode(TNotificationStatus.ECode.ERROR)
                .setDesctiption("Expected notify error")
                .build()));
    }

    @Test
    public void readAlertEvaluationStats() throws Exception {
        final String alertId = "root";
        unrollExecutor.predefineUnroll(alertId,
            () -> ImmutableSet.of(
                Labels.of("host", "solomon-1"),
                Labels.of("host", "solomon-2"),
                Labels.of("host", "solomon-3"),
                Labels.of("host", "solomon-4"),
                Labels.of("host", "solomon-5")
            )
        );

        TAlert alert = successCreate(convert(AlertTestSupport.randomThresholdAlert()
            .toBuilder()
            .setId(alertId)
            .setProjectId(projectId)
            .setState(AlertState.ACTIVE)
            .setSelectors("project=solomon, cluster=local, service=test, sensor=freeSpace")
            .setGroupByLabel("host")
            .build()));
        awaitUnroll(alert);

        createMute(((SelectorsMute) randomMute(MuteType.BY_SELECTORS)).toBuilder()
                .setProjectId(projectId)
                .setAlertSelector(SelectorsFormat.parseSelector("alert='" + alert.getId() + "'"))
                .setLabelSelectors(SelectorsFormat.parse("host='solomon-3|solomon-4|solomon-5'"))
                .setFrom(clock.instant())
                .setTo(clock.instant().plusSeconds(3600))
                .build());

        TListSubAlert[] subAlerts = successList(TListSubAlertRequest.newBuilder()
            .setParentId(alertId)
            .setProjectId(projectId)
            .build());
        assertThat(subAlerts, arrayWithSize(5));

        Map<String, EvaluationStatus> predefinedStatuses = Map.of(
                "solomon-1", EvaluationStatus.ALARM,
                "solomon-2", EvaluationStatus.OK,
                "solomon-3", EvaluationStatus.OK,
                "solomon-4", EvaluationStatus.NO_DATA,
                "solomon-5", EvaluationStatus.ERROR
        );
        for (var subAlert : subAlerts) {
            predefineAlertStatus(subAlert.getId(), predefinedStatuses.get(subAlert.getLabels(0).getValue()));
        }

        List<Semaphore> syncs = Arrays.stream(subAlerts)
                .map(a -> this.assignmentService.getSyncEval(a.getId()))
                .collect(Collectors.toUnmodifiableList());
        for (var sync : syncs) {
            sync.drainPermits();
        }
        clock.passedTime(1, TimeUnit.MINUTES);
        for (var sync : syncs) {
            sync.acquire();
        }

        TEvaluationStats evalStats = successReadEvalStats(alertId);

        TEvaluationStats expectedEvalStats = TEvaluationStats.newBuilder()
            .setCountAlarm(1)
            .setCountOk(2)
            .setCountError(1)
            .setCountNoData(1)
            .build();

        assertThat(evalStats, equalTo(expectedEvalStats));

        TEvaluationStats mutedStats = successReadMutedStats(alertId);

        TEvaluationStats expectedMutedStats = TEvaluationStats.newBuilder()
                .setCountOk(1)
                .setCountError(1)
                .setCountNoData(1)
                .build();

        assertThat(mutedStats, equalTo(expectedMutedStats));
    }

    @Test
    public void readSingleAlertNotificationStats() throws Exception {
        NotificationChannelStub channel1 = registerNotificationWithStatus(NotificationStatus.SUCCESS);
        NotificationChannelStub channel2 = registerNotificationWithStatus(NotificationStatus.ERROR);

        TAlert alert = successCreate(randomAlert()
            .toBuilder()
            .clearGroupByLabels()
            .clearConfiguredNotificationChannels()
            .clearNotificationChannelIds()
            .addNotificationChannelIds(channel1.getId())
            .addNotificationChannelIds(channel2.getId())
            .build());

        assignmentService.predefineStatus(alert.getId(), () -> EvaluationStatus.OK);

        var sync1 = channel1.getSendSync(alert.getId());
        var sync2 = channel2.getSendSync(alert.getId());

        clock.passedTime(1, TimeUnit.MINUTES);

        // First OKs
        sync1.acquire();
        sync2.acquire();

        assignmentService.predefineStatus(alert.getId(), () -> EvaluationStatus.ALARM);

        clock.passedTime(1, TimeUnit.MINUTES);

        sync1.acquire();
        sync2.acquire();

        TNotificationStats stats = successReadNotificationStats(alert.getId());
        assertThat(stats, equalTo(TNotificationStats.newBuilder()
            .setCountSuccess(1)
            .setCountError(1)
            .build()));
    }

    @Test
    public void readMultiAlertNotificationStats() throws Exception {
        String alertId = "root";

        NotificationChannelStub channel1 = registerNotificationWithStatus(NotificationStatus.SUCCESS);
        NotificationChannelStub channel2 = registerNotificationWithStatus(NotificationStatus.ERROR);

        unrollExecutor.predefineUnroll(alertId,
            () -> ImmutableSet.of(
                Labels.of("host", "solomon-1"),
                Labels.of("host", "solomon-2"),
                Labels.of("host", "solomon-3"),
                Labels.of("host", "solomon-4"),
                Labels.of("host", "solomon-5")
            )
        );

        TAlert alert = successCreate(convert(AlertTestSupport.randomThresholdAlert()
            .toBuilder()
            .setId(alertId)
            .setProjectId(projectId)
            .setState(AlertState.ACTIVE)
            .setSelectors("project=solomon, cluster=local, service=test, sensor=idleTime, host=*")
            .setNotificationChannels(Arrays.asList(channel1.getId(), channel2.getId()))
            .setGroupByLabel("host")
            .build()));

        awaitUnroll(alert);

        TListSubAlert[] subAlerts = successList(TListSubAlertRequest.newBuilder()
            .setProjectId(projectId)
            .setParentId(alertId)
            .build());

        assertThat(subAlerts, arrayWithSize(5));

        assignmentService.predefineStatus(subAlerts[0].getId(), () -> EvaluationStatus.ALARM);
        assignmentService.predefineStatus(subAlerts[1].getId(), () -> EvaluationStatus.OK);
        assignmentService.predefineStatus(subAlerts[2].getId(), () -> EvaluationStatus.NO_DATA);
        assignmentService.predefineStatus(subAlerts[3].getId(), () -> EvaluationStatus.OK);
        assignmentService.predefineStatus(subAlerts[4].getId(), () -> EvaluationStatus.ERROR);

        // await at lease two notifications by each channel, to guarantee sync
        for (int index = 0; index < 2; index++) {
            List<Semaphore> syncs = Stream.of(subAlerts)
                    .flatMap(a -> Stream.of(channel1.getSendSync(a.getId()), channel2.getSendSync(a.getId())))
                    .collect(toList());

            clock.passedTime(1, TimeUnit.MINUTES);
            for (Semaphore notifySync : syncs) {
                while (!notifySync.tryAcquire(10, TimeUnit.MILLISECONDS)) {
                    clock.passedTime(30, TimeUnit.SECONDS);
                }
            }
        }

        TNotificationStats stats = successReadNotificationStats(alert.getId());
        assertThat(stats, equalTo(TNotificationStats.newBuilder()
            .setCountSuccess(5)
            .setCountError(5)
            .build()));
    }

    @Test
    public void createTooManyAlerts() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        quotasDao.upsert("alerting", "project", null, "alerts.count", 10, "uranix", Instant.now());
        quotasDao.upsert("alerting", "project", projectId, "alerts.count", 20, "uranix", Instant.now());
        quotaWatcher.registerOnLoad(ignore -> latch.countDown());
        clock.passedTime(100, TimeUnit.SECONDS);
        latch.await();

        List<ERequestStatusCode> statuses = IntStream.range(0, 100).parallel()
            .mapToObj(ignore -> randomAlert())
            .map(alert -> service.createAlert(TCreateAlertRequest.newBuilder()
                    .setAlert(alert)
                    .build())
                .thenApply(TCreateAlertResponse::getRequestStatus))
            .collect(collectingAndThen(toList(), CompletableFutures::joinAll));

        HashMultiset<ERequestStatusCode> countStatuses = HashMultiset.create(statuses);
        assertThat(countStatuses.count(ERequestStatusCode.OK), equalTo(20));
        assertThat(countStatuses.count(ERequestStatusCode.RESOURCE_EXHAUSTED), equalTo(80));
    }

    @Test
    public void recycleAlertsAndNotHitLimit() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        quotasDao.upsert("alerting", "project", null, "alert.count", 10, "uranix", Instant.now());
        quotaWatcher.registerOnLoad(ignore -> latch.countDown());
        clock.passedTime(100, TimeUnit.SECONDS);
        latch.await();

        for (int i = 0; i < 100; i++) {
            TAlert alert = randomAlert();
            successCreate(alert);
            successDelete(alert.getId());
        }
    }

    private NotificationChannelStub registerNotificationWithStatus(NotificationStatus status) {
        NotificationChannelStub channel1 = randomNotification();
        channel1.predefineStatuses(status);
        channelStub.setNotificationChannel(channel1);
        return channel1;
    }

    @Test
    public void canceledNotAbleRunAgain() {
        service.run().join();
        service.close();
        // repeat run can be evaluated during retry init
        service.run().join();
        TCreateAlertResponse response = service.createAlert(TCreateAlertRequest.newBuilder()
                .setAlert(randomAlert())
                .build())
                .join();

        assertThat(response.getRequestStatus(), equalTo(ERequestStatusCode.SHARD_NOT_INITIALIZED));
    }

    @Test
    public void reportAlertMetrics() throws InterruptedException {
        for (EvaluationStatus.Code code : EvaluationStatus.Code.values()) {
            TAlert alert = successCreate(randomAlert()
                    .toBuilder()
                    .setName(code.name())
                    .clearGroupByLabels()
                    .setState(EAlertState.ACTIVE)
                    .build());
            assignmentService.predefineStatus(alert.getId(), code::toStatus);
            awaitEval(alert);

            TEvaluationState state = successReadEvalState(alert.getId());
            String expectedString = String.format("IGAUGE alert.evaluation.status{alertId='%s', projectId='%s'} [%s]",
                    alert.getId(),
                    alert.getProjectId(),
                    state.getStatus().getCode().getNumber());
            assertThat(getAlertMetrics(), containsString(expectedString));
        }
    }

    @Test
    public void reportMutedAlertMetrics() throws InterruptedException {
        Mute dt = SelectorsMute.newBuilder()
                .setProjectId(projectId)
                .setId("maintenance")
                .setName("Maintenance")
                .setFrom(Instant.EPOCH)
                .setTo(Instant.ofEpochMilli(Long.MAX_VALUE))
                .setTtlBase(Instant.ofEpochMilli(Long.MAX_VALUE))
                .setAlertSelector(ru.yandex.solomon.labels.query.Selector.any("alert"))
                .setCreatedBy("uranix")
                .setCreatedAt(clock.instant())
                .setUpdatedBy("uranix")
                .setUpdatedAt(clock.instant())
                .build();
        createMute(dt);

        for (EvaluationStatus.Code code : EvaluationStatus.Code.values()) {
            TAlert alert = successCreate(randomAlert()
                    .toBuilder()
                    .setName(code.name())
                    .clearGroupByLabels()
                    .setState(EAlertState.ACTIVE)
                    .build());
            assignmentService.predefineStatus(alert.getId(), code::toStatus);
            awaitEval(alert);

            TEvaluationState state = successReadEvalState(alert.getId());
            String expectedString = String.format("IGAUGE alert.evaluation.status{alertId='%s', projectId='%s'} [%s]",
                    alert.getId(),
                    alert.getProjectId(),
                    AlertStatusCodec.encode(
                            AlertConverter.protoToStatusCode(state.getStatus().getCode()),
                            ru.yandex.solomon.alert.rule.AlertMuteStatus.MuteStatusCode.MUTED)
            );
            assertThat(getAlertMetrics(), containsString(expectedString));
        }
    }

    @Test
    public void reportMutedMultiAlertMetrics() throws InterruptedException {
        unrollExecutor.predefineUnroll("root",
                () -> ImmutableSet.of(
                        Labels.of("host", "solomon-1"),
                        Labels.of("host", "solomon-2"),
                        Labels.of("host", "solomon-3"),
                        Labels.of("host", "solomon-4"),
                        Labels.of("host", "solomon-5")
                )
        );

        TAlert alert = successCreate(randomExpressionAlert()
                .toBuilder()
                .setId("root")
                .clearGroupByLabels()
                .addGroupByLabels("host")
                .setState(EAlertState.ACTIVE)
                .build());

        awaitUnroll(alert);

        String labelSelector = "host=";
        {
            TListSubAlert[] subAlerts = successList(TListSubAlertRequest.newBuilder()
                    .setParentId(alert.getId())
                    .setProjectId(alert.getProjectId())
                    .build());

            labelSelector += subAlerts[1].getLabels(0).getValue();
            labelSelector += "|" + subAlerts[3].getLabels(0).getValue();
        }

        Mute dt = SelectorsMute.newBuilder()
                .setProjectId(projectId)
                .setId("maintenance")
                .setName("Maintenance")
                .setFrom(Instant.EPOCH)
                .setTo(Instant.ofEpochMilli(Long.MAX_VALUE))
                .setTtlBase(Instant.ofEpochMilli(Long.MAX_VALUE))
                .setAlertSelector(ru.yandex.solomon.labels.query.Selector.any("alert"))
                .setLabelSelectors(SelectorsFormat.parse(labelSelector))
                .setCreatedBy("uranix")
                .setCreatedAt(clock.instant())
                .setUpdatedBy("uranix")
                .setUpdatedAt(clock.instant())
                .build();
        createMute(dt);

        TListSubAlert[] subAlerts = successList(TListSubAlertRequest.newBuilder()
                .setParentId(alert.getId())
                .setProjectId(alert.getProjectId())
                .build());

        final EvaluationStatus[] statusBySub = new EvaluationStatus[] {
                EvaluationStatus.ALARM,
                EvaluationStatus.OK,
                EvaluationStatus.NO_DATA,
                EvaluationStatus.WARN,
                EvaluationStatus.ERROR,
        };

        for (int i = 0; i < subAlerts.length; i++) {
            final int j = i;
            assignmentService.predefineStatus(subAlerts[j].getId(), () -> statusBySub[j]);
        }

        {
            List<Semaphore> syncs = Stream.of(subAlerts)
                    .map(a -> assignmentService.getSyncEval(a.getId()))
                    .collect(toList());

            for (var sync : syncs) {
                sync.drainPermits();
            }
            clock.passedTime(2, TimeUnit.MINUTES);
            for (var sync : syncs) {
                sync.acquire();
            }
        }

        var metrics = getAlertMetrics();

        for (int i = 0; i < subAlerts.length; i++) {
            assertThat(metrics, containsString(
                    "IGAUGE alert.evaluation.status{alertId='" + subAlerts[i].getId() +
                            "', parentId='root', projectId='" + projectId + "'} [" +
                            AlertStatusCodec.encode(statusBySub[i].getCode(), i == 1 || i == 3
                                    ? ru.yandex.solomon.alert.rule.AlertMuteStatus.MuteStatusCode.MUTED
                                    : ru.yandex.solomon.alert.rule.AlertMuteStatus.MuteStatusCode.NOT_MUTED)
                            + "]"));
        }

        for (var status : statusBySub) {
            assertThat(metrics, containsString(
                    "IGAUGE multiAlert.evaluation.status{alertId='root', projectId='" + projectId +
                            "', status='" + status.getCode() + "'} [1]"));
        }

        for (int i = 0; i < statusBySub.length; i++) {
            assertThat(metrics, containsString(
                    "IGAUGE multiAlert.evaluation.status{alertId='root', projectId='" + projectId +
                            "', status='" + statusBySub[i].getCode() + ":MUTED'} [" + (i == 1 || i == 3 ? "1" : "0") + "]"));
        }

        System.out.println(metrics);
    }

    @Test(expected = IllegalStateException.class)
    public void notAbleMakeSnapshotOnNotInitializedShard() {
        createBrokerService();
        assertThat(service.isReady(), equalTo(false));
        service.snapshot();
    }

    @Test
    public void projectSummaryStats() throws InterruptedException {
        TReadProjectStatsResponse empty = service.readProjectStats(TReadProjectStatsRequest.newBuilder()
                .setProjectId(projectId)
                .build())
                .join();

        assertThat(empty.getRequestStatus(), equalTo(ERequestStatusCode.OK));
        assertThat(empty.getMutedStats(), equalTo(TEvaluationStats.getDefaultInstance()));
        assertThat(empty.getEvaluationStats(), equalTo(TEvaluationStats.getDefaultInstance()));

        TAlert alarm = successCreate(randomAlert()
                .toBuilder()
                .setName("alarm alert one")
                .setFolderId("")
                .clearGroupByLabels()
                .build());

        assignmentService.predefineStatus(alarm.getId(), EvaluationStatus.Code.ALARM::toStatus);
        awaitEval(alarm);

        TReadProjectStatsResponse alarmSummary;
        do {
            clock.passedTime(15, TimeUnit.SECONDS);
            alarmSummary = service.readProjectStats(TReadProjectStatsRequest.newBuilder()
                    .setProjectId(projectId)
                    .build())
                    .join();
        } while (alarmSummary.getEvaluationStats().getCountAlarm() == 0);

        assertThat(alarmSummary.getRequestStatus(), equalTo(ERequestStatusCode.OK));
        assertThat(alarmSummary.getEvaluationStats(), equalTo(TEvaluationStats.newBuilder()
                .setCountAlarm(1)
                .build()));

        TAlert ok = successCreate(randomAlert()
                .toBuilder()
                .setName("ok alert")
                .clearGroupByLabels()
                .build());
        createMute(((SelectorsMute) randomMute(MuteType.BY_SELECTORS)).toBuilder()
                .setProjectId(projectId)
                .setAlertSelector(SelectorsFormat.parseSelector("alert='" + ok.getId() + "'"))
                .setLabelSelectors(SelectorsFormat.parse(""))
                .setFrom(clock.instant())
                .setTo(clock.instant().plusSeconds(3600))
                .build());
        assignmentService.predefineStatus(ok.getId(), EvaluationStatus.Code.OK::toStatus);
        awaitEval(ok);

        TReadProjectStatsResponse okAlarmSummary;
        do {
            // pass interval for refresh summary
            clock.passedTime(15, TimeUnit.SECONDS);
            okAlarmSummary = service.readProjectStats(TReadProjectStatsRequest.newBuilder()
                    .setProjectId(projectId)
                    .build())
                    .join();
        } while (okAlarmSummary.getEvaluationStats().getCountOk() == 0);

        assertThat(okAlarmSummary.getRequestStatus(), equalTo(ERequestStatusCode.OK));
        assertThat(okAlarmSummary.getEvaluationStats(), equalTo(TEvaluationStats.newBuilder()
                .setCountAlarm(1)
                .setCountOk(1)
                .build()));
        assertThat(okAlarmSummary.getMutedStats(), equalTo(TEvaluationStats.newBuilder()
                .setCountOk(1)
                .build()));
    }

    @Test
    public void projectSummaryStatsByFolder() throws InterruptedException {
        TReadProjectStatsResponse empty = service.readProjectStats(TReadProjectStatsRequest.newBuilder()
            .setProjectId(projectId)
            .setFolderId("some-folder-id")
            .build())
            .join();

        assertThat(empty.getRequestStatus(), equalTo(ERequestStatusCode.OK));
        assertThat(empty.getEvaluationStats(), equalTo(TEvaluationStats.getDefaultInstance()));

        TAlert alarm = successCreate(randomAlert()
            .toBuilder()
            .setName("alarm alert one")
            .setFolderId("some-folder-id")
            .clearGroupByLabels()
            .build());

        assignmentService.predefineStatus(alarm.getId(), EvaluationStatus.Code.ALARM::toStatus);
        awaitEval(alarm);

        TReadProjectStatsResponse alarmSummary;
        do {
            clock.passedTime(15, TimeUnit.SECONDS);
            alarmSummary = service.readProjectStats(TReadProjectStatsRequest.newBuilder()
                .setProjectId(projectId)
                .setFolderId("some-folder-id")
                .build())
                .join();
        } while (alarmSummary.getEvaluationStats().getCountAlarm() == 0);

        assertThat(alarmSummary.getRequestStatus(), equalTo(ERequestStatusCode.OK));
        assertThat(alarmSummary.getEvaluationStats(), equalTo(TEvaluationStats.newBuilder()
            .setCountAlarm(1)
            .build()));

        TAlert ok = successCreate(randomAlert()
            .toBuilder()
            .setName("ok alert")
            .clearGroupByLabels()
            .setFolderId("some-other-folder-id")
            .build());
        assignmentService.predefineStatus(ok.getId(), EvaluationStatus.Code.OK::toStatus);
        awaitEval(ok);

        TReadProjectStatsResponse okSummary;
        do {
            // pass interval for refresh summary
            clock.passedTime(15, TimeUnit.SECONDS);
            okSummary = service.readProjectStats(TReadProjectStatsRequest.newBuilder()
                .setProjectId(projectId)
                .setFolderId("some-other-folder-id")
                .build())
                .join();
        } while (okSummary.getEvaluationStats().getCountOk() == 0);

        assertThat(okSummary.getRequestStatus(), equalTo(ERequestStatusCode.OK));
        assertThat(okSummary.getEvaluationStats(), equalTo(TEvaluationStats.newBuilder()
            .setCountOk(1)
            .build()));
    }

    @Test
    public void recreateDeleted() {
        // https://st.yandex-team.ru/SOLOMON-4610
        TExpression e1 = TExpression.newBuilder().setCheckExpression("true").build();
        TExpression e2 = TExpression.newBuilder().setCheckExpression("false").build();
        TAlert source = randomExpressionAlert().toBuilder().setExpression(e1).build();
        String id = source.getId();
        successCreate(source);
        successDelete(id);
        TAlert recreate = successCreate(source.toBuilder().setExpression(e2).build());
        assertThat(recreate.getExpression().getCheckExpression(), equalTo("false"));
        TAlert reread = successRead(id);
        assertThat(reread.getExpression().getCheckExpression(), equalTo("false"));
    }

    @Test
    public void muteSimpleAlert() throws InterruptedException {
        var alert = randomAlert().toBuilder()
                        .clearGroupByLabels()
                        .build();
        successCreate(alert);

        var from = clock.instant().plus(4, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.MINUTES);
        var to = from.plus(3, ChronoUnit.MINUTES);

        Mute dt = SelectorsMute.newBuilder()
                .setProjectId(projectId)
                .setId("maintenance")
                .setName("Maintenance")
                .setFrom(from)
                .setTo(to)
                .setTtlBase(to)
                .setAlertSelector(ru.yandex.solomon.labels.query.Selector.exact("alert", alert.getId()))
                .setCreatedBy("uranix")
                .setCreatedAt(clock.instant())
                .setUpdatedBy("uranix")
                .setUpdatedAt(clock.instant())
                .build();

        createMute(dt);

        int mutedMinutes = 0;

        for (int i = 0; i < 10; i++) {
            awaitEval(alert);
            var response = successReadEvaluationState(alert.getId());
            if (response.muteStatus.getCode() == AlertMuteStatus.Code.MUTED) {
                mutedMinutes++;
            }
        }

        assertEquals(3, mutedMinutes);
    }

    @Test
    public void expiredMute() throws InterruptedException {
        var alert = randomAlert().toBuilder()
                .clearGroupByLabels()
                .build();
        successCreate(alert);

        var from = clock.instant().plus(4, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.MINUTES);
        var to = from.plus(3, ChronoUnit.MINUTES);

        Mute dt = SelectorsMute.newBuilder()
                .setProjectId(projectId)
                .setId("maintenance")
                .setName("Maintenance")
                .setFrom(from.minusSeconds(3600))
                .setTo(to.minusSeconds(3600))
                .setTtlBase(to)
                .setAlertSelector(ru.yandex.solomon.labels.query.Selector.exact("alert", alert.getId()))
                .setCreatedBy("uranix")
                .setCreatedAt(clock.instant())
                .setUpdatedBy("uranix")
                .setUpdatedAt(clock.instant())
                .build();

        createMute(dt);

        EnumMapToInt<MuteStatus> countDtByStatus = new EnumMapToInt<>(MuteStatus.class, 0);

        for (int i = 0; i < 10; i++) {
            awaitEval(alert);
            var response = successReadEvaluationState(alert.getId());
            response.muteStatus.getAffectingMutesList().forEach(
                    affDt -> countDtByStatus.addAndGet(affDt.getStatus(), 1)
            );
        }

        assertEquals(10, countDtByStatus.get(MuteStatus.EXPIRED));
    }

    @Test
    public void obsoleteMute() throws InterruptedException {
        var alert = randomAlert().toBuilder()
                .clearGroupByLabels()
                .build();
        successCreate(alert);

        var from = clock.instant().plus(4, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.MINUTES);
        var to = from.plus(3, ChronoUnit.MINUTES);

        Mute dt = SelectorsMute.newBuilder()
                .setProjectId(projectId)
                .setId("maintenance")
                .setName("Maintenance")
                .setFrom(from.minusSeconds(86400))
                .setTo(to.minusSeconds(86400))
                .setTtlBase(to)
                .setAlertSelector(ru.yandex.solomon.labels.query.Selector.exact("alert", alert.getId()))
                .setCreatedBy("uranix")
                .setCreatedAt(clock.instant())
                .setUpdatedBy("uranix")
                .setUpdatedAt(clock.instant())
                .build();

        createMute(dt);

        EnumMapToInt<MuteStatus> countDtByStatus = new EnumMapToInt<>(MuteStatus.class, 0);

        for (int i = 0; i < 10; i++) {
            awaitEval(alert);
            var response = successReadEvaluationState(alert.getId());
            response.muteStatus.getAffectingMutesList().forEach(
                    affDt -> countDtByStatus.addAndGet(affDt.getStatus(), 1)
            );
        }

        assertTrue(countDtByStatus.get(MuteStatus.EXPIRED) > 5);
        assertEquals("OBSOLETE matches must not be reported", 0, countDtByStatus.get(MuteStatus.ARCHIVED));
    }

    @Test
    public void transitionBetweenObsoleteAndRegular() throws InterruptedException {
        var alert = randomAlert().toBuilder()
                .clearGroupByLabels()
                .build();
        successCreate(alert);

        var from = clock.instant().plus(4, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.MINUTES);
        var to = from.plus(3, ChronoUnit.MINUTES);

        Mute dt = SelectorsMute.newBuilder()
                .setProjectId(projectId)
                .setId("maintenance")
                .setName("Maintenance")
                .setFrom(from.minusSeconds(86400))
                .setTo(to.minusSeconds(86400))
                .setTtlBase(to)
                .setAlertSelector(ru.yandex.solomon.labels.query.Selector.exact("alert", alert.getId()))
                .setCreatedBy("uranix")
                .setCreatedAt(clock.instant())
                .setUpdatedBy("uranix")
                .setUpdatedAt(clock.instant())
                .build();

        createMute(dt);

        EnumMapToInt<MuteStatus> countDtByStatus = new EnumMapToInt<>(MuteStatus.class, 0);

        for (int i = 0; i < 20; i++) {
            awaitEval(alert);
            var response = successReadEvaluationState(alert.getId());
            response.muteStatus.getAffectingMutesList().forEach(
                    affDt -> countDtByStatus.addAndGet(affDt.getStatus(), 1)
            );
        }

        StringWriter writer = new StringWriter();
        try (MetricTextEncoder e = new MetricTextEncoder(writer, true)) {
            e.onStreamBegin(-1);
            muteService.getMetrics().append(0, Labels.of(), e);
            e.onStreamEnd();
        }
        assertEquals(0, muteService.getRegularMutesCount());

        var list = muteService.listMutes(ListMutesRequest.getDefaultInstance()).join();
        assertEquals(1, list.getMutesCount());
        assertEquals(MuteStatus.ARCHIVED, list.getMutes(0).getStatus());

        updateMute(dt.toBuilder().setVersion(1).setTo(to.plusSeconds(3600)).build());

        awaitEval(alert);

        for (int i = 0; i < 3; i++) {
            awaitEval(alert);
            var response = successReadEvaluationState(alert.getId());
            assertEquals(MuteStatus.ACTIVE, response.muteStatus.getAffectingMutesList().get(0).getStatus());
        }
        assertEquals(1, muteService.getRegularMutesCount());
    }

    @Test
    public void muteWildcardSimpleAlert() throws InterruptedException {
        var alert = randomAlert().toBuilder()
                .clearGroupByLabels()
                .build();
        successCreate(alert);

        var from = clock.instant().plus(4, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.MINUTES);
        var to = from.plus(3, ChronoUnit.MINUTES);

        Mute dt = SelectorsMute.newBuilder()
                .setProjectId(projectId)
                .setId("maintenance")
                .setName("Maintenance")
                .setFrom(from)
                .setTo(to)
                .setTtlBase(to)
                .setAlertSelector(ru.yandex.solomon.labels.query.Selector.any("alert"))
                .setCreatedBy("uranix")
                .setCreatedAt(clock.instant())
                .setUpdatedBy("uranix")
                .setUpdatedAt(clock.instant())
                .build();

        createMute(dt);

        int mutedMinutes = 0;

        EnumMap<MuteStatus, MutableInt> counterByMuteStatus = new EnumMap<>(MuteStatus.class);

        for (int i = 0; i < 10; i++) {
            awaitEval(alert);
            var response = successReadEvaluationState(alert.getId());
            var alertMuteCode = response.muteStatus.getCode();
            var muteStatus = response.muteStatus.getAffectingMutes(0).getStatus();
            if (alertMuteCode == AlertMuteStatus.Code.MUTED) {
                mutedMinutes++;
            }
            assertEquals(
                    alertMuteCode == AlertMuteStatus.Code.MUTED,
                    muteStatus == MuteStatus.ACTIVE);
            if (mutedMinutes == 0) {
                assertEquals(muteStatus, MuteStatus.PENDING);
            }
            counterByMuteStatus.computeIfAbsent(muteStatus, ignore -> new MutableInt(0)).increment();
        }
        assertTrue(counterByMuteStatus.get(MuteStatus.PENDING).intValue() > 0);
        assertTrue(counterByMuteStatus.get(MuteStatus.ACTIVE).intValue() > 0);
        assertTrue(counterByMuteStatus.get(MuteStatus.EXPIRED).intValue() > 0);

        assertEquals(3, mutedMinutes);
    }

    @Test
    public void muteForeignSimpleAlert() throws InterruptedException {
        var alert = randomAlert().toBuilder()
                .clearGroupByLabels()
                .build();
        successCreate(alert);

        var from = clock.instant().plus(4, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.MINUTES);
        var to = from.plus(3, ChronoUnit.MINUTES);

        Mute dt = SelectorsMute.newBuilder()
                .setProjectId(projectId)
                .setId("maintenance")
                .setName("Maintenance")
                .setFrom(from)
                .setTo(to)
                .setTtlBase(to)
                .setAlertSelector(ru.yandex.solomon.labels.query.Selector.glob("alert", "foo|bar"))
                .setCreatedBy("uranix")
                .setCreatedAt(clock.instant())
                .setUpdatedBy("uranix")
                .setUpdatedAt(clock.instant())
                .build();

        createMute(dt);

        int mutedMinutes = 0;

        for (int i = 0; i < 10; i++) {
            awaitEval(alert);
            var response = successReadEvaluationState(alert.getId());
            if (response.muteStatus.getCode() == AlertMuteStatus.Code.MUTED) {
                mutedMinutes++;
            }
        }

        assertEquals(0, mutedMinutes);
    }

    @Test
    public void muteDeleted() throws InterruptedException {
        var alert = randomAlert().toBuilder()
                .clearGroupByLabels()
                .build();
        successCreate(alert);

        var from = clock.instant().minus(4, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.MINUTES);
        var to = from.plus(1, ChronoUnit.HOURS);

        Mute dt = SelectorsMute.newBuilder()
                .setProjectId(projectId)
                .setId("maintenance")
                .setName("Maintenance")
                .setFrom(from)
                .setTo(to)
                .setTtlBase(to)
                .setAlertSelector(ru.yandex.solomon.labels.query.Selector.any("alert"))
                .setCreatedBy("uranix")
                .setCreatedAt(clock.instant())
                .setUpdatedBy("uranix")
                .setUpdatedAt(clock.instant())
                .build();

        createMute(dt);

        int mutedMinutes = 0;

        for (int i = 0; i < 10; i++) {
            awaitEval(alert);
            if (i == 3) {
                deleteMute(dt.getId());
            }
            var response = successReadEvaluationState(alert.getId());
            if (response.muteStatus.getCode() == AlertMuteStatus.Code.MUTED) {
                mutedMinutes++;
            }
        }

        assertThat(mutedMinutes, lessThan(5));
    }

    @Test
    public void muteChanged() throws InterruptedException {
        var alert = randomAlert().toBuilder()
                .clearGroupByLabels()
                .build();
        successCreate(alert);

        var from = clock.instant().minus(4, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.MINUTES);
        var to = from.plus(1, ChronoUnit.HOURS);

        SelectorsMute dt = SelectorsMute.newBuilder()
                .setProjectId(projectId)
                .setId("maintenance")
                .setName("Maintenance")
                .setFrom(from)
                .setTo(to)
                .setTtlBase(to)
                .setAlertSelector(ru.yandex.solomon.labels.query.Selector.any("alert"))
                .setCreatedBy("uranix")
                .setCreatedAt(clock.instant())
                .setUpdatedBy("uranix")
                .setUpdatedAt(clock.instant())
                .build();

        createMute(dt);

        int mutedMinutes = 0;

        for (int i = 0; i < 10; i++) {
            awaitEval(alert);
            if (i == 3) {
                dt = dt.toBuilder()
                        .setLabelSelectors(SelectorsFormat.parse("host=solomon-04"))
                        .build();
                deleteMute(dt.getId());
            }
            var response = successReadEvaluationState(alert.getId());
            if (response.muteStatus.getCode() == AlertMuteStatus.Code.MUTED) {
                mutedMinutes++;
            }
        }

        assertThat(mutedMinutes, lessThan(5));
    }

    private static class VariableStatus implements Supplier<EvaluationStatus> {
        private final AtomicInteger counter = new AtomicInteger(0);
        private final IntFunction<EvaluationStatus> generator;

        private VariableStatus(IntFunction<EvaluationStatus> generator) {
            this.generator = generator;
        }

        @Override
        public EvaluationStatus get() {
            return generator.apply(counter.getAndIncrement());
        }
    }

    @Test
    public void doNotNotifyIfMuteSet() throws InterruptedException {
        clock.passedTime(Instant.parse("2000-01-01T00:00:00Z").toEpochMilli() - clock.instant().toEpochMilli(), TimeUnit.MILLISECONDS);

        MessageBoxChannel channel = new MessageBoxChannel(clock, NotificationTestSupport.randomNotification()
                .toBuilder()
                .setProjectId(projectId)
                .setNotifyAboutStatus(EvaluationStatus.Code.ALARM, EvaluationStatus.Code.WARN)
                .build());
        channelStub.setNotificationChannel(channel);

        var alert1 = randomAlert().toBuilder()
                .clearGroupByLabels()
                .clearConfiguredNotificationChannels()
                .clearNotificationChannelIds()
                .addNotificationChannelIds(channel.getId())
                .build();
        var alert2 = randomAlert().toBuilder()
                .clearGroupByLabels()
                .clearConfiguredNotificationChannels()
                .clearNotificationChannelIds()
                .addNotificationChannelIds(channel.getId())
                .build();
        assignmentService.predefineStatus(alert1.getId(), new VariableStatus(
                i -> (i == 7) ? EvaluationStatus.ALARM : EvaluationStatus.OK));
        assignmentService.predefineStatus(alert2.getId(), new VariableStatus(
                i -> (i == 7) ? EvaluationStatus.ALARM : EvaluationStatus.OK));

        successCreate(alert1);
        successCreate(alert2);

        var from = Instant.parse("2000-01-01T00:05:00Z");
        var to = Instant.parse("2000-01-01T00:10:00Z");

        SelectorsMute dt = SelectorsMute.newBuilder()
                .setProjectId(projectId)
                .setId("maintenance")
                .setName("Maintenance")
                .setFrom(from)
                .setTo(to)
                .setTtlBase(to)
                .setAlertSelector(ru.yandex.solomon.labels.query.Selector.exact("alert", alert1.getId()))
                .setCreatedBy("uranix")
                .setCreatedAt(clock.instant())
                .setUpdatedBy("uranix")
                .setUpdatedAt(clock.instant())
                .build();

        createMute(dt);

        int mutedMinutes = 0;

        for (int i = 0; i < 20; i++) {
            var sync1 = assignmentService.getSyncEval(alert1.getId());
            var sync2 = assignmentService.getSyncEval(alert2.getId());
            clock.passedTime(1, TimeUnit.MINUTES);
            sync1.acquire();
            sync2.acquire();
            var response = successReadEvaluationState(alert1.getId());
            if (response.muteStatus.getCode() == AlertMuteStatus.Code.MUTED) {
                mutedMinutes++;
            }
        }

        assertThat(mutedMinutes, lessThan(7));
        assertThat(channel.getOutbox(), iterableWithSize(1));
        assertThat(channel.getOutbox().get(0).event().getAlert().getId(), equalTo(alert2.getId()));
        assertThat(channel.getOutbox().get(0).sendTime(), lessThan(to));
    }

    @Test
    public void notifyAfterMuteIfAlarm() throws InterruptedException {
        clock.passedTime(Instant.parse("2000-01-01T00:00:00Z").toEpochMilli() - clock.instant().toEpochMilli(), TimeUnit.MILLISECONDS);

        MessageBoxChannel channel = new MessageBoxChannel(clock, NotificationTestSupport.randomNotification()
                .toBuilder()
                .setProjectId(projectId)
                .setNotifyAboutStatus(EvaluationStatus.Code.ALARM, EvaluationStatus.Code.WARN)
                .setRepeatNotifyDelay(Duration.ZERO)
                .build());
        channelStub.setNotificationChannel(channel);

        var alert1 = randomAlert().toBuilder()
                .clearGroupByLabels()
                .clearConfiguredNotificationChannels()
                .clearNotificationChannelIds()
                .addNotificationChannelIds(channel.getId())
                .build();
        var alert2 = randomAlert().toBuilder()
                .clearGroupByLabels()
                .clearConfiguredNotificationChannels()
                .clearNotificationChannelIds()
                .addNotificationChannelIds(channel.getId())
                .build();
        assignmentService.predefineStatus(alert1.getId(), new VariableStatus(
                i -> (i > 7) ? EvaluationStatus.ALARM : EvaluationStatus.OK));
        assignmentService.predefineStatus(alert2.getId(), new VariableStatus(
                i -> (i > 7) ? EvaluationStatus.ALARM : EvaluationStatus.OK));

        successCreate(alert1);
        successCreate(alert2);

        var from = Instant.parse("2000-01-01T00:05:00Z");
        var to = Instant.parse("2000-01-01T00:10:00Z");

        SelectorsMute dt = SelectorsMute.newBuilder()
                .setProjectId(projectId)
                .setId("maintenance")
                .setName("Maintenance")
                .setFrom(from)
                .setTo(to)
                .setTtlBase(to)
                .setAlertSelector(ru.yandex.solomon.labels.query.Selector.exact("alert", alert1.getId()))
                .setCreatedBy("uranix")
                .setCreatedAt(clock.instant())
                .setUpdatedBy("uranix")
                .setUpdatedAt(clock.instant())
                .build();

        createMute(dt);

        int mutedMinutes = 0;

        var sync1 = assignmentService.getSyncEval(alert1.getId());
        var sync2 = assignmentService.getSyncEval(alert2.getId());
        sync1.drainPermits();
        sync1.drainPermits();

        for (int i = 0; i < 20; i++) {
            clock.passedTime(1, TimeUnit.MINUTES);
            sync1.acquire();
            sync2.acquire();
            var response = successReadEvaluationState(alert1.getId());
            if (response.muteStatus.getCode() == AlertMuteStatus.Code.MUTED) {
                mutedMinutes++;
            }
        }

        assertThat(mutedMinutes, lessThan(7));

        for (var msg : channel.getOutbox()) {
            System.out.println(msg);
        }

        assertThat(channel.getOutbox(), iterableWithSize(2));
        assertThat(channel.getOutbox().get(0).event().getAlert().getId(), equalTo(alert2.getId()));
        assertThat(channel.getOutbox().get(0).sendTime(), lessThan(to));
        assertThat(channel.getOutbox().get(1).event().getAlert().getId(), equalTo(alert1.getId()));
        assertThat(channel.getOutbox().get(1).sendTime(), greaterThanOrEqualTo(to));
    }

    @Test
    public void doNotNotifyIfMuteFinishedWithSameStatus() throws InterruptedException {
        clock.passedTime(Instant.parse("2000-01-01T00:00:00Z").toEpochMilli() - clock.instant().toEpochMilli(), TimeUnit.MILLISECONDS);

        MessageBoxChannel channel = new MessageBoxChannel(clock, NotificationTestSupport.randomNotification()
                .toBuilder()
                .setProjectId(projectId)
                .setNotifyAboutStatus(EvaluationStatus.Code.ALARM, EvaluationStatus.Code.WARN)
                .build());
        channelStub.setNotificationChannel(channel);

        var alert = randomAlert().toBuilder()
                .clearGroupByLabels()
                .clearConfiguredNotificationChannels()
                .clearNotificationChannelIds()
                .addNotificationChannelIds(channel.getId())
                .build();
        assignmentService.predefineStatus(alert.getId(), new VariableStatus(
                i -> (i == 7) ? EvaluationStatus.ALARM : EvaluationStatus.WARN));


        successCreate(alert);

        var from = Instant.parse("2000-01-01T00:05:00Z");
        var to = Instant.parse("2000-01-01T00:10:00Z");

        SelectorsMute dt = SelectorsMute.newBuilder()
                .setProjectId(projectId)
                .setId("maintenance")
                .setName("Maintenance")
                .setFrom(from)
                .setTo(to)
                .setTtlBase(to)
                .setAlertSelector(ru.yandex.solomon.labels.query.Selector.exact("alert", alert.getId()))
                .setCreatedBy("uranix")
                .setCreatedAt(clock.instant())
                .setUpdatedBy("uranix")
                .setUpdatedAt(clock.instant())
                .build();

        createMute(dt);

        int mutedMinutes = 0;

        for (int i = 0; i < 20; i++) {
            var sync = assignmentService.getSyncEval(alert.getId());
            clock.passedTime(1, TimeUnit.MINUTES);
            sync.acquire();
            var response = successReadEvaluationState(alert.getId());
            if (response.muteStatus.getCode() == AlertMuteStatus.Code.MUTED) {
                mutedMinutes++;
            }
        }

        assertThat(mutedMinutes, lessThan(7));
        assertThat(channel.getOutbox(), iterableWithSize(1));
        assertThat(channel.getOutbox().get(0).sendTime(), lessThan(from));
    }

    @Test
    public void readInterpolatedExpressionAlert() {
        TAlert v1 = successCreate(randomExpressionAlert());

        var response = service.readAlert(TReadAlertInterpolatedRequest.newBuilder()
                .setAlertId(v1.getId())
                .setProjectId(v1.getProjectId())
                .build())
                .join();

        assertEquals(response.getAlert(), response.getInterpolatedAlert());
    }

    @Test
    public void readInterpolatedThresholdAlert() {
        TAlert v1 = successCreate(randomThresholdAlert());

        var response = service.readAlert(TReadAlertInterpolatedRequest.newBuilder()
                .setAlertId(v1.getId())
                .setProjectId(v1.getProjectId())
                .build())
                .join();

        assertEquals(response.getAlert(), response.getInterpolatedAlert());
    }

    @Test
    public void readInterpolatedTemplateAlert() {
        TAlert v1 = successCreate(randomAlertFromTemplate());

        var response = service.readAlert(TReadAlertInterpolatedRequest.newBuilder()
                .setAlertId(v1.getId())
                .setProjectId(v1.getProjectId())
                .build())
                .join();

        assertEquals(v1, response.getAlert());
        assertNotEquals(TAlert.TypeCase.ALERT_FROM_TEMPLATE, response.getInterpolatedAlert().getTypeCase());
        assertNotEquals(v1, response.getInterpolatedAlert());
    }

    @Test
    public void alertTemplateVersionUpdate() {
        TAlert v1 = successCreate(randomAlertFromTemplate());
        TAlert v2 = successUpdateAlertTemplateVersion(v1.toBuilder()
                .setAlertFromTemplate(v1.getAlertFromTemplate().toBuilder()
                        .setTemplateVersionTag("another")
                        .build())
                .build(), "another");

        TAlert read = successRead(v2.getId());
        assertThat(read, equalTo(v2));
        assertThat(read.getAlertFromTemplate().getTemplateVersionTag(), equalTo("another"));
        assertThat(read.getAlertFromTemplate().getTemplateId(), equalTo(v1.getAlertFromTemplate().getTemplateId()));
    }

    @Test
    public void notAbleUpdateNotExistsAlert_alertTemplateVersionUpdate() {
        UpdateAlertTemplateVersionResponse response = service.updateAlertTemplateVersion(UpdateAlertTemplateVersionRequest.newBuilder()
                .setTemplateId("")
                .setServiceProvider("")
                .setTemplateVersionTag("another")
                .setAlertData(UpdateAlertTemplateVersionRequest.AlertData.newBuilder()
                        .setAlertId("id1")
                        .build())
                .build())
                .join();

        assertThat(response.getRequestStatusCode(), equalTo(ERequestStatusCode.NOT_FOUND));
    }

    @Test
    public void updateNotAbleWhenServiceNotReady_alertTemplateVersionUpdate() {
        createBrokerService();
        UpdateAlertTemplateVersionResponse response = service.updateAlertTemplateVersion(UpdateAlertTemplateVersionRequest.newBuilder()
                .setTemplateId("")
                .setServiceProvider("")
                .setTemplateVersionTag("another")
                .setAlertData(UpdateAlertTemplateVersionRequest.AlertData.newBuilder()
                        .setAlertId("id1")
                        .build())
                .build())
                .join();

        assertThat(response.getRequestStatusCode(), equalTo(ERequestStatusCode.SHARD_NOT_INITIALIZED));
    }

    @Test
    public void anotherServiceProvider_alertTemplateVersionUpdate() {
        TAlert v1 = successCreate(randomAlertFromTemplate());
        UpdateAlertTemplateVersionResponse response = service.updateAlertTemplateVersion(UpdateAlertTemplateVersionRequest.newBuilder()
                .setTemplateId("")
                .setServiceProvider("another one")
                .setTemplateVersionTag("another")
                .setAlertData(UpdateAlertTemplateVersionRequest.AlertData.newBuilder()
                        .setAlertId(v1.getId())
                        .build())
                .build())
                .join();

        assertThat(response.getRequestStatusCode(), equalTo(ERequestStatusCode.NOT_AUTHORIZED));
    }

    @Test
    public void anotherAlertType_alertTemplateVersionUpdate() {
        TAlert v1 = successCreate(randomExpressionAlert());
        UpdateAlertTemplateVersionResponse response = service.updateAlertTemplateVersion(UpdateAlertTemplateVersionRequest.newBuilder()
                .setTemplateId("")
                .setServiceProvider("")
                .setTemplateVersionTag("another")
                .setAlertData(UpdateAlertTemplateVersionRequest.AlertData.newBuilder()
                        .setAlertId(v1.getId())
                        .build())
                .build())
                .join();

        assertThat(response.getRequestStatusCode(), equalTo(ERequestStatusCode.INVALID_REQUEST));
        assertThat(response.getStatusMessage(), equalTo("Can't update template version for alert: " + v1.getId()));
    }

    @Test
    public void failedChangeTemplateId_alertTemplateVersionUpdate() {
        TAlert v1 = successCreate(randomAlertFromTemplate());
        UpdateAlertTemplateVersionResponse response = service.updateAlertTemplateVersion(UpdateAlertTemplateVersionRequest.newBuilder()
                .setTemplateId("another template")
                .setServiceProvider("")
                .setTemplateVersionTag("another")
                .setAlertData(UpdateAlertTemplateVersionRequest.AlertData.newBuilder()
                        .setAlertId(v1.getId())
                        .build())
                .build())
                .join();

        assertThat(response.getRequestStatusCode(), equalTo(ERequestStatusCode.INVALID_REQUEST));
        assertThat(response.getStatusMessage(), equalTo("Can't update alert to another template, for alert: " + v1.getId()));
    }

    @Test
    public void notModified_alertTemplateVersionUpdate() {
        TAlert v1 = successCreate(randomAlertFromTemplate());
        UpdateAlertTemplateVersionResponse response = service.updateAlertTemplateVersion(UpdateAlertTemplateVersionRequest.newBuilder()
                .setTemplateId(v1.getAlertFromTemplate().getTemplateId())
                .setServiceProvider("")
                .setTemplateVersionTag(v1.getAlertFromTemplate().getTemplateVersionTag())
                .setAlertData(UpdateAlertTemplateVersionRequest.AlertData.newBuilder()
                        .setAlertId(v1.getId())
                        .build())
                .build())
                .join();

        assertThat(response.getRequestStatusCode(), equalTo(ERequestStatusCode.OK));
        assertThat(response.getStatusMessage(), equalTo("Not Modified"));
    }

    @Test
    public void updateByCount_alertTemplateVersionUpdate() {
        var templateId = "id1";
        var templateVersionTag = "tag1";
        for (int i = 0; i < 11; i++) {
            TAlert tAlert = randomAlertFromTemplate();
            successCreate(tAlert
                    .toBuilder()
                    .setAlertFromTemplate(tAlert.getAlertFromTemplate()
                            .toBuilder()
                            .setTemplateId(templateId)
                            .setTemplateVersionTag(templateVersionTag)
                            .build())
                    .build());
        }
        TAlert tAlert = randomAlertFromTemplate();
        successCreate(tAlert
                .toBuilder()
                .setAlertFromTemplate(tAlert.getAlertFromTemplate()
                        .toBuilder()
                        .setTemplateId(templateId)
                        .setTemplateVersionTag(templateVersionTag + 1)
                        .build())
                .build());
        successCreate(randomExpressionAlert());
        TAlert randomTemplateAlert = randomAlertFromTemplate();
        successCreate(randomTemplateAlert);

        UpdateAlertTemplateVersionResponse response = service.updateAlertTemplateVersion(UpdateAlertTemplateVersionRequest.newBuilder()
                .setTemplateId(templateId)
                .setServiceProvider("")
                .setTemplateVersionTag(templateVersionTag + 1)
                .setUpdateCount(10)
                .build())
                .join();

        assertThat(response.getRequestStatusCode(), equalTo(ERequestStatusCode.OK));
        assertEquals(response.getUpdated(), 10);

        response = service.updateAlertTemplateVersion(UpdateAlertTemplateVersionRequest.newBuilder()
                .setTemplateId(templateId)
                .setServiceProvider("")
                .setTemplateVersionTag(templateVersionTag + 1)
                .setUpdateCount(10)
                .build())
                .join();

        assertThat(response.getRequestStatusCode(), equalTo(ERequestStatusCode.OK));
        assertEquals(response.getUpdated(), 1);

        var alerts = service.listAlerts(TListAlertRequest.newBuilder()
                .setProjectId(projectId)
                .setPageSize(30)
                .setFullResultModel(true)
                .build())
                .join();

        assertEquals(1, alerts.getAlertList().getAlertsList().stream()
                .filter(tListAlert -> tListAlert.getTypeCase() == TAlert.TypeCase.EXPRESSION)
                .count());
        assertEquals(1, alerts.getAlertList().getAlertsList().stream()
                .filter(tListAlert -> tListAlert.getTypeCase() == TAlert.TypeCase.ALERT_FROM_TEMPLATE &&
                        tListAlert.getAlertFromTemplate().getTemplateId().equals(randomTemplateAlert.getAlertFromTemplate().getTemplateId()) &&
                                tListAlert.getAlertFromTemplate().getTemplateVersionTag().equals(randomTemplateAlert.getAlertFromTemplate().getTemplateVersionTag()))
                .count());
        assertEquals(12, alerts.getAlertList().getAlertsList().stream()
                .filter(tListAlert -> tListAlert.getTypeCase() == TAlert.TypeCase.ALERT_FROM_TEMPLATE &&
                        tListAlert.getAlertFromTemplate().getTemplateId().equals(templateId) &&
                        tListAlert.getAlertFromTemplate().getTemplateVersionTag().equals(templateVersionTag + 1))
                .count());
    }

    @Test
    public void createAlertsFromTemplateRequest() {
        TAlert v1 = successCreate(randomAlertFromTemplate());
        var template = alertTemplateDao.findById(v1.getAlertFromTemplate().getTemplateId(), v1.getAlertFromTemplate().getTemplateVersionTag()).join().get();
        var response = service.createAlerts(CreateAlertsFromTemplateRequest.newBuilder()
                .setServiceProviderId(template.getServiceProviderId())
                .addTemplateIds(template.getId())
                .setProjectId("another")
                .setCreatedBy("creator")
                .addAllResources(List.of(
                        CreateAlertsFromTemplateRequest.Resource.newBuilder()
                                .putAllResourceParameters(Map.of("1", "2"))
                                .build(),
                        CreateAlertsFromTemplateRequest.Resource.newBuilder()
                                .putAllResourceParameters(Map.of("2", "3"))
                                .build()
                ))
                .build())
                .join();

        assertThat(response.getRequestStatusCode(), equalTo(ERequestStatusCode.OK));
        assertEquals(2, response.getAlertsList().size());
    }

    @Test
    public void createAlertsFromTemplateRequest_idempotent() {
        TAlert v1 = successCreate(randomAlertFromTemplate());
        var template = alertTemplateDao.findById(v1.getAlertFromTemplate().getTemplateId(), v1.getAlertFromTemplate().getTemplateVersionTag()).join().get();

        var alert2 = randomAlertFromTemplate();
        alert2 = alert2.toBuilder().setAlertFromTemplate(alert2.getAlertFromTemplate().toBuilder().setTemplateId("1234").build()).build();
        TAlert v2 = successCreate(alert2);
        var template2 = alertTemplateDao.findById(v2.getAlertFromTemplate().getTemplateId(), v1.getAlertFromTemplate().getTemplateVersionTag()).join().get();
        var response = service.createAlerts(CreateAlertsFromTemplateRequest.newBuilder()
                .setServiceProviderId(template.getServiceProviderId())
                .addTemplateIds(template.getId())
                .setProjectId("another")
                .setCreatedBy("creator")
                .addAllResources(List.of(
                        CreateAlertsFromTemplateRequest.Resource.newBuilder()
                                .putAllResourceParameters(Map.of("1", "2"))
                                .build()
                ))
                .build())
                .join();

        assertThat(response.getRequestStatusCode(), equalTo(ERequestStatusCode.OK));
        assertEquals(1, response.getAlertsList().size());

        response = service.createAlerts(CreateAlertsFromTemplateRequest.newBuilder()
                .setServiceProviderId(template.getServiceProviderId())
                .addTemplateIds(template.getId())
                .addTemplateIds(template2.getId())
                .setProjectId("another")
                .setCreatedBy("creator")
                .addAllResources(List.of(
                        CreateAlertsFromTemplateRequest.Resource.newBuilder()
                                .putAllResourceParameters(Map.of("1", "2"))
                                .build(),
                        CreateAlertsFromTemplateRequest.Resource.newBuilder()
                                .putAllResourceParameters(Map.of("2", "2"))
                                .build()
                ))
                .build())
                .join();

        assertThat(response.getRequestStatusCode(), equalTo(ERequestStatusCode.OK));
        assertEquals(4, response.getAlertsList().size());

        TListAlert[] list = successList(TListAlertRequest.newBuilder()
                .setProjectId(projectId)
                .setPageSize(10)
                .build());

        assertEquals(6, list.length);
    }

    @Test
    public void listAlertLabels() {
        var projectId = "solomon";
        var response = successListLabels(projectId);
        assertEquals(List.of(), response.getLabelKeysList());

        successCreate(randomExpressionAlert().toBuilder().clearLabels().build());
        response = successListLabels(projectId);
        assertEquals(List.of(), response.getLabelKeysList());

        //l1-1
        var alert1 = successCreate(randomExpressionAlert().toBuilder().clearLabels().putAllLabels(Map.of("l1", "")).build());
        response = successListLabels(projectId);
        assertEquals(List.of("l1"), response.getLabelKeysList());

        //l1-2
        var alert2 = successCreate(randomExpressionAlert().toBuilder().clearLabels().putAllLabels(Map.of("l1", "")).build());
        response = successListLabels(projectId);
        assertEquals(List.of("l1"), response.getLabelKeysList());

        //l1-2, l2-1, l3-1
        var alert3 = successCreate(randomExpressionAlert().toBuilder().clearLabels().putAllLabels(Map.of("l2", "", "l3", "")).build());
        response = successListLabels(projectId);
        assertEquals(List.of("l1", "l2", "l3"), response.getLabelKeysList());

        //l1-2, l2-1, l3-1
        alert1 = successUpdate(alert1.toBuilder()
                .clearLabels()
                .putAllLabels(Map.of("l1", ""))
                .build());

        response = successListLabels(projectId);
        assertEquals(List.of("l1", "l2", "l3"), response.getLabelKeysList());

        //l1-1, l2-1, l3-1
        alert1 = successUpdate(alert1.toBuilder()
                .clearLabels()
                .build());

        response = successListLabels(projectId);
        assertEquals(List.of("l1", "l2", "l3"), response.getLabelKeysList());

        //l2-1, l3-1
        alert2 = successUpdate(alert2.toBuilder()
                .clearLabels()
                .build());

        response = successListLabels(projectId);
        assertEquals(List.of("l2", "l3"), response.getLabelKeysList());

        //all labels deleted
        successDelete(alert3.getId());
        response = successListLabels(projectId);
        assertEquals(List.of(), response.getLabelKeysList());
    }

    private static record StateAndMutes(TEvaluationState state, AlertMuteStatus muteStatus) {
    }

    private StateAndMutes successReadEvaluationState(String alertId) {
        TReadEvaluationStateResponse response = service.readEvaluationState(
                        TReadEvaluationStateRequest.newBuilder()
                                .setAlertId(alertId)
                                .setProjectId(projectId)
                                .build())
                .join();
        assertThat(response.getStatusMessage(), response.getRequestStatus(), equalTo(ERequestStatusCode.OK));
        return new StateAndMutes(response.getState(), response.getMuteStatus());
    }
}
