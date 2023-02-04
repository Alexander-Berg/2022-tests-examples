package ru.yandex.infra.auth.yp;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import ru.yandex.infra.auth.RolesInfo;
import ru.yandex.yp.model.YpTransaction;

import static java.util.Collections.emptyList;

public class DummyYpObjectPermissionsUpdater implements YpObjectPermissionsUpdater {
    public CompletableFuture<?> addResponse = new CompletableFuture<>();
    public CompletableFuture<?> updateResponse = new CompletableFuture<>();
    public CompletableFuture<?> removeRolesResponse = new CompletableFuture<>();
    public String lastUpdatedObjectId;
    public String lastAddedSubjectId;
    public List<RolesInfo.RoleAce> lastAddedAces;
    public int addRoleCallsCount;
    public Set<String> lastRemovedSubjectIds;

    public int removeRolesCallsCount;

    @Override
    public CompletableFuture<?> addRole(String objectId, String subjectId, List<RolesInfo.RoleAce> aces) {
        addRoleCallsCount++;
        lastUpdatedObjectId = objectId;
        lastAddedSubjectId = subjectId;
        lastAddedAces = aces;
        return addResponse;
    }

    @Override
    public CompletableFuture<?> addRole(String objectId, String subjectId, List<RolesInfo.RoleAce> aces,
            YpTransaction transaction) {
        return addRole(objectId, subjectId, aces);
    }

    @Override
    public CompletableFuture<?> updateRole(String objectId, String subjectId, List<RolesInfo.RoleAce> aces) {
        if (lastUpdatedObjectId != null && lastUpdatedObjectId.equals(objectId)
                && lastAddedSubjectId != null && lastAddedSubjectId.equals(subjectId))
        {
            lastAddedAces = aces;
        } else {
            lastAddedAces = emptyList();
        }
        return updateResponse;
    }

    @Override
    public CompletableFuture<?> removeRoles(String objectId, Set<String> subjectIds) {
        removeRolesCallsCount++;
        lastUpdatedObjectId = objectId;
        lastRemovedSubjectIds = subjectIds;
        return removeRolesResponse;
    }
}
