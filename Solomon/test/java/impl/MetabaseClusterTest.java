package ru.yandex.metabase.client.impl;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.google.common.net.HostAndPort;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import ru.yandex.discovery.DiscoveryService;
import ru.yandex.grpc.utils.DefaultClientOptions;
import ru.yandex.metabase.client.MetabaseClientOptions;
import ru.yandex.solomon.labels.query.Selectors;
import ru.yandex.solomon.metabase.api.protobuf.EMetabaseStatusCode;
import ru.yandex.solomon.metabase.api.protobuf.TServerStatusResponse;
import ru.yandex.solomon.metabase.api.protobuf.TShardStatus;
import ru.yandex.solomon.model.protobuf.Label;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class MetabaseClusterTest {
    @Rule
    public Timeout timeoutRule = Timeout.builder()
            .withLookingForStuckThread(true)
            .withTimeout(15, TimeUnit.SECONDS)
            .build();

    private static class BrokenDiscovery implements DiscoveryService {

        @Override
        public CompletableFuture<List<HostAndPort>> resolve(List<String> hosts) {
            return new CompletableFuture<>(); // never complete
        }
    }

    private static class StaticDiscovery implements DiscoveryService {

        @Override
        public CompletableFuture<List<HostAndPort>> resolve(List<String> hosts) {
            return CompletableFuture.completedFuture(List.of(
                    HostAndPort.fromString("alice:1234"),
                    HostAndPort.fromString("bob:1234"),
                    HostAndPort.fromString("eve:1234")
            ));
        }
    }

    @Test
    public void emptyWhenDiscoveryInProgress() {
        DiscoveryService discovery = new BrokenDiscovery();
        var cluster = new MetabaseCluster(
            List.of("discovery://metabase"),
            MetabaseClientOptions.newBuilder(DefaultClientOptions.empty())
                .setDiscoveryService(discovery)
                .build());

        Selectors selectors = Selectors.parse("{project=solomon, cluster=production, service=coremon}");

        try {
            PartitionedShardResolver.of(cluster).resolve(selectors).count();
        } catch (StatusRuntimeException e) {
            assertEquals(e.getStatus().getCode(), Status.Code.UNAVAILABLE);
            return;
        }
        fail("Must throw exception if discovery is not ready yet");
    }

    @Test
    public void deadNodeTotalShardsIgnored() throws InterruptedException {
        ConcurrentHashMap<String, MetabaseNodeStub> nodes = new ConcurrentHashMap<>();

        DiscoveryService discovery = new StaticDiscovery();
        var cluster = new MetabaseCluster(
                List.of("discovery://metabase"),
                (address, opts, executor, timer) -> nodes.computeIfAbsent(address.toString(), ignore -> new MetabaseNodeStub(address)),
                MetabaseClientOptions.newBuilder(DefaultClientOptions.empty())
                    .setDiscoveryService(discovery)
                    .build());

        cluster.forceUpdateClusterState().join();

        var alice = nodes.get("alice:1234");
        var bob = nodes.get("bob:1234");

        int hash = 42;
        for (var node : nodes.values()) {
            var resp = TServerStatusResponse
                    .newBuilder()
                    .setTotalPartitionCountKnown(true)
                    .setTotalPartitionCount(60)
                    .setShardIdsHash(hash++)
                    .setStatus(EMetabaseStatusCode.OK)
                    .addAllPartitionStatus(makeShardsFor(node.name, 0, 20))
                    .build();
            node.onResponse(resp);
        }

        // merge node state to cluster state
        cluster.forceUpdateClusterState().join();

        Assert.assertEquals(cluster.getAvailability().toString(), 1.0, cluster.getAvailability().getAvailability(), 1e-14);

        Thread.sleep(10);

        {
            var resp = TServerStatusResponse
                    .newBuilder()
                    .setTotalPartitionCountKnown(true)
                    .setTotalPartitionCount(30)
                    .setShardIdsHash(hash++)
                    .setStatus(EMetabaseStatusCode.OK)
                    .addAllPartitionStatus(makeShardsFor("alice", 0, 10))
                    .addAllPartitionStatus(makeShardsFor("eve", 0, 5))
                    .build();
            alice.onResponse(resp);
        }

        {
            var resp = TServerStatusResponse
                    .newBuilder()
                    .setTotalPartitionCountKnown(true)
                    .setTotalPartitionCount(30)
                    .setShardIdsHash(hash++)
                    .setStatus(EMetabaseStatusCode.OK)
                    .addAllPartitionStatus(makeShardsFor("bob", 0, 10))
                    .addAllPartitionStatus(makeShardsFor("eve", 5, 10))
                    .build();
            bob.onResponse(resp);
        }

        // merge node state to cluster state
        cluster.forceUpdateClusterState().join();

        Assert.assertEquals(cluster.getAvailability().toString(), 1.0, cluster.getAvailability().getAvailability(), 1e-14);
    }

    private List<TShardStatus> makeShardsFor(String name, int from, int to) {
        return IntStream.range(from, to).mapToObj(i -> TShardStatus.newBuilder()
                .addLabels(Label.newBuilder().setKey("project").setValue("solomon").build())
                .addLabels(Label.newBuilder().setKey("cluster").setValue(name).build())
                .addLabels(Label.newBuilder().setKey("service").setValue("microservice-" + i).build())
                .setReady(true)
                .setNumId(Objects.hash(name, i))
                .setShardId(name + "_" + i)
                .build())
                .collect(Collectors.toList());
    }

    public static class MetabaseNodeStub implements MetabaseNode {
        private final String name;
        private volatile ShardsState state;

        public MetabaseNodeStub(HostAndPort address) {
            name = address.getHost();
            state = ShardsState.init(name);
        }

        void onResponse(TServerStatusResponse response) {
            var currentState = state;
            state = ShardsState.update(name, currentState, response);
        }

        @Override
        public MetabaseNodeClient getClient() {
            return null;
        }

        @Override
        public CompletableFuture<?> forceUpdate() {
            return CompletableFuture.supplyAsync(() -> null);
        }

        @Override
        public ShardsState getState() {
            return state;
        }

        @Override
        public void close() throws Exception {

        }
    }
}
