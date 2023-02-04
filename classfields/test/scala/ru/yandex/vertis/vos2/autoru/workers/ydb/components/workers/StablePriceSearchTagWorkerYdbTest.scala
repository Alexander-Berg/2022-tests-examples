package ru.yandex.vertis.vos2.autoru.workers.ydb.components.workers

import org.joda.time.DateTime
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.auto.api.ApiOfferModel.Category
import ru.yandex.auto.core.AutoSchemaVersions
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vertis.vos2.autoru.workers.ydb.components.workers.StablePriceSearchTagWorkerYdb.{PercentageDiffForStablePrice, StablePriceTag}
import ru.yandex.vertis.vos2.autoru.workers.ydb.components.workers.utils.{getStateStr, NextCheckData}
import ru.yandex.vos2.AutoruModel.AutoruOffer
import ru.yandex.vos2.AutoruModel.AutoruOffer.{CarInfo, Location, Seller}
import ru.yandex.vos2.autoru.model.TestUtils
import ru.yandex.vos2.autoru.services.pricestats.PriceStatsClient
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class StablePriceSearchTagWorkerYdbTest extends AnyWordSpec with Matchers with MockitoSupport {

  implicit val traced: Traced = Traced.empty

  abstract private class Fixture {

    val offerBuilder = TestUtils
      .createOffer()
      .setOfferAutoru(
        AutoruOffer
          .newBuilder()
          .setCategory(Category.CARS)
          .setVersion(AutoSchemaVersions.CAR_AD_VERSION)
          .setCarInfo(
            CarInfo
              .newBuilder()
              .setMark("BMW")
              .setModel("3ER")
              .setSuperGenId(7744658)
          )
          .setSeller(Seller.newBuilder().setPlace(Location.newBuilder().setGeobaseId(213)))
      )

    val priceStatsClient = mock[PriceStatsClient]

    val worker = new StablePriceSearchTagWorkerYdb(
      priceStatsClient
    ) with YdbWorkerTestImpl
  }

  "Offer with stable price should contain tag stable_price" in new Fixture {
    val offer = offerBuilder.build()
    when(priceStatsClient.getAvgPricePercentageDiff(offer.getOfferAutoru))
      .thenReturn(Some(PercentageDiffForStablePrice + 3))

    val result = worker.process(offer, None)
    val resultOffer = result.updateOfferFunc.get(offer)
    assert(resultOffer.getTagList.contains(StablePriceTag) && result.nextState.nonEmpty && result.nextCheck.nonEmpty)
  }

  "Offer with stable price should be without changes" in new Fixture {
    offerBuilder.getOfferAutoruBuilder.getPriceStatsBuilder
      .setLastYearPricePercentageDiff(PercentageDiffForStablePrice + 3)

    val offer = offerBuilder
      .addTag(StablePriceTag)
      .build()

    when(priceStatsClient.getAvgPricePercentageDiff(offer.getOfferAutoru))
      .thenReturn(Some(PercentageDiffForStablePrice + 3))

    val result = worker.process(offer, None)
    assert(result.updateOfferFunc.isEmpty && result.nextState.nonEmpty && result.nextCheck.nonEmpty)
  }

  "Offer with non-stable price should not contain tag stable_price" in new Fixture {
    val offer = offerBuilder.build()
    when(priceStatsClient.getAvgPricePercentageDiff(offer.getOfferAutoru))
      .thenReturn(Some(PercentageDiffForStablePrice - 3))
    val result = worker.process(offer, None)
    assert(
      !offer.getTagList
        .contains(StablePriceTag) && result.updateOfferFunc.isEmpty && result.nextState.nonEmpty && result.nextCheck.nonEmpty
    )
  }

  "Offer with unknown price_stats result should not contain tag stable_price" in new Fixture {
    val offer = offerBuilder.build()

    val result = worker.process(offer, None)
    assert(!offer.getTagList.contains(StablePriceTag) && result.updateOfferFunc.isEmpty)
  }

  "Offer had contained stable_price tag, but as soon as price was updated to unstable," +
    "offer should not contain stable_price tag" in new Fixture {
    val offer = offerBuilder.addTag(StablePriceTag).build()
    when(priceStatsClient.getAvgPricePercentageDiff(offer.getOfferAutoru))
      .thenReturn(Some(PercentageDiffForStablePrice - 3))
    val result = worker.process(offer, None)
    val resultOffer = result.updateOfferFunc.get(offer)
    assert(!resultOffer.getTagList.contains(StablePriceTag) && result.nextState.nonEmpty && result.nextCheck.nonEmpty)
  }

  "Should not process offer with nearest update time" in new Fixture {

    val autoruOffer = AutoruOffer
      .newBuilder()
      .mergeFrom(offerBuilder.build().getOfferAutoru)
      .setLastBrandCertCheckTimestamp(System.currentTimeMillis() + 3000)
    val testOffer = TestUtils.createOffer().setOfferAutoru(autoruOffer).build()
    val state = getStateStr(NextCheckData((new DateTime().plus(3000))))
    val result = worker.shouldProcess(testOffer, Some(state))
    assert(!result.shouldProcess && result.shouldReschedule.nonEmpty)
  }

  "Should  process offer with empty state" in new Fixture {

    val autoruOffer = AutoruOffer
      .newBuilder()
      .mergeFrom(offerBuilder.build().getOfferAutoru)
    val testOffer = TestUtils.createOffer().setOfferAutoru(autoruOffer).build()
    assert(worker.shouldProcess(testOffer, None).shouldProcess)
  }

  "Should not process offer with moto category" in new Fixture {

    val autoruOffer = AutoruOffer
      .newBuilder()
      .mergeFrom(offerBuilder.build().getOfferAutoru)
      .setCategory(Category.MOTO)
    val testOffer = TestUtils.createOffer().setOfferAutoru(autoruOffer).build()
    val result = worker.shouldProcess(testOffer, None)

    assert(!result.shouldProcess && result.shouldReschedule.isEmpty)
  }

  "Should not process offer without mark" in new Fixture {
    val autoruOffer = AutoruOffer.newBuilder().mergeFrom(offerBuilder.build().getOfferAutoru)
    autoruOffer.getCarInfoBuilder.clearMark()
    val testOffer = TestUtils.createOffer().setOfferAutoru(autoruOffer).build()
    val result = worker.shouldProcess(testOffer, None)

    assert(!result.shouldProcess && result.shouldReschedule.nonEmpty)
  }

  "Should not process offer without model" in new Fixture {
    val autoruOffer = AutoruOffer.newBuilder().mergeFrom(offerBuilder.build().getOfferAutoru)
    autoruOffer.getCarInfoBuilder.clearModel()
    val testOffer = TestUtils.createOffer().setOfferAutoru(autoruOffer).build()
    val result = worker.shouldProcess(testOffer, None)

    assert(!result.shouldProcess && result.shouldReschedule.nonEmpty)
  }

  "Should not process offer without supergen" in new Fixture {
    val autoruOffer = AutoruOffer.newBuilder().mergeFrom(offerBuilder.build().getOfferAutoru)
    autoruOffer.getCarInfoBuilder.clearSuperGenId()
    val testOffer = TestUtils.createOffer().setOfferAutoru(autoruOffer).build()
    val result = worker.shouldProcess(testOffer, None)

    assert(!result.shouldProcess && result.shouldReschedule.nonEmpty)
  }

  "Should not process offer without geobase_id" in new Fixture {
    val autoruOffer = AutoruOffer.newBuilder().mergeFrom(offerBuilder.build().getOfferAutoru)
    autoruOffer.getSellerBuilder.clear()
    val testOffer = TestUtils.createOffer().setOfferAutoru(autoruOffer).build()
    val result = worker.shouldProcess(testOffer, None)

    assert(!result.shouldProcess && result.shouldReschedule.nonEmpty)
  }

}
