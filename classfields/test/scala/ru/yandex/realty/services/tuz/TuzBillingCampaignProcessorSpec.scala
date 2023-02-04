package ru.yandex.realty.services.tuz

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.extdata.core.lego.Provider
import ru.yandex.realty.{AsyncSpecBase, SpecBase}
import ru.yandex.realty.clients.abram.{Tariff, Tariffs}
import ru.yandex.realty.clients.seller.SellerClient
import ru.yandex.realty.features.SimpleFeatures
import ru.yandex.realty.model.serialization.MockOfferBuilder
import ru.yandex.realty.proto.api.v2.tariff.TariffType.TariffCase
import ru.yandex.realty.proto.seller.{
  CallsExtended,
  CallsMaximum,
  CallsMinimum,
  ListingExtended,
  ListingMaximum,
  ListingMinimum
}
import ru.yandex.realty.seller.proto.api.tariff.TariffResponse
import ru.yandex.realty.storage.CampaignHeadersStorage
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty.unification.OfferWrapper
import ru.yandex.vertis.billing.Model.Cost.PerCall
import ru.yandex.vertis.billing.Model.{
  CampaignHeader,
  CampaignSettings,
  Cost,
  CustomerHeader,
  CustomerId,
  Good,
  Order,
  Product,
  ResourceRef,
  User
}
import ru.yandex.realty.util.Mappings.MapAny

import scala.concurrent.Future

/**
  * @author azakharov
  */
@RunWith(classOf[JUnitRunner])
class TuzBillingCampaignProcessorSpec extends AsyncSpecBase {

  "TuzBillingCampaignProcessor" should {
    "not set tuz info if there is no campaign headers" in new TestCase {
      expectCampaignHeaders(emptyStorage)
      expectTariffCall(Tariffs.ListingMinimum)
      processor.process(offerWrapper).futureValue
      offer.getTuzInfo.getTariffType.hasListingMinimum shouldBe true
    }

    "set tuz info with calls minimum type for feed offer" in new TestCase {
      expectCampaignHeaders(minimumStorage)
      expectTariffCall(Tariffs.CallsMinimum)
      offer.setFromVos(false)
      processor.process(offerWrapper).futureValue
      offer.getTuzInfo shouldNot be(null)
      offer.getTuzInfo.getTariffType.getTariffCase shouldBe TariffCase.CALLS_MINIMUM
      offer.getTuzInfo.getActive shouldBe (true)
    }

    "set tuz info with calls minimum type for vos offer" in new TestCase {
      expectCampaignHeaders(minimumStorage)
      expectTariffCall(Tariffs.CallsMinimum)
      offer.setFromVos(true)
      processor.process(offerWrapper).futureValue
      offer.getTuzInfo shouldNot be(null)
      offer.getTuzInfo.getTariffType.getTariffCase shouldBe TariffCase.CALLS_MINIMUM
      offer.getTuzInfo.getActive shouldBe (true)
    }

    "set tuz info with calls extended type for feed offer" in new TestCase {
      expectCampaignHeaders(extendedStorage)
      expectTariffCall(Tariffs.CallsExtended)
      offer.setFromVos(false)
      processor.process(offerWrapper).futureValue
      offer.getTuzInfo shouldNot be(null)
      offer.getTuzInfo.getTariffType.getTariffCase shouldBe TariffCase.CALLS_EXTENDED
      offer.getTuzInfo.getActive shouldBe (true)
    }

    "set tuz info with calls extended type for vos offer" in new TestCase {
      expectCampaignHeaders(extendedStorage)
      expectTariffCall(Tariffs.CallsExtended)
      offer.setFromVos(true)
      processor.process(offerWrapper).futureValue
      offer.getTuzInfo shouldNot be(null)
      offer.getTuzInfo.getTariffType.getTariffCase shouldBe TariffCase.CALLS_EXTENDED
      offer.getTuzInfo.getActive shouldBe (true)
    }

    "set tuz info with calls maximum type for feed offer" in new TestCase {
      expectCampaignHeaders(maximumStorage)
      expectTariffCall(Tariffs.CallsMaximum)
      offer.setFromVos(false)
      processor.process(offerWrapper).futureValue
      offer.getTuzInfo shouldNot be(null)
      offer.getTuzInfo.getTariffType.getTariffCase shouldBe TariffCase.CALLS_MAXIMUM
      offer.getTuzInfo.getActive shouldBe (true)
    }

    "set tuz info with calls maximum type for vos offer" in new TestCase {
      expectCampaignHeaders(maximumStorage)
      expectTariffCall(Tariffs.CallsMaximum)
      offer.setFromVos(true)
      processor.process(offerWrapper).futureValue
      offer.getTuzInfo shouldNot be(null)
      offer.getTuzInfo.getTariffType.getTariffCase shouldBe TariffCase.CALLS_MAXIMUM
      offer.getTuzInfo.getActive shouldBe (true)
    }
  }

  trait Wiring {

    val campaignHeadersProvider =
      mock[Provider[CampaignHeadersStorage]]

    val sellerClient =
      mock[SellerClient]

    val processor = new TuzBillingCampaignProcessor(
      campaignHeadersProvider,
      sellerClient,
      new SimpleFeatures()
    )

    implicit val trace: Traced = Traced.empty
  }

  trait Data {
    self: Wiring =>

    val clientId = 1567855L
    val uid1 = 22222L
    val partnerId1 = 179234567L

    val offer = MockOfferBuilder.createMockOffer
    offer.setUid(uid1.toString)
    offer.setPartnerId(partnerId1)
    val offerWrapper = new OfferWrapper(null, offer, null)

    val minimumCh = CampaignHeader
      .newBuilder()
      .setVersion(1)
      .setName("Только уникальные звонки")
      .setId("bc708fdf-fb89-4cc5-b946-eadfffa59657")
      .setProduct(
        Product.newBuilder
          .setVersion(1)
          .addGoods(
            Good
              .newBuilder()
              .setVersion(1)
              .setCustom(
                Good.Custom
                  .newBuilder()
                  .setId(Tariffs.CallsMinimum.nameCampaign)
                  .setCost(Cost.newBuilder().setVersion(1).setPerCall(PerCall.newBuilder().setUnits(1)))
              )
          )
      )
      .setOrder(
        Order
          .newBuilder()
          .setVersion(1)
          .setId(309180)
          .setOwner(CustomerId.newBuilder().setVersion(1).setClientId(clientId))
          .setText("Яндекс.Комм.Недвижимость, Тест")
          .setCommitAmount(9878000)
          .setApproximateAmount(9878000)
      )
      .setOwner(
        CustomerHeader
          .newBuilder()
          .setVersion(1)
          .setId(CustomerId.newBuilder().setVersion(1).setClientId(clientId))
          .addResourceRef(
            ResourceRef
              .newBuilder()
              .setVersion(1)
              .setUser(User.newBuilder().setVersion(1).setUid(uid1))
          )
          .addResourceRef(
            ResourceRef
              .newBuilder()
              .setVersion(1)
              .setCapaPartnerId(partnerId1.toString)
          )
      )
      .setSettings(CampaignSettings.newBuilder().setVersion(1).setIsEnabled(true))
      .build()

    val extendedCh = minimumCh.toBuilder
      .applySideEffect(
        _.getProductBuilder
          .getGoodsBuilder(0)
          .getCustomBuilder
          .setId(Tariffs.CallsExtended.nameCampaign)
      )
      .build()

    val maximumCh = minimumCh.toBuilder
      .applySideEffect(
        _.getProductBuilder
          .getGoodsBuilder(0)
          .getCustomBuilder
          .setId(Tariffs.CallsMaximum.nameCampaign)
      )
      .build()

    val emptyStorage = new CampaignHeadersStorage(Seq.empty)
    val minimumStorage = new CampaignHeadersStorage(Seq(minimumCh))
    val extendedStorage = new CampaignHeadersStorage(Seq(extendedCh))
    val maximumStorage = new CampaignHeadersStorage(Seq(maximumCh))
  }

  trait MockHelpers {
    self: Wiring =>

    def expectCampaignHeaders(storage: CampaignHeadersStorage) =
      (campaignHeadersProvider.get _)
        .expects()
        .returning(storage)

    def expectTariffCall(tariff: Tariff) = {
      val response = TariffResponse.newBuilder()
      tariff match {
        case Tariffs.ListingMinimum => response.setListingMinimum(ListingMinimum.getDefaultInstance)
        case Tariffs.ListingExtended => response.setListingExtended(ListingExtended.getDefaultInstance)
        case Tariffs.ListingMaximum => response.setListingMaximum(ListingMaximum.getDefaultInstance)
        case Tariffs.CallsMinimum => response.setCallsMinimum(CallsMinimum.getDefaultInstance)
        case Tariffs.CallsExtended => response.setCallsExtended(CallsExtended.getDefaultInstance)
        case Tariffs.CallsMaximum => response.setCallsMaximum(CallsMaximum.getDefaultInstance)
      }
      (sellerClient
        .getActualTariff(_: String, _: Option[Int])(_: Traced))
        .expects(*, *, *)
        .returning(Future.successful(response.build))
    }

  }

  trait TestCase extends Wiring with Data with MockHelpers
}
