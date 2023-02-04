package ru.yandex.qe.dispenser.ws.api;

import java.util.Collection;
import java.util.Map;

import javax.ws.rs.HttpMethod;

import org.junit.jupiter.api.Test;

import ru.yandex.qe.dispenser.api.v1.DiAmount;
import ru.yandex.qe.dispenser.api.v1.DiUnit;
import ru.yandex.qe.dispenser.api.v1.request.DiEntity;
import ru.yandex.qe.dispenser.api.v1.request.DiEntityUsage;
import ru.yandex.qe.dispenser.api.v1.response.DiListResponse;
import ru.yandex.qe.dispenser.api.v1.response.DiQuotaChangeResponse;
import ru.yandex.qe.dispenser.client.v1.impl.SpyWebClient;
import ru.yandex.qe.dispenser.ws.BatchQuotaServiceImpl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class QuotaServiceApiTest extends ApiTestBase {
    /**
     * {@link BatchQuotaServiceImpl#changeQuotas}
     */
    @Test
    public void createEntityRegressionTest() {
        final DiEntity entity = DiEntity.withKey("pool")
                .bySpecification(YT_FILE)
                .occupies(STORAGE, DiAmount.of(60, DiUnit.BYTE))
                .build();

        final DiQuotaChangeResponse resp =
                dispenser().quotas()
                        .changeInService(NIRVANA)
                        .createEntity(entity, LYADZHIN.chooses(INFRA))
                        .perform();

        assertLastMethodEquals(HttpMethod.POST);
        assertLastPathEquals("/api/v1/change-quotas");
        assertLastRequestBodyEquals("/body/quota/acquire/create-entity-req.json");
        assertLastResponseEquals("/body/quota/acquire/create-entity-resp2.json");
        assertJsonEquals("/body/quota/acquire/create-entity-resp.json", resp);
    }

    @Test
    public void createDuplicateEntity() {
        final DiEntity entity = DiEntity.withKey("pool")
                .bySpecification(YT_FILE)
                .occupies(STORAGE, DiAmount.of(60, DiUnit.BYTE))
                .build();

        assertThrows(Exception.class, () -> {
            for (int i = 0; i < 2; i++) {
                dispenser().quotas()
                        .changeInService(NIRVANA)
                        .createEntity(entity, LYADZHIN.chooses(INFRA))
                        .perform();
            }
        });

        assertTrue(SpyWebClient.lastResponse().contains("already exists"));
        assertLastResponseStatusEquals(409);
    }

    /**
     * {@link BatchQuotaServiceImpl#changeQuotas}
     */
    @Test
    public void shareEntityRegressionTest() {
        final DiEntity entity = DiEntity.withKey("pool")
                .bySpecification(YT_FILE)
                .occupies(STORAGE, DiAmount.of(60, DiUnit.BYTE))
                .build();

        dispenser().quotas().changeInService(NIRVANA)
                .createEntity(entity, LYADZHIN.chooses(VERTICALI))
                .perform();

        final DiQuotaChangeResponse infraShare = dispenser().quotas()
                .changeInService(NIRVANA)
                .shareEntity(DiEntityUsage.singleOf(entity), LYADZHIN.chooses(INFRA))
                .perform();

        assertLastMethodEquals(HttpMethod.POST);
        assertLastPathEquals("/api/v1/change-quotas");
        assertLastRequestBodyEquals("/body/quota/acquire-usage/req-body.json");
        assertLastResponseEquals("/body/quota/acquire-usage/resp2.json");
        assertJsonEquals("/body/quota/acquire-usage/resp.json", infraShare);
    }

    /**
     * {@link BatchQuotaServiceImpl#changeQuotas}
     */
    @Test
    public void releaseAllSharingsRegressionTest() {
        dispenser().quotas().changeInService(NIRVANA).createEntity(UNIT_ENTITY, LYADZHIN.chooses(INFRA)).perform();

        final DiQuotaChangeResponse releaseAllUsagesResponse = dispenser().quotas()
                .changeInService(NIRVANA)
                .releaseAllEntitySharings(UNIT_ENTITY, LYADZHIN.chooses(INFRA))
                .perform();

        assertLastMethodEquals(HttpMethod.POST);
        assertLastPathEquals("/api/v1/change-quotas");
        assertLastRequestBodyEquals("/body/quota/release-all-usages/req-body.json");
        assertLastResponseEquals("/body/quota/release-all-usages/resp.json");
        assertJsonEquals("/body/quota/release-all-usages/resp-parsed.json", releaseAllUsagesResponse);
    }

    @Test
    public void getQuotasLightVersionShouldReturnLessData() {
        final DiListResponse<?> quotas = createAuthorizedLocalClient(STERLIGOVAK)
                .path("/v2/quotas/")
                .get(DiListResponse.class);

        final Map<?, ?> quota = (Map<?, ?>) quotas.getFirst();
        assertEquals(6, quota.size());
        final Map<?, ?> quotaKey = (Map<?, ?>) quota.get("key");
        assertTrue(quotaKey.get("projectKey") instanceof String);
        assertTrue(quotaKey.get("serviceKey") instanceof String);
        assertTrue(quotaKey.get("resourceKey") instanceof String);
        assertTrue(quotaKey.get("quotaSpecKey") instanceof String);
        assertTrue(quotaKey.get("segmentKeys") instanceof Collection);
        assertTrue(quota.get("max") instanceof Number);
        assertTrue(quota.get("actual") instanceof Number);
        assertTrue(quota.get("ownMax") instanceof Number);
        assertTrue(quota.get("ownActual") instanceof Number);
    }
}
