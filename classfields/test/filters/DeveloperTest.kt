package com.yandex.mobile.realty.test.filters

import com.yandex.mobile.realty.core.robot.performOnDeveloperScreen
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import org.junit.Test

/**
 * @author scrooge on 19.08.2019.
 */
class DeveloperTest : FilterParamTest() {

    @Test
    fun shouldChangeSellNewBuildingOffersCountWhenDeveloperSet() {
        selectDeveloper(PropertyType.APARTMENT) { listSuggestDeveloper1_0() }
    }

    @Test
    fun shouldChangeSellVillageHouseOffersCountWhenDeveloperSet() {
        selectDeveloper(PropertyType.HOUSE) { listSuggestDeveloper2_0() }
    }

    @Test
    fun shouldChangeSellVillageLotOffersCountWhenDeveloperSet() {
        selectDeveloper(PropertyType.LOT) { listSuggestDeveloper2_0() }
    }

    private fun selectDeveloper(
        propertyType: PropertyType,
        webServerConfiguration: DispatcherRegistry.() -> Unit
    ) {
        val offerCategory = OfferCategory.PRIMARY(propertyType)
        shouldChangeOffersCount(
            webServerConfiguration,
            actionConfiguration = {
                tapOn(lookup.matchesDealTypeSelector())
                tapOn(DealType.SELL.matcher.invoke(lookup))
                tapOn(lookup.matchesPropertyTypeSelector())
                tapOn(propertyType.matcher.invoke(lookup))
                tapOn(offerCategory.matcher.invoke(lookup))

                scrollToPosition(lookup.matchesFieldDeveloper()).tapOn()
                performOnDeveloperScreen {
                    typeSearchText("155")
                    waitUntil { containsSuggest("СУ-155") }
                    tapOn(lookup.matchesDeveloperSuggest("СУ-155"))
                }

                hasDeveloper("СУ-155")
            },
            params = arrayOf(
                DealType.SELL.param,
                propertyType.param,
                *(offerCategory.params),
                "developerId" to "6300"
            )
        )
    }

    private fun DispatcherRegistry.listSuggestDeveloper1_0() {
        register(
            request {
                path("1.0/suggest/developer")
                queryParam("text", "155")
            },
            response {
                assetBody("suggestDeveloper.json")
            }
        )
    }

    private fun DispatcherRegistry.listSuggestDeveloper2_0() {
        register(
            request {
                path("2.0/village/developerSuggest")
                queryParam("text", "155")
            },
            response {
                assetBody("suggestDeveloper.json")
            }
        )
    }
}
