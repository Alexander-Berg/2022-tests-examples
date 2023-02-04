package ru.yandex.partner.core.entity.designtemplates;

import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.model.Model;
import ru.yandex.direct.model.ModelProperty;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.partner.core.CoreTest;
import ru.yandex.partner.core.block.BlockUniqueIdConverter;
import ru.yandex.partner.core.entity.IncomingFields;
import ru.yandex.partner.core.entity.QueryOpts;
import ru.yandex.partner.core.entity.block.filter.BlockFilters;
import ru.yandex.partner.core.entity.block.model.RtbBlock;
import ru.yandex.partner.core.entity.block.service.BlockService;
import ru.yandex.partner.core.entity.designtemplates.model.CommonDesignTemplates;
import ru.yandex.partner.core.entity.designtemplates.model.DesignTemplates;
import ru.yandex.partner.core.entity.designtemplates.repository.DesignTemplatesModifyRepository;
import ru.yandex.partner.core.entity.designtemplates.service.add.DesignTemplatesAddOperationFactory;
import ru.yandex.partner.core.entity.designtemplates.service.add.DesignTemplatesAddOperationTypeSupportFacade;
import ru.yandex.partner.core.entity.designtemplates.service.validation.DesignTemplatesValidationTypeSupportFacade;
import ru.yandex.partner.core.filter.CoreFilterNode;
import ru.yandex.partner.core.junit.MySqlRefresher;
import ru.yandex.partner.dbschema.partner.enums.DesignTemplatesType;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static ru.yandex.partner.core.block.BlockUniqueIdConverter.Prefixes.CONTEXT_ON_SITE_RTB_PREFIX;

@ExtendWith(MySqlRefresher.class)
@CoreTest
public class DesignTemplatesAddTest {

    @Autowired
    DSLContext dslContext;

    @Autowired
    BlockService blockService;

    @Autowired
    DesignTemplatesAddOperationTypeSupportFacade addFacade;

    @Autowired
    DesignTemplatesValidationTypeSupportFacade validationFacade;

    @Autowired
    DesignTemplatesModifyRepository modifyRepository;

    @Autowired
    DesignTemplatesAddOperationFactory addOperationFactory;

    @Test
    void testAdd() {

        var blockId = BlockUniqueIdConverter.convertToUniqueId(CONTEXT_ON_SITE_RTB_PREFIX, 41443L, 1L);
        var blockBefore = blockService.findAll(QueryOpts.forClass(RtbBlock.class)
                .withFilter(CoreFilterNode.in(BlockFilters.ID, List.of(blockId)))
        ).get(0);

        var dtSizeBefore = blockBefore.getDesignTemplates().size();
        assertThat(dtSizeBefore).isEqualTo(2);
        var designTemplate = new DesignTemplates()
                .withPageId(41443L)
                .withBlockId(1L)
                .withCaption("new_template")
                .withDesignSettings(Map.of("name", "new_design",
                        "limit", "2"))
                .withType(DesignTemplatesType.tga)
                .withCustomFormatDirect(false)
                .withMultistate(null);
        var designProps = new HashSet<>(CommonDesignTemplates.allModelProperties());
        designProps.remove(DesignTemplates.MULTISTATE);
        var incomingFields = new IncomingFields();
        Multimap<Model, ModelProperty<?, ?>> data
                = Multimaps.newSetMultimap(new IdentityHashMap<>(), HashSet::new);
        data.putAll(designTemplate, designProps);
        incomingFields.addIncomingFields(data);
        var container = addOperationFactory
                .prepareAddOperationContainer(incomingFields);
        var addOperation = addOperationFactory
                .createAddOperation(Applicability.FULL, List.of(designTemplate), container);

        var result = addOperation.prepareAndApply();
        var blockAfter = blockService.findAll(QueryOpts.forClass(RtbBlock.class)
                .withFilter(CoreFilterNode.in(BlockFilters.ID, List.of(blockId)))
        ).get(0);
        var dtSizeAfter = blockAfter.getDesignTemplates().size();
        assertThat(blockAfter.getDesignTemplates().size()).isGreaterThan(dtSizeBefore);
        assertThat(dtSizeAfter).isEqualTo(3);

    }

}
