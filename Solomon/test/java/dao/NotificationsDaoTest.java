package ru.yandex.solomon.alert.dao;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Test;

import ru.yandex.solomon.alert.domain.AlertMatchers;
import ru.yandex.solomon.alert.domain.AlertSeverity;
import ru.yandex.solomon.alert.notification.domain.Notification;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static ru.yandex.solomon.alert.notification.domain.NotificationTestSupport.randomNotification;
import static ru.yandex.solomon.alert.util.CommonMatchers.reflectionEqualTo;
import static ru.yandex.solomon.idempotency.IdempotentOperation.NO_OPERATION;

/**
 * @author Vladimir Gordiychuk
 */
public abstract class NotificationsDaoTest {
    protected abstract NotificationsDao getDao();

    private NotificationsDao getDao(String projectId) {
        var dao = getDao();
        dao.createSchemaForTests()
            .thenCompose(o -> dao.createSchema(projectId))
            .join();
        return dao;
    }

    @Test
    public void emptyFind() {
        List<Notification> list = list("junk");
        assertThat(list, emptyIterable());
    }

    @Test
    public void insert() {
        Notification entry = randomNotification();
        var dao = getDao(entry.getProjectId());
        dao.insert(entry, NO_OPERATION).join();
        List<Notification> list = list(entry.getProjectId());
        assertEquals(1, list.size());
        assertThat(list.get(0), reflectionEqualTo(entry));
    }

    @Test
    public void insertIdempotent() {
        Notification notification = randomNotification();
        var dao = getDao(notification.getProjectId());
        var first = dao.insert(notification, NO_OPERATION).join();
        var second = dao.insert(notification, NO_OPERATION).join();
        assertTrue(first.isEmpty());
        assertEquals(notification, second.get());
    }

    @Test
    public void insertConflictIsNop() {
        Notification notification = randomNotification();
        var dao = getDao(notification.getProjectId());
        var inserted = dao.insert(notification, NO_OPERATION).join();
        assertTrue(inserted.isEmpty());
        Notification other = notification.toBuilder()
                .setName("other")
                .build();
        var first = dao.insert(other, NO_OPERATION).join();
        var second = dao.insert(other, NO_OPERATION).join();
        assertEquals(notification, first.get());
        assertEquals(notification, second.get());
    }

    @Test
    public void insertMany() throws InterruptedException {
        String projectId = "test";
        List<Notification> source = IntStream.range(0, 1001)
                .mapToObj(index -> randomNotification()
                        .toBuilder()
                        .setName(String.valueOf(index))
                        .setProjectId(projectId)
                        .build())
                .sorted(Comparator.comparing(Notification::getName))
                .collect(Collectors.toList());

        var dao = getDao(projectId);

        var threadPool = new ForkJoinPool(2);
        try {
            var future = threadPool.submit(() -> {
                source.stream()
                        .parallel()
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

        List<Notification> list = list(projectId)
                .stream()
                .sorted(Comparator.comparing(Notification::getName))
                .collect(Collectors.toList());

        assertThat(list.size(), equalTo(source.size()));
        for (int index = 0; index < source.size(); index++) {
            Notification expected = source.get(index);
            Notification result = list.get(index);
            assertThat(result, reflectionEqualTo(expected));
        }
    }

    @Test
    public void insertIntoDifferentProjects() {
        Notification alice = randomNotification()
                .toBuilder()
                .setProjectId("alice")
                .build();

        Notification bob = randomNotification()
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
    public void deleteProject() {
        Notification alice = randomNotification()
            .toBuilder()
            .setProjectId("alice")
            .build();

        Notification bob = randomNotification()
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
        var created = randomNotification();
        var dao = getDao(created.getProjectId());
        dao.insert(created, NO_OPERATION).join();
        var updated = created.toBuilder()
                .setName("updated name")
                .setVersion(created.getVersion() + 1)
                .build();
        dao.update(updated, NO_OPERATION).join();

        assertThat(list(created.getProjectId()), hasItem(AlertMatchers.reflectionEqualTo(updated)));
    }

    @Test
    public void updateChecksVersion() {
        var created = randomNotification();
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
    public void delete() {
        Notification alert = randomNotification();
        var dao = getDao(alert.getProjectId());
        dao.insert(alert, NO_OPERATION).join();
        dao.deleteById(alert.getProjectId(), alert.getId(), NO_OPERATION).join();
        List<Notification> list = list(alert.getProjectId());
        assertThat(list, emptyIterable());
    }

    @Test
    public void delete_severity() {
        Notification alertD1 = randomNotification().toBuilder().setDefaultForSeverity(Set.of(AlertSeverity.DISASTER)).setProjectId("1").build();
        Notification alertD2 = randomNotification().toBuilder().setDefaultForSeverity(Set.of(AlertSeverity.DISASTER)).setProjectId("1").build();
        var dao = getDao(alertD1.getProjectId());
        dao.insert(alertD1, NO_OPERATION).join();
        dao.insert(alertD2, NO_OPERATION).join();
        dao.deleteByIdWithValidations(alertD1.getProjectId(), alertD1.getId(), NO_OPERATION, Set.of(AlertSeverity.DISASTER)).join();
        try {
            dao.deleteByIdWithValidations(alertD1.getProjectId(), alertD2.getId(), NO_OPERATION, Set.of(AlertSeverity.DISASTER)).join();
        } catch (Exception e) {
            assertEquals(e.getCause().getMessage(), "Try to delete last default channel for DISASTER severity");
        }

        Notification alertD3 = randomNotification().toBuilder().setDefaultForSeverity(Set.of(AlertSeverity.DISASTER, AlertSeverity.INFO)).setProjectId("1").build();
        dao.insert(alertD3, NO_OPERATION).join();

        try {
            dao.deleteByIdWithValidations(alertD1.getProjectId(), alertD3.getId(), NO_OPERATION, Set.of(AlertSeverity.DISASTER, AlertSeverity.INFO)).join();
        } catch (Exception e) {
            assertEquals(e.getCause().getMessage(), "Try to delete last default channel for INFO severity");
        }
        dao.deleteByIdWithValidations(alertD1.getProjectId(), alertD2.getId(), NO_OPERATION, Set.of(AlertSeverity.DISASTER)).join();
    }

    @Test
    public void delete_severityEmpty() {
        Notification alertD1 = randomNotification().toBuilder().setDefaultForSeverity(Set.of(AlertSeverity.DISASTER)).setProjectId("1").build();
        var dao = getDao(alertD1.getProjectId());
        dao.insert(alertD1, NO_OPERATION).join();
        dao.deleteByIdWithValidations(alertD1.getProjectId(), alertD1.getId(), NO_OPERATION, Set.of()).join();
    }

    @Test
    public void update_severity() {
        Notification alertD1 = randomNotification().toBuilder().setDefaultForSeverity(Set.of(AlertSeverity.DISASTER)).setProjectId("1").build();
        Notification alertD2 = randomNotification().toBuilder().setDefaultForSeverity(Set.of(AlertSeverity.DISASTER)).setProjectId("1").build();
        var dao = getDao(alertD1.getProjectId());
        dao.insert(alertD1, NO_OPERATION).join();
        dao.insert(alertD2, NO_OPERATION).join();
        dao.updateWithValidations(alertD1.toBuilder().setDefaultForSeverity(Set.of()).build(), NO_OPERATION, Set.of(AlertSeverity.DISASTER)).join();
        try {
            dao.updateWithValidations(alertD2.toBuilder().setDefaultForSeverity(Set.of()).build(), NO_OPERATION, Set.of(AlertSeverity.DISASTER)).join();
        } catch (Exception e) {
            assertEquals(e.getCause().getMessage(), "Try to delete last default channel for DISASTER severity");
        }

        Notification alertD3 = randomNotification().toBuilder().setDefaultForSeverity(Set.of(AlertSeverity.DISASTER, AlertSeverity.INFO)).setProjectId("1").build();
        dao.insert(alertD3, NO_OPERATION).join();

        try {
            dao.updateWithValidations(alertD3.toBuilder().setDefaultForSeverity(Set.of()).build(), NO_OPERATION, Set.of(AlertSeverity.DISASTER, AlertSeverity.INFO)).join();
        } catch (Exception e) {
            assertEquals(e.getCause().getMessage(), "Try to delete last default channel for INFO severity");
        }
        dao.updateWithValidations(alertD2.toBuilder().setDefaultForSeverity(Set.of()).build(), NO_OPERATION, Set.of(AlertSeverity.DISASTER)).join();
    }

    @Test
    public void update_severityEmpty() {
        var created = randomNotification().toBuilder().setDefaultForSeverity(Set.of(AlertSeverity.DISASTER)).build();
        var dao = getDao(created.getProjectId());
        dao.insert(created, NO_OPERATION).join();
        var updated = created.toBuilder()
                .setName("updated name")
                .setVersion(created.getVersion() + 1)
                .build();
        dao.updateWithValidations(updated, NO_OPERATION, Set.of()).join();

        assertThat(list(created.getProjectId()), hasItem(AlertMatchers.reflectionEqualTo(updated)));
    }

    @Test
    public void findProjectIds() {
        var dao = getDao();
        dao.createSchemaForTests().join();

        assertEquals(Set.of(), dao.findProjects().join());
        for (var index = 0; index < 3; index++) {
            dao.insert(randomNotification().toBuilder().setProjectId("alice").build(), NO_OPERATION).join();
            assertEquals(Set.of("alice"), dao.findProjects().join());
        }

        for (var index = 0; index < 3; index++) {
            dao.insert(randomNotification().toBuilder().setProjectId("bob").build(), NO_OPERATION).join();
            assertEquals(Set.of("alice", "bob"), dao.findProjects().join());
        }
    }

    @Test
    public void insertWithDefault() {
        Notification n1 = randomNotification()
                .toBuilder()
                .setDefaultForProject(true)
                .build();

        Notification n2 = randomNotification()
                .toBuilder()
                .setDefaultForProject(false)
                .build();

        Notification n3 = randomNotification()
                .toBuilder()
                .setDefaultForProject(true)
                .build();

        getDao(n1.getProjectId()).insert(n1, NO_OPERATION).join();

        var list = list(n1.getProjectId());
        assertEquals(1, list.stream().filter(Notification::isDefaultForProject).count());
        assertEquals(n1.getId(), list.stream().filter(Notification::isDefaultForProject).map(Notification::getId).findFirst().get());


        getDao(n1.getProjectId()).insert(n2, NO_OPERATION).join();

        list = list(n1.getProjectId());
        assertEquals(1, list.stream().filter(Notification::isDefaultForProject).count());
        assertEquals(n1.getId(), list.stream().filter(Notification::isDefaultForProject).map(Notification::getId).findFirst().get());

        getDao(n1.getProjectId()).insert(n3, NO_OPERATION).join();

        list = list(n1.getProjectId());
        assertEquals(2, list.stream().filter(Notification::isDefaultForProject).count());
    }

    @Test
    public void updateWithDefault() {
        Notification n1 = randomNotification()
                .toBuilder()
                .setDefaultForProject(true)
                .build();

        Notification n2 = randomNotification()
                .toBuilder()
                .setDefaultForProject(false)
                .build();

        Notification n3 = randomNotification()
                .toBuilder()
                .setDefaultForProject(true)
                .build();

        getDao(n1.getProjectId()).insert(n1, NO_OPERATION).join();

        var list = list(n1.getProjectId());
        assertEquals(1, list.stream().filter(Notification::isDefaultForProject).count());
        assertEquals(n1.getId(), list.stream().filter(Notification::isDefaultForProject).map(Notification::getId).findFirst().get());


        getDao(n1.getProjectId()).insert(n2, NO_OPERATION).join();

        list = list(n1.getProjectId());
        assertEquals(1, list.stream().filter(Notification::isDefaultForProject).count());
        assertEquals(n1.getId(), list.stream().filter(Notification::isDefaultForProject).map(Notification::getId).findFirst().get());

        getDao(n1.getProjectId()).insert(n3, NO_OPERATION).join();

        list = list(n1.getProjectId());
        assertEquals(2, list.stream().filter(Notification::isDefaultForProject).count());
    }

    private List<Notification> list(String projectId) {
        var dao = getDao(projectId);
        return dao.findAll(projectId).join();
    }

    private Notification get(String projectId, String id) {
        return list(projectId).stream()
                .filter(alert -> alert.getId().equals(id))
                .findFirst()
                .get();
    }
}
