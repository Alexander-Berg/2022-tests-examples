package ru.yandex.partner.core.entity.block.service

import org.assertj.core.api.Assertions
import org.jooq.DSLContext
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.partner.core.CoreTest
import ru.yandex.partner.core.entity.QueryOpts
import ru.yandex.partner.core.entity.block.filter.BlockFilters
import ru.yandex.partner.core.entity.block.model.RtbBlock
import ru.yandex.partner.core.filter.CoreFilterNode
import ru.yandex.partner.core.junit.MySqlRefresher
import ru.yandex.partner.dbschema.partner.enums.ContextOnSiteRtbModel
import ru.yandex.partner.dbschema.partner.tables.ContextOnSiteRtb

@CoreTest
@ExtendWith(MySqlRefresher::class)
class BlockServiceTest(
    @Autowired
    val blockService: BlockService,
    @Autowired
    val dslContext: DSLContext
) {

    @Test
    fun findBlock() {
        val queryOpts = QueryOpts.forClass(RtbBlock::class.java)
            .withFilter(CoreFilterNode.eq(BlockFilters.ID, 347649081345L))
        Assertions.assertThat(blockService.findAll(queryOpts).singleOrNull()).isNotNull
        updateToInternalRtbBlock()
        Assertions.assertThat(blockService.findAll(queryOpts).singleOrNull()).isNull()

    }

    @Test
    fun countBlock() {
        val queryOpts = QueryOpts.forClass(RtbBlock::class.java)
        val before = blockService.count(queryOpts)
        updateToInternalRtbBlock()
        Assertions.assertThat(before - blockService.count(queryOpts)).isEqualTo(1)
    }

    private fun updateToInternalRtbBlock() {
        dslContext.update(ContextOnSiteRtb.CONTEXT_ON_SITE_RTB)
            .set(ContextOnSiteRtb.CONTEXT_ON_SITE_RTB.MODEL, ContextOnSiteRtbModel.internal_context_on_site_rtb)
            .where(ContextOnSiteRtb.CONTEXT_ON_SITE_RTB.UNIQUE_ID.`in`(347649081345L))
            .execute();
    }
}
