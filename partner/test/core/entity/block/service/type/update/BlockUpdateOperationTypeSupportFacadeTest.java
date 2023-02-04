package ru.yandex.partner.core.entity.block.service.type.update;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.model.ModelProperty;
import ru.yandex.partner.core.CoreTest;
import ru.yandex.partner.core.entity.block.model.BaseBlock;
import ru.yandex.partner.core.entity.block.model.RtbBlock;

import static org.assertj.core.api.Assertions.assertThat;

@CoreTest
class BlockUpdateOperationTypeSupportFacadeTest {
    @Autowired
    List<AbstractBlockUpdateOperationTypeSupport<? extends BaseBlock>> supports;

    @Test
    void testBkUpdatingFields() {
        // BlockStrategy props commented out
        assertThat(supports.stream().flatMap(sup -> sup.needBsResyncProps().stream())
                .map(ModelProperty.class::cast)
                .collect(Collectors.toSet())
        ).containsAll(List.of(
                RtbBlock.ALT_HEIGHT,
                RtbBlock.ALT_WIDTH,
                RtbBlock.ALTERNATIVE_CODE,
                RtbBlock.BK_DATA,
                RtbBlock.BLIND,
                RtbBlock.BRANDS,
                RtbBlock.CAPTION,
                RtbBlock.CUSTOM_BK_OPTIONS,
                RtbBlock.DESIGN_TEMPLATES,
                RtbBlock.DSP_BLOCKS,
                RtbBlock.DSPS,
                RtbBlock.GEO,
                RtbBlock.HORIZONTAL_ALIGN,
                RtbBlock.IS_CUSTOM_BK_DATA,
                RtbBlock.ONLY_PORTAL_TRUSTED_BANNERS,
                RtbBlock.ORDER_TAGS,
                RtbBlock.PI_CATEGORIES,
                RtbBlock.SHOW_VIDEO,
                RtbBlock.TARGET_TAGS,
                RtbBlock.DSPS_UNMODERATED,
                RtbBlock.MEDIA_ACTIVE,
                RtbBlock.MEDIA_BLOCKED,
                RtbBlock.MEDIA_CPM,
                RtbBlock.MINCPM,
                RtbBlock.STRATEGY_TYPE,
                RtbBlock.TEXT_ACTIVE,
                RtbBlock.TEXT_BLOCKED,
                RtbBlock.TEXT_CPM,
                RtbBlock.VIDEO_ACTIVE,
                RtbBlock.VIDEO_BLOCKED,
                RtbBlock.VIDEO_CPM


        ));
    }
}
