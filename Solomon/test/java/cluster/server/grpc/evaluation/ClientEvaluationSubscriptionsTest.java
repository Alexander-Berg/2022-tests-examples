package ru.yandex.solomon.alert.cluster.server.grpc.evaluation;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.ThreadLocalRandom;

import io.grpc.Status;
import io.grpc.Status.Code;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.solomon.alert.protobuf.EvaluationAssignmentKey;
import ru.yandex.solomon.alert.protobuf.EvaluationStreamServerMessage.Evaluation;
import ru.yandex.solomon.alert.protobuf.TAssignmentSeqNo;
import ru.yandex.solomon.alert.protobuf.TEvaluationState;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static ru.yandex.solomon.alert.cluster.server.grpc.GrpcEvaluationStreamConverter.toError;

/**
 * @author Vladimir Gordiychuk
 */
public class ClientEvaluationSubscriptionsTest {

    private ArrayBlockingQueue<EvaluationAssignmentKey> cancelQueue;
    private ClientEvaluationSubscriptions subscriptions;

    @Before
    public void setUp() throws Exception {
        cancelQueue = new ArrayBlockingQueue<>(100);
        subscriptions = new ClientEvaluationSubscriptions(cancelQueue::add);
    }

    @Test
    public void empty() {
        assertTrue(subscriptions.isEmpty());
        assertEquals(0, subscriptions.size());
    }

    @Test
    public void subscribe() {
        var groupId = nextSeqNo();

        var subscriber = nextSubscriber();
        subscribe(groupId, subscriber);

        assertNoActivity(subscriber);
    }

    @Test
    public void subscribeSameGroup() {
        var groupId = nextSeqNo();

        var alice = nextSubscriber();
        var aliceKey = subscribe(groupId, alice);

        var bob = nextSubscriber();
        var bobKey = subscribe(groupId, bob);

        assertNotEquals(aliceKey, bobKey);

        assertNoActivity(alice);
        assertNoActivity(bob);
    }

    @Test
    public void subscribeDifferentGroup() {
        var alice = nextSubscriber();
        var aliceKey = subscribe(nextSeqNo(), alice);

        var bob = nextSubscriber();
        var bobKey = subscribe(nextSeqNo(), bob);

        assertNotEquals(aliceKey, bobKey);
        assertNotEquals(aliceKey.getAssignId(), bobKey.getAssignId());

        assertNoActivity(alice);
        assertNoActivity(bob);
    }

    @Test
    public void evaluationNotExistGroup() {
        var groupId = nextSeqNo();

        var alice = nextSubscriber();
        var aliceKey = subscribe(groupId, alice);

        var unknownKey = aliceKey.toBuilder()
                .setAssignGroupId(nextSeqNo())
                .build();

        assertFalse(subscriptions.nextEvaluation(nextEvaluation(unknownKey)));
        assertEquals(1, subscriptions.size());
        assertNoActivity(alice);
    }

    @Test
    public void evaluationNoExistAssignId() {
        var groupId = nextSeqNo();

        var alice = nextSubscriber();
        var aliceKey = subscribe(groupId, alice);

        var unknownKey = aliceKey.toBuilder()
                .setAssignId(aliceKey.getAssignId() + 1)
                .build();

        assertFalse(subscriptions.nextEvaluation(nextEvaluation(unknownKey)));
        assertEquals(1, subscriptions.size());
        assertNoActivity(alice);
    }

    @Test
    public void evaluation() {
        var groupId = nextSeqNo();

        var alice = nextSubscriber();
        var aliceKey = subscribe(groupId, alice);
        var aliceEvaluation = nextEvaluation(aliceKey);

        var bob = nextSubscriber();
        var bobKey = subscribe(groupId, bob);
        var bobEvaluation = nextEvaluation(bobKey);

        assertNoActivity(alice);
        assertTrue(subscriptions.nextEvaluation(aliceEvaluation));
        assertEquals(aliceEvaluation, alice.takeEvent());
        assertFalse(alice.doneFuture.isDone());

        assertNoActivity(bob);
        assertTrue(subscriptions.nextEvaluation(bobEvaluation));
        assertEquals(bobEvaluation, bob.takeEvent());
        assertFalse(bob.doneFuture.isDone());

        alice.expectNoEvents();
        bob.expectNoEvents();
        assertEquals(2, subscriptions.size());
    }

    @Test
    public void evaluationErrorNotExistGroup() {
        var groupId = nextSeqNo();

        var alice = nextSubscriber();
        var aliceKey = subscribe(groupId, alice);

        var unknownKey = aliceKey.toBuilder()
                .setAssignGroupId(nextSeqNo())
                .build();

        assertFalse(subscriptions.nextEvaluationError(toError(unknownKey, Status.INTERNAL)));
        assertEquals(1, subscriptions.size());
        assertNoActivity(alice);
    }

    @Test
    public void evaluationErrorNoExistAssignId() {
        var groupId = nextSeqNo();

        var alice = nextSubscriber();
        var aliceKey = subscribe(groupId, alice);

        var unknownKey = aliceKey.toBuilder()
                .setAssignId(aliceKey.getAssignId() + 1)
                .build();

        assertFalse(subscriptions.nextEvaluationError(toError(unknownKey, Status.INTERNAL)));
        assertEquals(1, subscriptions.size());
        assertNoActivity(alice);
    }

    @Test
    public void evaluationError() {
        var groupId = nextSeqNo();

        var alice = nextSubscriber();
        var aliceKey = subscribe(groupId, alice);
        var aliceStatus = Status.RESOURCE_EXHAUSTED;

        var bob = nextSubscriber();
        var bobKey = subscribe(groupId, bob);
        var bobStatus = Status.UNAVAILABLE;

        assertEquals(2, subscriptions.size());

        assertNoActivity(alice);
        assertTrue(subscriptions.nextEvaluationError(toError(aliceKey, aliceStatus)));
        assertEquals(aliceStatus, alice.doneFuture.join());
        assertEquals(1, subscriptions.size());

        assertNoActivity(bob);
        assertTrue(subscriptions.nextEvaluationError(toError(bobKey, bobStatus)));
        assertEquals(bobStatus, bob.doneFuture.join());
        assertEquals(0, subscriptions.size());

        alice.expectNoEvents();
        bob.expectNoEvents();
    }

    @Test
    public void evaluationErrorGroup() {
        var groupId = nextSeqNo();

        var alice = nextSubscriber();
        var aliceKey = subscribe(groupId, alice);

        var bob = nextSubscriber();
        var bobKey = subscribe(groupId, bob);

        var eva = nextSubscriber();
        var evaKey = subscribe(nextSeqNo(), eva);

        var expectStatus = Status.ABORTED;

        assertEquals(3, subscriptions.size());

        assertNoActivity(alice);
        assertTrue(subscriptions.nextEvaluationError(toError(groupId, expectStatus)));
        assertEquals(expectStatus, alice.doneFuture.join());
        assertEquals(expectStatus, bob.doneFuture.join());
        assertEquals(1, subscriptions.size());

        assertNoActivity(eva);
        var evaEvaluation = nextEvaluation(evaKey);
        assertTrue(subscriptions.nextEvaluation(evaEvaluation));
        assertEquals(evaEvaluation, eva.takeEvent());
    }

    @Test
    public void cancel() throws InterruptedException {
        var groupId = nextSeqNo();

        var alice = nextSubscriber();
        var aliceKey = subscribe(groupId, alice);

        var bob = nextSubscriber();
        var bobKey = subscribe(groupId, bob);

        alice.subscription.cancel();
        assertEquals(aliceKey, cancelQueue.take());

        bob.subscription.cancel();
        assertEquals(bobKey, cancelQueue.take());

        assertEquals(2, subscriptions.size());
    }

    @Test
    public void cancelNoNextEvaluation() throws InterruptedException {
        var groupId = nextSeqNo();

        var alice = nextSubscriber();
        var aliceKey = subscribe(groupId, alice);

        alice.subscription.cancel();
        assertEquals(aliceKey, cancelQueue.take());

        assertFalse(subscriptions.nextEvaluation(nextEvaluation(aliceKey)));
        assertNoActivity(alice);
        assertEquals(0, subscriptions.size());
    }

    @Test
    public void cancelNoNextEvaluationError() throws InterruptedException {
        var groupId = nextSeqNo();

        var alice = nextSubscriber();
        var aliceKey = subscribe(groupId, alice);

        alice.subscription.cancel();
        assertEquals(aliceKey, cancelQueue.take());

        assertFalse(subscriptions.nextEvaluationError(toError(aliceKey, Status.INTERNAL)));
        assertNoActivity(alice);
        assertEquals(0, subscriptions.size());
    }

    @Test
    public void cancelNoNextComplete() throws InterruptedException {
        var groupId = nextSeqNo();

        var alice = nextSubscriber();
        var aliceKey = subscribe(groupId, alice);

        alice.subscription.cancel();
        assertEquals(aliceKey, cancelQueue.take());

        assertTrue(subscriptions.remove(aliceKey));
        assertNoActivity(alice);
        assertEquals(0, subscriptions.size());
    }

    @Test
    public void removeCompleteSubscription() {
        var groupId = nextSeqNo();

        var alice = nextSubscriber();
        var aliceKey = subscribe(groupId, alice);

        assertTrue(subscriptions.remove(aliceKey));
        assertEquals(Status.OK, alice.doneFuture.join());
    }

    @Test
    public void closeCompleteSubscriptions() {
        var groupId = nextSeqNo();

        var alice = nextSubscriber();
        subscribe(groupId, alice);

        var bob = nextSubscriber();
        subscribe(groupId, bob);

        var eva = nextSubscriber();
        subscribe(nextSeqNo(), eva);

        subscriptions.close();
        assertTrue(subscriptions.isEmpty());
        assertEquals(Status.OK, alice.doneFuture.join());
        assertEquals(Status.OK, bob.doneFuture.join());
        assertEquals(Status.OK, eva.doneFuture.join());
        assertEquals(0, subscriptions.size());
    }

    @Test
    public void onNextEvaluationException() {
        var groupId = nextSeqNo();

        var alice = new SubscriberStub<Evaluation>() {
            @Override
            public void onNext(Evaluation item) {
                throw Status.INTERNAL.asRuntimeException();
            }
        };
        var aliceKey = subscribe(groupId, alice);

        assertFalse(subscriptions.nextEvaluation(nextEvaluation(aliceKey)));
        assertEquals(Code.INTERNAL, alice.doneFuture.join().getCode());
        assertTrue(subscriptions.isEmpty());
        assertEquals(0, subscriptions.size());
    }

    @Test
    public void onNextEvaluationErrorException() {
        var groupId = nextSeqNo();

        var alice = new SubscriberStub<Evaluation>() {
            @Override
            public void onError(Throwable throwable) {
                throw Status.INTERNAL.asRuntimeException();
            }
        };
        var aliceKey = subscribe(groupId, alice);

        assertTrue(subscriptions.nextEvaluationError(toError(aliceKey, Status.ABORTED)));
        assertNoActivity(alice);
        assertTrue(subscriptions.isEmpty());
        assertEquals(0, subscriptions.size());
    }

    @Test
    public void onCompleteException() {
        var groupId = nextSeqNo();

        var alice = new SubscriberStub<Evaluation>() {
            @Override
            public void onComplete() {
                throw Status.INTERNAL.asRuntimeException();
            }
        };
        var aliceKey = subscribe(groupId, alice);

        assertTrue(subscriptions.remove(aliceKey));
        assertNoActivity(alice);
        assertTrue(subscriptions.isEmpty());
        assertEquals(0, subscriptions.size());
    }

    @Test
    public void onSubscribeException() {
        var groupId = nextSeqNo();

        var alice = new SubscriberStub<Evaluation>() {
            @Override
            public void onSubscribe(Subscription subscription) {
                throw Status.INTERNAL.asRuntimeException();
            }
        };
        var key = subscriptions.subscribe(groupId, alice);
        assertNull(key);
        assertEquals(0, subscriptions.size());

        assertTrue(subscriptions.isEmpty());
    }

    private void assertNoActivity(SubscriberStub<Evaluation> subscriber) {
        subscriber.expectNoEvents();
        assertFalse(subscriber.doneFuture.isDone());
    }

    private EvaluationAssignmentKey subscribe(TAssignmentSeqNo groupId, Subscriber<Evaluation> subscriber) {
        int prevSize = subscriptions.size();
        var key = subscriptions.subscribe(groupId, subscriber);
        assertNotNull(key);
        assertEquals(groupId, key.getAssignGroupId());
        assertNotEquals(0, key.getAssignId());
        assertEquals(prevSize + 1, subscriptions.size());
        return key;
    }

    private Evaluation nextEvaluation(EvaluationAssignmentKey key) {
        return Evaluation.newBuilder()
                .setAssignmentKey(key)
                .setState(TEvaluationState.newBuilder()
                        .setAlertId("alertId_" + key.getAssignId())
                        .setProjectId("project_id_" + key.getAssignGroupId().getProjectSeqNo() + "/" + key.getAssignGroupId().getLeaderSeqNo())
                        .setLatestEvalMillis(System.currentTimeMillis())
                        .setSinceMillis(System.currentTimeMillis() - ThreadLocalRandom.current().nextInt(10000))
                        .build())
                .build();
    }

    private SubscriberStub<Evaluation> nextSubscriber() {
        return new SubscriberStub<>();
    }

    private TAssignmentSeqNo nextSeqNo() {
        return TAssignmentSeqNo.newBuilder()
                .setLeaderSeqNo(ThreadLocalRandom.current().nextLong())
                .setProjectSeqNo(ThreadLocalRandom.current().nextLong())
                .build();
    }
}
