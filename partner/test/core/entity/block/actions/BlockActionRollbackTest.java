package ru.yandex.partner.core.entity.block.actions;

import java.util.List;
import java.util.Set;

import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.partner.core.CoreTest;
import ru.yandex.partner.core.action.ActionPerformer;
import ru.yandex.partner.core.action.ActionUserIdContext;
import ru.yandex.partner.core.entity.QueryOpts;
import ru.yandex.partner.core.entity.block.actions.rtb.external.RtbBlockDeleteFactory;
import ru.yandex.partner.core.entity.block.filter.BlockFilters;
import ru.yandex.partner.core.entity.block.model.BlockWithMultistate;
import ru.yandex.partner.core.entity.block.model.RtbBlock;
import ru.yandex.partner.core.entity.block.service.BlockService;
import ru.yandex.partner.core.entity.page.filter.PageFilters;
import ru.yandex.partner.core.entity.page.model.ContextPage;
import ru.yandex.partner.core.entity.page.service.PageService;
import ru.yandex.partner.core.filter.CoreFilterNode;
import ru.yandex.partner.core.junit.MySqlRefresher;
import ru.yandex.partner.core.multistate.block.BlockStateFlag;
import ru.yandex.partner.core.multistate.page.PageStateFlag;
import ru.yandex.partner.dbschema.partner.Tables;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@CoreTest
@ExtendWith(MySqlRefresher.class)
class BlockActionRollbackTest {

    @Autowired
    private DSLContext dsl;
    @Autowired
    private ActionPerformer actionPerformer;
    @Autowired
    private RtbBlockDeleteFactory rtbBlockDeleteFactory;
    @Autowired
    private BlockService blockService;
    @Autowired
    private PageService pageService;
    @Autowired
    private ActionUserIdContext actionUserIdContext;

    @Test
    void transactionRollbackTest() {
        var ids = List.of(347649081345L);

        var blockBefore = blockService.findAll(QueryOpts.forClass(RtbBlock.class)
                .withFilter(CoreFilterNode.in(BlockFilters.ID, ids))
                .withProps(Set.of(BlockWithMultistate.MULTISTATE))
        ).get(0);
        assertThat(blockBefore.getMultistate().test(BlockStateFlag.WORKING)).isTrue();
        int actionsCountBefore = blockActionsCount();

        var pageBefore = pageService.findAll(QueryOpts.forClass(ContextPage.class)
                .withFilter(CoreFilterNode.eq(PageFilters.PAGE_ID, blockBefore.getPageId()))
        ).get(0);
        assertThat(pageBefore.getMultistate().test(PageStateFlag.NEED_UPDATE)).isFalse();
        var blockCountBefore = pageBefore.getBlocksCount();
        var deleteActions = rtbBlockDeleteFactory.delete(ids);


        try {
            // userId - 9 отсуствует в базе поэтому будет выдавать Exception
            actionUserIdContext.setUserId(9L);

            assertThatThrownBy(() -> actionPerformer.doActions(deleteActions)).isInstanceOf(Exception.class);

            var blockAfter = blockService.findAll(QueryOpts.forClass(RtbBlock.class)
                    .withFilter(CoreFilterNode.in(BlockFilters.ID, ids))
                    .withProps(Set.of(BlockWithMultistate.MULTISTATE))
            ).get(0);
            var pageAfter = pageService.findAll(QueryOpts.forClass(ContextPage.class)
                    .withFilter(CoreFilterNode.eq(PageFilters.PAGE_ID, blockAfter.getPageId()))
            ).get(0);
            var blockCountAfter = pageAfter.getBlocksCount();
            int actionsCountAfter = blockActionsCount();

            assertThat(actionsCountAfter - actionsCountBefore).isZero();
            assertThat(blockCountBefore - blockCountAfter).isZero();
            assertThat(blockAfter.getMultistate().test(BlockStateFlag.DELETED)).isFalse();
            assertThat(blockAfter.getMultistate().test(BlockStateFlag.NEED_UPDATE)).isFalse();
            assertThat(blockAfter.getMultistate().test(BlockStateFlag.WORKING)).isTrue();
            assertThat(pageAfter.getMultistate().test(PageStateFlag.NEED_UPDATE)).isFalse();
        } finally {
            // emulate unsetting, which would usually be performed in ActionsController
            actionUserIdContext.setDefault();
        }

    }

    private int blockActionsCount() {
        return dsl.selectCount().from(Tables.CONTEXT_ON_SITE_RTB_ACTION_LOG)
                .fetchOptional(0, Integer.class).orElse(0);
    }

}
