package ru.yandex.partner.core.entity.block.type.common;

import java.util.List;

import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;

import ru.yandex.partner.core.entity.block.model.RtbBlock;
import ru.yandex.partner.core.holder.ModelPropertiesHolder;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.mock;
import static ru.yandex.partner.core.entity.block.model.prop.BlockWithCommonFieldsCaptionPropHolder.CAPTION;
import static ru.yandex.partner.core.entity.block.model.prop.BlockWithCommonFieldsCommentPropHolder.COMMENT;

class BlockWithCommonFieldsRepositoryTypeSupportTest {

    @Test
    void getEditableModelProperties() {
        BlockWithCommonFieldsRepositoryTypeSupport support =
                new BlockWithCommonFieldsRepositoryTypeSupport(mock(DSLContext.class));
        support.init();
        ModelPropertiesHolder editableModelProperties = support.getEditableModelProperties(new RtbBlock());
        assertThat(editableModelProperties.getEnableProperties()).containsExactlyInAnyOrder(
                List.of(CAPTION),
                List.of(COMMENT)
        );
    }
}
