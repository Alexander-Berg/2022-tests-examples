package ru.yandex.solomon.alert.dao.memory;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.GuardedBy;

import ru.yandex.solomon.alert.dao.EvaluationLogsDao;
import ru.yandex.solomon.alert.dao.ProjectEvaluationLogsDao;
import ru.yandex.solomon.alert.rule.EvaluationState;

/**
 * @author Vladimir Gordiychuk
 */
public class InMemoryEvaluationLogsDao implements EvaluationLogsDao {
    private ConcurrentMap<String, Project> byProjectId = new ConcurrentHashMap<>();

    @Override
    public ProjectEvaluationLogsDao forProject(String projectId) {
        return byProjectId.computeIfAbsent(projectId, Project::new);
    }

    private static class Project implements ProjectEvaluationLogsDao {
        private final String projectId;
        @GuardedBy("rwLock")
        private final TreeMap<Key, EvaluationState> map = new TreeMap<>();
        private ReadWriteLock rwLock = new ReentrantReadWriteLock();

        public Project(String projectId) {
            this.projectId = projectId;
        }

        @Override
        public CompletableFuture<Void> save(EvaluationState state) {
            return CompletableFuture.supplyAsync(() -> {
                rwLock.writeLock().lock();
                try {
                    map.put(new Key(state), state);
                } finally {
                    rwLock.writeLock().unlock();
                }
                return null;
            });
        }

        @Override
        public CompletableFuture<Void> save(List<EvaluationState> states) {
            return CompletableFuture.supplyAsync(() -> {
                rwLock.writeLock().lock();
                try {
                    for (EvaluationState state : states) {
                        map.put(new Key(state), state);
                    }
                } finally {
                    rwLock.writeLock().unlock();
                }
                return null;
            });
        }

        @Override
        public CompletableFuture<Void> delete(FilterOpts filter) {
            return findMany(filter)
                    .thenApply(records -> records.stream()
                            .map(Key::new)
                            .collect(Collectors.toList()))
                    .thenAccept(keys -> {
                        rwLock.writeLock().lock();
                        try {
                            keys.forEach(map::remove);
                        } finally {
                            rwLock.writeLock().unlock();
                        }
                    });
        }

        @Override
        public CompletableFuture<Optional<EvaluationState>> findOne(long tsMilli, String parentId, String alertId) {
            return CompletableFuture.supplyAsync(() -> {
                rwLock.readLock().lock();
                try {
                    return Optional.ofNullable(map.get(new Key(tsMilli, parentId, alertId)));
                } finally {
                    rwLock.readLock().unlock();
                }
            });
        }

        @Override
        public CompletableFuture<Void> findMany(FilterOpts filter, Consumer<EvaluationState> consumer) {
            return CompletableFuture.supplyAsync(() -> {
                rwLock.readLock().lock();
                try {
                    int processed = 0;
                    for (Map.Entry<Key, EvaluationState> entry : map.entrySet()) {
                        if (filter.fromMillis != 0 && filter.fromMillis > entry.getKey().tsMillis) {
                            continue;
                        }

                        if (filter.toMillis != 0 && filter.toMillis <= entry.getKey().tsMillis) {
                            return null;
                        }

                        if (filter.parentId != null && !filter.parentId.equals(entry.getKey().parentId)) {
                            continue;
                        }

                        if (filter.alertId != null && !filter.alertId.equals(entry.getKey().alertId)) {
                            continue;
                        }

                        if (filter.limit != 0 && filter.limit <= processed++) {
                            return null;
                        }

                        consumer.accept(entry.getValue());
                    }

                    return null;
                } finally {
                    rwLock.readLock().unlock();
                }
            });
        }

        @Override
        public CompletableFuture<?> createSchemaForTests() {
            return CompletableFuture.completedFuture(null);
        }
    }

    @ParametersAreNonnullByDefault
    private static class Key implements Comparable<Key> {
        private final long tsMillis;
        private final String parentId;
        private final String alertId;

        public Key(EvaluationState state) {
            tsMillis = state.getLatestEvalTruncated().toEpochMilli();
            parentId = state.getKey().getParentId();
            alertId = state.getKey().getAlertId();
        }

        public Key(long tsMillis, String parentId, String alertId) {
            this.tsMillis = tsMillis;
            this.parentId = parentId;
            this.alertId = alertId;
        }

        @Override
        public int compareTo(Key right) {
            int compare = Long.compare(tsMillis, right.tsMillis);
            if (compare != 0) {
                return compare;
            }

            compare = parentId.compareTo(right.parentId);
            if (compare != 0) {
                return compare;
            }

            return alertId.compareTo(right.alertId);
        }
    }
}
