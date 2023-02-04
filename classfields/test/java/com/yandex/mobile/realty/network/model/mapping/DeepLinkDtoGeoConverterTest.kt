package com.yandex.mobile.realty.network.model.mapping

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.yandex.mobile.realty.data.mapping.EmptyDescriptor
import com.yandex.mobile.realty.data.model.CommuteDto
import com.yandex.mobile.realty.data.model.GeoObjectDto
import com.yandex.mobile.realty.data.model.GeoPointDto
import com.yandex.mobile.realty.data.model.GeoPolygonDto
import com.yandex.mobile.realty.data.model.GeoRegionDto
import com.yandex.mobile.realty.data.model.deeplink.DeepLinkDto
import com.yandex.mobile.realty.data.model.search.ParamsItemDto
import com.yandex.mobile.realty.deeplink.DeepLinkParsedData
import com.yandex.mobile.realty.deeplink.OfferListDeepLinkContext
import com.yandex.mobile.realty.deeplink.OfferMapDeepLinkContext
import com.yandex.mobile.realty.deeplink.SiteListDeepLinkContext
import com.yandex.mobile.realty.deeplink.SiteMapDeepLinkContext
import com.yandex.mobile.realty.deeplink.VillageListDeepLinkContext
import com.yandex.mobile.realty.deeplink.VillageMapDeepLinkContext
import com.yandex.mobile.realty.domain.model.commute.CommuteParams
import com.yandex.mobile.realty.domain.model.geo.GeoIntent
import com.yandex.mobile.realty.domain.model.geo.GeoObject
import com.yandex.mobile.realty.domain.model.geo.GeoPoint
import com.yandex.mobile.realty.domain.model.geo.GeoPolygon
import com.yandex.mobile.realty.domain.model.geo.GeoRegion
import com.yandex.mobile.realty.domain.model.geo.GeoType
import com.yandex.mobile.realty.network.model.FilterParamsNames.CATEGORY
import com.yandex.mobile.realty.network.model.FilterParamsNames.TYPE
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * @author rogovalex on 2019-10-03.
 */
class DeepLinkDtoGeoConverterTest {

    private val converter = DeepLinkDto.Converter(Gson())

    @Test
    fun shouldConvertOfferListWithRegion() {
        val deepLinkDto = createDeepLinkDto(
            action = "OFFER_LIST",
            params = listOf(
                ParamsItemDto(name = TYPE, values = listOf("RENT")),
                ParamsItemDto(name = CATEGORY, values = listOf("ROOMS"))
            ),
            region = createTestRegionDto()
        )

        val deepLink = convert(deepLinkDto)

        val context = deepLink.context as OfferListDeepLinkContext

        val expectedRegion = createExpectedRegion()
        assertEquals(expectedRegion, context.region)
    }

    @Test
    fun shouldConvertOfferListWithGeoSuggestWithoutRegion() {
        val deepLinkDto = createDeepLinkDto(
            action = "OFFER_LIST",
            params = listOf(
                ParamsItemDto(name = TYPE, values = listOf("SELL")),
                ParamsItemDto(name = CATEGORY, values = listOf("LOT"))
            ),
            geoIntents = listOf(createTestGeoObjectDto())
        )

        val deepLink = convert(deepLinkDto)

        val context = deepLink.context as OfferListDeepLinkContext

        val geoIntent = context.geoIntent as GeoIntent.Objects
        assertNull(context.region)
        val expectedGeoObject = listOf(createExpectedGeoObject())
        assertEquals(expectedGeoObject, geoIntent.items)
    }

    @Test
    fun shouldConvertOfferListWithGeoSuggest() {
        val deepLinkDto = createDeepLinkDto(
            action = "OFFER_LIST",
            params = listOf(
                ParamsItemDto(name = TYPE, values = listOf("RENT")),
                ParamsItemDto(name = CATEGORY, values = listOf("ROOMS"))
            ),
            region = createTestRegionDto(),
            geoIntents = listOf(createTestGeoObjectDto())
        )

        val deepLink = convert(deepLinkDto)

        val context = deepLink.context as OfferListDeepLinkContext

        val geoIntent = context.geoIntent as GeoIntent.Objects
        assertEquals(createExpectedRegion(), context.region)
        val expectedGeoObject = listOf(createExpectedGeoObject())
        assertEquals(expectedGeoObject, geoIntent.items)
    }

    @Test
    fun shouldConvertOfferListWithGeoPolygonWithoutRegion() {
        val deepLinkDto = createDeepLinkDto(
            action = "OFFER_LIST",
            params = listOf(
                ParamsItemDto(name = TYPE, values = listOf("SELL")),
                ParamsItemDto(name = CATEGORY, values = listOf("LOT"))
            ),
            mapPolygons = listOf(createTestGeoPolygonDto())
        )

        val deepLink = convert(deepLinkDto)

        val context = deepLink.context as OfferListDeepLinkContext

        val geoIntent = context.geoIntent as GeoIntent.Polygons
        assertNull(context.region)
        val expected = listOf(createExpectedGeoPolygon())
        assertEquals(expected, geoIntent.items)
    }

    @Test
    fun shouldConvertOfferListWithGeoPolygon() {
        val deepLinkDto = createDeepLinkDto(
            action = "OFFER_LIST",
            params = listOf(
                ParamsItemDto(name = TYPE, values = listOf("SELL")),
                ParamsItemDto(name = CATEGORY, values = listOf("LOT"))
            ),
            region = createTestRegionDto(),
            mapPolygons = listOf(createTestGeoPolygonDto())
        )

        val deepLink = convert(deepLinkDto)

        val context = deepLink.context as OfferListDeepLinkContext

        val geoIntent = context.geoIntent as GeoIntent.Polygons
        assertEquals(createExpectedRegion(), context.region)
        val expected = listOf(createExpectedGeoPolygon())
        assertEquals(expected, geoIntent.items)
    }

    @Test
    fun shouldConvertOfferListWithGeoSuggestOverGeoPolygon() {
        val deepLinkDto = createDeepLinkDto(
            action = "OFFER_LIST",
            params = listOf(
                ParamsItemDto(name = TYPE, values = listOf("RENT")),
                ParamsItemDto(name = CATEGORY, values = listOf("APARTMENT"))
            ),
            region = createTestRegionDto(),
            geoIntents = listOf(createTestGeoObjectDto()),
            mapPolygons = listOf(createTestGeoPolygonDto())
        )

        val deepLink = convert(deepLinkDto)

        val context = deepLink.context as OfferListDeepLinkContext

        val geoIntent = context.geoIntent as GeoIntent.Objects
        assertEquals(createExpectedRegion(), context.region)
        val expectedGeoObject = listOf(createExpectedGeoObject())
        assertEquals(expectedGeoObject, geoIntent.items)
    }

    @Test
    fun shouldConvertSiteListWithRegion() {
        val deepLinkDto = createDeepLinkDto(
            action = "SITE_LIST",
            region = createTestRegionDto()
        )

        val deepLink = convert(deepLinkDto)

        val context = deepLink.context as SiteListDeepLinkContext

        val expectedRegion = createExpectedRegion()
        assertEquals(expectedRegion, context.region)
    }

    @Test
    fun shouldConvertSiteListWithGeoSuggestWithoutRegion() {
        val deepLinkDto = createDeepLinkDto(
            action = "SITE_LIST",
            geoIntents = listOf(createTestGeoObjectDto())
        )

        val deepLink = convert(deepLinkDto)

        val context = deepLink.context as SiteListDeepLinkContext

        val geoIntent = context.geoIntent as GeoIntent.Objects
        assertNull(context.region)
        val expectedGeoObject = listOf(createExpectedGeoObject())
        assertEquals(expectedGeoObject, geoIntent.items)
    }

    @Test
    fun shouldConvertSiteListWithGeoSuggest() {
        val deepLinkDto = createDeepLinkDto(
            action = "SITE_LIST",
            region = createTestRegionDto(),
            geoIntents = listOf(createTestGeoObjectDto())
        )

        val deepLink = convert(deepLinkDto)

        val context = deepLink.context as SiteListDeepLinkContext

        val geoIntent = context.geoIntent as GeoIntent.Objects
        assertEquals(createExpectedRegion(), context.region)
        val expectedGeoObject = listOf(createExpectedGeoObject())
        assertEquals(expectedGeoObject, geoIntent.items)
    }

    @Test
    fun shouldConvertSiteListWithGeoPolygonWithoutRegion() {
        val deepLinkDto = createDeepLinkDto(
            action = "SITE_LIST",
            mapPolygons = listOf(createTestGeoPolygonDto())
        )

        val deepLink = convert(deepLinkDto)

        val context = deepLink.context as SiteListDeepLinkContext

        val geoIntent = context.geoIntent as GeoIntent.Polygons
        assertNull(context.region)
        val expected = listOf(createExpectedGeoPolygon())
        assertEquals(expected, geoIntent.items)
    }

    @Test
    fun shouldConvertSiteListWithGeoPolygon() {
        val deepLinkDto = createDeepLinkDto(
            action = "SITE_LIST",
            region = createTestRegionDto(),
            mapPolygons = listOf(createTestGeoPolygonDto())
        )

        val deepLink = convert(deepLinkDto)

        val context = deepLink.context as SiteListDeepLinkContext

        val geoIntent = context.geoIntent as GeoIntent.Polygons
        assertEquals(createExpectedRegion(), context.region)
        val expected = listOf(createExpectedGeoPolygon())
        assertEquals(expected, geoIntent.items)
    }

    @Test
    fun shouldConvertSiteListWithGeoSuggestOverGeoPolygon() {
        val deepLinkDto = createDeepLinkDto(
            action = "SITE_LIST",
            region = createTestRegionDto(),
            geoIntents = listOf(createTestGeoObjectDto()),
            mapPolygons = listOf(createTestGeoPolygonDto())
        )

        val deepLink = convert(deepLinkDto)

        val context = deepLink.context as SiteListDeepLinkContext

        val geoIntent = context.geoIntent as GeoIntent.Objects
        assertEquals(createExpectedRegion(), context.region)
        val expectedGeoObject = listOf(createExpectedGeoObject())
        assertEquals(expectedGeoObject, geoIntent.items)
    }

    @Test
    fun shouldConvertVillageListWithRegion() {
        val deepLinkDto = createDeepLinkDto(
            action = "VILLAGE_LIST",
            params = listOf(
                ParamsItemDto(name = TYPE, values = listOf("RENT")),
                ParamsItemDto(name = CATEGORY, values = listOf("ROOMS"))
            ),
            region = createTestRegionDto()
        )

        val deepLink = convert(deepLinkDto)

        val context = deepLink.context as VillageListDeepLinkContext

        val expectedRegion = createExpectedRegion()
        assertEquals(expectedRegion, context.region)
    }

    @Test
    fun shouldConvertVillageListWithGeoSuggestWithoutRegion() {
        val deepLinkDto = createDeepLinkDto(
            action = "VILLAGE_LIST",
            params = listOf(
                ParamsItemDto(name = TYPE, values = listOf("SELL")),
                ParamsItemDto(name = CATEGORY, values = listOf("LOT"))
            ),
            geoIntents = listOf(createTestGeoObjectDto())
        )

        val deepLink = convert(deepLinkDto)

        val context = deepLink.context as VillageListDeepLinkContext

        val geoIntent = context.geoIntent as GeoIntent.Objects
        assertNull(context.region)
        val expectedGeoObject = listOf(createExpectedGeoObject())
        assertEquals(expectedGeoObject, geoIntent.items)
    }

    @Test
    fun shouldConvertVillageListWithGeoSuggest() {
        val deepLinkDto = createDeepLinkDto(
            action = "VILLAGE_LIST",
            params = listOf(
                ParamsItemDto(name = TYPE, values = listOf("RENT")),
                ParamsItemDto(name = CATEGORY, values = listOf("ROOMS"))
            ),
            region = createTestRegionDto(),
            geoIntents = listOf(createTestGeoObjectDto())
        )

        val deepLink = convert(deepLinkDto)

        val context = deepLink.context as VillageListDeepLinkContext

        val geoIntent = context.geoIntent as GeoIntent.Objects
        assertEquals(createExpectedRegion(), context.region)
        val expectedGeoObject = listOf(createExpectedGeoObject())
        assertEquals(expectedGeoObject, geoIntent.items)
    }

    @Test
    fun shouldConvertVillageListWithGeoPolygonWithoutRegion() {
        val deepLinkDto = createDeepLinkDto(
            action = "VILLAGE_LIST",
            params = listOf(
                ParamsItemDto(name = TYPE, values = listOf("SELL")),
                ParamsItemDto(name = CATEGORY, values = listOf("LOT"))
            ),
            mapPolygons = listOf(createTestGeoPolygonDto())
        )

        val deepLink = convert(deepLinkDto)

        val context = deepLink.context as VillageListDeepLinkContext

        val geoIntent = context.geoIntent as GeoIntent.Polygons
        assertNull(context.region)
        val expected = listOf(createExpectedGeoPolygon())
        assertEquals(expected, geoIntent.items)
    }

    @Test
    fun shouldConvertVillageListWithGeoPolygon() {
        val deepLinkDto = createDeepLinkDto(
            action = "VILLAGE_LIST",
            params = listOf(
                ParamsItemDto(name = TYPE, values = listOf("SELL")),
                ParamsItemDto(name = CATEGORY, values = listOf("LOT"))
            ),
            region = createTestRegionDto(),
            mapPolygons = listOf(createTestGeoPolygonDto())
        )

        val deepLink = convert(deepLinkDto)

        val context = deepLink.context as VillageListDeepLinkContext

        val geoIntent = context.geoIntent as GeoIntent.Polygons
        assertEquals(createExpectedRegion(), context.region)
        val expected = listOf(createExpectedGeoPolygon())
        assertEquals(expected, geoIntent.items)
    }

    @Test
    fun shouldConvertVillageListWithGeoSuggestOverGeoPolygon() {
        val deepLinkDto = createDeepLinkDto(
            action = "VILLAGE_LIST",
            params = listOf(
                ParamsItemDto(name = TYPE, values = listOf("RENT")),
                ParamsItemDto(name = CATEGORY, values = listOf("APARTMENT"))
            ),
            region = createTestRegionDto(),
            geoIntents = listOf(createTestGeoObjectDto()),
            mapPolygons = listOf(createTestGeoPolygonDto())
        )

        val deepLink = convert(deepLinkDto)

        val context = deepLink.context as VillageListDeepLinkContext

        val geoIntent = context.geoIntent as GeoIntent.Objects
        assertEquals(createExpectedRegion(), context.region)
        val expectedGeoObject = listOf(createExpectedGeoObject())
        assertEquals(expectedGeoObject, geoIntent.items)
    }

    @Test
    fun shouldConvertOfferMapWithRegion() {
        val deepLinkDto = createDeepLinkDto(
            action = "OFFER_MAP",
            params = listOf(
                ParamsItemDto(name = TYPE, values = listOf("RENT")),
                ParamsItemDto(name = CATEGORY, values = listOf("ROOMS"))
            ),
            region = createTestRegionDto()
        )

        val deepLink = convert(deepLinkDto)

        val context = deepLink.context as OfferMapDeepLinkContext

        val expectedRegion = createExpectedRegion()
        assertEquals(expectedRegion, context.region)
    }

    @Test
    fun shouldConvertOfferMapWithGeoSuggestWithoutRegion() {
        val deepLinkDto = createDeepLinkDto(
            action = "OFFER_MAP",
            params = listOf(
                ParamsItemDto(name = TYPE, values = listOf("SELL")),
                ParamsItemDto(name = CATEGORY, values = listOf("LOT"))
            ),
            geoIntents = listOf(createTestGeoObjectDto())
        )

        val deepLink = convert(deepLinkDto)

        val context = deepLink.context as OfferMapDeepLinkContext

        val geoIntent = context.geoIntent as GeoIntent.Objects
        assertNull(context.region)
        val expectedGeoObject = listOf(createExpectedGeoObject())
        assertEquals(expectedGeoObject, geoIntent.items)
    }

    @Test
    fun shouldConvertOfferMapWithGeoSuggest() {
        val deepLinkDto = createDeepLinkDto(
            action = "OFFER_MAP",
            params = listOf(
                ParamsItemDto(name = TYPE, values = listOf("RENT")),
                ParamsItemDto(name = CATEGORY, values = listOf("ROOMS"))
            ),
            region = createTestRegionDto(),
            geoIntents = listOf(createTestGeoObjectDto())
        )

        val deepLink = convert(deepLinkDto)

        val context = deepLink.context as OfferMapDeepLinkContext

        val geoIntent = context.geoIntent as GeoIntent.Objects
        assertEquals(createExpectedRegion(), context.region)
        val expectedGeoObject = listOf(createExpectedGeoObject())
        assertEquals(expectedGeoObject, geoIntent.items)
    }

    @Test
    fun shouldConvertOfferMapWithGeoPolygonWithoutRegion() {
        val deepLinkDto = createDeepLinkDto(
            action = "OFFER_MAP",
            params = listOf(
                ParamsItemDto(name = TYPE, values = listOf("SELL")),
                ParamsItemDto(name = CATEGORY, values = listOf("LOT"))
            ),
            mapPolygons = listOf(createTestGeoPolygonDto())
        )

        val deepLink = convert(deepLinkDto)

        val context = deepLink.context as OfferMapDeepLinkContext

        val geoIntent = context.geoIntent as GeoIntent.Polygons
        assertNull(context.region)
        val expected = listOf(createExpectedGeoPolygon())
        assertEquals(expected, geoIntent.items)
    }

    @Test
    fun shouldConvertOfferMapWithGeoPolygon() {
        val deepLinkDto = createDeepLinkDto(
            action = "OFFER_MAP",
            params = listOf(
                ParamsItemDto(name = TYPE, values = listOf("SELL")),
                ParamsItemDto(name = CATEGORY, values = listOf("LOT"))
            ),
            region = createTestRegionDto(),
            mapPolygons = listOf(createTestGeoPolygonDto())
        )

        val deepLink = convert(deepLinkDto)

        val context = deepLink.context as OfferMapDeepLinkContext

        val geoIntent = context.geoIntent as GeoIntent.Polygons
        assertEquals(createExpectedRegion(), context.region)
        val expected = listOf(createExpectedGeoPolygon())
        assertEquals(expected, geoIntent.items)
    }

    @Test
    fun shouldConvertOfferMapWithGeoSuggestOverGeoPolygon() {
        val deepLinkDto = createDeepLinkDto(
            action = "OFFER_MAP",
            params = listOf(
                ParamsItemDto(name = TYPE, values = listOf("RENT")),
                ParamsItemDto(name = CATEGORY, values = listOf("APARTMENT"))
            ),
            region = createTestRegionDto(),
            geoIntents = listOf(createTestGeoObjectDto()),
            mapPolygons = listOf(createTestGeoPolygonDto())
        )

        val deepLink = convert(deepLinkDto)

        val context = deepLink.context as OfferMapDeepLinkContext

        val geoIntent = context.geoIntent as GeoIntent.Objects
        assertEquals(createExpectedRegion(), context.region)
        val expectedGeoObject = listOf(createExpectedGeoObject())
        assertEquals(expectedGeoObject, geoIntent.items)
    }

    @Test
    fun shouldConvertSiteMapWithRegion() {
        val deepLinkDto = createDeepLinkDto(
            action = "SITE_MAP",
            region = createTestRegionDto()
        )

        val deepLink = convert(deepLinkDto)

        val context = deepLink.context as SiteMapDeepLinkContext

        val expectedRegion = createExpectedRegion()
        assertEquals(expectedRegion, context.region)
    }

    @Test
    fun shouldConvertSiteMapWithGeoSuggestWithoutRegion() {
        val deepLinkDto = createDeepLinkDto(
            action = "SITE_MAP",
            geoIntents = listOf(createTestGeoObjectDto())
        )

        val deepLink = convert(deepLinkDto)

        val context = deepLink.context as SiteMapDeepLinkContext

        val geoIntent = context.geoIntent as GeoIntent.Objects
        assertNull(context.region)
        val expectedGeoObject = listOf(createExpectedGeoObject())
        assertEquals(expectedGeoObject, geoIntent.items)
    }

    @Test
    fun shouldConvertSiteMapWithGeoSuggest() {
        val deepLinkDto = createDeepLinkDto(
            action = "SITE_MAP",
            region = createTestRegionDto(),
            geoIntents = listOf(createTestGeoObjectDto())
        )

        val deepLink = convert(deepLinkDto)

        val context = deepLink.context as SiteMapDeepLinkContext

        val geoIntent = context.geoIntent as GeoIntent.Objects
        assertEquals(createExpectedRegion(), context.region)
        val expectedGeoObject = listOf(createExpectedGeoObject())
        assertEquals(expectedGeoObject, geoIntent.items)
    }

    @Test
    fun shouldConvertSiteMapWithGeoPolygonWithoutRegion() {
        val deepLinkDto = createDeepLinkDto(
            action = "SITE_MAP",
            mapPolygons = listOf(createTestGeoPolygonDto())
        )

        val deepLink = convert(deepLinkDto)

        val context = deepLink.context as SiteMapDeepLinkContext

        val geoIntent = context.geoIntent as GeoIntent.Polygons
        assertNull(context.region)
        val expected = listOf(createExpectedGeoPolygon())
        assertEquals(expected, geoIntent.items)
    }

    @Test
    fun shouldConvertSiteMapWithGeoPolygon() {
        val deepLinkDto = createDeepLinkDto(
            action = "SITE_MAP",
            region = createTestRegionDto(),
            mapPolygons = listOf(createTestGeoPolygonDto())
        )

        val deepLink = convert(deepLinkDto)

        val context = deepLink.context as SiteMapDeepLinkContext

        val geoIntent = context.geoIntent as GeoIntent.Polygons
        assertEquals(createExpectedRegion(), context.region)
        val expected = listOf(createExpectedGeoPolygon())
        assertEquals(expected, geoIntent.items)
    }

    @Test
    fun shouldConvertSiteMapWithGeoSuggestOverGeoPolygon() {
        val deepLinkDto = createDeepLinkDto(
            action = "SITE_MAP",
            region = createTestRegionDto(),
            geoIntents = listOf(createTestGeoObjectDto()),
            mapPolygons = listOf(createTestGeoPolygonDto())
        )

        val deepLink = convert(deepLinkDto)

        val context = deepLink.context as SiteMapDeepLinkContext

        val geoIntent = context.geoIntent as GeoIntent.Objects
        assertEquals(createExpectedRegion(), context.region)
        val expectedGeoObject = listOf(createExpectedGeoObject())
        assertEquals(expectedGeoObject, geoIntent.items)
    }

    @Test
    fun shouldConvertVillageMapWithRegion() {
        val deepLinkDto = createDeepLinkDto(
            action = "VILLAGE_MAP",
            params = listOf(
                ParamsItemDto(name = TYPE, values = listOf("RENT")),
                ParamsItemDto(name = CATEGORY, values = listOf("ROOMS"))
            ),
            region = createTestRegionDto()
        )

        val deepLink = convert(deepLinkDto)

        val context = deepLink.context as VillageMapDeepLinkContext

        val expectedRegion = createExpectedRegion()
        assertEquals(expectedRegion, context.region)
    }

    @Test
    fun shouldConvertVillageMapWithGeoSuggestWithoutRegion() {
        val deepLinkDto = createDeepLinkDto(
            action = "VILLAGE_MAP",
            params = listOf(
                ParamsItemDto(name = TYPE, values = listOf("SELL")),
                ParamsItemDto(name = CATEGORY, values = listOf("LOT"))
            ),
            geoIntents = listOf(createTestGeoObjectDto())
        )

        val deepLink = convert(deepLinkDto)

        val context = deepLink.context as VillageMapDeepLinkContext

        val geoIntent = context.geoIntent as GeoIntent.Objects
        assertNull(context.region)
        val expectedGeoObject = listOf(createExpectedGeoObject())
        assertEquals(expectedGeoObject, geoIntent.items)
    }

    @Test
    fun shouldConvertVillageMapWithGeoSuggest() {
        val deepLinkDto = createDeepLinkDto(
            action = "VILLAGE_MAP",
            params = listOf(
                ParamsItemDto(name = TYPE, values = listOf("RENT")),
                ParamsItemDto(name = CATEGORY, values = listOf("ROOMS"))
            ),
            region = createTestRegionDto(),
            geoIntents = listOf(createTestGeoObjectDto())
        )

        val deepLink = convert(deepLinkDto)

        val context = deepLink.context as VillageMapDeepLinkContext

        val geoIntent = context.geoIntent as GeoIntent.Objects
        assertEquals(createExpectedRegion(), context.region)
        val expectedGeoObject = listOf(createExpectedGeoObject())
        assertEquals(expectedGeoObject, geoIntent.items)
    }

    @Test
    fun shouldIgnoreConvertVillageMapWithGeoPolygonWithoutRegion() {
        val deepLinkDto = createDeepLinkDto(
            action = "VILLAGE_MAP",
            params = listOf(
                ParamsItemDto(name = TYPE, values = listOf("SELL")),
                ParamsItemDto(name = CATEGORY, values = listOf("LOT"))
            ),
            mapPolygons = listOf(createTestGeoPolygonDto())
        )

        val deepLink = convert(deepLinkDto)

        val context = deepLink.context as VillageMapDeepLinkContext

        val geoIntent = context.geoIntent as GeoIntent.Polygons
        assertNull(context.region)
        val expected = listOf(createExpectedGeoPolygon())
        assertEquals(expected, geoIntent.items)
    }

    @Test
    fun shouldConvertVillageMapWithGeoPolygon() {
        val deepLinkDto = createDeepLinkDto(
            action = "VILLAGE_MAP",
            params = listOf(
                ParamsItemDto(name = TYPE, values = listOf("SELL")),
                ParamsItemDto(name = CATEGORY, values = listOf("LOT"))
            ),
            region = createTestRegionDto(),
            mapPolygons = listOf(createTestGeoPolygonDto())
        )

        val deepLink = convert(deepLinkDto)

        val context = deepLink.context as VillageMapDeepLinkContext

        val geoIntent = context.geoIntent as GeoIntent.Polygons
        assertEquals(createExpectedRegion(), context.region)
        val expected = listOf(createExpectedGeoPolygon())
        assertEquals(expected, geoIntent.items)
    }

    @Test
    fun shouldConvertVillageMapWithGeoSuggestOverGeoPolygon() {
        val deepLinkDto = createDeepLinkDto(
            action = "VILLAGE_MAP",
            params = listOf(
                ParamsItemDto(name = TYPE, values = listOf("RENT")),
                ParamsItemDto(name = CATEGORY, values = listOf("APARTMENT"))
            ),
            region = createTestRegionDto(),
            geoIntents = listOf(createTestGeoObjectDto()),
            mapPolygons = listOf(createTestGeoPolygonDto())
        )

        val deepLink = convert(deepLinkDto)

        val context = deepLink.context as VillageMapDeepLinkContext

        val geoIntent = context.geoIntent as GeoIntent.Objects
        assertEquals(createExpectedRegion(), context.region)
        val expectedGeoObject = listOf(createExpectedGeoObject())
        assertEquals(expectedGeoObject, geoIntent.items)
    }

    @Test
    fun shouldConvertOffersListWithBuyApartmentAndCommute() {
        val deepLinkDto = createDeepLinkDto(
            action = "OFFER_LIST",
            params = listOf(
                ParamsItemDto(name = TYPE, values = listOf("SELL")),
                ParamsItemDto(name = CATEGORY, values = listOf("APARTMENT"))
            ),
            region = createTestRegionDto(),
            commute = CommuteDto(
                commutePointLatitude = 10.0,
                commutePointLongitude = 11.0,
                commuteAddress = "test_commute_address",
                commuteTransport = "PUBLIC",
                commuteTime = 30
            )
        )

        val deepLink = convert(deepLinkDto)

        assertTrue(deepLink.context is OfferListDeepLinkContext)
        val context = deepLink.context as OfferListDeepLinkContext

        assertTrue(context.geoIntent is GeoIntent.Commute)
        val geoIntent = context.geoIntent as GeoIntent.Commute

        assertEquals("test_commute_address", geoIntent.commute.address)
        assertEquals(GeoPoint(10.0, 11.0), geoIntent.commute.point)
        assertEquals(CommuteParams.Transport.PUBLIC, geoIntent.commute.transport)
        assertEquals(30, geoIntent.commute.time)
    }

    @Test
    fun shouldSkipWhenInvalidCommuteTransport() {
        val deepLinkDto = createDeepLinkDto(
            action = "OFFER_LIST",
            params = listOf(
                ParamsItemDto(name = TYPE, values = listOf("SELL")),
                ParamsItemDto(name = CATEGORY, values = listOf("APARTMENT"))
            ),
            region = createTestRegionDto(),
            commute = CommuteDto(
                commutePointLatitude = 10.0,
                commutePointLongitude = 11.0,
                commuteAddress = "test_commute_address",
                commuteTransport = "INVALID_TRANSPORT",
                commuteTime = 20
            )
        )

        val deepLink = convert(deepLinkDto)

        assertTrue(deepLink.context is OfferListDeepLinkContext)
        val context = deepLink.context as OfferListDeepLinkContext

        assertTrue(context.geoIntent !is GeoIntent.Commute)
    }

    @Test
    fun shouldSkipWhenInvalidCommuteTime() {
        val deepLinkDto = createDeepLinkDto(
            action = "OFFER_LIST",
            params = listOf(
                ParamsItemDto(name = TYPE, values = listOf("SELL")),
                ParamsItemDto(name = CATEGORY, values = listOf("APARTMENT"))
            ),
            region = createTestRegionDto(),
            commute = CommuteDto(
                commutePointLatitude = 10.0,
                commutePointLongitude = 11.0,
                commuteAddress = "test_commute_address",
                commuteTransport = "PUBLIC",
                commuteTime = -100500
            )
        )

        val deepLink = convert(deepLinkDto)

        assertTrue(deepLink.context is OfferListDeepLinkContext)
        val context = deepLink.context as OfferListDeepLinkContext

        assertTrue(context.geoIntent !is GeoIntent.Commute)
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

    private fun createTestRegionDto(): GeoRegionDto {
        return GeoRegionDto(
            rgid = 1,
            type = "SUBJECT_FEDERATION",
            fullName = "test_region",
            shortName = "test_region",
            scope = "test_region_scope",
            point = GeoPointDto(10.0, 11.0),
            lt = GeoPointDto(9.0, 10.0),
            rb = GeoPointDto(11.0, 12.0),
            rgbColor = "550000",
            searchParams = mapOf("test_region_key" to listOf("test_region_value"))
        )
    }

    private fun createExpectedRegion(): GeoRegion {
        return GeoRegion(
            1,
            GeoObject(
                type = GeoType.SUBJECT_FEDERATION,
                fullName = "test_region",
                shortName = "test_region",
                scope = "test_region_scope",
                point = GeoPoint(10.0, 11.0),
                leftTop = GeoPoint(9.0, 10.0),
                rightBottom = GeoPoint(11.0, 12.0),
                colors = listOf(0xFF550000.toInt()),
                params = mapOf("test_region_key" to listOf("test_region_value"))
            )
        )
    }

    private fun createTestGeoObjectDto(): GeoObjectDto {
        return GeoObjectDto(
            fullName = "test_city",
            shortName = "test_city",
            type = "CITY",
            scope = "test_city_scope",
            point = GeoPointDto(10.3, 10.7),
            lt = GeoPointDto(10.7, 11.3),
            rb = GeoPointDto(9.3, 9.7),
            rgbColor = "88AAFFEE",
            searchParams = mapOf("test_city_key" to listOf("test_city_value"))
        )
    }

    private fun createExpectedGeoObject(): GeoObject {
        return GeoObject(
            type = GeoType.CITY,
            fullName = "test_city",
            shortName = "test_city",
            scope = "test_city_scope",
            point = GeoPoint(10.3, 10.7),
            leftTop = GeoPoint(10.7, 11.3),
            rightBottom = GeoPoint(9.3, 9.7),
            colors = listOf(0x88AAFFEE.toInt()),
            params = mapOf("test_city_key" to listOf("test_city_value"))
        )
    }

    private fun createTestGeoPolygonDto(): GeoPolygonDto {
        return GeoPolygonDto(
            latitudes = listOf(10.0, 10.0, 11.0, 11.0, 10.0),
            longitudes = listOf(10.0, 11.0, 11.0, 10.0, 10.0)
        )
    }

    private fun createExpectedGeoPolygon(): GeoPolygon {
        return GeoPolygon(
            listOf(
                GeoPoint(10.0, 10.0),
                GeoPoint(10.0, 11.0),
                GeoPoint(11.0, 11.0),
                GeoPoint(11.0, 10.0),
                GeoPoint(10.0, 10.0)
            )
        )
    }
}
