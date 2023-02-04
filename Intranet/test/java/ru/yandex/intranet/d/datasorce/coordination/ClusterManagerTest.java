package ru.yandex.intranet.d.datasorce.coordination;

import java.io.IOException;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import ru.yandex.intranet.d.IntegrationTest;
import ru.yandex.intranet.d.datasource.coordination.ClusterManager;
import ru.yandex.intranet.d.datasource.coordination.CoordinationClient;
import ru.yandex.intranet.d.datasource.coordination.Coordinator;
import ru.yandex.intranet.d.datasource.coordination.model.cluster.NodeInfo;
import ru.yandex.intranet.d.datasource.coordination.model.cluster.NodeLeadershipStatus;
import ru.yandex.intranet.d.datasource.coordination.rpc.CoordinationRpc;
import ru.yandex.intranet.d.datasource.coordination.rpc.grpc.GrpcCoordinationRpc;
import ru.yandex.intranet.d.datasource.impl.YdbRpcTransport;
import ru.yandex.intranet.d.util.Barrier;

/**
 * Cluster manager test.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
@IntegrationTest
public class ClusterManagerTest {

    private static final Logger LOG = LoggerFactory.getLogger(ClusterManagerTest.class);

    @Autowired
    private YdbRpcTransport ydbRpcTransport;
    @Value("${ydb.database}")
    private String database;

    @Test
    public void testStartStop() throws IOException, InterruptedException {
        try (CoordinationRpc rpc = GrpcCoordinationRpc.useTransport(ydbRpcTransport.getRpcTransport())) {
            try (CoordinationClient client = CoordinationClient.newClient(rpc).build()) {
                LOG.info("Creating new cluster manager...");
                ClusterManager clusterManager = ClusterManager.newClusterManager("leader-election-test",
                        "membership-discovery-test",
                        Coordinator.newCoordinator(client, database + "/coordination-test-node"))
                        .build();
                Barrier leaderElectedBarrier = new Barrier();
                leaderElectedBarrier.close();
                AtomicLong leaderElected = new AtomicLong(0L);
                Barrier leaderStepDownBarrier = new Barrier();
                leaderStepDownBarrier.close();
                AtomicLong leaderStepDown = new AtomicLong(0L);
                Barrier leaderNodeElectedBarrier = new Barrier();
                leaderNodeElectedBarrier.close();
                AtomicLong leaderNodeElected = new AtomicLong(0L);
                Barrier leaderNodeStepDownBarrier = new Barrier();
                leaderNodeStepDownBarrier.close();
                AtomicLong leaderNodeStepDown = new AtomicLong(0L);
                Barrier membershipAddedBarrier = new Barrier();
                membershipAddedBarrier.close();
                AtomicLong membershipAdded = new AtomicLong(0L);
                Barrier membershipRemovedBarrier = new Barrier();
                membershipRemovedBarrier.close();
                AtomicLong membershipRemoved = new AtomicLong(0L);
                LOG.info("Subscribing to leader...");
                clusterManager.addLeaderSubscriber(clusterLeader -> {
                    LOG.info("Leader event: {}", clusterLeader);
                    if (clusterLeader.isConnected() && clusterLeader.getLeader().isPresent()) {
                        leaderNodeElected.incrementAndGet();
                        leaderNodeElectedBarrier.open();
                    }
                    if (!clusterLeader.isConnected() && clusterLeader.getLeader().isEmpty()
                            && leaderNodeElected.get() >= 1L) {
                        leaderNodeStepDown.incrementAndGet();
                        leaderNodeStepDownBarrier.open();
                    }
                });
                LOG.info("Subscribed to leader");
                LOG.info("Subscribing to leadership...");
                clusterManager.addLeadershipSubscriber(status -> {
                            LOG.info("Leadership event: {}", status);
                            if (status == NodeLeadershipStatus.LEADER) {
                                leaderElected.incrementAndGet();
                                leaderElectedBarrier.open();
                            }
                            if (status == NodeLeadershipStatus.UNDEFINED && leaderElected.get() >= 1L) {
                                leaderStepDown.incrementAndGet();
                                leaderStepDownBarrier.open();
                            }
                        });
                LOG.info("Subscribed to leadership");
                LOG.info("Subscribing to members...");
                clusterManager.addMembershipSubscriber(members -> {
                    LOG.info("Members event: {}", members);
                    if (members.isConnected() && members.getClusterMembers().size() == 1) {
                        membershipAdded.incrementAndGet();
                        membershipAddedBarrier.open();
                    }
                    if (!members.isConnected() && members.getClusterMembers().isEmpty()
                            && membershipAdded.get() >= 1L) {
                        membershipRemoved.incrementAndGet();
                        membershipRemovedBarrier.open();
                    }
                });
                LOG.info("Subscribed to members");
                LOG.info("Cluster manager created, starting cluster manager...");
                clusterManager.start();
                LOG.info("Cluster manager started");
                LOG.info("Waiting for leader election...");
                leaderElectedBarrier.passThrough();
                LOG.info("Leader elected");
                LOG.info("Waiting for leader node election...");
                leaderNodeElectedBarrier.passThrough();
                LOG.info("Leader node elected");
                LOG.info("Waiting for members...");
                membershipAddedBarrier.passThrough();
                LOG.info("Members registered");
                Assertions.assertEquals(1L, leaderElected.get());
                Assertions.assertEquals(1L, leaderNodeElected.get());
                Assertions.assertEquals(1L, membershipAdded.get());
                LOG.info("Stopping cluster manager...");
                Barrier barrier = new Barrier();
                barrier.close();
                clusterManager.stop(barrier::open);
                LOG.info("Waiting for leader step down...");
                leaderStepDownBarrier.passThrough();
                LOG.info("Leader step down");
                LOG.info("Waiting for leader node step down...");
                leaderNodeStepDownBarrier.passThrough();
                LOG.info("Leader node step down");
                LOG.info("Waiting for members removal...");
                membershipRemovedBarrier.passThrough();
                LOG.info("Members removed");
                Assertions.assertEquals(1L, leaderStepDown.get());
                Assertions.assertEquals(1L, leaderNodeStepDown.get());
                Assertions.assertEquals(1L, membershipRemoved.get());
                LOG.info("Waiting until cluster manager is stopped...");
                barrier.passThrough();
                LOG.info("Cluster manager is stopped");
            }
        }
    }

    @Test
    public void testStartStopChecks() throws IOException, InterruptedException {
        try (CoordinationRpc rpc = GrpcCoordinationRpc.useTransport(ydbRpcTransport.getRpcTransport())) {
            try (CoordinationClient client = CoordinationClient.newClient(rpc).build()) {
                LOG.info("Creating new cluster manager...");
                ClusterManager clusterManager = ClusterManager.newClusterManager("leader-election-test",
                        "membership-discovery-test",
                        Coordinator.newCoordinator(client, database + "/coordination-test-node"))
                        .build();
                Barrier leaderElectedBarrier = new Barrier();
                leaderElectedBarrier.close();
                AtomicLong leaderElected = new AtomicLong(0L);
                Barrier leaderStepDownBarrier = new Barrier();
                leaderStepDownBarrier.close();
                AtomicLong leaderStepDown = new AtomicLong(0L);
                Barrier leaderNodeElectedBarrier = new Barrier();
                leaderNodeElectedBarrier.close();
                AtomicLong leaderNodeElected = new AtomicLong(0L);
                Barrier leaderNodeStepDownBarrier = new Barrier();
                leaderNodeStepDownBarrier.close();
                AtomicLong leaderNodeStepDown = new AtomicLong(0L);
                Barrier membershipAddedBarrier = new Barrier();
                membershipAddedBarrier.close();
                AtomicLong membershipAdded = new AtomicLong(0L);
                Barrier membershipRemovedBarrier = new Barrier();
                membershipRemovedBarrier.close();
                AtomicLong membershipRemoved = new AtomicLong(0L);
                LOG.info("Subscribing to leader...");
                clusterManager.addLeaderSubscriber(clusterLeader -> {
                    LOG.info("Leader event: {}", clusterLeader);
                    if (clusterLeader.isConnected() && clusterLeader.getLeader().isPresent()) {
                        leaderNodeElected.incrementAndGet();
                        leaderNodeElectedBarrier.open();
                    }
                    if (!clusterLeader.isConnected() && clusterLeader.getLeader().isEmpty()
                            && leaderNodeElected.get() >= 1L) {
                        leaderNodeStepDown.incrementAndGet();
                        leaderNodeStepDownBarrier.open();
                    }
                });
                LOG.info("Subscribed to leader");
                LOG.info("Subscribing to leadership...");
                clusterManager.addLeadershipSubscriber(status -> {
                    LOG.info("Leadership event: {}", status);
                    if (status == NodeLeadershipStatus.LEADER) {
                        leaderElected.incrementAndGet();
                        leaderElectedBarrier.open();
                    }
                    if (status == NodeLeadershipStatus.UNDEFINED && leaderElected.get() >= 1L) {
                        leaderStepDown.incrementAndGet();
                        leaderStepDownBarrier.open();
                    }
                });
                LOG.info("Subscribed to leadership");
                LOG.info("Subscribing to members...");
                clusterManager.addMembershipSubscriber(members -> {
                    LOG.info("Members event: {}", members);
                    if (members.isConnected() && members.getClusterMembers().size() == 1) {
                        membershipAdded.incrementAndGet();
                        membershipAddedBarrier.open();
                    }
                    if (!members.isConnected() && members.getClusterMembers().isEmpty()
                            && membershipAdded.get() >= 1L) {
                        membershipRemoved.incrementAndGet();
                        membershipRemovedBarrier.open();
                    }
                });
                LOG.info("Subscribed to members");
                LOG.info("Cluster manager created, starting cluster manager...");
                clusterManager.start();
                LOG.info("Cluster manager started");
                LOG.info("Waiting for leader election...");
                leaderElectedBarrier.passThrough();
                LOG.info("Leader elected");
                LOG.info("Waiting for leader node election...");
                leaderNodeElectedBarrier.passThrough();
                LOG.info("Leader node elected");
                LOG.info("Waiting for members...");
                membershipAddedBarrier.passThrough();
                LOG.info("Members registered");
                Assertions.assertEquals(1L, leaderElected.get());
                Assertions.assertEquals(1L, leaderNodeElected.get());
                Assertions.assertEquals(1L, membershipAdded.get());
                Boolean isLeader = clusterManager.isLeader().block();
                Assertions.assertNotNull(isLeader);
                Assertions.assertTrue(isLeader);
                Optional<Boolean> isLeaderCached = clusterManager.isLeaderCached();
                Assertions.assertTrue(isLeaderCached.isPresent());
                Assertions.assertTrue(isLeaderCached.get());
                Optional<NodeInfo> leader = clusterManager.getLeader().block();
                Assertions.assertNotNull(leader);
                Assertions.assertTrue(leader.isPresent());
                Assertions.assertEquals(clusterManager.getNodeId(), leader.get().getUuid());
                Optional<NodeInfo> leaderCached = clusterManager.getLeaderCached();
                Assertions.assertTrue(leaderCached.isPresent());
                Assertions.assertEquals(clusterManager.getNodeId(), leaderCached.get().getUuid());
                Set<NodeInfo> members = clusterManager.getMembers().block();
                Assertions.assertNotNull(members);
                Assertions.assertEquals(1, members.size());
                Optional<Set<NodeInfo>> membersCached = clusterManager.getMembersCached();
                Assertions.assertTrue(membersCached.isPresent());
                Assertions.assertEquals(1, membersCached.get().size());
                Set<NodeInfo> membersLast = clusterManager.getLastSeenClusterMembers();
                Assertions.assertEquals(1, membersLast.size());
                LOG.info("Stopping cluster manager...");
                Barrier barrier = new Barrier();
                barrier.close();
                clusterManager.stop(barrier::open);
                LOG.info("Waiting for leader step down...");
                leaderStepDownBarrier.passThrough();
                LOG.info("Leader step down");
                LOG.info("Waiting for leader node step down...");
                leaderNodeStepDownBarrier.passThrough();
                LOG.info("Leader node step down");
                LOG.info("Waiting for members removal...");
                membershipRemovedBarrier.passThrough();
                LOG.info("Members removed");
                Assertions.assertEquals(1L, leaderStepDown.get());
                Assertions.assertEquals(1L, leaderNodeStepDown.get());
                Assertions.assertEquals(1L, membershipRemoved.get());
                LOG.info("Waiting until cluster manager is stopped...");
                barrier.passThrough();
                LOG.info("Cluster manager is stopped");
            }
        }
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void testStartStopTwoManagers() throws IOException, InterruptedException {
        try (CoordinationRpc rpc = GrpcCoordinationRpc.useTransport(ydbRpcTransport.getRpcTransport())) {
            try (CoordinationClient client = CoordinationClient.newClient(rpc).build()) {
                LOG.info("Creating first new cluster manager...");
                ClusterManager clusterManagerOne = ClusterManager.newClusterManager("leader-election-test",
                        "membership-discovery-test",
                        Coordinator.newCoordinator(client, database + "/coordination-test-node"))
                        .build();
                LOG.info("First cluster manager created");
                LOG.info("Creating second new cluster manager...");
                ClusterManager clusterManagerTwo = ClusterManager.newClusterManager("leader-election-test",
                        "membership-discovery-test",
                        Coordinator.newCoordinator(client, database + "/coordination-test-node"))
                        .leadershipAcquisitionWaitDuration(Duration.ofMillis(30))
                        .build();
                LOG.info("Second cluster manager created");
                Barrier leaderElectedBarrierOne = new Barrier();
                leaderElectedBarrierOne.close();
                AtomicLong leaderElectedOne = new AtomicLong(0L);
                Barrier leaderStepDownBarrierOne = new Barrier();
                leaderStepDownBarrierOne.close();
                AtomicLong leaderStepDownOne = new AtomicLong(0L);
                Barrier leaderNodeElectedBarrierOne = new Barrier();
                leaderNodeElectedBarrierOne.close();
                AtomicLong leaderNodeElectedOne = new AtomicLong(0L);
                Barrier leaderNodeStepDownBarrierOne = new Barrier();
                leaderNodeStepDownBarrierOne.close();
                AtomicLong leaderNodeStepDownOne = new AtomicLong(0L);
                Barrier membershipAddedBarrierOne = new Barrier();
                membershipAddedBarrierOne.close();
                AtomicLong membershipAddedOne = new AtomicLong(0L);
                Barrier moreMembershipAddedBarrierOne = new Barrier();
                moreMembershipAddedBarrierOne.close();
                AtomicLong moreMembershipAddedOne = new AtomicLong(0L);
                Barrier membershipRemovedBarrierOne = new Barrier();
                membershipRemovedBarrierOne.close();
                AtomicLong membershipRemovedOne = new AtomicLong(0L);
                Barrier leaderElectedBarrierTwo = new Barrier();
                leaderElectedBarrierTwo.close();
                Barrier followerElectedBarrierTwo = new Barrier();
                followerElectedBarrierTwo.close();
                AtomicLong leaderElectedTwo = new AtomicLong(0L);
                AtomicLong followerElectedTwo = new AtomicLong(0L);
                Barrier leaderStepDownBarrierTwo = new Barrier();
                leaderStepDownBarrierTwo.close();
                AtomicLong leaderStepDownTwo = new AtomicLong(0L);
                Barrier leaderNodeElectedBarrierTwo = new Barrier();
                leaderNodeElectedBarrierTwo.close();
                AtomicLong leaderNodeElectedTwo = new AtomicLong(0L);
                Barrier followerNodeElectedBarrierTwo = new Barrier();
                followerNodeElectedBarrierTwo.close();
                AtomicLong followerNodeElectedTwo = new AtomicLong(0L);
                Barrier leaderNodeStepDownBarrierTwo = new Barrier();
                leaderNodeStepDownBarrierTwo.close();
                AtomicLong leaderNodeStepDownTwo = new AtomicLong(0L);
                Barrier membershipAddedBarrierTwo = new Barrier();
                membershipAddedBarrierTwo.close();
                AtomicLong membershipAddedTwo = new AtomicLong(0L);
                Barrier lessMembershipAddedBarrierTwo = new Barrier();
                lessMembershipAddedBarrierTwo.close();
                AtomicLong lessMembershipAddedTwo = new AtomicLong(0L);
                Barrier membershipRemovedBarrierTwo = new Barrier();
                membershipRemovedBarrierTwo.close();
                AtomicLong membershipRemovedTwo = new AtomicLong(0L);
                LOG.info("Subscribing to leader first...");
                clusterManagerOne.addLeaderSubscriber(clusterLeader -> {
                    LOG.info("Leader event first: {}", clusterLeader);
                    if (clusterLeader.isConnected() && clusterLeader.getLeader().isPresent()
                            && clusterLeader.getLeader().get().getUuid().equals(clusterManagerOne.getNodeId())) {
                        leaderNodeElectedOne.incrementAndGet();
                        leaderNodeElectedBarrierOne.open();
                    }
                    if (!clusterLeader.isConnected() && clusterLeader.getLeader().isEmpty()
                            && leaderNodeElectedOne.get() == 1L) {
                        leaderNodeStepDownOne.incrementAndGet();
                        leaderNodeStepDownBarrierOne.open();
                    }
                });
                LOG.info("Subscribed to leader first");
                LOG.info("Subscribing to leadership first...");
                clusterManagerOne.addLeadershipSubscriber(status -> {
                    LOG.info("Leadership event first: {}", status);
                    if (status == NodeLeadershipStatus.LEADER) {
                        leaderElectedOne.incrementAndGet();
                        leaderElectedBarrierOne.open();
                    }
                    if (status == NodeLeadershipStatus.UNDEFINED && leaderElectedOne.get() == 1L) {
                        leaderStepDownOne.incrementAndGet();
                        leaderStepDownBarrierOne.open();
                    }
                });
                LOG.info("Subscribed to leadership first");
                LOG.info("Subscribing to members first...");
                clusterManagerOne.addMembershipSubscriber(members -> {
                    LOG.info("Members event first: {}", members);
                    if (members.isConnected() && members.getClusterMembers().size() == 1) {
                        membershipAddedOne.incrementAndGet();
                        membershipAddedBarrierOne.open();
                    }
                    if (members.isConnected() && members.getClusterMembers().size() == 2) {
                        moreMembershipAddedOne.incrementAndGet();
                        moreMembershipAddedBarrierOne.open();
                    }
                    if (!members.isConnected() && members.getClusterMembers().isEmpty()
                            && membershipAddedOne.get() == 1L) {
                        membershipRemovedOne.incrementAndGet();
                        membershipRemovedBarrierOne.open();
                    }
                });
                LOG.info("Subscribed to members first");
                LOG.info("Starting first cluster manager...");
                clusterManagerOne.start();
                LOG.info("First cluster manager started");
                LOG.info("Waiting for leader election first...");
                leaderElectedBarrierOne.passThrough();
                LOG.info("Leader elected first");
                LOG.info("Waiting for leader node election first...");
                leaderNodeElectedBarrierOne.passThrough();
                LOG.info("Leader node elected first");
                LOG.info("Waiting for members first...");
                membershipAddedBarrierOne.passThrough();
                LOG.info("Members registered first");
                Assertions.assertEquals(1L, leaderElectedOne.get());
                Assertions.assertEquals(1L, leaderNodeElectedOne.get());
                Assertions.assertEquals(1L, membershipAddedOne.get());
                clusterManagerTwo.addLeaderSubscriber(clusterLeader -> {
                    LOG.info("Leader event second: {}", clusterLeader);
                    if (clusterLeader.isConnected() && clusterLeader.getLeader().isPresent()
                            && clusterLeader.getLeader().get().getUuid().equals(clusterManagerTwo.getNodeId())) {
                        leaderNodeElectedTwo.incrementAndGet();
                        leaderNodeElectedBarrierTwo.open();
                    }
                    if (clusterLeader.isConnected() && clusterLeader.getLeader().isPresent()
                            && clusterLeader.getLeader().get().getUuid().equals(clusterManagerOne.getNodeId())) {
                        followerNodeElectedTwo.incrementAndGet();
                        followerNodeElectedBarrierTwo.open();
                    }
                    if (!clusterLeader.isConnected() && clusterLeader.getLeader().isEmpty()
                            && leaderNodeElectedTwo.get() == 1L) {
                        leaderNodeStepDownTwo.incrementAndGet();
                        leaderNodeStepDownBarrierTwo.open();
                    }
                });
                LOG.info("Subscribed to leader second");
                LOG.info("Subscribing to leadership second...");
                clusterManagerTwo.addLeadershipSubscriber(status -> {
                    LOG.info("Leadership event second: {}", status);
                    if (status == NodeLeadershipStatus.LEADER) {
                        leaderElectedTwo.incrementAndGet();
                        leaderElectedBarrierTwo.open();
                    }
                    if (status == NodeLeadershipStatus.FOLLOWER) {
                        followerElectedTwo.incrementAndGet();
                        followerElectedBarrierTwo.open();
                    }
                    if (status == NodeLeadershipStatus.UNDEFINED && leaderElectedTwo.get() == 1L) {
                        leaderStepDownTwo.incrementAndGet();
                        leaderStepDownBarrierTwo.open();
                    }
                });
                LOG.info("Subscribed to leadership second");
                LOG.info("Subscribing to members second...");
                clusterManagerTwo.addMembershipSubscriber(members -> {
                    LOG.info("Members event second: {}", members);
                    if (members.isConnected() && members.getClusterMembers().size() == 2) {
                        membershipAddedTwo.incrementAndGet();
                        membershipAddedBarrierTwo.open();
                    }
                    if (members.isConnected() && members.getClusterMembers().size() == 1
                            && membershipAddedTwo.get() == 1L) {
                        lessMembershipAddedTwo.incrementAndGet();
                        lessMembershipAddedBarrierTwo.open();
                    }
                    if (!members.isConnected() && members.getClusterMembers().isEmpty()
                            && membershipAddedTwo.get() == 1L) {
                        membershipRemovedTwo.incrementAndGet();
                        membershipRemovedBarrierTwo.open();
                    }
                });
                LOG.info("Subscribed to members second");
                LOG.info("Starting second cluster manager...");
                clusterManagerTwo.start();
                LOG.info("Second cluster manager started");
                LOG.info("Waiting for follower election second...");
                followerElectedBarrierTwo.passThrough();
                LOG.info("Follower elected second");
                LOG.info("Waiting for follower node election second...");
                followerNodeElectedBarrierTwo.passThrough();
                LOG.info("Follower node elected second");
                LOG.info("Waiting for members second...");
                membershipAddedBarrierTwo.passThrough();
                LOG.info("Members registered second");
                LOG.info("Waiting for more members first...");
                moreMembershipAddedBarrierOne.passThrough();
                LOG.info("More members registered first");
                Assertions.assertEquals(1L, followerElectedTwo.get());
                Assertions.assertEquals(1L, followerNodeElectedTwo.get());
                Assertions.assertEquals(1L, membershipAddedTwo.get());
                Assertions.assertEquals(1L, moreMembershipAddedOne.get());
                LOG.info("Waiting...");
                Thread.sleep(100);
                LOG.info("Wait finished");
                LOG.info("Stopping first cluster manager...");
                Barrier barrierOne = new Barrier();
                barrierOne.close();
                clusterManagerOne.stop(barrierOne::open);
                LOG.info("Waiting for leader step down first...");
                leaderStepDownBarrierOne.passThrough();
                LOG.info("Leader step down first");
                LOG.info("Waiting for leader node step down first...");
                leaderNodeStepDownBarrierOne.passThrough();
                LOG.info("Leader node step down first");
                LOG.info("Waiting for members removal first...");
                membershipRemovedBarrierOne.passThrough();
                LOG.info("Members removed first");
                LOG.info("Waiting for leader election second...");
                leaderElectedBarrierTwo.passThrough();
                LOG.info("Leader elected second");
                LOG.info("Waiting for leader node election second...");
                leaderNodeElectedBarrierTwo.passThrough();
                LOG.info("Leader node elected second");
                LOG.info("Waiting for less members second...");
                lessMembershipAddedBarrierTwo.passThrough();
                LOG.info("Members removed second");
                Assertions.assertEquals(1L, leaderStepDownOne.get());
                Assertions.assertEquals(1L, leaderNodeStepDownOne.get());
                Assertions.assertEquals(1L, membershipRemovedOne.get());
                Assertions.assertEquals(1L, leaderElectedTwo.get());
                Assertions.assertEquals(1L, leaderNodeElectedTwo.get());
                Assertions.assertEquals(1L, lessMembershipAddedTwo.get());
                LOG.info("Waiting until first cluster manager is stopped...");
                barrierOne.passThrough();
                LOG.info("First cluster manager is stopped");
                LOG.info("Stopping second cluster manager...");
                Barrier barrierTwo = new Barrier();
                barrierTwo.close();
                clusterManagerTwo.stop(barrierTwo::open);
                LOG.info("Waiting for leader step down second...");
                leaderStepDownBarrierTwo.passThrough();
                LOG.info("Leader step down second");
                LOG.info("Waiting for leader node step down second...");
                leaderNodeStepDownBarrierTwo.passThrough();
                LOG.info("Leader node step down second");
                LOG.info("Waiting for members removal second...");
                membershipRemovedBarrierTwo.passThrough();
                LOG.info("Members removed second");
                Assertions.assertEquals(1L, leaderStepDownTwo.get());
                Assertions.assertEquals(1L, leaderNodeStepDownTwo.get());
                Assertions.assertEquals(1L, membershipRemovedTwo.get());
                LOG.info("Waiting until second cluster manager is stopped...");
                barrierTwo.passThrough();
                LOG.info("Second cluster manager is stopped");
            }
        }
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void testStartStopTwoManagersChecks() throws IOException, InterruptedException {
        try (CoordinationRpc rpc = GrpcCoordinationRpc.useTransport(ydbRpcTransport.getRpcTransport())) {
            try (CoordinationClient client = CoordinationClient.newClient(rpc).build()) {
                LOG.info("Creating first new cluster manager...");
                ClusterManager clusterManagerOne = ClusterManager.newClusterManager("leader-election-test",
                        "membership-discovery-test",
                        Coordinator.newCoordinator(client, database + "/coordination-test-node"))
                        .build();
                LOG.info("First cluster manager created");
                LOG.info("Creating second new cluster manager...");
                ClusterManager clusterManagerTwo = ClusterManager.newClusterManager("leader-election-test",
                        "membership-discovery-test",
                        Coordinator.newCoordinator(client, database + "/coordination-test-node"))
                        .leadershipAcquisitionWaitDuration(Duration.ofMillis(30))
                        .build();
                LOG.info("Second cluster manager created");
                Barrier leaderElectedBarrierOne = new Barrier();
                leaderElectedBarrierOne.close();
                AtomicLong leaderElectedOne = new AtomicLong(0L);
                Barrier leaderStepDownBarrierOne = new Barrier();
                leaderStepDownBarrierOne.close();
                AtomicLong leaderStepDownOne = new AtomicLong(0L);
                Barrier leaderNodeElectedBarrierOne = new Barrier();
                leaderNodeElectedBarrierOne.close();
                AtomicLong leaderNodeElectedOne = new AtomicLong(0L);
                Barrier leaderNodeStepDownBarrierOne = new Barrier();
                leaderNodeStepDownBarrierOne.close();
                AtomicLong leaderNodeStepDownOne = new AtomicLong(0L);
                Barrier membershipAddedBarrierOne = new Barrier();
                membershipAddedBarrierOne.close();
                AtomicLong membershipAddedOne = new AtomicLong(0L);
                Barrier moreMembershipAddedBarrierOne = new Barrier();
                moreMembershipAddedBarrierOne.close();
                AtomicLong moreMembershipAddedOne = new AtomicLong(0L);
                Barrier membershipRemovedBarrierOne = new Barrier();
                membershipRemovedBarrierOne.close();
                AtomicLong membershipRemovedOne = new AtomicLong(0L);
                Barrier leaderElectedBarrierTwo = new Barrier();
                leaderElectedBarrierTwo.close();
                Barrier followerElectedBarrierTwo = new Barrier();
                followerElectedBarrierTwo.close();
                AtomicLong leaderElectedTwo = new AtomicLong(0L);
                AtomicLong followerElectedTwo = new AtomicLong(0L);
                Barrier leaderStepDownBarrierTwo = new Barrier();
                leaderStepDownBarrierTwo.close();
                AtomicLong leaderStepDownTwo = new AtomicLong(0L);
                Barrier leaderNodeElectedBarrierTwo = new Barrier();
                leaderNodeElectedBarrierTwo.close();
                AtomicLong leaderNodeElectedTwo = new AtomicLong(0L);
                Barrier followerNodeElectedBarrierTwo = new Barrier();
                followerNodeElectedBarrierTwo.close();
                AtomicLong followerNodeElectedTwo = new AtomicLong(0L);
                Barrier leaderNodeStepDownBarrierTwo = new Barrier();
                leaderNodeStepDownBarrierTwo.close();
                AtomicLong leaderNodeStepDownTwo = new AtomicLong(0L);
                Barrier membershipAddedBarrierTwo = new Barrier();
                membershipAddedBarrierTwo.close();
                AtomicLong membershipAddedTwo = new AtomicLong(0L);
                Barrier lessMembershipAddedBarrierTwo = new Barrier();
                lessMembershipAddedBarrierTwo.close();
                AtomicLong lessMembershipAddedTwo = new AtomicLong(0L);
                Barrier membershipRemovedBarrierTwo = new Barrier();
                membershipRemovedBarrierTwo.close();
                AtomicLong membershipRemovedTwo = new AtomicLong(0L);
                LOG.info("Subscribing to leader first...");
                clusterManagerOne.addLeaderSubscriber(clusterLeader -> {
                    LOG.info("Leader event first: {}", clusterLeader);
                    if (clusterLeader.isConnected() && clusterLeader.getLeader().isPresent()
                            && clusterLeader.getLeader().get().getUuid().equals(clusterManagerOne.getNodeId())) {
                        leaderNodeElectedOne.incrementAndGet();
                        leaderNodeElectedBarrierOne.open();
                    }
                    if (!clusterLeader.isConnected() && clusterLeader.getLeader().isEmpty()
                            && leaderNodeElectedOne.get() == 1L) {
                        leaderNodeStepDownOne.incrementAndGet();
                        leaderNodeStepDownBarrierOne.open();
                    }
                });
                LOG.info("Subscribed to leader first");
                LOG.info("Subscribing to leadership first...");
                clusterManagerOne.addLeadershipSubscriber(status -> {
                    LOG.info("Leadership event first: {}", status);
                    if (status == NodeLeadershipStatus.LEADER) {
                        leaderElectedOne.incrementAndGet();
                        leaderElectedBarrierOne.open();
                    }
                    if (status == NodeLeadershipStatus.UNDEFINED && leaderElectedOne.get() == 1L) {
                        leaderStepDownOne.incrementAndGet();
                        leaderStepDownBarrierOne.open();
                    }
                });
                LOG.info("Subscribed to leadership first");
                LOG.info("Subscribing to members first...");
                clusterManagerOne.addMembershipSubscriber(members -> {
                    LOG.info("Members event first: {}", members);
                    if (members.isConnected() && members.getClusterMembers().size() == 1) {
                        membershipAddedOne.incrementAndGet();
                        membershipAddedBarrierOne.open();
                    }
                    if (members.isConnected() && members.getClusterMembers().size() == 2) {
                        moreMembershipAddedOne.incrementAndGet();
                        moreMembershipAddedBarrierOne.open();
                    }
                    if (!members.isConnected() && members.getClusterMembers().isEmpty()
                            && membershipAddedOne.get() == 1L) {
                        membershipRemovedOne.incrementAndGet();
                        membershipRemovedBarrierOne.open();
                    }
                });
                LOG.info("Subscribed to members first");
                LOG.info("Starting first cluster manager...");
                clusterManagerOne.start();
                LOG.info("First cluster manager started");
                LOG.info("Waiting for leader election first...");
                leaderElectedBarrierOne.passThrough();
                LOG.info("Leader elected first");
                LOG.info("Waiting for leader node election first...");
                leaderNodeElectedBarrierOne.passThrough();
                LOG.info("Leader node elected first");
                LOG.info("Waiting for members first...");
                membershipAddedBarrierOne.passThrough();
                LOG.info("Members registered first");
                Assertions.assertEquals(1L, leaderElectedOne.get());
                Assertions.assertEquals(1L, leaderNodeElectedOne.get());
                Assertions.assertEquals(1L, membershipAddedOne.get());
                Boolean isLeaderOne = clusterManagerOne.isLeader().block();
                Assertions.assertNotNull(isLeaderOne);
                Assertions.assertTrue(isLeaderOne);
                Optional<Boolean> isLeaderCachedOne = clusterManagerOne.isLeaderCached();
                Assertions.assertTrue(isLeaderCachedOne.isPresent());
                Assertions.assertTrue(isLeaderCachedOne.get());
                Optional<NodeInfo> leaderOne = clusterManagerOne.getLeader().block();
                Assertions.assertNotNull(leaderOne);
                Assertions.assertTrue(leaderOne.isPresent());
                Assertions.assertEquals(clusterManagerOne.getNodeId(), leaderOne.get().getUuid());
                Optional<NodeInfo> leaderCachedOne = clusterManagerOne.getLeaderCached();
                Assertions.assertTrue(leaderCachedOne.isPresent());
                Assertions.assertEquals(clusterManagerOne.getNodeId(), leaderCachedOne.get().getUuid());
                Set<NodeInfo> membersOne = clusterManagerOne.getMembers().block();
                Assertions.assertNotNull(membersOne);
                Assertions.assertEquals(1, membersOne.size());
                Optional<Set<NodeInfo>> membersCachedOne = clusterManagerOne.getMembersCached();
                Assertions.assertTrue(membersCachedOne.isPresent());
                Assertions.assertEquals(1, membersCachedOne.get().size());
                Set<NodeInfo> membersLastOne = clusterManagerOne.getLastSeenClusterMembers();
                Assertions.assertEquals(1, membersLastOne.size());
                clusterManagerTwo.addLeaderSubscriber(clusterLeader -> {
                    LOG.info("Leader event second: {}", clusterLeader);
                    if (clusterLeader.isConnected() && clusterLeader.getLeader().isPresent()
                            && clusterLeader.getLeader().get().getUuid().equals(clusterManagerTwo.getNodeId())) {
                        leaderNodeElectedTwo.incrementAndGet();
                        leaderNodeElectedBarrierTwo.open();
                    }
                    if (clusterLeader.isConnected() && clusterLeader.getLeader().isPresent()
                            && clusterLeader.getLeader().get().getUuid().equals(clusterManagerOne.getNodeId())) {
                        followerNodeElectedTwo.incrementAndGet();
                        followerNodeElectedBarrierTwo.open();
                    }
                    if (!clusterLeader.isConnected() && clusterLeader.getLeader().isEmpty()
                            && leaderNodeElectedTwo.get() == 1L) {
                        leaderNodeStepDownTwo.incrementAndGet();
                        leaderNodeStepDownBarrierTwo.open();
                    }
                });
                LOG.info("Subscribed to leader second");
                LOG.info("Subscribing to leadership second...");
                clusterManagerTwo.addLeadershipSubscriber(status -> {
                    LOG.info("Leadership event second: {}", status);
                    if (status == NodeLeadershipStatus.LEADER) {
                        leaderElectedTwo.incrementAndGet();
                        leaderElectedBarrierTwo.open();
                    }
                    if (status == NodeLeadershipStatus.FOLLOWER) {
                        followerElectedTwo.incrementAndGet();
                        followerElectedBarrierTwo.open();
                    }
                    if (status == NodeLeadershipStatus.UNDEFINED && leaderElectedTwo.get() == 1L) {
                        leaderStepDownTwo.incrementAndGet();
                        leaderStepDownBarrierTwo.open();
                    }
                });
                LOG.info("Subscribed to leadership second");
                LOG.info("Subscribing to members second...");
                clusterManagerTwo.addMembershipSubscriber(members -> {
                    LOG.info("Members event second: {}", members);
                    if (members.isConnected() && members.getClusterMembers().size() == 2) {
                        membershipAddedTwo.incrementAndGet();
                        membershipAddedBarrierTwo.open();
                    }
                    if (members.isConnected() && members.getClusterMembers().size() == 1
                            && membershipAddedTwo.get() == 1L) {
                        lessMembershipAddedTwo.incrementAndGet();
                        lessMembershipAddedBarrierTwo.open();
                    }
                    if (!members.isConnected() && members.getClusterMembers().isEmpty()
                            && membershipAddedTwo.get() == 1L) {
                        membershipRemovedTwo.incrementAndGet();
                        membershipRemovedBarrierTwo.open();
                    }
                });
                LOG.info("Subscribed to members second");
                LOG.info("Starting second cluster manager...");
                clusterManagerTwo.start();
                LOG.info("Second cluster manager started");
                LOG.info("Waiting for follower election second...");
                followerElectedBarrierTwo.passThrough();
                LOG.info("Follower elected second");
                LOG.info("Waiting for follower node election second...");
                followerNodeElectedBarrierTwo.passThrough();
                LOG.info("Follower node elected second");
                LOG.info("Waiting for members second...");
                membershipAddedBarrierTwo.passThrough();
                LOG.info("Members registered second");
                LOG.info("Waiting for more members first...");
                moreMembershipAddedBarrierOne.passThrough();
                LOG.info("More members registered first");
                Assertions.assertEquals(1L, followerElectedTwo.get());
                Assertions.assertEquals(1L, followerNodeElectedTwo.get());
                Assertions.assertEquals(1L, membershipAddedTwo.get());
                Assertions.assertEquals(1L, moreMembershipAddedOne.get());
                Boolean isLeaderTwo = clusterManagerOne.isLeader().block();
                Assertions.assertNotNull(isLeaderTwo);
                Assertions.assertTrue(isLeaderTwo);
                Optional<Boolean> isLeaderCachedTwo = clusterManagerOne.isLeaderCached();
                Assertions.assertTrue(isLeaderCachedTwo.isPresent());
                Assertions.assertTrue(isLeaderCachedTwo.get());
                Optional<NodeInfo> leaderTwo = clusterManagerOne.getLeader().block();
                Assertions.assertNotNull(leaderTwo);
                Assertions.assertTrue(leaderTwo.isPresent());
                Assertions.assertEquals(clusterManagerOne.getNodeId(), leaderTwo.get().getUuid());
                Optional<NodeInfo> leaderCachedTwo = clusterManagerOne.getLeaderCached();
                Assertions.assertTrue(leaderCachedTwo.isPresent());
                Assertions.assertEquals(clusterManagerOne.getNodeId(), leaderCachedTwo.get().getUuid());
                Set<NodeInfo> membersTwo = clusterManagerOne.getMembers().block();
                Assertions.assertNotNull(membersTwo);
                Assertions.assertEquals(2, membersTwo.size());
                Optional<Set<NodeInfo>> membersCachedTwo = clusterManagerOne.getMembersCached();
                Assertions.assertTrue(membersCachedTwo.isPresent());
                Assertions.assertEquals(2, membersCachedTwo.get().size());
                Set<NodeInfo> membersLastTwo = clusterManagerOne.getLastSeenClusterMembers();
                Assertions.assertEquals(2, membersLastTwo.size());
                Boolean isLeaderThree = clusterManagerTwo.isLeader().block();
                Assertions.assertNotNull(isLeaderThree);
                Assertions.assertFalse(isLeaderThree);
                Optional<Boolean> isLeaderCachedThree = clusterManagerTwo.isLeaderCached();
                Assertions.assertTrue(isLeaderCachedThree.isPresent());
                Assertions.assertFalse(isLeaderCachedThree.get());
                Optional<NodeInfo> leaderThree = clusterManagerTwo.getLeader().block();
                Assertions.assertNotNull(leaderThree);
                Assertions.assertTrue(leaderThree.isPresent());
                Assertions.assertEquals(clusterManagerOne.getNodeId(), leaderThree.get().getUuid());
                Optional<NodeInfo> leaderCachedThree = clusterManagerTwo.getLeaderCached();
                Assertions.assertTrue(leaderCachedThree.isPresent());
                Assertions.assertEquals(clusterManagerOne.getNodeId(), leaderCachedThree.get().getUuid());
                Set<NodeInfo> membersThree = clusterManagerTwo.getMembers().block();
                Assertions.assertNotNull(membersThree);
                Assertions.assertEquals(2, membersThree.size());
                Optional<Set<NodeInfo>> membersCachedThree = clusterManagerTwo.getMembersCached();
                Assertions.assertTrue(membersCachedThree.isPresent());
                Assertions.assertEquals(2, membersCachedThree.get().size());
                Set<NodeInfo> membersLastThree = clusterManagerTwo.getLastSeenClusterMembers();
                Assertions.assertEquals(2, membersLastThree.size());
                LOG.info("Waiting...");
                Thread.sleep(100);
                LOG.info("Wait finished");
                LOG.info("Stopping first cluster manager...");
                Barrier barrierOne = new Barrier();
                barrierOne.close();
                clusterManagerOne.stop(barrierOne::open);
                LOG.info("Waiting for leader step down first...");
                leaderStepDownBarrierOne.passThrough();
                LOG.info("Leader step down first");
                LOG.info("Waiting for leader node step down first...");
                leaderNodeStepDownBarrierOne.passThrough();
                LOG.info("Leader node step down first");
                LOG.info("Waiting for members removal first...");
                membershipRemovedBarrierOne.passThrough();
                LOG.info("Members removed first");
                LOG.info("Waiting for leader election second...");
                leaderElectedBarrierTwo.passThrough();
                LOG.info("Leader elected second");
                LOG.info("Waiting for leader node election second...");
                leaderNodeElectedBarrierTwo.passThrough();
                LOG.info("Leader node elected second");
                LOG.info("Waiting for less members second...");
                lessMembershipAddedBarrierTwo.passThrough();
                LOG.info("Members removed second");
                Assertions.assertEquals(1L, leaderStepDownOne.get());
                Assertions.assertEquals(1L, leaderNodeStepDownOne.get());
                Assertions.assertEquals(1L, membershipRemovedOne.get());
                Assertions.assertEquals(1L, leaderElectedTwo.get());
                Assertions.assertEquals(1L, leaderNodeElectedTwo.get());
                Assertions.assertEquals(1L, lessMembershipAddedTwo.get());
                LOG.info("Waiting until first cluster manager is stopped...");
                barrierOne.passThrough();
                LOG.info("First cluster manager is stopped");
                Boolean isLeaderFour = clusterManagerTwo.isLeader().block();
                Assertions.assertNotNull(isLeaderFour);
                Assertions.assertTrue(isLeaderFour);
                Optional<Boolean> isLeaderCachedFour = clusterManagerTwo.isLeaderCached();
                Assertions.assertTrue(isLeaderCachedFour.isPresent());
                Assertions.assertTrue(isLeaderCachedFour.get());
                Optional<NodeInfo> leaderFour = clusterManagerTwo.getLeader().block();
                Assertions.assertNotNull(leaderFour);
                Assertions.assertTrue(leaderFour.isPresent());
                Assertions.assertEquals(clusterManagerTwo.getNodeId(), leaderFour.get().getUuid());
                Optional<NodeInfo> leaderCachedFour = clusterManagerTwo.getLeaderCached();
                Assertions.assertTrue(leaderCachedFour.isPresent());
                Assertions.assertEquals(clusterManagerTwo.getNodeId(), leaderCachedFour.get().getUuid());
                Set<NodeInfo> membersFour = clusterManagerTwo.getMembers().block();
                Assertions.assertNotNull(membersFour);
                Assertions.assertEquals(1, membersFour.size());
                Optional<Set<NodeInfo>> membersCachedFour = clusterManagerTwo.getMembersCached();
                Assertions.assertTrue(membersCachedFour.isPresent());
                Assertions.assertEquals(1, membersCachedFour.get().size());
                Set<NodeInfo> membersLastFour = clusterManagerTwo.getLastSeenClusterMembers();
                Assertions.assertEquals(1, membersLastFour.size());
                LOG.info("Stopping second cluster manager...");
                Barrier barrierTwo = new Barrier();
                barrierTwo.close();
                clusterManagerTwo.stop(barrierTwo::open);
                LOG.info("Waiting for leader step down second...");
                leaderStepDownBarrierTwo.passThrough();
                LOG.info("Leader step down second");
                LOG.info("Waiting for leader node step down second...");
                leaderNodeStepDownBarrierTwo.passThrough();
                LOG.info("Leader node step down second");
                LOG.info("Waiting for members removal second...");
                membershipRemovedBarrierTwo.passThrough();
                LOG.info("Members removed second");
                Assertions.assertEquals(1L, leaderStepDownTwo.get());
                Assertions.assertEquals(1L, leaderNodeStepDownTwo.get());
                Assertions.assertEquals(1L, membershipRemovedTwo.get());
                LOG.info("Waiting until second cluster manager is stopped...");
                barrierTwo.passThrough();
                LOG.info("Second cluster manager is stopped");
            }
        }
    }

}
