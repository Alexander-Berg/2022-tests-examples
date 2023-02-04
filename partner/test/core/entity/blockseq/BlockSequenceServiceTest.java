package ru.yandex.partner.core.entity.blockseq;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.NoTransactionException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.partner.core.CoreTest;
import ru.yandex.partner.core.entity.blockseq.model.BlockSequence;
import ru.yandex.partner.core.entity.blockseq.model.ExternalContextBlockSeq;
import ru.yandex.partner.core.filter.CoreFilterNode;
import ru.yandex.partner.core.filter.dbmeta.NumberFilter;
import ru.yandex.partner.core.junit.MySqlRefresher;
import ru.yandex.partner.dbschema.partner.tables.records.ContextOnSiteBlockSeqRecord;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MySqlRefresher.class)
@CoreTest
class BlockSequenceServiceTest {
    @Autowired
    SimpleBlockSequenceService<ExternalContextBlockSeq, ContextOnSiteBlockSeqRecord> blockSequenceService;

    @Autowired
    ExternalContextBlockSeqRepository repository;

    @Autowired
    PlatformTransactionManager ptm;

    @Test
    void testSequence() {
        var pageIdsWithBlockCount = Map.of(
                41443L, 2L,
                41446L, 1L,
                1L, 1L);

        //не в транзакции - ошибка
        assertThrows(NoTransactionException.class, () ->
                blockSequenceService.getNextBlockIds(pageIdsWithBlockCount, false));

        var transactionTemplate = new TransactionTemplate(ptm);
        var result = transactionTemplate.execute(ctx ->
                blockSequenceService.getNextBlockIds(pageIdsWithBlockCount, false)
        );
        assertThat(result.get(1L)).contains(1L);
        assertThat(result.get(41446L)).contains(8L);
        assertThat(result.get(41443L)).contains(42L, 43L);


        var resultFromDd = repository.
                getAll(CoreFilterNode.in(new NumberFilter<>("id", ExternalContextBlockSeq.class,
                                repository.getPageIdField()),
                        List.of(41443L, 41446L, 1L)), null, null, false)
                .stream().collect(Collectors.toMap(BlockSequence::getId, BlockSequence::getNextBlockId));
        assertThat(resultFromDd.get(1L)).isEqualTo(2L);
        assertThat(resultFromDd.get(41446L)).isEqualTo(9L);
        assertThat(resultFromDd.get(41443L)).isEqualTo(44L);
    }


}
