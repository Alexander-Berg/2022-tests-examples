package com.yandex.teamcity.arc;

import java.util.HashSet;
import java.util.List;

import jetbrains.buildServer.vcs.CheckoutRules;
import jetbrains.buildServer.vcs.ModificationData;
import jetbrains.buildServer.vcs.RepositoryStateData;
import jetbrains.buildServer.vcs.VcsException;
import jetbrains.buildServer.vcs.VcsRoot;
import jetbrains.buildServer.vcs.impl.VcsRootImpl;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class DisablingCollectingChangesTests {
    private final TeamCityApi teamCityApi = new FakeTeamcityApi();
    private final VcsRoot repository = new VcsRootImpl(1L, "TestRoot");
    private final RepositoryStateData fakeVersionState = RepositoryStateData.createSingleVersionState("");
    private final CheckoutRules rules = CheckoutRules.DEFAULT;

    @Test
    public void testCollectingChangesSuccessfully() throws VcsException {
        FakeBridge bridge = new FakeBridge();
        TeamCityCollectChangesPolicy policy = new TeamCityCollectChangesPolicy(teamCityApi, bridge);
        List<ModificationData> result = policy.collectChanges(repository, fakeVersionState, fakeVersionState, rules);
        assertNotNull(result);
    }

    @Test
    public void testGetCurrentStateSuccessfully() throws VcsException {
        FakeBridge bridge = new FakeBridge();
        TeamCityCollectChangesPolicy policy = new TeamCityCollectChangesPolicy(teamCityApi, bridge);
        RepositoryStateData currentState = policy.getCurrentState(repository);
        assertNotNull(currentState.getBranchRevisions());
    }

    @Test(expected = VcsException.class)
    public void testCollectChangesDisabledByAdmin() throws VcsException {
        FakeBridge bridge = new FakeBridge();
        bridge.setCollectingChangesDisabled();
        TeamCityCollectChangesPolicy policy = new TeamCityCollectChangesPolicy(teamCityApi, bridge);
        policy.collectChanges(repository, fakeVersionState, fakeVersionState, rules);
    }

    @Test(expected = VcsException.class)
    public void testGetCurrentStateDisabledByAdmin() throws VcsException {
        FakeBridge bridge = new FakeBridge();
        bridge.setCollectingChangesDisabled();
        TeamCityCollectChangesPolicy policy = new TeamCityCollectChangesPolicy(teamCityApi, bridge);
        policy.getCurrentState(repository);
    }

    @Test(expected = VcsException.class)
    public void testGetCurrentStateForNotAllowedVcsRoot() throws VcsException {
        FakeBridge bridge = new FakeBridge();
        bridge.setUseAllowVcsRootList(true);
        TeamCityCollectChangesPolicy policy = new TeamCityCollectChangesPolicy(teamCityApi, bridge);
        policy.getCurrentState(repository);
    }

    @Test()
    public void testGetCurrentStateForAllowedVcsRoot() throws VcsException {
        FakeBridge bridge = new FakeBridge();
        bridge.setUseAllowVcsRootList(true);
        HashSet<Long> allowedVcsRoots = new HashSet<>();
        allowedVcsRoots.add(1L);
        bridge.setAllowedVcsRoots(allowedVcsRoots);
        TeamCityCollectChangesPolicy policy = new TeamCityCollectChangesPolicy(teamCityApi, bridge);
        RepositoryStateData currentState = policy.getCurrentState(repository);
        assertNotNull(currentState.getBranchRevisions());
    }

    @Test(expected = VcsException.class)
    public void testCollectChangesForNotAllowedVcsRoot() throws VcsException {
        FakeBridge bridge = new FakeBridge();
        bridge.setUseAllowVcsRootList(true);
        TeamCityCollectChangesPolicy policy = new TeamCityCollectChangesPolicy(teamCityApi, bridge);
        policy.collectChanges(repository, fakeVersionState, fakeVersionState, rules);
    }

    @Test
    public void testCollectChangesForAllowedVcsRoot() throws VcsException {
        FakeBridge bridge = new FakeBridge();
        bridge.setUseAllowVcsRootList(true);
        HashSet<Long> allowedVcsRoots = new HashSet<>();
        allowedVcsRoots.add(1L);
        bridge.setAllowedVcsRoots(allowedVcsRoots);
        TeamCityCollectChangesPolicy policy = new TeamCityCollectChangesPolicy(teamCityApi, bridge);
        List<ModificationData> result = policy.collectChanges(repository, fakeVersionState, fakeVersionState, rules);
        assertNotNull(result);
    }
}
