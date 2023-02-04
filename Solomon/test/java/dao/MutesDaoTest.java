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
import ru.yandex.solomon.alert.mute.domain.Mute;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static ru.yandex.solomon.alert.mute.domain.MuteTestSupport.randomMute;
import static ru.yandex.solomon.alert.util.CommonMatchers.reflectionEqualTo;
import static ru.yandex.solomon.idempotency.IdempotentOperation.NO_OPERATION;

/**
 * @author Ivan Tsybulin
 */
public abstract class MutesDaoTest {
    protected abstract EntitiesDao<Mute> getDao();

    private EntitiesDao<Mute> getDao(String projectId) {
        var dao = getDao();
        dao.createSchemaForTests()
            .thenCompose(o -> dao.createSchema(projectId))
            .join();
        return dao;
    }

    @Test
    public void emptyFind() {
        List<Mute> list = list("junk");
        assertThat(list, emptyIterable());
    }

    @Test
    public void insert() {
        Mute entry = randomMute();
        var dao = getDao(entry.getProjectId());
        dao.insert(entry, NO_OPERATION).join();
        List<Mute> list = list(entry.getProjectId());
        assertEquals(1, list.size());
        assertThat(list.get(0), reflectionEqualTo(entry));
    }

    @Test
    public void insertIdempotent() {
        Mute mute = randomMute();
        var dao = getDao(mute.getProjectId());
        var first = dao.insert(mute, NO_OPERATION).join();
        var second = dao.insert(mute, NO_OPERATION).join();
        assertTrue(first.isEmpty());
        assertEquals(mute, second.get());
    }

    @Test
    public void insertConflictIsNop() {
        Mute mute = randomMute();
        var dao = getDao(mute.getProjectId());
        var inserted = dao.insert(mute, NO_OPERATION).join();
        assertTrue(inserted.isEmpty());
        Mute other = mute.toBuilder()
                .setTicketId("other")
                .build();
        var first = dao.insert(other, NO_OPERATION).join();
        var second = dao.insert(other, NO_OPERATION).join();
        assertEquals(mute, first.get());
        assertEquals(mute, second.get());
    }

    @Test
    public void insertMany() throws InterruptedException {
        String projectId = "test";
        List<Mute> source = IntStream.range(0, 1001)
                .mapToObj(index -> randomMute()
                        .toBuilder()
                        .setTicketId(String.valueOf(index))
                        .setProjectId(projectId)
                        .build())
                .sorted(Comparator.comparing(Mute::getTicketId))
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

        List<Mute> list = list(projectId)
                .stream()
                .sorted(Comparator.comparing(Mute::getTicketId))
                .collect(Collectors.toList());

        assertThat(list.size(), equalTo(source.size()));
        for (int index = 0; index < source.size(); index++) {
            Mute expected = source.get(index);
            Mute result = list.get(index);
            assertThat(result, reflectionEqualTo(expected));
        }
    }

    @Test
    public void insertIntoDifferentProjects() {
        Mute alice = randomMute()
                .toBuilder()
                .setProjectId("alice")
                .build();

        Mute bob = randomMute()
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
        Mute alice = randomMute()
            .toBuilder()
            .setProjectId("alice")
            .build();

        Mute bob = randomMute()
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
        var created = randomMute();
        var dao = getDao(created.getProjectId());
        dao.insert(created, NO_OPERATION).join();
        var updated = created.toBuilder()
                .setTicketId("updated name")
                .setVersion(created.getVersion() + 1)
                .build();
        dao.update(updated, NO_OPERATION).join();

        assertThat(list(created.getProjectId()), hasItem(AlertMatchers.reflectionEqualTo(updated)));
    }

    @Test
    public void updateChecksVersion() {
        var created = randomMute();
        var dao = getDao(created.getProjectId());
        var inserted = dao.insert(created, NO_OPERATION).join();
        assertTrue(inserted.isEmpty());
        {
            var prev = dao.update(created.toBuilder()
                    .setTicketId("No way")
                    .build(), NO_OPERATION).join().get();
            assertEquals(created, prev);
            assertEquals(created.getTicketId(), get(created.getProjectId(), created.getId()).getTicketId());
        }
        {
            var prev = dao.update(created.toBuilder()
                    .setTicketId("Modified")
                    .setVersion(created.getVersion() + 1).build(), NO_OPERATION).join().get();
            assertEquals(created, prev);
            assertEquals("Modified", get(created.getProjectId(), created.getId()).getTicketId());
        }
        {
            var prev = dao.update(created.toBuilder()
                    .setTicketId("Modified again")
                    .setVersion(created.getVersion() + 2).build(), NO_OPERATION).join().get();
            assertEquals(created.getVersion() + 1, prev.getVersion());
            assertEquals("Modified again", get(created.getProjectId(), created.getId()).getTicketId());
        }
    }

    @Test
    public void delete() {
        Mute alert = randomMute();
        var dao = getDao(alert.getProjectId());
        dao.insert(alert, NO_OPERATION).join();
        dao.deleteById(alert.getProjectId(), alert.getId(), NO_OPERATION).join();
        List<Mute> list = list(alert.getProjectId());
        assertThat(list, emptyIterable());
    }

    @Test
    public void findProjectIds() {
        var dao = getDao();
        dao.createSchemaForTests().join();

        assertEquals(Set.of(), dao.findProjects().join());
        for (var index = 0; index < 3; index++) {
            dao.insert(randomMute().toBuilder().setProjectId("alice").build(), NO_OPERATION).join();
            assertEquals(Set.of("alice"), dao.findProjects().join());
        }

        for (var index = 0; index < 3; index++) {
            dao.insert(randomMute().toBuilder().setProjectId("bob").build(), NO_OPERATION).join();
            assertEquals(Set.of("alice", "bob"), dao.findProjects().join());
        }
    }

    private List<Mute> list(String projectId) {
        var dao = getDao(projectId);
        return dao.findAll(projectId).join();
    }

    private Mute get(String projectId, String id) {
        return list(projectId).stream()
                .filter(alert -> alert.getId().equals(id))
                .findFirst()
                .get();
    }
}
