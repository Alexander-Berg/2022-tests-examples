package ru.yandex.solomon.alert.dao.memory;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;

import javax.annotation.ParametersAreNonnullByDefault;

import ru.yandex.solomon.alert.dao.AlertStatesDao;
import ru.yandex.solomon.alert.protobuf.TPersistAlertState;
import ru.yandex.solomon.alert.protobuf.TPersistStateChunk;
import ru.yandex.solomon.balancer.AssignmentSeqNo;

/**
 * @author Vladimir Gordiychuk
 */
public class InMemoryAlertStatesDao implements AlertStatesDao {
    private final ConcurrentMap<String, Snapshot> daoByProject = new ConcurrentHashMap<>();

    @Override
    public CompletableFuture<?> createSchema(String projectId) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<?> save(String projectId, Instant time, AssignmentSeqNo seqNo, List<TPersistAlertState> states) {
        return CompletableFuture.supplyAsync(() -> {
            return daoByProject.compute(projectId, (id, snapshot) -> {
                Snapshot next = new Snapshot(time, seqNo, TPersistStateChunk.newBuilder()
                    .addAllStates(states)
                    .build());

                if (snapshot == null) {
                    return next;
                }

                if (snapshot.compareTo(next) >= 0) {
                    return snapshot;
                }

                return next;
            });
        });
    }

    @Override
    public CompletableFuture<Void> find(String projectId, Consumer<TPersistAlertState> consumer) {
        return CompletableFuture.runAsync(() -> {
            daoByProject.getOrDefault(projectId, Snapshot.EMPTY)
                .state.getStatesList()
                .forEach(consumer);
        });
    }

    @Override
    public CompletableFuture<?> deleteProject(String projectId) {
        return CompletableFuture.runAsync(() -> {
            daoByProject.remove(projectId);
        });
    }

    @Override
    public CompletableFuture<?> createSchemaForTests() {
        return CompletableFuture.completedFuture(null);
    }

    @ParametersAreNonnullByDefault
    private static class Snapshot implements Comparable<Snapshot> {
        private static final Snapshot EMPTY = new Snapshot(Instant.EPOCH, AssignmentSeqNo.EMPTY, TPersistStateChunk.getDefaultInstance());

        private final Instant time;
        private final AssignmentSeqNo seqNo;
        private final TPersistStateChunk state;

        public Snapshot(Instant time, AssignmentSeqNo seqNo, TPersistStateChunk state) {
            this.time = time;
            this.seqNo = seqNo;
            this.state = state;
        }

        @Override
        public int compareTo(Snapshot snapshot) {
            int compare = seqNo.compareTo(snapshot.seqNo);
            if (compare == 0) {
                compare = time.compareTo(snapshot.time);
            }

            return compare;
        }
    }
}
