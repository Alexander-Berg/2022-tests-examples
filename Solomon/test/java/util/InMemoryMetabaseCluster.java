package ru.yandex.metabase.client.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.Iterators;
import com.google.common.net.HostAndPort;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.netty.NettyServerBuilder;
import io.netty.util.concurrent.DefaultThreadFactory;

import ru.yandex.solomon.labels.shard.ShardKey;

/**
 * @author Vladimir Gordiychuk
 */
@ParametersAreNonnullByDefault
public class InMemoryMetabaseCluster implements AutoCloseable {
    private final ExecutorService executorService;
    private final ConcurrentMap<HostAndPort, Server> addressToServer;
    private final Map<HostAndPort, InMemoryMetabaseService> addressToService;
    private final boolean portBinding;
    private final Iterator<InMemoryMetabaseService> cycleServiceIterator;

    private InMemoryMetabaseCluster(Builder builder) throws IOException {
        this.executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors(),
                new DefaultThreadFactory("metabase-server-grpc")
        );

        this.portBinding = builder.portBinding;
        this.addressToServer = new ConcurrentHashMap<>();
        this.addressToService = new HashMap<>();
        for (int serverNum = 0; serverNum < builder.countServer; serverNum++) {
            InMemoryMetabaseService service = new InMemoryMetabaseService();
            final String host;
            final int port;
            final Server server;
            if (!builder.portBinding) {
                host = "in-memory-metabase-server-" + serverNum;
                server = InProcessServerBuilder.forName(host)
                        .addService(service)
                        .executor(executorService)
                        .build()
                        .start();

                port = server.getPort();
            } else {
                host = "localhost";
                server = NettyServerBuilder.forPort(0)
                        .addService(service)
                        .executor(executorService)
                        .build()
                        .start();
                port = server.getPort();
            }

            HostAndPort address;
            if (port == -1) {
                address = HostAndPort.fromString(host);
            } else {
                address = HostAndPort.fromParts(host, port);
            }
            addressToServer.put(address, server);
            addressToService.put(address, service);
        }

        cycleServiceIterator = Iterators.cycle(addressToService.values());
        for (ShardKey shardKey : builder.shards) {
            addShard(shardKey);
        }
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public List<HostAndPort> getServerList() {
        return new ArrayList<>(addressToServer.keySet());
    }

    public void addShard(ShardKey key) {
        cycleServiceIterator.next().addShard(key);
    }

    public void setMetricsLimit(ShardKey key, int limit) {
        getServiceByAddress(getAddressByShard(key)).setMetricsLimit(key, limit);
    }

    public void moveShard(ShardKey key) {
        InMemoryMetabaseService service = addressToService.get(getAddressByShard(key));
        InMemoryMetabaseShard oldShard = service.getShard(key);
        service.removeShard(key);
        do {
            var next = cycleServiceIterator.next();
            if (next == service) {
                continue;
            }

            next.addShard(key, oldShard);
            break;
        } while (true);
    }

    public void iterate(int iterations) {
        for (int i = 0; i < iterations; i++) {
            cycleServiceIterator.next();
        }
    }

    public HostAndPort getAddressByShard(ShardKey shardKey) {
        for (Map.Entry<HostAndPort, InMemoryMetabaseService> entry : addressToService.entrySet()) {
            if (entry.getValue().hasShard(shardKey)) {
                return entry.getKey();
            }
        }

        throw new RuntimeException("Not found service with shardKey:" + shardKey);
    }

    public InMemoryMetabaseService getServiceByAddress(HostAndPort address) {
        return addressToService.get(address);
    }

    public void forceStopNodeByAddress(HostAndPort address) throws InterruptedException {
        Server server = addressToServer.get(address);
        server.shutdownNow();
        server.awaitTermination();
    }

    public void restartNodeByAddress(HostAndPort address) throws InterruptedException, IOException {
        Server server = addressToServer.get(address);
        if (!server.isTerminated()) {
            forceStopNodeByAddress(address);
        }

        final Server newServer;
        if (portBinding) {
            newServer = ServerBuilder.forPort(address.getPort())
                    .addService(addressToService.get(address))
                    .executor(executorService)
                    .build()
                    .start();
        } else {
            newServer = InProcessServerBuilder.forName(address.getHost())
                    .addService(addressToService.get(address))
                    .executor(executorService)
                    .build()
                    .start();
        }

        addressToServer.replace(address, newServer);
    }

    @Override
    public void close() {
        addressToServer.values().forEach(Server::shutdown);
        executorService.shutdown();
    }

    public static class Builder {
        private int countServer = 1;
        private boolean portBinding = true;
        private List<ShardKey> shards = new ArrayList<>();

        private Builder() {
        }

        public Builder serverCount(int countServer) {
            this.countServer = countServer;
            return this;
        }

        public Builder inProcess() {
            portBinding = false;
            return this;
        }

        public Builder withShard(String project, String cluster, String service) {
            return withShard(new ShardKey(project, cluster, service));
        }

        public Builder withShard(ShardKey key) {
            shards.add(key);
            return this;
        }

        public InMemoryMetabaseCluster build() throws IOException {
            return new InMemoryMetabaseCluster(this);
        }
    }
}
