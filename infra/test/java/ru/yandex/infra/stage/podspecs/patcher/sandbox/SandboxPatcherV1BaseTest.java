package ru.yandex.infra.stage.podspecs.patcher.sandbox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import ru.yandex.infra.stage.deployunit.DeployUnitContext;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTreeBuilder;
import ru.yandex.yp.client.api.DataModel;
import ru.yandex.yp.client.api.TPodTemplateSpec;
import ru.yandex.yp.client.pods.TLayer;
import ru.yandex.yp.client.pods.TPodAgentSpec;
import ru.yandex.yp.client.pods.TResource;
import ru.yandex.yp.client.pods.TResourceGang;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.infra.stage.TestData.DEFAULT_UNIT_CONTEXT;

abstract class SandboxPatcherV1BaseTest {

    private static final String RESOURCE_ID_PREFIX = "id_";
    private static final String LAYER_ID_PREFIX = "layer_id_";
    private static final String RESOURCE_URL_PREFIX = "sbr:";
    private static final String RESOURCE_RB_TORRENT_PREFIX = "rbtorrent:";
    private static final String LAYER_URL_PREFIX = "sbr:123";
    private static final String LAYER_RB_TORRENT_PREFIX = "rbtorrent:321";

    @Test
    void resolvingOrderCheck() {
        Map<String, String> resolvedSbr = new HashMap<>();
        TPodTemplateSpec.Builder spec = createContext(resolvedSbr);
        DeployUnitContext context = DEFAULT_UNIT_CONTEXT.withResolvedSbr(resolvedSbr);

        SandboxPatcherV1Base patcher = new SandboxPatcherV1();
        patcher.patch(spec, context, new YTreeBuilder());

        List<TResource> resolvedResources =
                spec.getSpec().getPodAgentPayload().getSpec().getResources().getStaticResourcesList();
        for (int i = 0; i < 5; i++) {
            assertThat(resolvedResources.get(i).getUrl(), equalTo(RESOURCE_RB_TORRENT_PREFIX + i));
        }
        List<TLayer> resolvedLayers =
                spec.getSpec().getPodAgentPayload().getSpec().getResources().getLayersList();
        for (int i = 0; i < 5; i++) {
            assertThat(resolvedLayers.get(i).getUrl(), equalTo(LAYER_RB_TORRENT_PREFIX + i));
        }
    }

    TPodTemplateSpec.Builder createContext(Map<String, String> resolved) {
        TResourceGang.Builder resourceBuilder = TResourceGang.newBuilder();
        for (int i = 0; i < 5; i++) {
            String id = RESOURCE_ID_PREFIX + i;
            TResource resource = TResource.newBuilder().setUrl(RESOURCE_URL_PREFIX + i).setId(id).build();
            resourceBuilder.addStaticResources(resource);
            resolved.put(id, RESOURCE_RB_TORRENT_PREFIX + i);
        }
        for (int i = 0; i < 5; i++) {
            String id = LAYER_ID_PREFIX + i;
            TLayer layer = TLayer.newBuilder().setUrl(LAYER_URL_PREFIX + i).setId(id).build();
            resourceBuilder.addLayers(layer);
            resolved.put(id, LAYER_RB_TORRENT_PREFIX + i);
        }
        TPodAgentSpec podAgentSpec = TPodAgentSpec.newBuilder().setResources(resourceBuilder).build();
        DataModel.TPodSpec.TPodAgentPayload podAgentPayload =
                DataModel.TPodSpec.TPodAgentPayload.newBuilder().setSpec(podAgentSpec).build();
        DataModel.TPodSpec podSpec = DataModel.TPodSpec.newBuilder().setPodAgentPayload(podAgentPayload).build();

        return TPodTemplateSpec.newBuilder().setSpec(podSpec);
    }
}
