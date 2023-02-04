package ru.yandex.infra.stage.podspecs;

import java.util.concurrent.CompletableFuture;

import ru.yandex.bolts.collection.Try;

class DummyReleaseGetter implements SandboxReleaseGetter {
    private volatile Try<ResourceWithMeta> stored;

    void setResource(ResourceWithMeta resource) {
        stored = Try.success(resource);
    }

    void setError(Exception error) {
        stored = Try.failure(error);
    }

    @Override
    public CompletableFuture<ResourceWithMeta> getLatestRelease(String resourceType, boolean useChecksum) {
        return stored
                .toCompletedFuture();
    }

    @Override
    public CompletableFuture<Void> validateSandboxResource(long resourceId, String resourceUrl) {
        return Try.success().toCompletedFuture();
    }

    @Override
    public CompletableFuture<ResourceWithMeta> getReleaseByResourceId(long resourceId, boolean useChecksum) {
        return stored.toCompletedFuture();
    }

}
