package ru.yandex.partner.core.entity.block.type.tags;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;

import ru.yandex.partner.core.entity.block.model.BlockWithTags;
import ru.yandex.partner.core.entity.block.model.RtbBlock;
import ru.yandex.partner.core.entity.custombkoptions.CustomBkOptionsTypedRepository;
import ru.yandex.partner.core.holder.ModelPropertiesHolder;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.mock;

class BlockWithTagsRepositoryTypeSupportTest {

    @Test
    void getEditableModelProperties() {
        BlockWithTagsRepositoryTypeSupport support =
                new BlockWithTagsRepositoryTypeSupport(mock(DSLContext.class), mock(ObjectMapper.class),
                        mock(CustomBkOptionsTypedRepository.class),
                        mock(TagService.class));
        support.init();
        ModelPropertiesHolder editableModelProperties = support.getEditableModelProperties(new RtbBlock());
        assertThat(editableModelProperties.getEnableProperties()).containsExactlyInAnyOrder(
                List.of(BlockWithTags.ORDER_TAGS),
                List.of(BlockWithTags.TARGET_TAGS)
        );
    }
}
