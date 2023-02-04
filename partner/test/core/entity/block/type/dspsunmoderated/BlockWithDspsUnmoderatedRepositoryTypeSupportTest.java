package ru.yandex.partner.core.entity.block.type.dspsunmoderated;

import java.util.List;

import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;

import ru.yandex.partner.core.entity.block.model.BlockWithDspsUnmoderated;
import ru.yandex.partner.core.entity.block.model.RtbBlock;
import ru.yandex.partner.core.entity.dsp.repository.DspModifyRepository;
import ru.yandex.partner.core.entity.dsp.repository.DspTypedRepository;
import ru.yandex.partner.core.holder.ModelPropertiesHolder;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.mock;

class BlockWithDspsUnmoderatedRepositoryTypeSupportTest {

    @Test
    void getEditableModelProperties() {
        BlockWithDspsUnmoderatedRepositoryTypeSupport support =
                new BlockWithDspsUnmoderatedRepositoryTypeSupport(
                        mock(DSLContext.class), mock(DspTypedRepository.class), mock(DspModifyRepository.class));
        ModelPropertiesHolder editableModelProperties = support.getEditableModelProperties(new RtbBlock());
        assertThat(editableModelProperties.getEnableProperties()).containsExactlyInAnyOrder(
                List.of(BlockWithDspsUnmoderated.DSPS_UNMODERATED)
        );
    }
}
