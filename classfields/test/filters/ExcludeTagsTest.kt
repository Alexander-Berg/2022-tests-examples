package com.yandex.mobile.realty.test.filters

import com.yandex.mobile.realty.core.robot.performOnTagsScreen
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import org.junit.Test

/**
 * @author scrooge on 08.07.2019.
 */
class ExcludeTagsTest : FilterParamTest() {

    @Test
    fun shouldChangeSellApartmentOffersCountWhenExcludeTagSet() {
        selectExcludeTag(
            dealType = DealType.SELL,
            propertyType = PropertyType.APARTMENT
        )
    }

    @Test
    fun shouldChangeSellRoomOffersCountWhenExcludeTagSet() {
        selectExcludeTag(
            dealType = DealType.SELL,
            propertyType = PropertyType.ROOM
        )
    }

    @Test
    fun shouldChangeSellHouseOffersCountWhenExcludeTagSet() {
        selectExcludeTag(
            dealType = DealType.SELL,
            propertyType = PropertyType.HOUSE
        )
    }

    @Test
    fun shouldChangeSellLotOffersCountWhenExcludeTagSet() {
        selectExcludeTag(
            dealType = DealType.SELL,
            propertyType = PropertyType.LOT
        )
    }

    @Test
    fun shouldChangeSellCommercialOffersCountWhenExcludeTagSet() {
        selectExcludeTag(
            dealType = DealType.SELL,
            propertyType = PropertyType.COMMERCIAL
        )
    }

    @Test
    fun shouldChangeSellGarageOffersCountWhenExcludeTagSet() {
        selectExcludeTag(
            dealType = DealType.SELL,
            propertyType = PropertyType.GARAGE
        )
    }

    @Test
    fun shouldChangeRentApartmentOffersCountWhenExcludeTagSet() {
        selectExcludeTag(
            dealType = DealType.RENT,
            propertyType = PropertyType.APARTMENT
        )
    }

    @Test
    fun shouldChangeRentRoomOffersCountWhenExcludeTagSet() {
        selectExcludeTag(
            dealType = DealType.RENT,
            propertyType = PropertyType.ROOM
        )
    }

    @Test
    fun shouldChangeRentHouseOffersCountWhenExcludeTagSet() {
        selectExcludeTag(
            dealType = DealType.RENT,
            propertyType = PropertyType.HOUSE
        )
    }

    @Test
    fun shouldChangeRentCommercialOffersCountWhenExcludeTagSet() {
        selectExcludeTag(
            dealType = DealType.RENT,
            propertyType = PropertyType.COMMERCIAL
        )
    }

    @Test
    fun shouldChangeRentGarageOffersCountWhenExcludeTagSet() {
        selectExcludeTag(
            dealType = DealType.RENT,
            propertyType = PropertyType.GARAGE
        )
    }

    private fun selectExcludeTag(
        dealType: DealType,
        propertyType: PropertyType
    ) {
        shouldChangeOffersCount(
            webServerConfiguration = {
                listSuggestTags()
            },
            actionConfiguration = {
                tapOn(lookup.matchesDealTypeSelector())
                tapOn(dealType.matcher.invoke(lookup))
                tapOn(lookup.matchesPropertyTypeSelector())
                tapOn(propertyType.matcher.invoke(lookup))
                scrollToPosition(lookup.matchesFieldExcludeTags())
                tapOn(lookup.matchesExcludeTagsAddButton())

                performOnTagsScreen {
                    waitUntil { containsTag("в стиле лофт") }
                    tapOn(lookup.matchesTag("в стиле лофт"))
                }

                hasExcludeTag("в стиле лофт")
            },
            params = arrayOf(
                dealType.param,
                propertyType.param,
                "excludeTag" to "1700312"
            )
        )
    }

    private fun DispatcherRegistry.listSuggestTags() {
        register(
            request {
                path("1.0/suggest/tags")
            },
            response {
                assetBody("suggestTagsLoft.json")
            }
        )
    }
}
