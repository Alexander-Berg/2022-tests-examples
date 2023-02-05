package ru.yandex.direct.ui.fragment.campaigns

import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import org.assertj.core.api.Assertions.assertThat
import org.junit.BeforeClass
import org.junit.Test
import ru.yandex.direct.Configuration
import ru.yandex.direct.data.ApiSampleData
import ru.yandex.direct.ui.fragment.campaign.CampaignAction
import ru.yandex.direct.utils.CurrencyInitializer

class CampaignActionTest {

    companion object {
        @BeforeClass
        @JvmStatic
        fun runBeforeAllTests() {
            CurrencyInitializer.injectTestDataInStaticFields()
            val configuration = mock<Configuration> {
                on { isAgency } doReturn false
                on { isSharedAccountEnabled } doReturn false
                on { isCreateEventActionVisible } doReturn false
                on { currentClient } doReturn ApiSampleData.clientInfo
            }
            for (campaign in ApiSampleData.campaigns) {
                campaign.setConfiguration(configuration)
            }
        }
    }

    @Test
    fun priceMaster_shouldBeEnabled_onNonDynamicCampaignWithManualStrategy() {
        val manualCampaigns = ApiSampleData.campaigns
            .filter { it.hasManualRatesControl() and !it.isDynamic and !it.isArchived }
        for (campaign in manualCampaigns) {
            assertThat(CampaignAction.getPossibleActions(campaign)).contains(CampaignAction.PRICE)
        }
    }

    @Test
    fun priceMaster_shouldBeDisabled_onNonDynamicCampaignWithAutoStrategy() {
        val manualCampaigns = ApiSampleData.campaigns
            .filter { it.hasAnyAutoStrategy() and !it.isDynamic and !it.isArchived }
        for (campaign in manualCampaigns) {
            assertThat(CampaignAction.getPossibleActions(campaign)).doesNotContain(CampaignAction.PRICE)
        }
    }

    @Test
    fun priceMaster_shouldBeDisabled_onDynamicCampaigns() {
        val manualCampaigns = ApiSampleData.campaigns.filter { it.isDynamic }
        for (campaign in manualCampaigns) {
            assertThat(CampaignAction.getPossibleActions(campaign)).doesNotContain(CampaignAction.PRICE)
        }
    }

    @Test
    fun statistics_shouldBeEnabled_onNotArchivedCampaigns() {
        val notArchivedCampaigns = ApiSampleData.campaigns.filter { !it.isArchived }
        for (campaign in notArchivedCampaigns) {
            assertThat(CampaignAction.getPossibleActions(campaign)).contains(CampaignAction.CHART)
        }
    }

    @Test
    fun statistics_shouldBeDisabled_onArchivedCampaigns() {
        val archivedCampaigns = ApiSampleData.campaigns.filter { it.isArchived }
        for (campaign in archivedCampaigns) {
            assertThat(CampaignAction.getPossibleActions(campaign)).doesNotContain(CampaignAction.CHART)
        }
    }

    @Test
    fun addToImportant_shouldBeEnabled_onNotImportantCampaigns() {
        val notImportant = ApiSampleData.campaigns.filter { !it.isImportant and !it.isArchived }
        for (campaign in notImportant) {
            val actions = CampaignAction.getPossibleActions(campaign)
            assertThat(actions).contains(CampaignAction.MARK_AS_IMPORTANT)
            assertThat(actions).doesNotContain(CampaignAction.UNMARK_AS_IMPORTANT)
        }
    }

    @Test
    fun removeFromImportant_shouldBeEnabled_onImportantCampaigns() {
        val important = ApiSampleData.campaigns.filter { it.isImportant }
        for (campaign in important) {
            val actions = CampaignAction.getPossibleActions(campaign)
            assertThat(actions).doesNotContain(CampaignAction.MARK_AS_IMPORTANT)
            assertThat(actions).contains(CampaignAction.UNMARK_AS_IMPORTANT)
        }
    }

    @Test
    fun topUp_shouldBeEnabled_onCampaignsWithSharedAccount() {
        val withSharedAccount = ApiSampleData.campaigns
            .filter { it.possiblePaymentWays.isNotEmpty() and !it.isArchived }
        for (campaign in withSharedAccount) {
            assertThat(CampaignAction.getPossibleActions(campaign)).contains(CampaignAction.PAY)
        }
    }

    @Test
    fun unarchive_shouldBeTheOnlyVisibleButton_onUnarchivedCampaigns() {
        val archived = ApiSampleData.campaigns.filter { it.isArchived }
        for (campaign in archived) {
            assertThat(CampaignAction.getPossibleActions(campaign))
                .isEqualTo(listOf(CampaignAction.UNARCHIVE))
        }
    }
}

