package ru.yandex.solomon.alert.cluster.broker.notification;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import ru.yandex.solomon.alert.api.converters.NotificationConverter;
import ru.yandex.solomon.alert.dao.EntitiesDao;
import ru.yandex.solomon.alert.dao.NotificationsDao;
import ru.yandex.solomon.alert.domain.AlertSeverity;
import ru.yandex.solomon.alert.notification.channel.NotificationChannelStubFactory;
import ru.yandex.solomon.alert.notification.channel.telegram.ChatIdResolverStub;
import ru.yandex.solomon.alert.notification.domain.Notification;
import ru.yandex.solomon.alert.protobuf.notification.TNotification;
import ru.yandex.solomon.flags.FeatureFlagHolderStub;
import ru.yandex.solomon.idempotency.IdempotentOperation;
import ru.yandex.solomon.ut.ManualClock;
import ru.yandex.solomon.ut.ManualScheduledExecutorService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * @author Ivan Tsybulin
 */
@ParametersAreNonnullByDefault
public abstract class DesyncOnRetryTest extends ProjectNotificationServiceTestBase {
    @Rule
    public Timeout timeoutRule = Timeout.builder()
            .withLookingForStuckThread(true)
            .withTimeout(15, TimeUnit.SECONDS)
            .build();

    public void setUp(EntitiesDao<Notification> notificationDao) {
        this.projectId = "junk" + ThreadLocalRandom.current().nextInt(0, 100);
        this.clock = new ManualClock();
        this.notificationsDao = new RetryingDao((NotificationsDao)notificationDao);
        this.flagsHolder = new FeatureFlagHolderStub();
        this.executorService = new ManualScheduledExecutorService(2, clock);
        this.channelFactory = new NotificationChannelStubFactory();
        this.notificationConverter = new NotificationConverter(new ChatIdResolverStub());
        restartService();
    }

    public void tearDown() {
        if (executorService != null) {
            executorService.shutdownNow();
        }
    }

    public static class RetryingDao implements NotificationsDao {
        private final NotificationsDao dao;

        public RetryingDao(NotificationsDao dao) {
            this.dao = dao;
        }

        @Override
        public CompletableFuture<?> createSchemaForTests() {
            return dao.createSchemaForTests();
        }

        @Override
        public CompletableFuture<?> createSchema(String projectId) {
            return dao.createSchema(projectId);
        }

        private <R> CompletableFuture<R> retryAfterTimeout(Supplier<CompletableFuture<R>> action) {
            return action.get()
                    // Simulate applied, but timeout
                    .thenCompose(ignore -> action.get());
        }

        @Override
        public CompletableFuture<Optional<Notification>> insert(Notification entity, IdempotentOperation op) {
            return retryAfterTimeout(() -> dao.insert(entity, op));
        }

        @Override
        public CompletableFuture<Optional<Notification>> update(Notification entity, IdempotentOperation op) {
            return retryAfterTimeout(() -> dao.update(entity, op));
        }

        @Override
        public CompletableFuture<Void> deleteById(String projectId, String id, IdempotentOperation op) {
            return retryAfterTimeout(() -> dao.deleteById(projectId, id, op));
        }

        @Override
        public CompletableFuture<Void> deleteProject(String projectId) {
            return retryAfterTimeout(() -> dao.deleteProject(projectId));
        }

        @Override
        public CompletableFuture<Void> find(String projectId, Consumer<Notification> consumer) {
            return retryAfterTimeout(() -> dao.find(projectId, consumer));
        }

        @Override
        public CompletableFuture<Set<String>> findProjects() {
            return retryAfterTimeout(dao::findProjects);
        }

        @Override
        public CompletableFuture<Void> deleteByIdWithValidations(String projectId, String id, IdempotentOperation op, Set<AlertSeverity> validateSeverities) {
            return retryAfterTimeout(() -> dao.deleteById(projectId, id, op));
        }

        @Override
        public CompletableFuture<Optional<Notification>> updateWithValidations(Notification entity, IdempotentOperation op, Set<AlertSeverity> validateSeverities) {
            return retryAfterTimeout(() -> dao.update(entity, op));
        }
    }

    @Test
    public void failedCreateRetried() {
        TNotification create = successCreate(randomNotification().toBuilder().clearDefaultForAlertSeverity().build());
        TNotification read = successRead(create.getId());
        assertThat(create, equalTo(read));
    }

    @Test
    public void failedUpdateRetried() {
        TNotification create = successCreate(randomNotification().toBuilder().clearDefaultForAlertSeverity().build());
        TNotification read = successRead(create.getId());
        assertThat(create, equalTo(read));
        TNotification updated = successUpdate(read.toBuilder()
                .setName("Other name")
                .build());
        assertThat(updated.getName(), equalTo("Other name"));
        try {
            successUpdate(read.toBuilder()
                    .setName("Yet another name")
                    .build());
        } catch (AssertionError e) {
            return;
        }
        Assert.fail("Last update must not be successful");
    }
}
