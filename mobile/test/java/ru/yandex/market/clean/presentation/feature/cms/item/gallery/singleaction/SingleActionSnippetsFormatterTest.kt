package ru.yandex.market.clean.presentation.feature.cms.item.gallery.singleaction

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import ru.yandex.market.clean.domain.model.cms.CmsActualProductOrderItem
import ru.yandex.market.clean.domain.model.cms.CmsActualOrderItem
import ru.yandex.market.clean.domain.model.cms.CmsAdvertisingCampaignItem
import ru.yandex.market.clean.domain.model.cms.CmsGrowingCashbackItem
import ru.yandex.market.clean.domain.model.cms.CmsItem
import ru.yandex.market.clean.domain.model.cms.CmsPlusBenefitsItem
import ru.yandex.market.clean.domain.model.cms.CmsPlusHomeNavigationItem
import ru.yandex.market.clean.domain.model.cms.CmsReferralProgramItem
import ru.yandex.market.clean.domain.model.cms.SoftUpdateMergedWidgetCmsItem
import ru.yandex.market.clean.presentation.feature.cms.item.gallery.singleaction.snippet.advertisingcamaign.CmsAdvertisingCampaignVo
import ru.yandex.market.clean.presentation.feature.cms.item.gallery.singleaction.snippet.growingcashback.CmsGrowingCashbackVo
import ru.yandex.market.clean.presentation.feature.cms.item.gallery.singleaction.snippet.plushome.CmsPlusHomeNavigationVo
import ru.yandex.market.clean.presentation.feature.cms.item.gallery.singleaction.snippet.referralprogram.CmsReferralProgramVo
import ru.yandex.market.clean.presentation.feature.cms.model.CmsActualProductOrderVo
import ru.yandex.market.clean.presentation.feature.cms.model.CmsActualOrderVo
import ru.yandex.market.clean.presentation.feature.cms.model.CmsViewObject
import ru.yandex.market.clean.presentation.feature.cms.model.PlusBenefitsWidgetCmsVo
import ru.yandex.market.clean.presentation.feature.cms.model.SoftUpdateMergedWidgetCmsVo

class SingleActionSnippetsFormatterTest {

    private val actualOrderCmsItem = mock<CmsActualOrderItem>()
    private val actualProductCmsItem = mock<CmsActualProductOrderItem>()
    private val plusBenefitsCmsItem = mock<CmsPlusBenefitsItem>()
    private val softUpdateCmsItem = mock<SoftUpdateMergedWidgetCmsItem>()
    private val advertisingCampaignItem = mock<CmsAdvertisingCampaignItem>()
    private val referralProgramItem = mock<CmsReferralProgramItem>()
    private val plusHomeItem = mock<CmsPlusHomeNavigationItem>()
    private val growingCashbackItem = mock<CmsGrowingCashbackItem>()

    private val actualOrderViewObject = mock<CmsActualOrderVo>()
    private val actualLavkaViewObject = mock<CmsActualProductOrderVo>()
    private val plusBenefitsViewObject = mock<PlusBenefitsWidgetCmsVo>()
    private val softUpdateViewObject = mock<SoftUpdateMergedWidgetCmsVo>()
    private val advertisingCampaignVo = mock<CmsAdvertisingCampaignVo>()
    private val referralProgramVo = mock<CmsReferralProgramVo>()
    private val plusHomeVo = mock<CmsPlusHomeNavigationVo>()
    private val growingCashbackVo = mock<CmsGrowingCashbackVo>()

    private val formatter = SingleActionSnippetsFormatter(
        actualOrderFormatter = mock { on { format(actualOrderCmsItem) } doReturn actualOrderViewObject },
        actualProductOrderFormatter = mock { on { format(actualProductCmsItem) } doReturn actualLavkaViewObject },
        plusBenefitsFormatter = mock { on { format(plusBenefitsCmsItem) } doReturn plusBenefitsViewObject },
        softUpdateFormatter = mock { on { format(softUpdateCmsItem) } doReturn softUpdateViewObject },
        cmsAdvertisingCampaignFormatter = mock { on { format(advertisingCampaignItem) } doReturn advertisingCampaignVo },
        cmsReferralProgramFormatter = mock { on { format(referralProgramItem) } doReturn referralProgramVo },
        cmsPlusHomeNavigationFormatter = mock { on { format(plusHomeItem) } doReturn plusHomeVo },
        cmsGrowingCashbackFormatter = mock { on { format(growingCashbackItem) } doReturn growingCashbackVo }
    )

    @Test
    fun `format supported items`() {
        val items = listOf(
            actualOrderCmsItem,
            actualProductCmsItem,
            plusBenefitsCmsItem,
            softUpdateCmsItem,
            advertisingCampaignItem,
            referralProgramItem,
            plusHomeItem,
            growingCashbackItem
        )
        val expected = listOf(
            actualOrderViewObject,
            actualLavkaViewObject,
            plusBenefitsViewObject,
            softUpdateViewObject,
            advertisingCampaignVo,
            referralProgramVo,
            plusHomeVo,
            growingCashbackVo
        )

        assertThat(formatter.format(items)).isEqualTo(expected)
    }

    @Test
    fun `skip unsupported items`() {
        val items = listOf<CmsItem>(
            mock(),
            mock(),
        )
        val expected = emptyList<CmsViewObject>()

        assertThat(expected).isEqualTo(formatter.format(items))
    }
}