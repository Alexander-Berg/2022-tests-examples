package ru.yandex.solomon.alert.notification.channel.cloud;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.ParametersAreNonnullByDefault;

import yandex.cloud.auth.api.AsyncCloudAuthClient;
import yandex.cloud.auth.api.Resource;
import yandex.cloud.auth.api.Subject;
import yandex.cloud.auth.api.credentials.AbstractCredentials;
import yandex.cloud.auth.api.exception.CloudAuthInternalException;
import yandex.cloud.auth.api.exception.CloudAuthPermissionDeniedException;
import yandex.cloud.auth.api.exception.CloudAuthUnauthenticatedException;

import ru.yandex.bolts.internal.NotImplementedException;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.failedFuture;
import static java.util.stream.Collectors.toList;

/**
 * @author Ivan Tsybulin
 */
@ParametersAreNonnullByDefault
public class CloudAuthClientStub implements AsyncCloudAuthClient {
    private final Map<String, Map<String, Set<Resource>>> db;
    private boolean enabled;

    public CloudAuthClientStub() {
        db = new HashMap<>();
        enabled = true;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void add(String userId, String permission, Resource... resources) {
        db.computeIfAbsent(userId, ignore -> new HashMap<>())
                .computeIfAbsent(permission, ignore -> new HashSet<>())
                .addAll(Arrays.stream(resources).collect(toList()));
    }

    public void drop(String userId, String permission) {
        var user = db.get(userId);
        if (user != null) {
            user.remove(permission);
        }
    }

    public void drop(String userId) {
        db.remove(userId);
    }

    @Override
    public CompletableFuture<Subject> authenticate(AbstractCredentials credentials) {
        return failedFuture(new UnsupportedOperationException("too abstract"));
    }

    @Override
    public CompletableFuture<Subject> authorize(AbstractCredentials credentials, String permission, Stream<Resource> path) {
        return failedFuture(new UnsupportedOperationException("too abstract"));
    }

    private static class FakeSubject implements Subject.UserAccount {
        private final Subject.UserAccount.Id id;

        private FakeSubject(Subject.UserAccount.Id id) {
            this.id = id;
        }

        @Override
        public Subject.UserAccount.Id toId() {
            return id;
        }

        @Override
        public String getFederationId() {
            return null;
        }
    }

    @Override
    public CompletableFuture<Subject> authorize(Subject.Id subjectId, String permission, Stream<Resource> path) {
        if (subjectId instanceof Subject.UserAccount.Id) {
            Subject.UserAccount.Id userId = (Subject.UserAccount.Id) subjectId;

            return syncAuthorize(userId, permission, path);
        }
        return failedFuture(new NotImplementedException(subjectId.getClass().getName() + " not yet supported"));
    }

    private CompletableFuture<Subject> syncAuthorize(Subject.UserAccount.Id id, String permission, Stream<Resource> path) {
        if (!enabled) {
            return failedFuture(new CloudAuthInternalException(new RuntimeException(), "Service is disabled"));
        }
        var user = db.get(id.getId());
        if (user == null) {
            return failedFuture(new CloudAuthUnauthenticatedException(new RuntimeException(), "Service is disabled"));
        }

        Set<String> available = user.getOrDefault(permission, Set.of()).stream()
            .map(res -> res.getResourceType() + ":" + res.getResourceId())
            .collect(Collectors.toSet());

        Set<String> requested = path
            .map(res -> res.getResourceType() + ":" + res.getResourceId())
            .collect(Collectors.toSet());

        if (available.containsAll(requested)) {
            return completedFuture(new FakeSubject(id));
        } else {
            return failedFuture(new CloudAuthPermissionDeniedException(new RuntimeException(), "Available " + available + ", requested " + requested));
        }
    }
}
