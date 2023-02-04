package ru.yandex.partner.core.entity.block.type.adfox;

import java.util.List;

import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;

import ru.yandex.partner.core.entity.block.model.RtbBlock;
import ru.yandex.partner.core.holder.ModelPropertiesHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static ru.yandex.partner.core.entity.block.model.prop.BlockWithAdfoxAdfoxBlockPropHolder.ADFOX_BLOCK;

class BlockWithAdfoxRepositoryTypeSupportTest {

    @Test
    void getEditableModelProperties() {
        BlockWithAdfoxRepositoryTypeSupport support =
                new BlockWithAdfoxRepositoryTypeSupport(mock(DSLContext.class));
        support.init();
        ModelPropertiesHolder editableModelProperties = support.getEditableModelProperties(new RtbBlock());
        assertThat(editableModelProperties.getEnableProperties()).containsExactlyInAnyOrder(
                List.of(ADFOX_BLOCK)
        );
    }
}
