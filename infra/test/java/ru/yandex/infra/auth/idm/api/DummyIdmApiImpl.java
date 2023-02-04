package ru.yandex.infra.auth.idm.api;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DummyIdmApiImpl implements IdmApi {
    private static final Logger LOG = LoggerFactory.getLogger(DummyIdmApiImpl.class);

    private CompletableFuture<List<RoleNodeInfo>> roleNodesFailFuture = new CompletableFuture<>();
    private CompletableFuture<List<RoleNodeInfo>> emptyFuture = new CompletableFuture<>();
    private CompletableFuture<HttpResponse> errorResponse = new CompletableFuture<>();
    private CompletableFuture<HttpResponse> successResponse = new CompletableFuture<>();
    private CompletableFuture<BatchResponse> errorBatchResponse = new CompletableFuture<>();
    private CompletableFuture<BatchResponse> successBatchResponse = new CompletableFuture<>();
    private List<BatchRequest> lastBatchRequest = new ArrayList<>();

    public int fullChunkCount = 0;
    public int failChunkCount = 0;
    public int currentChunk = 0;
    public int errorResponseCount = 0;
    public int currentResponseCounter = 0;

    private AddSubjectRequest lastAddSubjectRequest;

    public List<RoleNodeInfo> nodesToReturn;
    public CompletableFuture<RoleNodesResponse> getRoleNodesResponse;
    public List<Long> requestsWithLastKey = new ArrayList<>();

    public static CompletableFuture<RoleNodesResponse> getRolenodesResponse(CompletableFuture<List<RoleNodeInfo>> future) {
        return future.thenApply(list -> new RoleNodesResponse(0, null, 0, null, list.size(), list));
    }

    public DummyIdmApiImpl(int retriesCount) {
        emptyFuture.complete(new ArrayList<>());
        successResponse.complete(new HttpResponse(200, "Success"));
        successBatchResponse.complete(new BatchResponse.Builder()
                .addStatusCode(200)
                .addResponse(new BatchResponse.Response("add_/ROOT/", 200, "OK"))
                .addResponse(new BatchResponse.Response("add_/ROOT/role/", 200, "OK"))
                .addResponse(new BatchResponse.Response("add_/ROOT/role/role/", 200, "OK"))
                .build());
        fullChunkCount = retriesCount;
    }

    @Override
    public CompletableFuture<RoleNodesResponse> getRoleNodesByOffset(int offset, int limit) {
        return getRoleNodes(offset, limit, -1);
    }

    @Override
    public CompletableFuture<RoleNodesResponse> getRoleNodesAfterNodeWithId(long lastKey, int limit) {
        requestsWithLastKey.add(lastKey);
        return getRoleNodes(-1, limit, lastKey);
    }

    private CompletableFuture<RoleNodesResponse> getRoleNodes(int offset, int limit, long lastKey) {
        LOG.info("getAllRoleNodes: offset {}, limit {}, lastKey {}", offset, limit, lastKey);
        ++currentChunk;
        if (currentChunk == fullChunkCount + failChunkCount + 1) {
            return getRolenodesResponse(emptyFuture);
        }
        if (currentChunk <= failChunkCount) {
            return getRolenodesResponse(roleNodesFailFuture);
        }
        if (getRoleNodesResponse != null) {
            return getRoleNodesResponse;
        }

        List<RoleNodeInfo> list;

        if (lastKey != -1) {
            if (nodesToReturn.stream().noneMatch(n -> n.getId() == lastKey)) {
                return CompletableFuture.failedFuture(new RuntimeException("BAD_REQUEST: incorrect key of sorting"));
            }
            list = nodesToReturn.stream().filter(n -> n.getId() > lastKey).limit(limit).collect(Collectors.toList());
        } else {
            list = nodesToReturn.stream().skip(offset).limit(limit).collect(Collectors.toList());
        }

        final String next = !list.isEmpty() && list.get(list.size() - 1).getId() != nodesToReturn.get(nodesToReturn.size() - 1).getId() ?
                "to be continued..." : null;
        return CompletableFuture.completedFuture(new RoleNodesResponse(limit, next, offset, null, nodesToReturn.size(), list));
    }

    @Override
    public CompletableFuture<HttpResponse> removeRoleNode(String path) {
        LOG.info("removeRoleNode: {}", path);
        ++currentResponseCounter;
        if (currentResponseCounter <= errorResponseCount) {
            return errorResponse;
        }

        return successResponse;
    }

    @Override
    public CompletableFuture<BatchResponse> runBatch(List<BatchRequest> requests) {
        LOG.info("runBatch: {}", requests);
        lastBatchRequest = requests;
        ++currentResponseCounter;
        if (currentResponseCounter <= errorResponseCount) {
            return errorBatchResponse;
        }

        return successBatchResponse;
    }

    @Override
    public CompletableFuture<HttpResponse> addRoleSubject(AddSubjectRequest request) {
        LOG.info("addRoleSubject: {}", request);
        lastAddSubjectRequest = request;
        ++currentResponseCounter;
        if (currentResponseCounter <= errorResponseCount) {
            return errorResponse;
        }

        return successResponse;
    }

    public void setFailChunkCount(int failChunkCount) {
        this.failChunkCount = failChunkCount;
    }

    public CompletableFuture<List<RoleNodeInfo>> getRoleNodesFailFuture() {
        return roleNodesFailFuture;
    }

    public void setErrorResponseCount(int errorResponseCount) {
        this.errorResponseCount = errorResponseCount;
    }

    public int getCurrentResponseCounter() {
        return currentResponseCounter;
    }

    public CompletableFuture<HttpResponse> getErrorResponse() {
        return errorResponse;
    }

    public CompletableFuture<BatchResponse> getErrorBatchResponse() {
        return errorBatchResponse;
    }

    public List<BatchRequest> getLastBatchRequest() {
        return lastBatchRequest;
    }

    public AddSubjectRequest getLastAddSubjectRequest() {
        return lastAddSubjectRequest;
    }
}
