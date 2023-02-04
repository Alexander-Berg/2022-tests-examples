package ru.yandex.partner.core.entity.block.type.designtemplates;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;

import ru.yandex.partner.core.action.ActionPerformerImpl;
import ru.yandex.partner.core.entity.block.model.BlockWithDesignTemplates;
import ru.yandex.partner.core.entity.block.model.RtbBlock;
import ru.yandex.partner.core.entity.designtemplates.actions.all.factories.DesignTemplatesActionAddFactory;
import ru.yandex.partner.core.entity.designtemplates.service.DesignTemplatesService;
import ru.yandex.partner.core.entity.designtemplates.service.add.DesignTemplatesAddOperationFactory;
import ru.yandex.partner.core.holder.ModelPropertiesHolder;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.mock;

class BlockWithDesignTemplatesRepositoryTypeSupportTest {

    @Test
    void getEditableModelProperties() {
        BlockWithDesignTemplatesRepositoryTypeSupport support =
                new BlockWithDesignTemplatesRepositoryTypeSupport(
                        mock(DSLContext.class), mock(DesignTemplatesService.class),
                        mock(DesignTemplatesAddOperationFactory.class),
                        mock(DesignTemplatesActionAddFactory.class),
                        mock(ActionPerformerImpl.class),
                        mock(ObjectMapper.class));
        ModelPropertiesHolder editableModelProperties = support.getEditableModelProperties(new RtbBlock());
        assertThat(editableModelProperties.getEnableProperties()).containsExactlyInAnyOrder(
                List.of(BlockWithDesignTemplates.DESIGN_TEMPLATES)
        );
    }
}
