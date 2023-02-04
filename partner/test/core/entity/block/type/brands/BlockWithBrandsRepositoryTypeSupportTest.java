package ru.yandex.partner.core.entity.block.type.brands;

import java.util.List;

import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;

import ru.yandex.partner.core.entity.block.model.BlockWithBrands;
import ru.yandex.partner.core.entity.block.model.RtbBlock;
import ru.yandex.partner.core.entity.brand.BrandRepository;
import ru.yandex.partner.core.holder.ModelPropertiesHolder;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.mock;

class BlockWithBrandsRepositoryTypeSupportTest {

    @Test
    void getEditableModelProperties() {
        BlockWithBrandsRepositoryTypeSupport support =
                new BlockWithBrandsRepositoryTypeSupport(
                        mock(DSLContext.class), mock(BrandRepository.class));
        ModelPropertiesHolder editableModelProperties = support.getEditableModelProperties(new RtbBlock());
        assertThat(editableModelProperties.getEnableProperties()).containsExactlyInAnyOrder(
                List.of(BlockWithBrands.BRANDS)
        );
    }
}
