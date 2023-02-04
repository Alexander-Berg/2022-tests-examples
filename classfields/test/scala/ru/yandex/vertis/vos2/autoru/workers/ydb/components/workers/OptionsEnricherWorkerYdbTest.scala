package ru.yandex.vertis.vos2.autoru.workers.ydb.components.workers

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.feature.impl.BasicFeatureTypes.BooleanFeatureType
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vos2.OfferModel.Offer
import ru.yandex.vos2.autoru.InitTestDbs
import ru.yandex.vos2.commonfeatures.FeaturesManager
import ru.yandex.vos2.util.{IO, Protobuf}

import scala.jdk.CollectionConverters._
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class OptionsEnricherWorkerYdbTest extends AnyWordSpec with Matchers with MockitoSupport with InitTestDbs {

  implicit val traced: Traced = Traced.empty
  initDbs()
  components.featureRegistry.updateFeature(components.featuresManager.CatalogEquipments.name, true)
  components.featureRegistry.updateFeature(components.featuresManager.EnrichOptionsCheckIncompatibleYdb.name, true)

  abstract private class Fixture {
    val offer: Offer

    val worker = new OptionsEnricherWorkerYdb(
      components.equipmentHolder
    ) with YdbWorkerTestImpl {
      override def features: FeaturesManager = components.featuresManager
    }
  }

  val offerFromFile: Offer = {
    val res = IO.using(this.getClass.getResourceAsStream("/incompatible_equipment_offer.json")) { is =>
      IO.readLines(is).foldLeft("") { (acc, el) =>
        acc + el
      }
    }
    (Protobuf.fromJson[Offer](res))
  }

  "equipment Worker YDB" should {

    "Offer with cleaned equipment " in new Fixture {
      val offerBuilder =
        Offer.newBuilder(offerFromFile)
      val offer: Offer = offerBuilder.build

      val result = worker.process(offer, None)

      val updatedOffer = result.updateOfferFunc.get(offer)
      updatedOffer
      assert(
        !updatedOffer.getOfferAutoru.getCarInfo.getEquipmentList.asScala.toSet.exists(res =>
          res.getName == "driver-seat-electric"
        )
      )
    }

  }
}
