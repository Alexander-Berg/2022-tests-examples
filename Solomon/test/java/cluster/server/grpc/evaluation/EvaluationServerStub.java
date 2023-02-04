package ru.yandex.solomon.alert.cluster.server.grpc.evaluation;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcServerRulePublic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.solomon.alert.cluster.server.grpc.StreamObserverStub;
import ru.yandex.solomon.alert.protobuf.EvaluationServerStatusRequest;
import ru.yandex.solomon.alert.protobuf.EvaluationServerStatusResponse;
import ru.yandex.solomon.alert.protobuf.EvaluationStreamClientMessage;
import ru.yandex.solomon.alert.protobuf.EvaluationStreamServerMessage;
import ru.yandex.solomon.alert.protobuf.TAlertingClusterServiceGrpc.TAlertingClusterServiceImplBase;

/**
 * @author Vladimir Gordiychuk
 */
public class EvaluationServerStub extends TAlertingClusterServiceImplBase {
    private static final Logger logger = LoggerFactory.getLogger(EvaluationServerStub.class);
    private final String name;
    public GrpcServerRulePublic server;

    public final ArrayBlockingQueue<ServerStream> inboundStream = new ArrayBlockingQueue<>(100);
    public volatile Status predefinedStatus = Status.OK;
    public volatile CountDownLatch sync = new CountDownLatch(1);

    public EvaluationServerStub(String name) {
        this.name = name;
    }

    void setUp() throws Throwable {
        server = new GrpcServerRulePublic();
        server.before();
        logger.info("{}: address {}", name, server.getServerName());
        server.getServiceRegistry().addService(this);
    }

    void tearDown() {
        predefinedStatus = Status.UNAVAILABLE;
        server.after();
    }

    @Override
    public void evaluationServerStatus(EvaluationServerStatusRequest request, StreamObserver<EvaluationServerStatusResponse> responseObserver) {
        responseObserver.onNext(EvaluationServerStatusResponse.newBuilder()
                .setNode(server.getServerName())
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public StreamObserver<EvaluationStreamClientMessage> evaluationStream(StreamObserver<EvaluationStreamServerMessage> responseObserver) {
        if (!predefinedStatus.isOk()) {
            responseObserver.onError(predefinedStatus.asRuntimeException());
            return new StreamObserverStub<>();
        }

        var stream = new ServerStream(responseObserver);
        inboundStream.add(stream);
        sync.countDown();
        return stream.inbound;
    }

    public static class ServerStream {
        public final StreamObserver<EvaluationStreamServerMessage> outbound;
        public final StreamObserverStub<EvaluationStreamClientMessage> inbound;

        public ServerStream(StreamObserver<EvaluationStreamServerMessage> outbound) {
            this.outbound = outbound;
            this.inbound = new StreamObserverStub<>();
        }
    }
}

