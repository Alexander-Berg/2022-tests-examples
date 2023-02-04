package ru.yandex.vertis.vos2.autoru.workers.ydb.components.workers

import ru.auto.api.ApiOfferModel.Category
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vertis.vos2.autoru.workers.ydb.components.workers.CartinderAllowedTagWorkerYdb.AllowedForCartinderTag
import ru.yandex.vos2.AutoruModel.AutoruOffer
import ru.yandex.vos2.AutoruModel.AutoruOffer.{CarInfo, Exchange}
import ru.yandex.vos2.autoru.model.TestUtils
import org.junit.runner.RunWith
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.auto.core.AutoSchemaVersions
import ru.yandex.vos2.autoru.dao.proxy.OffersReader

@RunWith(classOf[JUnitRunner])
class CartinderAllowedTagWorkerYdbTest extends AnyWordSpec with Matchers with MockitoSupport {

  implicit val traced: Traced = Traced.empty

  abstract private class Fixture {

    def offerBuilder =
      TestUtils
        .createOffer()
        .setOfferAutoru(
          AutoruOffer
            .newBuilder()
            .setCategory(Category.CARS)
            .setVersion(AutoSchemaVersions.CAR_AD_VERSION)
            .setExchangeStatus(Exchange.POSSIBLE)
            .setCarInfo(
              CarInfo
                .newBuilder()
                .setMark("BMW")
                .setModel("3ER")
                .setSuperGenId(7744658)
            )
        )

    val otherOfferBuilder = TestUtils
      .createOffer()
      .setOfferAutoru(
        AutoruOffer
          .newBuilder()
          .setCategory(Category.CARS)
          .setVersion(AutoSchemaVersions.CAR_AD_VERSION)
          .setExchangeStatus(Exchange.POSSIBLE)
      )

    val offersReader = mock[OffersReader]

    val worker = new CartinderAllowedTagWorkerYdb(
      offersReader
    ) with YdbWorkerTestImpl
  }

  "Successfully set tag" in new Fixture {
    val offer = offerBuilder.build()
    when(offersReader.getOffers(?, ?, ?, ?, ?, ?, ?, ?)(?)).thenReturn(Seq(offer))

    val result = worker.process(offer, None)
    val resultOffer = result.updateOfferFunc.get(offer)
    assert(resultOffer.getTagList.contains(AllowedForCartinderTag))
    assert(result.nextCheck.isEmpty)
  }

  "doesn't set tag because more offers" in new Fixture {
    val offer = offerBuilder.build()
    val otherOffer = otherOfferBuilder.build()
    when(offersReader.getOffers(?, ?, ?, ?, ?, ?, ?, ?)(?)).thenReturn(Seq(offer, otherOffer))

    val result = worker.process(offer, None)
    assert(result.updateOfferFunc.isEmpty)
    assert(result.nextCheck.nonEmpty)
  }

  "remove tag because no more exchange possible" in new Fixture {
    val updatedOfferBuilder = offerBuilder
    updatedOfferBuilder.getOfferAutoruBuilder.setExchangeStatus(Exchange.NO_EXCHANGE)
    updatedOfferBuilder.addTag(AllowedForCartinderTag)
    val offer = updatedOfferBuilder.build
    when(offersReader.getOffers(?, ?, ?, ?, ?, ?, ?, ?)(?)).thenReturn(Seq(offer))

    val result = worker.process(offer, None)
    val resultOffer = result.updateOfferFunc.get(offer)
    assert(!resultOffer.getTagList.contains(AllowedForCartinderTag))
    assert(result.nextCheck.isEmpty)
  }

  "Should process offer with exchange possible" in new Fixture {
    val offer = offerBuilder.build()
    val result = worker.shouldProcess(offer, None)

    assert(result.shouldProcess)
    assert(result.shouldReschedule.isEmpty)
  }

  "Should not process offer without exchange" in new Fixture {
    val updatedOfferBuilder = offerBuilder
    updatedOfferBuilder.getOfferAutoruBuilder.setExchangeStatus(Exchange.NO_EXCHANGE)
    val offer = updatedOfferBuilder.build()
    val result = worker.shouldProcess(offer, None)

    assert(!result.shouldProcess)
    assert(result.shouldReschedule.isEmpty)
  }

}
