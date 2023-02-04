package ru.yandex.infra.stage.podspecs.patcher;

import java.util.List;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import ru.yandex.infra.stage.podspecs.SpecPatcher;
import ru.yandex.infra.stage.podspecs.patcher.dummy.first.FirstDummyPatcherV1;
import ru.yandex.infra.stage.podspecs.patcher.dummy.first.FirstDummyPatcherV1Base;
import ru.yandex.infra.stage.podspecs.patcher.dummy.not_included.NotIncludedDummyPatcherV1;
import ru.yandex.infra.stage.podspecs.patcher.dummy.not_included.NotIncludedDummyPatcherV1Base;
import ru.yandex.infra.stage.podspecs.patcher.dummy.second.SecondDummyPatcherV1;
import ru.yandex.infra.stage.podspecs.patcher.dummy.second.SecondDummyPatcherV1Base;
import ru.yandex.infra.stage.podspecs.patcher.dummy.second.SecondDummyPatcherV2;
import ru.yandex.infra.stage.podspecs.patcher.dummy.third.ThirdDummyPatcherV1;
import ru.yandex.infra.stage.podspecs.patcher.dummy.third.ThirdDummyPatcherV1Base;
import ru.yandex.infra.stage.podspecs.patcher.dummy.third.ThirdDummyPatcherV2;
import ru.yandex.infra.stage.podspecs.patcher.dummy.third.ThirdDummyPatcherV2Base;
import ru.yandex.yp.client.api.TPodTemplateSpec;

import static ru.yandex.infra.stage.podspecs.patcher.DummyPatchersHolderFactory.CLASS_TO_PATCHER;
import static ru.yandex.infra.stage.util.AssertUtils.assertThatEquals;
import static ru.yandex.infra.stage.util.AssertUtils.assertThatSameInstance;
import static ru.yandex.infra.stage.util.AssertUtils.assertThatThrowsWithMessage;

public class PatchersHolderImplTest {

    private static PatchersHolder<TPodTemplateSpec.Builder> patchersHolder;

    @BeforeAll
    static void beforeTest() {
        patchersHolder = DummyPatchersHolderFactory.create();
    }

    @Test
    void testGetPatchersInExactOrder() {
        List<Class<? extends SpecPatcher<TPodTemplateSpec.Builder>>> patchersOrder = ImmutableList.of(
                FirstDummyPatcherV1.class,
                SecondDummyPatcherV1.class, SecondDummyPatcherV2.class,
                ThirdDummyPatcherV1.class, ThirdDummyPatcherV2.class
        );

        var patchers = patchersHolder.getPatchersInExactOrder(patchersOrder);

        assertThatEquals(patchers.size(), patchersOrder.size());
        for (int index = 0; index < patchersOrder.size(); ++index) {
            var patcherClass = patchersOrder.get(index);
            var expectedPatcher = CLASS_TO_PATCHER.get(patcherClass);

            var actualPatcher = patchers.get(index);

            assertThatEquals(actualPatcher.getClass(), patcherClass);
            assertThatSameInstance(actualPatcher, expectedPatcher);
        }
    }

    @ParameterizedTest
    @ValueSource(classes = {
            FirstDummyPatcherV1Base.class,
            SecondDummyPatcherV1Base.class,
            ThirdDummyPatcherV1Base.class,
            ThirdDummyPatcherV2Base.class,
            NotIncludedDummyPatcherV1.class,
            NotIncludedDummyPatcherV1Base.class
    })
    void failForNotFoundClass(Class<? extends SpecPatcher<TPodTemplateSpec.Builder>> shouldNotBeFoundPatcherClass) {
        var patchersOrder = ImmutableList.of(
                FirstDummyPatcherV1.class, shouldNotBeFoundPatcherClass, ThirdDummyPatcherV1.class
        );

        assertThatThrowsWithMessage(RuntimeException.class,
                String.format("Patcher with class %s not found", shouldNotBeFoundPatcherClass.getCanonicalName()),
                () -> patchersHolder.getPatchersInExactOrder(patchersOrder)
        );
    }
}
