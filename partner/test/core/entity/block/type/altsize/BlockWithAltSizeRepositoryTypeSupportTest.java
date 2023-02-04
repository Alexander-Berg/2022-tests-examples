package ru.yandex.partner.core.entity.block.type.altsize;

import java.util.List;

import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;

import ru.yandex.partner.core.entity.block.model.BlockWithAltSize;
import ru.yandex.partner.core.entity.block.model.RtbBlock;
import ru.yandex.partner.core.holder.ModelPropertiesHolder;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.mock;

class BlockWithAltSizeRepositoryTypeSupportTest {

    @Test
    void getEditableModelProperties() {
        BlockWithAltSizeRepositoryTypeSupport support =
                new BlockWithAltSizeRepositoryTypeSupport(mock(DSLContext.class));
        support.init();
        ModelPropertiesHolder editableModelProperties = support.getEditableModelProperties(new RtbBlock());
        assertThat(editableModelProperties.getEnableProperties()).containsExactlyInAnyOrder(
                List.of(BlockWithAltSize.ALT_WIDTH),
                List.of(BlockWithAltSize.ALT_HEIGHT)
        );
    }
}
