package ru.yandex.partner.core.entity.designtemplates.actions;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.model.ModelChanges;
import ru.yandex.partner.core.CoreTest;
import ru.yandex.partner.core.action.ActionPerformer;
import ru.yandex.partner.core.entity.QueryOpts;
import ru.yandex.partner.core.entity.designtemplates.actions.all.factories.DesignTemplatesActionAddFactory;
import ru.yandex.partner.core.entity.designtemplates.actions.all.factories.DesignTemplatesDeleteFactory;
import ru.yandex.partner.core.entity.designtemplates.actions.all.factories.DesignTemplatesEditFactory;
import ru.yandex.partner.core.entity.designtemplates.filter.DesignTemplatesFilters;
import ru.yandex.partner.core.entity.designtemplates.model.CommonDesignTemplates;
import ru.yandex.partner.core.entity.designtemplates.model.DesignTemplates;
import ru.yandex.partner.core.entity.designtemplates.service.DesignTemplatesService;
import ru.yandex.partner.core.filter.CoreFilterNode;
import ru.yandex.partner.core.junit.MySqlRefresher;
import ru.yandex.partner.core.multistate.designtemplates.DesignTemplatesStateFlag;
import ru.yandex.partner.dbschema.partner.enums.DesignTemplatesType;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MySqlRefresher.class)
@CoreTest
public class DesignTemplatesActionTest {
    @Autowired
    DesignTemplatesDeleteFactory deleteFactory;
    @Autowired
    DesignTemplatesEditFactory editFactory;
    @Autowired
    ActionPerformer actionPerformer;
    @Autowired
    DesignTemplatesService service;
    @Autowired
    DesignTemplatesActionAddFactory addFactory;


    @Test
    void testDeleteAction() {
        var ids = List.of(1L);
        var templateBefore = service.findAll(QueryOpts.forClass(CommonDesignTemplates.class)
                .withFilter(CoreFilterNode.in(DesignTemplatesFilters.ID, ids))
        ).get(0);
        assertThat(templateBefore.getMultistate().test(DesignTemplatesStateFlag.DELETED)).isFalse();
        var actionDelete = deleteFactory.delete(ids);
        var result = actionPerformer.doActions(actionDelete);

        var templateAfter = service.findAll(QueryOpts.forClass(DesignTemplates.class)
                .withFilter(CoreFilterNode.in(DesignTemplatesFilters.ID, ids))
        ).get(0);
        assertThat(templateAfter.getMultistate().test(DesignTemplatesStateFlag.DELETED)).isTrue();
        assertThat(result.isCommitted()).isTrue();
    }

    @Test
    void actionEditTest() {
        var ids = List.of(1L, 2L);
        var templatesBefore = service.findAll(QueryOpts.forClass(DesignTemplates.class)
                .withFilter(CoreFilterNode.in(DesignTemplatesFilters.ID, ids))
        );
        assertThat(templatesBefore.get(0).getCaption()).isEqualTo("Test template 1");
        var designSettingsBefore = templatesBefore.get(1).getDesignSettings();
        assertThat(designSettingsBefore.get("limit")).isEqualTo("2");
        designSettingsBefore.put("limit", "3");

        var actionEdit = editFactory.edit(List.of(
                new ModelChanges<>(1L, DesignTemplates.class).process("changed_caption", DesignTemplates.CAPTION),
                new ModelChanges<>(2L, DesignTemplates.class).process(designSettingsBefore,
                        DesignTemplates.DESIGN_SETTINGS)
        ));
        var result = actionPerformer.doActions(actionEdit);
        var templatesAfter = service.findAll(QueryOpts.forClass(DesignTemplates.class)
                .withFilter(CoreFilterNode.in(DesignTemplatesFilters.ID, ids))
        );
        assertThat(templatesAfter.get(0).getCaption()).isEqualTo("changed_caption");
        var designSettingsAfter = templatesAfter.get(1).getDesignSettings();
        assertThat(designSettingsAfter.get("limit")).isEqualTo(3);
        assertThat(result.isCommitted()).isTrue();

    }

    @Test
    void testAddAction() {
        var ids = List.of(1L);
        var designTemplate = new DesignTemplates()
                .withId(8L)
                .withPageId(41443L)
                .withBlockId(1L)
                .withCaption("new_template")
                .withDesignSettings(Map.of("name", "new_design",
                        "limit", "2"))
                .withType(DesignTemplatesType.tga)
                .withCustomFormatDirect(false);
        var actionAdd = addFactory.createAction(Map.of(8L, designTemplate), Map.of());
        var result = actionPerformer.doActions(actionAdd);
        assertThat(result.isCommitted()).isTrue();
    }


}
