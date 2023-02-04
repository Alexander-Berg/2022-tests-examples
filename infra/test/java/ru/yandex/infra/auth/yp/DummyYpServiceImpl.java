package ru.yandex.infra.auth.yp;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.infra.auth.Role;
import ru.yandex.infra.auth.RoleSubject;
import ru.yandex.infra.auth.RolesInfo;

public class DummyYpServiceImpl implements YpService {
    private static final Logger LOG = LoggerFactory.getLogger(DummyYpServiceImpl.class);

    public Set<RoleSubject> roleSubjects;
    public Map<String, Set<String>> memberships;
    public Map<Role, Set<String>> membershipsByRole;
    public Set<Role> roles;
    public YpClients ypClients;
    public Map<String, YpClients> ypSlaveClients;
    public String systemName;
    public Set<String> garbageGroups;

    public DummyYpServiceImpl() {
        this.roleSubjects = new HashSet<>();
        this.memberships = new HashMap<>();
        this.membershipsByRole = new HashMap<>();
    }

    @Override
    public Set<Role> getRoles() {
        LOG.info("getRoles");
        return roles;
    }

    @Override
    public Set<RoleSubject> getRoleSubjects() {
        LOG.info("getRoleSubjects");
        return roleSubjects;
    }

    @Override
    public Map<Role, Set<String>> getRoleMembers() {
        LOG.info("getRoleMembers");
        return membershipsByRole;
    }

    @Override
    public Map<String, Set<String>> getGroupsWithPrefix(String prefix) {
        LOG.info("getGroupsWithPrefix: {}", prefix);
        return memberships.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    public Map<String, Set<String>> getGroupsWithLabels(Map<String, String> labels) {
        LOG.info("getGroupsWithLabels: {}", labels);
        return memberships.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    public Map<String, Set<String>> getRoleMembers(RolesInfo.LevelName level, String objectId) {
        return null;
    }

    @Override
    public String getProjectIdFor(String nannyServiceId) {
        return null;
    }

    @Override
    public List<String> getNannyServices(String projectId) {
        return null;
    }

    @Override
    public void addRoleSubject(RoleSubject roleSubject) {
        LOG.info("addRoleSubject: {}", roleSubject);
        roleSubjects.add(roleSubject);
    }

    @Override
    public void updateRoleSubject(RoleSubject roleSubject) {
        LOG.info("updateRoleSubject: {}", roleSubject);
        roleSubjects.add(roleSubject);
    }

    @Override
    public void removeRoleSubject(RoleSubject roleSubject) {
        LOG.info("removeRoleSubject: {}", roleSubject);
        roleSubjects.remove(roleSubject);
    }

    @Override
    public void addMembersToGroup(String groupId, Set<String> loginsToAdd) {
        LOG.info("addMembersToGroup: {} {}", groupId, loginsToAdd);
        Set<String> oldSet = memberships.getOrDefault(groupId, new HashSet<>());
        oldSet.addAll(loginsToAdd);
        memberships.put(groupId, oldSet);
    }

    @Override
    public void removeMembersFromGroup(String groupId, Set<String> loginsToRemove) {
        LOG.info("removeMembersFromGroup: {} {}", groupId, loginsToRemove);
        Set<String> oldSet = memberships.getOrDefault(groupId, new HashSet<>());
        oldSet.removeAll(loginsToRemove);
        memberships.put(groupId, oldSet);
    }

    @Override
    public void syncGroupMembersToAllSlaveClusters(Map<String, String> labels) {
        LOG.info("syncGroupMembersToAllSlaveClusters: {}", labels);
    }

    @Override
    public void removeGroup(String groupId) {
        LOG.info("removeGroup: {} ", groupId);
    }

    @Override
    public CompletableFuture<?> removeRolesFromStageAcl(String stageId, Set<Role> removedRoles) {
        LOG.info("removeRolesFromStageAcl: stage '{}'", stageId);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void setGlobalCleanupFlag() {
        LOG.info("setGlobalCleanupFlag");
    }

    @Override
    public Set<String> getGarbageGroups() {
        LOG.info("getGarbageGroups");
        return garbageGroups;
    }

    @Override
    public YpClients getMasterClusterClients() {
        return ypClients;
    }

    @Override
    public Map<String, YpClients> getSlaveClusterClients() {
        return ypSlaveClients;
    }

    @Override
    public String getSystemName() {
        return systemName;
    }
}
