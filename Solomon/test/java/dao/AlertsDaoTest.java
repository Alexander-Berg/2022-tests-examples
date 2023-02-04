package ru.yandex.solomon.alert.dao;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.google.common.base.Throwables;
import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import org.junit.Test;

import ru.yandex.solomon.alert.api.converters.AlertConverter;
import ru.yandex.solomon.alert.domain.Alert;
import ru.yandex.solomon.alert.protobuf.TAlert;
import ru.yandex.solomon.core.container.ContainerType;
import ru.yandex.solomon.idempotency.IdempotentOperation;
import ru.yandex.solomon.idempotency.IdempotentOperationExistException;
import ru.yandex.solomon.idempotency.dao.IdempotentOperationDao;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static ru.yandex.solomon.alert.domain.AlertMatchers.reflectionEqualTo;
import static ru.yandex.solomon.alert.domain.AlertTestSupport.alertFromTemplatePersistent;
import static ru.yandex.solomon.alert.domain.AlertTestSupport.randomAlert;
import static ru.yandex.solomon.alert.domain.AlertTestSupport.randomAlertWithConfiguredChannels;
import static ru.yandex.solomon.idempotency.IdempotentOperation.NO_OPERATION;

/**
 * @author Vladimir Gordiychuk
 */
public abstract class AlertsDaoTest {
    protected abstract EntitiesDao<Alert> getDao();
    protected abstract IdempotentOperationDao getOperationsDao();

    private EntitiesDao<Alert> getDao(String projectId) {
        var dao = getDao();
        dao.createSchemaForTests()
            .thenCompose(o -> dao.createSchema(projectId))
            .join();
        return dao;
    }

    @Test
    public void createSchemaMultipleTimes() {
        getDao().createSchemaForTests().join();
        getDao().createSchemaForTests().join();
        getDao().createSchemaForTests().join();
    }

    @Test
    public void emptyFind() {
        List<Alert> list = list("junk");
        assertThat(list, emptyIterable());
    }

    @Test
    public void insertAlert() {
        Alert alert = randomAlertWithConfiguredChannels(ThreadLocalRandom.current());
        var dao = getDao(alert.getProjectId());
        dao.insert(alert, NO_OPERATION).join();
        var alerts = list(alert.getProjectId());
        assertThat(alerts, hasItem(reflectionEqualTo(alert)));
    }

    @Test
    public void insertAlert_IdempotentExist() {
        try {
            Alert alert = randomAlertWithConfiguredChannels(ThreadLocalRandom.current());
            Alert alert2 = randomAlertWithConfiguredChannels(ThreadLocalRandom.current());
            var dao = getDao(alert.getProjectId());
            dao.insert(alert, operation(alert, "op1")).join();
            dao.insert(alert2, operation(alert2, "op2")).join();
            dao.insert(alert, operation(alert, "op1")).join();
        } catch (CompletionException e) {
            assertTrue(Throwables.getRootCause(e) instanceof IdempotentOperationExistException);
        }
    }

    @Test
    public void insertAlert_Idempotent() throws InvalidProtocolBufferException {
        Alert alert = randomAlertWithConfiguredChannels(ThreadLocalRandom.current());
        Alert alert2 = randomAlertWithConfiguredChannels(ThreadLocalRandom.current());
        var dao = getDao(alert.getProjectId());
        dao.insert(alert, operation(alert, "op1")).join();
        dao.insert(alert2, operation(alert2, "op2")).join();

        var alerts = list(alert.getProjectId());
        assertThat(alerts, hasSize(2));
        assertThat(alerts, hasItem(reflectionEqualTo(alert)));
        assertThat(alerts, hasItem(reflectionEqualTo(alert2)));

        var op1 = getOperationsDao().get("op1", alert.getProjectId(), ContainerType.PROJECT, "1").join();
        assertTrue(op1.isPresent());
        assertEquals(op1.get().entityId(), alert.getId());
        assertEquals(AlertConverter.protoToAlert(op1.get().result().unpack(TAlert.class)), alert);

        var op2 = getOperationsDao().get("op2", alert.getProjectId(), ContainerType.PROJECT, "1").join();
        assertTrue(op2.isPresent());
        assertEquals(op2.get().entityId(), alert2.getId());
        assertEquals(AlertConverter.protoToAlert(op2.get().result().unpack(TAlert.class)), alert2);
    }

    @Test
    public void insertAlert_noIdempotent() throws InvalidProtocolBufferException {
        Alert alert = randomAlertWithConfiguredChannels(ThreadLocalRandom.current());
        var dao = getDao(alert.getProjectId());
        dao.insert(alert, NO_OPERATION).join();

        var alerts = list(alert.getProjectId());
        assertThat(alerts, hasSize(1));
        assertThat(alerts, hasItem(reflectionEqualTo(alert)));

        var op1 = getOperationsDao().get(NO_OPERATION.id(), NO_OPERATION.containerId(), NO_OPERATION.containerType(), NO_OPERATION.operationType()).join();
        assertTrue(op1.isEmpty());
    }

    @Test
    public void insertMany() throws InterruptedException {
        String projectId = "test";
        List<Alert> source = IntStream.range(0, 1001)
                .mapToObj(index -> randomAlert()
                        .toBuilder()
                        .setName(String.valueOf(index))
                        .setProjectId(projectId)
                        .build())
                .sorted(Comparator.comparing(Alert::getName))
                .collect(Collectors.toList());

        var dao = getDao(projectId);

        var threadPool = new ForkJoinPool(2);
        try {
            var future = threadPool.submit(() -> {
                source.stream()
                        .parallel() // implicitly uses current FJP to run tasks
                        .map(entity -> dao.insert(entity, NO_OPERATION))
                        .forEach(CompletableFuture::join);
            });
            // rethrow exceptions from submitted task
            future.get();
        } catch (ExecutionException e) {
            throw new RuntimeException("cannot process source stream", e);
        } finally {
            threadPool.shutdown();
        }

        List<Alert> list = list(projectId)
                .stream()
                .sorted(Comparator.comparing(Alert::getName))
                .collect(Collectors.toList());

        for (int index = 0; index < source.size(); index++) {
            Alert expected = source.get(index);
            Alert result = list.get(index);
            assertThat(result, reflectionEqualTo(expected));
        }
    }

    @Test
    public void insertIntoDifferentProjects() {
        Alert alice = randomAlert()
                .toBuilder()
                .setProjectId("alice")
                .build();

        Alert bob = randomAlert()
                .toBuilder()
                .setProjectId("bob")
                .build();

        getDao(alice.getProjectId())
                .insert(alice, NO_OPERATION)
                .join();

        getDao(bob.getProjectId())
                .insert(bob, NO_OPERATION)
                .join();

        assertThat(list(alice.getProjectId()),
                allOf(
                        iterableWithSize(1),
                        hasItem(reflectionEqualTo(alice))));

        assertThat(list(bob.getProjectId()),
                allOf(
                        iterableWithSize(1),
                        hasItem(reflectionEqualTo(bob))));
    }

    @Test
    public void deleteProjectById() {
        Alert alice = randomAlert()
            .toBuilder()
            .setProjectId("alice")
            .build();

        Alert bob = randomAlert()
            .toBuilder()
            .setProjectId("bob")
            .build();

        getDao(alice.getProjectId())
            .insert(alice, NO_OPERATION)
            .join();

        getDao(bob.getProjectId())
            .insert(bob, NO_OPERATION)
            .join();

        getDao().deleteProject(alice.getProjectId()).join();

        assertThat(list(alice.getProjectId()), emptyIterable());
        assertThat(list(bob.getProjectId()),
            allOf(
                iterableWithSize(1),
                hasItem(reflectionEqualTo(bob))));
    }

    @Test
    public void updateByVersion() {
        var created = randomAlert();
        var dao = getDao(created.getProjectId());
        dao.insert(created, NO_OPERATION).join();
        var updated = created.toBuilder()
                .setName("updated name")
                .setVersion(created.getVersion() + 1)
                .build();
        dao.update(updated, NO_OPERATION).join();

        assertThat(list(created.getProjectId()), hasItem(reflectionEqualTo(updated)));
    }

    @Test
    public void updateChecksVersion() {
        var created = randomAlert();
        var dao = getDao(created.getProjectId());
        var inserted = dao.insert(created, NO_OPERATION).join();
        assertTrue(inserted.isEmpty());
        {
            var prev = dao.update(created.toBuilder()
                    .setName("No way")
                    .build(), NO_OPERATION).join().get();
            assertEquals(created, prev);
            assertEquals(created.getName(), get(created.getProjectId(), created.getId()).getName());
        }
        {
            var prev = dao.update(created.toBuilder()
                    .setName("Modified")
                    .setVersion(created.getVersion() + 1).build(), NO_OPERATION).join().get();
            assertEquals(created, prev);
            assertEquals("Modified", get(created.getProjectId(), created.getId()).getName());
        }
        {
            var prev = dao.update(created.toBuilder()
                    .setName("Modified again")
                    .setVersion(created.getVersion() + 2).build(), NO_OPERATION).join().get();
            assertEquals(created.getVersion() + 1, prev.getVersion());
            assertEquals("Modified again", get(created.getProjectId(), created.getId()).getName());
        }
    }

    @Test
    public void updateAlert_IdempotentExist() {
        Alert alert = randomAlertWithConfiguredChannels(ThreadLocalRandom.current());
        var dao = getDao(alert.getProjectId());
        dao.insert(alert, operation(alert, "op1")).join();
        var updated = alert.toBuilder()
                .setName("updated name")
                .setVersion(alert.getVersion() + 1)
                .build();
        dao.update(updated, operation(updated, "op2")).join();
        try {
            var updated2 = alert.toBuilder()
                    .setName("updated name")
                    .setVersion(updated.getVersion() + 1)
                    .build();
            dao.update(updated2, operation(updated2, "op2")).join();
        } catch (CompletionException e) {
            assertTrue(Throwables.getRootCause(e) instanceof IdempotentOperationExistException);
        }
    }

    @Test
    public void updateAlert_Idempotent() throws InvalidProtocolBufferException {
        Alert alert = randomAlertWithConfiguredChannels(ThreadLocalRandom.current());
        var dao = getDao(alert.getProjectId());
        dao.insert(alert, operation(alert, "op1")).join();

        var updated = alert.toBuilder()
                .setName("updated name")
                .setVersion(alert.getVersion() + 1)
                .build();
        dao.update(updated, operation(updated, "op2")).join();
        var updated2 = updated.toBuilder()
                .setDescription("updated desc")
                .setVersion(updated.getVersion() + 1)
                .build();
        dao.update(updated2, operation(updated2, "op3")).join();

        var alerts = list(alert.getProjectId());
        assertThat(alerts, hasSize(1));
        assertThat(alerts, hasItem(reflectionEqualTo(updated2)));

        var op1 = getOperationsDao().get("op2", alert.getProjectId(), ContainerType.PROJECT, "1").join();
        assertTrue(op1.isPresent());
        assertEquals(op1.get().entityId(), alert.getId());
        assertEquals(AlertConverter.protoToAlert(op1.get().result().unpack(TAlert.class)), updated);

        var op2 = getOperationsDao().get("op3", alert.getProjectId(), ContainerType.PROJECT, "1").join();
        assertTrue(op2.isPresent());
        assertEquals(op2.get().entityId(), alert.getId());
        assertEquals(AlertConverter.protoToAlert(op2.get().result().unpack(TAlert.class)), updated2);
    }

    @Test
    public void updateAlert_noIdempotent() {
        Alert alert = randomAlertWithConfiguredChannels(ThreadLocalRandom.current());
        var dao = getDao(alert.getProjectId());
        dao.insert(alert, NO_OPERATION).join();
        var updated = alert.toBuilder()
                .setName("updated name")
                .setVersion(alert.getVersion() + 1)
                .build();
        dao.update(updated, NO_OPERATION).join();

        var alerts = list(alert.getProjectId());
        assertThat(alerts, hasSize(1));
        assertThat(alerts, hasItem(reflectionEqualTo(updated)));

        var op1 = getOperationsDao().get(NO_OPERATION.id(), NO_OPERATION.containerId(), NO_OPERATION.containerType(), NO_OPERATION.operationType()).join();
        assertTrue(op1.isEmpty());
    }

    @Test
    public void deleteProjectById_IdempotentExist() {
        Alert alert = randomAlert();
        Alert alert2 = randomAlert();
        var dao = getDao(alert.getProjectId());
        dao.insert(alert, NO_OPERATION).join();
        dao.insert(alert2, NO_OPERATION).join();
        dao.deleteById(alert.getProjectId(), alert.getId(), operation(alert, "op1")).join();

        try {
            dao.deleteById(alert2.getProjectId(), alert2.getId(), operation(alert2, "op1")).join();
        } catch (CompletionException e) {
            assertTrue(Throwables.getRootCause(e) instanceof IdempotentOperationExistException);
        }
    }

    @Test
    public void delete_Idempotent() {
        Alert alert = randomAlert();
        Alert alert2 = randomAlert();
        var dao = getDao(alert.getProjectId());
        dao.insert(alert, NO_OPERATION).join();
        dao.insert(alert2, NO_OPERATION).join();
        dao.deleteById(alert.getProjectId(), alert.getId(), operation(alert, "op1")).join();
        dao.deleteById(alert2.getProjectId(), alert2.getId(), operation(alert2, "op2")).join();

        var op1 = getOperationsDao().get("op1", alert.getProjectId(), ContainerType.PROJECT, "1").join();
        assertTrue(op1.isPresent());
        assertEquals(op1.get().entityId(), alert.getId());

        var op2 = getOperationsDao().get("op2", alert.getProjectId(), ContainerType.PROJECT, "1").join();
        assertTrue(op2.isPresent());
        assertEquals(op2.get().entityId(), alert2.getId());
    }

    @Test
    public void delete_noIdempotent() {
        Alert alert = randomAlert();
        var dao = getDao(alert.getProjectId());
        dao.insert(alert, NO_OPERATION).join();
        dao.deleteById(alert.getProjectId(), alert.getId(), NO_OPERATION).join();
        List<Alert> list = list(alert.getProjectId());
        assertThat(list, emptyIterable());

        var op1 = getOperationsDao().get(NO_OPERATION.id(), NO_OPERATION.containerId(), NO_OPERATION.containerType(), NO_OPERATION.operationType()).join();
        assertTrue(op1.isEmpty());
    }

    @Test
    public void insertIdempotent() {
        Alert alert = randomAlert();
        var dao = getDao(alert.getProjectId());
        var first = dao.insert(alert, NO_OPERATION).join();
        var second = dao.insert(alert, NO_OPERATION).join();
        assertTrue(first.isEmpty());
        assertEquals(alert, second.get());
    }

    @Test
    public void insertConflictIsNop() {
        Alert alert = randomAlert();
        var dao = getDao(alert.getProjectId());
        var inserted = dao.insert(alert, NO_OPERATION).join();
        assertTrue(inserted.isEmpty());
        Alert other = alert.toBuilder()
                .setDescription("other")
                .build();
        var first = dao.insert(other, NO_OPERATION).join();
        var second = dao.insert(other, NO_OPERATION).join();
        assertEquals(alert, first.get());
        assertEquals(alert, second.get());
    }

    @Test
    public void delete() {
        Alert alert = randomAlert();
        var dao = getDao(alert.getProjectId());
        dao.insert(alert, NO_OPERATION).join();
        dao.deleteById(alert.getProjectId(), alert.getId(), NO_OPERATION).join();
        List<Alert> list = list(alert.getProjectId());
        assertThat(list, emptyIterable());
    }

    @Test
    public void delaySeconds() {
        Alert v1 = randomAlert()
                .toBuilder()
                .setDelaySeconds(ThreadLocalRandom.current().nextInt(1, 360))
                .build();

        var dao = getDao(v1.getProjectId());
        dao.insert(v1, NO_OPERATION).join();
        assertThat(dao.findAll(v1.getProjectId()).join(), hasItem(reflectionEqualTo(v1)));

        Alert v2 = v1.toBuilder()
                .setDelaySeconds(ThreadLocalRandom.current().nextInt())
                .setVersion(v1.getVersion() + 1)
                .build();
        dao.update(v2, NO_OPERATION).join();
        assertThat(list(v1.getProjectId()), hasItem(reflectionEqualTo(v2)));
    }

    // Necessary to support new functionality
    @Test
    public void description() {
        Alert v1 = randomAlert()
                .toBuilder()
                .setDescription("Description with random: " + ThreadLocalRandom.current().nextInt(0, 100))
                .build();

        var dao = getDao(v1.getProjectId());
        dao.insert(v1, NO_OPERATION).join();
        assertThat(dao.findAll(v1.getProjectId()).join(), hasItem(reflectionEqualTo(v1)));

        Alert v2 = v1.toBuilder()
                .setDescription("Description with random: " + ThreadLocalRandom.current().nextInt(200, 300))
                .setVersion(v1.getVersion() + 1)
                .build();
        dao.update(v2, NO_OPERATION).join();
        assertThat(list(v1.getProjectId()), hasItem(reflectionEqualTo(v2)));
    }

    @Test
    public void findProjectIds() {
        var dao = getDao();
        dao.createSchemaForTests().join();

        assertEquals(Set.of(), dao.findProjects().join());
        for (var index = 0; index < 3; index++) {
            dao.insert(randomAlert().toBuilder().setProjectId("alice").build(), NO_OPERATION).join();
            assertEquals(Set.of("alice"), dao.findProjects().join());
        }

        for (var index = 0; index < 3; index++) {
            dao.insert(randomAlert().toBuilder().setProjectId("bob").build(), NO_OPERATION).join();
            assertEquals(Set.of("alice", "bob"), dao.findProjects().join());
        }
    }

    @Test
    public void insertAlertFromTemplate() {
        Alert alert = alertFromTemplatePersistent(ThreadLocalRandom.current());
        var dao = getDao(alert.getProjectId());
        dao.insert(alert, NO_OPERATION).join();
        var alerts = list(alert.getProjectId());
        assertThat(alerts, hasItem(reflectionEqualTo(alert)));
    }

    private List<Alert> list(String projectId) {
        var dao = getDao(projectId);
        return dao.findAll(projectId).join();
    }

    private Alert get(String projectId, String id) {
        return list(projectId).stream()
                .filter(alert -> alert.getId().equals(id))
                .findFirst()
                .get();
    }


    private IdempotentOperation operation(Alert alert, String id) {
        return new IdempotentOperation(id,
                alert.getProjectId(),
                ContainerType.PROJECT,
                "1",
                alert.getId(),
                Any.pack(AlertConverter.alertToProto(alert)),
                0);
    }
}
