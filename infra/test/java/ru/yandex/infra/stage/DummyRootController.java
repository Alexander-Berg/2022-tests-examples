package ru.yandex.infra.stage;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ru.yandex.bolts.collection.Try;
import ru.yandex.infra.controller.dto.SchemaMeta;
import ru.yandex.infra.controller.dto.StageMeta;
import ru.yandex.infra.controller.yp.YpObject;
import ru.yandex.yp.client.api.TProjectSpec;
import ru.yandex.yp.client.api.TProjectStatus;
import ru.yandex.yp.client.api.TStageSpec;
import ru.yandex.yp.client.api.TStageStatus;

public class DummyRootController implements RootController {

    public Runnable updateStatusesAction = null;
    public Runnable syncAction = null;

    public int updateStatusesCalls;
    public int syncCalls;
    public int processGcForRemovedStagesCalls;

    public Set<String> lastStageIdsWithDeployEngine;
    public Set<String> lastAllStageIds;
    public List<Map<String, Try<YpObject<StageMeta, TStageSpec, TStageStatus>>>> lastSyncStages = new ArrayList<>();
    public Map<String, Try<YpObject<SchemaMeta, TProjectSpec, TProjectStatus>>> lastSyncProjects;

    @Override
    public void beginStatisticsCollection() {

    }

    @Override
    public void buildStatistics() {

    }

    @Override
    public void updateStatuses() {
        updateStatusesCalls++;
        if (updateStatusesAction != null) {
            updateStatusesAction.run();
        }
    }

    @Override
    public void processGcForRemovedStages(Set<String> stageIdsWithDeployEngine, Set<String> allStageIds) {
        lastAllStageIds = allStageIds;
        lastStageIdsWithDeployEngine = stageIdsWithDeployEngine;
        processGcForRemovedStagesCalls++;
    }

    @Override
    public void sync(Map<String, Try<YpObject<StageMeta, TStageSpec, TStageStatus>>> currentSpecs,
                     Map<String, Try<YpObject<SchemaMeta, TProjectSpec, TProjectStatus>>> currentProjects) {
        lastSyncStages.add(currentSpecs);
        lastSyncProjects = currentProjects;
        syncCalls++;
        if (syncAction != null) {
            syncAction.run();
        }
    }

    public Set<String> getAllSyncedIds() {
        Set<String> result = new HashSet<>();
        lastSyncStages.forEach(map -> result.addAll(map.keySet()));
        return result;
    }
}
