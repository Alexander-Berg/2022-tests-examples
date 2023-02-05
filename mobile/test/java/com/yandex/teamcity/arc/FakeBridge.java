package com.yandex.teamcity.arc;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class FakeBridge implements TeamcityPropertiesBridge {

    private boolean collectingChangesDisabled;
    private boolean allOperationDisabled;
    private boolean useAllowVcsRootList;
    private Set<Long> allowedVcsRoots = Collections.emptySet();
    private boolean useGetCurrentStateBatchCache;
    private boolean processedBranchLimitEnabled;
    private long processedBranchLimitValue;
    private List<String> allowedBranchPrefixes = Arrays.asList(
            "trunk",
            "release",
            "tags/groups/sdg/pipeline", //intentionally duplicated
            "tags/groups/sdg/pipeline"
    );
    private boolean useBranchPrefetchThread;
    private int threadPoolSizeForFetchBranches = -1;
    private boolean parallelizeCollectingChanges;

    protected void setCollectingChangesDisabled() {
        this.collectingChangesDisabled = true;
    }

    protected void setAllOperationDisabled() {
        this.allOperationDisabled = true;
    }

    protected void setUseAllowVcsRootList(boolean useAllowVcsRootList) {
        this.useAllowVcsRootList = useAllowVcsRootList;
    }

    protected void setAllowedVcsRoots(Set<Long> allowedVcsRoots) {
        this.allowedVcsRoots = allowedVcsRoots;
    }

    protected void setUseGetCurrentStateBatchCache(boolean useGetCurrentStateBatchCache) {
        this.useGetCurrentStateBatchCache = useGetCurrentStateBatchCache;
    }

    protected void setProcessedBranchLimitEnabled(boolean processedBranchLimitEnabled) {
        this.processedBranchLimitEnabled = processedBranchLimitEnabled;
    }

    protected void setProcessedBranchLimitValue(long processedBranchLimitValue) {
        this.processedBranchLimitValue = processedBranchLimitValue;
    }

    protected void setAllowedBranchPrefixes(String allowedBranchPrefixes) {
        if ("".equals(allowedBranchPrefixes)) {
            this.allowedBranchPrefixes = Collections.emptyList();
        } else {
            this.allowedBranchPrefixes = Arrays.asList(allowedBranchPrefixes.split(","));
        }
    }

    protected void setUseBranchPrefetchThread(boolean useBranchPrefetchThread) {
        this.useBranchPrefetchThread = useBranchPrefetchThread;
    }

    protected void setThreadPoolSizeForFetchBranches(int threadPoolSizeForFetchBranches) {
        this.threadPoolSizeForFetchBranches = threadPoolSizeForFetchBranches;
    }

    protected void setParallelizeCollectingChanges(boolean parallelizeCollectingChanges) {
        this.parallelizeCollectingChanges = parallelizeCollectingChanges;
    }

    @Override
    public boolean isChangesCollectingDisabled() {
        return collectingChangesDisabled;
    }

    @Override
    public boolean isAllOperationDisabled() {
        return allOperationDisabled;
    }

    @Override
    public boolean useAllowVcsRootList() {
        return useAllowVcsRootList;
    }

    @Override
    public Set<Long> getAllowedVcsRoots() {
        return allowedVcsRoots;
    }

    @Override
    public boolean useGetCurrentStateBatchCache() {
        return useGetCurrentStateBatchCache;
    }

    @Override
    public int getTrunkLogLimit() {
        return 1000;
    }

    @Override
    public int getSpecialBranchLogLimit() {
        return 3;
    }

    @Override
    public int getNormalBranchLogLimit() {
        return 10;
    }

    @Override
    public long getFileContentLimit() {
        return 10_000_000L;
    }

    @Override
    public boolean isProcessedBranchLimitEnabled() {
        return processedBranchLimitEnabled;
    }

    @Override
    public long getProcessedBranchLimit() {
        return processedBranchLimitValue;
    }

    @Override
    public List<String> getAllowedBranchPrefixes() {
        return allowedBranchPrefixes;
    }

    @Override
    public boolean useBranchPrefetchThread() {
        return useBranchPrefetchThread;
    }

    @Override
    public int getThreadPoolSizeForFetchBranches() {
        return threadPoolSizeForFetchBranches;
    }

    public boolean parallelizeCollectingChanges() {
        return parallelizeCollectingChanges;
    }

    @Override
    public int gettersCollectingByBranchPoolSize() {
        return 10;
    }
}
