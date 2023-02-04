package ru.yandex.realty.managers.useroffers

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.common.util.currency.Currency
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.clients.billing.BillingRequestContextResolver
import ru.yandex.realty.clients.searcher.SearcherClient
import ru.yandex.realty.clients.searcher.SearcherResponseModel.{Author, GeoPoint, Location, Price, SearchCard}
import ru.yandex.realty.clients.statistics.RawStatisticsClient
import ru.yandex.realty.clients.vos.ng.VosClientNG
import ru.yandex.realty.features.FeaturesStubComponent
import ru.yandex.realty.managers.cadastr.ExcerptManager
import ru.yandex.realty.managers.offers.UpdateOfferLocationManager
import ru.yandex.realty.managers.products.ProductManager
import ru.yandex.realty.model.offer.{AreaUnit, CategoryType, OfferType, PricingPeriod, SalesAgentCategory}
import ru.yandex.realty.model.user.UserRef
import ru.yandex.realty.persistence.OfferId
import ru.yandex.realty.proto.offer.vos.OfferResponse.VosOfferResponse
import ru.yandex.realty.proto.unified.offer.state.OfferState
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty.vos.model.user.User

import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class DefaultUserOffersManagerSpec extends AsyncSpecBase with FeaturesStubComponent {

  "DefaultUserOffersManagerSpec" should {
    "getCard if vos response has coordinates " in new DefaultUserOffersManagerFixture() {
      val builder: VosOfferResponse.Builder = VosOfferResponse.newBuilder()
      builder.getContentBuilder.getLocationBuilder
        .setLongitude(2.0f)
        .setLatitude(1.0f)
      val offer: VosOfferResponse = builder.build()

      (vos
        .getOffer(_: String, _: OfferId)(_: Traced))
        .expects(*, offerId, *)
        .returning(Future.successful(Some(offer)))

      val cardData: UserOffersManager.GetCardData =
        manager.getCard(userRef, offerId)(Traced.empty, Set.empty, Set.empty).futureValue

      cardData.vosResponse.getContent.getLocation.getLatitude shouldBe 1.0f
      cardData.vosResponse.getContent.getLocation.getLongitude shouldBe 2.0f
    }

    "getCard if vos response has no coordinates and searcher response has coordinates" in
      new DefaultUserOffersManagerFixture() {
        val builder: VosOfferResponse.Builder = VosOfferResponse.newBuilder()
        builder.getContentBuilder.setId(offerId)
        val offer: VosOfferResponse = builder.build()

        (vos
          .getOffer(_: String, _: OfferId)(_: Traced))
          .expects(*, offerId, *)
          .returning(Future.successful(Some(offer)))

        val card: SearchCard = SearchCard(
          offerId = offerId,
          offerType = OfferType.SELL,
          offerCategory = CategoryType.LOT,
          location = Location(address = "address1", point = Some(GeoPoint(3.0f, 4.0f, "EXACT"))),
          price = price,
          author = author,
          active = true,
          offerState = OfferState.getDefaultInstance
        )

        (searcherClient
          .getCard(_: OfferId)(_: Traced))
          .expects(offerId, *)
          .returning(Future.successful(Some(card)))

        val cardData: UserOffersManager.GetCardData =
          manager.getCard(userRef, offerId)(Traced.empty, Set.empty, Set.empty).futureValue

        cardData.vosResponse.getContent.getLocation.getLatitude shouldBe 3.0f
        cardData.vosResponse.getContent.getLocation.getLongitude shouldBe 4.0f
      }

    "getCard if vos response has no coordinates and searcher response has no coordinates" in
      new DefaultUserOffersManagerFixture() {
        val builder: VosOfferResponse.Builder = VosOfferResponse.newBuilder()
        builder.getContentBuilder.setId(offerId)
        val offer: VosOfferResponse = builder.build()

        (vos
          .getOffer(_: String, _: OfferId)(_: Traced))
          .expects(*, offerId, *)
          .returning(Future.successful(Some(offer)))

        val card: SearchCard = SearchCard(
          offerId = offerId,
          offerType = OfferType.SELL,
          offerCategory = CategoryType.LOT,
          location = Location(address = "address2"),
          price = price,
          author = author,
          active = true,
          offerState = OfferState.getDefaultInstance
        )

        (searcherClient
          .getCard(_: OfferId)(_: Traced))
          .expects(offerId, *)
          .returning(Future.successful(Some(card)))

        val cardData: UserOffersManager.GetCardData =
          manager.getCard(userRef, offerId)(Traced.empty, Set.empty, Set.empty).futureValue

        cardData.vosResponse.getContent.getLocation.getLatitude shouldBe 0.0f
        cardData.vosResponse.getContent.getLocation.getLongitude shouldBe 0.0f
      }
  }

  trait DefaultUserOffersManagerFixture {
    val rawStatisticsClient: RawStatisticsClient = mock[RawStatisticsClient]
    val productManager: ProductManager = mock[ProductManager]
    val excerptManager: ExcerptManager = mock[ExcerptManager]
    val billingRequestContextResolver: BillingRequestContextResolver = mock[BillingRequestContextResolver]
    val vos: VosClientNG = mock[VosClientNG]
    val searcherClient: SearcherClient = mock[SearcherClient]
    val userRef: UserRef = UserRef.web("yandeUid")
    val user: User = User.getDefaultInstance
    val offerId: OfferId = "offerId"

    (vos
      .getUser(_: String, _: Boolean, _: Iterable[User.Feature])(_: Traced))
      .expects(*, *, *, *)
      .returning(Future.successful(Some(user)))

    val price: Price = Price(
      currency = Currency.RUR,
      value = 1L,
      period = PricingPeriod.PER_DAY,
      unit = AreaUnit.WHOLE_OFFER,
      valuePerPart = None,
      unitPerPart = None
    )

    val author: Author = Author(
      category = SalesAgentCategory.AGENT,
      phones = List.empty[String],
      agentName = None,
      organization = None
    )

    val manager = new DefaultUserOffersManager(
      vos,
      rawStatisticsClient,
      new UpdateOfferLocationManager(searcherClient),
      productManager,
      excerptManager,
      billingRequestContextResolver,
      features
    )
  }
}
