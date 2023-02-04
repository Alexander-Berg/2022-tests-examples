package ru.yandex.qe.dispenser.ws.api;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

import ru.yandex.qe.dispenser.api.v1.response.DiQuotaGetResponse;
import ru.yandex.qe.dispenser.ws.QuotaSpecService;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class QuotaSpecServiceApiTest extends ApiTestBase {
    /**
     * {@link QuotaSpecService#read}
     */
    @Test
    public void getQuotaSpecRegressionTest() {
        // TODO: use java client
        final JsonNode quotaSpec = createLocalClient()
                .path("/v1/quota-specifications/nirvana/storage/storage")
                .get(JsonNode.class);
        assertJsonEquals("/body/quota/spec/read/quota-spec.json", quotaSpec);
    }

    @Test
    public void createQuotaSpecRegressionTest() {
        final JsonNode quotaSpec = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-specifications/" + NIRVANA + "/" + STORAGE + "/storage-quota-2")
                .put(fromClasspath("/body/quota/spec/create/new-quota-spec-body.json"), JsonNode.class);
        assertLastResponseStatusEquals(HttpStatus.SC_OK);
        assertJsonEquals("/body/quota/spec/create/new-quota-spec.json", quotaSpec);
    }

    @Test
    public void changingKeyInQuotaSpecUpdateRequestShouldMakeNoEffect() {
        createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-specifications/nirvana/storage/storage")
                .post(fromClasspath("/body/quota/spec/update/quota-spec-body-updated-key.json"));
        assertLastResponseStatusEquals(HttpStatus.SC_OK);
        updateHierarchy();
        getQuotaSpecRegressionTest();
    }

    @Test
    public void creatingQuotaSpecShouldCreateQuotasForAllSegmentations() {
        final String newQuotaSpecKey = "yt-cpu-quota-2";

        final JsonNode quotaSpec = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-specifications/" + YP + "/" + SEGMENT_CPU + "/" + newQuotaSpecKey)
                .put(fromClasspath("/body/quota/spec/create/new-quota-spec-body.json"), JsonNode.class);
        assertLastResponseStatusEquals(HttpStatus.SC_OK);
        updateHierarchy();

        final DiQuotaGetResponse quotas = dispenser().quotas().get()
                .ofProject(INFRA)
                .inService(YP)
                .forResource(SEGMENT_CPU)
                .perform();


        final long quotasCount = quotas.stream()
                .filter(quota -> quota.getSpecification().getKey().equals(newQuotaSpecKey))
                .count();

        assertEquals((3 + 1) * (2 + 1), quotasCount);
    }
}
