package ru.yandex.solomon.alert.cluster.broker.notification;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.jns.dto.JnsListEscalationPolicy;
import ru.yandex.solomon.alert.EvaluationStatus;
import ru.yandex.solomon.alert.api.converters.NotificationConverter;
import ru.yandex.solomon.alert.dao.memory.InMemoryNotificationsDao;
import ru.yandex.solomon.alert.domain.Alert;
import ru.yandex.solomon.alert.domain.ChannelConfig;
import ru.yandex.solomon.alert.notification.NotificationState;
import ru.yandex.solomon.alert.notification.channel.NotificationChannelStubFactory;
import ru.yandex.solomon.alert.notification.channel.NotificationStatus;
import ru.yandex.solomon.alert.notification.channel.telegram.ChatIdResolverStub;
import ru.yandex.solomon.alert.notification.state.StatefulNotificationChannel;
import ru.yandex.solomon.alert.protobuf.EOrderDirection;
import ru.yandex.solomon.alert.protobuf.ERequestStatusCode;
import ru.yandex.solomon.alert.protobuf.EscalationView;
import ru.yandex.solomon.alert.protobuf.Severity;
import ru.yandex.solomon.alert.protobuf.TCreateNotificationRequest;
import ru.yandex.solomon.alert.protobuf.TCreateNotificationResponse;
import ru.yandex.solomon.alert.protobuf.TDeleteNotificationRequest;
import ru.yandex.solomon.alert.protobuf.TDeleteNotificationResponse;
import ru.yandex.solomon.alert.protobuf.TEvaluationStatus;
import ru.yandex.solomon.alert.protobuf.TListEscalationsRequest;
import ru.yandex.solomon.alert.protobuf.TListNotificationsRequest;
import ru.yandex.solomon.alert.protobuf.TListNotificationsResponse;
import ru.yandex.solomon.alert.protobuf.TReadNotificationRequest;
import ru.yandex.solomon.alert.protobuf.TReadNotificationResponse;
import ru.yandex.solomon.alert.protobuf.TResolveNotificationDetailsRequest;
import ru.yandex.solomon.alert.protobuf.TUpdateNotificationRequest;
import ru.yandex.solomon.alert.protobuf.TUpdateNotificationResponse;
import ru.yandex.solomon.alert.protobuf.notification.ENotificationChannelType;
import ru.yandex.solomon.alert.protobuf.notification.TNotification;
import ru.yandex.solomon.alert.protobuf.notification.TNotificationDetails;
import ru.yandex.solomon.flags.FeatureFlagHolderStub;
import ru.yandex.solomon.ut.ManualClock;
import ru.yandex.solomon.ut.ManualScheduledExecutorService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * @author Vladimir Gordiychuk
 */
public class ProjectNotificationServiceTest extends ProjectNotificationServiceTestBase {
    @Before
    public void setUp() throws Exception {
        this.projectId = "junk" + ThreadLocalRandom.current().nextInt(0, 100);
        this.clock = new ManualClock();
        this.notificationsDao = new InMemoryNotificationsDao();
        this.flagsHolder = new FeatureFlagHolderStub();
        this.executorService = new ManualScheduledExecutorService(2, clock);
        this.channelFactory = new NotificationChannelStubFactory();
        this.notificationConverter = new NotificationConverter(new ChatIdResolverStub());
        restartService();
    }

    @After
    public void tearDown() throws Exception {
        if (executorService != null) {
            executorService.shutdownNow();
        }
    }

    @Test
    public void create() {
        TNotification source = randomNotification();
        TNotification result = successCreate(source)
                .toBuilder()
                .clearVersion()
                .clearUpdatedAt()
                .clearCreatedAt()
                .build();

        assertThat(result, equalTo(source));
    }

    @Test
    public void createAndRead() {
        TNotification create = successCreate(randomNotification());
        TNotification read = successRead(create.getId());
        assertThat(read, equalTo(create));
    }

    @Test
    public void notAbleCreateAlertWithSameIdTwice() {
        TNotification v1 = successCreate(randomNotification());
        TCreateNotificationResponse response = service.createNotification(TCreateNotificationRequest
                .newBuilder()
                .setNotification(randomNotification()
                        .toBuilder()
                        .setId(v1.getId())
                        .build())
                .build())
                .join();

        assertThat(response.getRequestStatus(), equalTo(ERequestStatusCode.INVALID_REQUEST));
    }

    @Test
    public void update() {
        TNotification v1 = successCreate(randomNotification().toBuilder().clearDefaultForAlertSeverity().build());
        TNotification v2 = successUpdate(v1.toBuilder()
                .setName("More pretty name!")
                .build());

        assertThat(v2, not(equalTo(v1)));
        assertThat(v2.getName(), equalTo("More pretty name!"));

        TNotification v3 = successUpdate(v2.toBuilder()
                .setName("Prev name was not so good")
                .build());
        assertThat(v3, not(equalTo(v2)));
        assertThat(v3.getName(), equalTo("Prev name was not so good"));
    }

    @Test
    public void updateWithoutVersion() {
        TNotification v1 = successCreate(randomNotification().toBuilder().clearDefaultForAlertSeverity().build());

        for (int version = 1; version < 10; ++version) {
            successUpdate(v1.toBuilder()
                    .setName("Channel (updated #" + version + ")")
                    .setVersion(version)
                    .build());
        }

        TNotification v11 = successUpdate(v1.toBuilder()
                .setName("Notification (updated #11)")
                .setVersion(-1)
                .build());

        assertThat(v11.getName(), equalTo("Notification (updated #11)"));
        assertThat(v11.getVersion(), equalTo(11));
    }

    @Test
    public void saveCreatedFieldsImmutable() {
        Instant now = clock.instant().truncatedTo(ChronoUnit.SECONDS);
        String createdBy = "bob";

        TNotification v1 = successCreate(randomNotification()
                .toBuilder()
                .clearDefaultForAlertSeverity()
                .setCreatedAt(now.toEpochMilli())
                .setCreatedBy(createdBy)
                .build());

        assertThat(v1.getCreatedBy(), equalTo(createdBy));
        assertThat(v1.getCreatedAt(), greaterThanOrEqualTo(now.toEpochMilli()));

        TNotification v2 = successUpdate(v1.toBuilder()
                .setName("More pretty name!")
                .setCreatedAt(now.toEpochMilli() + TimeUnit.SECONDS.toMillis(100))
                .setCreatedBy("alice")
                .build());

        assertThat(v2.getName(), equalTo("More pretty name!"));
        assertThat(v2.getCreatedBy(), equalTo(createdBy));
        assertThat(v2.getCreatedAt(), equalTo(v1.getCreatedAt()));
    }

    @Test
    public void readUpdated() {
        TNotification v1 = successCreate(randomNotification().toBuilder().clearDefaultForAlertSeverity().build());
        TNotification v2 = successUpdate(v1.toBuilder()
                .setName("changed name")
                .build());

        TNotification read = successRead(v2.getId());
        assertThat(read, equalTo(v2));
    }

    @Test
    public void optimisticLockViaVersion() {
        TNotification v1 = successCreate(randomNotification().toBuilder().clearDefaultForAlertSeverity().build());
        TNotification v2 = successUpdate(v1.toBuilder()
                .setName("changed name")
                .build());

        TUpdateNotificationResponse response = service.updateNotification(TUpdateNotificationRequest.newBuilder()
                .setNotification(v1.toBuilder()
                        .setName("Conflict name")
                        .build())
                .build())
                .join();

        assertThat(response.getRequestStatus(), equalTo(ERequestStatusCode.CONCURRENT_MODIFICATION));

        TNotification read = successRead(v2.getId());
        assertThat(read, equalTo(v2));
    }

    @Test
    public void notAbleUpdateNotExists() {
        TUpdateNotificationResponse response = service.updateNotification(TUpdateNotificationRequest.newBuilder()
                .setNotification(randomNotification())
                .build())
                .join();

        assertThat(response.getRequestStatus(), equalTo(ERequestStatusCode.NOT_FOUND));
    }

    @Test
    public void notAbleDeleteNotExistAlert() {
        TDeleteNotificationResponse response = service.deleteNotification(TDeleteNotificationRequest.newBuilder()
                .setNotificationId("notExistsId")
                .build())
                .join();

        assertThat(response.getRequestStatus(), equalTo(ERequestStatusCode.NOT_FOUND));
    }

    @Test
    public void notAbleReadNotExistAlert() {
        TReadNotificationResponse response = service.readNotification(TReadNotificationRequest.newBuilder()
                .setNotificationId("notExistsId")
                .build())
                .join();

        assertThat(response.getRequestStatus(), equalTo(ERequestStatusCode.NOT_FOUND));
    }

    @Test
    public void updateIncrementUpdateTime() {
        TNotification v1 = successCreate(randomNotification().toBuilder().clearDefaultForAlertSeverity().build());

        clock.passedTime(1, TimeUnit.MINUTES);
        TNotification v2 = successUpdate(v1.toBuilder()
                .setName("v2")
                .build());

        assertThat(v2.getUpdatedAt(), greaterThan(v1.getUpdatedAt()));

        clock.passedTime(5, TimeUnit.MINUTES);
        TNotification v3 = successUpdate(v2.toBuilder()
                .setName("v3")
                .build());

        assertThat(v3.getUpdatedAt(), greaterThan(v2.getUpdatedAt()));
    }

    @Test
    public void delete() {
        TNotification created = successCreate(randomNotification().toBuilder().clearDefaultForAlertSeverity().build());
        successDelete(created.getId());

        TReadNotificationResponse response = service.readNotification(TReadNotificationRequest.newBuilder()
                .setNotificationId(created.getId())
                .build())
                .join();

        assertThat(response.getRequestStatus(), equalTo(ERequestStatusCode.NOT_FOUND));
    }

    @Test
    public void createPersisted() {
        TNotification create = successCreate(randomNotification());
        restartService();

        TNotification read = successRead(create.getId());
        assertThat(read, equalTo(create));
    }

    @Test
    public void updatePersisted() {
        TNotification create = successCreate(randomNotification().toBuilder().clearDefaultForAlertSeverity().build());
        TNotification updated = successUpdate(create.toBuilder()
                .setName("changed")
                .build());
        restartService();

        TNotification read = successRead(create.getId());
        assertThat(read, equalTo(updated));
    }

    @Test
    public void updateSameSeverity() {
        TNotification create = successCreate(randomNotification().toBuilder().addAllDefaultForAlertSeverity(Set.of(Severity.SEVERITY_CRITICAL)).build());
        TNotification updated = successUpdate(create.toBuilder()
                .setName("changed")
                .build());
        restartService();

        TNotification read = successRead(create.getId());
        assertThat(read, equalTo(updated));
    }

    @Test
    public void updatePersisted_multipleSeverity() {
        TNotification created = successCreate(randomNotification().toBuilder()
                .setProjectId(projectId)
                .clearDefaultForAlertSeverity().addAllDefaultForAlertSeverity(Set.of(Severity.SEVERITY_DISASTER)).build());
        TNotification created2 = successCreate(randomNotification().toBuilder()
                .setProjectId(projectId)
                .clearDefaultForAlertSeverity().addAllDefaultForAlertSeverity(Set.of(Severity.SEVERITY_DISASTER)).build());
        var r = service.updateNotification(TUpdateNotificationRequest.newBuilder()
                .setNotification(created.toBuilder().clearDefaultForAlertSeverity().build())
                .build())
                .join();

        assertThat(r.getStatusMessage(), r.getRequestStatus(), equalTo(ERequestStatusCode.OK));
        var response = service.updateNotification(TUpdateNotificationRequest.newBuilder()
                .setNotification(created2.toBuilder().clearDefaultForAlertSeverity().build())
                .build())
                .join();

        assertThat(response.getStatusMessage(), response.getRequestStatus(), equalTo(ERequestStatusCode.INVALID_REQUEST));
    }

    @Test
    public void deletePersisted() {
        TNotification created = successCreate(randomNotification().toBuilder().clearDefaultForAlertSeverity().build());
        successDelete(created.getId());
        restartService();

        TReadNotificationResponse response = service.readNotification(TReadNotificationRequest.newBuilder()
                .setProjectId(projectId)
                .setNotificationId(created.getId())
                .build())
                .join();

        assertThat(response.getRequestStatus(), equalTo(ERequestStatusCode.NOT_FOUND));
    }

    @Test
    public void deletePersisted_multipleSeverity() {
        TNotification created = successCreate(randomNotification().toBuilder()
                .setProjectId(projectId)
                .clearDefaultForAlertSeverity().addAllDefaultForAlertSeverity(Set.of(Severity.SEVERITY_DISASTER)).build());
        TNotification created2 = successCreate(randomNotification().toBuilder()
                .setProjectId(projectId)
                .clearDefaultForAlertSeverity().addAllDefaultForAlertSeverity(Set.of(Severity.SEVERITY_DISASTER)).build());
        var r = service.deleteNotification(TDeleteNotificationRequest.newBuilder()
                .setNotificationId(created.getId())
                .setProjectId(created.getId())
                .build())
                .join();
        assertThat(r.getStatusMessage(), r.getRequestStatus(), equalTo(ERequestStatusCode.OK));
        TDeleteNotificationResponse response = service.deleteNotification(TDeleteNotificationRequest.newBuilder()
                .setNotificationId(created2.getId())
                .setProjectId(created2.getId())
                .build())
                .join();

        assertThat(response.getStatusMessage(), response.getRequestStatus(), equalTo(ERequestStatusCode.INVALID_REQUEST));
    }

    @Test
    public void createNotAbleWhenServiceNotReady() {
        service.close();
        createService();
        TCreateNotificationResponse response = service.createNotification(TCreateNotificationRequest.newBuilder()
                .setNotification(randomNotification())
                .build())
                .join();

        assertThat(response.getRequestStatus(), equalTo(ERequestStatusCode.SHARD_NOT_INITIALIZED));
    }

    @Test
    public void updateNotAbleWhenServiceNotReady() {
        service.close();
        createService();
        TUpdateNotificationResponse response = service.updateNotification(TUpdateNotificationRequest.newBuilder()
                .setNotification(randomNotification())
                .build())
                .join();

        assertThat(response.getRequestStatus(), equalTo(ERequestStatusCode.SHARD_NOT_INITIALIZED));
    }

    @Test
    public void readNotAbleWhenServiceNotReady() {
        service.close();
        createService();
        TReadNotificationResponse response = service.readNotification(TReadNotificationRequest.newBuilder()
                .setProjectId(projectId)
                .setNotificationId("myId")
                .build())
                .join();

        assertThat(response.getRequestStatus(), equalTo(ERequestStatusCode.SHARD_NOT_INITIALIZED));
    }

    @Test
    public void listNotAbleWhenServiceNotReady() {
        service.close();
        createService();
        TListNotificationsResponse response = service.listNotification(TListNotificationsRequest.newBuilder()
                .setProjectId(projectId)
                .setPageSize(10)
                .build())
                .join();

        assertThat(response.getRequestStatus(), equalTo(ERequestStatusCode.SHARD_NOT_INITIALIZED));
    }

    @Test
    public void deleteNotAbleWhenServiceNotReady() {
        service.close();
        createService();
        TDeleteNotificationResponse response = service.deleteNotification(TDeleteNotificationRequest.newBuilder()
                .setProjectId(projectId)
                .setNotificationId("myId")
                .build())
                .join();

        assertThat(response.getRequestStatus(), equalTo(ERequestStatusCode.SHARD_NOT_INITIALIZED));
    }

    @Test
    public void prepareEmptyChannel() {
        Alert alert = randomAlert();
        Map<String, StatefulNotificationChannel> result = service.prepareChannels(alert);
        assertThat(result, equalTo(Collections.emptyMap()));
    }

    @Test
    public void prepareDevNullChannel() {
        final String channelId = "notExistChannelId";
        Alert alert = randomAlert()
                .toBuilder()
                .setNotificationChannel(channelId)
                .build();

        StatefulNotificationChannel channel = prepareChannel(alert);
        NotificationStatus status = channel.send(okToAlarmState(alert)).join();
        assertThat(status.getCode(), equalTo(NotificationStatus.Code.ABSENT_NOTIFICATION_CHANNEL));
    }

    @Test
    public void prepareExistChannel() {
        TNotification created = successCreate(randomNotification()
                .toBuilder()
                .addNotifyAboutStatuses(TEvaluationStatus.ECode.ALARM)
                .build());

        Alert alert = randomAlert()
                .toBuilder()
                .setNotificationChannel(created.getId())
                .build();

        StatefulNotificationChannel channel = prepareChannel(alert);
        NotificationStatus status = channel.send(okToAlarmState(alert)).join();
        assertThat(status.getCode(), equalTo(NotificationStatus.Code.SUCCESS));
    }

    @Test
    public void updatePreparedChannel() {
        TNotification create = successCreate(randomNotification()
                .toBuilder()
                .clearNotifyAboutStatuses()
                .clearDefaultForAlertSeverity()
                .addNotifyAboutStatuses(TEvaluationStatus.ECode.OK)
                .addNotifyAboutStatuses(TEvaluationStatus.ECode.ALARM)
                .build());

        Alert alert = randomAlert()
                .toBuilder()
                .setNotificationChannel(create.getId())
                .build();

        StatefulNotificationChannel channel = prepareChannel(alert);
        var alarm = okToAlarmState(alert);
        assertThat(channel.send(alarm).join().getCode(), equalTo(NotificationStatus.Code.SUCCESS));
        var ok = nextEval(alarm, EvaluationStatus.OK);
        assertThat(channel.send(ok).join().getCode(), equalTo(NotificationStatus.Code.SUCCESS));

        TNotification updated = successUpdate(create
                .toBuilder()
                .clearNotifyAboutStatuses()
                .addNotifyAboutStatuses(TEvaluationStatus.ECode.ALARM)
                .build());

        var backToAlarm = nextEval(ok, EvaluationStatus.ALARM);
        assertThat(channel.send(backToAlarm).join().getCode(), equalTo(NotificationStatus.Code.SUCCESS));
        var backToOk = nextEval(backToAlarm, EvaluationStatus.OK);
        assertThat(channel.send(backToOk).join().getCode(), equalTo(NotificationStatus.Code.SKIP_BY_STATUS));
    }

    @Test
    public void deletePreparedChannel() {
        TNotification created = successCreate(randomNotification()
                .toBuilder()
                .clearNotifyAboutStatuses()
                .clearDefaultForAlertSeverity()
                .addNotifyAboutStatuses(TEvaluationStatus.ECode.OK)
                .addNotifyAboutStatuses(TEvaluationStatus.ECode.ALARM)
                .build());

        Alert alert = randomAlert()
                .toBuilder()
                .setNotificationChannel(created.getId())
                .build();

        StatefulNotificationChannel channel = prepareChannel(alert);
        var alarm = okToAlarmState(alert);
        assertThat(channel.send(alarm).join().getCode(), equalTo(NotificationStatus.Code.SUCCESS));

        successDelete(created.getId());
        var ok = nextEval(alarm, EvaluationStatus.OK);
        assertThat(channel.send(ok).join().getCode(), equalTo(NotificationStatus.Code.ABSENT_NOTIFICATION_CHANNEL));
    }

    @Test
    public void prepareForeignChannel() {
        TNotification created = successCreate(randomNotification()
            .toBuilder()
            .setFolderId("foreign")
            .setDefaultForProject(false)
            .clearNotifyAboutStatuses()
            .addNotifyAboutStatuses(TEvaluationStatus.ECode.OK)
            .addNotifyAboutStatuses(TEvaluationStatus.ECode.ALARM)
            .build());

        Alert alert = randomAlert()
            .toBuilder()
            .setNotificationChannel(created.getId())
            .build();

        StatefulNotificationChannel channel = prepareChannel(alert);
        var alarm = okToAlarmState(alert);
        assertThat(channel.send(alarm).join().getCode(), equalTo(NotificationStatus.Code.ABSENT_NOTIFICATION_CHANNEL));
    }

    @Test
    public void createChannelAfterPrepareIt() {
        final String channelId = "myChannelId";

        Alert alert = randomAlert()
                .toBuilder()
                .setNotificationChannel(channelId)
                .build();

        StatefulNotificationChannel channel = prepareChannel(alert);
        var alarm = okToAlarmState(alert);
        assertThat(channel.send(alarm).join().getCode(), equalTo(NotificationStatus.Code.ABSENT_NOTIFICATION_CHANNEL));

        TNotification created = successCreate(randomNotification()
                .toBuilder()
                .setId(channelId)
                .clearNotifyAboutStatuses()
                .addNotifyAboutStatuses(TEvaluationStatus.ECode.ALARM)
                .build());

        var stillAlarm = nextEval(alarm, EvaluationStatus.ALARM);
        assertThat(channel.send(stillAlarm).join().getCode(), equalTo(NotificationStatus.Code.SUCCESS));
    }

    @Test
    public void prepareChannelPerAlert() {
        TNotification created = successCreate(randomNotification()
                .toBuilder()
                .clearNotifyAboutStatuses()
                .addNotifyAboutStatuses(TEvaluationStatus.ECode.ALARM)
                .addNotifyAboutStatuses(TEvaluationStatus.ECode.OK)
                .build());

        Alert alice = randomAlert()
                .toBuilder()
                .setId("alice")
                .setNotificationChannel(created.getId())
                .build();

        Alert bob = randomAlert()
                .toBuilder()
                .setId("bob")
                .setNotificationChannel(created.getId())
                .build();

        StatefulNotificationChannel aliceChannel = prepareChannel(alice);
        StatefulNotificationChannel bobChannel = prepareChannel(bob);

        assertThat(aliceChannel.send(okToAlarmState(alice)).join().getCode(), equalTo(NotificationStatus.Code.SUCCESS));
        clock.passedTime(30, TimeUnit.SECONDS);
        assertThat(bobChannel.send(okToAlarmState(bob)).join().getCode(), equalTo(NotificationStatus.Code.SUCCESS));

        NotificationState aliceState = aliceChannel.getLatestNotificationState();
        NotificationState bobState = bobChannel.getLatestNotificationState();

        assertThat(bobState.getLatestStatus(), equalTo(aliceState.getLatestStatus()));
        assertThat(bobState.getLatestEval().toEpochMilli(), greaterThan(aliceState.getLatestEval().toEpochMilli()));
        assertThat(bobState.getLatestSuccessNotify().toEpochMilli(), greaterThan(aliceState.getLatestSuccessNotify().toEpochMilli()));
    }

    @Test
    public void overrideNotifyStatusesPerAlert() {
        TNotification created = successCreate(randomNotification()
            .toBuilder()
            .clearNotifyAboutStatuses()
            .addNotifyAboutStatuses(TEvaluationStatus.ECode.ALARM)
            .addNotifyAboutStatuses(TEvaluationStatus.ECode.OK)
            .build());

        ChannelConfig aliceConfig = new ChannelConfig(Set.of(EvaluationStatus.Code.OK), Duration.ofMinutes(5));
        ChannelConfig bobConfig = new ChannelConfig(Set.of(EvaluationStatus.Code.ALARM), Duration.ofMinutes(15));

        Alert alice = randomAlert()
            .toBuilder()
            .setId("alice")
            .setNotificationChannels(Map.of(created.getId(), aliceConfig))
            .setEscalation(UUID.randomUUID().toString())
            .build();

        Alert bob = randomAlert()
            .toBuilder()
            .setId("bob")
            .setNotificationChannels(Map.of(created.getId(), bobConfig))
            .setEscalation(UUID.randomUUID().toString())
            .build();

        StatefulNotificationChannel aliceChannel = prepareChannel(alice);
        StatefulNotificationChannel bobChannel = prepareChannel(bob);

        Instant firstEval = clock.instant();

        assertThat(aliceChannel.send(okToAlarmState(alice)).join().getCode(), equalTo(NotificationStatus.Code.SKIP_BY_STATUS));
        assertThat(bobChannel.send(okToAlarmState(bob)).join().getCode(), equalTo(NotificationStatus.Code.SUCCESS));

        clock.passedTime(10, TimeUnit.MINUTES);

        assertThat(aliceChannel.send(okToAlarmState(alice, firstEval)).join().getCode(), equalTo(NotificationStatus.Code.SKIP_BY_STATUS));
        assertThat(bobChannel.send(okToAlarmState(bob, firstEval)).join().getCode(), equalTo(NotificationStatus.Code.SKIP_REPEAT));
    }

    @Test
    public void preparedChannelHaveResetState() {
        TNotification created = successCreate(randomNotification()
                .toBuilder()
                .clearNotifyAboutStatuses()
                .addNotifyAboutStatuses(TEvaluationStatus.ECode.OK)
                .addNotifyAboutStatuses(TEvaluationStatus.ECode.ALARM)
                .build());

        Alert alert = randomAlert()
                .toBuilder()
                .setNotificationChannel(created.getId())
                .build();

        StatefulNotificationChannel channel = prepareChannel(alert);
        NotificationState state = channel.getLatestNotificationState();
        assertThat(state.getLatestSuccessNotify(), equalTo(Instant.EPOCH));
        assertThat(state.getLatestEval(), equalTo(Instant.EPOCH));
        assertThat(state.getLatestStatus().getCode(), equalTo(NotificationStatus.Code.OBSOLETE));
    }

    @Test
    public void chanelForAlertWithDiffVersionHaveDiffState() {
        TNotification created = successCreate(randomNotification()
                .toBuilder()
                .clearNotifyAboutStatuses()
                .addNotifyAboutStatuses(TEvaluationStatus.ECode.OK)
                .addNotifyAboutStatuses(TEvaluationStatus.ECode.ALARM)
                .build());

        Alert v1 = randomAlert()
                .toBuilder()
                .setNotificationChannel(created.getId())
                .build();

        StatefulNotificationChannel channelV1 = prepareChannel(v1);
        assertThat(channelV1.send(okToAlarmState(v1)).join().getCode(), equalTo(NotificationStatus.Code.SUCCESS));

        NotificationState v1State = channelV1.getLatestNotificationState();
        Alert v2 = v1.toBuilder()
                .setName("Changed alert")
                .setVersion(v1.getVersion() + 1)
                .build();

        StatefulNotificationChannel channelV2 = prepareChannel(v2);
        NotificationState v2State = channelV2.getLatestNotificationState();

        assertThat(v2State.getLatestSuccessNotify(), not(equalTo(v1State.getLatestSuccessNotify())));
        assertThat(v2State.getLatestEval(), not(equalTo(v1State.getLatestEval())));
        assertThat(v2State.getLatestStatus(), not(equalTo(v1State.getLatestStatus())));
    }

    @Test
    public void listNameSortAsc() {
        List<TNotification> source = createManyNotifications();

        TNotification[] expected = source.stream()
                .sorted(Comparator.comparing(TNotification::getName))
                .toArray(TNotification[]::new);

        checkPagedResult(expected, TListNotificationsRequest.newBuilder()
                .setProjectId(projectId)
                .setOrderByName(EOrderDirection.ASC)
                .build());
    }

    @Test
    public void listNameSortDesc() {
        List<TNotification> source = createManyNotifications();

        TNotification[] expected = source.stream()
                .sorted(Comparator.comparing(TNotification::getName).reversed())
                .toArray(TNotification[]::new);

        checkPagedResult(expected, TListNotificationsRequest.newBuilder()
                .setProjectId(projectId)
                .setOrderByName(EOrderDirection.DESC)
                .build());
    }

    @Test
    public void listByName() {
        List<TNotification> source = createManyNotifications();

        TNotification[] expected = source.stream()
                .filter(entry -> entry.getName().contains("42"))
                .sorted(Comparator.comparing(TNotification::getName))
                .toArray(TNotification[]::new);

        checkPagedResult(expected, TListNotificationsRequest.newBuilder()
                .setProjectId(projectId)
                .setFilterByName("42")
                .build());
    }

    @Test
    public void listByType() {
        List<TNotification> source = createManyNotifications();

        TNotification[] expected = source.stream()
                .filter(entry -> entry.getTypeCase() == TNotification.TypeCase.EMAIL)
                .sorted(Comparator.comparing(TNotification::getName))
                .toArray(TNotification[]::new);

        checkPagedResult(expected, TListNotificationsRequest.newBuilder()
                .setProjectId(projectId)
                .addFilterByType(ENotificationChannelType.EMAIL)
                .build());
    }

    @Test
    public void listBySeverity() {
        List<TNotification> source = createManyNotifications();

        TNotification[] expected = source.stream()
                .filter(entry -> entry.getDefaultForAlertSeverityList().contains(Severity.SEVERITY_DISASTER) || entry.getDefaultForAlertSeverityList().contains(Severity.SEVERITY_CRITICAL))
                .sorted(Comparator.comparing(TNotification::getName))
                .toArray(TNotification[]::new);

        checkPagedResult(expected, TListNotificationsRequest.newBuilder()
                .setProjectId(projectId)
                .addFilterByDefaultSeverity(Severity.SEVERITY_CRITICAL)
                .addFilterByDefaultSeverity(Severity.SEVERITY_DISASTER)
                .build());
    }

    @Test
    public void listByDefaultSortByName() {
        List<TNotification> source = createManyNotifications();

        TNotification[] expected = source.stream()
                .sorted(Comparator.comparing(TNotification::getName))
                .toArray(TNotification[]::new);

        checkPagedResult(expected, TListNotificationsRequest.newBuilder()
                .setProjectId(projectId)
                .build());
    }

    @Test
    public void listByDefaultLimited() {
        List<TNotification> source = createManyNotifications();

        TNotification[] result = successList(TListNotificationsRequest.newBuilder()
                .setProjectId(projectId)
                .build());

        assertThat(result.length, allOf(greaterThan(0), lessThanOrEqualTo(source.size())));
    }

    @Test
    public void listNextPageToken() {
        List<TNotification> source = createManyNotifications();

        {
            var response = service.listNotification(TListNotificationsRequest.newBuilder()
                .setProjectId(projectId)
                .setPageSize(source.size())
                .build())
                .join();

            assertEquals(ERequestStatusCode.OK, response.getRequestStatus());
            assertEquals(source.size(), response.getNotificationCount());
            assertEquals("", response.getNextPageToken());
        }
        {
            var response = service.listNotification(TListNotificationsRequest.newBuilder()
                .setProjectId(projectId)
                .setPageSize(5)
                .build())
                .join();

            assertEquals(ERequestStatusCode.OK, response.getRequestStatus());
            assertEquals(5, response.getNotificationCount());
            assertNotEquals("", response.getNextPageToken());
        }
    }

    @Test
    public void resolveByNotificationIds() {
        List<TNotification> source = createManyNotifications();

        var channels = List.of(7, 42, 17).stream()
            .map(source::get)
            .collect(Collectors.toList());

        var channelIds = channels.stream()
            .map(TNotification::getId)
            .collect(Collectors.toList());

        TNotificationDetails[] result = successListDetails(TResolveNotificationDetailsRequest.newBuilder()
            .setProjectId(projectId)
            .addAllNotificationIds(channelIds)
            .build());

        Map<String, TNotificationDetails> detailsById = Arrays.stream(result)
            .collect(Collectors.toMap(TNotificationDetails::getNotificationId, Function.identity()));

        assertThat(detailsById.keySet(), iterableWithSize(3));
        for (int i = 0; i < 3; i++) {
            TNotificationDetails details = detailsById.get(channels.get(i).getId());
            assertThat(details.getName(), equalTo(channels.get(i).getName()));
            assertThat(details.getNotificationId(), equalTo(channels.get(i).getId()));
            assertThat(details.getProjectId(), equalTo(projectId));
            assertThat(details.getFolderId(), equalTo(channels.get(i).getFolderId()));
            assertThat(details.getType(), equalTo(typeToProto(channels.get(i).getTypeCase())));
            assertThat(details.getRecipientsList(), iterableWithSize(greaterThan(0)));
        }
    }

    @Test
    public void resolveByNotificationIdsAndFolder() {
        List<TNotification> source = createManyNotifications();

        var channels = List.of(7, 42, 17).stream()
            .map(source::get)
            .collect(Collectors.toList());

        var channelIds = channels.stream()
            .map(TNotification::getId)
            .collect(Collectors.toList());

        TNotificationDetails[] result = successListDetails(TResolveNotificationDetailsRequest.newBuilder()
            .setProjectId(projectId)
            .setFolderId("myfolder")
            .addAllNotificationIds(channelIds)
            .build());

        assertThat(result.length, equalTo(3));

        TNotificationDetails[] result2 = successListDetails(TResolveNotificationDetailsRequest.newBuilder()
            .setProjectId(projectId)
            .setFolderId("otherfolder")
            .addAllNotificationIds(channelIds)
            .build());

        assertThat(result2.length, equalTo(0));
    }

    private ENotificationChannelType typeToProto(TNotification.TypeCase typeCase) {
        return switch (typeCase) {
            case SMS -> ENotificationChannelType.SMS;
            case EMAIL -> ENotificationChannelType.EMAIL;
            case JUGGLER -> ENotificationChannelType.JUGGLER;
            case TELEGRAM -> ENotificationChannelType.TELEGRAM;
            case WEBKOOK -> ENotificationChannelType.WEBHOOK;
            case CLOUDEMAIL -> ENotificationChannelType.CLOUD_EMAIL;
            case CLOUDSMS -> ENotificationChannelType.CLOUD_SMS;
            case YACHATS -> ENotificationChannelType.YA_CHATS;
            case DATALENSEMAIL -> ENotificationChannelType.DATALENS_EMAIL;
            case CLOUDPUSH -> ENotificationChannelType.CLOUD_PUSH;
            case PHONE -> ENotificationChannelType.PHONE_CALL;
            default -> ENotificationChannelType.UNRECOGNIZED;
        };
    }

    @Test
    public void canceledNotAbleRunAgain() {
        service.run().join();
        service.close();
        // repeat run can be evaluated during retry init
        service.run().join();
        TCreateNotificationResponse response = service.createNotification(TCreateNotificationRequest.newBuilder()
                .setNotification(randomNotification())
                .build())
                .join();

        assertThat(response.getRequestStatus(), equalTo(ERequestStatusCode.SHARD_NOT_INITIALIZED));
    }

    @Test
    public void create_default() {
        TNotification source = randomNotification().toBuilder()
                .setDefaultForProject(true)
                .build();
        TNotification source2 = randomNotification().toBuilder()
                .setDefaultForProject(true)
                .setProjectId(source.getProjectId())
                .build();
        successCreate(source);
        successCreate(source2);

        var defaults = service.getDefaultChannelsHolder();
        var channel = service.getNotificationChannelById(source.getId());
        var channel2 = service.getNotificationChannelById(source2.getId());

        assertEquals(2, defaults.getDefaultChannelsMap().size());
        assertEquals(channel, defaults.getDefaultChannelsMap().get(source.getId()));
        assertEquals(channel2, defaults.getDefaultChannelsMap().get(source2.getId()));
    }

    @Test
    public void update_default() {
        TNotification source = randomNotification().toBuilder()
                .setDefaultForProject(true)
                .clearDefaultForAlertSeverity()
                .build();
        TNotification source2 = randomNotification().toBuilder()
                .setDefaultForProject(true)
                .setProjectId(source.getProjectId())
                .clearDefaultForAlertSeverity()
                .build();
        TNotification source3 = randomNotification().toBuilder()
                .setDefaultForProject(false)
                .setProjectId(source.getProjectId())
                .clearDefaultForAlertSeverity()
                .build();
        successCreate(source);
        successCreate(source2);
        successCreate(source3);

        var channel = service.getNotificationChannelById(source.getId());
        var channel2 = service.getNotificationChannelById(source2.getId());
        var defaults = service.getDefaultChannelsHolder();
        assertEquals(2, defaults.getDefaultChannelsMap().size());
        assertEquals(channel, defaults.getDefaultChannelsMap().get(source.getId()));
        assertEquals(channel2, defaults.getDefaultChannelsMap().get(source2.getId()));

        successUpdate(source3.toBuilder()
                .setDefaultForProject(true)
                .setVersion(-1)
                .build());
        successUpdate(source2.toBuilder()
                .setDefaultForProject(false)
                .setVersion(-1)
                .build());
        var channel3 = service.getNotificationChannelById(source3.getId());

        assertEquals(2, defaults.getDefaultChannelsMap().size());
        assertEquals(channel, defaults.getDefaultChannelsMap().get(source.getId()));
        assertEquals(channel3, defaults.getDefaultChannelsMap().get(source3.getId()));
    }

    @Test
    public void update_defaultForCreated() {
        TNotification source = randomNotification().toBuilder()
                .setDefaultForProject(false)
                .clearDefaultForAlertSeverity()
                .build();
        successCreate(source);

        var defaults = service.getDefaultChannelsHolder();
        assertEquals(0, defaults.getDefaultChannelsMap().size());

        Alert alert = randomAlert().toBuilder()
                .setNotificationChannels(Map.of(source.getId(), ChannelConfig.EMPTY))
                .setEscalation(UUID.randomUUID().toString())
                .build();
        service.prepareChannels(alert);
        successUpdate(source.toBuilder()
                .setDefaultForProject(true)
                .setVersion(-1)
                .build());
        var channel = service.getNotificationChannelById(source.getId());

        assertEquals(1, defaults.getDefaultChannelsMap().size());
        assertEquals(channel, defaults.getDefaultChannelsMap().get(source.getId()));
    }

    @Test
    public void delete_default() {
        TNotification source = randomNotification().toBuilder()
                .setDefaultForProject(true)
                .clearDefaultForAlertSeverity()
                .build();
        TNotification source2 = randomNotification().toBuilder()
                .setDefaultForProject(true)
                .clearDefaultForAlertSeverity()
                .setProjectId(source.getProjectId())
                .build();
        TNotification source3 = randomNotification().toBuilder()
                .setDefaultForProject(false)
                .clearDefaultForAlertSeverity()
                .setProjectId(source.getProjectId())
                .build();
        successCreate(source);
        successCreate(source2);
        successCreate(source3);

        var channel = service.getNotificationChannelById(source.getId());
        var channel2 = service.getNotificationChannelById(source2.getId());
        var defaults = service.getDefaultChannelsHolder();
        assertEquals(2, defaults.getDefaultChannelsMap().size());
        assertEquals(channel, defaults.getDefaultChannelsMap().get(source.getId()));
        assertEquals(channel2, defaults.getDefaultChannelsMap().get(source2.getId()));

        successDelete(source3.getId());
        successDelete(source2.getId());

        assertEquals(1, defaults.getDefaultChannelsMap().size());
        assertEquals(channel, defaults.getDefaultChannelsMap().get(source.getId()));
    }

    @Test
    public void listEscalations() {
        jns.escalations = new JnsListEscalationPolicy("", "", List.of(
                new JnsListEscalationPolicy.JnsEscalationPolicy("e1", "e11"),
                new JnsListEscalationPolicy.JnsEscalationPolicy("e2", "e22")
        ));
        var response = service.listEscalations(TListEscalationsRequest.newBuilder()
                .build()).join();
        assertEquals(response.getRequestStatus(), ERequestStatusCode.OK);
        assertEquals(List.of(
                EscalationView.newBuilder()
                        .setId("e1")
                        .setTitle("e11")
                        .build(),
                EscalationView.newBuilder()
                        .setId("e2")
                        .setTitle("e22")
                        .build()
        ), response.getEscalationViewsList());

        jns.escalations = new JnsListEscalationPolicy("error", "error", List.of());
        response = service.listEscalations(TListEscalationsRequest.newBuilder()
                .build()).join();
        assertEquals(response.getRequestStatus(), ERequestStatusCode.NODE_UNAVAILABLE);
        assertEquals(List.of(), response.getEscalationViewsList());

        jns.setFail(true);
        response = service.listEscalations(TListEscalationsRequest.newBuilder()
                .build()).join();
        assertEquals(response.getRequestStatus(), ERequestStatusCode.NODE_UNAVAILABLE);
        assertEquals(List.of(), response.getEscalationViewsList());
    }
}
