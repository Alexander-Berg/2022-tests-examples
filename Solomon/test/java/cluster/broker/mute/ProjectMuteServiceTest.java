package ru.yandex.solomon.alert.cluster.broker.mute;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import javax.annotation.ParametersAreNonnullByDefault;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.misc.concurrent.CompletableFutures;
import ru.yandex.solomon.alert.api.converters.MuteConverter;
import ru.yandex.solomon.alert.dao.memory.InMemoryMutesDao;
import ru.yandex.solomon.alert.protobuf.CreateMuteRequest;
import ru.yandex.solomon.alert.protobuf.DeleteMuteRequest;
import ru.yandex.solomon.alert.protobuf.ListMutesRequest;
import ru.yandex.solomon.alert.protobuf.MuteStatus;
import ru.yandex.solomon.alert.protobuf.ReadMuteRequest;
import ru.yandex.solomon.alert.protobuf.ReadMuteStatsRequest;
import ru.yandex.solomon.alert.protobuf.UpdateMuteRequest;
import ru.yandex.solomon.alert.protobuf.mute.Mute;
import ru.yandex.solomon.ut.ManualClock;
import ru.yandex.solomon.ut.ManualScheduledExecutorService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.fail;

/**
 * @author Ivan Tsybulin
 */
@ParametersAreNonnullByDefault
public class ProjectMuteServiceTest extends ProjectMuteServiceTestBase {
    @Before
    public void setUp() throws Exception {
        this.projectId = "junk" + ThreadLocalRandom.current().nextInt(0, 100);
        this.clock = new ManualClock();
        this.dao = new InMemoryMutesDao();
        this.executorService = new ManualScheduledExecutorService(2, clock);
        this.converter = MuteConverter.INSTANCE;
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
        Mute source = randomMute();
        Mute result = successCreate(source)
                .toBuilder()
                .clearStatus()
                .clearVersion()
                .clearUpdatedAt()
                .clearCreatedAt()
                .build();

        assertEquals(source, result);
    }

    private static void expectFail(Status.Code code, CompletableFuture<?> future) {
        try {
            future.join();
        } catch (CompletionException e) {
            StatusRuntimeException sre = (StatusRuntimeException) CompletableFutures.unwrapCompletionException(e);
            assertEquals(sre.getMessage(), code, sre.getStatus().getCode());
            return;
        }

        fail("Expected to fail with status code " + code);
    }

    @Test
    public void createAndRead() {
        Mute create = successCreate(randomMute());
        Mute read = successRead(create.getId());
        assertEquals(create, read);
    }

    @Test
    public void notAbleCreateAlertWithSameIdTwice() {
        Mute v1 = successCreate(randomMute());

        expectFail(Status.Code.ALREADY_EXISTS, service.createMute(CreateMuteRequest.newBuilder()
                            .setMute(randomMute().toBuilder()
                                    .setId(v1.getId()))
                            .build()));
    }

    @Test
    public void update() {
        Mute v1 = successCreate(randomMute());
        Mute v2 = successUpdate(v1.toBuilder()
                .setDescription("Better description")
                .build());

        assertNotEquals(v1, v2);
        assertEquals("Better description", v2.getDescription());

        Mute v3 = successUpdate(v2.toBuilder()
                .setDescription("Much better description")
                .build());
        assertNotEquals(v2, v3);
        assertEquals("Much better description", v3.getDescription());
    }

    @Test
    public void updateWithoutVersion() {
        Mute v1 = successCreate(randomMute());

        for (int version = 1; version < 10; ++version) {
            successUpdate(v1.toBuilder()
                    .setDescription("Mute (updated #" + version + ")")
                    .setVersion(version)
                    .build());
        }

        Mute v11 = successUpdate(v1.toBuilder()
                .setDescription("Mute (updated #11)")
                .setVersion(-1)
                .build());

        assertEquals("Mute (updated #11)", v11.getDescription());
        assertEquals(11, v11.getVersion());
    }

    @Test
    public void saveCreatedFieldsImmutable() {
        Instant now = clock.instant().truncatedTo(ChronoUnit.SECONDS);
        String createdBy = "bob";

        Mute v1 = successCreate(randomMute()
                .toBuilder()
                .setCreatedAt(now.toEpochMilli())
                .setCreatedBy(createdBy)
                .build());

        assertEquals(createdBy, v1.getCreatedBy());
        assertThat(v1.getCreatedAt(), greaterThanOrEqualTo(now.toEpochMilli()));

        Mute v2 = successUpdate(v1.toBuilder()
                .setDescription("Maintenance")
                .setCreatedAt(now.toEpochMilli() + TimeUnit.SECONDS.toMillis(100))
                .setCreatedBy("alice")
                .build());

        assertEquals("Maintenance", v2.getDescription());
        assertEquals(createdBy, v2.getCreatedBy());
        assertEquals(v1.getCreatedAt(), v2.getCreatedAt());
    }

    @Test
    public void readUpdated() {
        Mute v1 = successCreate(randomMute());
        Mute v2 = successUpdate(v1.toBuilder()
                .setDescription("updating")
                .build());

        Mute read = successRead(v2.getId());
        assertEquals(v2, read);
    }

    @Test
    public void conflictingUpdate() {
        Mute v1 = successCreate(randomMute());
        Mute v2 = successUpdate(v1.toBuilder()
                .setDescription("updating")
                .build());

        expectFail(Status.Code.FAILED_PRECONDITION, service.updateMute(UpdateMuteRequest.newBuilder()
                        .setMute(v1.toBuilder()
                                .setDescription("Conflict")
                                .build())
                        .build()));

        Mute read = successRead(v2.getId());
        assertEquals(v2, read);
    }

    @Test
    public void notAbleUpdateNotExistingAlert() {
        expectFail(Status.Code.NOT_FOUND, service.updateMute(UpdateMuteRequest.newBuilder()
                        .setMute(randomMute())
                        .build()));
    }

    @Test
    public void notAbleDeleteNotExistingAlert() {
        expectFail(Status.Code.NOT_FOUND, service.deleteMute(DeleteMuteRequest.newBuilder()
                        .setId("notExistsId")
                        .build()));
    }

    @Test
    public void notAbleReadNotExistingAlert() {
        expectFail(Status.Code.NOT_FOUND, service.readMute(ReadMuteRequest.newBuilder()
                        .setId("notExistsId")
                        .build()));
    }

    @Test
    public void updateAltersUpdateTime() {
        Mute v1 = successCreate(randomMute());

        clock.passedTime(1, TimeUnit.MINUTES);
        Mute v2 = successUpdate(v1.toBuilder()
                .setDescription("v2")
                .build());

        assertThat(v2.getUpdatedAt(), greaterThan(v1.getUpdatedAt()));

        clock.passedTime(5, TimeUnit.MINUTES);
        Mute v3 = successUpdate(v2.toBuilder()
                .setDescription("v3")
                .build());

        assertThat(v3.getUpdatedAt(), greaterThan(v2.getUpdatedAt()));
    }

    @Test
    public void delete() {
        Mute created = successCreate(randomMute());
        successDelete(created.getId());

        expectFail(Status.Code.NOT_FOUND, service.readMute(ReadMuteRequest.newBuilder()
                        .setId(created.getId())
                        .build()));
    }

    @Test
    public void createPersisted() {
        Mute create = successCreate(randomMute());
        restartService();

        Mute read = successRead(create.getId());
        assertEquals(create, read);
    }

    @Test
    public void updatePersisted() {
        Mute create = successCreate(randomMute());
        Mute updated = successUpdate(create.toBuilder()
                .setDescription("changed")
                .build());
        restartService();

        Mute read = successRead(create.getId());
        assertEquals(updated, read);
    }

    @Test
    public void deletePersisted() {
        Mute created = successCreate(randomMute());
        successDelete(created.getId());
        restartService();

        expectFail(Status.Code.NOT_FOUND, service.readMute(ReadMuteRequest.newBuilder()
                        .setId(created.getId())
                        .setProjectId(projectId)
                        .build()));
    }

    @Test
    public void createNotAbleWhenServiceNotReady() {
        service.close();
        createService();
        expectFail(Status.Code.UNAVAILABLE, service.createMute(CreateMuteRequest.newBuilder()
                        .setMute(randomMute())
                        .build()));
    }

    @Test
    public void updateNotAbleWhenServiceNotReady() {
        service.close();
        createService();
        expectFail(Status.Code.UNAVAILABLE, service.updateMute(UpdateMuteRequest.newBuilder()
                        .setMute(randomMute())
                        .build()));
    }

    @Test
    public void readNotAbleWhenServiceNotReady() {
        service.close();
        createService();
        expectFail(Status.Code.UNAVAILABLE, service.readMute(ReadMuteRequest.newBuilder()
                        .setId("myId")
                        .setProjectId(projectId)
                        .build()));
    }

    @Test
    public void listNotAbleWhenServiceNotReady() {
        service.close();
        createService();
        expectFail(Status.Code.UNAVAILABLE, service.listMutes(ListMutesRequest.newBuilder()
                        .setProjectId(projectId)
                        .setPageSize(10)
                        .build()));
    }

    @Test
    public void deleteNotAbleWhenServiceNotReady() {
        service.close();
        createService();
        expectFail(Status.Code.UNAVAILABLE, service.deleteMute(DeleteMuteRequest.newBuilder()
                        .setId("myId")
                        .setProjectId(projectId)
                        .build()));
    }

    @Test
    public void listByDefaultLimited() {
        List<Mute> source = createManyMutes();

        Mute[] result = successList(ListMutesRequest.newBuilder()
                .setProjectId(projectId)
                .build());

        assertThat(result.length, allOf(greaterThan(0), lessThanOrEqualTo(source.size())));
    }


    @Test
    public void listNextPageToken() {
        List<Mute> source = createManyMutes();

        {
            var response = service.listMutes(ListMutesRequest.newBuilder()
                            .setProjectId(projectId)
                            .setPageSize(source.size())
                            .build())
                    .join();

            assertEquals(source.size(), response.getMutesCount());
            assertEquals("", response.getNextPageToken());
        }
        {
            var response = service.listMutes(ListMutesRequest.newBuilder()
                            .setProjectId(projectId)
                            .setPageSize(5)
                            .build())
                    .join();

            assertEquals(5, response.getMutesCount());
            assertNotEquals("", response.getNextPageToken());
        }
    }

    private Mute createMuteAndExpectSuccess(Mute.Builder builder) {
        return service.createMute(CreateMuteRequest.newBuilder()
                .setMute(builder)
                .build()).join().getMute();
    }

    @Test
    public void readStats() {
        Random rnd = ThreadLocalRandom.current();
        long nowMillis = clock.millis();

        long active = 0;
        long pending = 0;
        long expired = 0;
        long obsolete = 0;

        for (int i = 0; i < 100; i++) {
            var builder = randomMute().toBuilder();
            switch (rnd.nextInt(4)) {
                case 0 -> {
                    builder
                            .setFromMillis(nowMillis - 10_000)
                            .setToMillis(nowMillis + 10_000);
                    active++;
                }
                case 1 -> {
                    builder
                            .setFromMillis(nowMillis + 10_000)
                            .setToMillis(nowMillis + 20_000);
                    pending++;
                }
                case 2 -> {
                    builder
                            .setFromMillis(nowMillis - 20_000)
                            .setToMillis(nowMillis - 10_000);
                    expired++;
                }
                case 3 -> {
                    builder
                            .setFromMillis(nowMillis - 3 * 86400_000)
                            .setToMillis(nowMillis - 2 * 86400_000);
                    obsolete++;
                }
            }
            createMuteAndExpectSuccess(builder);
        }

        var stats = service.readMuteStats(ReadMuteStatsRequest.newBuilder()
                .setProjectId(projectId)
                .build())
                .join()
                .getMutesStats();

        System.out.println(stats);

        assertEquals(active, stats.getCountActive());
        assertEquals(pending, stats.getCountPending());
        assertEquals(expired, stats.getCountExpired());
        assertEquals(obsolete, stats.getCountArchived());
    }

    @Test
    public void oldArchivedAreDeleted() {
        long nowMillis = clock.millis();

        Mute old = createMuteAndExpectSuccess(randomMute().toBuilder()
                .setProjectId(projectId)
                .setFromMillis(nowMillis)
                .setToMillis(nowMillis + TimeUnit.DAYS.toMillis(1))
                .clearTtlBase());

        Mute fresh = createMuteAndExpectSuccess(randomMute().toBuilder()
                .setProjectId(projectId)
                .setFromMillis(nowMillis + TimeUnit.DAYS.toMillis(6))
                .setToMillis(nowMillis + TimeUnit.DAYS.toMillis(7))
                .clearTtlBase());

        clock.passedTime(10, TimeUnit.DAYS);

        var response = service.listMutes(ListMutesRequest.newBuilder()
                        .setProjectId(projectId)
                        .setPageSize(10)
                        .build())
                .join();
        assertEquals(1, response.getMutesCount());
        assertEquals(fresh.toBuilder().setStatus(MuteStatus.ARCHIVED).build(), response.getMutes(0));

        expectFail(Status.Code.NOT_FOUND, service.readMute(ReadMuteRequest.newBuilder()
                .setId(old.getId())
                .setProjectId(old.getProjectId())
                .build()));
    }

    @Test
    public void oldArchivedMayBeOverwritten() {
        long nowMillis = clock.millis();

        Mute old = createMuteAndExpectSuccess(randomMute().toBuilder()
                    .setProjectId(projectId)
                    .setFromMillis(nowMillis)
                    .setToMillis(nowMillis + TimeUnit.DAYS.toMillis(1))
                    .clearTtlBase());

        Mute fresh = createMuteAndExpectSuccess(randomMute().toBuilder()
                    .setProjectId(projectId)
                    .setFromMillis(nowMillis + TimeUnit.DAYS.toMillis(6))
                    .setToMillis(nowMillis + TimeUnit.DAYS.toMillis(7))
                    .clearTtlBase());

        clock.passedTime(10, TimeUnit.DAYS);

        var response = service.listMutes(ListMutesRequest.newBuilder()
                        .setProjectId(projectId)
                        .setPageSize(10)
                        .build())
                .join();
        assertEquals(1, response.getMutesCount());
        assertEquals(fresh.toBuilder().setStatus(MuteStatus.ARCHIVED).build(), response.getMutes(0));

        Mute replacement = createMuteAndExpectSuccess(randomMute().toBuilder()
                .setId(old.getId())
                .setProjectId(old.getProjectId())
                .setFromMillis(clock.millis())
                .setToMillis(clock.millis() + 1000)
                .clearTtlBase());

        var read = service.readMute(ReadMuteRequest.newBuilder()
                .setId(replacement.getId())
                .setProjectId(replacement.getProjectId())
                .build()).join().getMute();

        assertEquals(read, replacement);

        var response2 = service.listMutes(ListMutesRequest.newBuilder()
                        .setProjectId(projectId)
                        .setPageSize(10)
                        .build())
                .join();
        assertEquals(2, response2.getMutesCount());
    }

    @Test
    public void canceledNotAbleRunAgain() {
        service.run().join();
        service.close();
        // repeat run can be evaluated during retry init
        service.run().join();

        expectFail(Status.Code.UNAVAILABLE, service.createMute(CreateMuteRequest.newBuilder()
                .setMute(randomMute())
                .build()));
    }
}
