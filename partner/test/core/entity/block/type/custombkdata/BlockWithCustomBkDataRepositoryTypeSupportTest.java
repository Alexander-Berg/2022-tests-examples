package ru.yandex.partner.core.entity.block.type.custombkdata;

import java.util.List;

import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;

import ru.yandex.partner.core.entity.block.model.BlockWithCustomBkData;
import ru.yandex.partner.core.entity.block.model.RtbBlock;
import ru.yandex.partner.core.holder.ModelPropertiesHolder;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.mock;

class BlockWithCustomBkDataRepositoryTypeSupportTest {

    @Test
    void getEditableModelProperties() {
        BlockWithCustomBkDataRepositoryTypeSupport support =
                new BlockWithCustomBkDataRepositoryTypeSupport(mock(DSLContext.class));
        support.init();
        ModelPropertiesHolder editableModelProperties = support.getEditableModelProperties(new RtbBlock());
        assertThat(editableModelProperties.getEnableProperties()).containsExactlyInAnyOrder(
                List.of(BlockWithCustomBkData.BK_DATA)
        );
    }
}
