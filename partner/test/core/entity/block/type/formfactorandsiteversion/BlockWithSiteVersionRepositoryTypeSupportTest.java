package ru.yandex.partner.core.entity.block.type.formfactorandsiteversion;

import java.util.List;

import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;

import ru.yandex.partner.core.entity.block.model.RtbBlock;
import ru.yandex.partner.core.holder.ModelPropertiesHolder;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.mock;
import static ru.yandex.partner.core.entity.block.model.prop.BlockWithSiteVersionSiteVersionPropHolder.SITE_VERSION;

class BlockWithSiteVersionRepositoryTypeSupportTest {

    @Test
    void getEditableModelProperties() {
        BlockWithSiteVersionRepositoryTypeSupport support =
                new BlockWithSiteVersionRepositoryTypeSupport(mock(DSLContext.class));
        ModelPropertiesHolder editableModelProperties = support.getEditableModelProperties(new RtbBlock());
        assertThat(editableModelProperties.getEnableProperties()).containsExactlyInAnyOrder(
                List.of(SITE_VERSION)
        );
    }
}
