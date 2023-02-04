package ru.yandex.partner.core.entity.block.actions;

import java.util.List;
import java.util.Set;

import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.partner.core.CoreTest;
import ru.yandex.partner.core.action.ActionPerformer;
import ru.yandex.partner.core.action.exception.ActionError;
import ru.yandex.partner.core.entity.QueryOpts;
import ru.yandex.partner.core.entity.block.actions.all.BlockActionDelete;
import ru.yandex.partner.core.entity.block.actions.rtb.external.RtbBlockDeleteFactory;
import ru.yandex.partner.core.entity.block.actions.rtb.external.RtbBlockRestoreFactory;
import ru.yandex.partner.core.entity.block.actions.rtb.external.RtbDeleteFromAdfoxFactory;
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
import ru.yandex.partner.dbschema.partner.enums.ContextOnSiteRtbSiteVersion;
import ru.yandex.partner.dbschema.partner.tables.ContextOnSiteRtb;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@CoreTest
@ExtendWith(MySqlRefresher.class)
class BlockActionDeleteRestoreTest {
    @Autowired
    private ActionPerformer actionPerformer;
    @Autowired
    private RtbBlockDeleteFactory rtbBlockDeleteFactory;
    @Autowired
    private BlockService blockService;
    @Autowired
    private PageService pageService;
    @Autowired
    private RtbBlockRestoreFactory rtbBlockRestoreFactory;
    @Autowired
    private RtbDeleteFromAdfoxFactory rtbDeleteFromAdfoxFactory;
    @Autowired
    private DSLContext dslContext;

    @Test
    void archiveRestoreTest() {
        var ids = List.of(347649081345L);
        archiveWorkingBlock(ids, rtbBlockDeleteFactory.delete(List.of(347649081345L)));
        restoreDeletedBlock(ids);
    }

    @Test
    void archiveAdfoxBlock() {
        var ids = List.of(347674247171L);
        archiveWorkingBlock(ids, rtbDeleteFromAdfoxFactory.createAction(ids, null));
    }

    void archiveWorkingBlock(List<Long> ids, BlockActionDelete<RtbBlock> deleteAction) {
        var blockBefore = blockService.findAll(QueryOpts.forClass(RtbBlock.class)
                .withFilter(CoreFilterNode.in(BlockFilters.ID, ids))
                .withProps(Set.of(BlockWithMultistate.MULTISTATE))
        ).get(0);
        assertThat(blockBefore.getMultistate().test(BlockStateFlag.WORKING)).isTrue();

        var pageBefore = pageService.findAll(QueryOpts.forClass(ContextPage.class)
                .withFilter(CoreFilterNode.eq(PageFilters.PAGE_ID, blockBefore.getPageId()))
        ).get(0);
        assertThat(pageBefore.getMultistate().test(PageStateFlag.NEED_UPDATE)).isFalse();
        var blockCountBefore = pageBefore.getBlocksCount();
        var result = actionPerformer.doActions(deleteAction);
        var blockAfter = blockService.findAll(QueryOpts.forClass(RtbBlock.class)
                .withFilter(CoreFilterNode.in(BlockFilters.ID, ids))
                .withProps(Set.of(BlockWithMultistate.MULTISTATE))
        ).get(0);
        var pageAfter = pageService.findAll(QueryOpts.forClass(ContextPage.class)
                .withFilter(CoreFilterNode.eq(PageFilters.PAGE_ID, blockAfter.getPageId()))
        ).get(0);
        var blockCountAfter = pageAfter.getBlocksCount();

        assertThat(blockCountBefore - blockCountAfter).isEqualTo(1L);
        assertThat(blockAfter.getMultistate().test(BlockStateFlag.DELETED)).isTrue();
        assertThat(blockAfter.getMultistate().test(BlockStateFlag.NEED_UPDATE)).isTrue();
        assertThat(blockAfter.getMultistate().test(BlockStateFlag.WORKING)).isFalse();
        assertThat(pageAfter.getMultistate().test(PageStateFlag.NEED_UPDATE)).isTrue();
        assertThat(result.isCommitted()).isTrue();
    }

    void restoreDeletedBlock(List<Long> ids) {
        var blockBefore = blockService.findAll(QueryOpts.forClass(RtbBlock.class)
                .withFilter(CoreFilterNode.in(BlockFilters.ID, ids))
                .withProps(Set.of(BlockWithMultistate.MULTISTATE))
        ).get(0);
        assertThat(blockBefore.getMultistate().test(BlockStateFlag.DELETED)).isTrue();

        var pageBefore = pageService.findAll(QueryOpts.forClass(ContextPage.class)
                .withFilter(CoreFilterNode.eq(PageFilters.PAGE_ID, blockBefore.getPageId()))
        ).get(0);
        var blockCountBefore = pageBefore.getBlocksCount();
        var restoreActions = rtbBlockRestoreFactory.createAction(ids);
        var result = actionPerformer.doActions(restoreActions);
        var blockAfter = blockService.findAll(QueryOpts.forClass(RtbBlock.class)
                .withFilter(CoreFilterNode.in(BlockFilters.ID, ids))
                .withProps(Set.of(BlockWithMultistate.MULTISTATE))
        ).get(0);
        var pageAfter = pageService.findAll(QueryOpts.forClass(ContextPage.class)
                .withFilter(CoreFilterNode.eq(PageFilters.PAGE_ID, blockAfter.getPageId()))
        ).get(0);
        var blockCountAfter = pageAfter.getBlocksCount();

        assertThat(blockCountBefore - blockCountAfter).isEqualTo(-1L);
        assertThat(blockAfter.getMultistate().test(BlockStateFlag.DELETED)).isFalse();
        assertThat(blockAfter.getMultistate().test(BlockStateFlag.NEED_UPDATE)).isTrue();
        assertThat(result.isCommitted()).isTrue();
    }

    @Test
    void failRestoreBockWithoutFeature() {
        var ids = List.of(347649081345L);
        archiveWorkingBlock(ids, rtbBlockDeleteFactory.delete(List.of(347649081345L)));
        dslContext.update(ContextOnSiteRtb.CONTEXT_ON_SITE_RTB)
                .set(ContextOnSiteRtb.CONTEXT_ON_SITE_RTB.SITE_VERSION, ContextOnSiteRtbSiteVersion.mobile_fullscreen)
                .where(ContextOnSiteRtb.CONTEXT_ON_SITE_RTB.UNIQUE_ID.eq(347649081345L))
                .execute();
        var restoreActions = rtbBlockRestoreFactory.createAction(ids);
        var result = actionPerformer.doActions(restoreActions);
        assertThat(result.isCommitted()).isFalse();
        assertThat(result.getErrors().get(RtbBlock.class).get(347649081345L).get(0).getDefectType())
                .isEqualTo(ActionError.ActionDefectType.CAN_NOT_DO_ACTION);
    }

}
