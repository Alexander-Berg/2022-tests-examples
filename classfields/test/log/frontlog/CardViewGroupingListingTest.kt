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
import ru.auto.ara.core.dispatchers.DelegateDispatcher
import ru.auto.ara.core.dispatchers.RequestWatcher
import ru.auto.ara.core.dispatchers.catalog.getCatalogSubtree
import ru.auto.ara.core.dispatchers.frontlog.FrontLogDispatcher
import ru.auto.ara.core.dispatchers.offer_card.GetOfferDispatcher
import ru.auto.ara.core.dispatchers.search_offers.PostEquipmentFiltersDispatcher
import ru.auto.ara.core.dispatchers.search_offers.PostSearchOffersDispatcher
import ru.auto.ara.core.interaction.offercard.OfferCardInteractions
import ru.auto.ara.core.robot.offercard.checkGroupOfferCard
import ru.auto.ara.core.robot.offercard.performGroupOfferCard
import ru.auto.ara.core.robot.searchfeed.performListingOffers
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.rules.DisableAdsRule
import ru.auto.ara.core.rules.SetupTimeRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.screenbundles.NewCarsFeedFragmentBundles.newCarsFeedBundle
import ru.auto.ara.core.utils.launchFragment
import ru.auto.ara.ui.activity.SimpleSecondLevelActivity
import ru.auto.data.model.VehicleCategory
import ru.auto.data.model.filter.CatalogFilter
import ru.auto.data.model.frontlog.SelfType
import ru.auto.feature.new_cars.ui.fragment.NewCarsFeedFragment


@RunWith(AndroidJUnit4::class)
class CardViewGroupingListingTest {

    private val watcher = RequestWatcher()

    private val dispatchers: List<DelegateDispatcher> = listOf(
        PostSearchOffersDispatcher("feed_of_grouping_card"),
        GetOfferDispatcher.getOffer("cars", "1092694362-bd611d8e"),
        FrontLogDispatcher(watcher),
        PostEquipmentFiltersDispatcher(filePath = "filters/equipment_filters_bmw_x3.json")
    )

    private val activityRule = lazyActivityScenarioRule<SimpleSecondLevelActivity>()

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        WebServerRule {
            delegateDispatchers(dispatchers)
            getCatalogSubtree("bmw_catalog")
        },
        SetupTimeRule(localTime = "12:00"),
        DisableAdsRule(),
        activityRule
    )

    @Test
    fun shouldLogGroupItemSnippetCardView() {

        activityRule.launchFragment<NewCarsFeedFragment>(newCarsFeedBundle(listOf(CATALOG_FILTER)))

        checkGroupOfferCard {
            isOfferCardTitle("BMW X3 III (G01)")
            isOffersCounter("131 предложение")
        }
        performGroupOfferCard {
            scrollToSnippet()
        }

        performListingOffers {
            openOfferWithTitle("xDrive20d")
        }.checkResult {
            OfferCardInteractions.onCardTitle().waitUntilIsCompletelyDisplayed()
            watcher.checkBody {
                asObject {
                    get("events").asArray {
                        last { e -> e is BodyNode.Object && e.value.containsKey("card_view_event") }.asObject {
                            get("card_view_event").asObject {
                                get("app_version").assertValue(BuildConfig.VERSION_NAME)
                                get("card_id").assertValue("1092694362-bd611d8e")
                                get("category").assertValue(VehicleCategory.CARS.toString())
                                get("context_block").assertValue("BLOCK_LISTING")
                                get("context_page").assertValue("PAGE_GROUP")
                                get("context_service").assertValue("SERVICE_AUTORU")
                                get("group_size").assertValue("131")
                                get("grouping_id").assertValue(SYNTHETIC_GROUPING_ID)
                                get("index").assertValue("0")
                                get("search_query_id").assertValue("1fa9721a8f269d5a7c881d117f9eebcadf7dad0621600000")
                                get("section").assertValue("NEW")
                                get("self_type").assertValue(SelfType.TYPE_SINGLE.toString())
                            }
                            get("uuid").assertValue(Matchers.not(Matchers.isEmptyOrNullString()))
                            get("timestamp").assertValue(Matchers.not(Matchers.isEmptyOrNullString()))
                        }
                    }
                }
            }
        }
    }

    companion object {
        private val CATALOG_FILTER = CatalogFilter(
            mark = "BMW",
            model = "X3",
            generation = 21029610L,
            configuration = 21029647L
        )

        private const val SYNTHETIC_GROUPING_ID = "mark=BMW,model=X3,generation=21029610,configuration=21029647"
    }
}
