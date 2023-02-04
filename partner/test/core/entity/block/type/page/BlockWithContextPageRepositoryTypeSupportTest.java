package ru.yandex.partner.core.entity.block.type.page;

import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;

import ru.yandex.partner.core.entity.block.model.RtbBlock;
import ru.yandex.partner.core.entity.page.service.PageService;
import ru.yandex.partner.core.holder.ModelPropertiesHolder;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.mock;

class BlockWithContextPageRepositoryTypeSupportTest {

    @Test
    void getEditableModelProperties() {
        BlockWithContextPageRepositoryTypeSupport support =
                new BlockWithContextPageRepositoryTypeSupport(mock(DSLContext.class), mock(PageService.class));
        ModelPropertiesHolder editableModelProperties = support.getEditableModelProperties(new RtbBlock());
        assertThat(editableModelProperties.getEnableProperties()).isEmpty();
    }
}
