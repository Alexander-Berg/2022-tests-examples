package ru.yandex.solomon.alert.cluster.server.grpc.evaluation;

import java.util.concurrent.ThreadLocalRandom;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.junit.Test;

import ru.yandex.solomon.alert.cluster.server.grpc.StreamObserverStub;
import ru.yandex.solomon.alert.protobuf.EvaluationAssignmentKey;
import ru.yandex.solomon.alert.protobuf.EvaluationStreamClientMessage;
import ru.yandex.solomon.alert.protobuf.EvaluationStreamClientMessage.UnassignEvaluation;
import ru.yandex.solomon.alert.protobuf.TAssignmentSeqNo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Vladimir Gordiychuk
 */
public class EvaluationStreamClientMessageWriterTest {
    private static final int MAX_BATCH_SIZE = 100;

    @Test
    public void closedAfterOnError() {
        var output = new StreamObserverStub<EvaluationStreamClientMessage>();
        var writer = writer(output);

        var status = Status.INTERNAL;
        writer.onError(status.asRuntimeException());

        assertTrue(writer.isClosed());
        assertEquals(status, output.done.join());
    }

    @Test
    public void closedAfterOnComplete() {
        var output = new StreamObserverStub<EvaluationStreamClientMessage>();
        var writer = writer(output);
        writer.onCompleted();

        assertTrue(writer.isClosed());
        assertEquals(Status.OK, output.done.join());
    }

    @Test(expected = IllegalStateException.class)
    public void alreadyClosedOnError() {
        var output = new StreamObserverStub<EvaluationStreamClientMessage>();

        var writer = writer(output);
        writer.onCompleted();
        writer.onError(Status.INTERNAL.asRuntimeException());
    }

    @Test(expected = IllegalStateException.class)
    public void alreadyClosedOnComplete() {
        var output = new StreamObserverStub<EvaluationStreamClientMessage>();

        var writer = writer(output);
        writer.onError(Status.INTERNAL.asRuntimeException());
        writer.onCompleted();
    }

    @Test(expected = IllegalStateException.class)
    public void alreadyClosedOnNext() {
        var output = new StreamObserverStub<EvaluationStreamClientMessage>();

        var writer = writer(output);
        writer.onError(Status.INTERNAL.asRuntimeException());

        writer.onNext(nextUnassign());
        writer.flush();
    }

    @Test
    public void closeWhenOnNextThrowException() {
        var output = new StreamObserverStub<EvaluationStreamClientMessage>() {
            @Override
            public void onNext(EvaluationStreamClientMessage item) {
                throw Status.ABORTED.asRuntimeException();
            }
        };

        var writer = writer(output);

        try {
            writer.onNext(nextUnassign());
            writer.flush();
            fail("message not deliver to stream");
        } catch (Throwable e) {
            e.printStackTrace();
            assertEquals(Status.ABORTED, Status.fromThrowable(e));
        }

        assertTrue(writer.isClosed());
        assertEquals(Status.ABORTED, output.done.join());
    }

    @Test
    public void saveOriginalExceptionThrowByOnNext() {
        var output = new StreamObserverStub<EvaluationStreamClientMessage>() {
            @Override
            public void onNext(EvaluationStreamClientMessage item) {
                throw Status.ABORTED.asRuntimeException();
            }

            @Override
            public void onError(Throwable t) {
                throw Status.UNAVAILABLE.asRuntimeException();
            }
        };

        var writer = writer(output);

        try {
            writer.onNext(nextUnassign());
            writer.flush();
            fail("message not deliver to stream");
        } catch (Throwable e) {
            e.printStackTrace();
            assertEquals(Status.ABORTED, Status.fromThrowable(e));
        }

        assertTrue(writer.isClosed());
    }

    @Test
    public void flushBatch() {
        var one = nextUnassign();
        var two = nextUnassign();

        var output = new StreamObserverStub<EvaluationStreamClientMessage>();
        var writer = writer(output);

        writer.onNext(one);
        writer.onNext(two);
        writer.flush();

        var expected = EvaluationStreamClientMessage.newBuilder()
                .addUnassignEvaluations(one)
                .addUnassignEvaluations(two)
                .build();

        assertEquals(expected, output.takeEvent());
    }

    @Test
    public void flushSendMessageOnlyOnce() {
        var output = new StreamObserverStub<EvaluationStreamClientMessage>();
        var writer = writer(output);

        writer.onNext(nextUnassign());
        writer.flush();
        writer.flush();
        writer.flush();

        assertNotNull(output.takeEvent());
        assertEquals(0, output.events.size());
    }

    @Test
    public void splitMessagesBySize() {
        var unassign = nextUnassign();

        var output = new StreamObserverStub<EvaluationStreamClientMessage>();
        var writer = writer(output);

        for (int index = 0; index < 100; index++) {
            writer.onNext(unassign);
        }

        assertNotEquals(0, output.events.size());
        int[] sizes = output.events.stream()
                .mapToInt(EvaluationStreamClientMessage::getSerializedSize)
                .distinct()
                .toArray();

        assertEquals(1, sizes.length);
        assertEquals(MAX_BATCH_SIZE, sizes[0], unassign.getSerializedSize());
    }

    private UnassignEvaluation nextUnassign() {
        return UnassignEvaluation.newBuilder()
                .setAssignKey(EvaluationAssignmentKey.newBuilder()
                        .setAssignId(ThreadLocalRandom.current().nextLong())
                        .setAssignGroupId(TAssignmentSeqNo.newBuilder()
                                .setProjectSeqNo(ThreadLocalRandom.current().nextLong())
                                .setLeaderSeqNo(ThreadLocalRandom.current().nextLong())
                                .build())
                        .build())
                .build();
    }

    private EvaluationStreamClientMessageWriter writer(StreamObserver<EvaluationStreamClientMessage> output) {
        return new EvaluationStreamClientMessageWriter(output, MAX_BATCH_SIZE);
    }
}
