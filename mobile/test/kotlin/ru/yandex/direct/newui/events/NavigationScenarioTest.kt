package ru.yandex.direct.newui.events

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.stub
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import org.junit.Before
import org.junit.Test
import ru.yandex.direct.Configuration
import ru.yandex.direct.domain.ShortCampaignInfo
import ru.yandex.direct.domain.account.DailyBudget
import ru.yandex.direct.domain.account.management.SharedAccount
import ru.yandex.direct.domain.banners.ShortBannerInfo
import ru.yandex.direct.domain.banners.ShortBannerPhraseInfo
import ru.yandex.direct.domain.clients.ClientInfo
import ru.yandex.direct.domain.enums.EventType
import ru.yandex.direct.domain.enums.PaymentWay
import ru.yandex.direct.domain.events.LightWeightEvent
import ru.yandex.direct.loaders.impl.statistic.ReportTargetInfo
import ru.yandex.direct.newui.events.navigation.EventDestination
import ru.yandex.direct.newui.events.navigation.EventNavigationDataHolder
import ru.yandex.direct.newui.events.navigation.NavigationScenarioImpl

class NavigationScenarioTest {
    private val configuration = mock<Configuration> {
        on { isAgency } doReturn true
    }

    private val navigation = NavigationScenarioImpl(configuration, mock()).defaultNavigationScenarios

    private val event = LightWeightEvent().apply {
        campaignID = 0
        accountID = 0
        shortCampaignInfo = ShortCampaignInfo()
    }

    private lateinit var dataHolder: EventNavigationDataHolder

    private lateinit var presenter: EventsListPresenter

    private lateinit var view: EventsListView

    private lateinit var client: ClientInfo

    @Before
    fun runBeforeEachTest() {
        client = mock {
            on { isSharedAccountEnabled } doReturn false
            on { getPaymentWays(any()) } doReturn listOf(PaymentWay.TERMINAL)
            on { canEditDailyBudget() } doReturn true
        }
        dataHolder = EventNavigationDataHolder(event, client)
        presenter = mock()
        view = mock()
    }

    @Test
    fun navigation_moneyIn() {
        runCampaignNavigationTest(EventType.MONEY_IN)
    }

    @Test
    fun navigation_moneyOutAccount_sharedAccountEnabled() {
        runSharedAccountPaymentNavigationTest(EventType.MONEY_OUT_ACCOUNT)
    }

    @Test
    fun navigation_moneyOutAccount_sharedAccountDisabled() {
        runCampaignPaymentNavigationTest(EventType.MONEY_OUT_ACCOUNT)
    }

    @Test
    fun navigation_campaignFinished() {
        runCampaignNavigationTest(EventType.CAMPAIGN_FINISHED)
    }

    @Test
    fun navigation_moneyOut_payment_sharedAccountEnabled() {
        val navigation = navigation[EventType.MONEY_OUT]!!

        navigation.navigate(view, presenter, dataHolder)
        verify(view).askDestination(
            dataHolder, listOf(
                EventDestination.PAYMENT_FRAGMENT, EventDestination.CAMPAIGN_FRAGMENT
            )
        )

        dataHolder.destination = EventDestination.PAYMENT_FRAGMENT
        runSharedAccountPaymentNavigationTest(EventType.MONEY_OUT)
    }

    @Test
    fun navigation_moneyOut_payment_sharedAccountDisabled() {
        val navigation = navigation[EventType.MONEY_OUT]!!

        navigation.navigate(view, presenter, dataHolder)
        verify(view).askDestination(
            dataHolder, listOf(
                EventDestination.PAYMENT_FRAGMENT, EventDestination.CAMPAIGN_FRAGMENT
            )
        )

        dataHolder.destination = EventDestination.PAYMENT_FRAGMENT
        runCampaignPaymentNavigationTest(EventType.MONEY_OUT)
    }

    @Test
    fun navigation_moneyOut_campaign() {
        val navigation = navigation[EventType.MONEY_OUT]!!

        navigation.navigate(view, presenter, dataHolder)
        verify(view).askDestination(
            dataHolder, listOf(
                EventDestination.PAYMENT_FRAGMENT, EventDestination.CAMPAIGN_FRAGMENT
            )
        )

        dataHolder.destination = EventDestination.CAMPAIGN_FRAGMENT
        runCampaignNavigationTest(EventType.MONEY_OUT)
    }

    @Test
    fun navigation_moneyWarning_campaign() {
        val navigation = navigation[EventType.MONEY_WARNING]!!

        navigation.navigate(view, presenter, dataHolder)
        verify(view).askDestination(
            dataHolder, listOf(
                EventDestination.PAYMENT_FRAGMENT, EventDestination.CAMPAIGN_STATISTICS_FRAGMENT
            )
        )

        dataHolder.destination = EventDestination.PAYMENT_FRAGMENT
        runCampaignPaymentNavigationTest(EventType.MONEY_WARNING)
    }

    @Test
    fun navigation_moneyWarning_statistics() {
        val navigation = navigation[EventType.MONEY_WARNING]!!

        navigation.navigate(view, presenter, dataHolder)
        verify(view).askDestination(
            dataHolder, listOf(
                EventDestination.PAYMENT_FRAGMENT, EventDestination.CAMPAIGN_STATISTICS_FRAGMENT
            )
        )

        dataHolder.destination = EventDestination.CAMPAIGN_STATISTICS_FRAGMENT
        runCampaignStatisticsNavigationTest(EventType.MONEY_WARNING)
    }

    @Test
    fun navigation_moneyWarningAccount_payment() {
        val navigation = navigation[EventType.MONEY_WARNING_ACCOUNT]!!

        client.stub {
            on { isSharedAccountEnabled } doReturn true
        }

        navigation.navigate(view, presenter, dataHolder)
        verify(view).askDestination(
            dataHolder, listOf(
                EventDestination.PAYMENT_FRAGMENT, EventDestination.OVERALL_STATISTICS_FRAGMENT
            )
        )

        dataHolder.destination = EventDestination.PAYMENT_FRAGMENT
        runSharedAccountPaymentNavigationTest(EventType.MONEY_WARNING_ACCOUNT)
    }

    @Test
    fun navigation_moneyWarningAccount_statistics() {
        val navigation = navigation[EventType.MONEY_WARNING_ACCOUNT]!!

        navigation.navigate(view, presenter, dataHolder)
        verify(view).askDestination(
            dataHolder, listOf(
                EventDestination.PAYMENT_FRAGMENT, EventDestination.OVERALL_STATISTICS_FRAGMENT
            )
        )

        dataHolder.destination = EventDestination.OVERALL_STATISTICS_FRAGMENT
        runOverallStatisticsNavigationTest(EventType.MONEY_WARNING_ACCOUNT)
    }

    @Test
    fun navigation_warnPlace_multiplePhrase_priceMaster() {
        event.phraseID = listOf(0, 1)
        val navigation = navigation[EventType.WARN_PLACE]!!

        navigation.navigate(view, presenter, dataHolder)
        verify(view).askDestination(
            dataHolder, listOf(
                EventDestination.CAMPAIGN_FRAGMENT,
                EventDestination.PRICE_MASTER_FRAGMENT,
                EventDestination.PAYMENT_FRAGMENT
            )
        )

        dataHolder.destination = EventDestination.CAMPAIGN_FRAGMENT
        runCampaignNavigationTest(EventType.WARN_PLACE)
    }

    @Test
    fun navigation_warnPlace_singlePhrase_phrase() {
        event.phraseID = listOf(0)
        val navigation = navigation[EventType.WARN_PLACE]!!

        navigation.navigate(view, presenter, dataHolder)
        verify(view).askDestination(
            dataHolder, listOf(
                EventDestination.CAMPAIGN_FRAGMENT,
                EventDestination.PHRASE_FRAGMENT,
                EventDestination.PAYMENT_FRAGMENT
            )
        )

        dataHolder.destination = EventDestination.PHRASE_FRAGMENT
        navigation.navigate(view, presenter, dataHolder)
        verify(presenter).beginLoadCampaign(dataHolder)

        val campaign = ShortCampaignInfo()
        dataHolder.setCampaign(campaign)
        navigation.navigate(view, presenter, dataHolder)
        verify(presenter).beginLoadPhrase(dataHolder)

        val phrase = ShortBannerPhraseInfo()
        dataHolder.setPhrase(phrase)
        navigation.navigate(view, presenter, dataHolder)
        verify(view).navigateToPhraseFragment(campaign, phrase)
    }

    @Test
    fun navigation_bannerModerated_multipleBanners() {
        event.bannerID = listOf(0, 1)
        runCampaignNavigationTest(EventType.BANNER_MODERATED)
    }

    @Test
    fun navigation_bannerModerated_singleBanner() {
        event.bannerID = listOf(0)
        val navigation = navigation[EventType.BANNER_MODERATED]!!

        navigation.navigate(view, presenter, dataHolder)
        verify(view).askDestination(
            dataHolder, listOf(
                EventDestination.CAMPAIGN_FRAGMENT,
                EventDestination.BANNER_FRAGMENT
            )
        )

        dataHolder.destination = EventDestination.BANNER_FRAGMENT
        navigation.navigate(view, presenter, dataHolder)
        verify(presenter).beginLoadCampaign(dataHolder)

        val campaign = ShortCampaignInfo()
        dataHolder.setCampaign(campaign)
        navigation.navigate(view, presenter, dataHolder)
        verify(presenter).beginLoadBanner(dataHolder)

        val banner = ShortBannerInfo()
        dataHolder.setBanner(banner)
        navigation.navigate(view, presenter, dataHolder)
        verify(view).navigateToBannerFragment(campaign, banner)
    }

    @Test
    fun navigation_moneyOut_noPaymentWays_sharedAccountOn() {
        runMoneyOutNavigationTest(false, true, false)
    }

    @Test
    fun navigation_moneyOut_noPaymentWays_sharedAccountOff() {
        runMoneyOutNavigationTest(false, true, false)
    }

    @Test
    fun navigation_moneyOut_hasPaymentWays_sharedAccountOn() {
        runMoneyOutNavigationTest(true, true, true)
    }

    @Test
    fun navigation_moneyOut_hasPaymentWays_sharedAccountOff() {
        runMoneyOutNavigationTest(true, false, true)
    }

    private fun runMoneyOutNavigationTest(
        hasPaymentWays: Boolean,
        isSharedAccountEnabled: Boolean,
        expectedNavigation: Boolean
    ) {
        event.accountID = 123
        event.campaignID = 456
        client.stub {
            on { this.isSharedAccountEnabled } doReturn isSharedAccountEnabled
            on { getPaymentWays(any()) } doReturn
                    if (hasPaymentWays) {
                        listOf(PaymentWay.TERMINAL)
                    } else {
                        emptyList()
                    }
        }
        val campaign = mock<ShortCampaignInfo>()
        val sharedAccount = SharedAccount().apply { accountID = event.accountID }
        dataHolder.setCampaign(campaign)
        dataHolder.setSharedAccount(sharedAccount)

        navigation[EventType.MONEY_OUT_ACCOUNT]!!.navigate(view, presenter, dataHolder)
        val verify = verify(view, if (expectedNavigation) times(1) else never())
        if (isSharedAccountEnabled) {
            verify.navigateToPaymentFragment(sharedAccount)
        } else {
            verify.navigateToPaymentFragment(campaign)
        }

        navigation[EventType.MONEY_OUT]!!.navigate(view, presenter, dataHolder)
        if (expectedNavigation) {
            verify(view).askDestination(
                dataHolder, listOf(
                    EventDestination.PAYMENT_FRAGMENT,
                    EventDestination.CAMPAIGN_FRAGMENT
                )
            )
        } else {
            verify(view).navigateToCampaignFragment(campaign)
        }
    }

    private fun runCampaignNavigationTest(eventType: EventType) {
        val navigation = navigation[eventType]!!

        navigation.navigate(view, presenter, dataHolder)
        verify(presenter).beginLoadCampaign(dataHolder)

        val campaign = ShortCampaignInfo()
        dataHolder.setCampaign(campaign)
        navigation.navigate(view, presenter, dataHolder)
        verify(view).navigateToCampaignFragment(campaign)
    }

    private fun runCampaignStatisticsNavigationTest(eventType: EventType) {
        val navigation = navigation[eventType]!!

        navigation.navigate(view, presenter, dataHolder)
        verify(presenter).beginLoadCampaign(dataHolder)

        val campaign = ShortCampaignInfo().apply { campaignId = 0 }
        dataHolder.setCampaign(campaign)
        navigation.navigate(view, presenter, dataHolder)
        verify(view).navigateToStatisticsFragment(ReportTargetInfo.forCampaign(campaign.campaignId))
    }

    private fun runOverallStatisticsNavigationTest(eventType: EventType) {
        val navigation = navigation[eventType]!!

        navigation.navigate(view, presenter, dataHolder)
        verify(view).navigateToStatisticsFragment(ReportTargetInfo.overall())
    }

    private fun runSharedAccountPaymentNavigationTest(eventType: EventType) {
        val navigation = navigation[eventType]!!

        client.stub {
            on { isSharedAccountEnabled } doReturn true
        }

        navigation.navigate(view, presenter, dataHolder)
        verify(presenter).beginLoadSharedAccount(dataHolder)

        val sharedAccount = SharedAccount().apply { accountID = 0 }
        dataHolder.setSharedAccount(sharedAccount)
        navigation.navigate(view, presenter, dataHolder)
        verify(view).navigateToPaymentFragment(sharedAccount)
    }

    private fun runCampaignPaymentNavigationTest(eventType: EventType) {
        val navigation = navigation[eventType]!!

        client.stub {
            on { isSharedAccountEnabled } doReturn false
        }

        navigation.navigate(view, presenter, dataHolder)
        verify(presenter).beginLoadCampaign(dataHolder)

        val campaign = ShortCampaignInfo()
        dataHolder.setCampaign(campaign)
        navigation.navigate(view, presenter, dataHolder)
        verify(view).navigateToPaymentFragment(campaign)
    }

    @Test
    fun navigation_pausedByDailyBudget_to_campaignBudgetDialog_ifCanEditCampaigns() {
        val navigation = navigation[EventType.PAUSED_BY_DAY_BUDGET]!!

        navigation.navigate(view, presenter, dataHolder)
        verify(presenter).beginLoadCampaign(dataHolder)

        val campaign = ShortCampaignInfo().apply { dailyBudget = DailyBudget() }
        dataHolder.setCampaign(campaign)
        navigation.navigate(view, presenter, dataHolder)
        verify(view).navigateToCampaignBudgetFragment(campaign)
    }

    @Test
    fun navigation_pausedByDailyBudget_to_campaign_ifHasNoRightToEditBudget() {
        val navigation = navigation[EventType.PAUSED_BY_DAY_BUDGET]!!
        client.stub { on { canEditDailyBudget() } doReturn false }

        navigation.navigate(view, presenter, dataHolder)
        verify(presenter).beginLoadCampaign(dataHolder)

        val campaign = ShortCampaignInfo().apply { dailyBudget = DailyBudget() }
        dataHolder.setCampaign(campaign)
        navigation.navigate(view, presenter, dataHolder)
        verify(view).navigateToCampaignFragment(campaign)
    }

    @Test
    fun navigation_pausedByDailyBudget_to_campaign_ifDailyBudgetIsDisabled() {
        val navigation = navigation[EventType.PAUSED_BY_DAY_BUDGET]!!

        navigation.navigate(view, presenter, dataHolder)
        verify(presenter).beginLoadCampaign(dataHolder)

        val campaign = ShortCampaignInfo()
        dataHolder.setCampaign(campaign)
        navigation.navigate(view, presenter, dataHolder)
        verify(view).navigateToCampaignFragment(campaign)
    }

    @Test
    fun navigation_pausedByDailyBudgetAccount_to_accountBudgetDialog() {
        val navigation = navigation[EventType.PAUSED_BY_DAY_BUDGET_ACCOUNT]!!

        navigation.navigate(view, presenter, dataHolder)
        verify(presenter).beginLoadSharedAccount(dataHolder)

        val sharedAccount = SharedAccount().apply { dailyBudget = DailyBudget() }
        dataHolder.setSharedAccount(sharedAccount)
        navigation.navigate(view, presenter, dataHolder)
        verify(view).navigateToAccountBudgetFragment(sharedAccount)
    }

    @Test
    fun navigation_pausedByDailyBudgetAccount_to_sharedAccount_ifDailyBudgetIsDisabled() {
        val navigation = navigation[EventType.PAUSED_BY_DAY_BUDGET_ACCOUNT]!!

        navigation.navigate(view, presenter, dataHolder)
        verify(presenter).beginLoadSharedAccount(dataHolder)

        dataHolder.setSharedAccount(SharedAccount())
        navigation.navigate(view, presenter, dataHolder)
        verify(view).navigateToSharedAccountFragment()
    }

    @Test
    fun navigation_pausedByDailyBudgetAccount_to_sharedAccount_ifHasNoRightToEditBudget() {
        val navigation = navigation[EventType.PAUSED_BY_DAY_BUDGET_ACCOUNT]!!
        client.stub { on { canEditDailyBudget() } doReturn false }

        navigation.navigate(view, presenter, dataHolder)
        verify(view).navigateToSharedAccountFragment()
    }
}