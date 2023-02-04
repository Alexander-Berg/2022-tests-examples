package ru.yandex.partner.core.entity.block.actions.rtb;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.partner.core.CoreTest;
import ru.yandex.partner.core.action.ActionPerformer;
import ru.yandex.partner.core.action.result.ActionsResult;
import ru.yandex.partner.core.entity.QueryOpts;
import ru.yandex.partner.core.entity.block.actions.BlockActionType;
import ru.yandex.partner.core.entity.block.actions.BlockActionsEnum;
import ru.yandex.partner.core.entity.block.actions.all.factories.BlockActionRestoreWithPageFactory;
import ru.yandex.partner.core.entity.block.actions.all.factories.BlockMultistateActionFactory;
import ru.yandex.partner.core.entity.block.actions.rtb.mobile.internal.InternalMobileRtbBlockDeleteFactory;
import ru.yandex.partner.core.entity.block.actions.rtb.mobile.internal.InternalMobileRtbBlockEditFactory;
import ru.yandex.partner.core.entity.block.actions.rtb.mobile.internal.InternalMobileRtbBlockSetCheckStatisticsFactory;
import ru.yandex.partner.core.entity.block.actions.rtb.mobile.internal.InternalMobileRtbBlockSetNeedUpdateFactory;
import ru.yandex.partner.core.entity.block.actions.rtb.mobile.internal.InternalMobileRtbBlockStopFactory;
import ru.yandex.partner.core.entity.block.filter.BlockFilters;
import ru.yandex.partner.core.entity.block.model.BlockWithMultistate;
import ru.yandex.partner.core.entity.block.model.InternalMobileRtbBlock;
import ru.yandex.partner.core.entity.block.service.BlockService;
import ru.yandex.partner.core.entity.page.repository.PageModifyRepository;
import ru.yandex.partner.core.entity.page.service.PageService;
import ru.yandex.partner.core.junit.MySqlRefresher;
import ru.yandex.partner.core.multistate.block.BlockStateFlag;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.partner.core.filter.CoreFilterNode.and;
import static ru.yandex.partner.core.filter.CoreFilterNode.eq;
import static ru.yandex.partner.libs.multistate.MultistatePredicates.has;
import static ru.yandex.partner.libs.multistate.MultistatePredicates.hasNoneOf;

@ExtendWith(MySqlRefresher.class)
@CoreTest
public class InternalMobileRtbActionsTest {
    @Autowired
    BlockService blockService;
    @Autowired
    PageService pageService;
    @Autowired
    PageModifyRepository pageModifyRepository;
    @Autowired
    ActionPerformer actionPerformer;

    @BlockActionType(BlockActionsEnum.DELETE_WITH_PAGE)
    @Autowired
    BlockMultistateActionFactory<InternalMobileRtbBlock> mobileRtbDeleteWithPageActionFactory;

    @BlockActionType(BlockActionsEnum.RESTORE_WITH_PAGE)
    @Autowired
    BlockActionRestoreWithPageFactory<InternalMobileRtbBlock> restoreWithPage;

    @Autowired
    InternalMobileRtbBlockStopFactory blockStopFactory;

    @Autowired
    InternalMobileRtbBlockSetCheckStatisticsFactory mobileRtbSetCheckStatisticsActionFactory;

    @BlockActionType(BlockActionsEnum.RESET_CHECK_STATISTICS)
    @Autowired
    BlockMultistateActionFactory<InternalMobileRtbBlock> mobileRtbResetCheckStatisticsActionFactory;

    @Autowired
    InternalMobileRtbBlockSetNeedUpdateFactory setNeedUpdateFactory;

    @BlockActionType(BlockActionsEnum.START_UPDATE)
    @Autowired
    BlockMultistateActionFactory<InternalMobileRtbBlock> mobileRtbStartUpdateActionFactory;

    @BlockActionType(BlockActionsEnum.STOP_UPDATE)
    @Autowired
    BlockMultistateActionFactory<InternalMobileRtbBlock> mobileRtbStopUpdateActionFactory;

    @Autowired
    InternalMobileRtbBlockEditFactory mobileRtbBlockEditFactory;

    @Autowired
    InternalMobileRtbBlockDeleteFactory mobileRtbBlockDeleteFactory;


    @Test
    void deleteAndRestoreWithPage() {
        var block = blockUnderTest();
        var blockId = block.getId();
        assertThat(block.getMultistate())
                .matches(hasNoneOf(BlockStateFlag.DELETED, BlockStateFlag.DELETED_WITH_PAGE));


        ActionsResult<?> deleteResult = actionPerformer.doActions(
                // заодно останавливаем, чтобы проверить старт при восстановлении
                blockStopFactory.createAction(List.of(blockId)),
                mobileRtbDeleteWithPageActionFactory.createAction(List.of(blockId))
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
    void setAndResetCheckStatistics() {
        var block = blockUnderTest();
        var blockId = block.getId();
        assertThat(block.getMultistate()).matches(hasNoneOf(BlockStateFlag.CHECK_STATISTICS));

        ActionsResult<?> setCheckResult = actionPerformer.doActions(
                mobileRtbSetCheckStatisticsActionFactory.createAction(List.of(blockId))
        );

        assertThat(setCheckResult.getErrors()).isEmpty();
        assertThat(setCheckResult.isCommitted()).isTrue();

        assertThat(blockUnderTest().getMultistate()).matches(has(BlockStateFlag.CHECK_STATISTICS));

        ActionsResult<?> resetCheckResult = actionPerformer.doActions(
                mobileRtbResetCheckStatisticsActionFactory.createAction(List.of(blockId))
        );

        assertThat(resetCheckResult.getErrors()).isEmpty();
        assertThat(resetCheckResult.isCommitted()).isTrue();

        assertThat(blockUnderTest().getMultistate()).matches(hasNoneOf(BlockStateFlag.CHECK_STATISTICS));
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
                mobileRtbStartUpdateActionFactory.createAction(List.of(blockId))
        );

        assertThat(startUpdateResult.getErrors()).isEmpty();
        assertThat(startUpdateResult.isCommitted()).isTrue();

        assertThat(blockUnderTest().getMultistate()).matches(has(BlockStateFlag.UPDATING));

        ActionsResult<?> stopUpdateResult = actionPerformer.doActions(
                mobileRtbStopUpdateActionFactory.createAction(List.of(blockId))
        );

        assertThat(stopUpdateResult.getErrors()).isEmpty();
        assertThat(stopUpdateResult.isCommitted()).isTrue();

        assertThat(blockUnderTest().getMultistate()).matches(hasNoneOf(BlockStateFlag.UPDATING));
    }

    private BlockWithMultistate blockUnderTest() {
        return blockService.findAll(QueryOpts.forClass(InternalMobileRtbBlock.class)
                .withFilter(and(eq(BlockFilters.BLOCK_ID, 2L),
                        eq(BlockFilters.PAGE_ID, 132439L)))
                .withProps(Set.of(BlockWithMultistate.MULTISTATE))
        ).iterator().next();
    }
}
