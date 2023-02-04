package ru.auto.ara.test.deeplink

import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.auto.ara.R
import ru.auto.ara.core.dispatchers.device.copyWithSort
import ru.auto.ara.core.dispatchers.device.getParsedDeeplink
import ru.auto.ara.core.dispatchers.salon.getCustomizableSalonById
import ru.auto.ara.core.dispatchers.search_offers.PostSearchOffersDispatcher
import ru.auto.ara.core.robot.appmetrica.checkAppMetrica
import ru.auto.ara.core.robot.autoselectionrequestform.checkAutoSelectionForm
import ru.auto.ara.core.robot.burger.checkBurger
import ru.auto.ara.core.robot.chat.checkChatRoom
import ru.auto.ara.core.robot.dealer.checkDealerFeed
import ru.auto.ara.core.robot.searchfeed.checkSearchFeed
import ru.auto.ara.core.robot.searchfeed.performFeedSort
import ru.auto.ara.core.robot.searchfeed.performSearchFeed
import ru.auto.ara.core.robot.transporttab.performMain
import ru.auto.ara.core.robot.webview.performWebView
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.rules.DisableAdsRule
import ru.auto.ara.core.rules.GrantPermissionsRule
import ru.auto.ara.core.rules.SetPreferencesRule
import ru.auto.ara.core.rules.WebClientJsEnabledRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.getResourceString
import ru.auto.ara.core.utils.launchDeepLinkActivity
import ru.auto.ara.core.utils.pressBack
import ru.auto.ara.deeplink.DeeplinkActivity
import ru.auto.data.model.filter.StateGroup
import ru.auto.data.network.scala.response.DeeplinkParseResponse

@RunWith(Parameterized::class)
class DeeplinkTest( private val testParameter: TestParameter) {

    private val activityTestRule = lazyActivityScenarioRule<DeeplinkActivity>()
    private val webServerRule = WebServerRule {
        getCustomizableSalonById(dealerId = 20135120, dealerCode = "panavto_moskva_mercedes")
        getCustomizableSalonById(dealerId = 20134482, dealerCode = "panavto_moskva_mercedes")
        delegateDispatchers(PostSearchOffersDispatcher("panavto_moskva_mercedes"))
        getParsedDeeplink(
            expectedResponse = testParameter.parseDeeplinkFile,
            mapper = testParameter.parseDeeplinkMapper
        )
    }

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        webServerRule,
        DisableAdsRule(),
        activityTestRule,
        SetPreferencesRule(),
        WebClientJsEnabledRule(false),
        GrantPermissionsRule() // to grant location permission
    )

    @Test
    fun shouldOpenDeeplink() {
        activityTestRule.launchDeepLinkActivity(testParameter.uri)
        testParameter.check()
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "index={index}")
        fun data(): Collection<Array<Any>> =
            listOf(
                TestParameter(
                    uri = "http://auto.ru/for_me",
                    check = { performMain {}.checkResult { isMainTabSelected(R.string.for_me) } }
                ),
                TestParameter(
                    uri = "autoru://app/for_me",
                    check = { performMain {}.checkResult { isMainTabSelected(R.string.for_me) } }
                ),
                TestParameter(
                    uri = "https://auto.ru/diler/cars/all/aksel_motors_v_o_sankt_peterburg_bmw/?from=wmbt",
                    check = { checkDealerFeed { checkDealerFeedIsOpen() } },
                    parseDeeplinkFile = "diler_card"
                ),
                TestParameter(
                    uri = "https://auto.ru/diler-oficialniy/cars/all/panavto_moskva_mercedes/?from=dealer-listing-list",
                    check = { checkDealerFeed { checkDealerFeedIsOpen() } },
                    parseDeeplinkFile = "diler_card"
                ),
                TestParameter(
                    uri = "https://mag.auto.ru/",
                    check = { performWebView {}.checkResult { isWebViewToolBarDisplayed("Журнал") } }
                ),
                TestParameter(
                    uri = "https://mag.auto.ru/theme/uchebnik/",
                    check = { performWebView {}.checkResult { isWebViewToolBarDisplayed("Учебник") } }
                ),
                TestParameter(
                    uri = "autoru://app/chat",
                    check = { checkChatRoom { interactions.onChatScreenTitle().waitUntilIsCompletelyDisplayed() } }
                ),
                TestParameter(
                    uri = "autoru://app/reviews/chat",
                    check = { checkChatRoom { interactions.onChatScreenTitle().waitUntilIsCompletelyDisplayed() } }
                ),
                TestParameter(
                    uri = "https://auto.ru/chat",
                    check = { checkChatRoom { interactions.onChatScreenTitle().waitUntilIsCompletelyDisplayed() } }
                ),
                TestParameter(
                    uri = "https://auto.ru/reviews/chat",
                    check = { checkChatRoom { interactions.onChatScreenTitle().waitUntilIsCompletelyDisplayed() } }
                ),
                TestParameter(
                    uri = "https://auto.ru",
                    check = { performMain {}.checkResult { isMainTabSelected(R.string.transport) } }
                ),
                TestParameter(
                    uri = "https://auto.ru/?from=wizard.brand&geo_id=213",
                    check = { performMain {}.checkResult { isMainTabSelected(R.string.transport) }  }
                ),
                TestParameter(
                    uri = "https://auto.ru/?nosplash=1",
                    check = { performMain {}.checkResult { isMainTabSelected(R.string.transport) }  }
                ),
                TestParameter(
                    uri = "https://auto.ru/?utm_referrer=https:%2F%2Fyandex.ru%2F%3Ffrom%3Dalice",
                    check = { performMain {}.checkResult { isMainTabSelected(R.string.transport) }  }
                ),
                TestParameter(
                    uri = "https://auto.ru/promo/from-web-to-app/",
                    check = { performMain {}.checkResult { isMainTabSelected(R.string.transport) }  }
                ),
                TestParameter(
                    uri = "https://auto.ru/like/cars/",
                    check = { performMain {}.checkResult { isMainTabSelected(R.string.transport) }  }
                ),
                TestParameter(
                    uri = "https://autoru-pdd.ru",
                    check = { performWebView {}.checkResult { isWebViewToolBarDisplayed(R.string.auto_pdd_test) } }
                ),
                TestParameter(
                    uri = "https://auto.ru/moskva/cars/new/get_best_price",
                    check = {
                        checkAutoSelectionForm { checkTitleVisible() }
                        pressBack()
                        checkSearchFeed {
                            isStateSelectorChecked(StateGroup.NEW)
                            isEmptyMMNGFilterDisplayed()
                        }
                    }
                ),
                TestParameter(
                    uri = "https://auto.ru/cars/all/" +
                        "?year_from=2011&from=yandex-m-gl&utm_source=yandex-m_list_service&utm_medium=cpm" +
                        "&utm_campaign=q2_ymls_r1_subtitle&utm_content=mbwrdvzrst-34312_r1" +
                        "&utm_term=s-probegom-do-5-let_mbwrdvzrst-34312",
                    check = {
                        performSearchFeed { waitFirstPage() }
                        checkAppMetrica {
                            checkAppMetricaEvent(
                                eventName = "Deeplink. Открыть листинг по марке",
                                eventParams = mapOf(
                                    "utm_term" to "s-probegom-do-5-let_mbwrdvzrst-34312",
                                    "utm_campaign" to "q2_ymls_r1_subtitle",
                                    "utm_medium" to "cpm",
                                    "from" to "yandex-m-gl",
                                    "utm_source" to "yandex-m_list_service",
                                    "utm_content" to "mbwrdvzrst-34312_r1"
                                )
                            )
                        }
                    }
                ),
                TestParameter(
                    uri = "autoru://app/menu",
                    check = { checkBurger { isMenuDisplayed() } }
                ),
                getSearchSortTestParameter("fresh_relevance_1-desc", R.string.sort_relevance, true),
                getSearchSortTestParameter("cr_date-desc", R.string.sort_date, true),
                getSearchSortTestParameter("price-asc", R.string.sort_price_asc),
                getSearchSortTestParameter("price-desc", R.string.sort_price_desc, true),
                getSearchSortTestParameter("year-asc", R.string.sort_year_asc),
                getSearchSortTestParameter("year-desc", R.string.sort_year_desc, true),
                getSearchSortTestParameter("km_age-asc", R.string.sort_run),
                getSearchSortTestParameter("alphabet-asc", R.string.sort_name),
                getSearchSortTestParameter("autoru_exclusive-desc", R.string.sort_exclusive, true),
                getSearchSortTestParameter("price_profitability-desc", R.string.sort_deals, true),
            ).map { arrayOf(it) }

        private fun getSearchSortTestParameter(sort: String, sortNameRes: Int, desc: Boolean = false) =
            TestParameter(
                uri = "https://auto.ru/moskva/cars/audi/all/?sort=$sort",
                check = {
                    val sortName = getResourceString(sortNameRes)
                    performSearchFeed { openSort() }
                    performFeedSort {
                        scrollToSortItem(sortName)
                    }.checkResult { checkSortSelected(sortName) }
                },
                parseDeeplinkMapper = { it.copyWithSort(sort.substringBefore("-"), desc) }
            )

        data class TestParameter(
             val uri: String,
             val check: () -> Unit,
             val parseDeeplinkFile: String = "cars_all",
             val parseDeeplinkMapper: (DeeplinkParseResponse) -> DeeplinkParseResponse = { it }
        )
    }
}
