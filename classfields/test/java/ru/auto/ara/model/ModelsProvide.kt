package ru.auto.ara.model

import ru.auto.data.model.data.offer.Offer
import ru.auto.data.model.network.scala.offer.converter.OfferConverter
import ru.auto.data.network.scala.response.OfferListingResponse
import ru.auto.testextension.FileTestUtils

/**
 * @author dumchev on 05/12/2018.
 */
object ModelsProvide {

    private val nwOffersResponse = FileTestUtils.readJsonAsset(
        assetPath = "/assets/offers.json",
        classOfT = OfferListingResponse::class.java
    )

    val offers: List<Offer> = nwOffersResponse.offers
        ?.map { OfferConverter().fromNetwork(it) } as List<Offer>
}