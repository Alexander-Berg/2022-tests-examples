package ru.yandex.solomon.alert.cluster.broker.alert;

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

import ru.yandex.solomon.alert.api.converters.AlertConverter;
import ru.yandex.solomon.alert.cluster.broker.alert.activity.ActivityFactory;
import ru.yandex.solomon.alert.cluster.broker.alert.activity.SimpleActivitiesFactory;
import ru.yandex.solomon.alert.cluster.broker.alert.activity.TemplateActivityFactory;
import ru.yandex.solomon.alert.cluster.broker.alert.activity.TemplateAlertFactory;
import ru.yandex.solomon.alert.cluster.broker.evaluation.EvaluationAssignmentServiceStub;
import ru.yandex.solomon.alert.cluster.broker.notification.StatefulNotificationChannelFactoryStub;
import ru.yandex.solomon.alert.cluster.project.ProjectAssignment;
import ru.yandex.solomon.alert.dao.EntitiesDao;
import ru.yandex.solomon.alert.dao.memory.InMemoryAlertStatesDao;
import ru.yandex.solomon.alert.dao.memory.InMemoryAlertTemplateDao;
import ru.yandex.solomon.alert.domain.Alert;
import ru.yandex.solomon.alert.protobuf.ERequestStatusCode;
import ru.yandex.solomon.alert.protobuf.TAlert;
import ru.yandex.solomon.alert.protobuf.TCreateAlertRequest;
import ru.yandex.solomon.alert.protobuf.TCreateAlertResponse;
import ru.yandex.solomon.alert.protobuf.TDeleteAlertRequest;
import ru.yandex.solomon.alert.protobuf.TReadAlertRequest;
import ru.yandex.solomon.alert.protobuf.TReadAlertResponse;
import ru.yandex.solomon.alert.protobuf.TUpdateAlertRequest;
import ru.yandex.solomon.alert.protobuf.TUpdateAlertResponse;
import ru.yandex.solomon.alert.template.MustacheTemplateFactory;
import ru.yandex.solomon.alert.unroll.UnrollExecutorStub;
import ru.yandex.solomon.balancer.AssignmentSeqNo;
import ru.yandex.solomon.core.container.ContainerType;
import ru.yandex.solomon.core.db.dao.memory.InMemoryQuotasDao;
import ru.yandex.solomon.idempotency.IdempotentOperation;
import ru.yandex.solomon.idempotency.dao.IdempotentOperationDao;
import ru.yandex.solomon.quotas.watcher.QuotaWatcher;
import ru.yandex.solomon.ut.ManualClock;
import ru.yandex.solomon.ut.ManualScheduledExecutorService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Ivan Tsybulin
 */
@ParametersAreNonnullByDefault
public abstract class DesyncOnRetryTest extends ProjectAlertServiceTestBase {
    @Rule
    public Timeout timeoutRule = Timeout.builder()
            .withLookingForStuckThread(true)
            .withTimeout(15, TimeUnit.SECONDS)
            .build();

    public void setUp(EntitiesDao<Alert> alertDao, IdempotentOperationDao operationsDao) {
        this.projectId = "junk" + ThreadLocalRandom.current().nextInt(0, 100);
        AssignmentSeqNo seqNo = new AssignmentSeqNo(ThreadLocalRandom.current().nextInt(10, 10000), ThreadLocalRandom.current().nextInt(10, 100));
        this.assignment = new ProjectAssignment(projectId, "localhost", seqNo);
        this.clock = new ManualClock();
        this.executorService = new ManualScheduledExecutorService(2, clock);
        this.alertDao = new RetryingDao<>(alertDao);
        this.statesDao = new InMemoryAlertStatesDao();
        this.channelStub = new StatefulNotificationChannelFactoryStub(executorService, projectId);
        this.assignmentService = new EvaluationAssignmentServiceStub(clock, executorService);
        this.unrollExecutor = new UnrollExecutorStub(executorService);
        createAndRunMuteService();
        this.simpleActivitiesFactory = new SimpleActivitiesFactory( assignment,
                unrollExecutor,
                assignmentService,
                channelStub,
                muteService);
        this.templateActivityFactory = new TemplateActivityFactory(
                alertTemplateDao = new InMemoryAlertTemplateDao(true),
                templateAlertFactory = new TemplateAlertFactory(new MustacheTemplateFactory()),
                simpleActivitiesFactory);
        this.activityFactory = new ActivityFactory(
                simpleActivitiesFactory,
                templateActivityFactory);
        this.quotasDao = new InMemoryQuotasDao();
        quotasDao.createSchemaForTests();
        quotaWatcher = new QuotaWatcher(null, quotasDao, "alerting", executorService);
        idempotentDao = operationsDao;
        restartProjectAssignment();
    }

    public void tearDown() {
        if (executorService != null) {
            executorService.shutdownNow();
        }
    }

    public static class RetryingDao<T> implements EntitiesDao<T> {
        private final EntitiesDao<T> dao;

        public RetryingDao(EntitiesDao<T> dao) {
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
        public CompletableFuture<Optional<T>> insert(T entity, IdempotentOperation op) {
            return retryAfterTimeout(() -> dao.insert(entity, op));
        }

        @Override
        public CompletableFuture<Optional<T>> update(T entity, IdempotentOperation op) {
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
        public CompletableFuture<Void> find(String projectId, Consumer<T> consumer) {
            return retryAfterTimeout(() -> dao.find(projectId, consumer));
        }

        @Override
        public CompletableFuture<Set<String>> findProjects() {
            return retryAfterTimeout(dao::findProjects);
        }
    }

    @Test
    public void failedCreateRetried() {
        TAlert create = successCreate(randomAlert());
        TAlert read = successRead(create.getId());
        assertThat(create, equalTo(read));
    }

    @Test
    public void failedCreateRetried_idempotent() {
        var alert = randomAlert();
        TCreateAlertResponse response = service.createAlert(TCreateAlertRequest.newBuilder()
                .setAlert(alert)
                .setIdempotentOperationId("1")
                .build())
                .join();
        assertThat(response.getStatusMessage(), response.getRequestStatus(), equalTo(ERequestStatusCode.OK));

        response = service.createAlert(TCreateAlertRequest.newBuilder()
                .setAlert(alert)
                .setIdempotentOperationId("1")
                .build())
                .join();
        assertThat(response.getStatusMessage(), response.getRequestStatus(), equalTo(ERequestStatusCode.OK));

        TAlert read = successRead(response.getAlert().getId());
        assertTrue(AlertConverter.protoToAlert(alert).equalContent(AlertConverter.protoToAlert(read)));

        var op = idempotentDao.get("1", read.getProjectId(), ContainerType.PROJECT, AlertingIdempotency.CREATE_OPERATION_TYPE).join();
        assertTrue(op.isPresent());
        assertEquals(read.getId(), op.get().entityId());
    }

    @Test
    public void failedUpdateRetried() {
        TAlert create = successCreate(randomAlert());
        TAlert read = successRead(create.getId());
        assertThat(create, equalTo(read));
        TAlert updated = successUpdate(read.toBuilder()
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

    @Test
    public void failedUpdateRetried_idempotent() {
        TAlert create = successCreate(randomAlert());
        TAlert read = successRead(create.getId());
        assertThat(create, equalTo(read));

        TUpdateAlertResponse response = service.updateAlert(TUpdateAlertRequest.newBuilder()
                .setAlert(read.toBuilder()
                        .setName("Other name")
                        .build())
                .setIdempotentOperationId("op1")
                .build())
                .join();
        assertThat(response.getStatusMessage(), response.getRequestStatus(), equalTo(ERequestStatusCode.OK));
        assertThat(response.getAlert().getName(), equalTo("Other name"));

        response = service.updateAlert(TUpdateAlertRequest.newBuilder()
                .setAlert(response.getAlert().toBuilder()
                        .setName("Another name")
                        .build())
                .setIdempotentOperationId("op1")
                .build())
                .join();
        assertThat(response.getStatusMessage(), response.getRequestStatus(), equalTo(ERequestStatusCode.OK));
        assertThat(response.getAlert().getName(), equalTo("Other name"));

        response = service.updateAlert(TUpdateAlertRequest.newBuilder()
                .setAlert(response.getAlert().toBuilder()
                        .setName("Another name")
                        .setVersion(-1)
                        .build())
                .setIdempotentOperationId("op2")
                .build())
                .join();
        assertThat(response.getStatusMessage(), response.getRequestStatus(), equalTo(ERequestStatusCode.OK));
        assertThat(response.getAlert().getName(), equalTo("Another name"));

        var op = idempotentDao.get("op1", read.getProjectId(), ContainerType.PROJECT, AlertingIdempotency.UPDATE_OPERATION_TYPE).join();
        assertTrue(op.isPresent());
        assertEquals(read.getId(), op.get().entityId());

        var op2 = idempotentDao.get("op2", read.getProjectId(), ContainerType.PROJECT, AlertingIdempotency.UPDATE_OPERATION_TYPE).join();
        assertTrue(op2.isPresent());
        assertEquals(read.getId(), op2.get().entityId());
    }

    @Test
    public void failedDeleteRetried() {
        TAlert create = successCreate(randomAlert());
        TAlert read = successRead(create.getId());
        assertThat(create, equalTo(read));

        successDelete(read.getId());

        TReadAlertResponse response = service.readAlert(TReadAlertRequest.newBuilder()
                .setAlertId(read.getId())
                .setProjectId(projectId)
                .build())
                .join();
        assertThat(response.getStatusMessage(), response.getRequestStatus(), equalTo(ERequestStatusCode.NOT_FOUND));
    }

    @Test
    public void failedDeleteRetried_idempotent() {
        TAlert create = successCreate(randomAlert());
        TAlert read = successRead(create.getId());
        assertThat(create, equalTo(read));

        var responseDelete = service.deleteAlert(TDeleteAlertRequest.newBuilder()
                .setAlertId(read.getId())
                .setProjectId(read.getProjectId())
                .setIdempotentOperationId("op1")
                .build())
                .join();
        assertThat(responseDelete.getStatusMessage(), responseDelete.getRequestStatus(), equalTo(ERequestStatusCode.OK));
        responseDelete = service.deleteAlert(TDeleteAlertRequest.newBuilder()
                .setAlertId(read.getId())
                .setProjectId(read.getProjectId())
                .setIdempotentOperationId("op1")
                .build())
                .join();
        assertThat(responseDelete.getStatusMessage(), responseDelete.getRequestStatus(), equalTo(ERequestStatusCode.OK));

        TReadAlertResponse responseRead = service.readAlert(TReadAlertRequest.newBuilder()
                .setAlertId(read.getId())
                .setProjectId(projectId)
                .build())
                .join();
        assertThat(responseRead.getStatusMessage(), responseRead.getRequestStatus(), equalTo(ERequestStatusCode.NOT_FOUND));

        var op = idempotentDao.get("op1", read.getProjectId(), ContainerType.PROJECT, AlertingIdempotency.DELETE_OPERATION_TYPE).join();
        assertTrue(op.isPresent());
        assertEquals(read.getId(), op.get().entityId());
    }
}
