package ru.yandex.partner.core.entity.block.type.rtb;

import java.util.List;

import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;

import ru.yandex.partner.core.entity.block.model.BlockWithRtbFields;
import ru.yandex.partner.core.entity.block.model.RtbBlock;
import ru.yandex.partner.core.holder.ModelPropertiesHolder;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.mock;

class BlockWithRtbFieldsRepositoryTypeSupportTest {

    @Test
    void getEditableModelProperties() {
        BlockWithRtbFieldsRepositoryTypeSupport support =
                new BlockWithRtbFieldsRepositoryTypeSupport(mock(DSLContext.class));
        support.init();
        ModelPropertiesHolder editableModelProperties = support.getEditableModelProperties(new RtbBlock());
        assertThat(editableModelProperties.getEnableProperties()).containsExactlyInAnyOrder(
                List.of(BlockWithRtbFields.BLIND),
                List.of(BlockWithRtbFields.HORIZONTAL_ALIGN)
        );
    }
}
