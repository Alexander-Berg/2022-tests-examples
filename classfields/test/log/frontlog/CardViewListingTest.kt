package ru.auto.ara.test.log.frontlog

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.hamcrest.Matchers
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.BuildConfig
import ru.auto.ara.core.dispatchers.BodyNode
import ru.auto.ara.core.dispatchers.BodyNode.Companion.asArray
import ru.auto.ara.core.dispatchers.BodyNode.Companion.asObject
import ru.auto.ara.core.dispatchers.BodyNode.Companion.assertValue
import ru.auto.ara.core.dispatchers.RequestWatcher
import ru.auto.ara.core.dispatchers.frontlog.FrontLogDispatcher
import ru.auto.ara.core.dispatchers.offer_card.GetOfferDispatcher
import ru.auto.ara.core.dispatchers.offer_card.GetRelatedDispatcher
import ru.auto.ara.core.dispatchers.offer_card.GetSpecialsDispatcher
import ru.auto.ara.core.dispatchers.search_offers.PostSearchOffersDispatcher
import ru.auto.ara.core.interaction.offercard.OfferCardInteractions
import ru.auto.ara.core.robot.appmetrica.checkAppMetrica
import ru.auto.ara.core.robot.offercard.checkOfferCard
import ru.auto.ara.core.robot.offercard.performOfferCard
import ru.auto.ara.core.robot.searchfeed.performListingOffers
import ru.auto.ara.core.routing.delegateDispatcher
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.rules.DisableAdsRule
import ru.auto.ara.core.rules.SetPreferencesRule
import ru.auto.ara.core.rules.SetupTimeRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.screenbundles.SearchFeedFragmentBundles.searchFeedBundle
import ru.auto.ara.core.utils.launchFragment
import ru.auto.ara.core.utils.waitSomething
import ru.auto.ara.ui.activity.SearchFeedActivity
import ru.auto.ara.ui.fragment.feed.SearchFeedFragment
import ru.auto.data.model.VehicleCategory
import ru.auto.data.model.filter.StateGroup
import ru.auto.data.model.frontlog.SelfType
import java.util.concurrent.TimeUnit

private const val TEST_REQUEST_ID = "testrequestid"

@RunWith(AndroidJUnit4::class)
class CardViewListingTest {

    private val watcher = RequestWatcher()

    private val webServerRule = WebServerRule {
        delegateDispatchers(
            PostSearchOffersDispatcher(
                fileName = "informers_common_snippet_vin_ok_no_history",
                xRequestId = TEST_REQUEST_ID
            ),
            GetOfferDispatcher.getOffer("cars", "1087439802-b6940925"),
            FrontLogDispatcher(watcher)
        )
    }

    private val activityRule = lazyActivityScenarioRule<SearchFeedActivity>()

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        webServerRule,
        SetupTimeRule(localTime = "12:00"),
        DisableAdsRule(),
        SetPreferencesRule(),
        activityRule
    )

    @Test
    fun shouldLogSnippetCardView() {
        activityRule.launchFragment<SearchFeedFragment>(searchFeedBundle(VehicleCategory.CARS, StateGroup.ALL))
        performListingOffers {
            openOfferWithTitle("BMW X5 35d II (E70) Рестайлинг, 2010")
        }.checkResult {
            OfferCardInteractions.onCardTitle().waitUntilIsCompletelyDisplayed()
            watcher.checkBody {
                asObject {
                    get("events").asArray {
                        single { e -> e is BodyNode.Object && e.value.containsKey("card_view_event") }.asObject {
                            get("card_view_event").asObject {
                                get("app_version").assertValue(BuildConfig.VERSION_NAME)
                                get("card_id").assertValue("1087439802-b6940925")
                                get("category").assertValue(VehicleCategory.CARS.toString())
                                get("context_block").assertValue("BLOCK_LISTING")
                                get("context_page").assertValue("PAGE_LISTING")
                                get("context_service").assertValue("SERVICE_AUTORU")
                                get("index").assertValue("0")
                                get("search_query_id").assertValue("29f032b3608d896975864674736f076c169ca4d421600000")
                                get("section").assertValue("USED")
                                get("self_type").assertValue(SelfType.TYPE_SINGLE.toString())
                            }
                            get("uuid").assertValue(Matchers.not(Matchers.isEmptyOrNullString()))
                            get("timestamp").assertValue(Matchers.not(Matchers.isEmptyOrNullString()))
                            get("original_request_id").assertValue(TEST_REQUEST_ID)
                            get("gaid").assertValue(Matchers.not(Matchers.isEmptyOrNullString()))
                        }
                    }
                }
            }
        }
        checkAppMetrica {
            checkAppMetricaEvent(
                eventName = "Просмотр объявления",
                eventParams = mapOf(
                    "категория" to "Легковые автомобили",
                    "Состояние" to "С пробегом",
                    "С транспортным налогом" to "Да",
                    "Тип выдачи" to "Все",
                    "Источник" to "Поисковая выдача",
                    "Наличие панорамы" to "Спинкар"
                )
            )
        }
    }

    @Test
    fun shouldLogSpecialsOffersView() {
        webServerRule.routing {
            delegateDispatcher(GetSpecialsDispatcher.specialsOneOffer())
        }
        activityRule.launchFragment<SearchFeedFragment>(searchFeedBundle(VehicleCategory.CARS, StateGroup.ALL))
        performListingOffers {
            openOfferWithTitle("BMW X5 35d II (E70) Рестайлинг, 2010")
        }
        performOfferCard {
            scrollToSpecials()
        }
        run {
            OfferCardInteractions.onCardTitle().waitUntilIsCompletelyDisplayed()
            watcher.checkBody {
                asObject {
                    get("events").asArray {
                        single { e -> e is BodyNode.Object && e.value.containsKey("card_show_event") }.asObject {
                            get("card_show_event").asObject {
                                get("app_version").assertValue(BuildConfig.VERSION_NAME)
                                get("card_id").assertValue("1089341194-7afb6e9d")
                                get("category").assertValue(VehicleCategory.CARS.toString())
                                get("context_block").assertValue("BLOCK_SPECIALS")
                                get("context_page").assertValue("PAGE_CARD")
                                get("context_service").assertValue("SERVICE_AUTORU")
                                get("index").assertValue("0")
                                get("search_query_id").assertValue("29f032b3608d896975864674736f076c169ca4d421600000")
                                get("section").assertValue("USED")
                                get("self_type").assertValue(SelfType.TYPE_SINGLE.toString())
                            }
                            get("uuid").assertValue(Matchers.not(Matchers.isEmptyOrNullString()))
                            get("timestamp").assertValue(Matchers.not(Matchers.isEmptyOrNullString()))
                            get("original_request_id").assertValue(TEST_REQUEST_ID)
                            get("gaid").assertValue(Matchers.not(Matchers.isEmptyOrNullString()))
                        }
                    }
                }
            }
        }
    }

    @Test
    fun shouldLogRelatedOffersView() {
        webServerRule.routing {
            delegateDispatcher(GetRelatedDispatcher.relatedCarsOneOffer())
        }
        activityRule.launchFragment<SearchFeedFragment>(searchFeedBundle(VehicleCategory.CARS, StateGroup.ALL))
        performListingOffers {
            openOfferWithTitle("BMW X5 35d II (E70) Рестайлинг, 2010")
        }
        scrollToSpecials()
        run {
            watcher.checkBody {
                asObject {
                    get("events").asArray {
                        single { e -> e is BodyNode.Object && e.value.containsKey("card_show_event") }.asObject {
                            get("card_show_event").asObject {
                                get("app_version").assertValue(BuildConfig.VERSION_NAME)
                                get("card_id").assertValue("1089803948-e864fbec")
                                get("category").assertValue(VehicleCategory.CARS.toString())
                                get("context_block").assertValue("BLOCK_RELATED")
                                get("context_page").assertValue("PAGE_CARD")
                                get("context_service").assertValue("SERVICE_AUTORU")
                                get("index").assertValue("0")
                                get("search_query_id").assertValue("29f032b3608d896975864674736f076c169ca4d421600000")
                                get("section").assertValue("USED")
                                get("self_type").assertValue(SelfType.TYPE_SINGLE.toString())
                            }
                            get("uuid").assertValue(Matchers.not(Matchers.isEmptyOrNullString()))
                            get("timestamp").assertValue(Matchers.not(Matchers.isEmptyOrNullString()))
                            get("original_request_id").assertValue(TEST_REQUEST_ID)
                            get("gaid").assertValue(Matchers.not(Matchers.isEmptyOrNullString()))
                        }
                    }
                }
            }
        }
    }

    private fun scrollToSpecials() {
        checkOfferCard { interactions.onMakeCallOnCardButton().waitUntilIsCompletelyDisplayed() }
        performOfferCard {
            collapseAppBar()
            scrollToComplain() //to initiate request for special block
            waitSomething(500, TimeUnit.MILLISECONDS) //wait until specials view is presented
            scrollToSpecials()
        }
    }
}
