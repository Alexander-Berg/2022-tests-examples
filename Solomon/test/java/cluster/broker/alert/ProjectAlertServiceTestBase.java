package ru.yandex.solomon.alert.cluster.broker.alert;

import java.io.StringWriter;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.Iterables;

import ru.yandex.misc.concurrent.CompletableFutures;
import ru.yandex.monlib.metrics.encode.text.MetricTextEncoder;
import ru.yandex.monlib.metrics.registry.MetricRegistry;
import ru.yandex.solomon.alert.EvaluationStatus;
import ru.yandex.solomon.alert.api.converters.AlertConverter;
import ru.yandex.solomon.alert.api.converters.MuteConverter;
import ru.yandex.solomon.alert.api.converters.NotificationConverter;
import ru.yandex.solomon.alert.cluster.broker.alert.activity.ActivityFactory;
import ru.yandex.solomon.alert.cluster.broker.alert.activity.SimpleActivitiesFactory;
import ru.yandex.solomon.alert.cluster.broker.alert.activity.TemplateActivityFactory;
import ru.yandex.solomon.alert.cluster.broker.alert.activity.TemplateAlertFactory;
import ru.yandex.solomon.alert.cluster.broker.alert.activity.search.ActivityFilters;
import ru.yandex.solomon.alert.cluster.broker.alert.activity.search.ActivitySearch;
import ru.yandex.solomon.alert.cluster.broker.evaluation.EvaluationAssignmentServiceStub;
import ru.yandex.solomon.alert.cluster.broker.mute.ProjectMuteService;
import ru.yandex.solomon.alert.cluster.broker.mute.search.MuteSearch;
import ru.yandex.solomon.alert.cluster.broker.notification.StatefulNotificationChannelFactoryStub;
import ru.yandex.solomon.alert.cluster.project.ProjectAssignment;
import ru.yandex.solomon.alert.dao.AlertStatesDao;
import ru.yandex.solomon.alert.dao.EntitiesDao;
import ru.yandex.solomon.alert.dao.memory.InMemoryAlertTemplateDao;
import ru.yandex.solomon.alert.dao.memory.InMemoryMutesDao;
import ru.yandex.solomon.alert.domain.Alert;
import ru.yandex.solomon.alert.domain.AlertState;
import ru.yandex.solomon.alert.domain.AlertTestSupport;
import ru.yandex.solomon.alert.mute.domain.Mute;
import ru.yandex.solomon.alert.mute.domain.MuteStatus;
import ru.yandex.solomon.alert.notification.channel.NotificationChannelStub;
import ru.yandex.solomon.alert.notification.channel.telegram.ChatIdResolverStub;
import ru.yandex.solomon.alert.notification.domain.NotificationTestSupport;
import ru.yandex.solomon.alert.protobuf.AlertMuteStatus;
import ru.yandex.solomon.alert.protobuf.CreateMuteRequest;
import ru.yandex.solomon.alert.protobuf.DeleteMuteRequest;
import ru.yandex.solomon.alert.protobuf.EAlertType;
import ru.yandex.solomon.alert.protobuf.ERequestStatusCode;
import ru.yandex.solomon.alert.protobuf.ListAlertLabelsRequest;
import ru.yandex.solomon.alert.protobuf.ListAlertLabelsResponse;
import ru.yandex.solomon.alert.protobuf.TAlert;
import ru.yandex.solomon.alert.protobuf.TCreateAlertRequest;
import ru.yandex.solomon.alert.protobuf.TCreateAlertResponse;
import ru.yandex.solomon.alert.protobuf.TDeleteAlertRequest;
import ru.yandex.solomon.alert.protobuf.TDeleteAlertResponse;
import ru.yandex.solomon.alert.protobuf.TEvaluationState;
import ru.yandex.solomon.alert.protobuf.TEvaluationStats;
import ru.yandex.solomon.alert.protobuf.TEvaluationStatus;
import ru.yandex.solomon.alert.protobuf.TListAlert;
import ru.yandex.solomon.alert.protobuf.TListAlertRequest;
import ru.yandex.solomon.alert.protobuf.TListAlertResponse;
import ru.yandex.solomon.alert.protobuf.TListSubAlert;
import ru.yandex.solomon.alert.protobuf.TListSubAlertRequest;
import ru.yandex.solomon.alert.protobuf.TListSubAlertResponse;
import ru.yandex.solomon.alert.protobuf.TNotificationState;
import ru.yandex.solomon.alert.protobuf.TNotificationStats;
import ru.yandex.solomon.alert.protobuf.TReadAlertRequest;
import ru.yandex.solomon.alert.protobuf.TReadAlertResponse;
import ru.yandex.solomon.alert.protobuf.TReadEvaluationStateRequest;
import ru.yandex.solomon.alert.protobuf.TReadEvaluationStateResponse;
import ru.yandex.solomon.alert.protobuf.TReadEvaluationStatsRequest;
import ru.yandex.solomon.alert.protobuf.TReadEvaluationStatsResponse;
import ru.yandex.solomon.alert.protobuf.TReadNotificationStateRequest;
import ru.yandex.solomon.alert.protobuf.TReadNotificationStateResponse;
import ru.yandex.solomon.alert.protobuf.TReadNotificationStatsRequest;
import ru.yandex.solomon.alert.protobuf.TReadNotificationStatsResponse;
import ru.yandex.solomon.alert.protobuf.TReadSubAlertRequest;
import ru.yandex.solomon.alert.protobuf.TReadSubAlertResponse;
import ru.yandex.solomon.alert.protobuf.TSubAlert;
import ru.yandex.solomon.alert.protobuf.TUpdateAlertRequest;
import ru.yandex.solomon.alert.protobuf.TUpdateAlertResponse;
import ru.yandex.solomon.alert.protobuf.UpdateAlertTemplateVersionRequest;
import ru.yandex.solomon.alert.protobuf.UpdateAlertTemplateVersionResponse;
import ru.yandex.solomon.alert.protobuf.UpdateMuteRequest;
import ru.yandex.solomon.alert.rule.AlertEvalStateTestSupport;
import ru.yandex.solomon.alert.template.domain.AlertTemplateType;
import ru.yandex.solomon.alert.unroll.UnrollExecutorStub;
import ru.yandex.solomon.core.db.dao.QuotasDao;
import ru.yandex.solomon.idempotency.IdempotentOperationServiceImpl;
import ru.yandex.solomon.idempotency.dao.IdempotentOperationDao;
import ru.yandex.solomon.quotas.watcher.QuotaWatcher;
import ru.yandex.solomon.ut.ManualClock;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * @author Ivan Tsybulin
 */
@ParametersAreNonnullByDefault
class ProjectAlertServiceTestBase {
    protected String projectId;
    protected ProjectAssignment assignment;
    protected ManualClock clock;
    protected ScheduledExecutorService executorService;
    protected EntitiesDao<Alert> alertDao;
    protected AlertStatesDao statesDao;
    protected StatefulNotificationChannelFactoryStub channelStub;
    protected EvaluationAssignmentServiceStub assignmentService;
    protected UnrollExecutorStub unrollExecutor;
    protected ActivityFactory activityFactory;
    protected ProjectAlertService service;
    protected QuotasDao quotasDao;
    protected QuotaWatcher quotaWatcher;
    protected SimpleActivitiesFactory simpleActivitiesFactory;
    protected TemplateActivityFactory templateActivityFactory;
    protected InMemoryAlertTemplateDao alertTemplateDao;
    protected TemplateAlertFactory templateAlertFactory;
    protected ProjectMuteService muteService;
    protected IdempotentOperationDao idempotentDao;

    protected String getAlertMetrics() {
        StringWriter writer = new StringWriter();
        try (MetricTextEncoder e = new MetricTextEncoder(writer, true)) {
            e.onStreamBegin(-1);
            service.appendAlertMetrics(e);
            e.onStreamEnd();
        }
        return writer.toString();
    }

    protected TListAlert alertToListAlert(TAlert alert) {
        return alertToListAlert(alert, null, null);
    }

    protected TListAlert alertToListAlert(TAlert alert, @Nullable TEvaluationStatus.ECode code, @Nullable AlertMuteStatus.Code muteCode) {
        var builder = TListAlert.newBuilder()
                .setId(alert.getId())
                .setProjectId(alert.getProjectId())
                .setFolderId(alert.getFolderId())
                .setName(alert.getName())
                .setMultiAlert(alert.getGroupByLabelsCount() > 0)
                .setAlertState(alert.getState())
                .addAllNotificationChannelIds(alert.getNotificationChannelIdsList())
                .putAllLabels(alert.getLabelsMap())
                .putAllConfiguredNotificationChannels(alert.getConfiguredNotificationChannelsMap());
        switch (alert.getTypeCase()) {
            case THRESHOLD:
                builder.setAlertType(EAlertType.THRESHOLD);
                break;
            case EXPRESSION:
                builder.setAlertType(EAlertType.EXPRESSION);
                break;
            case ALERT_FROM_TEMPLATE:
                var template = alertTemplateDao.findById(alert.getAlertFromTemplate().getTemplateId(), alert.getAlertFromTemplate().getTemplateVersionTag()).join();
                builder.setAlertType(EAlertType.FROM_TEMPLATE)
                        .setTemplateData(TListAlert.FromTemplateData.newBuilder()
                                .setTemplateServiceProviderId(template.get().getServiceProviderId())
                                .setTemplateType(template.get().getAlertTemplateType() == AlertTemplateType.EXPRESSION
                                        ? EAlertType.EXPRESSION
                                        : EAlertType.THRESHOLD)
                                .build());
                break;
        }
        if (code != null) {
            builder.setEvaluationStatusCode(code);
        }

        if (muteCode != null) {
            builder.setMuteStatusCode(muteCode);
        }

        return builder.build();
    }

    protected void awaitNotify(String alertId, NotificationChannelStub channel) throws InterruptedException {
        TNotificationState prev = Iterables.getOnlyElement(
                filterStateByChannel(successReadNotificationState(alertId), channel));

        Semaphore sync = channel.getSendSync();
        sync.drainPermits();
        clock.passedTime(1, TimeUnit.MINUTES);
        sync.acquire();

        TNotificationState current;
        do {
            current = Iterables.getOnlyElement(
                    filterStateByChannel(successReadNotificationState(alertId), channel));
            if (prev.getLatestEvalMillis() < current.getLatestEvalMillis()) {
                return;
            }
            TimeUnit.MILLISECONDS.sleep(ThreadLocalRandom.current().nextLong(0, 5));
        } while (true);
    }

    protected void awaitNotify(String parentId, String alertId, NotificationChannelStub channel) throws InterruptedException {
        TNotificationState prev = Iterables.getOnlyElement(
                filterStateByChannel(successReadNotificationState(parentId, alertId), channel));

        Semaphore sync = channel.getSendSync();
        sync.drainPermits();
        clock.passedTime(1, TimeUnit.MINUTES);
        sync.acquire();

        TNotificationState current;
        do {
            current = Iterables.getOnlyElement(
                    filterStateByChannel(successReadNotificationState(parentId, alertId), channel));
            if (prev.getLatestEvalMillis() < current.getLatestEvalMillis()) {
                return;
            }
            TimeUnit.MILLISECONDS.sleep(ThreadLocalRandom.current().nextLong(0, 5));
        } while (true);
    }

    protected List<TNotificationState> filterStateByChannel(
            List<TNotificationState> states,
            NotificationChannelStub channel)
    {
        return states
                .stream()
                .filter(state -> state != null && channel.getId().equals(state.getNotificationChannelId()))
                .collect(Collectors.toList());
    }

    protected void awaitEval(TAlert alert) throws InterruptedException {
        awaitEval(alert.getId());
    }

    protected void awaitEval(String id) throws InterruptedException {
        Semaphore evalSync = assignmentService.getSyncEval(id);
        evalSync.drainPermits();
        clock.passedTime(1, TimeUnit.MINUTES);
        evalSync.acquire();
    }

    protected boolean awaitEval(TAlert alert, long time, TimeUnit unit) throws InterruptedException {
        Semaphore evalSync = assignmentService.getSyncEval(alert.getId());
        evalSync.drainPermits();
        clock.passedTime(1, TimeUnit.MINUTES);
        return evalSync.tryAcquire(time, unit);
    }

    protected void awaitUnroll(TAlert alert) throws InterruptedException {
        var sync = unrollExecutor.getSyncEval(alert.getId(), alert.getVersion());
        sync.drainPermits();
        clock.passedTime(1, TimeUnit.MINUTES);
        for (int retries = 0; retries < 2; retries++) {
            if (sync.tryAcquire(100, TimeUnit.MILLISECONDS)) {
                return;
            }
            clock.passedTime(1, TimeUnit.MINUTES);
        }
        sync.acquire();
    }

    protected void createMute(Mute dt) {
        muteService.createMute(CreateMuteRequest.newBuilder()
                .setMute(MuteConverter.INSTANCE.muteToProto(dt, MuteStatus.UNKNOWN))
                .build()
        ).join();
    }

    protected void updateMute(Mute dt) {
        muteService.updateMute(UpdateMuteRequest.newBuilder()
                .setMute(MuteConverter.INSTANCE.muteToProto(dt, MuteStatus.UNKNOWN))
                .build()
        ).join();
    }

    protected void deleteMute(String id) {
        muteService.deleteMute(DeleteMuteRequest.newBuilder()
                .setProjectId(projectId)
                .setId(id)
                .build()
        ).join();
    }

    protected void restartProjectAssignment() {
        if (service != null) {
            service.close();
            statesDao.save(projectId, clock.instant(), assignment.getSeqNo(), service.snapshot());
        }

        createBrokerService();
        service.run().join();
    }

    protected void createAndRunMuteService() {
        muteService = new ProjectMuteService(
                projectId,
                clock,
                new InMemoryMutesDao(),
                new MuteSearch(MuteConverter.INSTANCE),
                MuteConverter.INSTANCE
        );
        muteService.run().join();
    }

    protected void createBrokerService() {
        NotificationConverter notificationConverter = new NotificationConverter(new ChatIdResolverStub());
        ActivitySearch activitySearch = new ActivitySearch(new ActivityFilters(notificationConverter));
        service = new ProjectAlertService(
                projectId,
                clock,
                alertDao,
                statesDao,
                activityFactory,
                executorService,
                notificationConverter,
                activitySearch,
                quotaWatcher,
                new ProjectAlertServiceValidatorStub(),
                new AlertPostInitializerStub(),
                Executors.newSingleThreadExecutor(),
                new MetricRegistry(),
                new IdempotentOperationServiceImpl(idempotentDao),
                ForkJoinPool.commonPool());
    }

    protected List<TAlert> createManyAlerts() {
        return IntStream.range(1, 100)
                .parallel()
                .mapToObj(index -> TCreateAlertRequest.newBuilder()
                        .setAlert(randomAlert()
                                .toBuilder()
                                .setName("Alert #" + index)
                                .build())
                        .build())
                .map(request -> service.createAlert(request))
                .map(future -> future.thenApply(response -> {
                    assertThat(response.getStatusMessage(), response.getRequestStatus(), equalTo(ERequestStatusCode.OK));
                    return response.getAlert();
                }))
                .collect(collectingAndThen(toList(), CompletableFutures::joinAll));
    }

    protected List<TAlert> createManySimpleAlerts() {
        return IntStream.range(1, 100)
                .parallel()
                .mapToObj(index -> TCreateAlertRequest.newBuilder()
                        .setAlert(randomAlert()
                                .toBuilder()
                                .setName("Alert #" + index)
                                .clearGroupByLabels()
                                .build())
                        .build())
                .map(request -> service.createAlert(request))
                .map(future -> future.thenApply(response -> {
                    assertThat(response.getStatusMessage(), response.getRequestStatus(), equalTo(ERequestStatusCode.OK));
                    return response.getAlert();
                }))
                .collect(collectingAndThen(toList(), CompletableFutures::joinAll));
    }

    protected TAlert successCreate(TAlert source) {
        TCreateAlertResponse response = service.createAlert(TCreateAlertRequest.newBuilder()
                .setAlert(source)
                .build())
                .join();

        assertThat(response.getStatusMessage(), response.getRequestStatus(), equalTo(ERequestStatusCode.OK));
        return response.getAlert();
    }

    protected ListAlertLabelsResponse successListLabels(String projectId) {
        var response = service.listAlertLabels(ListAlertLabelsRequest.newBuilder()
                .setProjectId(projectId)
                .build())
                .join();

        assertThat(response.getRequestStatusCode(), equalTo(ERequestStatusCode.OK));
        return response;
    }

    protected TAlert successRead(String alertId) {
        TReadAlertResponse response = service.readAlert(TReadAlertRequest.newBuilder()
                .setAlertId(alertId)
                .setProjectId(projectId)
                .build())
                .join();

        assertThat(response.getStatusMessage(), response.getRequestStatus(), equalTo(ERequestStatusCode.OK));
        return response.getAlert();
    }

    protected TSubAlert successReadSubAlert(String parentId, String alertId) {
        TReadSubAlertResponse response = service.readSubAlert(TReadSubAlertRequest.newBuilder()
                .setAlertId(alertId)
                .setParentId(parentId)
                .setProjectId(projectId)
                .build())
                .join();

        assertThat(response.getStatusMessage(), response.getRequestStatus(), equalTo(ERequestStatusCode.OK));
        return response.getSubAlert();
    }

    protected TAlert successUpdate(TAlert alert) {
        TUpdateAlertResponse response = service.updateAlert(TUpdateAlertRequest.newBuilder()
                .setAlert(alert)
                .build())
                .join();

        assertThat(response.getStatusMessage(), response.getRequestStatus(), equalTo(ERequestStatusCode.OK));
        return response.getAlert();
    }

    protected TAlert successUpdateAlertTemplateVersion(TAlert alert, String newTag) {
        UpdateAlertTemplateVersionResponse response = service.updateAlertTemplateVersion(
                UpdateAlertTemplateVersionRequest.newBuilder()
                        .setTemplateId(alert.getAlertFromTemplate().getTemplateId())
                        .setServiceProvider("")
                        .setTemplateVersionTag(newTag)
                        .setAlertData(UpdateAlertTemplateVersionRequest.AlertData.newBuilder()
                                .setAlertId(alert.getId())
                                .build())
                        .build())
                .join();

        assertThat(response.getStatusMessage(), response.getRequestStatusCode(), equalTo(ERequestStatusCode.OK));
        return response.getAlert();
    }

    protected void successDelete(String alertId) {
        TDeleteAlertResponse response = service.deleteAlert(TDeleteAlertRequest.newBuilder()
                .setAlertId(alertId)
                .build())
                .join();

        assertThat(response.getStatusMessage(), response.getRequestStatus(), equalTo(ERequestStatusCode.OK));
    }

    protected TEvaluationState successReadEvalState(String alertId) {
        TReadEvaluationStateResponse response = service.readEvaluationState(TReadEvaluationStateRequest.newBuilder()
                .setAlertId(alertId)
                .setProjectId(projectId)
                .build())
                .join();

        assertThat(response.getStatusMessage(), response.getRequestStatus(), equalTo(ERequestStatusCode.OK));
        return response.getState();
    }

    protected TEvaluationState successReadEvalState(String parentId, String alertId) {
        TReadEvaluationStateResponse response = service.readEvaluationState(TReadEvaluationStateRequest.newBuilder()
                .setAlertId(alertId)
                .setParentId(parentId)
                .setProjectId(projectId)
                .build())
                .join();

        assertThat(response.getStatusMessage(), response.getRequestStatus(), equalTo(ERequestStatusCode.OK));
        return response.getState();
    }

    protected TEvaluationStats successReadEvalStats(String alertId) {
        TReadEvaluationStatsResponse response = service.readEvaluationStats(TReadEvaluationStatsRequest.newBuilder()
                .setAlertId(alertId)
                .setProjectId(projectId)
                .build())
                .join();

        assertThat(response.getStatusMessage(), response.getRequestStatus(), equalTo(ERequestStatusCode.OK));
        return response.getStats();
    }

    protected TEvaluationStats successReadMutedStats(String alertId) {
        TReadEvaluationStatsResponse response = service.readEvaluationStats(TReadEvaluationStatsRequest.newBuilder()
                        .setAlertId(alertId)
                        .setProjectId(projectId)
                        .build())
                .join();

        assertThat(response.getStatusMessage(), response.getRequestStatus(), equalTo(ERequestStatusCode.OK));
        return response.getMutedStats();
    }

    protected List<TNotificationState> successReadNotificationState(String alertId) {
        return successReadNotificationState(TReadNotificationStateRequest.newBuilder()
                .setAlertId(alertId)
                .setProjectId(projectId)
                .build());
    }

    protected List<TNotificationState> successReadNotificationState(String parentId, String alertId) {
        return successReadNotificationState(TReadNotificationStateRequest.newBuilder()
                .setAlertId(alertId)
                .setParentId(parentId)
                .setProjectId(projectId)
                .build());
    }

    protected List<TNotificationState> successReadNotificationState(TReadNotificationStateRequest request) {
        TReadNotificationStateResponse response = service.readNotificationState(request).join();
        assertThat(response.getStatusMessage(), response.getRequestStatus(), equalTo(ERequestStatusCode.OK));
        return response.getStatesList();
    }

    protected TNotificationStats successReadNotificationStats(String alertId) {
        TReadNotificationStatsResponse response = service.readNotificationStats(TReadNotificationStatsRequest.newBuilder()
                .setAlertId(alertId)
                .setProjectId(projectId)
                .build())
                .join();

        assertThat(response.getStatusMessage(), response.getRequestStatus(), equalTo(ERequestStatusCode.OK));
        return response.getStats();
    }

    protected TListAlert[] successList(TListAlertRequest request) {
        TListAlertResponse response = service.listAlerts(request).join();
        assertThat(response.getStatusMessage(), response.getRequestStatus(), equalTo(ERequestStatusCode.OK));
        assertThat(response.getListAlertList().getAlertsList(), equalTo(response.getAlertsList()));
        return response.getAlertsList()
                .stream()
                .map(proto -> proto.toBuilder()
                        .clearEvaluationStats()
                        .clearNotificationStats()
                        .clearMutedStats()
                        .clearStatusSinceTimestamp()
                        .build())
                .toArray(TListAlert[]::new);
    }

    protected TAlert[] successFullList(TListAlertRequest request) {
        TListAlertResponse response = service.listAlerts(request).join();
        assertThat(response.getStatusMessage(), response.getRequestStatus(), equalTo(ERequestStatusCode.OK));
        return response.getAlertList().getAlertsList().toArray(TAlert[]::new);
    }

    protected TListSubAlert[] successList(TListSubAlertRequest request) {
        TListSubAlertResponse response = service.listSubAlerts(request).join();
        assertThat(response.getStatusMessage(), response.getRequestStatus(), equalTo(ERequestStatusCode.OK));
        return response.getAlertsList()
                .stream()
                .map(proto -> proto.toBuilder()
                        .clearEvaluationStatusCode()
                        .build())
                .toArray(TListSubAlert[]::new);
    }

    protected TAlert randomExpressionAlert() {
        Alert alert = AlertTestSupport.randomExpressionAlert(ThreadLocalRandom.current())
                .toBuilder()
                .setState(AlertState.ACTIVE)
                .setProjectId(projectId)
                .build();

        return convert(alert);
    }

    protected TAlert randomThresholdAlert() {
        Alert alert = AlertTestSupport.randomThresholdAlert(ThreadLocalRandom.current())
                .toBuilder()
                .setState(AlertState.ACTIVE)
                .setProjectId(projectId)
                .build();

        return convert(alert);
    }

    /**
     * Group by labels are overridden from template, so setGroupByLabels does not work properly for template alerts
     */
    protected TAlert randomNonTemplateAlert() {
        Alert alert = AlertTestSupport.randomAlert(ThreadLocalRandom.current(), false)
                .toBuilder()
                .setState(AlertState.ACTIVE)
                .setProjectId(projectId)
                .build();

        return convert(alert);
    }

    protected TAlert randomAlert() {
        Alert alert = AlertTestSupport.randomActiveAlert()
                .toBuilder()
                .setProjectId(projectId)
                .build();

        return convert(alert);
    }

    protected TAlert randomAlertFromTemplate() {
        Alert alert = AlertTestSupport.alertFromTemplatePersistent(ThreadLocalRandom.current())
                .toBuilder()
                .setProjectId(projectId)
                .build();

        return convert(alert);
    }

    protected TAlert convert(Alert alert) {
        return AlertConverter.alertToProto(alert)
                .toBuilder()
                .clearCreatedAt()
                .clearUpdatedAt()
                .clearVersion()
                .build();
    }

    protected TEvaluationStatus randomEvaluationStatus() {
        return AlertConverter.statusToProto(AlertEvalStateTestSupport.randomEvalStatus());
    }

    protected NotificationChannelStub randomNotification() {
        return new NotificationChannelStub(NotificationTestSupport.randomNotification()
                .toBuilder()
                .setProjectId(projectId)
                .setNotifyAboutStatus(EvaluationStatus.Code.OK, EvaluationStatus.Code.values())
                .setRepeatNotifyDelay(Duration.ofNanos(1))
                .build());
    }

    protected void predefineAlertStatus(String subAlertId, EvaluationStatus status) throws InterruptedException {
        assignmentService.predefineStatus(subAlertId, () -> status.withDescription(""));
        awaitEval(subAlertId);
    }

}
