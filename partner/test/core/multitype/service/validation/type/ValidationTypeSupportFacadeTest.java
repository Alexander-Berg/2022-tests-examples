package ru.yandex.partner.core.multitype.service.validation.type;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.jooq.DSLContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.DefectIds;
import ru.yandex.direct.validation.result.DefectInfo;
import ru.yandex.direct.validation.result.PathNode;
import ru.yandex.direct.validation.result.ValidationResult;
import ru.yandex.partner.core.CoreTest;
import ru.yandex.partner.core.action.ActionPerformer;
import ru.yandex.partner.core.entity.QueryOpts;
import ru.yandex.partner.core.entity.block.actions.rtb.external.RtbBlockDeleteFactory;
import ru.yandex.partner.core.entity.block.actions.rtb.external.RtbBlockEditFactory;
import ru.yandex.partner.core.entity.block.container.BlockContainerImpl;
import ru.yandex.partner.core.entity.block.filter.BlockFilters;
import ru.yandex.partner.core.entity.block.model.RtbBlock;
import ru.yandex.partner.core.entity.block.service.BlockService;
import ru.yandex.partner.core.entity.block.service.OperationMode;
import ru.yandex.partner.core.entity.block.service.validation.defects.BlockDefectIds;
import ru.yandex.partner.core.entity.block.service.validation.type.BlockValidationTypeSupportFacade;
import ru.yandex.partner.core.entity.block.type.commonshowvideoandstrategy.BlockWithCommonShowVideoAndStrategyValidatorProvider;
import ru.yandex.partner.core.filter.CoreFilterNode;
import ru.yandex.partner.core.junit.MySqlRefresher;
import ru.yandex.partner.dbschema.partner.tables.ContextOnSiteRtb;

@CoreTest
@ExtendWith(MySqlRefresher.class)
class ValidationTypeSupportFacadeTest {

    @Autowired
    private BlockValidationTypeSupportFacade blockValidationTypeSupportFacade;

    @Autowired
    private BlockService blockService;

    @Autowired
    private ActionPerformer actionPerformer;

    @Autowired
    private RtbBlockDeleteFactory rtbBlockDeleteFactory;

    @Autowired
    private RtbBlockEditFactory rtbBlockEditFactory;

    @Autowired
    private DSLContext dslContext;

    @Autowired
    private BlockWithCommonShowVideoAndStrategyValidatorProvider blockWithCommonShowVideoAndStrategyValidatorProvider;

    @Test
    void validate() {
        var id = 1L;
        var oltRtb = new RtbBlock()
                .withId(id)
                .withShowVideo(false);

        var vr = ValidationResult.<List<RtbBlock>, Defect>success(List.of(
                oltRtb
        ));
        blockValidationTypeSupportFacade.validate(BlockContainerImpl.create(OperationMode.EDIT),
                vr,
                Map.of(0, ModelChanges.build(id, RtbBlock.class, RtbBlock.SHOW_VIDEO, true).applyTo(oltRtb))
        );


        // При изменении show_video должны провалидировать
        //    Strategy
        //    Design_templates
        //    Show_video
        //  Остальные поля не должны валидироваться
        Assertions.assertEquals(vr.getSubResults().size(), 1);

        var errors = vr.flattenErrors()
                .stream()
                .map(DefectInfo::getDefect)
                .map(Defect::defectId)
                .collect(Collectors.toSet());

        Assertions.assertEquals(
                Set.of(DefectIds.CANNOT_BE_NULL, BlockDefectIds.DesignTemplates.EMPTY_DESIGN_TEMPLATES),
                errors //BlockDefectIds.Strategy.EMPTY_STRATEGY

        );

        // проверяем что show_video было провалидировано
        var hasShowVideo = vr.getSubResults().values()
                .stream()
                .map(ValidationResult::getSubResults)
                .map(Map::keySet)
                .flatMap(Collection::stream)
                .anyMatch(pathNode -> pathNode.equals(new PathNode.Field(RtbBlock.SHOW_VIDEO.name())));

        Assertions.assertTrue(hasShowVideo);
    }

    @Test
    void deleteInvalidBlock() {
        var block = blockService.findAll(QueryOpts.forClass(RtbBlock.class)
                .withFilter(CoreFilterNode.in(BlockFilters.ID, List.of(347649081345L)))
        ).get(0);

        var invalidBlock = invalidateStrategyType(block);

        var result = actionPerformer.doActions(rtbBlockDeleteFactory.delete(List.of(invalidBlock.getId())));

        // успешно удалился не смотря на то что не валиден
        Assertions.assertTrue(result.isCommitted());
    }

    @Test
    void editInvalidBlock() {
        var block = blockService.findAll(QueryOpts.forClass(RtbBlock.class)
                .withFilter(CoreFilterNode.in(BlockFilters.ID, List.of(347649081345L)))
        ).get(0);

        var invalidBlock = invalidateStrategyType(block);


        var modelChangesList = List.of(
               new ModelChanges<>(invalidBlock.getId(), RtbBlock.class)
                       .process(block.getStrategyType(), RtbBlock.STRATEGY_TYPE)
        );
        var result = actionPerformer.doActions(rtbBlockEditFactory.edit(modelChangesList));

        Assertions.assertTrue(result.isCommitted());

    }

    private RtbBlock invalidateStrategyType(RtbBlock block) {

        var invalidStrategyType = 928L;
        // Проверить на design_templates не удалось, т.к. design_templates зависит от site_version
        // А site_version зависит от multistates, поэтому при смене статуса валидируется и design_templates
        dslContext.update(ContextOnSiteRtb.CONTEXT_ON_SITE_RTB)
                .set(ContextOnSiteRtb.CONTEXT_ON_SITE_RTB.STRATEGY, invalidStrategyType)
                .where(ContextOnSiteRtb.CONTEXT_ON_SITE_RTB.UNIQUE_ID.eq(block.getId()))
                .execute();

        var invalidBlock = blockService.findAll(QueryOpts.forClass(RtbBlock.class)
                .withFilter(CoreFilterNode.in(BlockFilters.ID, List.of(block.getId())))
        ).get(0);

        // проверяем что сломалось
        Assertions.assertEquals(invalidStrategyType, invalidBlock.getStrategyType());
        var validationResult = blockWithCommonShowVideoAndStrategyValidatorProvider.validator()
                .apply(invalidBlock);

        Assertions.assertFalse(validationResult.flattenErrors().isEmpty());

        return invalidBlock;
    }
}
