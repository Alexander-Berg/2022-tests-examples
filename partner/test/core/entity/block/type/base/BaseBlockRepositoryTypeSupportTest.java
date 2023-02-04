package ru.yandex.partner.core.entity.block.type.base;

import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;

import ru.yandex.partner.core.block.BlockModelTypesHolder;
import ru.yandex.partner.core.entity.block.model.RtbBlock;
import ru.yandex.partner.core.holder.ModelPropertiesHolder;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.mock;

class BaseBlockRepositoryTypeSupportTest {

    @Test
    void getEditableModelProperties() {
        BaseBlockRepositoryTypeSupport support;
        support = new BaseBlockRepositoryTypeSupport(mock(DSLContext.class), new BlockModelTypesHolder());
        ModelPropertiesHolder editableModelProperties = support.getEditableModelProperties(new RtbBlock());
        assertThat(editableModelProperties.getEnableProperties()).isEmpty();
    }
}
