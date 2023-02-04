package ru.yandex.infra.stage.podspecs.patcher;

import java.util.Map;

import com.google.common.collect.ImmutableMap;

import ru.yandex.infra.stage.podspecs.SpecPatcher;
import ru.yandex.infra.stage.podspecs.patcher.dummy.first.FirstDummyPatcherV1;
import ru.yandex.infra.stage.podspecs.patcher.dummy.second.SecondDummyPatcherV1;
import ru.yandex.infra.stage.podspecs.patcher.dummy.second.SecondDummyPatcherV2;
import ru.yandex.infra.stage.podspecs.patcher.dummy.third.ThirdDummyPatcherV1;
import ru.yandex.infra.stage.podspecs.patcher.dummy.third.ThirdDummyPatcherV2;
import ru.yandex.yp.client.api.TPodTemplateSpec;

public class DummyPatchersHolderFactory {

    public static final Map<Class<? extends SpecPatcher<TPodTemplateSpec.Builder>>, SpecPatcher<TPodTemplateSpec.Builder>> CLASS_TO_PATCHER = ImmutableMap.of(
            FirstDummyPatcherV1.class, new FirstDummyPatcherV1(),
            SecondDummyPatcherV1.class, new SecondDummyPatcherV1(),
            SecondDummyPatcherV2.class, new SecondDummyPatcherV2(),
            ThirdDummyPatcherV1.class, new ThirdDummyPatcherV1(),
            ThirdDummyPatcherV2.class, new ThirdDummyPatcherV2()
    );

    public static PatchersHolder<TPodTemplateSpec.Builder> create() {
        return new PatchersHolder<>(CLASS_TO_PATCHER);
    }
}
