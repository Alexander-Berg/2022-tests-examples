package ru.yandex.infra.auth.yp;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;

import ru.yandex.infra.controller.yp.YpObject;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTreeBuilder;
import ru.yandex.yp.client.api.DataModel;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class YpGroupTest {

    @Test
    void fromProtoIgnoreNonStringLabels() {

        var spec = DataModel.TGroupSpec.newBuilder()
                        .addMembers("user1")
                        .addMembers("user2")
                        .build();
        var labels = ImmutableMap.of("boolLabel", new YTreeBuilder().value(true).build(),
                "stringLabel1", new YTreeBuilder().value("value1").build(),
                "intLabel", new YTreeBuilder().value(45).build(),
                "stringLabel2", new YTreeBuilder().value("value2").build());
        var groups = YpGroup.fromProto("group1", new YpObject<>(spec, null, null, null, labels, 0, 0, 0));
        assertThat(groups.getLabels(), equalTo(Map.of("stringLabel1", "value1", "stringLabel2", "value2")));
    }

}
