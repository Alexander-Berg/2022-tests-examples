package ru.yandex.partner.core.entity.block.type.commonshowvideoandstrategy;

import java.util.List;

import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;

import ru.yandex.partner.core.CoreConstants;
import ru.yandex.partner.core.entity.block.model.BlockWithStrategy;
import ru.yandex.partner.core.entity.block.model.RtbBlock;
import ru.yandex.partner.core.holder.ModelPropertiesHolder;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.mock;

class BlockWithCommonShowVideoAndStrategyRepositoryTypeSupportTest {

    @Test
    void getEditableModelProperties() {
        BlockWithCommonShowVideoAndStrategyRepositoryTypeSupport support =
                new BlockWithCommonShowVideoAndStrategyRepositoryTypeSupport(mock(DSLContext.class));
        RtbBlock block = new RtbBlock();
        ModelPropertiesHolder editableModelProperties = support.getEditableModelProperties(block);
        assertThat(editableModelProperties.getEnableProperties()).isEmpty();

        block.setShowVideo(true);
        block.setStrategyType(CoreConstants.Strategies.SEPARATE_CPM_STRATEGY_ID);
        editableModelProperties = support.getEditableModelProperties(block);
        assertThat(editableModelProperties.getEnableProperties()).containsExactlyInAnyOrder(
                List.of(BlockWithStrategy.VIDEO_ACTIVE),
                List.of(BlockWithStrategy.VIDEO_CPM),
                List.of(BlockWithStrategy.VIDEO_BLOCKED)
        );
    }
}
