package ru.yandex.partner.core.entity.block

import NPartner.Page.TPartnerPage
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.jooq.DSLContext
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.partner.core.CoreConstants
import ru.yandex.partner.core.CoreTest
import ru.yandex.partner.core.block.BlockUniqueIdConverter
import ru.yandex.partner.core.bs.BkDataRepository
import ru.yandex.partner.core.bs.BkFiller
import ru.yandex.partner.core.bs.InheritanceOrder
import ru.yandex.partner.core.bs.PartialOrdering
import ru.yandex.partner.core.entity.block.model.BaseBlock
import ru.yandex.partner.core.entity.block.repository.BlockBkFiller
import ru.yandex.partner.core.entity.block.type.dspsunmoderated.BlockWithDspsUnmoderatedRepositoryTypeSupport.DSP_OPTION_UNMODERATED_RTB_AUCTION
import ru.yandex.partner.core.junit.MySqlRefresher
import ru.yandex.partner.dbschema.partner.enums.ContextOnSiteRtbSiteVersion
import ru.yandex.partner.dbschema.partner.tables.ContextOnSiteRtb.CONTEXT_ON_SITE_RTB
import ru.yandex.partner.libs.bs.json.BkDataConverter
import ru.yandex.partner.test.http.json.utils.TestCaseManager
import ru.yandex.partner.test.utils.TestUtils
import ru.yandex.partner.test.utils.TestUtils.getAbsolutePath
import ru.yandex.partner.test.utils.TestUtils.prepareJsonFile

@CoreTest
@ExtendWith(MySqlRefresher::class)
internal class BlockBkDataRepositoryTest(
    @Autowired
    val blockBkDataRepository: BkDataRepository<BaseBlock, TPartnerPage.TBlock>,
    @Autowired
    val objectMapper: ObjectMapper,
    @Autowired
    val dslContext: DSLContext,
    @Autowired
    val fillers: List<BlockBkFiller<out BaseBlock>>
) {

    @Test
    internal fun getBkData() {
        var bkData = blockBkDataRepository.getBkData(listOf(347674247170L)).first()

        assertThat(bkData.adFoxBlock).isNotNull
        assertThat(bkData.adFoxBlock).isFalse

        assertThat(bkData.geoList).isNotEmpty
        assertThat(bkData.dspType).isEqualTo(1)

        assertThat(bkData.cpmCurrency).isEqualTo(CoreConstants.Currency.DEFAULT) // user has no currency in opts

        bkData = blockBkDataRepository.getBkData(listOf(347649081345L)).first() // with brands
        assertThat(bkData.brandList).isNotEmpty
    }

    @Test
    internal fun getBkDataDspTypeByShowVideo() {
        val id = BlockUniqueIdConverter.convertToUniqueId(
            BlockUniqueIdConverter.Prefixes.CONTEXT_ON_SITE_RTB_PREFIX, 41449, 2
        )

        var bkData = blockBkDataRepository.getBkData(listOf(id)).first()
        assertThat(bkData.dspType).isEqualTo(1)

        // todo переделать, когда появиться show_video = 1 в наливке бд
        dslContext.update(CONTEXT_ON_SITE_RTB)
            .set(CONTEXT_ON_SITE_RTB.SITE_VERSION, ContextOnSiteRtbSiteVersion.desktop)
            .set(CONTEXT_ON_SITE_RTB.SHOW_VIDEO, 1L)
            .execute()

        bkData = blockBkDataRepository.getBkData(listOf(id)).first()
        assertThat(bkData.dspType).isEqualTo(3)
    }

    @Test
    internal fun getBkDataDspInfo() {
        val bkData = blockBkDataRepository.getBkData(listOf(347674247169L)).first()

        assertThat(bkData.dspInfoList).isNotEmpty

        assertThat(bkData.dspInfoList.find { it.dspid == 2563081L }!!)
            .satisfies { dsp ->
                assertThat(dsp.pageDspOptions.enableList)
                    .contains(DSP_OPTION_UNMODERATED_RTB_AUCTION)
                assertThat(dsp.cpm)
                    .describedAs("cpm is zero for MAX_REVENUE strategy")
                    .isEqualTo(0)
            }
        assertThat(bkData.dspInfoList.find { it.dspid == 2563120L }!!)
            .satisfies { dsp ->
                assertThat(dsp.pageDspOptions.disableList)
                    .contains(DSP_OPTION_UNMODERATED_RTB_AUCTION)
                assertThat(dsp.cpm)
                    .describedAs("cpm is zero for MAX_REVENUE strategy")
                    .isEqualTo(0)
            }
    }

    @Test
    internal fun getBkDataPiCategories() {
        val bkData = blockBkDataRepository.getBkData(listOf(347649081345L)).first()

        assertThat(bkData.piCategoryIABList.size).isEqualTo(3)

        assertThat(bkData.piCategoryIABList.find { it.categoryID == 53L }!!)
            .satisfies { pic ->
                assertThat(pic.mediaCreativeReach).isEqualTo(8000)
                assertThat(pic.mediaImageReach).isEqualTo(8000)
            }

        assertThat(bkData.piCategoryIABList.find { it.categoryID == 54L }!!)
            .satisfies { pic ->
                assertThat(pic.mediaCreativeReach).isEqualTo(8000)
                assertThat(pic.mediaImageReach).isEqualTo(8000)
            }
        assertThat(bkData.piCategoryIABList.find { it.categoryID == 55L }!!)
            .satisfies { pic ->
                assertThat(pic.mediaCreativeReach).isEqualTo(8000)
                assertThat(pic.mediaImageReach).isEqualTo(8000)
            }

    }

    @Test
    internal fun siteVersionWithoutImpOptionTest() {
        val bkData = blockBkDataRepository.getBkData(listOf(347674247170L)).first() // block with desktop version

        assertThat(bkData.pageImpOptions.disableList).containsAll(listOf("amp", "turbo", "turbo-desktop"))
        assertThat(bkData.pageImpOptions.enableList).isEmpty()
    }


    @Test
    internal fun getBkDataDesignTemplates() {
        val bkData = blockBkDataRepository.getBkData(
            listOf(
                BlockUniqueIdConverter.convertToUniqueId(
                    BlockUniqueIdConverter.Prefixes.CONTEXT_ON_SITE_RTB_PREFIX,
                    41443,
                    2
                )
            )
        ).first()

        val rtbDesign = """
{
    "4": {
        "name": "default template",
        "design": {
            "borderType": "ad",
            "imagesFirst": false,
            "siteBgColor": "FFFFFF",
            "headerBgColor": "FFFFFF",
            "borderColor": "DDDCDA",
            "urlBackgroundColor": "0000CC",
            "favicon": false,
            "noSitelinks": false,
            "linksUnderline": false,
            "hoverColor": "DD0000",
            "urlColor": "000000",
            "textColor": "000000",
            "blockId": "R-A-41443-2",
            "sitelinksColor": "0000CC",
            "bgColor": "FF667F",
            "titleColor": "00EECA",
            "borderRadius": false,
            "horizontalAlign": true,
            "name": "modernAdaptive",
            "limit": "2"
        },
        "type": "tga"
    },
    "11": {
        "name": "default template",
        "design": {
            "filterSizes": false,
            "blockId": "R-A-41443-2",
            "horizontalAlign": true
        },
        "type": "media"
    }
}
        """

        val objectMapper = ObjectMapper()
        assertThat(objectMapper.readTree(bkData.rtbDesign)).isEqualTo(objectMapper.readTree(rtbDesign))
        assertThat(bkData.adTypeSetList).containsExactlyInAnyOrder(
            TPartnerPage.TBlock.TAdTypeSet.newBuilder()
                .setAdType(TPartnerPage.TBlock.EAdType.MEDIA)
                .setValue(true)
                .build(),
            TPartnerPage.TBlock.TAdTypeSet.newBuilder()
                .setAdType(TPartnerPage.TBlock.EAdType.MEDIA_PERFORMANCE)
                .setValue(true)
                .build(),
            TPartnerPage.TBlock.TAdTypeSet.newBuilder()
                .setAdType(TPartnerPage.TBlock.EAdType.TEXT)
                .setValue(true)
                .build(),
            TPartnerPage.TBlock.TAdTypeSet.newBuilder()
                .setAdType(TPartnerPage.TBlock.EAdType.VIDEO)
                .setValue(false)
                .build(),
            TPartnerPage.TBlock.TAdTypeSet.newBuilder()
                .setAdType(TPartnerPage.TBlock.EAdType.VIDEO_MOTION)
                .setValue(false)
                .build(),
            TPartnerPage.TBlock.TAdTypeSet.newBuilder()
                .setAdType(TPartnerPage.TBlock.EAdType.VIDEO_PERFORMANCE)
                .setValue(false)
                .build()
        )
    }

    @Test
    internal fun siteVersionWithImpOptionTest() {
        val bkData = blockBkDataRepository.getBkData(listOf(347699412994)).first() // block with amp

        assertThat(bkData.pageImpOptions.enableList).contains("amp")
        assertThat(bkData.pageImpOptions.disableList).containsAll(listOf("turbo", "turbo-desktop"))
    }

    @Test
    internal fun nestedFieldsBuildTest() {
        val builder = TPartnerPage.TBlock.newBuilder()

        builder.pageImpOptionsBuilder
            .addEnable("enable1")
            .addEnable("enable2")

        builder.pageImpOptionsBuilder
            .addDisable("disable1")

        val block = builder.build()

        val impOptions = block.pageImpOptions

        assertThat(impOptions.enableList).isNotEmpty
        assertThat(impOptions.disableList).isNotEmpty
    }

    @Test
    internal fun assertFillersSortIsCorrect() {
        val fillerInheritanceOrder =
            Comparator.comparing<BkFiller<*, *, *>, Class<*>>({ it.typeClass }, InheritanceOrder())
        val sortedFillers = PartialOrdering(
            fillerInheritanceOrder
        ).sort(fillers.sortedBy { it.order() })

        assertThat(sortedFillers)
            .isSortedAccordingTo(fillerInheritanceOrder)

        val fillersWithCustomOrder = sortedFillers
            .filter { it.order() != Integer.MAX_VALUE }.toList()

        assertThat(fillersWithCustomOrder).isEqualTo(
            fillersWithCustomOrder.sortedBy { it.order() }.toList()
        )
    }

    @Test
    internal fun mockedDbBkFillerTest() {
        val converter = BkDataConverter()

        data class Ids(val uniqueId: Long, val id: Long, val pageId: Long);

        val isExpectedBkData: MutableMap<String, Boolean> = mutableMapOf()

        val objectMapper = ObjectMapper()
            .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
            .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
            .enable(SerializationFeature.INDENT_OUTPUT)

        dslContext.select(
            CONTEXT_ON_SITE_RTB.UNIQUE_ID,
            CONTEXT_ON_SITE_RTB.ID,
            CONTEXT_ON_SITE_RTB.CAMPAIGN_ID
        ).from(CONTEXT_ON_SITE_RTB)
            .fetchStream()
            .parallel()
            .map { Ids(
                it.get(0, Long::class.java),
                it.get(1, Long::class.java),
                it.get(2, Long::class.java)
            ) }
            .forEach {
                val proto = blockBkDataRepository.getBkData(listOf(it.uniqueId)).first()
                val actual = TestCaseManager.sortJsonNode(converter.convertProtoToJsonTree(proto))
                val referenceDataPath = "bkdata/block/${it.pageId}-${it.id}.json"

                val file = prepareJsonFile(getAbsolutePath(referenceDataPath))
                val expected = objectMapper.readTree(file)

                if (TestUtils.needSelfUpdate()) {
                    objectMapper.writeValue(file, actual)
                } else {
                    isExpectedBkData["${it.pageId}-${it.id}"] = expected.equals(actual)
                }
            }

        if (TestUtils.needSelfUpdate()) {
            Assertions.fail("Self updated")
        }

        val bkDataWithDiff = isExpectedBkData.filterValues { !it }
        if (bkDataWithDiff.isNotEmpty()) {
            Assertions.fail(
                "Bk data comparison failed for ${bkDataWithDiff.size} blocks: ${bkDataWithDiff.keys}"
            )
        }
    }
}
