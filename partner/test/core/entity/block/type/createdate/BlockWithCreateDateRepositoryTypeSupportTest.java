package ru.yandex.partner.core.entity.block.type.createdate;

import java.util.List;

import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;

import ru.yandex.partner.core.entity.block.model.BlockWithCreateDate;
import ru.yandex.partner.core.entity.block.model.RtbBlock;
import ru.yandex.partner.core.holder.ModelPropertiesHolder;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.mock;

class BlockWithCreateDateRepositoryTypeSupportTest {

    @Test
    void getEditableModelProperties() {
        BlockWithCreateDateRepositoryTypeSupport support =
                new BlockWithCreateDateRepositoryTypeSupport(mock(DSLContext.class));
        support.init();
        ModelPropertiesHolder editableModelProperties = support.getEditableModelProperties(new RtbBlock());
        assertThat(editableModelProperties.getEnableProperties()).containsExactlyInAnyOrder(
                List.of(BlockWithCreateDate.CREATE_DATE)
        );
    }
}
