package ru.yandex.partner.core.entity.block.actions;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.partner.core.CoreTest;
import ru.yandex.partner.core.action.ActionPerformer;
import ru.yandex.partner.core.entity.QueryOpts;
import ru.yandex.partner.core.entity.block.actions.rtb.external.RtbBlockStopFactory;
import ru.yandex.partner.core.entity.block.filter.BlockFilters;
import ru.yandex.partner.core.entity.block.model.BlockWithMultistate;
import ru.yandex.partner.core.entity.block.model.RtbBlock;
import ru.yandex.partner.core.entity.block.service.BlockService;
import ru.yandex.partner.core.filter.CoreFilterNode;
import ru.yandex.partner.core.junit.MySqlRefresher;
import ru.yandex.partner.core.multistate.block.BlockStateFlag;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@CoreTest
@ExtendWith(MySqlRefresher.class)
class BlockStopActionTest {
    @Autowired
    private ActionPerformer actionPerformer;
    @Autowired
    private RtbBlockStopFactory rtbBlockStopFactory;
    @Autowired
    private BlockService blockService;

    @Test
    void stopWorkingBlock() {
        var ids = List.of(347649081345L);

        var blockBefore = blockService.findAll(QueryOpts.forClass(RtbBlock.class)
                .withFilter(CoreFilterNode.in(BlockFilters.ID, ids))
                .withProps(Set.of(BlockWithMultistate.MULTISTATE))
        ).get(0);
        assertThat(blockBefore.getMultistate().test(BlockStateFlag.WORKING)).isTrue();

        var stopAction = rtbBlockStopFactory.createAction(ids);
        var result = actionPerformer.doActions(stopAction);
        var blockAfter = blockService.findAll(QueryOpts.forClass(RtbBlock.class)
                .withFilter(CoreFilterNode.in(BlockFilters.ID, ids))
                .withProps(Set.of(BlockWithMultistate.MULTISTATE))
        ).get(0);
        assertThat(blockAfter.getMultistate().test(BlockStateFlag.WORKING)).isFalse();
        assertThat(blockAfter.getMultistate().test(BlockStateFlag.DELETED)).isFalse();
        assertThat(result.isCommitted()).isTrue();
    }

    @Test
    void stopDeletedBlock() {
        var ids = List.of(347674247170L);

        var blockBefore = blockService.findAll(QueryOpts.forClass(RtbBlock.class)
                .withFilter(CoreFilterNode.in(BlockFilters.ID, ids))
                .withProps(Set.of(BlockWithMultistate.MULTISTATE))
        ).get(0);
        assertThat(blockBefore.getMultistate().test(BlockStateFlag.DELETED)).isTrue();

        var stopAction = rtbBlockStopFactory.createAction(ids);
        var result = actionPerformer.doActions(stopAction);
        var blockAfter = blockService.findAll(QueryOpts.forClass(RtbBlock.class)
                .withFilter(CoreFilterNode.in(BlockFilters.ID, ids))
                .withProps(Set.of(BlockWithMultistate.MULTISTATE))
        ).get(0);
        assertThat(blockBefore.getMultistate().getEnabledFlags())
                .isEqualTo(blockAfter.getMultistate().getEnabledFlags());
        assertThat(result.isCommitted()).isFalse();
        assertThat(result.getErrors().isEmpty()).isFalse();
    }

}
