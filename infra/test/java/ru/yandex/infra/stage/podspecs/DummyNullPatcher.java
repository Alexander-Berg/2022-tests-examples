package ru.yandex.infra.stage.podspecs;

import com.google.protobuf.Message;

import ru.yandex.infra.stage.deployunit.DeployUnitContext;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTreeBuilder;
import ru.yandex.yp.client.api.TPodTemplateSpec;

public class DummyNullPatcher<T extends Message.Builder> implements SpecPatcher<T> {
    public static final DummyNullPatcher<TPodTemplateSpec.Builder> INSTANCE = new DummyNullPatcher<>();

    @Override
    public void patch(T builder, DeployUnitContext context, YTreeBuilder labelsBuilder) {
    }
}
