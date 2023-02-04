package ru.yandex.partner.core.entity.block.type.custombkdata;

import java.util.List;

import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;

import ru.yandex.partner.core.entity.block.model.BlockWithCustomBkDataFlag;
import ru.yandex.partner.core.entity.block.model.RtbBlock;
import ru.yandex.partner.core.holder.ModelPropertiesHolder;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.mock;

class BlockWithCustomBkDataFlagRepositoryTypeSupportTest {

    @Test
    void getEditableModelProperties() {
        BlockWithCustomBkDataFlagRepositoryTypeSupport support =
                new BlockWithCustomBkDataFlagRepositoryTypeSupport(mock(DSLContext.class));
        support.init();
        ModelPropertiesHolder editableModelProperties = support.getEditableModelProperties(new RtbBlock());
        assertThat(editableModelProperties.getEnableProperties()).containsExactlyInAnyOrder(
                List.of(BlockWithCustomBkDataFlag.IS_CUSTOM_BK_DATA)
        );
    }
}
