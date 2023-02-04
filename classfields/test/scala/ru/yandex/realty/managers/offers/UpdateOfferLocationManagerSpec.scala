package ru.yandex.realty.managers.offers

import play.api.libs.json.{JsObject, JsValue, Json}
import ru.yandex.common.util.currency.Currency
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.clients.searcher.SearcherClient
import ru.yandex.realty.clients.searcher.SearcherResponseModel.{Author, GeoPoint, Location, Price, SearchCard}
import ru.yandex.realty.model.offer.{AreaUnit, CategoryType, OfferType, PricingPeriod, SalesAgentCategory}
import ru.yandex.realty.persistence.OfferId
import ru.yandex.realty.proto.unified.offer.state.OfferState
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty.util.Resource

import scala.concurrent.Future

class UpdateOfferLocationManagerSpec extends AsyncSpecBase {

  private val pathOfferWithCoordinates = "/transformers/offer.json"
  private val pathOfferWithoutCoordinates = "/locationUpdate/offerWithoutCoordinates.json"
  private val pathOffersWithCoordinates = "/locationUpdate/offers.json"
  private val pathOffersWithoutCoordinates = "/locationUpdate/offersWithoutCoordinates.json"
  private val price: Price = Price(
    currency = Currency.RUR,
    value = 1L,
    period = PricingPeriod.PER_DAY,
    unit = AreaUnit.WHOLE_OFFER,
    valuePerPart = None,
    unitPerPart = None
  )
  private val author: Author = Author(
    category = SalesAgentCategory.AGENT,
    phones = List.empty[String],
    agentName = None,
    organization = None
  )

  "UpdateOfferLocationManagerSpec" should {
    "no update needed if coordinates are in place" in {
      val searcherClient = mock[SearcherClient]
      val manager = new UpdateOfferLocationManager(searcherClient)
      val offer = getJson(pathOfferWithCoordinates)

      val updatedOffer = manager.updateOfferLocation(offer)(ec, Traced.empty).futureValue
      updatedOffer shouldBe offer
    }

    "update if coordinates are absent" in {
      val searcherClient = mock[SearcherClient]
      val manager = new UpdateOfferLocationManager(searcherClient)
      val offer = getJson(pathOfferWithoutCoordinates).as[JsObject]
      val id: OfferId = "5191937699526305025"

      val card: SearchCard = SearchCard(
        offerId = id,
        offerType = OfferType.SELL,
        offerCategory = CategoryType.LOT,
        location = Location(address = "address2", point = Some(GeoPoint(5.0f, 6.0f, "EXACT"))),
        price = price,
        author = author,
        active = true,
        offerState = OfferState.getDefaultInstance
      )

      (searcherClient
        .getCard(_: OfferId)(_: Traced))
        .expects(id, *)
        .returning(Future.successful(Some(card)))

      val result = manager.updateOfferLocation(offer)(ec, Traced.empty).futureValue
      val updatedOffer = (result \ "specific").asOpt[JsObject].get

      (updatedOffer \ "latitude").get shouldBe Json.toJson(5)
      (updatedOffer \ "longitude").get shouldBe Json.toJson(6)
    }

    "no update needed if coordinates are in place in all offers" in {
      val searcherClient = mock[SearcherClient]
      val manager = new UpdateOfferLocationManager(searcherClient)
      val offers = getJson(pathOffersWithCoordinates)

      val updatedOffers = manager.updateOffersLocation(offers)(ec, Traced.empty).futureValue
      updatedOffers shouldBe offers
    }

    "update offers with no coordinates" in {
      val searcherClient = mock[SearcherClient]
      val manager = new UpdateOfferLocationManager(searcherClient)
      val offersWithCoordinates = getJson(pathOffersWithCoordinates)
      val offersWithoutCoordinates = getJson(pathOffersWithoutCoordinates).as[JsObject]
      val oneCoordinateId: OfferId = "4841101740110335745"
      val noCoordinatesId: OfferId = "1103230881364047873"

      val oneCoordinateCard: SearchCard = SearchCard(
        offerId = oneCoordinateId,
        offerType = OfferType.SELL,
        offerCategory = CategoryType.LOT,
        location = Location(address = "address4", point = Some(GeoPoint(56.0f, 38.0f, "EXACT"))),
        price = price,
        author = author,
        active = true,
        offerState = OfferState.getDefaultInstance
      )

      val noCoordinatesCard: SearchCard = SearchCard(
        offerId = noCoordinatesId,
        offerType = OfferType.SELL,
        offerCategory = CategoryType.HOUSE,
        location = Location(address = "address3", point = Some(GeoPoint(55.0f, 37.0f, "EXACT"))),
        price = price,
        author = author,
        active = true,
        offerState = OfferState.getDefaultInstance
      )

      (searcherClient
        .getCard(_: OfferId)(_: Traced))
        .expects(oneCoordinateId, *)
        .returning(Future.successful(Some(oneCoordinateCard)))

      (searcherClient
        .getCard(_: OfferId)(_: Traced))
        .expects(noCoordinatesId, *)
        .returning(Future.successful(Some(noCoordinatesCard)))

      val result = manager.updateOffersLocation(offersWithoutCoordinates)(ec, Traced.empty).futureValue

      result shouldBe offersWithCoordinates
    }
  }

  def getJson(path: String): JsValue = {
    val jsonStr = Resource.fromClassPathToString(path)
    Json.parse(jsonStr)
  }
}
