package ru.yandex.market.clean.presentation.feature.cms.item.gallery.singleaction.snippet.referralprogram

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import ru.beru.android.R
import ru.yandex.market.clean.domain.model.cms.CmsReferralProgramItem
import ru.yandex.market.domain.media.model.measuredImageReferenceTestInstance
import ru.yandex.market.common.android.ResourcesManager

class CmsReferralProgramFormatterTest {

    private val cmsReferralProgramItem = CmsReferralProgramItem(
        bonusAmount = 300,
        icon = measuredImageReferenceTestInstance()
    )
    private val expected = CmsReferralProgramVo(
        title = "Expected title",
        subtitle = "Expected subtitle",
        primaryButtonLabel = "Expected primaryButtonLabel",
        secondaryButtonLabel = "Expected secondaryButtonLabel",
        image = cmsReferralProgramItem.icon
    )

    private val resourcesDataStore = mock<ResourcesManager> {
        on { getString(R.string.cms_widget_referral_program_title) } doReturn expected.title
        on {
            getFormattedString(
                R.string.cms_widget_referral_program_subtitle,
                cmsReferralProgramItem.bonusAmount
            )
        } doReturn expected.subtitle
        on { getString(R.string.cms_widget_referral_program_primary_button) } doReturn expected.primaryButtonLabel
        on { getString(R.string.cms_widget_referral_program_secondary_button) } doReturn expected.secondaryButtonLabel
    }

    private val formatter = CmsReferralProgramFormatter(resourcesDataStore)

    @Test
    fun testFormat() {
        assertThat(formatter.format(cmsReferralProgramItem)).isEqualTo(expected)
    }
}