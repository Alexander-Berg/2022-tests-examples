package ru.yandex.market.clean.presentation.feature.cms.item.gallery.singleaction

import com.mikepenz.fastadapter.IItem
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.kotlin.mock
import ru.yandex.market.clean.presentation.feature.cms.item.gallery.singleaction.snippet.advertisingcamaign.AdvertisingCampaignSnippetItem
import ru.yandex.market.clean.presentation.feature.cms.item.gallery.singleaction.snippet.advertisingcamaign.CmsAdvertisingCampaignVo
import ru.yandex.market.clean.presentation.feature.cms.item.gallery.singleaction.snippet.productorder.ActualProductOrderSnippetItem
import ru.yandex.market.clean.presentation.feature.cms.item.gallery.singleaction.snippet.order.ActualOrderSnippetItem
import ru.yandex.market.clean.presentation.feature.cms.item.gallery.singleaction.snippet.plusbenefits.PlusBenefitsSnippetItem
import ru.yandex.market.clean.presentation.feature.cms.item.gallery.singleaction.snippet.plushome.CmsPlusHomeNavigationVo
import ru.yandex.market.clean.presentation.feature.cms.item.gallery.singleaction.snippet.plushome.PlusHomeSnippetItem
import ru.yandex.market.clean.presentation.feature.cms.item.gallery.singleaction.snippet.referralprogram.CmsReferralProgramVo
import ru.yandex.market.clean.presentation.feature.cms.item.gallery.singleaction.snippet.referralprogram.ReferralProgramSnippetItem
import ru.yandex.market.clean.presentation.feature.cms.item.gallery.singleaction.snippet.softupdate.SoftUpdateSnippetItem
import ru.yandex.market.clean.presentation.feature.cms.model.CmsActualProductOrderVo
import ru.yandex.market.clean.presentation.feature.cms.model.CmsActualOrderVo
import ru.yandex.market.clean.presentation.feature.cms.model.CmsViewObject
import ru.yandex.market.clean.presentation.feature.cms.model.PlusBenefitsWidgetCmsVo
import ru.yandex.market.clean.presentation.feature.cms.model.SoftUpdateMergedWidgetCmsVo

@RunWith(Parameterized::class)
class SingleActionGallerySnippetItemFactoryTest(
    private val viewObject: CmsViewObject,
    private val expectedItem: IItem<*>?,
) {

    private val factory = SingleActionGallerySnippetItemFactory(
        mvpDelegate = mock(),
        imageLoader = { mock() },
        plusBenefitsSnippetPresenterFactory = { mock() },
        actualOrderSnippetPresenterFactory = { mock() },
        actualProductOrderSnippetPresenterFactory = { mock() },
        softUpdateSnippetPresenterFactory = { mock() },
        advertisingCampaignSnippetPresenterFactory = { mock() },
        referralProgramSnippetPresenterFactory = { mock() },
        analyticsService = { mock() },
        plusHomeSnippetPresenterFactory = { mock() },
        cmsGrowingCashbackPresenterFactory = { mock() },
    )

    @Test
    fun `Create item`() {
        val item = factory.createSnippets("parentId", listOf(viewObject))
        assertThat(item.getOrNull(0)).isEqualTo(expectedItem)
    }

    companion object {
        private val ACTUAL_ORDER_VO = mock<CmsActualOrderVo>()
        private val LAVKA_ORDER_VO = mock<CmsActualProductOrderVo>()
        private val BENEFITS_VO = mock<PlusBenefitsWidgetCmsVo>()
        private val SOFT_UPDATE_VO = mock<SoftUpdateMergedWidgetCmsVo>()
        private val ADVERTISING_CAMPAIGN_VO = mock<CmsAdvertisingCampaignVo>()
        private val REFERRAL_PROGRAM_VO = mock<CmsReferralProgramVo>()
        private val PLUS_HOME_VO = mock<CmsPlusHomeNavigationVo>()

        @Parameterized.Parameters(name = "{index}: {0} -> {1}")
        @JvmStatic
        fun data(): Iterable<Array<*>> = listOf(
            //0
            arrayOf(
                ACTUAL_ORDER_VO,
                ActualOrderSnippetItem(
                    mock(),
                    ACTUAL_ORDER_VO,
                    mock(),
                    mock(),
                )
            ),
            //1
            arrayOf(
                LAVKA_ORDER_VO,
                ActualProductOrderSnippetItem(
                    mock(),
                    LAVKA_ORDER_VO,
                    mock(),
                    mock()
                )
            ),
            //2
            arrayOf(
                BENEFITS_VO,
                PlusBenefitsSnippetItem(
                    mock(),
                    BENEFITS_VO,
                    mock(),
                    mock()
                )
            ),
            //3
            arrayOf(
                SOFT_UPDATE_VO,
                SoftUpdateSnippetItem(
                    mock(),
                    SOFT_UPDATE_VO,
                    mock(),
                    mock()
                )
            ),
            //4
            arrayOf(
                ADVERTISING_CAMPAIGN_VO,
                AdvertisingCampaignSnippetItem(
                    mock(),
                    ADVERTISING_CAMPAIGN_VO,
                    mock(),
                    mock()
                )
            ),
            //5
            arrayOf(
                REFERRAL_PROGRAM_VO,
                ReferralProgramSnippetItem(
                    mock(),
                    REFERRAL_PROGRAM_VO,
                    mock(),
                    mock()
                )
            ),
            //6
            arrayOf(
                PLUS_HOME_VO,
                PlusHomeSnippetItem(
                    mock(),
                    PLUS_HOME_VO,
                    mock(),
                    mock()
                )
            ),
            //7
            arrayOf(
                mock<CmsViewObject>(),
                null
            )
        )
    }
}