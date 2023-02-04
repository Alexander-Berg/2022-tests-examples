package ru.yandex.partner.core.entity.block.type.commonshowvideo;

import java.util.List;

import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;

import ru.yandex.partner.core.entity.block.model.BlockWithCommonShowVideo;
import ru.yandex.partner.core.entity.block.model.RtbBlock;
import ru.yandex.partner.core.holder.ModelPropertiesHolder;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.mock;

class BlockWithCommonShowVideoRepositoryTypeSupportTest {

    @Test
    void getEditableModelProperties() {
        BlockWithCommonShowVideoRepositoryTypeSupport support =
                new BlockWithCommonShowVideoRepositoryTypeSupport(mock(DSLContext.class));
        support.init();
        ModelPropertiesHolder editableModelProperties = support.getEditableModelProperties(new RtbBlock());
        assertThat(editableModelProperties.getEnableProperties()).containsExactlyInAnyOrder(
                List.of(BlockWithCommonShowVideo.SHOW_VIDEO)
        );
    }
}
