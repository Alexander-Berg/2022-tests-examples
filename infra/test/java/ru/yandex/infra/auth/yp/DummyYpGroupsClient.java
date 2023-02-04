package ru.yandex.infra.auth.yp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import ru.yandex.yp.model.YpTransaction;

public class DummyYpGroupsClient implements YpGroupsClient {
    public Set<String> createdGroup = new HashSet<>();

    public CompletableFuture<?> addGroupResponse = new CompletableFuture<>();
    public CompletableFuture<?> removeGroupResponse = new CompletableFuture<>();
    public CompletableFuture<?> removeGroupsResponse = new CompletableFuture<>();
    public CompletableFuture<?> addMembersResponse = new CompletableFuture<>();
    public CompletableFuture<?> removeMembersResponse = new CompletableFuture<>();
    public CompletableFuture<?> updateMembersResponse = new CompletableFuture<>();
    public CompletableFuture<Map<String, YpGroup>> getGroupsByLabelsResponse = new CompletableFuture<>();
    public CompletableFuture<Boolean> existsResponse;
    public CompletableFuture<Map<String, Set<String>>> getGroupsWithPrefixResponse = new CompletableFuture<>();
    public CompletableFuture<List<YpAbcRoleGroup>> getAbcRoleGroupsResponse = new CompletableFuture<>();
    public Map<String, List<String>> getMembersResponse;
    public CompletableFuture<Set<String>> getAllIdsResponse = new CompletableFuture<>();

    public String lastAddedGroup;
    public String lastRemovedGroup;
    public String lastUpdatedGroup;
    public String lastCheckedGroup;
    public String lastRequestedPrefix;
    public Collection<String> lastRemovedGroups;
    public Set<String> lastMembers;
    public Map<String, Object> lastLabels = new HashMap<>();
    public Map<String, Set<String>> lastMembersUpdatesByGroupId = new HashMap<>();

    public int errorResponseCount = 0;
    public int currentResponseCounter = 0;
    CompletableFuture<?> errorResponse;

    @Override
    public CompletableFuture<?> addGroup(String id, Set<String> members, YpTransaction transaction, Map<String,
            Object> labels) {
        ++currentResponseCounter;
        if (currentResponseCounter <= errorResponseCount) {
            return errorResponse;
        }

        lastAddedGroup = id;
        lastMembers = members;
        createdGroup.add(id);
        lastLabels = labels;
        lastMembersUpdatesByGroupId.put(id, members);
        return addGroupResponse;
    }

    @Override
    public CompletableFuture<?> removeGroup(String id, YpTransaction transaction) {
        if (!createdGroup.contains(id)) {
            return CompletableFuture.failedFuture(new RuntimeException());
        }
        lastRemovedGroup = id;

        return removeGroupResponse;
    }

    @Override
    public CompletableFuture<?> removeGroup(String id) {
        return removeGroup(id, null);
    }

    @Override
    public CompletableFuture<?> removeGroups(Collection<String> ids) {
        lastRemovedGroups = new ArrayList<>(ids);
        return removeGroupsResponse;
    }

    @Override
    public CompletableFuture<?> addMembers(String groupId, Set<String> membersId, YpTransaction transaction) {
        ++currentResponseCounter;
        if (currentResponseCounter <= errorResponseCount) {
            return errorResponse;
        }

        if (!createdGroup.contains(groupId)) {
            return CompletableFuture.failedFuture(new RuntimeException());
        }
        lastUpdatedGroup = groupId;
        lastMembers = membersId;
        lastMembersUpdatesByGroupId.put(groupId, membersId);
        return addMembersResponse;
    }

    @Override
    public CompletableFuture<?> removeMembers(String groupId, Set<String> membersId, YpTransaction transaction) {
        ++currentResponseCounter;
        if (currentResponseCounter <= errorResponseCount) {
            return errorResponse;
        }

        if (!createdGroup.contains(groupId)) {
            return CompletableFuture.failedFuture(new RuntimeException());
        }
        lastUpdatedGroup = groupId;
        lastMembers = membersId;
        lastMembersUpdatesByGroupId.put(groupId, membersId);
        return removeMembersResponse;
    }

    @Override
    public CompletableFuture<?> updateMembers(String groupId, Set<String> memberIds, YpTransaction transaction) {
        return updateMembers(groupId, memberIds);
    }

    @Override
    public CompletableFuture<?> updateMembers(String groupId, Set<String> memberIds) {
        ++currentResponseCounter;
        if (currentResponseCounter <= errorResponseCount) {
            return errorResponse;
        }

        if (!createdGroup.contains(groupId)) {
            return CompletableFuture.failedFuture(new RuntimeException());
        }
        lastUpdatedGroup = groupId;
        lastMembers = memberIds;
        lastMembersUpdatesByGroupId.put(groupId, memberIds);
        return updateMembersResponse;
    }

    @Override
    public CompletableFuture<Boolean> exists(String groupId, YpTransaction transaction) {
        lastCheckedGroup = groupId;
        return existsResponse != null ? existsResponse : CompletableFuture.completedFuture(createdGroup.contains(groupId));
    }

    @Override
    public CompletableFuture<Map<String, Set<String>>> getGroupsWithPrefix(String prefix) {
        lastRequestedPrefix = prefix;
        return getGroupsWithPrefixResponse;
    }

    @Override
    public CompletableFuture<Map<String, Set<String>>> getGroupsWithLabels(Map<String, String> labels) {
        String prefix = labels.getOrDefault("system", "") + ":" + labels.getOrDefault("project_id", "");
        return getGroupsWithPrefix(prefix);
    }

    @Override
    public CompletableFuture<Map<String, Set<String>>> getGroupsWithLabels(Map<String, String> labels, long transactionTimestamp) {
        return getGroupsWithLabels(labels);
    }

    @Override
    public CompletableFuture<List<String>> getMembers(String groupId, YpTransaction transaction) {
        return CompletableFuture.completedFuture(getMembersResponse.get(groupId));
    }

    @Override
    public CompletableFuture<Map<String, YpGroup>> getGroupsByLabels(Map<String, String> labels, Optional<Long> transactionTimestamp) {
        return getGroupsByLabelsResponse;
    }

    @Override
    public CompletableFuture<Set<String>> listAllIds() {
        return getAllIdsResponse;
    }

    @Override
    public CompletableFuture<List<YpAbcRoleGroup>> getAbcRoleGroups() {
        return getAbcRoleGroupsResponse;
    }
}
