package ru.auto.ara.presentation.presenter.vas

import io.kotest.core.spec.style.DescribeSpec
import ru.auto.data.factory.vas.VASCatchFactoryProducer
import ru.auto.data.model.data.offer.Offer
import ru.auto.data.model.network.scala.offer.converter.OfferConverter
import ru.auto.data.model.vas.EventWithOffer
import ru.auto.data.model.vas.VASComparableItem
import ru.auto.data.network.scala.response.OfferListingResponse
import ru.auto.testextension.FileTestUtils

/**
 * @author dumchev on 1/16/19.
 */
class VASCatchFactoryProducerTest : DescribeSpec({

    describe("VASCatchFactoryProducer") {

        val nwOffersResponse = FileTestUtils.readJsonAsset(
            assetPath = "/assets/offers.json",
            classOfT = OfferListingResponse::class.java
        )

        val offers = nwOffersResponse.offers?.map { OfferConverter().fromNetwork(it) } as List<Offer>
        val firstOffer = offers[0]

        fun getItemsAfterEvent(
            event: EventWithOffer,
            offer: Offer = firstOffer,
            filterCond: (Any) -> Boolean = { true }
        ): List<VASComparableItem> {
            val producer = VASCatchFactoryProducer(event, offer)
            val factory = producer.getFactory()
            return factory.build().filter(filterCond)
        }

        context("check scheme") {
            context("animation items count") {
                it("after activation") {
                    val animationItems = getItemsAfterEvent(EventWithOffer.ACTIVATE) { it is VASComparableItem.TopWithAnimation }
                    check(animationItems.isEmpty()) { "There should be zero animation items after activation" }
                }
                it("after adding") {
                    val animationItems = getItemsAfterEvent(EventWithOffer.ADD) { it is VASComparableItem.TopWithAnimation }
                    check(animationItems.size == 1) { "There should be 1 animation item after adding" }
                }
                it("after editing") {
                    val animationItems = getItemsAfterEvent(EventWithOffer.EDIT) { it is VASComparableItem.TopWithAnimation }
                    check(animationItems.size == 1) { "There should be 1 animation item after editing" }
                }
            }

            context("show animation when out-of-scheme-vas has bigger priority") {
                val offerWithUnknownServiceThatHasBigPriority = firstOffer.copy(
                    servicePrices = firstOffer.servicePrices.map {
                        if (it.serviceId == "certification_mobile") it.copy(priority = Integer.MAX_VALUE) else it
                    }
                )
                val animationItems = getItemsAfterEvent(
                    EventWithOffer.ADD,
                    offerWithUnknownServiceThatHasBigPriority
                ) { it is VASComparableItem.TopWithAnimation }
                check(animationItems.size == 1) { "There should be 1 animation item after adding" }
            }
        }
    }
})
