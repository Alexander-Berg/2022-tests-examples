package com.yandex.teamcity.arc;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.yandex.teamcity.arc.grpc.ArcGrpcApi;
import com.yandex.teamcity.arc.grpc.ArcGrpcApiCache;
import com.yandex.teamcity.arc.monitoring.ChangesCountHolder;
import jetbrains.buildServer.util.ThreadUtil;
import jetbrains.buildServer.vcs.ModificationData;
import jetbrains.buildServer.vcs.VcsChangeInfo;
import jetbrains.buildServer.vcs.VcsException;
import jetbrains.buildServer.vcs.VcsFileData;
import jetbrains.buildServer.vcs.VcsRoot;
import jetbrains.buildServer.vcs.impl.VcsRootImpl;
import org.jetbrains.annotations.NotNull;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.teamcity.arc.client.ArcGrpcException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class TeamCityApiImplTest {
    private final TeamCityApi teamCityApi;
    private final VcsRoot repository = new VcsRootImpl(1L, "TestRoot");
    private final FakeBridge bridge;

    public TeamCityApiImplTest() throws IOException, VcsException {
        ArcGrpcApiCache arcGrpcApiCache = ArcTestUtil.provideArcGrpcApiCache();
        this.teamCityApi = new TeamCityApiImpl(arcGrpcApiCache);
        bridge = new FakeBridge();
    }

    @Test
    public void getCurrentStateTest() throws VcsException, ArcGrpcException {
        ArcGrpcApi arcGrpcApi = ArcTestUtil.provideArcGrpcApiCache().getArcGrpcApi();
        Map<String, String> state = teamCityApi.getCurrentState(repository, bridge);
        assertTrue(state.size() > 2);
        assertTrue(state.containsKey("arcadia/trunk"));
        verify(arcGrpcApi, times(1)).listRefs(bridge.getAllowedBranchPrefixes());
    }

    @Test
    public void getCurrentStateTest2() throws VcsException {
        FakeBridge withCache = new FakeBridge();
        withCache.setUseGetCurrentStateBatchCache(true);
        Map<String, String> state = teamCityApi.getCurrentState(repository, withCache);
        assertTrue(state.size() > 2);
        assertTrue(state.containsKey("arcadia/trunk"));
    }

    @Test
    public void getCurrentStateNormalBlocking() throws ArcGrpcException, VcsException {
        VcsRoot testRoot = new VcsRootImpl(2L, "TestRoot");
        ArcGrpcApi arcGrpcApi = ArcTestUtil.provideArcGrpcApiCache().getArcGrpcApi();
        TeamCityApi teamCityApi = this.teamCityApi;
        Runnable first = createCurrentStateRunnable(teamCityApi, testRoot, bridge);
        Runnable second = createCurrentStateRunnable(teamCityApi, testRoot, bridge);

        Thread thread1 = new Thread(first);
        thread1.start();
        Thread thread2 = new Thread(second);
        thread2.start();
        try {
            thread1.join();
            thread2.join();
        } catch (InterruptedException ignored) { }
        verify(arcGrpcApi, times(2)).listRefs(bridge.getAllowedBranchPrefixes()); //both request fully fetched
    }

    @Test
    public void getCurrentStateBatchCacheTwoRequests() throws ArcGrpcException, VcsException {
        VcsRoot testRoot = new VcsRootImpl(3L, "TestRoot");
        ArcGrpcApi arcGrpcApi = ArcTestUtil.provideArcGrpcApiCache().getArcGrpcApi();
        TeamCityApi teamCityApi = this.teamCityApi;
        FakeBridge withCache = new FakeBridge();
        withCache.setUseGetCurrentStateBatchCache(true);
        Runnable first = createCurrentStateRunnable(teamCityApi, testRoot, withCache);
        Runnable second = createCurrentStateRunnable(teamCityApi, testRoot, withCache);

        Thread thread1 = new Thread(first);
        thread1.start();
        ThreadUtil.sleep(50);
        Thread thread2 = new Thread(second);
        thread2.start();

        try {
            thread1.join();
            thread2.join();
        } catch (InterruptedException ignored) {}

        verify(arcGrpcApi, times(2)).listRefs(bridge.getAllowedBranchPrefixes()); //both request fully fetched
    }

    @Test
    public void getCurrentStateBatchCacheThreeRequests() throws ArcGrpcException, VcsException {
        VcsRoot testRoot = new VcsRootImpl(4L, "TestRoot");
        ArcGrpcApi arcGrpcApi = ArcTestUtil.provideArcGrpcApiCache().getArcGrpcApi();
        TeamCityApi teamCityApi = this.teamCityApi;
        FakeBridge withCache = new FakeBridge();
        withCache.setUseGetCurrentStateBatchCache(true);
        Runnable first = createCurrentStateRunnable(teamCityApi, testRoot, withCache);
        Runnable second = createCurrentStateRunnable(teamCityApi, testRoot, withCache);
        Runnable third = createCurrentStateRunnable(teamCityApi, testRoot, withCache);

        Thread thread1 = new Thread(first);
        thread1.start();
        ThreadUtil.sleep(50);
        Thread thread2 = new Thread(second);
        thread2.start();
        ThreadUtil.sleep(50);
        Thread thread3 = new Thread(third);
        thread3.start();

        try {
            thread1.join();
            thread2.join();
            thread3.join();
        } catch (InterruptedException ignored) {}

        verify(arcGrpcApi, times(2)).listRefs(bridge.getAllowedBranchPrefixes()); //first and second request fully fetched, third cached
    }

    @Test
    public void startStopPrefetchThreadForStateTest() throws VcsException, ArcGrpcException {
        Map<String, String> state = null;
        FakeBridge withPrefetch = new FakeBridge();
        withPrefetch.setUseBranchPrefetchThread(true);
        state = teamCityApi.getCurrentState(repository, withPrefetch);
        assertTrue(state.size() > 2);
        assertTrue(state.containsKey("arcadia/trunk"));
        state = null;

        ThreadUtil.sleep(5);
        ArcGrpcApi arcGrpcApi = ArcTestUtil.provideArcGrpcApiCache().getArcGrpcApi();
        state = teamCityApi.getCurrentState(repository, withPrefetch);
        assertTrue(state.size() > 2);
        assertTrue(state.containsKey("arcadia/trunk"));
        verify(arcGrpcApi, times(0)).listRefs(bridge.getAllowedBranchPrefixes());
        state = null;

        FakeBridge withOutPrefetch = new FakeBridge();
        withOutPrefetch.setUseBranchPrefetchThread(false);
        state = teamCityApi.getCurrentState(repository, withOutPrefetch);
        assertTrue(state.size() > 3);
        assertTrue(state.containsKey("arcadia/trunk"));
    }

    @Ignore("OOM on JDK8")
    @Test
    public void getCurrentStateWithoutFilterByPrefixTest() throws VcsException, ArcGrpcException {
        ArcGrpcApi arcGrpcApi = ArcTestUtil.provideArcGrpcApiCache().getArcGrpcApi();
        FakeBridge withoutPrefix = new FakeBridge();
        withoutPrefix.setAllowedBranchPrefixes("");
        Map<String, String> state = teamCityApi.getCurrentState(repository, withoutPrefix);
        assertTrue(state.size() > 2);
        assertTrue(state.containsKey("arcadia/trunk"));
        verify(arcGrpcApi, times(1)).listRefs();
    }

    @Test
    public void getCurrentStateFilterByPrefixTest() throws VcsException, ArcGrpcException {
        ArcGrpcApi arcGrpcApi = ArcTestUtil.provideArcGrpcApiCache().getArcGrpcApi();
        FakeBridge withPrefix = new FakeBridge();
        Map<String, String> state = teamCityApi.getCurrentState(repository, withPrefix);
        assertTrue(state.size() > 2);
        assertTrue(state.containsKey("arcadia/trunk"));
        String duplicate = "tags/groups/sdg/pipeline";
        List<String> prefixes = Arrays.asList("trunk", "release", duplicate, duplicate);
        verify(arcGrpcApi, times(1)).listRefs(prefixes);
    }

    @Test
    public void getCurrentStateFilterByPrefixParallelTest() throws VcsException, ArcGrpcException {
        ArcGrpcApi arcGrpcApi = ArcTestUtil.provideArcGrpcApiCache().getArcGrpcApi();
        FakeBridge withPrefix = new FakeBridge();
        withPrefix.setThreadPoolSizeForFetchBranches(2);
        Map<String, String> state = teamCityApi.getCurrentState(repository, withPrefix);
        assertTrue(state.size() > 2);
        assertTrue(state.containsKey("arcadia/trunk"));
        verify(arcGrpcApi, times(1)).listRefs(Arrays.asList("trunk"));
        verify(arcGrpcApi, times(1)).listRefs(Arrays.asList("release"));
        verify(arcGrpcApi, times(2)).listRefs(Arrays.asList("tags/groups/sdg/pipeline"));
    }


    @NotNull
    private Runnable createCurrentStateRunnable(TeamCityApi teamCityApi,
                                                VcsRoot testRoot,
                                                TeamcityPropertiesBridge bridge) {
        return () -> {
            try {
                teamCityApi.getCurrentState(testRoot, bridge);
            } catch (VcsException e) {
                throw new RuntimeException(e);
            }
        };
    }

    @Test
    public void collectChangesOverGrpcTest() throws VcsException {
        long changesCountBefore = ChangesCountHolder.getProcessedChangesCount();
        Map<String, String> from = new HashMap<>();
        from.put("arcadia/trunk", "bf5d876bc8529844f404f36ef89d463a2cc469ee");
        Map<String, String> to = new HashMap<>();
        to.put("arcadia/trunk", "0d30083b91ca44017bdd020a66050941ce518183");
        List<ModificationData> changes = teamCityApi.collectChangeOverGrpc(repository, from, to, bridge);

        assertEquals(1, changes.size());
        assertEquals(1, changes.get(0).getChangeCount());
        assertEquals("uzhas", changes.get(0).getUserName());
        assertEquals("yql/ci/teamcity/teamcity_yql_yc_test_deploy.py",
                changes.get(0).getChanges().get(0).getFileName());
        assertEquals(VcsChangeInfo.Type.CHANGED, changes.get(0).getChanges().get(0).getType());
        assertEquals(changesCountBefore + changes.size(), ChangesCountHolder.getProcessedChangesCount());
    }

    @Test
    public void collectChangesOverGrpcTest1() throws VcsException {
        Map<String, String> from = new HashMap<>();
        from.put("arcadia/trunk", "0e4d59c63ea23f297de54d6cf075a1c1e8ec8851");
        Map<String, String> to = new HashMap<>();
        to.put("arcadia/trunk", "bed4fa4d3e04f7ed1c91839107a8925630b453b3");
        List<ModificationData> changes = teamCityApi.collectChangeOverGrpc(repository, from, to, bridge);
        assertEquals(1, changes.size());
        assertEquals(8, changes.get(0).getChangeCount());
        assertEquals("ppalex", changes.get(0).getUserName());
        assertEquals(1, changes.get(0).getParentRevisions().size());
        assertEquals("0e4d59c63ea23f297de54d6cf075a1c1e8ec8851", changes.get(0).getParentRevisions().get(0));

        String removedFileName = "direct/libs/banner-system-client/src/main/java/ru/yandex/direct/bannersystem" +
                "/container/domainmarketrating/BsImportDomainMarketRatingData.java";
        String addedFileName = "direct/libs/banner-system-client/src/main/java/ru/yandex/direct/bannersystem/handle" +
                "/BsJsonFormHandleSpec.java";

        assertTrue(changes.get(0).getChanges()
                .stream()
                .anyMatch(change -> VcsChangeInfo.Type.REMOVED == change.getType() &&
                        removedFileName.equals(change.getFileName())));

        assertTrue(changes.get(0).getChanges()
                .stream()
                .anyMatch(change -> VcsChangeInfo.Type.ADDED == change.getType() &&
                        addedFileName.equals(change.getFileName())));
    }

    @Test
    public void collectChangesForRebaseBranchOverGrpcTest() throws VcsException {
        Map<String, String> from = Mockito.spy(new HashMap<>());
        String rebase_branch = "arcadia/users/thevery/1095036/rebase";
        from.put(rebase_branch, "4ca678124a6af174b41a2a10993b52b66c518526"); // three commits ago
        Map<String, String> to = new HashMap<>();
        to.put(rebase_branch, "ba44233981de05a9cb5105074fd9eee55e1d09ca");
        List<ModificationData> changes = teamCityApi.collectChangeOverGrpc(repository, from, to, bridge);
        assertEquals(3, changes.size());
        verify(from).get(rebase_branch);
    }

    @Test
    public void collectChangesForNotChangedRebaseBranchOverGrpcTest() throws VcsException {
        Map<String, String> from = Mockito.spy(new HashMap<>());
        String rebase_branch = "arcadia/users/thevery/1095036/rebase";
        from.put(rebase_branch, "ba44233981de05a9cb5105074fd9eee55e1d09ca");
        Map<String, String> to = new HashMap<>();
        to.put(rebase_branch, "ba44233981de05a9cb5105074fd9eee55e1d09ca");
        VcsRoot root = new VcsRootImpl(1, "TestRoot");
        List<ModificationData> changes = teamCityApi.collectChangeOverGrpc(root, from, to, bridge);
        assertEquals(0, changes.size());
        verify(from).get(rebase_branch);
    }

    @Test
    public void collectChangesParallelTest() throws VcsException {
        long changesCountBefore = ChangesCountHolder.getProcessedChangesCount();
        Map<String, String> from = new HashMap<>();
        from.put("arcadia/trunk", "bf5d876bc8529844f404f36ef89d463a2cc469ee");
        Map<String, String> to = new HashMap<>();
        to.put("arcadia/trunk", "0d30083b91ca44017bdd020a66050941ce518183");
        List<ModificationData> changes = teamCityApi.collectChangeParallel(repository, from, to, bridge);

        assertEquals(1, changes.size());
        assertEquals(1, changes.get(0).getChangeCount());
        assertEquals("uzhas", changes.get(0).getUserName());
        assertEquals("yql/ci/teamcity/teamcity_yql_yc_test_deploy.py",
                changes.get(0).getChanges().get(0).getFileName());
        assertEquals(VcsChangeInfo.Type.CHANGED, changes.get(0).getChanges().get(0).getType());
        assertEquals(changesCountBefore + changes.size(), ChangesCountHolder.getProcessedChangesCount());
    }

    @Test
    public void collectChangesParallelTest1() throws VcsException {
        Map<String, String> from = new HashMap<>();
        from.put("arcadia/trunk", "0e4d59c63ea23f297de54d6cf075a1c1e8ec8851");
        Map<String, String> to = new HashMap<>();
        to.put("arcadia/trunk", "bed4fa4d3e04f7ed1c91839107a8925630b453b3");
        List<ModificationData> changes = teamCityApi.collectChangeParallel(repository, from, to, bridge);
        assertEquals(1, changes.size());
        assertEquals(8, changes.get(0).getChangeCount());
        assertEquals("ppalex", changes.get(0).getUserName());
        assertEquals(1, changes.get(0).getParentRevisions().size());
        assertEquals("0e4d59c63ea23f297de54d6cf075a1c1e8ec8851", changes.get(0).getParentRevisions().get(0));

        String removedFileName = "direct/libs/banner-system-client/src/main/java/ru/yandex/direct/bannersystem" +
                "/container/domainmarketrating/BsImportDomainMarketRatingData.java";
        String addedFileName = "direct/libs/banner-system-client/src/main/java/ru/yandex/direct/bannersystem/handle" +
                "/BsJsonFormHandleSpec.java";

        assertTrue(changes.get(0).getChanges()
                .stream()
                .anyMatch(change -> VcsChangeInfo.Type.REMOVED == change.getType() &&
                        removedFileName.equals(change.getFileName())));

        assertTrue(changes.get(0).getChanges()
                .stream()
                .anyMatch(change -> VcsChangeInfo.Type.ADDED == change.getType() &&
                        addedFileName.equals(change.getFileName())));
    }

    @Test
    public void collectChangesForRebaseBranchParallelTest() throws VcsException {
        Map<String, String> from = Mockito.spy(new HashMap<>());
        String rebase_branch = "arcadia/users/thevery/1095036/rebase";
        from.put(rebase_branch, "4ca678124a6af174b41a2a10993b52b66c518526"); // three commits ago
        Map<String, String> to = new HashMap<>();
        to.put(rebase_branch, "ba44233981de05a9cb5105074fd9eee55e1d09ca");
        List<ModificationData> changes = teamCityApi.collectChangeParallel(repository, from, to, bridge);
        assertEquals(3, changes.size());
        verify(from).get(rebase_branch);
    }

    @Test
    public void collectChangesForNotChangedRebaseBranchParallelTest() throws VcsException {
        Map<String, String> from = Mockito.spy(new HashMap<>());
        String rebase_branch = "arcadia/users/thevery/1095036/rebase";
        from.put(rebase_branch, "ba44233981de05a9cb5105074fd9eee55e1d09ca");
        Map<String, String> to = new HashMap<>();
        to.put(rebase_branch, "ba44233981de05a9cb5105074fd9eee55e1d09ca");
        VcsRoot root = new VcsRootImpl(1, "TestRoot");
        List<ModificationData> changes = teamCityApi.collectChangeParallel(root, from, to, bridge);
        assertEquals(0, changes.size());
        verify(from).get(rebase_branch);
    }

    @Test
    public void getContentOverGrpcTest() throws VcsException {
        byte[] content = teamCityApi.getContentOverGrpc("devtools/dummy_arcadia/hello_go/ya.make",
                "164e7a26e4f44bdc4ac50973499902aa619805e2", bridge.getFileContentLimit());
        assertEquals("OWNER(g:yatool)\n" +
                        "\n" +
                        "GO_PROGRAM()\n" +
                        "\n" +
                        "PEERDIR(\n" +
                        "    contrib/go/src/fmt\n" +
                        ")\n" +
                        "\n" +
                        "SRCS(\n" +
                        "    main.go\n" +
                        ")\n" +
                        "\n" +
                        "END()\n",
                new String(content));

    }

    @Test(expected = VcsException.class)
    public void getContentOverGrpcTest1() throws VcsException {
        teamCityApi.getContentOverGrpc("junk/.arcignore", "not_a_real_commit", bridge.getFileContentLimit());
    }

    @Test(expected = VcsException.class)
    public void getContentOverGrpcTest2() throws VcsException {
        teamCityApi.getContentOverGrpc("not_a_real_file", "ef3868033f58379a8e27bacd720e6a07b23132b0", bridge.getFileContentLimit());
    }

    @Test
    public void listFilesOverGrpcTest() throws VcsException {
        Collection<VcsFileData> files = teamCityApi.listFilesOverGrpc("");
        assertTrue(files.size() > 2);
        //checking arc directory
        assertTrue(files.stream().anyMatch(file -> file.isDirectory() && "arc".equals(file.getName())));
        //checking robot directory
        assertTrue(files.stream().anyMatch(file -> file.isDirectory() && "robot".equals(file.getName())));
    }

    @Test
    public void collectChangesWhenProcessedBranchLimitDisabled() throws VcsException {
        Map<String, String> from = new HashMap<>();
        from.put("arcadia/trunk", "0e4d59c63ea23f297de54d6cf075a1c1e8ec8851");
        Map<String, String> to = new HashMap<>();
        to.put("arcadia/trunk", "bed4fa4d3e04f7ed1c91839107a8925630b453b3");

        List<ModificationData> changes = teamCityApi.collectChangeOverGrpc(repository, from, to, bridge);
        assertEquals(1, changes.size());
    }

    @Test(expected = VcsException.class)
    public void collectChangesWhenProcessedBranchLimitNotSatisfied() throws VcsException {
        FakeBridge notSatisfied = new FakeBridge();
        notSatisfied.setProcessedBranchLimitEnabled(true);
        notSatisfied.setProcessedBranchLimitValue(0);
        Map<String, String> from = new HashMap<>();
        from.put("arcadia/trunk", "0e4d59c63ea23f297de54d6cf075a1c1e8ec8851");
        Map<String, String> to = new HashMap<>();
        to.put("arcadia/trunk", "bed4fa4d3e04f7ed1c91839107a8925630b453b3");

        teamCityApi.collectChangeOverGrpc(repository, from, to, notSatisfied);
    }

    @Test
    public void collectChangesWhenProcessedBranchLimitSatisfied() throws VcsException {
        FakeBridge satisfied = new FakeBridge();
        satisfied.setProcessedBranchLimitEnabled(true);
        satisfied.setProcessedBranchLimitValue(2);
        Map<String, String> from = new HashMap<>();
        from.put("arcadia/trunk", "0e4d59c63ea23f297de54d6cf075a1c1e8ec8851");
        Map<String, String> to = new HashMap<>();
        to.put("arcadia/trunk", "bed4fa4d3e04f7ed1c91839107a8925630b453b3");

        List<ModificationData> changes = teamCityApi.collectChangeOverGrpc(repository, from, to, satisfied);
        assertEquals(1, changes.size());
    }

    @Test
    public void collectChangesParallelWhenProcessedBranchLimitDisabled() throws VcsException {
        Map<String, String> from = new HashMap<>();
        from.put("arcadia/trunk", "0e4d59c63ea23f297de54d6cf075a1c1e8ec8851");
        Map<String, String> to = new HashMap<>();
        to.put("arcadia/trunk", "bed4fa4d3e04f7ed1c91839107a8925630b453b3");

        List<ModificationData> changes = teamCityApi.collectChangeParallel(repository, from, to, bridge);
        assertEquals(1, changes.size());
    }

    @Test(expected = VcsException.class)
    public void collectChangesParallelWhenProcessedBranchLimitNotSatisfied() throws VcsException {
        FakeBridge notSatisfied = new FakeBridge();
        notSatisfied.setProcessedBranchLimitEnabled(true);
        notSatisfied.setProcessedBranchLimitValue(0);
        Map<String, String> from = new HashMap<>();
        from.put("arcadia/trunk", "0e4d59c63ea23f297de54d6cf075a1c1e8ec8851");
        Map<String, String> to = new HashMap<>();
        to.put("arcadia/trunk", "bed4fa4d3e04f7ed1c91839107a8925630b453b3");

        teamCityApi.collectChangeParallel(repository, from, to, notSatisfied);
    }

    @Test
    public void collectChangesParallelWhenProcessedBranchLimitSatisfied() throws VcsException {
        FakeBridge satisfied = new FakeBridge();
        satisfied.setProcessedBranchLimitEnabled(true);
        satisfied.setProcessedBranchLimitValue(2);
        Map<String, String> from = new HashMap<>();
        from.put("arcadia/trunk", "0e4d59c63ea23f297de54d6cf075a1c1e8ec8851");
        Map<String, String> to = new HashMap<>();
        to.put("arcadia/trunk", "bed4fa4d3e04f7ed1c91839107a8925630b453b3");

        List<ModificationData> changes = teamCityApi.collectChangeParallel(repository, from, to, satisfied);
        assertEquals(1, changes.size());
    }
}
