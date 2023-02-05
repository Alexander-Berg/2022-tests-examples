package ru.yandex.market.clean.data.mapper

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.yandex.market.domain.product.model.offer.OfferFeature
import ru.yandex.market.data.searchitem.offer.DeliveryDto
import ru.yandex.market.data.searchitem.offer.DeliveryPartnerTypeDto
import ru.yandex.market.data.searchitem.offer.OfferInfo
import ru.yandex.market.data.searchitem.offer.offerInfoTestInstance

@RunWith(Parameterized::class)
class OfferFeaturesMapperTest(
    private val offer: OfferInfo,
    private val expectedFeatures: Set<OfferFeature>
) {
    private val mapper = OfferFeaturesMapper(configuration)

    @Test
    fun `Check mapped features match expectations`() {
        val mappedFeatures = mapper.map(offer)
        assertThat(mappedFeatures).isEqualTo(expectedFeatures)
    }

    companion object {

        private val configuration = OfferFeaturesMapper.Configuration(cargoTypeBulky = "300", cargoTypeBulkyInt = 300)

        @Parameterized.Parameters(name = "{index}: expected features: {1}")
        @JvmStatic
        fun data() = listOf<Array<*>>(

            // 0
            arrayOf(
                offerInfoTestInstance(isFulfillment = true, cargoTypes = emptyList(), cpa = "true", isResale = false),
                setOf(OfferFeature.EATS_RETAIL),
            ),

            // 1
            arrayOf(
                offerInfoTestInstance(
                    isFulfillment = false,
                    cpa = "true",
                    delivery = DeliveryDto.testBuilder()
                        .deliveryPartnerTypes(listOf(DeliveryPartnerTypeDto.YANDEX_MARKET))
                        .build(),
                    cargoTypes = emptyList(),
                    isResale = false,
                ),
                setOf(OfferFeature.EATS_RETAIL),
            ),

            // 2
            arrayOf(
                offerInfoTestInstance(
                    isFulfillment = null,
                    cpa = "true",
                    delivery = DeliveryDto.testBuilder()
                        .deliveryPartnerTypes(listOf(DeliveryPartnerTypeDto.YANDEX_MARKET))
                        .build(),
                    cargoTypes = emptyList()
                ),
                setOf(OfferFeature.EATS_RETAIL, OfferFeature.RESALE_GOODS),
            ),

            // 3
            arrayOf(
                offerInfoTestInstance(
                    isFulfillment = false,
                    cpa = "true",
                    delivery = DeliveryDto.testBuilder()
                        .deliveryPartnerTypes(listOf(DeliveryPartnerTypeDto.SHOP))
                        .build(),
                    cargoTypes = emptyList(),
                    offerColor = OfferColorMapper.OFFER_COLOR_BLUE,
                    isResale = false,
                ),
                setOf(
                    OfferFeature.EATS_RETAIL,
                    OfferFeature.CLICK_AND_COLLECT,
                )
            ),

            // 4
            arrayOf(
                offerInfoTestInstance(
                    isFulfillment = null,
                    cpa = "true",
                    delivery = DeliveryDto.testBuilder()
                        .deliveryPartnerTypes(listOf(DeliveryPartnerTypeDto.SHOP))
                        .build(),
                    cargoTypes = emptyList(),
                    offerColor = OfferColorMapper.OFFER_COLOR_BLUE,
                    isResale = false,
                ),
                setOf(
                    OfferFeature.EATS_RETAIL,
                    OfferFeature.CLICK_AND_COLLECT,
                )
            ),

            // 5
            arrayOf(
                offerInfoTestInstance(
                    isFulfillment = null,
                    cpa = "true",
                    delivery = DeliveryDto.testBuilder()
                        .deliveryPartnerTypes(emptyList())
                        .build(),
                    cargoTypes = emptyList()
                ),
                setOf(OfferFeature.EATS_RETAIL, OfferFeature.RESALE_GOODS)
            ),

            // 6
            arrayOf(
                offerInfoTestInstance(
                    isFulfillment = false,
                    cpa = "true",
                    delivery = DeliveryDto.testBuilder()
                        .deliveryPartnerTypes(emptyList())
                        .build(),
                    cargoTypes = emptyList()
                ),
                setOf(OfferFeature.EATS_RETAIL, OfferFeature.RESALE_GOODS)
            ),

            // 7
            arrayOf(
                offerInfoTestInstance(
                    isFulfillment = true,
                    cpa = "true",
                    cargoTypes = listOf(configuration.cargoTypeBulky),
                    isResale = false,
                ),
                setOf(
                    OfferFeature.EATS_RETAIL,
                    OfferFeature.BULKY,
                )
            ),

            // 8
            arrayOf(
                offerInfoTestInstance(
                    isFulfillment = false,
                    cpa = "true",
                    delivery = DeliveryDto.testBuilder()
                        .deliveryPartnerTypes(
                            listOf(
                                DeliveryPartnerTypeDto.SHOP,
                                DeliveryPartnerTypeDto.YANDEX_MARKET
                            )
                        )
                        .build(),
                    cargoTypes = listOf(configuration.cargoTypeBulky),
                    offerColor = OfferColorMapper.OFFER_COLOR_BLUE,
                ),
                setOf(
                    OfferFeature.EATS_RETAIL,
                    OfferFeature.CLICK_AND_COLLECT,
                    OfferFeature.BULKY,
                    OfferFeature.RESALE_GOODS,
                )
            )
        )
    }
}
