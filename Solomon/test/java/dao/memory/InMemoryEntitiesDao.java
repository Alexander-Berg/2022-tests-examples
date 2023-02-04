package ru.yandex.solomon.alert.dao.memory;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import ru.yandex.solomon.alert.dao.EntitiesDao;
import ru.yandex.solomon.alert.dao.Entity;
import ru.yandex.solomon.alert.dao.codec.AbstractCodec;
import ru.yandex.solomon.alert.dao.codec.AbstractRecord;
import ru.yandex.solomon.core.container.ContainerType;
import ru.yandex.solomon.idempotency.IdempotentOperation;
import ru.yandex.solomon.idempotency.IdempotentOperationExistException;
import ru.yandex.solomon.idempotency.dao.IdempotentOperationDao;

/**
 * @author Vladimir Gordiychuk
 */
public class InMemoryEntitiesDao<T extends Entity, U extends AbstractRecord> implements EntitiesDao<T>, IdempotentOperationDao {
    private final ConcurrentMap<Key, U> recordById = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, IdempotentOperation> operations = new ConcurrentHashMap<>();
    private final AbstractCodec<T, U> codec;
    private volatile Throwable throwable;

    public InMemoryEntitiesDao(AbstractCodec<T, U> codec) {
        this.codec = codec;
    }

    public void setError(@Nullable Throwable throwable) {
        this.throwable = throwable;
    }

    public CompletableFuture<Void> createSchemaForTests() {
        return CompletableFuture.supplyAsync(() -> {
            throwIfRequired();
            return null;
        });
    }

    @Override
    public CompletableFuture<Void> dropSchemaForTests() {
        return CompletableFuture.supplyAsync(() -> {
            throwIfRequired();
            return null;
        });
    }

    @Override
    public CompletableFuture<?> createSchema(String projectId) {
        return CompletableFuture.supplyAsync(() -> {
            throwIfRequired();
            return null;
        });
    }

    public CompletableFuture<Optional<T>> insert(T entity, IdempotentOperation operation) {
        return CompletableFuture.supplyAsync(() -> {
            throwIfRequired();
            var opKey = operation.id() + operation.containerId() + operation.containerType() + operation.operationType();
            if (operation != IdempotentOperation.NO_OPERATION && operations.containsKey(opKey)) {
                throw new IdempotentOperationExistException();
            }
            U record = codec.encode(entity);
            Key key = new Key(record.projectId, record.id);
            U prev = recordById.putIfAbsent(key, record);
            if (operation != IdempotentOperation.NO_OPERATION) {
                operations.put(opKey, operation);
            }

            return Optional.ofNullable(prev).map(codec::decode);
        });
    }

    public CompletableFuture<Optional<T>> update(T entity, IdempotentOperation operation) {
        return CompletableFuture.supplyAsync(() -> {
            throwIfRequired();
            var opKey = operation.id() + operation.containerId() + operation.containerType() + operation.operationType();
            if (operation != IdempotentOperation.NO_OPERATION && operations.containsKey(opKey)) {
                throw new IdempotentOperationExistException();
            }
            U record = codec.encode(entity);
            Key key = new Key(record.projectId, record.id);
            synchronized (recordById) { // ¯\_(ツ)_/¯
                U prev = recordById.get(key);
                if (prev == null) {
                    return Optional.empty();
                }
                if (prev.version == record.version - 1) {
                    recordById.put(key, record);
                }
                if (operation != IdempotentOperation.NO_OPERATION) {
                    operations.put(opKey, operation);
                }
                return Optional.of(prev).map(codec::decode);
            }
        });
    }

    @Override
    public CompletableFuture<Void> deleteById(String projectId, String id, IdempotentOperation operation) {
        return CompletableFuture.runAsync(() -> {
            throwIfRequired();
            var opKey = operation.id() + operation.containerId() + operation.containerType() + operation.operationType();
            if (operation != IdempotentOperation.NO_OPERATION && operations.containsKey(opKey)) {
                throw new IdempotentOperationExistException();
            }
            recordById.remove(new Key(projectId, id));
            if (operation != IdempotentOperation.NO_OPERATION) {
                operations.put(opKey, operation);
            }
        });
    }

    @Override
    public CompletableFuture<Void> deleteProject(String projectId) {
        return CompletableFuture.runAsync(() -> {
            throwIfRequired();
            recordById.keySet().removeIf(key -> key.projectId.equals(projectId));
        });
    }

    @Override
    public CompletableFuture<Void> find(String projectId, Consumer<T> consumer) {
        return CompletableFuture.runAsync(() -> {
            throwIfRequired();
            recordById.values()
                    .stream()
                    .filter(t -> projectId.equals(t.projectId))
                    .forEach(record -> consumer.accept(codec.decode(record)));
        });
    }

    @Override
    public CompletableFuture<Set<String>> findProjects() {
        return CompletableFuture.supplyAsync(() -> {
            throwIfRequired();
            return recordById.keySet()
                    .stream()
                    .map(key -> key.projectId)
                    .collect(Collectors.toSet());
        });
    }

    private void throwIfRequired() {
        var copy = throwable;
        if (copy != null) {
            throw new RuntimeException(throwable);
        }
    }

    @Override
    public CompletableFuture<Optional<IdempotentOperation>> get(String id, String containerId, ContainerType containerType, String operationType) {
        return CompletableFuture.supplyAsync(() -> {
            var opKey = id + containerId + containerType + operationType;
            return Optional.ofNullable(operations.get(opKey));
        });
    }

    @Override
    public CompletableFuture<Boolean> complete(IdempotentOperation operation) {
        return CompletableFuture.supplyAsync(() -> {
            var opKey = operation.id() + operation.containerId() + operation.containerType() + operation.operationType();
            if (operations.containsKey(opKey)) {
                return false;
            }
            operations.put(opKey, operation);
            return true;
        });
    }

    @Override
    public CompletableFuture<Boolean> deleteOne(IdempotentOperation operation) {
        var opKey = operation.id() + operation.containerId() + operation.containerType() + operation.operationType();
        return CompletableFuture.supplyAsync(() -> operations.remove(opKey) != null);
    }

    private static class Key {
        private final String projectId;
        private final String id;

        private Key(String projectId, String id) {
            this.projectId = projectId;
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Key key = (Key) o;

            if (!projectId.equals(key.projectId)) return false;
            return id.equals(key.id);
        }

        @Override
        public int hashCode() {
            int result = projectId.hashCode();
            result = 31 * result + id.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "Key{" + projectId + ", " + id + '}';
        }
    }
}
