package ru.yandex.partner.core.entity.block.type.strategy;

import java.util.List;

import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;

import ru.yandex.partner.core.entity.block.model.BlockWithStrategy;
import ru.yandex.partner.core.entity.block.model.RtbBlock;
import ru.yandex.partner.core.holder.ModelPropertiesHolder;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.mock;
import static ru.yandex.partner.core.CoreConstants.Strategies.MIN_CPM_STRATEGY_ID;
import static ru.yandex.partner.core.CoreConstants.Strategies.SEPARATE_CPM_STRATEGY_ID;

class BlockWithStrategyRepositoryTypeSupportTest {

    @Test
    void getEditableModelProperties() {
        BlockWithStrategyRepositoryTypeSupport support =
                new BlockWithStrategyRepositoryTypeSupport(mock(DSLContext.class));
        RtbBlock block = new RtbBlock();
        ModelPropertiesHolder editableModelProperties = support.getEditableModelProperties(block);
        assertThat(editableModelProperties.getEnableProperties()).containsExactlyInAnyOrder(
                List.of(BlockWithStrategy.STRATEGY_TYPE)
        );



        block.setStrategyType(MIN_CPM_STRATEGY_ID);
        editableModelProperties = support.getEditableModelProperties(block);
        assertThat(editableModelProperties.getEnableProperties()).containsExactlyInAnyOrder(
               List.of(BlockWithStrategy.STRATEGY_TYPE),
                List.of(BlockWithStrategy.MINCPM)
        );

        block.setStrategyType(SEPARATE_CPM_STRATEGY_ID);
        editableModelProperties = support.getEditableModelProperties(block);
        assertThat(editableModelProperties.getEnableProperties()).containsExactlyInAnyOrder(
                List.of(BlockWithStrategy.STRATEGY_TYPE),
                List.of(BlockWithStrategy.MEDIA_ACTIVE),
                List.of(BlockWithStrategy.MEDIA_CPM),
                List.of(BlockWithStrategy.MEDIA_BLOCKED),
                List.of(BlockWithStrategy.TEXT_ACTIVE),
                List.of(BlockWithStrategy.TEXT_CPM),
                List.of(BlockWithStrategy.TEXT_BLOCKED)

        );
    }
}
