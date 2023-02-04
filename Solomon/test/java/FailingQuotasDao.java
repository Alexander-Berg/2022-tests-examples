package ru.yandex.solomon.quotas.watcher;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import ru.yandex.solomon.core.db.dao.QuotasDao;
import ru.yandex.solomon.core.db.model.Quota;

/**
 * @author Ivan Tsybulin
 */
@ParametersAreNonnullByDefault
class FailingQuotasDao implements QuotasDao {

    @Override
    public CompletableFuture<List<Quota>> findAllByNamespace(String namespace) {
        return CompletableFuture.failedFuture(new UnsupportedOperationException());
    }

    @Override
    public CompletableFuture<List<Quota>> findAllIndicators(
            String namespace,
            String scopeType,
            @Nullable String scopeId)
    {
        return CompletableFuture.failedFuture(new UnsupportedOperationException());
    }

    @Override
    public CompletableFuture<Void> upsert(
            String namespace,
            String scopeType,
            @Nullable String scopeId,
            String indicator,
            long newLimit,
            String updatedBy,
            Instant updatedAt)
    {
        return CompletableFuture.failedFuture(new UnsupportedOperationException());
    }

    @Override
    public CompletableFuture<Void> deleteOne(
            String namespace,
            String scopeType,
            @Nullable String scopeId,
            String indicator)
    {
        return CompletableFuture.failedFuture(new UnsupportedOperationException());
    }

    @Override
    public CompletableFuture<Void> delete(String namespace, String scopeType, String indicator) {
        return CompletableFuture.failedFuture(new UnsupportedOperationException());
    }

    @Override
    public CompletableFuture<Void> createSchemaForTests() {
        return CompletableFuture.failedFuture(new UnsupportedOperationException());
    }

    @Override
    public CompletableFuture<Void> dropSchemaForTests() {
        return CompletableFuture.failedFuture(new UnsupportedOperationException());
    }
}
