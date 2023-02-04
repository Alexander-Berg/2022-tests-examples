package ru.yandex.vertis.vos2.autoru.workers.ydb.components.workers.tag_processor

import org.scalatest.wordspec.AnyWordSpec
import ru.auto.api.ApiOfferModel.PtsStatus
import ru.yandex.vertis.feature.impl.BasicFeatureTypes.BooleanFeatureType
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vertis.vos2.autoru.workers.ydb.components.workers.YdbWorkerTestImpl
import ru.yandex.vertis.vos2.autoru.workers.ydb.components.workers.tag_processor.substages.{WithoutPtsCustomClearedTag, WithoutPtsCustomNotClearedTag}
import ru.yandex.vos2.AutoruModel.AutoruOffer.CustomHouseStatus
import ru.yandex.vos2.autoru.model.TestUtils
import ru.yandex.vos2.commonfeatures.{FeatureRegistryFactory, FeaturesManager}
import ru.yandex.vos2.model.ModelUtils.RichOffer

class SimpleTagProcessorWithPtsTagCheckTest extends AnyWordSpec {
  implicit val trace = Traced.empty

  val featureRegistry = FeatureRegistryFactory.inMemory()
  val featuresManager = new FeaturesManager(featureRegistry)
  featureRegistry.updateFeature(featuresManager.WithoutPtsTagWorker.name, true)
  featureRegistry.updateFeature(featuresManager.SimpleTagProcessor.name, true)

  val withoutPtsCustomClearedTag = new WithoutPtsCustomClearedTag {
    override def features: FeaturesManager = featuresManager
  }

  val withoutPtsCustomNotClearedTag = new WithoutPtsCustomNotClearedTag {
    override def features: FeaturesManager = featuresManager
  }

  abstract private class Fixture {
    val offerBuilder = TestUtils.createOffer()

    val worker: SimpleTagProcessor = new SimpleTagProcessor(
      Seq(withoutPtsCustomClearedTag, withoutPtsCustomNotClearedTag)
    ) with YdbWorkerTestImpl {
      override def features: FeaturesManager = featuresManager
    }

  }

  ("should add withoutPtsCustomNOTClearedTag") in new Fixture {
    offerBuilder.getOfferAutoruBuilder.getDocumentsBuilder.setCustomHouseState(CustomHouseStatus.NOT_CLEARED)
    val res = worker.process(offerBuilder.build(), None).updateOfferFunc.get(offerBuilder.build())
    assert(res.existsTag(WithoutPtsCustomNotClearedTag.Tag))
  }
  ("should NOT add withoutPtsCustomNOTClearedTag") in new Fixture {
    val res = worker.shouldProcess(offerBuilder.build(), None).shouldProcess
    assert(!res)
  }
  ("should add withoutPtsCustomClearedTag") in new Fixture {
    offerBuilder.getOfferAutoruBuilder.getDocumentsBuilder.setCustomHouseState(CustomHouseStatus.CLEARED)
    offerBuilder.getOfferAutoruBuilder.getDocumentsBuilder.setPtsStatus(PtsStatus.NO_PTS)
    val res = worker.process(offerBuilder.build(), None).updateOfferFunc.get(offerBuilder.build())
    assert(res.existsTag(WithoutPtsCustomClearedTag.Tag))
  }

  ("should NOT add withoutPtsCustomClearedTag") in new Fixture {
    offerBuilder.getOfferAutoruBuilder.getDocumentsBuilder.setCustomHouseState(CustomHouseStatus.CLEARED)
    val res = worker.shouldProcess(offerBuilder.build(), None).shouldProcess
    assert(!res)
  }

  ("should clear withoutPtsCustomClearedTag") in new Fixture {
    val offer = offerBuilder.addTag(WithoutPtsCustomClearedTag.Tag)
    val res = worker.process(offer.build(), None).updateOfferFunc.get(offer.build())
    assert(!res.existsTag(WithoutPtsCustomClearedTag.Tag))
  }

  ("should clear withoutPtsCustomNOTClearedTag") in new Fixture {
    val offer = offerBuilder.addTag(WithoutPtsCustomNotClearedTag.Tag)
    val res = worker.process(offer.build(), None).updateOfferFunc.get(offer.build())
    assert(!res.existsTag(WithoutPtsCustomNotClearedTag.Tag))
  }
  ("should not process second time") in new Fixture {
    offerBuilder.getOfferAutoruBuilder.getDocumentsBuilder.setCustomHouseState(CustomHouseStatus.CLEARED)
    offerBuilder.getOfferAutoruBuilder.getDocumentsBuilder.setPtsStatus(PtsStatus.NO_PTS)
    val res = worker.process(offerBuilder.build(), None).updateOfferFunc.get(offerBuilder.build())
    assert(res.existsTag(WithoutPtsCustomClearedTag.Tag))

    val res2 = worker.shouldProcess(res, None).shouldProcess
    assert(!res2)
  }

}
