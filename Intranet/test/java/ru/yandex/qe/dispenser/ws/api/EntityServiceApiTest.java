package ru.yandex.qe.dispenser.ws.api;

import javax.ws.rs.HttpMethod;

import org.junit.jupiter.api.Test;

import ru.yandex.qe.dispenser.api.v1.DiAmount;
import ru.yandex.qe.dispenser.api.v1.DiUnit;
import ru.yandex.qe.dispenser.api.v1.request.DiEntity;
import ru.yandex.qe.dispenser.api.v1.response.DiListResponse;
import ru.yandex.qe.dispenser.ws.EntityService;

public final class EntityServiceApiTest extends ApiTestBase {
    /**
     * {@link EntityService#getResourceEntity}
     * {@link EntityService#filterEntities}
     */
    @Test
    public void gettingResourceEntitiesMustReturnOwnersWithoutOwnedSize() {
        final DiEntity file40 = DiEntity.withKey("file40")
                .bySpecification(YT_FILE)
                .occupies(STORAGE, DiAmount.of(40, DiUnit.BYTE))
                .build();
        dispenser().quotas()
                .changeInService(NIRVANA)
                .createEntity(file40, LYADZHIN.chooses(INFRA))
                .perform();
        final DiEntity file40view = dispenser().getEntities().inService(NIRVANA).bySpecification(YT_FILE).withKey("file40").perform();
        assertLastMethodEquals(HttpMethod.GET);
        assertLastPathEquals("/api/v1/entities/nirvana/yt-file/file40");
        assertLastResponseEquals("/body/entity/read/entity.json");
        assertJsonEquals("/body/entity/read/entity.json", file40view);

        final DiEntity file50 = DiEntity.withKey("file50")
                .bySpecification(YT_FILE)
                .occupies(STORAGE, DiAmount.of(50, DiUnit.BYTE))
                .build();
        dispenser().quotas()
                .changeInService(NIRVANA)
                .createEntity(file50, LYADZHIN.chooses(INFRA))
                .perform();
        final DiListResponse<DiEntity> storageFiles = dispenser().getEntities().inService(NIRVANA).bySpecification(YT_FILE).perform();
        assertLastMethodEquals(HttpMethod.GET);
        assertLastPathQueryEquals("/api/v1/entities?entitySpec=/nirvana/yt-file");
        assertLastResponseEquals("/body/entity/read/entities.json");
        assertJsonEquals("/body/entity/read/entities.json", storageFiles);
    }
}
