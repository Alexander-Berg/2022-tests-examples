package ru.yandex.partner.core.entity.block.type.dspblocks;

import java.util.List;

import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;

import ru.yandex.partner.core.entity.block.model.BlockWithDspBlocks;
import ru.yandex.partner.core.entity.block.model.RtbBlock;
import ru.yandex.partner.core.entity.media.MediaSizeRepository;
import ru.yandex.partner.core.holder.ModelPropertiesHolder;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.mock;

class BlockWithDspBlocksRepositoryTypeSupportTest {

    @Test
    void getEditableModelProperties() {
        BlockWithDspBlocksRepositoryTypeSupport support =
                new BlockWithDspBlocksRepositoryTypeSupport(mock(DSLContext.class), mock(MediaSizeRepository.class));
        ModelPropertiesHolder editableModelProperties = support.getEditableModelProperties(new RtbBlock());
        assertThat(editableModelProperties.getEnableProperties()).containsExactlyInAnyOrder(
                List.of(BlockWithDspBlocks.DSP_BLOCKS)
        );
    }
}
