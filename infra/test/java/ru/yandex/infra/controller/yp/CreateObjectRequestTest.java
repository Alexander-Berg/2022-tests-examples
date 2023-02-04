package ru.yandex.infra.controller.yp;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import ru.yandex.yp.client.api.DataModel.TGroupSpec;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;

public class CreateObjectRequestTest {
    private static final String KEY = "key";
    private static final String VALUE = "value";

    @Test
    void setImmutableLabelsAndAddNewLabelTest() {
        CreateObjectRequest<TGroupSpec> request = new CreateObjectRequest.Builder<>(TGroupSpec.newBuilder().build())
                .setLabels(Collections.emptyMap())
                .addLabel(KEY, VALUE)
                .build();

        assertThat(request.getLabels(), hasEntry(KEY, VALUE));
    }

    @Test
    void setImmutableMetaFieldsAndAddNewFieldTest() {
        CreateObjectRequest<TGroupSpec> request = new CreateObjectRequest.Builder<>(TGroupSpec.newBuilder().build())
                .setSpecificMetaFields(Collections.emptyMap())
                .addSpecificMetaField(KEY, VALUE)
                .build();

        assertThat(request.getSpecificMetaFields(), hasEntry(KEY, VALUE));
    }
}
