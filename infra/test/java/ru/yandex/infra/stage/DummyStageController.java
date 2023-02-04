package ru.yandex.infra.stage;

import java.util.Map;

import ru.yandex.infra.controller.dto.Acl;
import ru.yandex.infra.stage.deployunit.DeployUnitStats;
import ru.yandex.infra.stage.dto.ClusterAndType;
import ru.yandex.infra.stage.dto.RuntimeDeployControls;
import ru.yandex.infra.stage.dto.StageSpec;
import ru.yandex.infra.stage.yp.DeployObjectId;
import ru.yandex.infra.stage.yp.Retainment;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;

import static java.util.Collections.emptyMap;

public class DummyStageController implements StageController {
    public boolean wasSynced = false;
    public boolean wasRestored = false;
    public boolean wasGotStatus = false;
    public boolean wasShutdown = false;
    public boolean wasAddedMetric = false;
    public String lastProjectId;
    public String lastRestoredAccountId;
    public StageSpec lastSpec;
    public Acl lastAcl;
    public RuntimeDeployControls runtimeDeployControls;

    @Override
    public void sync(String stageFqid,
                     StageSpec newSpec,
                     RuntimeDeployControls runtimeDeployControls,
                     Map<String, YTreeNode> labels,
                     long specTimestamp,
                     Acl acl,
                     String projectId) {
        wasSynced = true;
        lastProjectId = projectId;
        lastSpec = newSpec;
        lastAcl = acl;
        this.runtimeDeployControls = runtimeDeployControls;
    }

    @Override
    public void restoreFromStatus(String stageFqid,
                                  int stageRevision,
                                  Status status,
                                  RuntimeDeployControls runtimeDeployControls,
                                  Map<String, YTreeNode> labels,
                                  String accountId, Acl acl,
                                  String projectId,
                                  Map<String, String> envVars) {
        wasRestored = true;
        lastProjectId = projectId;
        lastRestoredAccountId = accountId;
        lastAcl = acl;
        this.runtimeDeployControls = runtimeDeployControls;
    }

    @Override
    public Status getStatus() {
        wasGotStatus = true;
        return new Status(emptyMap(), emptyMap());
    }

    @Override
    public Retainment shouldRetain(DeployObjectId primitiveId, ClusterAndType clusterAndType) {
        return TestData.RETAINMENT;
    }

    @Override
    public void shutdown() {
        wasShutdown = true;
    }

    @Override
    public void addStats(DeployUnitStats.Builder builder) {
        wasAddedMetric = true;
    }
}
