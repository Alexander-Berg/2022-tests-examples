package ru.yandex.partner.core.entity.block.actions.all;

import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.node.MissingNode;
import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.model.ModelProperty;
import ru.yandex.partner.core.CoreTest;
import ru.yandex.partner.core.action.ActionPerformer;
import ru.yandex.partner.core.action.exception.DefectInfoWithMsgParams;
import ru.yandex.partner.core.action.exception.presentation.ActionDefectMsg;
import ru.yandex.partner.core.entity.QueryOpts;
import ru.yandex.partner.core.entity.block.actions.rtb.external.RtbBlockDuplicateFactory;
import ru.yandex.partner.core.entity.block.filter.BlockFilters;
import ru.yandex.partner.core.entity.block.model.RtbBlock;
import ru.yandex.partner.core.entity.block.service.BlockService;
import ru.yandex.partner.core.entity.page.container.PageOperationContainer;
import ru.yandex.partner.core.entity.page.model.ContextPage;
import ru.yandex.partner.core.entity.page.repository.PageModifyRepository;
import ru.yandex.partner.core.entity.page.repository.PageTypedRepository;
import ru.yandex.partner.core.filter.CoreFilterNode;
import ru.yandex.partner.core.junit.MySqlRefresher;
import ru.yandex.partner.core.multistate.page.PageStateFlag;
import ru.yandex.partner.dbschema.partner.Tables;
import ru.yandex.partner.libs.i18n.MsgWithArgs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@CoreTest
@ExtendWith(MySqlRefresher.class)
class BlockActionDuplicateTest {

    @Autowired
    ActionPerformer actionPerformer;
    @Autowired
    RtbBlockDuplicateFactory rtbBlockDuplicateFactory;

    @Autowired
    DSLContext dslContext;

    @Autowired
    BlockService blockService;

    @Autowired
    private PageTypedRepository pageRepository;

    @Autowired
    private PageModifyRepository pageModifyRepository;

    @Test
    void normalDuplicate() {
        Long blockId = 347649081345L;
        BlockActionDuplicate<RtbBlock, ?> action = rtbBlockDuplicateFactory.createAction(
                List.of(blockId), MissingNode.getInstance()
        );
        actionPerformer.doActions(action);
        assertNotEquals("{}", getInsertedOpts(),
                "Log opts must be non empty json on add! (sub action of duplicate)"
        );
    }

    @Test
    void failedDuplicate() {
        Long blockId = 347649081345L;
        var ids = List.of(blockId);
        Set<ModelProperty<?, ?>> fields = Set.of(RtbBlock.MULTISTATE,
                RtbBlock.MEDIA_ACTIVE,
                RtbBlock.MEDIA_BLOCKED,
                RtbBlock.MEDIA_CPM,
                RtbBlock.MINCPM,
                RtbBlock.STRATEGY_TYPE,
                RtbBlock.TEXT_ACTIVE,
                RtbBlock.TEXT_BLOCKED,
                RtbBlock.TEXT_CPM,
                RtbBlock.VIDEO_ACTIVE,
                RtbBlock.VIDEO_BLOCKED,
                RtbBlock.VIDEO_CPM,
                RtbBlock.DESIGN_TEMPLATES);
        var blockBefore = blockService.findAll(QueryOpts.forClass(RtbBlock.class)
                .withFilter(CoreFilterNode.in(BlockFilters.ID, ids))
                .withProps(fields)
        ).get(0);

        rejectPage(blockBefore.getPageId());

        BlockActionDuplicate<RtbBlock, ?> action = rtbBlockDuplicateFactory.createAction(
                List.of(blockId), MissingNode.getInstance()
        );
        var result = actionPerformer.doActions(action);
        assertThat(result.getErrors())
                .containsKey(RtbBlock.class);
        assertThat(result.getErrors()
                .get(RtbBlock.class)
                .get(blockBefore.getId())
                .get(0)
                .getDefectInfo()
                .getDefect()
                .params()
        ).isEqualTo(new DefectInfoWithMsgParams(MsgWithArgs.of(ActionDefectMsg.CAN_NOT_DO_ACTION, "duplicate")));
    }

    private String getInsertedOpts() {
        return dslContext.selectFrom(Tables.CONTEXT_ON_SITE_RTB_ACTION_LOG)
                .where(Tables.CONTEXT_ON_SITE_RTB_ACTION_LOG.ACTION.eq("add"))
                .orderBy(Tables.CONTEXT_ON_SITE_RTB_ACTION_LOG.ID.desc())
                .limit(1)
                .fetchOne()
                .getOpts();
    }

    private void rejectPage(long pageId) {
        var curPage = pageRepository.getStrictlyFullyFilled(
                List.of(pageId), ContextPage.class, true).get(0);

        var withRejectedState = curPage.getMultistate().copy();
        withRejectedState.setFlag(PageStateFlag.REJECTED, true);

        pageModifyRepository.update(PageOperationContainer.create(), List.of(
                new ModelChanges<>(pageId, ContextPage.class)
                        .process(withRejectedState, ContextPage.MULTISTATE)
                        .applyTo(curPage)
        ));
    }
}
