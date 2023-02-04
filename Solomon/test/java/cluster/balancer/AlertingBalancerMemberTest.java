package ru.yandex.solomon.alert.cluster.balancer;

import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import io.grpc.Status;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.solomon.alert.protobuf.THeartbeatRequest;
import ru.yandex.solomon.alert.protobuf.THeartbeatResponse;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Vladimir Gordiychuk
 */
public class AlertingBalancerMemberTest {

    private StockpileNode alice;
    private StockpileNode bob;

    private volatile String leader;
    private AlertingBalancerClientStub balancerClient;

    @Before
    public void setUp() {
        alice = new StockpileNode("alice");
        bob = new StockpileNode("bob");

        balancerClient = new AlertingBalancerClientStub();
        balancerClient.register(alice, bob);
    }

    @Test
    public void abortHeartbeatWhenLeaderIsUndefined() {
        var member = new AlertingBalancerMember(null, balancerClient);
        Status status = member.receiveHeartbeat(THeartbeatRequest.newBuilder()
            .setNode("solomon-test-01.yandex-team.net")
            .build())
            .thenApply(ignore -> Status.OK)
            .exceptionally(Status::fromThrowable)
            .join();

        assertEquals(Status.Code.ABORTED, status.getCode());
    }

    @Test
    public void forwardHeartbeatToLeader() {
        leader = alice.name;
        var member = new AlertingBalancerMember(alice.name, balancerClient);
        THeartbeatRequest heartbeat = THeartbeatRequest.newBuilder()
            .setNode("solomon-test-01.yandex-team.net")
            .build();

        for (int index = 0; index < 3; index++) {
            assertNotNull(member.receiveHeartbeat(heartbeat).join());
        }

        assertEquals(3, alice.getCounter(heartbeat.getNode()).get());
        assertEquals(0, bob.getCounter(heartbeat.getNode()).get());
    }

    @Test
    public void forwardToActualLeader() {
        THeartbeatRequest heartbeat = THeartbeatRequest.newBuilder()
            .setNode("solomon-test-01.yandex-team.net")
            .build();

        leader = alice.name;
        var member = new AlertingBalancerMember(alice.name, balancerClient);
        for (int index = 0; index < 5; index++) {
            if (index == 2) {
                leader = bob.name;
                member = new AlertingBalancerMember(bob.name, balancerClient);
            }

            assertNotNull(member.receiveHeartbeat(heartbeat).join());
        }

        assertEquals(2, alice.getCounter(heartbeat.getNode()).get());
        assertEquals(3, bob.getCounter(heartbeat.getNode()).get());
    }

    private class StockpileNode extends AbstractAlertingNode {
        private final ConcurrentMap<String, AtomicInteger> heartbeatCount = new ConcurrentHashMap<>();

        public StockpileNode(String name) {
            super(name);
        }

        @Override
        public CompletableFuture<THeartbeatResponse> heartbeat(THeartbeatRequest request) {
            return CompletableFuture.supplyAsync(() -> {
                if (!name.equals(leader)) {
                    throw new IllegalStateException("I'm not a leader");
                }

                String node = Objects.requireNonNull(request.getNode());
                getCounter(node).incrementAndGet();

                return THeartbeatResponse.newBuilder()
                    .setLeaderSeqNo(42)
                    .setGlobalProjectSeqNo(55)
                    .setExpiredAt(Instant.now().plusSeconds(60).toEpochMilli())
                    .build();
            });
        }

        public AtomicInteger getCounter(String node) {
            return heartbeatCount.computeIfAbsent(node, ignore -> new AtomicInteger());
        }
    }
}
