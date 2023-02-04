package ru.yandex.infra.stage.deployunit;

import ru.yandex.infra.controller.dto.SchemaMeta;
import ru.yandex.infra.stage.TestData;
import ru.yandex.infra.stage.yp.AclUpdater;
import ru.yandex.infra.stage.yp.ObjectLifeCycleManager;
import ru.yandex.yp.client.api.TReplicaSetSpec;
import ru.yandex.yp.client.api.TReplicaSetStatus;

class SingleReplicaSetControllerTest extends ReplicaSetControllerTestBase<TReplicaSetSpec, TReplicaSetStatus, String> {
    @Override
    protected ReplicaSetControllerBase<TReplicaSetSpec, TReplicaSetStatus, String> createController(
            String id, ObjectLifeCycleManager<SchemaMeta, TReplicaSetSpec, TReplicaSetStatus> manager,
            AclUpdater aclUpdater) {
        return new SingleReplicaSetController(id, manager, (v) -> {
        }, TestData.CONVERTER, aclUpdater, TestData.DEFAULT_STAGE_FQID, relationController);
    }

    @Override
    protected TReplicaSetSpec generateSpec(int podsTotal, int revision) {
        return TReplicaSetSpec.newBuilder()
                .setRevisionId(String.valueOf(revision))
                .setReplicaCount(podsTotal)
                .build();
    }

    @Override
    protected TReplicaSetStatus generateStatus(int podsTotal, int revision) {
        TReplicaSetStatus.TDeployProgress progress = TReplicaSetStatus.TDeployProgress.newBuilder()
                .setPodsTotal(podsTotal).build();

        return TReplicaSetStatus.newBuilder()
                .setCurrentRevisionProgress(progress)
                .putRevisionsProgress(String.valueOf(revision), progress)
                .build();
    }
}
