package com.edadeal.android.data

import com.edadeal.android.dto.Promo
import com.edadeal.android.model.ads.AdContent
import com.edadeal.android.model.ads.AdItem
import com.edadeal.android.model.ads.AdScreenConfig
import com.edadeal.android.model.entity.OfferEntity
import com.edadeal.android.model.entity.Retailer
import com.edadeal.android.model.toUuidByteString
import com.edadeal.android.model.toUuidString
import com.edadeal.android.util.fromJsonSafely
import com.edadeal.android.util.setupMoshi
import com.squareup.moshi.Moshi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PromoTest {

    @Test
    fun `ad should be ok in offer details`() {
        val id = "a1ef256d-393a-48ff-ba58-cd0d43a1b54f".toUuidByteString()
        val brandId = "c0ef256d-393a-48ff-ba58-cd0d43a1b54f".toUuidByteString()
        val retailerId = "f0ff256d-393a-48ff-ba58-cd0d43a1b54f".toUuidByteString()
        val compilationId = "b0ef256d-393a-48ff-ba58-cd0d43a1b54f".toUuidByteString()
        val offer = OfferEntity.EMPTY.copy(
            brandIds = listOf(brandId),
            compilationIds = listOf(compilationId),
            retailer = Retailer.EMPTY.copy(id = retailerId)
        )
        val specialOffer = OfferEntity.EMPTY.copy(
            id = id,
            brandIds = listOf(brandId),
            compilationIds = listOf(compilationId),
            retailer = Retailer.EMPTY.copy(id = retailerId)
        )
        val homeBanner = BannerBuilder(Promo.LayoutType.banner, Promo.Screen.main).build()
        val offerDetailsBanner = BannerBuilder(Promo.LayoutType.banner, Promo.Screen.offer)
            .withConditionsHaving(
                itemsCount = Promo.Having.ItemsCount(min = 1),
                brands = Promo.IncludeExclude(include = listOf(brandId.toUuidString()))
            )
            .withPositionIn(
                retailers = Promo.IncludeExclude(include = listOf(retailerId.toUuidString())),
                compilations = Promo.IncludeExclude(include = listOf(compilationId.toUuidString()))
            )
            .build()
        val specialOfferDetailsBanner = BannerBuilder(offerDetailsBanner)
            .withPositionIn(
                offers = Promo.IncludeExclude(include = listOf(id.toUuidString()))
            )
            .build()
        val screenConfig = AdScreenConfig.OfferDetails(offer, "", "", "", "")
        val specialScreenConfig = AdScreenConfig.OfferDetails(specialOffer, "", "", "", "")

        with(assertNotNull(adItemFrom(specialOfferDetailsBanner))) {
            assertTrue(specialScreenConfig.isOk(this, itemsCount = 1))
            assertFalse(screenConfig.isOk(this, itemsCount = 1))
        }
        with(assertNotNull(adItemFrom(offerDetailsBanner))) {
            assertFalse(specialScreenConfig.isOk(this, itemsCount = 0))
            assertFalse(screenConfig.isOk(this, itemsCount = 0))
            assertTrue(specialScreenConfig.isOk(this, itemsCount = 1))
            assertTrue(screenConfig.isOk(this, itemsCount = 1))
        }
        with(assertNotNull(adItemFrom(homeBanner))) {
            assertFalse(specialScreenConfig.isOk(this, itemsCount = 1))
            assertFalse(screenConfig.isOk(this, itemsCount = 1))
        }
    }

    @Test
    fun `ad should be ok in retailers and compilations`() {
        val retailerId = "f0ef256d-393a-48ff-ba58-cd0d43a1b54f".toUuidByteString()
        val compilationId = "a0ef256d-393a-48ff-ba58-cd0d43a1b54f".toUuidByteString()
        val homeBanner = BannerBuilder(Promo.LayoutType.banner, Promo.Screen.main).build()
        val offersBanner = BannerBuilder(Promo.LayoutType.banner, Promo.Screen.compilation)
            .withPositionIn(
                retailers = Promo.IncludeExclude(include = listOf(retailerId.toUuidString())),
                compilations = Promo.IncludeExclude(include = listOf(compilationId.toUuidString()))
            )
            .build()
        val mainScreenConfig = AdScreenConfig.Main("")
        val offersScreenConfig = AdScreenConfig.Offers(retailerId, compilationId, "", "", "", "")

        with(assertNotNull(adItemFrom(offersBanner))) {
            assertTrue(offersScreenConfig.isOk(this, itemsCount = 0))
            assertFalse(mainScreenConfig.isOk(this, itemsCount = 0))
        }
        with(assertNotNull(adItemFrom(homeBanner))) {
            assertFalse(offersScreenConfig.isOk(this, itemsCount = 0))
            assertTrue(mainScreenConfig.isOk(this, itemsCount = 0))
        }
    }

    @Test
    @Suppress("Detekt.LongMethod")
    fun `moshi should parse correct promo json`() {
        val json = """{
    "banners": [
        {
            "uuid":"5a61d329e9939a882aaf792e",
            "slug":"banner_slug",
            "position": {
                "screen": "main",
                "offset": 50,
                "ordernum": 2,
                "dynamicOffset": true,
                "in": {
                    "retailers": { "include": [ "includeRetailers" ], "exclude": [ "excludeRetailers" ] },
                    "compilations": { "include": [ "includeCompilations" ] },
                    "offers": { "include": [], "exclude": [] }
                }
            },
            "conditions": {
                "having": {
                    "brands": { "include": [ "includeBrands" ], "exclude": [ "excludeBrands" ] },
                    "itemsCount": { "min": 1, "max": 100 }
                }
            },
            "counters": {
                "slotViewDelay": 1000,
                "bannerViewDelay": 1000,
                "view": [
                    "https://ads.edadeal.ru/v1/telemetry?action=view&slug=banner_slug&distinctId={{DISTINCT_ID}}"
                ],
                "click": [
                    "https://ads.edadeal.ru/v1/telemetry?action=click&slug=banner_slug&distinctId={{DISTINCT_ID}}"
                ]
            },
            "layout":{
                "type":"banner",
                "inventoryTitle": "title",
                "slots":[
                    {
                        "images":{
                            "hdpi": "https://ads.edadev.ru/media/5a61d329e9939a882aaf792e/banner.png",
                            "xhdpi": "https://ads.edadev.ru/media/5a61d329e9939a882aaf792e/banner2x.png",
                            "xxhdpi": "https://ads.edadev.ru/media/5a61d329e9939a882aaf792e/banner3x.png",
                            "xxxhdpi": "https://ads.edadev.ru/media/5a61d329e9939a882aaf792e/banner4x.png"
                        },
                        "deeplink": "edadeal://map"
                    }
                ],
                "payload": { "templates": {} }
            }
        }
    ],
    "pinups": [
        {
            "uuid": "3b6d5813-6311-11e6-849f-52540010b608",
            "slug": "lowenbrau",
            "ordernum": 1
        }
    ],
    "direct": {
        "backgroundColor": "B3218A",
         "keywords": [ "тест" ]
    }
}""".replace("\r", "").replace("\n", "")
        val moshi = Moshi.Builder()
            .setupMoshi()
            .build()
        with(moshi.fromJsonSafely<Promo>(json) ?: Promo()) {
            assertEquals(1, banners.size)
            assertEquals("5a61d329e9939a882aaf792e", banners[0].uuid)
            assertEquals("banner_slug", banners[0].slug)
            assertEquals(Promo.Screen.main, banners[0].position.screen)
            assertEquals(50, banners[0].position.offset)
            assertEquals(2, banners[0].position.ordernum)
            assertEquals(true, banners[0].position.dynamicOffset)
            assertEquals(listOf("includeRetailers"), banners[0].position.`in`.retailers.include)
            assertEquals(listOf("excludeRetailers"), banners[0].position.`in`.retailers.exclude)
            assertEquals(listOf("includeCompilations"), banners[0].position.`in`.compilations.include)
            assertEquals(emptyList(), banners[0].position.`in`.compilations.exclude)
            assertEquals(emptyList(), banners[0].position.`in`.offers.include)
            assertEquals(listOf("includeBrands"), banners[0].conditions.having.brands.include)
            assertEquals(listOf("excludeBrands"), banners[0].conditions.having.brands.exclude)
            assertEquals(1, banners[0].conditions.having.itemsCount.min)
            assertEquals(100, banners[0].conditions.having.itemsCount.max)
            assertEquals(1000, banners[0].counters.slotViewDelay)
            assertEquals(1000, banners[0].counters.bannerViewDelay)
            assertEquals(
                listOf("https://ads.edadeal.ru/v1/telemetry?action=view&slug=banner_slug&distinctId={{DISTINCT_ID}}"),
                banners[0].counters.view
            )
            assertEquals(
                listOf("https://ads.edadeal.ru/v1/telemetry?action=click&slug=banner_slug&distinctId={{DISTINCT_ID}}"),
                banners[0].counters.click
            )
            assertEquals(Promo.LayoutType.banner, banners[0].layout.type)
            assertEquals("title", banners[0].layout.inventoryTitle)
            assertEquals("edadeal://map", banners[0].layout.slots[0].deeplink)
            assertEquals(
                "https://ads.edadev.ru/media/5a61d329e9939a882aaf792e/banner3x.png",
                banners[0].layout.slots[0].images["xxhdpi"]
            )
            assertEquals("{ \"templates\": {} }", banners[0].layout.payload?.toJson()?.trim())
        }
    }

    private var nextBannerId = 0

    private inner class BannerBuilder(layoutType: Promo.LayoutType, screen: Promo.Screen) {
        private val uuid = "${++nextBannerId}"
        private var position = Promo.Position(screen = screen)
        private var counters = Promo.Counters()
        private var conditions = Promo.Conditions()
        private var layout = Promo.Layout(type = layoutType, slots = listOf(Promo.Slot()))

        constructor(banner: Promo.Banner) : this(banner.layout.type, banner.position.screen) {
            layout = banner.layout
            counters = banner.counters
            position = banner.position
            conditions = banner.conditions
        }

        fun withConditionsHaving(
            brands: Promo.IncludeExclude = Promo.IncludeExclude(),
            itemsCount: Promo.Having.ItemsCount = Promo.Having.ItemsCount()
        ): BannerBuilder = apply {
            conditions = Promo.Conditions(
                having = Promo.Having(
                    brands = brands,
                    itemsCount = itemsCount
                )
            )
        }

        fun withPositionIn(
            offers: Promo.IncludeExclude = Promo.IncludeExclude(),
            retailers: Promo.IncludeExclude = Promo.IncludeExclude(),
            compilations: Promo.IncludeExclude = Promo.IncludeExclude()
        ): BannerBuilder = apply {
            position = Promo.Position(
                screen = position.screen,
                `in` = Promo.Position.In(offers, retailers, compilations)
            )
        }

        fun build() = Promo.Banner(
            uuid = uuid,
            slug = "slug_$uuid",
            layout = layout,
            position = position,
            counters = counters,
            conditions = conditions
        )
    }

    private fun adItemFrom(banner: Promo.Banner): AdItem? = AdItem.from(
        AdContent.CreationContext(Promo.Direct(), emptyMap()),
        banner
    )
}
