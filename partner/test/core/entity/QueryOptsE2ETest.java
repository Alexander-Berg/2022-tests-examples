package ru.yandex.partner.core.entity;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.partner.core.CoreTest;
import ru.yandex.partner.core.entity.block.filter.BlockFilters;
import ru.yandex.partner.core.entity.block.model.RtbBlock;
import ru.yandex.partner.core.entity.block.service.BlockService;
import ru.yandex.partner.core.filter.CoreFilterNode;

@CoreTest
public class QueryOptsE2ETest {

    @Autowired
    BlockService blockService;

    @Test
    void batchTest() {

        int totalBlocks = (int) blockService.count(QueryOpts.forClass(RtbBlock.class)
                .withFilter(CoreFilterNode.neutral()));
        int batchSize = 5;
        int expectedRequests = totalBlocks / batchSize;
        expectedRequests += (totalBlocks % batchSize != 0) ? 1 : 0; // добавляем последний поход в базу за остаточком

        expectedRequests += 1; // когда lastId будет > любого id в базе, сервис сходит в бд и вернет пустой список.

        QueryOpts<RtbBlock> opts = (QueryOpts<RtbBlock>) QueryOpts.forClass(RtbBlock.class)
                .withProps(Set.of(RtbBlock.MULTISTATE, RtbBlock.CAPTION))
                .withBatch(batchSize, BlockFilters.ID);

        int actualRequests = 0;
        int actualRead = 0;
        List<RtbBlock> blocks = blockService.findAll(opts);
        actualRequests += 1;
        actualRead += blocks.size();

        while (opts.nextBatch(blocks)) { // сдвиг lastId
            blocks = blockService.findAll(opts);
            actualRequests += 1;
            actualRead += blocks.size();
        }

        Assertions.assertEquals(totalBlocks, actualRead);
        Assertions.assertEquals(expectedRequests, actualRequests);
    }
}
