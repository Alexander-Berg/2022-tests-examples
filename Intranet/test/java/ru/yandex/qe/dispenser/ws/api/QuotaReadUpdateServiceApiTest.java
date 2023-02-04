package ru.yandex.qe.dispenser.ws.api;

import javax.inject.Inject;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.qe.dispenser.api.util.SerializationUtils;
import ru.yandex.qe.dispenser.api.v1.DiQuota;
import ru.yandex.qe.dispenser.api.v1.response.DiQuotaGetResponse;
import ru.yandex.qe.dispenser.domain.QuotaSpec;
import ru.yandex.qe.dispenser.domain.Resource;
import ru.yandex.qe.dispenser.domain.dao.property.PropertyDao;
import ru.yandex.qe.dispenser.domain.dao.resource.ResourceDao;
import ru.yandex.qe.dispenser.domain.dao.service.ServiceDao;
import ru.yandex.qe.dispenser.domain.property.Property;
import ru.yandex.qe.dispenser.ws.ProxyDServer;
import ru.yandex.qe.dispenser.ws.ProxyDServerV2;
import ru.yandex.qe.dispenser.ws.QuotaReadUpdateService;


public final class QuotaReadUpdateServiceApiTest extends ApiTestBase {
    @Inject
    @Qualifier("propertyDao")
    private PropertyDao propertyDao;
    @Autowired
    private ResourceDao resourceDao;
    @Autowired
    private ServiceDao serviceDao;

    @Test
    public void testRead() {
        final DiQuotaGetResponse response = dispenser().quotas().get()
                .inService(NIRVANA)
                .forResource(STORAGE)
                .ofProject(INFRA)
                .perform();
        assertJsonEquals("/body/quota/read/quota.json", response);
    }

    /**
     * {@link QuotaReadUpdateService#update}
     */
    @Test
    public void quotaSetMaxRegressionTest() {
        // TODO: decompose test

        // TODO: use API
        final QuotaSpec quotaSpec = new QuotaSpec.Builder("storage2", resourceDao.read(new Resource.Key(STORAGE,
                serviceDao.read(NIRVANA))))
                .description("Nirvana storage quota 2")
                .build();
        quotaSpecDao.create(quotaSpec);
        updateHierarchy();

        final int setQuotaToRootStatus = createAuthorizedLocalClient(SANCHO)
                .path("/v1/quotas/yandex/nirvana/storage/storage2")
                .post(fromClasspath("/body/quota/update/max-value.json"))
                .getStatus();
        Assertions.assertEquals(HttpStatus.SC_OK, setQuotaToRootStatus);
        final JsonNode updatedQuota = createAuthorizedLocalClient(WHISTLER)
                .path("/v1/quotas/infra/nirvana/storage/storage2")
                .post(fromClasspath("/body/quota/update/max-value.json"), JsonNode.class);
        assertJsonEquals("/body/quota/update/updated-quota.json", updatedQuota);
    }

    @Test
    public void proxyDServerDiQuotaDeserializationTest() throws JsonProcessingException {
        DiQuota quota = fromClasspath("/body/quota/update/proxy-d-server-test-updated-quota-real.json",
                new TypeReference<>() {
                });
        Assertions.assertNotNull(quota);

        String quotaSerialized = SerializationUtils.writeValueAsString(quota);
        Assertions.assertNotNull(quotaSerialized);
    }

    /**
     * {@link ProxyDServer#update}
     */
    @Test
    public void proxyDServerTest() {
        Property property = propertyDao.create(new Property(-1L,
                "ProxyDServer", "ProxyDServerMode", Property.Type.STRING.createValue("PROXY_TO_D")
        ));
        updateHierarchy();

        JsonNode updatedQuota = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quotas/yandex/mdb/cpu/cpu-quota")
                .post(
                        """
                                    {
                                      "maxValue": 200,
                                      "unit": "CORES"
                                    }
                                """,
                        JsonNode.class
                );
        assertJsonEquals("/body/quota/update/proxy-d-server-test-updated-quota.json", updatedQuota);

        propertyDao.delete(property);
        updateHierarchy();
    }
    /**
     * {@link ProxyDServerV2#read}
     */
    @Test
    public void ProxyDServerFilterProjectsTestNotFound() {
        Property property = propertyDao.create(new Property(-1L,
                "ProxyDServer", "ProxyDServerMode", Property.Type.STRING.createValue("PROXY_TO_D")
        ));
        updateHierarchy();

        Response response = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v2/quotas")
                .get();
        Assertions.assertEquals(404, response.getStatus());

        propertyDao.delete(property);
        updateHierarchy();
    }

    @Disabled
    @Test
    public void getAllQuotasRegressionTest() {
        final DiQuotaGetResponse resp = dispenser().quotas().get().perform();

        assertLastMethodEquals(HttpMethod.GET);
        assertLastPathEquals("/api/v1/quotas");
        assertLastResponseEquals("/body/quota/read/all-quotas.json");
        assertJsonEquals("/body/quota/read/all-quotas.json", resp);
    }
}
