package ru.yandex.vertis.vos2.autoru.workers.ydb.components.workers

import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vertis.vos2.autoru.workers.ydb.components.workers.tag_processor.substages.HistoryDiscountTag
import ru.yandex.vertis.vos2.autoru.workers.ydb.components.workers.tag_processor.substages.HistoryDiscountTag._
import ru.yandex.vos2.AutoruModel.AutoruOffer.{Price, SellerType}
import ru.yandex.vos2.BasicsModel.Currency
import ru.yandex.vos2.autoru.model.TestUtils
import ru.yandex.vos2.commonfeatures.FeaturesManager

import scala.jdk.CollectionConverters._
import scala.concurrent.duration._

class HistoryDiscountTagWorkerYdbTest extends AnyWordSpec with MockitoSupport with Matchers with BeforeAndAfterAll {

  implicit val traced: Traced = Traced.empty

  abstract private class Fixture {

    val worker = new HistoryDiscountTag {
      override def features: FeaturesManager = null
    }
  }

  ("last price change > 7 days ago - should not process") in new Fixture {
    val builder = TestUtils.createOffer()

    val price1 = createPrice(LowPriceBound - 100000, 8.days)
    val price2 = createPrice(LowPriceBound, 9.days)

    builder.setTimestampCreate((System.currentTimeMillis().millis - 3.days).toMillis)
    builder.getOfferAutoruBuilder
      .addPriceHistory(price1)
      .addPriceHistory(price2)

    val orderedHistory = builder.getOfferAutoru.getPriceHistoryList.asScala
      .sortBy(_.getCreated)(Ordering[Long].reverse)
      .toList

    assert(!worker.meetsConditions(builder, orderedHistory))
  }

  ("last price change < 7 days ago - should process") in new Fixture {
    val builder = TestUtils.createOffer()

    val price1 = createPrice(LowPriceBound - 10000, 6.days)
    val price2 = createPrice(LowPriceBound, 3.days)

    builder.setTimestampCreate((System.currentTimeMillis().millis - 3.days).toMillis)
    builder.getOfferAutoruBuilder
      .addPriceHistory(price1)
      .addPriceHistory(price2)

    val orderedHistory = builder.getOfferAutoru.getPriceHistoryList.asScala
      .sortBy(_.getCreated)(Ordering[Long].reverse)
      .toList

    assert(worker.meetsConditions(builder, orderedHistory))
  }

  ("gap between price changing < 2 days ago - should not process") in new Fixture {
    val builder = TestUtils.createOffer()

    val price1 = createPrice(LowPriceBound - 10000, 4.days)
    val price2 = createPrice(LowPriceBound, 3.days)

    builder.setTimestampCreate((System.currentTimeMillis().millis - 3.days).toMillis)
    builder.getOfferAutoruBuilder
      .addPriceHistory(price1)
      .addPriceHistory(price2)

    val orderedHistory = builder.getOfferAutoru.getPriceHistoryList.asScala
      .sortBy(_.getCreated)(Ordering[Long].reverse)
      .toList

    assert(!worker.meetsConditions(builder, orderedHistory))
  }

  ("price changed > 50% - no tags") in new Fixture {
    val builder = TestUtils.createOffer()

    val price1 = createPrice(LowPriceBound * 0.4, 3.days)
    val price2 = createPrice(LowPriceBound, 6.days)

    builder.setTimestampCreate((System.currentTimeMillis().millis - 3.days).toMillis)
    builder.getOfferAutoruBuilder
      .addPriceHistory(price1)
      .addPriceHistory(price2)

    val offer = builder.build()
    assert(!worker.shouldProcess(offer, None))

    assert(offer.getTagList.asScala.diff(offer.getTagList.asScala).isEmpty)
  }

  ("price <= 200000, 5% < decrease < 50% - discount tags") in new Fixture {
    val builder = TestUtils.createOffer()

    val price1 = createPrice(200000 * 0.94, 3.days)
    val price2 = createPrice(200000, 6.days)

    builder.setTimestampCreate((System.currentTimeMillis().millis - 3.days).toMillis)
    builder.getOfferAutoruBuilder
      .addPriceHistory(price1)
      .addPriceHistory(price2)

    val offer = builder.build()
    assert(worker.shouldProcess(offer, None))
    val processed = worker.process(offer, None).updateOfferFunc.get(offer)
    assert(
      processed.getTagList.asScala
        .diff(offer.getTagList.asScala)
        .contains(Tag.Discount)
    )
    assert(
      processed.getTagList.asScala
        .diff(offer.getTagList.asScala)
        .contains(Tag.HistoryDiscount)
    )
  }

  ("price <= 200000, 5% < increase < 50% - increase tag") in new Fixture {
    val builder = TestUtils.createOffer()

    val price1 = createPrice(200000 * 1.06, 3.days)
    val price2 = createPrice(200000, 6.days)

    builder.setTimestampCreate((System.currentTimeMillis().millis - 3.days).toMillis)
    builder.getOfferAutoruBuilder
      .addPriceHistory(price1)
      .addPriceHistory(price2)

    val offer = builder.build()
    assert(worker.shouldProcess(offer, None))
    val processed = worker.process(offer, None).updateOfferFunc.get(offer)
    assert(
      processed.getTagList.asScala
        .diff(offer.getTagList.asScala)
        .contains(Tag.HistoryIncrease)
    )
  }

  ("price <= 200000, decrease < 5% - no tags") in new Fixture {
    val builder = TestUtils.createOffer()

    val price1 = createPrice(200000 * 0.96, 3.days)
    val price2 = createPrice(200000, 6.days)

    builder.setTimestampCreate((System.currentTimeMillis().millis - 3.days).toMillis)
    builder.getOfferAutoruBuilder
      .addPriceHistory(price1)
      .addPriceHistory(price2)

    val offer = builder.build()
    assert(!worker.shouldProcess(offer, None))
    assert(offer.getTagList.asScala.diff(offer.getTagList.asScala).isEmpty)
  }

  ("price <= 200000, increase < 5% - no tags") in new Fixture {
    val builder = TestUtils.createOffer()

    val price1 = createPrice(200000 * 1.04, 3.days)
    val price2 = createPrice(200000, 6.days)

    builder.setTimestampCreate((System.currentTimeMillis().millis - 3.days).toMillis)
    builder.getOfferAutoruBuilder
      .addPriceHistory(price1)
      .addPriceHistory(price2)

    val offer = builder.build()
    assert(!worker.shouldProcess(offer, None))
    assert(offer.getTagList.asScala.diff(offer.getTagList.asScala).isEmpty)
  }

  ("200 000 < price < 400 000, 10 000 < discount < 50% - discount tags") in new Fixture {
    val builder = TestUtils.createOffer()

    val price1 = createPrice(300000 - 11000, 3.days)
    val price2 = createPrice(300000, 6.days)

    builder.setTimestampCreate((System.currentTimeMillis().millis - 3.days).toMillis)
    builder.getOfferAutoruBuilder
      .addPriceHistory(price1)
      .addPriceHistory(price2)

    val offer = builder.build()
    assert(worker.shouldProcess(offer, None))
    val processed = worker.process(offer, None).updateOfferFunc.get(offer)
    assert(
      processed.getTagList.asScala
        .diff(offer.getTagList.asScala)
        .contains(Tag.Discount)
    )
    assert(
      processed.getTagList.asScala
        .diff(offer.getTagList.asScala)
        .contains(Tag.HistoryDiscount)
    )
  }

  ("200 000 < price < 400 000, 10 000 < increase < 50% - increase tag") in new Fixture {
    val builder = TestUtils.createOffer()

    val price1 = createPrice(300000 + 11000, 3.days)
    val price2 = createPrice(300000, 6.days)

    builder.setTimestampCreate((System.currentTimeMillis().millis - 3.days).toMillis)
    builder.getOfferAutoruBuilder
      .addPriceHistory(price1)
      .addPriceHistory(price2)

    val offer = builder.build()
    assert(worker.shouldProcess(offer, None))
    val processed = worker.process(offer, None).updateOfferFunc.get(offer)
    assert(
      processed.getTagList.asScala
        .diff(offer.getTagList.asScala)
        .contains(Tag.HistoryIncrease)
    )
  }

  ("200 000 < price < 400 000, decrease < 10 000 - no tags") in new Fixture {
    val builder = TestUtils.createOffer()

    val price1 = createPrice(300000 - 5000, 3.days)
    val price2 = createPrice(300000, 6.days)

    builder.setTimestampCreate((System.currentTimeMillis().millis - 3.days).toMillis)
    builder.getOfferAutoruBuilder
      .addPriceHistory(price1)
      .addPriceHistory(price2)

    val offer = builder.build()
    assert(!worker.shouldProcess(offer, None))
    assert(offer.getTagList.asScala.diff(offer.getTagList.asScala).isEmpty)
  }

  ("200 000 < price < 400 000, increase < 10 000 - no tags") in new Fixture {
    val builder = TestUtils.createOffer()

    val price1 = createPrice(300000 + 5000, 3.days)
    val price2 = createPrice(300000, 6.days)

    builder.setTimestampCreate((System.currentTimeMillis().millis - 3.days).toMillis)
    builder.getOfferAutoruBuilder
      .addPriceHistory(price1)
      .addPriceHistory(price2)

    val offer = builder.build()
    assert(!worker.shouldProcess(offer, None))
    assert(offer.getTagList.asScala.diff(offer.getTagList.asScala).isEmpty)
  }

  ("price > 400 000, 3% < decrease < 50%  - discount tags") in new Fixture {
    val builder = TestUtils.createOffer()

    val price1 = createPrice(410000 * 0.96, 3.days)
    val price2 = createPrice(410000, 6.days)

    builder.setTimestampCreate((System.currentTimeMillis().millis - 3.days).toMillis)
    builder.getOfferAutoruBuilder
      .addPriceHistory(price1)
      .addPriceHistory(price2)

    val offer = builder.build()
    assert(worker.shouldProcess(offer, None))
    val processed = worker.process(offer, None).updateOfferFunc.get(offer)
    assert(
      processed.getTagList.asScala
        .diff(offer.getTagList.asScala)
        .contains(Tag.Discount)
    )
    assert(
      processed.getTagList.asScala
        .diff(offer.getTagList.asScala)
        .contains(Tag.HistoryDiscount)
    )
  }

  ("price > 400 000, 3% < increase < 50% - increase tag") in new Fixture {
    val builder = TestUtils.createOffer()

    val price1 = createPrice(410000 * 1.04, 3.days)
    val price2 = createPrice(410000, 6.days)

    builder.setTimestampCreate((System.currentTimeMillis().millis - 3.days).toMillis)
    builder.getOfferAutoruBuilder
      .addPriceHistory(price1)
      .addPriceHistory(price2)

    val offer = builder.build()
    assert(worker.shouldProcess(offer, None))
    val processed = worker.process(offer, None).updateOfferFunc.get(offer)
    assert(
      processed.getTagList.asScala
        .diff(offer.getTagList.asScala)
        .contains(Tag.HistoryIncrease)
    )
  }

  ("price > 400 000, decrease < 3% - no tags") in new Fixture {
    val builder = TestUtils.createOffer()

    val price1 = createPrice(410000 * 0.98, 3.days)
    val price2 = createPrice(410000, 6.days)

    builder.setTimestampCreate((System.currentTimeMillis().millis - 3.days).toMillis)
    builder.getOfferAutoruBuilder
      .addPriceHistory(price1)
      .addPriceHistory(price2)

    val offer = builder.build()
    assert(!worker.shouldProcess(offer, None))
    assert(offer.getTagList.asScala.diff(offer.getTagList.asScala).isEmpty)
  }

  ("price > 400 000, increase < 3% - no tags") in new Fixture {
    val builder = TestUtils.createOffer()

    val price1 = createPrice(410000 * 1.02, 3.days)
    val price2 = createPrice(410000, 6.days)

    builder.setTimestampCreate((System.currentTimeMillis().millis - 3.days).toMillis)
    builder.getOfferAutoruBuilder
      .addPriceHistory(price1)
      .addPriceHistory(price2)

    val offer = builder.build()
    assert(!worker.shouldProcess(offer, None))
    assert(offer.getTagList.asScala.diff(offer.getTagList.asScala).isEmpty)
  }

  ("dealer with fresh date - increase tag") in new Fixture {
    val builder = TestUtils.createOffer()

    val price1 = createPrice(410000 * 1.04, 3.days)
    val price2 = createPrice(410000, 6.days)

    builder.setTimestampCreate((System.currentTimeMillis().millis - 3.days).toMillis)
    builder.getOfferAutoruBuilder
      .addPriceHistory(price1)
      .addPriceHistory(price2)
      .setSellerType(SellerType.COMMERCIAL)
      .setFreshDate((System.currentTimeMillis().millis - 5.day).toMillis)

    val offer = builder.build()
    assert(worker.shouldProcess(offer, None))
    val processed = worker.process(offer, None).updateOfferFunc.get(offer)
    assert(
      processed.getTagList.asScala
        .diff(offer.getTagList.asScala)
        .contains(Tag.HistoryIncrease)
    )
  }

  ("dealer with fresh date - no tags") in new Fixture {
    val builder = TestUtils.createOffer()

    val price1 = createPrice(410000 * 1.04, 3.days)
    val price2 = createPrice(410000, 6.days)

    builder.setTimestampCreate((System.currentTimeMillis().millis - 3.days).toMillis)
    builder.getOfferAutoruBuilder
      .addPriceHistory(price1)
      .addPriceHistory(price2)
      .setSellerType(SellerType.COMMERCIAL)
      .setFreshDate((System.currentTimeMillis().millis - 4.day).toMillis)

    val offer = builder.build()
    assert(worker.shouldProcess(offer, None))
    val processed = worker.process(offer, None).updateOfferFunc.get(offer)
    assert(
      processed.getTagList.asScala
        .diff(offer.getTagList.asScala)
        .contains(Tag.HistoryIncrease)
    )
  }

  ("last price change > 7 days ago - should clear tag") in new Fixture {
    val builder = TestUtils.createOffer()

    val price1 = createPrice(LowPriceBound - 100000, 8.days)
    val price2 = createPrice(LowPriceBound, 9.days)

    builder.setTimestampCreate((System.currentTimeMillis().millis - 3.days).toMillis)
    builder.getOfferAutoruBuilder
      .addPriceHistory(price1)
      .addPriceHistory(price2)

    builder.addTag(Tag.Discount)

    val orderedHistory = builder.getOfferAutoru.getPriceHistoryList.asScala
      .sortBy(_.getCreated)(Ordering[Long].reverse)
      .toList
    val processed = worker.process(builder.build(), None).updateOfferFunc.get(builder.build())

    assert(!worker.meetsConditions(builder, orderedHistory))
    assert(processed.getTagList.asScala.isEmpty)
  }

  private def createPrice(price: Double, timeAgo: FiniteDuration): Price =
    Price
      .newBuilder()
      .setCurrency(Currency.RUB)
      .setPrice(price)
      .setCreated((System.currentTimeMillis().millis - timeAgo).toMillis)
      .build()
}
