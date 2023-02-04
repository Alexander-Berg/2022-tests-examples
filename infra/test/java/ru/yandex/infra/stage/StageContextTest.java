package ru.yandex.infra.stage;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTreeBuilder;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.infra.stage.primitives.DeployPrimitiveController.DEPLOY_LABEL_KEY;
import static ru.yandex.infra.stage.primitives.DeployPrimitiveController.DISABLED_CLUSTERS_LABEL_KEY;

class StageContextTest {

    private GlobalContext getContext(Set<String> disabledClusters) {
        return new GlobalContext(disabledClusters);
    }

    private Map<String, YTreeNode> getLabels(Set<String> disabledClusters) {
        final YTreeNode deployLabelNode = YTree.builder().beginMap()
                .key(DISABLED_CLUSTERS_LABEL_KEY)
                .value(YTree.builder()
                        .beginList()
                        .forEach(disabledClusters, YTreeBuilder::value)
                        .endList()
                        .build())
                .key("another_label").value("value2")
                .endMap()
                .build();

        return Map.of(
                "testlabel1", YTree.stringNode("some value"),
                DEPLOY_LABEL_KEY, deployLabelNode);
    }

    @Test
    void getDisabledClustersTest() {

        assertThat(StageContext.getDisabledClusters(Collections.emptyMap(), GlobalContext.EMPTY),
                equalTo(Set.of()));

        assertThat(StageContext.getDisabledClusters(getLabels(Set.of()), GlobalContext.EMPTY),
                equalTo(Set.of()));

        assertThat(StageContext.getDisabledClusters(getLabels(Set.of("man", "vla")), GlobalContext.EMPTY),
                equalTo(Set.of("man", "vla")));

        assertThat(StageContext.getDisabledClusters(Collections.emptyMap(), getContext(Set.of("man", "vla"))),
                equalTo(Set.of("man", "vla")));

        assertThat(StageContext.getDisabledClusters(getLabels(Set.of()), getContext(Set.of("man", "vla"))),
                equalTo(Set.of("man", "vla")));

        assertThat(StageContext.getDisabledClusters(getLabels(Set.of("iva", "man")), getContext(Set.of("man", "vla"))),
                equalTo(Set.of("iva", "man", "vla")));
    }
}
