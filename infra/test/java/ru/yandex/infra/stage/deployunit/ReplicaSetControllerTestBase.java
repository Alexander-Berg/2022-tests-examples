package ru.yandex.infra.stage.deployunit;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import com.google.protobuf.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.infra.controller.dto.Acl;
import ru.yandex.infra.controller.dto.SchemaMeta;
import ru.yandex.infra.stage.TestData;
import ru.yandex.infra.stage.dto.DeployProgress;
import ru.yandex.infra.stage.yp.AclUpdater;
import ru.yandex.infra.stage.yp.AppendingAclUpdater;
import ru.yandex.infra.stage.yp.DummyObjectLifeCycleManager;
import ru.yandex.infra.stage.yp.ObjectLifeCycleManager;
import ru.yandex.infra.stage.yp.SpecStatusMeta;
import ru.yandex.yp.model.YpObjectType;
import ru.yandex.yp.model.YpTypedId;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;

abstract class ReplicaSetControllerTestBase<Spec extends Message, Status extends Message, RevisionType> {
    private static final int TARGET_SPEC_PODS_TOTAL = 10;
    private static final int TARGET_REVISION = 11;
    private static final int STATUS_PODS_TOTAL = 20;
    private static final int CURRENT_REVISION = 10;
    private static final String REPLICA_SET_ID = "replica_set";

    protected DummyRelationController relationController;

    @BeforeEach
    void before() {
        relationController = new DummyRelationController();
    }

    // test a situation when there is no status for target spec revision,
    // but there is status for current spec revision (targetSpec.revision != currentSpec.revision)
    @Test
    void reportEmptyProgressIfTargetSpecRevisionNotInStatusTemplate() {
        DummyObjectLifeCycleManager<SchemaMeta, Spec, Status> lifeCycleManager = new DummyObjectLifeCycleManager<>();
        ReplicaSetControllerBase<Spec, Status, RevisionType> controller = createController(REPLICA_SET_ID,
                lifeCycleManager, AclUpdater.IDENTITY);
        controller.sync(generateSpec(TARGET_SPEC_PODS_TOTAL, TARGET_REVISION), TestData.DEFAULT_STAGE_CONTEXT);
        lifeCycleManager.notifySubscriber(REPLICA_SET_ID, Optional.of(new SpecStatusMeta<>(
                generateSpec(STATUS_PODS_TOTAL, CURRENT_REVISION),
                generateStatus(STATUS_PODS_TOTAL, CURRENT_REVISION),
                new SchemaMeta(REPLICA_SET_ID, TestData.STAGE_ACL, "", "", 0),
                1, 1
        )));
        // to somehow ensure that handling code has been run
        assertThat(controller.getHandleCurrentStateCounter(), equalTo(1));
        assertThat(controller.getStatus().getProgress(), equalTo(new DeployProgress(0, 0, TARGET_SPEC_PODS_TOTAL)));
    }

    @Test
    void updateAclIfDoesNotMatch() {
        DummyObjectLifeCycleManager<SchemaMeta, Spec, Status> lifeCycleManager = new DummyObjectLifeCycleManager<>();
        ReplicaSetControllerBase<Spec, Status, RevisionType> controller = createController(REPLICA_SET_ID,
                lifeCycleManager, AclUpdater.IDENTITY);
        Spec spec = generateSpec(TARGET_SPEC_PODS_TOTAL, TARGET_REVISION);
        controller.sync(spec, TestData.DEFAULT_STAGE_CONTEXT);
        lifeCycleManager.notifySubscriber(REPLICA_SET_ID, Optional.of(new SpecStatusMeta<>(
                spec,
                generateStatus(STATUS_PODS_TOTAL, CURRENT_REVISION),
                new SchemaMeta(REPLICA_SET_ID, Acl.EMPTY, "", "", 0), 1, 1
        )));

        assertThat(lifeCycleManager.getCurrentAcl(REPLICA_SET_ID), equalTo(TestData.STAGE_ACL));
    }

    @Test
    void patchAclViaUpdater() {
        DummyObjectLifeCycleManager<SchemaMeta, Spec, Status> lifeCycleManager = new DummyObjectLifeCycleManager<>();
        lifeCycleManager.repository.createResponse = CompletableFuture.completedFuture(
                new YpTypedId(REPLICA_SET_ID, YpObjectType.REPLICA_SET, Optional.of("fqid")));
        ReplicaSetControllerBase<Spec, Status, RevisionType> controller = createController(REPLICA_SET_ID,
                lifeCycleManager, new AppendingAclUpdater(TestData.ACL_WRITE_ENTRY));
        controller.sync(
                generateSpec(TARGET_SPEC_PODS_TOTAL, TARGET_REVISION),
                TestData.DEFAULT_STAGE_CONTEXT.withAcl(Acl.EMPTY)
        );
        lifeCycleManager.notifySubscriber(REPLICA_SET_ID, Optional.empty());

        assertThat(lifeCycleManager.getCurrentAcl(REPLICA_SET_ID).getEntries(), contains(TestData.ACL_WRITE_ENTRY));
    }

    @Test
    void checkRelationWithUpdatedStageFqid() {
        DummyObjectLifeCycleManager<SchemaMeta, Spec, Status> lifeCycleManager = new DummyObjectLifeCycleManager<>();
        ReplicaSetControllerBase<Spec, Status, RevisionType> controller = createController(REPLICA_SET_ID,
                lifeCycleManager, AclUpdater.IDENTITY);
        Spec spec = generateSpec(TARGET_SPEC_PODS_TOTAL, TARGET_REVISION);

        controller.sync(spec, TestData.DEFAULT_STAGE_CONTEXT);
        lifeCycleManager.notifySubscriber(REPLICA_SET_ID, Optional.of(new SpecStatusMeta<>(
                spec,
                generateStatus(STATUS_PODS_TOTAL, CURRENT_REVISION),
                new SchemaMeta(REPLICA_SET_ID, Acl.EMPTY, "", "", 0), 1, 1
        )));

        assertThat(relationController.lastUsedStageFqid, equalTo(TestData.DEFAULT_STAGE_FQID));

        var newFqid = TestData.composeStageFqid(TestData.DEFAULT_STAGE_ID, "76b6b731-14012d6d-3cac2591-ed9eca20");
        controller.sync(
                spec,
                TestData.DEFAULT_STAGE_CONTEXT.withStageFqid(newFqid)
        );
        lifeCycleManager.notifySubscriber(REPLICA_SET_ID, Optional.of(new SpecStatusMeta<>(
                spec,
                generateStatus(STATUS_PODS_TOTAL, CURRENT_REVISION),
                new SchemaMeta(REPLICA_SET_ID, Acl.EMPTY, "", "", 0), 1, 1
        )));

        assertThat(relationController.lastUsedStageFqid, equalTo(newFqid));
    }

    protected abstract ReplicaSetControllerBase<Spec, Status, RevisionType> createController(
            String id, ObjectLifeCycleManager<SchemaMeta, Spec, Status> manager, AclUpdater aclUpdater);

    protected abstract Spec generateSpec(int podsTotal, int revision);

    protected abstract Status generateStatus(int podsTotal, int revision);
}
