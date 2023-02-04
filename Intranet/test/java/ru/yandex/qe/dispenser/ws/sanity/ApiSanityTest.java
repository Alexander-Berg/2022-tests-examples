package ru.yandex.qe.dispenser.ws.sanity;

import javax.ws.rs.BadRequestException;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.qe.dispenser.api.v1.DiAmount;
import ru.yandex.qe.dispenser.api.v1.DiPerformer;
import ru.yandex.qe.dispenser.api.v1.DiProject;
import ru.yandex.qe.dispenser.api.v1.DiUnit;
import ru.yandex.qe.dispenser.api.v1.request.DiEntity;
import ru.yandex.qe.dispenser.api.v1.response.DiQuotaChangeResponse;
import ru.yandex.qe.dispenser.ws.AcceptanceTestBase;

public class ApiSanityTest extends AcceptanceTestBase {
    @Test
    public void apiShouldAcceptClusterApiReqIdAndEntityKeyFormat() {
        final DiQuotaChangeResponse response = createClusterApiEntity("cluster-api_-9223372036832753263", "rel-cluster-api_-9223372036832753263");
        Assertions.assertTrue(response.isSuccess());
    }

    @Test
    public void apiShouldNotAcceptReqIdsWithNotAllowedSymbols() {
        Assertions.assertThrows(BadRequestException.class, () -> {
            createClusterApiEntity("cluster-api_-9223372036832753263", "reqId-123-#@%");
        });
    }

    @Test
    public void apiShouldNotAcceptEntityKeysWithNotAllowedSymbols() {
        Assertions.assertThrows(BadRequestException.class, () -> {
            createClusterApiEntity("entity-123-@#$", "rel-cluster-api_-9223372036832753263");
        });
    }

    private DiQuotaChangeResponse createClusterApiEntity(@NotNull final String entityKey, @NotNull final String reqId) {
        final DiEntity entity = DiEntity.withKey(entityKey)
                .bySpecification(WORKLOAD)
                .occupies(CPU, DiAmount.of(1, DiUnit.COUNT))
                .occupies(RAM, DiAmount.of(1, DiUnit.GIBIBYTE))
                .build();
        return dispenser().quotas().changeInService(CLUSTER_API)
                .createEntity(entity, DiPerformer.login(LYADZHIN.getLogin()).chooses(INFRA))
                .withReqId(reqId)
                .perform();
    }

    @Test
    public void apiShouldAcceptCorrectProjectKeys() {
        createProject("project-key-123");
    }

    @Test
    public void apiShouldAcceptProjectKeysWithAllowedSymbols() {
        createProject("project_key_123");
    }

    private DiProject createProject(@NotNull final String projectKey) {
        final DiProject project = DiProject.withKey(projectKey)
                .withName("Project Name")
                .withDescription("Some description")
                .withAbcServiceId(TEST_ABC_SERVICE_ID)
                .withParentProject(YANDEX)
                .build();
        return dispenser().projects().create(project).performBy(WHISTLER);
    }

}
