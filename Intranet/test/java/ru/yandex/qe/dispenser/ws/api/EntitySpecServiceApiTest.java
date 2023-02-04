package ru.yandex.qe.dispenser.ws.api;

import javax.ws.rs.HttpMethod;

import org.junit.jupiter.api.Test;

import ru.yandex.qe.dispenser.api.v1.DiEntitySpec;
import ru.yandex.qe.dispenser.api.v1.response.DiListResponse;

public final class EntitySpecServiceApiTest extends ApiTestBase {
    @Test
    public void oneEntitySpecificationRegressionTest() throws Exception {
        final DiEntitySpec spec = dispenser().entitySpecifications().get(NIRVANA, YT_FILE).perform();

        assertLastMethodEquals(HttpMethod.GET);
        assertLastPathQueryEquals("/api/v1/entity-specifications/nirvana/yt-file");
        assertLastResponseEquals("/body/entity/spec/read/entity-spec.json");
        assertJsonEquals("/body/entity/spec/read/entity-spec.json", spec);
    }

    @Test
    public void gettingEntitySpecificationListRegressionTest() {
        final DiListResponse<DiEntitySpec> specs = dispenser().entitySpecifications().get().inService(NIRVANA).perform();

        assertLastMethodEquals(HttpMethod.GET);
        assertLastPathQueryEquals("/api/v1/entity-specifications?service=nirvana");
        assertLastResponseEquals("/body/entity/spec/read/entity-specs.json");
        assertJsonEquals("/body/entity/spec/read/entity-specs.json", specs);
    }
}
