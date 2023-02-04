package ru.yandex.vertis.vos2.autoru.workers.ydb.components.workers

import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.auto.api.ApiOfferModel.Category
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.shark.proto.Api.EcreditPreconditionResponse
import ru.yandex.vertis.shark.proto.{AmountRange, EcreditPrecondition, TermMonthsRange}
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vertis.vos2.autoru.workers.ydb.components.worker.YdbWorker
import ru.yandex.vos2.AutoruModel.AutoruOffer
import ru.yandex.vos2.OfferModel.{Offer, OfferFlag, OfferService}
import ru.yandex.vos2.autoru.model.AutoruOfferID
import ru.yandex.vos2.autoru.services.shark.SharkClient
import ru.yandex.vos2.commonfeatures.FeaturesManager
import ru.yandex.vos2.util.Dates.millisToTimestamp
import ru.yandex.vos2.util.Protobuf
import ru.yandex.vos2.{getNow, BasicsModel}

import scala.concurrent.duration._
import scala.util.{Failure, Success}

class EcreditWorkerYdbTest extends AnyWordSpec with MockitoSupport with Matchers with BeforeAndAfterAll {

  import EcreditWorkerYdbTest._

  implicit val traced: Traced = Traced.empty

  abstract private class Fixture {

    val mockFeatureManager: FeaturesManager = mock[FeaturesManager]
    val mockFeatureEcreditYdb: Feature[Boolean] = mock[Feature[Boolean]]
    val mockSharkClient: SharkClient = mock[SharkClient]

    when(mockFeatureEcreditYdb.value).thenReturn(true)
    when(mockFeatureManager.EcreditYdb).thenReturn(mockFeatureEcreditYdb)
    when(mockSharkClient.getEcreditPrecondition(?)(?)).thenReturn(Success(PreconditionResponse))

    val worker: YdbWorker = new EcreditWorkerYdb(mockSharkClient) with YdbWorkerTestImpl {

      override def features: FeaturesManager = mockFeatureManager
    }
  }

  "should not process draft" in new Fixture {
    assert(!worker.shouldProcess(SampleOffer.withDraft, state = None).shouldProcess)
  }

  "should not process no dealer" in new Fixture {
    assert(!worker.shouldProcess(SampleOffer.withNoDealer, state = None).shouldProcess)
  }

  "should not process no car" in new Fixture {
    assert(!worker.shouldProcess(SampleOffer.withNoCar, state = None).shouldProcess)
  }

  "should not process already processed offer" in new Fixture {
    assert(!worker.shouldProcess(SampleOffer, state = Some(statePresent)).shouldProcess)
  }

  "should process suitable offer" in new Fixture {
    assert(worker.shouldProcess(SampleOffer, state = None).shouldProcess)
  }

  "should process suitable offer (after price update)" in new Fixture {
    assert(worker.shouldProcess(SampleOffer, state = Some(statePastTwelveHours)).shouldProcess)
  }

  "should process suitable offer (once every 24 hours)" in new Fixture {
    assert(worker.shouldProcess(SampleOffer.withPastPriceCreated, state = Some(statePastTwoDays)).shouldProcess)
  }

  "process suitable offer (set precondition)" in new Fixture {
    val updateFuncOpt = worker.process(SampleOffer, state = None).updateOfferFunc
    val preconditionOpt = updateFuncOpt.map(_.apply(SampleOffer).getOfferAutoru.getEcreditPrecondition)
    assert(preconditionOpt.exists(_.equals(Precondition)))
  }

  "process suitable offer (clear precondition)" in new Fixture {
    when(mockSharkClient.getEcreditPrecondition(?)(?)).thenReturn(Success(EmptyPreconditionResponse))
    val offer = SampleOffer.withEcreditPrecondition
    val updateFuncOpt = worker.process(offer, state = None).updateOfferFunc
    val hasEcreditPrecondition = updateFuncOpt.forall(_.apply(offer).getOfferAutoru.hasEcreditPrecondition)
    assert(!hasEcreditPrecondition)
  }
}

object EcreditWorkerYdbTest {

  private[this] val UserRef: String = "ac_123456"
  private[this] val OfferId: String = AutoruOfferID.generateID(UserRef).toPlain
  private[this] val TimestampUpdate: Long = 0L
  private[this] val CarPrice: Double = 2000000d

  private[this] val EcreditProductId: String = "ecredit-product-id"
  private[this] val InterestRate: Float = 10.0f
  private[this] val MinInitialFeeRate: Float = 50000.0f
  private[this] val AmountRangeFrom: Long = 100000L
  private[this] val AmountRangeTo: Long = 10000000L
  private[this] val TermMonthsRangeFrom: Int = 6
  private[this] val TermMonthsRangeTo: Int = 60
  private[this] val MonthlyPayment: Long = 50000L

  private[this] val NoDealerUserRef: String = "a_123456"
  private[this] val OneHourMillis: Long = 1.hour.toMillis
  private[this] val TwelveHoursMillis: Long = 12.hours.toMillis
  private[this] val OneDayMillis: Long = 1.day.toMillis

  private val SampleOffer: Offer =
    Offer.newBuilder
      .setUserRef(UserRef)
      .setOfferID(OfferId)
      .setTimestampUpdate(TimestampUpdate)
      .setOfferService(OfferService.OFFER_AUTO)
      .setOfferAutoru(
        AutoruOffer.newBuilder
          .setVersion(1)
          .setCategory(Category.CARS)
          .setPrice(
            AutoruOffer.Price.newBuilder
              .setCurrency(BasicsModel.Currency.RUB)
              .setPrice(CarPrice)
              .setPriceRub(CarPrice)
              .setCreated(getNow - OneHourMillis)
              .build
          )
      )
      .build

  private val Precondition: EcreditPrecondition =
    EcreditPrecondition.newBuilder
      .setEcreditProductId(EcreditProductId)
      .setInterestRate(InterestRate)
      .setMinInitialFeeRate(MinInitialFeeRate)
      .setAmountRange(
        AmountRange.newBuilder
          .setFrom(AmountRangeFrom)
          .setTo(AmountRangeTo)
          .build
      )
      .setTermMonthsRange(
        TermMonthsRange.newBuilder
          .setFrom(TermMonthsRangeFrom)
          .setTo(TermMonthsRangeTo)
          .build
      )
      .setMonthlyPayment(MonthlyPayment)
      .build

  private val PreconditionResponse =
    EcreditPreconditionResponse.newBuilder
      .setEcreditPrecondition(Precondition)
      .build

  private val EmptyPreconditionResponse = EcreditPreconditionResponse.getDefaultInstance

  private def statePresent: String = Protobuf.toJson(millisToTimestamp(getNow))
  private def statePastTwelveHours: String = Protobuf.toJson(millisToTimestamp(getNow - TwelveHoursMillis))
  private def statePastTwoDays: String = Protobuf.toJson(millisToTimestamp(getNow - 2 * OneDayMillis))

  implicit private class RichOffer(val value: Offer) extends AnyVal {

    def withDraft: Offer =
      value.toBuilder.clearFlag
        .addFlag(OfferFlag.OF_DRAFT)
        .build

    def withNoDealer: Offer =
      value.toBuilder
        .setUserRef(NoDealerUserRef)
        .build

    def withNoCar: Offer =
      value.toBuilder
        .setOfferAutoru(
          value.getOfferAutoru.toBuilder
            .setCategory(Category.MOTO)
            .build
        )
        .build

    def withPastPriceCreated: Offer =
      value.toBuilder
        .setOfferAutoru(
          value.getOfferAutoru.toBuilder
            .setPrice(
              value.getOfferAutoru.getPrice.toBuilder
                .setCreated(getNow - 3 * OneDayMillis)
                .build
            )
            .build
        )
        .build

    def withEcreditPrecondition: Offer =
      value.toBuilder
        .setOfferAutoru(
          value.getOfferAutoru.toBuilder
            .setEcreditPrecondition(Precondition)
            .build
        )
        .build
  }
}
