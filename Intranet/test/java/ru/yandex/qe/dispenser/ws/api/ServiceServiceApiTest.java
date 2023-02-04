package ru.yandex.qe.dispenser.ws.api;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

import ru.yandex.qe.dispenser.api.v1.DiAmount;
import ru.yandex.qe.dispenser.api.v1.DiQuotingMode;
import ru.yandex.qe.dispenser.api.v1.DiResource;
import ru.yandex.qe.dispenser.api.v1.DiResourceType;
import ru.yandex.qe.dispenser.api.v1.DiService;
import ru.yandex.qe.dispenser.api.v1.DiUnit;
import ru.yandex.qe.dispenser.api.v1.request.DiActualQuotaUpdate;
import ru.yandex.qe.dispenser.api.v1.request.DiQuotaState;
import ru.yandex.qe.dispenser.api.v1.response.DiQuotaGetResponse;
import ru.yandex.qe.dispenser.api.v1.response.DiResponse;
import ru.yandex.qe.dispenser.client.v1.DiPerson;
import ru.yandex.qe.dispenser.ws.ServiceService;

public class ServiceServiceApiTest extends ApiTestBase {
    @Test
    public void creationOfServiceMustReturnServiceView() {
        disableHierarchy();

        final DiService createdService = dispenser().service("new-service").create()
                .withName("New Service")
                .withAbcServiceId(TEST_ABC_SERVICE_ID)
                .withSettings(DiService.Settings.DEFAULT)
                .withAdmins(SANCHO.getLogin())
                .performBy(AMOSOV_F);

        assertLastMethodEquals(HttpMethod.PUT);
        assertLastPathEquals("/api/v1/services/new-service");
        assertLastRequestBodyEquals("/body/service/create/data.json");
        assertLastResponseEquals("/body/service/create/data.json");
        assertJsonEquals("/body/service/create/data.json", createdService);
    }

    @Test
    public void creationOfServiceShouldNotRequireServiceKeyInBody() {
        disableHierarchy();

        final JsonNode response = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/services/new-service")
                .put(fromClasspath("/body/service/create/data-no-key.json"), JsonNode.class);
        assertLastResponseStatusEquals(HttpStatus.SC_OK);
        assertJsonEquals("/body/service/create/data.json", response);
    }


    @Test
    public void getAllServicesRegressionTest() {
        createLocalClient().path("/v1/services/all").get(JsonNode.class);
        assertLastResponseStatusEquals(HttpStatus.SC_OK);
        assertLastResponseEquals("/body/service/read/all-services.json");
    }

    @Test
    public void creationOfResourceMustReturnResourceView() {
        final DiResource createdResource = dispenser().service(NIRVANA)
                .resource("resourcekey")
                .create()
                .withName("resource name")
                .withDescription("resource description")
                .withType(DiResourceType.STORAGE)
                .inMode(DiQuotingMode.ENTITIES_ONLY)
                .performBy(SANCHO);

        assertLastMethodEquals(HttpMethod.PUT);
        assertLastPathEquals("/api/v1/services/nirvana/resources/resourcekey");
        assertLastRequestBodyEquals("/body/service/resource/create/req.json");
        assertLastResponseEquals("/body/service/resource/create/resp.json");
        assertJsonEquals("/body/service/resource/create/resp.json", createdResource);
    }

    @Test
    public void newResourceMayNotHaveDescription() {
        final DiResource createdResource = dispenser().service(NIRVANA)
                .resource("resourcekey")
                .create()
                .withName("resource name")
                .withType(DiResourceType.STORAGE)
                .performBy(SANCHO);

        assertLastMethodEquals(HttpMethod.PUT);
        assertLastPathEquals("/api/v1/services/nirvana/resources/resourcekey");
        assertLastRequestBodyEquals("/body/service/resource/create/no-description-req.json");
        assertLastResponseEquals("/body/service/resource/create/no-description-resp.json");
        assertJsonEquals("/body/service/resource/create/no-description-resp.json", createdResource);
    }

    @Test
    public void batchSetMaxRegressionTest() {
        final DiQuotaGetResponse resp = dispenser().service(CLUSTER_API)
                .syncState()
                .quotas()
                .changeQuota(DiQuotaState.forResource(COMPUTER).forProject(INFRA).withMax(DiAmount.of(2, DiUnit.CURRENCY)).build())
                .changeQuota(DiQuotaState.forResource(COMPUTER).forProject(DEFAULT).withMax(DiAmount.of(3, DiUnit.CURRENCY)).build())
                .performBy(WHISTLER);

        assertLastMethodEquals(HttpMethod.POST);
        assertLastPathEquals("/api/v1/services/cluster-api/sync-state/quotas/set");
        assertLastRequestBodyEquals("/body/service/sync-state/quotas/req.json");
        assertLastResponseEquals("/body/service/sync-state/quotas/resp.json");
        assertJsonEquals("/body/service/sync-state/quotas/resp.json", resp);
    }

    @Test
    public void batchSetActualTest() {
        final DiResponse resp = dispenser().service(CLUSTER_API)
                .syncState()
                .actualQuotas()
                .changeActualQuota(DiActualQuotaUpdate.forResource(SSD).project(INFRA).actual(DiAmount.of(2, DiUnit.BYTE)).build())
                .changeActualQuota(DiActualQuotaUpdate.forResource(SSD).project(DEFAULT).actual(DiAmount.of(3, DiUnit.BYTE)).build())
                .performBy(DiPerson.login(ZOMB_MOBSEARCH));

        assertLastMethodEquals(HttpMethod.POST);
        assertLastPathEquals("/api/v1/services/cluster-api/sync-state/actual-quotas/set");
        assertLastRequestBodyEquals("/body/service/sync-state/actual-quotas/req.json");
        assertLastResponseStatusEquals(Response.Status.OK.getStatusCode());
        assertLastResponseEquals("/body/service/sync-state/actual-quotas/resp.json");
        assertJsonEquals("/body/service/sync-state/actual-quotas/resp.json", resp);
    }

    @Test
    public void batchSetRawTest() {
        dispenser()
                .properties()
                .setProperty(ServiceService.RAW_QUOTA_IMPORT_ENTITY, ServiceService.ENDPOINT_ENABLED_PROPERTY, true)
                .performBy(AMOSOV_F);
        updateHierarchy();
        final DiResponse resp = dispenser().service(CLUSTER_API)
                .syncState()
                .rawQuotas()
                .changeRawQuota(DiQuotaState.forResource(SSD).forProject(INFRA)
                        .withActual(DiAmount.of(2, DiUnit.BYTE))
                        .withMax(DiAmount.of(2, DiUnit.BYTE))
                        .withOwnMax(DiAmount.of(2, DiUnit.BYTE))
                        .build())
                .changeRawQuota(DiQuotaState.forResource(SSD).forProject(DEFAULT)
                        .withActual(DiAmount.of(3, DiUnit.BYTE))
                        .withMax(DiAmount.of(3, DiUnit.BYTE))
                        .withOwnMax(DiAmount.of(3, DiUnit.BYTE))
                        .build())
                .performBy(DiPerson.login(ZOMB_MOBSEARCH));

        assertLastMethodEquals(HttpMethod.POST);
        assertLastPathEquals("/api/v1/services/cluster-api/sync-raw-state/quotas/set");
        assertLastRequestBodyEquals("/body/service/sync-state/raw-quotas/req.json");
        assertLastResponseStatusEquals(Response.Status.OK.getStatusCode());
        assertLastResponseEquals("/body/service/sync-state/raw-quotas/resp.json");
        assertJsonEquals("/body/service/sync-state/raw-quotas/resp.json", resp);
    }

}
