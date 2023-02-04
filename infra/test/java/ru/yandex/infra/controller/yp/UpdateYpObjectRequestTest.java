package ru.yandex.infra.controller.yp;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.ytree.YTreeStringNode;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static ru.yandex.yp.client.api.DataModel.TGroupSpec;
import static ru.yandex.yp.client.api.DataModel.TGroupStatus;

public class UpdateYpObjectRequestTest {
    private static final String KEY = "key";
    private static final YTreeStringNode VALUE = YTree.stringNode("value");

    @Test
    void setImmutableLabelsAndAddNewLabelTest() {
        UpdateYpObjectRequest<TGroupSpec, TGroupStatus> request = new UpdateYpObjectRequest.Builder<TGroupSpec, TGroupStatus>()
                .setLabels(Collections.emptyMap())
                .addLabel(KEY, VALUE)
                .build();

        assertThat(request.getLabels(), hasEntry(KEY, VALUE));
    }

}
