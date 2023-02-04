package ru.yandex.realty.api.routes.v2.useroffers

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.SpecBase
import ru.yandex.realty.api.routes.v2.useroffers.OfferFromVosConverter.Components
import ru.yandex.realty.bunker.BunkerResources
import ru.yandex.realty.geo.RegionGraphTestComponents
import ru.yandex.realty.proto.Price
import ru.yandex.realty.proto.offer.SellPrice
import ru.yandex.realty.proto.offer.vos.Offer
import ru.yandex.realty.proto.offer.vos.Offer.{OfferInBuilding, SellOffer, VosOfferSource}
import ru.yandex.realty.proto.offer.vos.OfferResponse.VosOfferResponse

@RunWith(classOf[JUnitRunner])
class OfferFromVosConverterSpec extends SpecBase with RegionGraphTestComponents {

  val offerBuilder: VosOfferResponse.Builder = VosOfferResponse
    .newBuilder()
    .setContent(
      VosOfferSource.newBuilder
        .setSellOffer(
          SellOffer.newBuilder
            .setPrice(SellPrice.newBuilder.setPrice(Price.newBuilder.setValue(11L)))
        )
        .setOfferInBuilding(
          OfferInBuilding
            .newBuilder()
            .setLivingOffer(
              Offer.LivingOffer
                .newBuilder()
                .setRoomsOffer(Offer.RoomsOffer.newBuilder().addFloor(1).build())
                .build()
            )
        )
        .build()
    )

  "OfferFromVosConverterSpec" should {

    "convertCard if location is ok" in {
      offerBuilder.getContentBuilder.setLocation(
        Offer.Location
          .newBuilder()
          .setLongitude(56.11f)
          .setLatitude(34.6f)
      )

      val response = OfferFromVosConverter.convertCard(
        offerBuilder.build(),
        None,
        None,
        None,
        None,
        productsFeatureEnabled = false,
        Components(
          BunkerResources(Map.empty, Set.empty, Set.empty, Set.empty, Set.empty, Set.empty, Map.empty, 1),
          regionGraphProvider.get()
        )
      )

      response.getContent.hasVosLocation shouldBe true

      val point = response.getContent.getVosLocation.getPoint
      point.getLongitude shouldBe 56.11f
      point.getLatitude shouldBe 34.6f
      point.getDefined shouldBe true
    }

    "convertCard if location has default values" in {
      offerBuilder.getContentBuilder.setLocation(
        Offer.Location
          .newBuilder()
          .setLongitude(0)
          .setLatitude(0)
      )

      val response = OfferFromVosConverter.convertCard(
        offerBuilder.build(),
        None,
        None,
        None,
        None,
        productsFeatureEnabled = false,
        Components(
          BunkerResources(Map.empty, Set.empty, Set.empty, Set.empty, Set.empty, Set.empty, Map.empty, 1),
          regionGraphProvider.get()
        )
      )

      response.getContent.hasVosLocation shouldBe true

      val point = response.getContent.getVosLocation.getPoint
      point.getLatitude shouldBe 0.0
      point.getLongitude shouldBe 0.0
      point.getDefined shouldBe false
    }
  }
}
