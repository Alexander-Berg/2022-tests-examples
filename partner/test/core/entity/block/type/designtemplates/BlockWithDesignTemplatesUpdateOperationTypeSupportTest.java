package ru.yandex.partner.core.entity.block.type.designtemplates;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.model.ModelChanges;
import ru.yandex.partner.core.CoreTest;
import ru.yandex.partner.core.action.ActionPerformer;
import ru.yandex.partner.core.entity.block.actions.rtb.external.RtbBlockEditFactory;
import ru.yandex.partner.core.entity.block.model.BlockWithDesignTemplates;
import ru.yandex.partner.core.entity.block.model.RtbBlock;
import ru.yandex.partner.core.entity.block.repository.BlockTypedRepository;
import ru.yandex.partner.core.entity.designtemplates.model.DesignTemplates;
import ru.yandex.partner.core.junit.MySqlRefresher;
import ru.yandex.partner.core.multistate.designtemplates.DesignTemplatesMultistate;
import ru.yandex.partner.dbschema.partner.enums.DesignTemplatesType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MySqlRefresher.class)
@CoreTest
class BlockWithDesignTemplatesUpdateOperationTypeSupportTest {
    @Autowired
    RtbBlockEditFactory rtbBlockEditFactory;
    @Autowired
    ActionPerformer actionPerformer;
    @Autowired
    BlockTypedRepository blockTypedRepository;

    /**
     * Если дизайн, которым патчим, имеет айдишку существующего дизайна,
     * то мы должны его просто отредактировать
     */
    @Test
    void simpleEditTemplate() {
        DesignTemplates templateToUpdate = new DesignTemplates()
                // set id to edit template
                .withId(3L)
                .withBlockId(1L)
                .withPageId(41443L)
                .withType(DesignTemplatesType.tga)
                .withCaption("hello caption")
                .withDesignSettings(
                        Map.of(
                                "name", "inpage",
                                "limit", 4
                        )
                )
                .withCustomFormatDirect(false)
                .withFilterTags(List.of("tag1", "tag2"));
        var result = actionPerformer.doActions(rtbBlockEditFactory.edit(List.of(
                new ModelChanges<>(347649081345L, RtbBlock.class)
                        .process(List.of(
                                templateToUpdate
                        ), BlockWithDesignTemplates.DESIGN_TEMPLATES)
        )));

        assertThat(result.getErrors()).isEmpty();
        assertTrue(result.isCommitted());

        RtbBlock blockAfter = blockTypedRepository.getSafely(List.of(347649081345L), RtbBlock.class).get(0);
        assertThat(blockAfter.getDesignTemplates()).hasSize(1);
        DesignTemplates designTemplatesAfter = blockAfter.getDesignTemplates().get(0);

        assertThat(designTemplatesAfter.getUpdateTime()).isNotNull();

        assertThat(designTemplatesAfter).isEqualTo(
                templateToUpdate.withMultistate(new DesignTemplatesMultistate())
                        // steal update time for comparison
                        .withUpdateTime(designTemplatesAfter.getUpdateTime())
        );
    }

    /**
     * Если патч-дизайн айдишки _не имеет_,
     * то мы заменяем все существующие на блоке дизайны новым
     */
    @Test
    void simpleEditReplaceTemplate() {
        DesignTemplates templateToReplaceWith = new DesignTemplates()
                // do not set id to rewrite template
                .withBlockId(1L)
                .withPageId(41443L)
                .withType(DesignTemplatesType.tga)
                .withCaption("hello caption")
                .withDesignSettings(
                        Map.of(
                                "name", "inpage",
                                "limit", 4
                        )
                )
                .withCustomFormatDirect(false)
                .withFilterTags(List.of("tag1", "tag2"));
        var result = actionPerformer.doActions(rtbBlockEditFactory.edit(List.of(
                new ModelChanges<>(347649081345L, RtbBlock.class)
                        .process(List.of(
                                templateToReplaceWith
                        ), BlockWithDesignTemplates.DESIGN_TEMPLATES)
        )));

        assertThat(result.getErrors()).isEmpty();
        assertTrue(result.isCommitted());

        RtbBlock blockAfter = blockTypedRepository.getSafely(List.of(347649081345L), RtbBlock.class).get(0);
        assertThat(blockAfter.getDesignTemplates()).hasSize(1);
        DesignTemplates designTemplatesAfter = blockAfter.getDesignTemplates().get(0);

        assertThat(designTemplatesAfter).isEqualTo(templateToReplaceWith
                .withId(13395349L)
                .withMultistate(new DesignTemplatesMultistate())
                // steal update time for comparison
                .withUpdateTime(designTemplatesAfter.getUpdateTime())
        );
    }
}
