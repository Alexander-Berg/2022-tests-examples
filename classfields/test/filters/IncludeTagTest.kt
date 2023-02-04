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
class IncludeTagTest : FilterParamTest() {

    @Test
    fun shouldChangeSellApartmentOffersCountWhenIncludeTagSet() {
        selectIncludeTag(
            dealType = DealType.SELL,
            propertyType = PropertyType.APARTMENT
        )
    }

    @Test
    fun shouldChangeSellRoomOffersCountWhenIncludeTagSet() {
        selectIncludeTag(
            dealType = DealType.SELL,
            propertyType = PropertyType.ROOM
        )
    }

    @Test
    fun shouldChangeSellHouseOffersCountWhenIncludeTagSet() {
        selectIncludeTag(
            dealType = DealType.SELL,
            propertyType = PropertyType.HOUSE
        )
    }

    @Test
    fun shouldChangeSellLotOffersCountWhenIncludeTagSet() {
        selectIncludeTag(
            dealType = DealType.SELL,
            propertyType = PropertyType.LOT
        )
    }

    @Test
    fun shouldChangeSellCommercialOffersCountWhenIncludeTagSet() {
        selectIncludeTag(
            dealType = DealType.SELL,
            propertyType = PropertyType.COMMERCIAL
        )
    }

    @Test
    fun shouldChangeSellGarageOffersCountWhenIncludeTagSet() {
        selectIncludeTag(
            dealType = DealType.SELL,
            propertyType = PropertyType.GARAGE
        )
    }

    @Test
    fun shouldChangeRentApartmentOffersCountWhenIncludeTagSet() {
        selectIncludeTag(
            dealType = DealType.RENT,
            propertyType = PropertyType.APARTMENT
        )
    }

    @Test
    fun shouldChangeRentRoomOffersCountWhenIncludeTagSet() {
        selectIncludeTag(
            dealType = DealType.RENT,
            propertyType = PropertyType.ROOM
        )
    }

    @Test
    fun shouldChangeRentHouseOffersCountWhenIncludeTagSet() {
        selectIncludeTag(
            dealType = DealType.RENT,
            propertyType = PropertyType.HOUSE
        )
    }

    @Test
    fun shouldChangeRentCommercialOffersCountWhenIncludeTagSet() {
        selectIncludeTag(
            dealType = DealType.RENT,
            propertyType = PropertyType.COMMERCIAL
        )
    }

    @Test
    fun shouldChangeRentGarageOffersCountWhenIncludeTagSet() {
        selectIncludeTag(
            dealType = DealType.RENT,
            propertyType = PropertyType.GARAGE
        )
    }

    private fun selectIncludeTag(
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
                scrollToPosition(lookup.matchesFieldIncludeTags())
                tapOn(lookup.matchesIncludeTagsAddButton())

                performOnTagsScreen {
                    waitUntil { containsTag("в стиле лофт") }
                    tapOn(lookup.matchesTag("в стиле лофт"))
                }

                hasIncludeTag("в стиле лофт")
            },
            params = arrayOf(
                dealType.param,
                propertyType.param,
                "includeTag" to "1700312"
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
