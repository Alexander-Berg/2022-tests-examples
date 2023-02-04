package ru.yandex.vertis.vos2.autoru.workers.ydb.components.workers

import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vos2.AutoruModel.AutoruOffer._
import ru.yandex.vos2.OfferModel.OfferFlag
import ru.yandex.vos2.autoru.dao.old.proxy.OldDbWriter
import ru.yandex.vos2.autoru.model.TestUtils
import ru.yandex.vos2.commonfeatures.{FeatureRegistryFactory, FeaturesManager}
import ru.yandex.vos2.model.ModelUtils.RichOffer
import ru.yandex.vertis.feature.impl.BasicFeatureTypes._

class DealerServicesCheckWorkerYdbTest extends AnyWordSpec with MockitoSupport with Matchers with BeforeAndAfterAll {
  implicit val traced: Traced = Traced.empty

  private val paidService = PaidService.newBuilder
    .setIsActive(true)
    .setServiceType(PaidService.ServiceType.ADD)
    .setCreated(111)
    .build()

  abstract private class Fixture {

    val featureRegistry = FeatureRegistryFactory.inMemory()
    val featuresManager = new FeaturesManager(featureRegistry)
    featureRegistry.updateFeature(featuresManager.DealersServicesCheckUpdateStatusYdb.name, true)

    val mockedOldDb: OldDbWriter = mock[OldDbWriter]

    val worker = new DealerServicesCheckWorkerYdb(mockedOldDb) with YdbWorkerTestImpl {
      override def features = featuresManager
    }

    when(mockedOldDb.updateStatus(?, ?, ?, ?)(?)).thenReturn(true)

    private val testOffer = TestUtils.createOffer(
      dealer = true
    )
    val notValidOffer = testOffer.build()
    testOffer.getOfferAutoruBuilder.addServices(paidService)
    val offer = testOffer.build()
  }

  "should NOT process" in new Fixture {
    assert(!worker.shouldProcess(offer, None).shouldProcess)
  }

  "should  process" in new Fixture {
    assert(worker.shouldProcess(notValidOffer, None).shouldProcess)
  }
  "processing offer" in new Fixture {
    assert(worker.shouldProcess(notValidOffer, None).shouldProcess)
    val result = worker.process(notValidOffer, None).updateOfferFunc.get(notValidOffer)
    assert(result.hasFlag(OfferFlag.OF_NEED_ACTIVATION))
  }

}
