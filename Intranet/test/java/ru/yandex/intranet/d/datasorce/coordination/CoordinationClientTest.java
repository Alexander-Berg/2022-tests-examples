package ru.yandex.intranet.d.datasorce.coordination;

import java.io.IOException;
import java.time.Duration;

import com.yandex.ydb.core.Status;
import com.yandex.ydb.core.rpc.OutStreamObserver;
import com.yandex.ydb.core.rpc.StreamObserver;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import reactor.core.publisher.Mono;

import ru.yandex.intranet.d.IntegrationTest;
import ru.yandex.intranet.d.datasource.coordination.CoordinationClient;
import ru.yandex.intranet.d.datasource.coordination.model.AlterCoordinationNodeRequest;
import ru.yandex.intranet.d.datasource.coordination.model.CoordinationNode;
import ru.yandex.intranet.d.datasource.coordination.model.CoordinationNodeDescription;
import ru.yandex.intranet.d.datasource.coordination.model.CreateCoordinationNodeRequest;
import ru.yandex.intranet.d.datasource.coordination.model.DescribeCoordinationNodeRequest;
import ru.yandex.intranet.d.datasource.coordination.model.DropCoordinationNodeRequest;
import ru.yandex.intranet.d.datasource.coordination.model.TargetNodeConfig;
import ru.yandex.intranet.d.datasource.coordination.model.session.AcquireSemaphorePendingResponse;
import ru.yandex.intranet.d.datasource.coordination.model.session.AcquireSemaphoreResultResponse;
import ru.yandex.intranet.d.datasource.coordination.model.session.CoordinationSessionRequest;
import ru.yandex.intranet.d.datasource.coordination.model.session.CoordinationSessionResponse;
import ru.yandex.intranet.d.datasource.coordination.model.session.CreateSemaphoreResultResponse;
import ru.yandex.intranet.d.datasource.coordination.model.session.DeleteSemaphoreResultResponse;
import ru.yandex.intranet.d.datasource.coordination.model.session.DescribeSemaphoreChangedResponse;
import ru.yandex.intranet.d.datasource.coordination.model.session.DescribeSemaphoreResultResponse;
import ru.yandex.intranet.d.datasource.coordination.model.session.FailureResponse;
import ru.yandex.intranet.d.datasource.coordination.model.session.PingPongRequest;
import ru.yandex.intranet.d.datasource.coordination.model.session.PingPongResponse;
import ru.yandex.intranet.d.datasource.coordination.model.session.ReleaseSemaphoreResultResponse;
import ru.yandex.intranet.d.datasource.coordination.model.session.SessionOperationParameters;
import ru.yandex.intranet.d.datasource.coordination.model.session.SessionStartRequest;
import ru.yandex.intranet.d.datasource.coordination.model.session.SessionStartedResponse;
import ru.yandex.intranet.d.datasource.coordination.model.session.SessionStopRequest;
import ru.yandex.intranet.d.datasource.coordination.model.session.SessionStoppedResponse;
import ru.yandex.intranet.d.datasource.coordination.model.session.UpdateSemaphoreResultResponse;
import ru.yandex.intranet.d.datasource.coordination.rpc.CoordinationRpc;
import ru.yandex.intranet.d.datasource.coordination.rpc.grpc.GrpcCoordinationRpc;
import ru.yandex.intranet.d.datasource.impl.YdbRpcTransport;
import ru.yandex.intranet.d.util.Barrier;

/**
 * Coordination client test.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
@IntegrationTest
public class CoordinationClientTest {

    private static final Logger LOG = LoggerFactory.getLogger(CoordinationClientTest.class);

    @Autowired
    private YdbRpcTransport ydbRpcTransport;
    @Value("${ydb.database}")
    private String database;

    @Test
    public void testCreateAndDrop() throws IOException {
        try (CoordinationRpc rpc = GrpcCoordinationRpc.useTransport(ydbRpcTransport.getRpcTransport())) {
            try (CoordinationClient client = CoordinationClient.newClient(rpc).build()) {
                Mono.usingWhen(client.createNode(CreateCoordinationNodeRequest.builder(database + "/test").build()),
                        n -> Mono.empty(),
                        n -> client.dropNode(DropCoordinationNodeRequest.builder(n.getPath()).builder())
                ).block();
            }
        }
    }

    @Test
    public void testDescribe() throws IOException {
        try (CoordinationRpc rpc = GrpcCoordinationRpc.useTransport(ydbRpcTransport.getRpcTransport())) {
            try (CoordinationClient client = CoordinationClient.newClient(rpc).build()) {
                CoordinationNodeDescription description =
                        Mono.usingWhen(client.createNode(CreateCoordinationNodeRequest
                                        .builder(database + "/test").build()),
                        n -> client.describeNode(DescribeCoordinationNodeRequest.builder(n.getPath()).builder()),
                        n -> client.dropNode(DropCoordinationNodeRequest.builder(n.getPath()).builder())
                ).block();
                Assertions.assertNotNull(description);
                Assertions.assertEquals("test", description.getSelf().getName());
            }
        }
    }

    @Test
    public void testAlter() throws IOException {
        try (CoordinationRpc rpc = GrpcCoordinationRpc.useTransport(ydbRpcTransport.getRpcTransport())) {
            try (CoordinationClient client = CoordinationClient.newClient(rpc).build()) {
                CoordinationNodeDescription description =
                        Mono.usingWhen(client.createNode(CreateCoordinationNodeRequest
                                        .builder(database + "/test").build()),
                                n -> client.alterNode(AlterCoordinationNodeRequest.builder(n.getPath())
                                        .configuration(TargetNodeConfig.builder()
                                                .selfCheckPeriod(Duration.ofSeconds(2))
                                                .sessionGracePeriod(Duration.ofSeconds(15))
                                                .build())
                                        .build()).then(
                                        client.describeNode(DescribeCoordinationNodeRequest
                                                .builder(n.getPath()).builder())),
                                n -> client.dropNode(DropCoordinationNodeRequest.builder(n.getPath()).builder())
                        ).block();
                Assertions.assertNotNull(description);
                Assertions.assertEquals("test", description.getSelf().getName());
                Assertions.assertEquals(Duration.ofSeconds(2), description.getConfiguration().getSelfCheckPeriod());
                Assertions.assertEquals(Duration.ofSeconds(15),
                        description.getConfiguration().getSessionGracePeriod());
            }
        }
    }

    @Test
    public void testSession() throws IOException, InterruptedException {
        try (CoordinationRpc rpc = GrpcCoordinationRpc.useTransport(ydbRpcTransport.getRpcTransport())) {
            try (CoordinationClient client = CoordinationClient.newClient(rpc).build()) {
                LOG.info("Creating coordination node...");
                CoordinationNode node = client.createNode(CreateCoordinationNodeRequest
                        .builder(database + "/test").build()).block();
                Assertions.assertNotNull(node);
                try {
                    LOG.info("Coordination node created");
                    Barrier startBarrier = new Barrier();
                    startBarrier.close();
                    Barrier stopBarrier = new Barrier();
                    stopBarrier.close();
                    SessionOperationParameters params = SessionOperationParameters.builder().build();
                    var sessionObserver = new StreamObserver<CoordinationSessionResponse>() {
                        volatile OutStreamObserver<CoordinationSessionRequest> requestObserver;

                        @Override
                        public void onNext(CoordinationSessionResponse value) {
                            value.match(new CoordinationSessionResponse.Cases<Void>() {
                                @Override
                                public Void ping(PingPongResponse response) {
                                    LOG.info("Coordination ping received {}", response);
                                    if (requestObserver != null) {
                                        requestObserver.onNext(CoordinationSessionRequest
                                                .pong(new PingPongRequest(response.getOpaque())));
                                    }
                                    return null;
                                }

                                @Override
                                public Void pong(PingPongResponse response) {
                                    return null;
                                }

                                @Override
                                public Void failure(FailureResponse response) {
                                    LOG.error("Coordination failure received {}", response);
                                    if (requestObserver != null) {
                                        requestObserver.onNext(CoordinationSessionRequest
                                                .sessionStop(new SessionStopRequest()));
                                    }
                                    return null;
                                }

                                @Override
                                public Void sessionStarted(SessionStartedResponse response) {
                                    LOG.info("Coordination session started {}", response);
                                    startBarrier.open();
                                    return null;
                                }

                                @Override
                                public Void sessionStopped(SessionStoppedResponse response) {
                                    LOG.info("Coordination session stopped {}", response);
                                    stopBarrier.open();
                                    return null;
                                }

                                @Override
                                public Void acquireSemaphorePending(AcquireSemaphorePendingResponse response) {
                                    return null;
                                }

                                @Override
                                public Void acquireSemaphoreResult(AcquireSemaphoreResultResponse response) {
                                    return null;
                                }

                                @Override
                                public Void releaseSemaphoreResult(ReleaseSemaphoreResultResponse response) {
                                    return null;
                                }

                                @Override
                                public Void describeSemaphoreResult(DescribeSemaphoreResultResponse response) {
                                    return null;
                                }

                                @Override
                                public Void describeSemaphoreChanged(DescribeSemaphoreChangedResponse response) {
                                    return null;
                                }

                                @Override
                                public Void createSemaphoreResult(CreateSemaphoreResultResponse response) {
                                    return null;
                                }

                                @Override
                                public Void updateSemaphoreResult(UpdateSemaphoreResultResponse response) {
                                    return null;
                                }

                                @Override
                                public Void deleteSemaphoreResult(DeleteSemaphoreResultResponse response) {
                                    return null;
                                }
                            });
                        }

                        @Override
                        public void onError(Status status) {
                            LOG.error("Coordination session error: {}", status);
                        }

                        @Override
                        public void onCompleted() {
                            LOG.info("Coordination session subscription is complete");
                        }
                    };
                    OutStreamObserver<CoordinationSessionRequest> requestObserver = client.session(params,
                            sessionObserver);
                    sessionObserver.requestObserver = requestObserver;
                    LOG.info("Starting coordination session sequence...");
                    requestObserver.onNext(CoordinationSessionRequest.sessionStart(
                            new SessionStartRequest(node.getPath(),
                                    0L, Duration.ofSeconds(10), "Test session", 1L,
                                    new byte[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15})));
                    LOG.info("Session start scheduled...");
                    LOG.info("Waiting for session start...");
                    startBarrier.passThrough();
                    LOG.info("Session started");
                    LOG.info("Stopping session");
                    requestObserver.onNext(CoordinationSessionRequest.sessionStop(new SessionStopRequest()));
                    LOG.info("Waiting for session stop...");
                    stopBarrier.passThrough();
                    LOG.info("Session stopped");
                    requestObserver.onCompleted();
                    LOG.info("Coordination session sequence is complete");
                } finally {
                    LOG.info("Dropping coordination node...");
                    client.dropNode(DropCoordinationNodeRequest.builder(node.getPath()).builder()).block();
                    LOG.info("Coordination node was dropped");
                }
            }
        }
    }

}
