package ru.yandex.partner.core.entity.block.type.picategories;

import java.util.List;

import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;

import ru.yandex.partner.core.entity.block.model.BlockWithPiCategories;
import ru.yandex.partner.core.entity.block.model.RtbBlock;
import ru.yandex.partner.core.entity.picategories.PiCategoriesRepository;
import ru.yandex.partner.core.holder.ModelPropertiesHolder;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.mock;

class BlockWithPiCategoriesRepositoryTypeSupportTest {

    @Test
    void getEditableModelProperties() {
        BlockWithPiCategoriesRepositoryTypeSupport support =
                new BlockWithPiCategoriesRepositoryTypeSupport(
                        mock(DSLContext.class), mock(PiCategoriesRepository.class),
                        mock(PiCategoriesDictService.class));
        ModelPropertiesHolder editableModelProperties = support.getEditableModelProperties(new RtbBlock());
        assertThat(editableModelProperties.getEnableProperties()).containsExactlyInAnyOrder(
                List.of(BlockWithPiCategories.PI_CATEGORIES)
        );
    }
}
