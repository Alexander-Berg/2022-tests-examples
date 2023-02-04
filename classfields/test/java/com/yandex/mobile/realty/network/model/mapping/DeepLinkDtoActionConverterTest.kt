package com.yandex.mobile.realty.network.model.mapping

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.yandex.mobile.realty.data.mapping.EmptyDescriptor
import com.yandex.mobile.realty.data.model.CommuteDto
import com.yandex.mobile.realty.data.model.GeoObjectDto
import com.yandex.mobile.realty.data.model.GeoPolygonDto
import com.yandex.mobile.realty.data.model.GeoRegionDto
import com.yandex.mobile.realty.data.model.deeplink.DeepLinkDto
import com.yandex.mobile.realty.data.model.search.ParamsItemDto
import com.yandex.mobile.realty.deeplink.DeepLinkParsedData
import com.yandex.mobile.realty.deeplink.InvalidDeepLinkActionException
import com.yandex.mobile.realty.deeplink.InvalidDeepLinkParameterException
import com.yandex.mobile.realty.deeplink.NewBuildingDeepLinkContext
import com.yandex.mobile.realty.deeplink.OfferDeepLinkContext
import com.yandex.mobile.realty.deeplink.OfferListDeepLinkContext
import com.yandex.mobile.realty.deeplink.OfferMapDeepLinkContext
import com.yandex.mobile.realty.deeplink.SiteListDeepLinkContext
import com.yandex.mobile.realty.deeplink.SiteMapDeepLinkContext
import com.yandex.mobile.realty.deeplink.VillageDeepLinkContext
import com.yandex.mobile.realty.deeplink.VillageListDeepLinkContext
import com.yandex.mobile.realty.deeplink.VillageMapDeepLinkContext
import com.yandex.mobile.realty.network.model.FilterParamsNames.CATEGORY
import com.yandex.mobile.realty.network.model.FilterParamsNames.TYPE
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * @author rogovalex on 2019-10-03.
 */
class DeepLinkDtoActionConverterTest {

    private val converter = DeepLinkDto.Converter(Gson())

    @Test(expected = InvalidDeepLinkActionException::class)
    fun shouldThrowWhenActionMissed() {
        val deepLinkDto = createDeepLinkDto()

        convert(deepLinkDto)
    }

    @Test(expected = InvalidDeepLinkActionException::class)
    fun shouldThrowWhenActionUnknown() {
        val deepLinkDto = createDeepLinkDto(
            action = "SOME_ACTION"
        )

        convert(deepLinkDto)
    }

    @Test(expected = InvalidDeepLinkParameterException::class)
    fun shouldThrowWhenParamsMissedForOfferCard() {
        val deepLinkDto = createDeepLinkDto(
            action = "OFFER_CARD"
        )

        convert(deepLinkDto)
    }

    @Test(expected = InvalidDeepLinkParameterException::class)
    fun shouldThrowWhenIdMissedForOfferCard() {
        val deepLinkDto = createDeepLinkDto(
            action = "OFFER_CARD",
            params = listOf()
        )

        convert(deepLinkDto)
    }

    @Test
    fun shouldConvertOfferCard() {
        val deepLinkDto = createDeepLinkDto(
            action = "OFFER_CARD",
            params = listOf(
                ParamsItemDto(name = "id", values = listOf("1"))
            )
        )

        val deepLink = convert(deepLinkDto)

        val context = deepLink.context as OfferDeepLinkContext
        assertEquals("1", context.offerId)
    }

    @Test(expected = InvalidDeepLinkParameterException::class)
    fun shouldThrowWhenParamsMissedForSiteCard() {
        val deepLinkDto = createDeepLinkDto(
            action = "SITE_CARD"
        )

        convert(deepLinkDto)
    }

    @Test(expected = InvalidDeepLinkParameterException::class)
    fun shouldThrowWhenIdMissedForSiteCard() {
        val deepLinkDto = createDeepLinkDto(
            action = "SITE_CARD",
            params = listOf()
        )

        convert(deepLinkDto)
    }

    @Test
    fun shouldConvertSiteCard() {
        val deepLinkDto = createDeepLinkDto(
            action = "SITE_CARD",
            params = listOf(
                ParamsItemDto(name = "id", values = listOf("1"))
            )
        )

        val deepLink = convert(deepLinkDto)

        val context = deepLink.context as NewBuildingDeepLinkContext
        assertEquals("1", context.siteId)
    }

    @Test(expected = InvalidDeepLinkParameterException::class)
    fun shouldThrowWhenParamsMissedForVillageCard() {
        val deepLinkDto = createDeepLinkDto(
            action = "VILLAGE_CARD"
        )

        convert(deepLinkDto)
    }

    @Test(expected = InvalidDeepLinkParameterException::class)
    fun shouldThrowWhenIdMissedForVillageCard() {
        val deepLinkDto = createDeepLinkDto(
            action = "VILLAGE_CARD",
            params = listOf()
        )

        convert(deepLinkDto)
    }

    @Test
    fun shouldConvertVillageCard() {
        val deepLinkDto = createDeepLinkDto(
            action = "VILLAGE_CARD",
            params = listOf(
                ParamsItemDto(name = "id", values = listOf("1"))
            )
        )

        val deepLink = convert(deepLinkDto)

        val context = deepLink.context as VillageDeepLinkContext
        assertEquals("1", context.villageId)
    }

    @Test(expected = InvalidDeepLinkParameterException::class)
    fun shouldThrowWhenParamsMissedForOfferList() {
        val deepLinkDto = createDeepLinkDto(
            action = "OFFER_LIST"
        )

        convert(deepLinkDto)
    }

    @Test(expected = InvalidDeepLinkParameterException::class)
    fun shouldThrowWhenDealAndPropertyTypeMissedForOfferList() {
        val deepLinkDto = createDeepLinkDto(
            action = "OFFER_LIST",
            params = listOf()
        )

        convert(deepLinkDto)
    }

    @Test(expected = InvalidDeepLinkParameterException::class)
    fun shouldThrowWhenDealTypeMissedForOfferList() {
        val deepLinkDto = createDeepLinkDto(
            action = "OFFER_LIST",
            params = listOf(
                ParamsItemDto(name = CATEGORY, values = listOf("APARTMENT"))
            )
        )

        convert(deepLinkDto)
    }

    @Test(expected = InvalidDeepLinkParameterException::class)
    fun shouldThrowWhenPropertyTypeMissedForOfferList() {
        val deepLinkDto = createDeepLinkDto(
            action = "OFFER_LIST",
            params = listOf(
                ParamsItemDto(name = TYPE, values = listOf("SELL"))
            )
        )

        convert(deepLinkDto)
    }

    @Test
    fun shouldConvertOfferList() {
        val deepLinkDto = createDeepLinkDto(
            action = "OFFER_LIST",
            params = listOf(
                ParamsItemDto(name = TYPE, values = listOf("RENT")),
                ParamsItemDto(name = CATEGORY, values = listOf("ROOMS"))
            )
        )

        val deepLink = convert(deepLinkDto)

        assertTrue(deepLink.context is OfferListDeepLinkContext)
    }

    @Test
    fun shouldConvertSiteList() {
        val deepLinkDto = createDeepLinkDto(
            action = "SITE_LIST"
        )

        val deepLink = convert(deepLinkDto)

        assertTrue(deepLink.context is SiteListDeepLinkContext)
    }

    @Test
    fun shouldConvertVillageList() {
        val deepLinkDto = createDeepLinkDto(
            action = "VILLAGE_LIST"
        )

        val deepLink = convert(deepLinkDto)

        assertTrue(deepLink.context is VillageListDeepLinkContext)
    }

    @Test(expected = InvalidDeepLinkParameterException::class)
    fun shouldThrowWhenParamsMissedForOfferMap() {
        val deepLinkDto = createDeepLinkDto(
            action = "OFFER_MAP"
        )

        convert(deepLinkDto)
    }

    @Test(expected = InvalidDeepLinkParameterException::class)
    fun shouldThrowWhenDealAndPropertyTypeMissedForOfferMap() {
        val deepLinkDto = createDeepLinkDto(
            action = "OFFER_MAP",
            params = listOf()
        )

        convert(deepLinkDto)
    }

    @Test(expected = InvalidDeepLinkParameterException::class)
    fun shouldThrowWhenDealTypeMissedForOfferMap() {
        val deepLinkDto = createDeepLinkDto(
            action = "OFFER_MAP",
            params = listOf(
                ParamsItemDto(name = CATEGORY, values = listOf("APARTMENT"))
            )
        )

        convert(deepLinkDto)
    }

    @Test(expected = InvalidDeepLinkParameterException::class)
    fun shouldThrowWhenPropertyTypeMissedForOfferMap() {
        val deepLinkDto = createDeepLinkDto(
            action = "OFFER_MAP",
            params = listOf(
                ParamsItemDto(name = TYPE, values = listOf("SELL"))
            )
        )

        convert(deepLinkDto)
    }

    @Test
    fun shouldConvertOfferMap() {
        val deepLinkDto = createDeepLinkDto(
            action = "OFFER_MAP",
            params = listOf(
                ParamsItemDto(name = TYPE, values = listOf("SELL")),
                ParamsItemDto(name = CATEGORY, values = listOf("APARTMENT"))
            )
        )

        val deepLink = convert(deepLinkDto)

        assertTrue(deepLink.context is OfferMapDeepLinkContext)
    }

    @Test
    fun shouldConvertSiteMap() {
        val deepLinkDto = createDeepLinkDto(
            action = "SITE_MAP"
        )

        val deepLink = convert(deepLinkDto)

        assertTrue(deepLink.context is SiteMapDeepLinkContext)
    }

    @Test
    fun shouldConvertVillageMap() {
        val deepLinkDto = createDeepLinkDto(
            action = "VILLAGE_MAP"
        )

        val deepLink = convert(deepLinkDto)

        assertTrue(deepLink.context is VillageMapDeepLinkContext)
    }

    private fun convert(deepLinkDto: DeepLinkDto): DeepLinkParsedData {
        return converter.map(deepLinkDto, EmptyDescriptor)
    }

    private fun createDeepLinkDto(
        action: String? = null,
        region: GeoRegionDto? = null,
        geoIntents: List<GeoObjectDto>? = null,
        mapPolygons: List<GeoPolygonDto>? = null,
        commute: CommuteDto? = null,
        params: List<ParamsItemDto>? = null,
        filter: JsonObject? = null
    ): DeepLinkDto {
        return DeepLinkDto(
            action,
            region,
            geoIntents,
            mapPolygons,
            commute,
            params,
            filter
        )
    }
}
