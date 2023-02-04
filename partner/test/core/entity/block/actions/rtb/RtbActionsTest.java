package ru.yandex.partner.core.entity.block.actions.rtb;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.model.ModelChanges;
import ru.yandex.partner.core.CoreTest;
import ru.yandex.partner.core.action.ActionPerformer;
import ru.yandex.partner.core.action.result.ActionsResult;
import ru.yandex.partner.core.entity.QueryOpts;
import ru.yandex.partner.core.entity.block.actions.BlockActionType;
import ru.yandex.partner.core.entity.block.actions.BlockActionsEnum;
import ru.yandex.partner.core.entity.block.actions.all.factories.BlockActionRestoreWithPageFactory;
import ru.yandex.partner.core.entity.block.actions.all.factories.BlockMultistateActionFactory;
import ru.yandex.partner.core.entity.block.actions.rtb.external.RtbBlockAddFactory;
import ru.yandex.partner.core.entity.block.actions.rtb.external.RtbBlockSetCheckStatisticsFactory;
import ru.yandex.partner.core.entity.block.actions.rtb.external.RtbBlockSetNeedUpdateFactory;
import ru.yandex.partner.core.entity.block.actions.rtb.external.RtbBlockStopFactory;
import ru.yandex.partner.core.entity.block.filter.BlockFilters;
import ru.yandex.partner.core.entity.block.model.BlockWithMultistate;
import ru.yandex.partner.core.entity.block.model.RtbBlock;
import ru.yandex.partner.core.entity.block.service.BlockService;
import ru.yandex.partner.core.entity.page.container.PageOperationContainer;
import ru.yandex.partner.core.entity.page.model.ContextPage;
import ru.yandex.partner.core.entity.page.model.PageWithMultistate;
import ru.yandex.partner.core.entity.page.repository.PageModifyRepository;
import ru.yandex.partner.core.entity.page.service.PageService;
import ru.yandex.partner.core.junit.MySqlRefresher;
import ru.yandex.partner.core.multistate.block.BlockStateFlag;
import ru.yandex.partner.core.multistate.page.PageMultistate;
import ru.yandex.partner.core.multistate.page.PageStateFlag;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.partner.core.filter.CoreFilterNode.and;
import static ru.yandex.partner.core.filter.CoreFilterNode.eq;
import static ru.yandex.partner.libs.multistate.MultistatePredicates.has;
import static ru.yandex.partner.libs.multistate.MultistatePredicates.hasNoneOf;

@ExtendWith(MySqlRefresher.class)
@CoreTest
class RtbActionsTest {
    @Autowired
    @BlockActionType(BlockActionsEnum.RESET_CHECK_STATISTICS)
    BlockMultistateActionFactory<RtbBlock> resetCheckStatisticsFactory;

    @Autowired
    @BlockActionType(BlockActionsEnum.DELETE_WITH_PAGE)
    BlockMultistateActionFactory<RtbBlock> deleteWithPage;

    @Autowired
    BlockActionRestoreWithPageFactory<RtbBlock> restoreWithPage;

    @Autowired
    @BlockActionType(BlockActionsEnum.START_UPDATE)
    BlockMultistateActionFactory<RtbBlock> startUpdate;

    @Autowired
    @BlockActionType(BlockActionsEnum.STOP_UPDATE)
    BlockMultistateActionFactory<RtbBlock> stopUpdate;

    @Autowired
    RtbBlockSetNeedUpdateFactory setNeedUpdateFactory;

    @Autowired
    RtbBlockStopFactory rtbBlockStopFactory;

    @Autowired
    RtbBlockAddFactory rtbBlockRtbBlockAddFactory;

    @Autowired
    RtbBlockSetCheckStatisticsFactory setCheckStatisticsFactory;

    @Autowired
    BlockService blockService;

    @Autowired
    PageService pageService;

    @Autowired
    PageModifyRepository pageModifyRepository;

    @Autowired
    ActionPerformer actionPerformer;

    @Test
    void setAndResetCheckStatistics() {
        var block = blockUnderTest();
        var blockId = block.getId();
        assertThat(block.getMultistate()).matches(hasNoneOf(BlockStateFlag.CHECK_STATISTICS));

        ActionsResult<?> setCheckResult = actionPerformer.doActions(
                setCheckStatisticsFactory.createAction(List.of(blockId))
        );

        assertThat(setCheckResult.getErrors()).isEmpty();
        assertThat(setCheckResult.isCommitted()).isTrue();

        assertThat(blockUnderTest().getMultistate()).matches(has(BlockStateFlag.CHECK_STATISTICS));

        ActionsResult<?> resetCheckResult = actionPerformer.doActions(
                resetCheckStatisticsFactory.createAction(List.of(blockId))
        );

        assertThat(resetCheckResult.getErrors()).isEmpty();
        assertThat(resetCheckResult.isCommitted()).isTrue();

        assertThat(blockUnderTest().getMultistate()).matches(hasNoneOf(BlockStateFlag.CHECK_STATISTICS));
    }

    @Test
    void deleteAndRestoreWithPage() {
        var block = blockUnderTest();
        var blockId = block.getId();
        assertThat(block.getMultistate())
                .matches(hasNoneOf(BlockStateFlag.DELETED, BlockStateFlag.DELETED_WITH_PAGE));

        // переводим пейдж в тест, чтобы на нём было разрешение запускать блоки
        var page = pageService.findAll(QueryOpts.forClass(ContextPage.class)
                .withFilterByIds(List.of(block.getPageId()))
                .withProps(Set.of(PageWithMultistate.MULTISTATE))
        ).iterator().next();

        PageMultistate fixupMultistate = page.getMultistate().copy();
        fixupMultistate.setFlag(PageStateFlag.TESTING, true);
        pageModifyRepository.update(PageOperationContainer.create(), Set.of(ModelChanges.build(
                page,
                PageWithMultistate.MULTISTATE,
                fixupMultistate
        ).applyTo(page)));


        ActionsResult<?> deleteResult = actionPerformer.doActions(
                // заодно останавливаем, чтобы проверить старт при восстановлении
                rtbBlockStopFactory.createAction(List.of(blockId)),
                deleteWithPage.createAction(List.of(blockId))
        );

        assertThat(deleteResult.getErrors()).isEmpty();
        assertThat(deleteResult.isCommitted()).isTrue();

        assertThat(blockUnderTest().getMultistate()).matches(
                has(BlockStateFlag.DELETED).and(has(BlockStateFlag.DELETED_WITH_PAGE))
        );

        ActionsResult<?> restoreResult = actionPerformer.doActions(
                restoreWithPage.restoreWithPage(List.of(blockId))
        );

        assertThat(restoreResult.getErrors()).isEmpty();
        assertThat(restoreResult.isCommitted()).isTrue();

        assertThat(blockUnderTest().getMultistate()).matches(
                hasNoneOf(BlockStateFlag.DELETED, BlockStateFlag.DELETED_WITH_PAGE)
                        .and(has(BlockStateFlag.WORKING))
        );
    }

    @Test
    void startAndStopUpdate() {
        var blockId = blockUnderTest().getId();

        // prepare with set need update
        var setNeedUpdateResult = actionPerformer.doActions(
                setNeedUpdateFactory.createAction(List.of(blockId))
        );

        assertThat(setNeedUpdateResult.getErrors()).isEmpty();
        assertThat(setNeedUpdateResult.isCommitted()).isTrue();

        assertThat(blockUnderTest().getMultistate()).matches(
                hasNoneOf(BlockStateFlag.UPDATING).and(has(BlockStateFlag.NEED_UPDATE))
        );

        ActionsResult<?> startUpdateResult = actionPerformer.doActions(
                startUpdate.createAction(List.of(blockId))
        );

        assertThat(startUpdateResult.getErrors()).isEmpty();
        assertThat(startUpdateResult.isCommitted()).isTrue();

        assertThat(blockUnderTest().getMultistate()).matches(has(BlockStateFlag.UPDATING));

        ActionsResult<?> stopUpdateResult = actionPerformer.doActions(
                stopUpdate.createAction(List.of(blockId))
        );

        assertThat(stopUpdateResult.getErrors()).isEmpty();
        assertThat(stopUpdateResult.isCommitted()).isTrue();

        assertThat(blockUnderTest().getMultistate()).matches(hasNoneOf(BlockStateFlag.UPDATING));
    }

    private BlockWithMultistate blockUnderTest() {
        return blockService.findAll(QueryOpts.forClass(RtbBlock.class)
                .withFilter(and(eq(BlockFilters.BLOCK_ID, 1L), eq(BlockFilters.PAGE_ID, 41443L)))
                .withProps(Set.of(BlockWithMultistate.MULTISTATE))
        ).iterator().next();
    }
}
