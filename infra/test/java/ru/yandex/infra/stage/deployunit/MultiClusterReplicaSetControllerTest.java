package ru.yandex.infra.stage.deployunit;

import ru.yandex.infra.controller.dto.SchemaMeta;
import ru.yandex.infra.stage.TestData;
import ru.yandex.infra.stage.yp.AclUpdater;
import ru.yandex.infra.stage.yp.ObjectLifeCycleManager;
import ru.yandex.yp.client.api.TDeployProgress;
import ru.yandex.yp.client.api.TMultiClusterReplicaSetSpec;
import ru.yandex.yp.client.api.TMultiClusterReplicaSetStatus;

class MultiClusterReplicaSetControllerTest extends ReplicaSetControllerTestBase<TMultiClusterReplicaSetSpec, TMultiClusterReplicaSetStatus, Long> {
    @Override
    protected ReplicaSetControllerBase<TMultiClusterReplicaSetSpec, TMultiClusterReplicaSetStatus, Long> createController(
            String id, ObjectLifeCycleManager<SchemaMeta, TMultiClusterReplicaSetSpec, TMultiClusterReplicaSetStatus> manager,
            AclUpdater aclUpdater) {
        return new MultiClusterReplicaSetController(id, manager, (v) -> {
        }, TestData.CONVERTER, aclUpdater, TestData.DEFAULT_STAGE_FQID, relationController);
    }

    @Override
    protected TMultiClusterReplicaSetSpec generateSpec(int podsTotal, int revision) {
        return TMultiClusterReplicaSetSpec.newBuilder()
                .setRevision(revision)
                .addClusters(TMultiClusterReplicaSetSpec.TClusterReplicaSetSpecPreferences.newBuilder()
                        .setCluster(TestData.CLUSTER)
                        .setSpec(TMultiClusterReplicaSetSpec.TReplicaSetSpecPreferences.newBuilder().setReplicaCount(podsTotal).build())
                        .build())
                .build();
    }

    @Override
    protected TMultiClusterReplicaSetStatus generateStatus(int podsTotal, int revision) {
        TDeployProgress progressInStatus = TDeployProgress.newBuilder()
                .setPodsTotal(podsTotal)
                .build();

        TMultiClusterReplicaSetStatus.TPodsSummary summary = TMultiClusterReplicaSetStatus.TPodsSummary.newBuilder()
                .putClustersProgress(TestData.CLUSTER, progressInStatus)
                .setMultiClusterProgress(progressInStatus)
                .build();

        return TMultiClusterReplicaSetStatus.newBuilder()
                .setCurrentRevisionSummary(summary)
                .putRevisions(revision, summary)
                .build();
    }
}
