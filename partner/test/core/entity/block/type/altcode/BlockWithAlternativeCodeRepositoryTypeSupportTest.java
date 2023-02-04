package ru.yandex.partner.core.entity.block.type.altcode;

import java.util.List;

import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;

import ru.yandex.partner.core.entity.block.model.BlockWithAlternativeCode;
import ru.yandex.partner.core.entity.block.model.RtbBlock;
import ru.yandex.partner.core.holder.ModelPropertiesHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class BlockWithAlternativeCodeRepositoryTypeSupportTest {

    @Test
    void getEditableModelProperties() {
        BlockWithAlternativeCodeRepositoryTypeSupport support =
                new BlockWithAlternativeCodeRepositoryTypeSupport(mock(DSLContext.class));
        support.init();
        ModelPropertiesHolder editableModelProperties = support.getEditableModelProperties(new RtbBlock());
        assertThat(editableModelProperties.getEnableProperties()).containsExactly(
                List.of(BlockWithAlternativeCode.ALTERNATIVE_CODE)
        );
    }
}
