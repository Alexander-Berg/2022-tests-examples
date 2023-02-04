package ru.yandex.solomon.alert.cluster.balancer;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.annotation.concurrent.GuardedBy;

import com.google.common.collect.ImmutableSet;
import com.google.protobuf.TextFormat;
import io.grpc.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.solomon.alert.cluster.balancer.client.AlertingBalancerClient;
import ru.yandex.solomon.alert.protobuf.TAssignProjectRequest;
import ru.yandex.solomon.alert.protobuf.TAssignProjectResponse;
import ru.yandex.solomon.alert.protobuf.THeartbeatRequest;
import ru.yandex.solomon.alert.protobuf.THeartbeatResponse;
import ru.yandex.solomon.alert.protobuf.TProjectAssignmentRequest;
import ru.yandex.solomon.alert.protobuf.TProjectAssignmentResponse;
import ru.yandex.solomon.alert.protobuf.TUnassignProjectRequest;
import ru.yandex.solomon.alert.protobuf.TUnassignProjectResponse;

/**
 * @author Vladimir Gordiychuk
 */
public class AlertingBalancerClientStub implements AlertingBalancerClient {
    private static final Logger logger = LoggerFactory.getLogger(AlertingBalancerClientStub.class);
    private final ConcurrentMap<String, AbstractAlertingNode> unitByName = new ConcurrentHashMap<>();
    @GuardedBy("this")
    private List<Runnable> callabacksOnClusterChanges = new ArrayList<>();

    public void register(AbstractAlertingNode... nodes) {
        List<Runnable> callbacks;
        synchronized (this) {
            for (AbstractAlertingNode node : nodes) {
                this.unitByName.put(node.getName(), node);
            }
            callbacks = callabacksOnClusterChanges;
            callabacksOnClusterChanges = new ArrayList<>();
        }

        for (Runnable callback: callbacks) {
            callback.run();
        }
    }

    @Override
    public boolean hasNode(String node) {
        return unitByName.containsKey(node);
    }

    @Override
    public Set<String> getNodes() {
        return ImmutableSet.copyOf(unitByName.keySet());
    }

    @Override
    public CompletableFuture<TAssignProjectResponse> assignShard(String node, TAssignProjectRequest request) {
        return getTarget(node)
            .thenCompose(unit -> {
                logger.debug("{} receive assign: {{}}", node, TextFormat.shortDebugString(request));
                return unit.assignShard(request)
                    .whenComplete((ignore, ignore2) -> {
                        unit.onCompleteMessage();
                    });
            });
    }

    @Override
    public CompletableFuture<TUnassignProjectResponse> unassignShard(String node, TUnassignProjectRequest request) {
        return getTarget(node)
            .thenCompose(unit -> {
                logger.debug("{} receive unassign: {}", node, TextFormat.shortDebugString(request));
                return unit.unassignShard(request)
                    .whenComplete((ignore, ignore2) -> {
                        unit.onCompleteMessage();
                    });
            });
    }

    @Override
    public CompletableFuture<TProjectAssignmentResponse> listAssignments(String node, TProjectAssignmentRequest request) {
        return getTarget(node)
            .thenCompose(unit -> {
                logger.debug("{} receive ls: {}", node, TextFormat.shortDebugString(request));
                return unit.listAssignments(request)
                    .whenComplete((ignore, ignore2) -> {
                        unit.onCompleteMessage();
                    });
            });
    }

    @Override
    public CompletableFuture<THeartbeatResponse> heartbeat(String node, THeartbeatRequest request) {
        return getTarget(node)
            .thenCompose(unit -> {
                logger.debug("{} receive heartbeat: {}", node, TextFormat.shortDebugString(request));
                return unit.heartbeat(request)
                    .whenComplete((ignore, ignore2) -> {
                        unit.onCompleteMessage();
                    });
            });
    }

    private CompletableFuture<AbstractAlertingNode> getTarget(String host) {
        return CompletableFuture.supplyAsync(() -> {
            AbstractAlertingNode unit = unitByName.get(host);
            if (unit == null) {
                throw Status.NOT_FOUND
                    .withDescription("unknown host: " + host)
                    .asRuntimeException();
            }

            return unit;
        });
    }
}
