package ru.yandex.partner.core.entity.block.type.geo;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;

import ru.yandex.partner.core.entity.block.model.BlockWithGeo;
import ru.yandex.partner.core.entity.block.model.RtbBlock;
import ru.yandex.partner.core.entity.crimea.CrimeaRepository;
import ru.yandex.partner.core.entity.geo.GeoBaseRepository;
import ru.yandex.partner.core.entity.geo.GeoService;
import ru.yandex.partner.core.holder.ModelPropertiesHolder;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.mock;

class BlockWithGeoRepositoryTypeSupportTest {

    @Test
    void getEditableModelProperties() {
        BlockWithGeoRepositoryTypeSupport support =
                new BlockWithGeoRepositoryTypeSupport(mock(DSLContext.class), mock(ObjectMapper.class),
                        mock(GeoService.class), mock(GeoBaseRepository.class),
                        mock(CrimeaRepository.class));
        ModelPropertiesHolder editableModelProperties = support.getEditableModelProperties(new RtbBlock());
        assertThat(editableModelProperties.getEnableProperties()).containsExactlyInAnyOrder(
                List.of(BlockWithGeo.GEO)
        );
    }
}
