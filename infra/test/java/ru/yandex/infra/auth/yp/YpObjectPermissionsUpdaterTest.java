package ru.yandex.infra.auth.yp;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.infra.auth.RolesInfo;
import ru.yandex.infra.controller.dto.Acl;
import ru.yandex.infra.controller.dto.StageMeta;
import ru.yandex.infra.controller.yp.DummyYpObjectTransactionalRepository;
import ru.yandex.infra.controller.yp.DummyYpTransactionClient;
import ru.yandex.infra.controller.yp.YpObject;
import ru.yandex.yp.client.api.AccessControl;
import ru.yandex.yp.client.api.TStageSpec;
import ru.yandex.yp.client.api.TStageStatus;

import static com.spotify.hamcrest.optional.OptionalMatchers.emptyOptional;
import static com.spotify.hamcrest.optional.OptionalMatchers.optionalWithValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static ru.yandex.infra.auth.yp.YpUtils.generateACE;

class YpObjectPermissionsUpdaterTest {
    private static final String stageId = "stage";
    private static final String projectId = "project_id";

    private static final String subjectMaintainerId = "deploy:project.stage.MAINTAINER";
    private static final List<String> maintainerAttributePaths = List.of("/spec");
    private static final Set<AccessControl.EAccessControlPermission> maintainerPermissions = Set.of(
            AccessControl.EAccessControlPermission.ACA_WRITE,
            AccessControl.EAccessControlPermission.ACP_READ
    );

    private static final String subjectDeveloperId = "deploy:project.stage.DEVELOPER";
    private static final List<String> developerAttributePaths = List.of("/access/delploy/box");
    private static final Set<AccessControl.EAccessControlPermission> developerPermissions = Set.of(
            AccessControl.EAccessControlPermission.ACA_SSH_ACCESS
    );

    private final StageMeta developerMeta = new StageMeta(
            stageId,
            new Acl(List.of(generateACE(subjectDeveloperId, developerPermissions, developerAttributePaths))),
            "",
            "",
            0,
            projectId
    );

    private final StageMeta fullMeta = new StageMeta(
            stageId,
            new Acl(List.of(
                    generateACE(subjectDeveloperId, developerPermissions, developerAttributePaths),
                    generateACE(subjectMaintainerId, developerPermissions, developerAttributePaths),
                    generateACE(subjectMaintainerId, maintainerPermissions, maintainerAttributePaths))),
            "",
            "",
            0,
            projectId
    );

    private DummyYpObjectTransactionalRepository<StageMeta, TStageSpec, TStageStatus> ypRepository;
    private YpObjectPermissionsUpdater permissionsUpdater;

    @BeforeEach
    void before() {
        ypRepository = new DummyYpObjectTransactionalRepository<>();
        DummyYpTransactionClient ypTransactionClient = new DummyYpTransactionClient();
        permissionsUpdater = new YpObjectPermissionsUpdaterImpl<>(ypTransactionClient, ypRepository);

        CompletableFuture<?> updateResponse = new CompletableFuture<>();
        updateResponse.complete(null);
        ypRepository.updateResponse = updateResponse;
    }

    @Test
    void addRole() {
        YpObject<StageMeta, TStageSpec, TStageStatus> object =
                new YpObject.Builder<StageMeta, TStageSpec, TStageStatus>()
                        .setMeta(developerMeta)
                        .build();

        ypRepository.getResponses.put(stageId, CompletableFuture.completedFuture(Optional.of(object)));

        assertDoesNotThrow(() -> {
            permissionsUpdater.addRole(stageId, subjectMaintainerId, ImmutableList.of(
                    new RolesInfo.RoleAce(maintainerPermissions, maintainerAttributePaths),
                    new RolesInfo.RoleAce(developerPermissions, developerAttributePaths))
            ).get();
        });

        assertThat(ypRepository.updatedIds, contains(stageId));
        assertThat(ypRepository.lastUpdateRequest.getAcl().get().getEntries(),
                containsInAnyOrder(fullMeta.getAcl().getEntries().toArray()));
    }

    @Test
    void addExistingRole() {
        YpObject<StageMeta, TStageSpec, TStageStatus> object =
                new YpObject.Builder<StageMeta, TStageSpec, TStageStatus>()
                        .setMeta(developerMeta)
                        .build();

        ypRepository.getResponses.put(stageId, CompletableFuture.completedFuture(Optional.of(object)));

        assertDoesNotThrow(() -> {
            permissionsUpdater.addRole(stageId, subjectDeveloperId, ImmutableList.of(
                    new RolesInfo.RoleAce(developerPermissions, developerAttributePaths))
            ).get();
        });

        assertThat(ypRepository.updatedIds, not(contains(stageId)));
        assertThat(ypRepository.lastUpdateRequest.getAcl(), emptyOptional());
    }

    @Test
    void removeExistingRole() {
        YpObject<StageMeta, TStageSpec, TStageStatus> object =
                new YpObject.Builder<StageMeta, TStageSpec, TStageStatus>()
                        .setMeta(fullMeta)
                        .build();

        ypRepository.getResponses.put(stageId, CompletableFuture.completedFuture(Optional.of(object)));

        assertDoesNotThrow(() -> {
            permissionsUpdater.removeRoles(stageId, Set.of(subjectMaintainerId)).get();
        });

        assertThat(ypRepository.updatedIds, contains(stageId));
        assertThat(ypRepository.lastUpdateRequest.getAcl(), optionalWithValue(
                equalTo(new Acl(List.of(
                        AccessControl.TAccessControlEntry.newBuilder()
                                .addSubjects(subjectDeveloperId)
                                .addAllPermissions(developerPermissions)
                                .addAllAttributes(developerAttributePaths)
                                .setAction(AccessControl.EAccessControlAction.ACA_ALLOW)
                                .build())))));
    }

    @Test
    void removeNotExistingRole() {
        YpObject<StageMeta, TStageSpec, TStageStatus> object =
                new YpObject.Builder<StageMeta, TStageSpec, TStageStatus>()
                        .setMeta(developerMeta)
                        .build();

        ypRepository.getResponses.put(stageId, CompletableFuture.completedFuture(Optional.of(object)));

        assertDoesNotThrow(() -> {
            permissionsUpdater.removeRoles(stageId, Set.of(subjectMaintainerId)).get();
        });

        assertThat(ypRepository.updatedIds, not(contains(stageId)));
        assertThat(ypRepository.lastUpdateRequest.getAcl(), emptyOptional());
    }
}
