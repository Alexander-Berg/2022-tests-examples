package ru.yandex.partner.core.entity.block.type

import NPartner.Page.TPartnerPage
import org.assertj.core.api.Assertions
import org.jooq.DSLContext
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.partner.core.CoreTest
import ru.yandex.partner.core.bs.BkDataRepository
import ru.yandex.partner.core.entity.QueryOpts
import ru.yandex.partner.core.entity.block.filter.BlockFilters
import ru.yandex.partner.core.entity.block.model.BaseBlock
import ru.yandex.partner.core.entity.block.model.RtbBlock
import ru.yandex.partner.core.entity.block.service.BlockService
import ru.yandex.partner.core.entity.dsp.DspConstants
import ru.yandex.partner.core.filter.CoreFilterNode
import ru.yandex.partner.core.junit.MySqlRefresher
import ru.yandex.partner.dbschema.partner.tables.ContextOnSiteRtb
import java.math.BigDecimal

@CoreTest
@ExtendWith(MySqlRefresher::class)
class RtbBlockFillerTest(
    @Autowired val blockBkDataRepository: BkDataRepository<BaseBlock, TPartnerPage.TBlock>,
    @Autowired val blockService: BlockService,
    @Autowired val dslContext: DSLContext

) {

    @Test
    fun testRtbFields() {
        val id = 347649081345L

        val bkData = blockBkDataRepository.getBkData(listOf(id)).first()

        val block = blockService.findAll(
            QueryOpts.forClass(RtbBlock::class.java)
                .withFilter(CoreFilterNode.eq(BlockFilters.ID, id)).withProps(
                    RtbBlock.ADFOX_BLOCK,
                    RtbBlock.MULTISTATE,
                    RtbBlock.BLIND,
                    RtbBlock.STRATEGY_TYPE,
                    RtbBlock.ALTERNATIVE_CODE,
                    RtbBlock.ALT_HEIGHT,
                    RtbBlock.ALT_WIDTH,
                    RtbBlock.CAPTION,
                    RtbBlock.SHOW_VIDEO
                )
        ).first()

        Assertions.assertThat(bkData.hasPageImpOptions()).isTrue
        Assertions.assertThat(bkData.pageImpOptions.disableList)
            .containsAll(setOf("rtbshadow"))
        Assertions.assertThat(bkData.dspType).isEqualTo(1)
        Assertions.assertThat(bkData.blindLevel).isEqualTo(block.blind.toInt())
        Assertions.assertThat(bkData.alternativeCode).isEqualTo(block.alternativeCode)
        Assertions.assertThat(bkData.altHeight).isEqualTo(block.altHeight?.toInt() ?: 0)
        Assertions.assertThat(bkData.altWidth).isEqualTo(block.altWidth?.toInt() ?: 0)
        Assertions.assertThat(bkData.optimizeType).isEqualTo(block.strategyType.toInt())
        Assertions.assertThat(bkData.multiState).isEqualTo(block.multistate.toMultistateValue().toInt())
        Assertions.assertThat(bkData.adBlockBlock).isEqualTo(block.adfoxBlock)
        Assertions.assertThat(bkData.directLimit).isEqualTo(2)
        Assertions.assertThat(bkData.width).isEqualTo(970)
        Assertions.assertThat(bkData.height).isEqualTo(90)
        Assertions.assertThat(bkData.sizesList[0]).satisfies { size ->
            Assertions.assertThat(size.width).isEqualTo(970)
            Assertions.assertThat(size.height).isEqualTo(90)
        }
        Assertions.assertThat(bkData.sizesList[1]).satisfies { size ->
            Assertions.assertThat(size.width).isEqualTo(0)
            Assertions.assertThat(size.height).isEqualTo(0)
        }
        Assertions.assertThat(bkData.adTypeSetList.filter { it.value }.map { it.adType }).satisfies {
            Assertions.assertThat(it).containsAll(
                setOf(
                    TPartnerPage.TBlock.EAdType.TEXT,
                    TPartnerPage.TBlock.EAdType.MEDIA,
                    TPartnerPage.TBlock.EAdType.MEDIA_PERFORMANCE
                )
            )
            Assertions.assertThat(it).doesNotContain(
                TPartnerPage.TBlock.EAdType.VIDEO,
            )
            Assertions.assertThat(it).doesNotContain(
                TPartnerPage.TBlock.EAdType.VIDEO_PERFORMANCE,
            )
            Assertions.assertThat(it).doesNotContain(
                TPartnerPage.TBlock.EAdType.VIDEO_MOTION,
            )
        }

    }

    @Test
    fun testCpmBk() {
        val id = 347649081345L

        dslContext.update(ContextOnSiteRtb.CONTEXT_ON_SITE_RTB)
            .set(ContextOnSiteRtb.CONTEXT_ON_SITE_RTB.MEDIA_BLOCKED, 1)
            .set(ContextOnSiteRtb.CONTEXT_ON_SITE_RTB.VIDEO_ACTIVE, 1)
            .set(ContextOnSiteRtb.CONTEXT_ON_SITE_RTB.VIDEO_CPM, BigDecimal.valueOf(5.5))
            .set(ContextOnSiteRtb.CONTEXT_ON_SITE_RTB.SHOW_VIDEO, 1)
            .where(ContextOnSiteRtb.CONTEXT_ON_SITE_RTB.UNIQUE_ID.eq(id))
            .execute()

        val bkData = blockBkDataRepository.getBkData(listOf(id)).first()

        Assertions.assertThat(bkData.cpm).isEqualTo(0)

        Assertions.assertThat(bkData.adTypeSetList.filter { it.value }.map { it.adType }).satisfies {
            Assertions.assertThat(it).containsAll(
                setOf(
                    TPartnerPage.TBlock.EAdType.TEXT,
                    TPartnerPage.TBlock.EAdType.VIDEO,
                    TPartnerPage.TBlock.EAdType.VIDEO_PERFORMANCE,
                    TPartnerPage.TBlock.EAdType.VIDEO_MOTION
                )
            )
            Assertions.assertThat(it).doesNotContain(
                TPartnerPage.TBlock.EAdType.MEDIA,
            )
            Assertions.assertThat(it).doesNotContain(
                TPartnerPage.TBlock.EAdType.MEDIA_PERFORMANCE,
            )
        }

        Assertions.assertThat(bkData.adTypeList).satisfies { types ->
            Assertions.assertThat(types.map { it.adType }).containsAll(
                setOf(
                    TPartnerPage.TBlock.EAdType.VIDEO,
                    TPartnerPage.TBlock.EAdType.VIDEO_PERFORMANCE,
                    TPartnerPage.TBlock.EAdType.VIDEO_MOTION
                )
            )
            val videoAd = types.first { it.adType == TPartnerPage.TBlock.EAdType.VIDEO }
            Assertions.assertThat(videoAd.currency).isEqualTo("RUB")
            Assertions.assertThat(videoAd.value).isEqualTo(5500L)
        }
    }

    @Test
    fun testBlockCpm() {
        val id = 347649081345L

        dslContext.update(ContextOnSiteRtb.CONTEXT_ON_SITE_RTB)
            .set(ContextOnSiteRtb.CONTEXT_ON_SITE_RTB.STRATEGY, 0)
            .set(ContextOnSiteRtb.CONTEXT_ON_SITE_RTB.MINCPM, BigDecimal.valueOf(5.5))
            .where(ContextOnSiteRtb.CONTEXT_ON_SITE_RTB.UNIQUE_ID.eq(id))
            .execute()

        val bkData = blockBkDataRepository.getBkData(listOf(id)).first()
        Assertions.assertThat(bkData.minCPM).isEqualTo(5500)
    }

    @Test
    fun testBlockDspSettings() {
        val id = 347674247169L

        val bkData = blockBkDataRepository.getBkData(listOf(id)).first()

        Assertions.assertThat(bkData.dspSettings.dspBindMode.name).isEqualTo(DspConstants.DspMode.FORCE.name)

        Assertions.assertThat(bkData.dspSettings.dspList).isEqualTo(listOf(2563081L, 2563120L))
        Assertions.assertThat(bkData.dspSettings.unmoderatedList).isEqualTo(listOf(2563081L))
    }
}
