package ru.yandex.partner.core.action.factories;

import java.util.Set;

import org.junit.jupiter.api.Test;

import ru.yandex.direct.model.ModelProperty;
import ru.yandex.partner.core.entity.block.model.RtbBlock;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ActionConfigurationBuilderTest {

    @Test
    void buildNestedTest() {
        var child1 = new ActionConfigurationBuilder<>()
                .dependsOn(RtbBlock.ID)
                .dependsOn(RtbBlock.MULTISTATE)
                .build();

        var child2 = new ActionConfigurationBuilder<>()
                .dependsOn(RtbBlock.PAGE_ID)
                .dependsOn(RtbBlock.ADFOX_BLOCK)
                .build();

        var parent = new ActionConfigurationBuilder<>()
                .dependsOn(RtbBlock.BK_DATA)
                .dependsOn(RtbBlock.IS_CUSTOM_BK_DATA)
                .allowNestedInternal(child1)
                .allowNestedInternal(child2)
                .build();

        Set<ModelProperty<?, ?>> expected = Set.of(
                RtbBlock.BK_DATA, RtbBlock.IS_CUSTOM_BK_DATA, RtbBlock.PAGE_ID,
                RtbBlock.ADFOX_BLOCK, RtbBlock.ID, RtbBlock.MULTISTATE
        );

        assertTrue(parent.getDependsOn().containsAll(expected));
    }
}
