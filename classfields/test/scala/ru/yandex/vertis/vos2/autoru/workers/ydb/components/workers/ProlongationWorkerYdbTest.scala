package ru.yandex.vertis.vos2.autoru.workers.ydb.components.workers

import org.joda.time.DateTime
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.feature.impl.BasicFeatureTypes._
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vos2.OfferModel
import ru.yandex.vos2.OfferModel.OfferFlag
import ru.yandex.vos2.autoru.InitTestDbs
import ru.yandex.vos2.commonfeatures.FeaturesManager
import ru.yandex.vos2.model.ModelUtils.RichOfferBuilder

class ProlongationWorkerYdbTest
  extends AnyWordSpec
  with Matchers
  with MockitoSupport
  with InitTestDbs
  with BeforeAndAfterAll {

  override def beforeAll(): Unit = {
    initOldSalesDbs()
  }

  val allowedRegions = Seq(
    10693L,
    10832L,
    10819L
  )

  implicit val traced: Traced = Traced.empty

  val saleId1 = 1043270830 // объявление от частника с активными услугами

  val saleId2 = 1044159039 // объявление клиента с активной услугой

  val offer1: OfferModel.Offer = getOfferById(saleId1) // expire_date = Thu Oct 20 14:37:02 MSK 2016
  .toBuilder.clearFlag().build

  val offer2: OfferModel.Offer = getOfferById(saleId2) // expire_date = Wed Sep 21 22:42:17 MSK 2016
  components.featureRegistry.updateFeature(components.featuresManager.ExperimentInfiniteOfferYdb.name, true)

  abstract private class Fixture {

    val worker = new ProlongationWorkerYdb(
      components.oldDbWriter
    ) with YdbWorkerTestImpl {
      override def features: FeaturesManager = components.featuresManager
    }
  }

  "no process" in new Fixture {
    // 1. если не активное объявление - ничего не делаем
    assert(!worker.shouldProcess(offer1.toBuilder.addFlag(OfferFlag.OF_INACTIVE).build(), None).shouldProcess)
    // 2. если стоит флаг OF_DRAFT - тоже ничего не делаем
    assert(!worker.shouldProcess(offer1.toBuilder.putFlag(OfferFlag.OF_DRAFT).build(), None).shouldProcess)
    // 3. если объявление от салона - ничего не делаем
    assert(!worker.shouldProcess(offer2, None).shouldProcess)

    // 4. продлевать еще рано
    val offer4 = {
      val builder = offer1.toBuilder
      builder.setTimestampWillExpire(DateTime.now.plusDays(6).getMillis)
      builder.build()
    }
    assert(!worker.shouldProcess(offer4, None).shouldProcess)
  }

  allowedRegions.foreach { getId =>
    s"process for region $getId" in new Fixture {

      val now = DateTime.now

      val offer = {
        val builder = offer1.toBuilder
        builder.getOfferAutoruBuilder.getSellerBuilder.getPlaceBuilder.setGeobaseId(getId).build()
        builder.setTimestampWillExpire(now.minusDays(6).getMillis)
        builder.build()
      }

      assert(worker.shouldProcess(offer, None).shouldProcess)
      val result = worker.process(offer, None)
      result.nextCheck.get.getMillis shouldBe new DateTime().plusDays(27).getMillis +- 1000
      assert(result.updateOfferFunc.get(offer).getTimestampWillExpire - now.plusMonths(3).getMillis < 1000)

    }
  }

}
