package ru.auto.ara.test.user_offers

import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.auto.ara.MainActivity
import ru.auto.ara.R
import ru.auto.ara.core.dispatchers.user.userSetup
import ru.auto.ara.core.dispatchers.user_offers.GetUserOffersDispatcher
import ru.auto.ara.core.robot.transporttab.performMain
import ru.auto.ara.core.robot.useroffers.OffersRobot
import ru.auto.ara.core.robot.useroffers.OffersRobotChecker
import ru.auto.ara.core.robot.useroffers.performOffers
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.rules.SetupAuthRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.data.util.RUB_UNICODE

@RunWith(Parameterized::class)
class UserOffersVasBlocksTest(private val param: TestParam) {

    private val activityTestRule = lazyActivityScenarioRule<MainActivity>()

    private val webServerRule = WebServerRule {
        userSetup()
        delegateDispatchers(
            GetUserOffersDispatcher(page = 1, filePath = param.mock),
            GetUserOffersDispatcher(page = 2, filePath = MOCK_EMPTY_PAGE_2)
        )
    }

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        webServerRule,
        SetupAuthRule()
    )

    @Test
    fun shouldSeeBlock() {
        activityTestRule.launchActivity()

        performMain {
            openLowTab(R.string.offers)
        }.checkResult {
            isLowTabSelected(R.string.offers)
        }

        performOffers {
            waitForOfferSnippets(count = 1)
            collapseToolbar()
            param.onPerform.invoke(this)
        }.checkResult {
            param.onCheck.invoke(this)
        }
    }

    companion object {
        private const val MOCK_EMPTY_PAGE_2 = "user_offers/empty_page_2.json"
        private const val MOCK_VAS_INACTIVE_PAGE_1 = "user_offers/vas_inactive_page_1.json"
        private const val MOCK_VAS_ACTIVE_VIP_PAGE_1 = "user_offers/vas_active_vip_page_1.json"
        private const val MOCK_VAS_ACTIVE_TURBO_PAGE_1 = "user_offers/vas_active_turbo_page_1.json"
        private const val MOCK_VAS_ACTIVE_TOPLIST_PAGE_1 = "user_offers/vas_active_toplist_page_1.json"
        private const val MOCK_VAS_ACTIVE_EXPRESS_PROLONGATION_OFF_PAGE_1 =
            "user_offers/vas_active_express_prolongation_off_page_1.json"
        private const val MOCK_VAS_ACTIVE_SPECIAL_PROLONGATION_OFF_PAGE_1 =
            "user_offers/vas_active_special_prolongation_off_page_1.json"
        private const val MOCK_VAS_ACTIVE_COLOR_PROLONGATION_NOT_AVAILABLE_PAGE_1 =
            "user_offers/vas_active_color_prolongation_not_available_page_1.json"
        private const val MOCK_VAS_STORIES_INACTIVE = "user_offers/vas_stories_inactive.json"
        private const val MOCK_VAS_STORIES_ACTIVE = "user_offers/vas_stories_active.json"

        @JvmStatic
        @Parameterized.Parameters(name = "index={index}")
        fun data() = listOf(
            TestParam(
                MOCK_VAS_INACTIVE_PAGE_1,
                { scrollToFreshVasBlock() },
                {
                    isFreshVasBlockDisplayed(
                        title = "Поднять в поиске за 197 $RUB_UNICODE",
                        autoUpTitle = "Автоподнятие за 197 $RUB_UNICODE в сутки",
                        isActive = false
                    )
                }
            ),
            TestParam(
                MOCK_VAS_INACTIVE_PAGE_1,
                { scrollToVIPVasBlock() },
                {
                    isInactiveVipVasBlockDisplayed(
                        oldPrice = "6 661 $RUB_UNICODE",
                        buyButton = "3 997 $RUB_UNICODE",
                        discount = "-40%"
                    )
                }
            ),
            TestParam(
                MOCK_VAS_INACTIVE_PAGE_1,
                { scrollToTurboVasBlock() },
                {
                    isInactiveTurboVasBlockDisplayed(
                        expiration = "Действует 3 дня",
                        oldPrice = "2 495 $RUB_UNICODE",
                        buyButton = "1 497 $RUB_UNICODE",
                        discount = "-40%"
                    )
                }
            ),
            TestParam(
                MOCK_VAS_INACTIVE_PAGE_1,
                { scrollToExpressVasBlock() },
                {
                    isInactiveExpressVasBlockDisplayed(
                        expiration = "Действует 6 дней",
                        oldPrice = "1 161 $RUB_UNICODE",
                        buyButton = "697 $RUB_UNICODE",
                        discount = "-40%"
                    )
                }
            ),
            TestParam(
                MOCK_VAS_INACTIVE_PAGE_1,
                { scrollToTopListVasBlock() },
                {
                    isInactiveTopListVasBlockDisplayed(
                        expiration = "Действует 9 дней",
                        oldPrice = "1 400 $RUB_UNICODE",
                        buyButton = "1 247 $RUB_UNICODE"
                    )
                }
            ),
            TestParam(
                MOCK_VAS_INACTIVE_PAGE_1,
                { scrollToColorVasBlock() },
                {
                    isInactiveColorVasBlockDisplayed(
                        expiration = "Действует 1 день",
                        buyButton = "297 $RUB_UNICODE"
                    )
                }
            ),
            TestParam(
                MOCK_VAS_INACTIVE_PAGE_1,
                { scrollToSpecialVasBlock() },
                {
                    isInactiveSpecialVasBlockDisplayed(
                        expiration = "Действует 3 дня",
                        oldPrice = "700 $RUB_UNICODE",
                        buyButton = "597 $RUB_UNICODE"
                    )
                }
            ),
            TestParam(
                MOCK_VAS_ACTIVE_VIP_PAGE_1,
                { scrollToVIPVasBlock() },
                { isActiveVipVasBlockDisplayed() }
            ),
            TestParam(
                MOCK_VAS_ACTIVE_TURBO_PAGE_1,
                { scrollToTurboVasBlock() },
                {
                    isActiveTurboVasBlockDisplayed(
                        autoprolongDetails = "2 495 $RUB_UNICODE каждые 3 дня",
                        autoprolongIsActive = true
                    )
                }
            ),
            TestParam(
                MOCK_VAS_ACTIVE_TOPLIST_PAGE_1,
                { scrollToTopListVasBlock() },
                {
                    isActiveTopListVasBlockDisplayed(
                        autoprolongDetails = "1 247 $RUB_UNICODE каждые 3 дня",
                        autoprolongIsActive = true
                    )
                }
            ),
            TestParam(
                MOCK_VAS_ACTIVE_EXPRESS_PROLONGATION_OFF_PAGE_1,
                { scrollToExpressVasBlock() },
                {
                    isActiveExpressVasBlockDisplayed(
                        autoprolongDetails = "1 161 $RUB_UNICODE каждые 6 дней",
                        autoprolongIsActive = false
                    )
                }
            ),
            TestParam(
                MOCK_VAS_ACTIVE_SPECIAL_PROLONGATION_OFF_PAGE_1,
                { scrollToSpecialVasBlock() },
                {
                    isActiveSpecialVasBlockDisplayed(
                        autoprolongDetails = "597 $RUB_UNICODE каждые 3 дня",
                        autoprolongIsActive = false
                    )
                }
            ),
            TestParam(
                MOCK_VAS_ACTIVE_COLOR_PROLONGATION_NOT_AVAILABLE_PAGE_1,
                { scrollToColorVasBlock() },
                { isActiveColorVasBlockDisplayed() }
            ),
            TestParam(
                MOCK_VAS_STORIES_INACTIVE,
                { scrollToStoriesVasBlock() },
                { isStoriesVasBlockDisplayed("user_offers/vas_stories_block_container_inactive.png") }
            ),
            TestParam(
                MOCK_VAS_STORIES_ACTIVE,
                { scrollToStoriesVasBlock() },
                { isStoriesVasBlockDisplayed("user_offers/vas_stories_block_container_active.png") }
            )
        )
    }

    class TestParam(
        val mock: String,
        val onPerform: OffersRobot.() -> Unit,
        val onCheck: OffersRobotChecker.() -> Unit
    )
}
