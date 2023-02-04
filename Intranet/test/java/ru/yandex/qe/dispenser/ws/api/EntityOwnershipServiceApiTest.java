package ru.yandex.qe.dispenser.ws.api;

import javax.ws.rs.HttpMethod;

import org.junit.jupiter.api.Test;

import ru.yandex.qe.dispenser.api.v1.request.DiEntityOwnership;
import ru.yandex.qe.dispenser.api.v1.response.DiEntityOwnershipResponse;
import ru.yandex.qe.dispenser.api.v1.response.DiListResponse;

public final class EntityOwnershipServiceApiTest extends ApiTestBase {
    @Test
    public void gettingEntityOwnershipListRegressionTest() {
        dispenser().quotas().changeInService(NIRVANA).createEntity(UNIT_ENTITY, LYADZHIN.chooses(INFRA)).perform();

        final DiListResponse<DiEntityOwnership> ownerships = dispenser().getEntityOwnerships()
                .inService(NIRVANA)
                .bySpecification(YT_FILE)
                .withEntityKey(UNIT_ENTITY.getKey())
                .ofProject(INFRA)
                .perform();

        assertLastMethodEquals(HttpMethod.POST);
        assertLastPathQueryEquals("/api/v1/entity-ownerships");
        assertLastRequestBodyEquals("/body/entity/ownership/read/entity-ownerships-req.json");
        assertLastResponseEquals("/body/entity/ownership/read/entity-ownerships-resp.json");
        assertJsonEquals("/body/entity/ownership/read/entity-ownerships-resp.json", ownerships);
    }

    @Test
    public void gettingEntityOwnershipListOldApiRegressionTest() {
        dispenser().quotas().changeInService(NIRVANA).createEntity(UNIT_ENTITY, LYADZHIN.chooses(INFRA)).perform();

        final DiListResponse<DiEntityOwnership> ownerships = createLocalClient()
                .path("/v1/entity-ownerships")
                .query("entity", "/" + NIRVANA + "/" + UNIT_ENTITY.getSpecificationKey() + "/" + UNIT_ENTITY.getKey())
                .query("project", INFRA)
                .get(DiEntityOwnershipResponse.class);

        assertLastMethodEquals(HttpMethod.GET);
        assertLastPathQueryEquals("/api/v1/entity-ownerships?entity=/nirvana/yt-file/pool&project=infra");
        assertLastResponseEquals("/body/entity/ownership/read/entity-ownerships-resp.json");
        assertJsonEquals("/body/entity/ownership/read/entity-ownerships-resp.json", ownerships);
    }
}
