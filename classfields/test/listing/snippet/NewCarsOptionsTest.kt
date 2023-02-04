package ru.auto.ara.test.listing.snippet

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.R
import ru.auto.ara.core.dispatchers.BodyNode.Companion.asArray
import ru.auto.ara.core.dispatchers.BodyNode.Companion.asObject
import ru.auto.ara.core.dispatchers.BodyNode.Companion.assertSize
import ru.auto.ara.core.dispatchers.BodyNode.Companion.assertValue
import ru.auto.ara.core.dispatchers.DelegateDispatcher
import ru.auto.ara.core.dispatchers.DelegateDispatcher.Companion.ERROR_CODE
import ru.auto.ara.core.dispatchers.RequestWatcher
import ru.auto.ara.core.dispatchers.asArrayInPath
import ru.auto.ara.core.dispatchers.catalog.getCatalogSubtree
import ru.auto.ara.core.dispatchers.device.ParseDeeplinkDispatcher
import ru.auto.ara.core.dispatchers.device.getParsedDeeplink
import ru.auto.ara.core.dispatchers.search_offers.PostEquipmentFiltersDispatcher
import ru.auto.ara.core.dispatchers.search_offers.PostSearchOffersDispatcher
import ru.auto.ara.core.dispatchers.search_offers.PostSearchOffersGroupDispatcher
import ru.auto.ara.core.robot.offercard.checkGroupOfferCard
import ru.auto.ara.core.robot.offercard.performGroupOfferCard
import ru.auto.ara.core.routing.delegateDispatcher
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.getResourceString
import ru.auto.ara.core.utils.launchDeepLinkActivity
import ru.auto.ara.deeplink.DeeplinkActivity
import ru.auto.data.model.filter.GroupBy

@RunWith(AndroidJUnit4::class)
class NewCarsOptionsTest {

    private val equipmentFiltersWatcher: RequestWatcher = RequestWatcher()
    private val webServerRule = WebServerRule {
        delegateDispatchers(
            ParseDeeplinkDispatcher("relevance"),
            PostSearchOffersDispatcher("feed_of_grouping_card"),
            PostSearchOffersGroupDispatcher("group/mini_hatch.json", setOf(GroupBy.COMPLECTATION_NAME)),
            PostSearchOffersGroupDispatcher("group/mini_hatch.json", setOf(GroupBy.CONFIGURATION))
        )
        getCatalogSubtree("bmw_catalog")
    }

    var activityTestRule = lazyActivityScenarioRule<DeeplinkActivity>()

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        webServerRule,
        activityTestRule
    )

    @Test
    fun shouldShowSortedOptions() {
        webServerRule.routing {
            getParsedDeeplink(expectedResponse = "search_data_bmw_x3", method = DelegateDispatcher.METHOD_POST)
            delegateDispatcher(
                PostEquipmentFiltersDispatcher(
                    filePath = "filters/equipment_filters_bmw_x3.json",
                    requestWatcher = equipmentFiltersWatcher
                )
            )
        }

        openGroup()

        equipmentFiltersWatcher.checkBody {
            asObject {
                getValue("catalog_filter").asArray {
                    assertSize(1)
                    get(0).asObject {
                        getValue("mark").assertValue("BMW")
                        getValue("model").assertValue("X3")
                        getValue("generation").assertValue("21029610")
                        getValue("configuration").assertValue("21029647")
                    }
                }
                getValue("cars_params").asArrayInPath("body_type_group") {
                    single().assertValue("ANY_BODY")
                }
                getValue("in_stock").assertValue("ANY_STOCK")
            }
        }

        performGroupOfferCard()
            .checkResult {
                isSnippetSame(0, "snippets/new_car_group_snippet_with_additional_options.png")
                isAdditionalOptionsDisplayedOrdered(
                    "xDrive20d",
                    "4 доп. опции:",
                    "Легкосплавные диски",
                    "Велюр (Материал салона)",
                    "Рейлинги на крыше",
                    "Обогрев рулевого колеса"
                )
            }
    }

    @Test
    fun shouldShowUnsortedOptionsInCaseOfFailedResponse() {
        webServerRule.routing {
            delegateDispatchers(
                PostEquipmentFiltersDispatcher(
                    filePath = "status_unknown_error.json",
                    code = ERROR_CODE,
                    requestWatcher = equipmentFiltersWatcher
                )
            )
        }

        openGroup()

        performGroupOfferCard()
            .checkResult {
                isAdditionalOptionsDisplayedOrdered(
                    "xDrive20d",
                    "4 доп. опции:",
                    "Рейлинги на крыше",
                    "Велюровый салон",
                    "Легкосплавные диски",
                    "Обогрев рулевого колеса"
                )
            }
    }

    @Test
    fun shouldRequestOptionsInCaseOfFilterChanged() {
        webServerRule.routing {
            getParsedDeeplink(expectedResponse = "search_data_bmw_x3", method = DelegateDispatcher.METHOD_POST)
            delegateDispatchers(
                PostEquipmentFiltersDispatcher(
                    filePath = "filters/equipment_filters_bmw_x3.json",
                    requestWatcher = equipmentFiltersWatcher
                )
            )
        }

        openGroup()

        equipmentFiltersWatcher.checkRequestsCount(1)

        webServerRule.routing { delegateDispatchers(PostSearchOffersDispatcher("feed_of_grouping_card_in_stock")) }

        performGroupOfferCard {
            val inStockStr = getResourceString(R.string.in_stock)
            scrollToFilterChip(inStockStr)
            clickOnFilterChipUntilClearIconDisplayed(inStockStr)
            scrollToSnippet()
        }

        equipmentFiltersWatcher.checkInHistory {
            asObject {
                getValue("catalog_filter").asArray {
                    assertSize(1)
                    get(0).asObject {
                        getValue("mark").assertValue("BMW")
                        getValue("model").assertValue("X3")
                        getValue("generation").assertValue("21029610")
                        getValue("configuration").assertValue("21029647")
                    }
                }
                getValue("cars_params").asArrayInPath("body_type_group") {
                    single().assertValue("ANY_BODY")
                }
                getValue("in_stock").assertValue("IN_STOCK")
            }
            true
        }

        performGroupOfferCard()
            .checkResult {
                isAdditionalOptionsDisplayedOrdered(
                    "xDrive40d",
                    "5 доп. опций:",
                    "Легкосплавные диски",
                    "Система управления дальним светом",
                    "Электрорегулировка передних сидений",
                    "Рейлинги на крыше",
                    "Обогрев рулевого колеса"
                )
            }
    }

    private fun openGroup() {
        activityTestRule.launchDeepLinkActivity("https://auto.ru/cars/new/group/bmw/x3/21029738/21184790/")
        checkGroupOfferCard {
            isOfferCardTitle("BMW X3 Cooper III (G01)")
            isOffersCounter("131 предложение")
        }
    }
}
