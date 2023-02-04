package ru.yandex.vertis.vos2.autoru.workers.ydb.components.workers

import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vos2.AutoruModel.AutoruOffer.PaidService.ServiceType
import ru.yandex.vos2.OfferModel.Offer
import ru.yandex.vos2.autoru.InitTestDbs
import ru.yandex.vos2.autoru.model.TestUtils
import ru.yandex.vos2.commonfeatures.{FeatureRegistryFactory, FeaturesManager}
import ru.yandex.vos2.getNow

class PaidServiceExpirationWorkerYdbTest
  extends AnyWordSpec
  with MockitoSupport
  with Matchers
  with InitTestDbs
  with BeforeAndAfterAll {
  implicit val traced: Traced = Traced.empty

  abstract private class Fixture {
    val offer: Offer
    val featureRegistry = FeatureRegistryFactory.inMemory()
    val featuresManager = new FeaturesManager(featureRegistry)

    val worker = new PaidServiceExpirationWorkerYdb(
      components.oldDbWriter
    ) with YdbWorkerTestImpl {
      override def features: FeaturesManager = featuresManager
    }
  }

  override def beforeAll(): Unit = {
    initOldSalesDbs()
  }
  "Ignore service without expire date" in new Fixture {
    val offerBuiler = TestUtils.createOffer(dealer = false)
    offerBuiler.getOfferAutoruBuilder
      .addServicesBuilder()
      .setIsActive(true)
      .setCreated(getNow - 20000)
      .setServiceType(ServiceType.TOP)

    val offer = offerBuiler.build()

    val result = worker.shouldProcess(offer, None)

    result.shouldProcess shouldEqual false
  }

  "Disable expired service" in new Fixture {
    val offerBuiler = TestUtils.createOffer(dealer = false)
    offerBuiler.getOfferAutoruBuilder
      .addServicesBuilder()
      .setIsActive(true)
      .setCreated(getNow - 20000)
      .setExpireDate(getNow - 10000)
      .setServiceType(ServiceType.TOP)
    val offer = offerBuiler.build()

    val result = worker.shouldProcess(offer, None)

    result.shouldProcess shouldEqual true
    val workerRes = worker.process(offer, None)
    val newOffer = workerRes.updateOfferFunc.get(offer)
    newOffer.getOfferAutoru.getServices(0).getIsActive shouldBe false
  }

  "Ignore disabled services" in new Fixture {
    val offerBuiler = TestUtils.createOffer(dealer = false)
    offerBuiler.getOfferAutoruBuilder
      .addServicesBuilder()
      .setIsActive(false)
      .setCreated(getNow - 20000)
      .setExpireDate(getNow - 10000)
      .setServiceType(ServiceType.TOP)
    val offer = offerBuiler.build()

    val result = worker.shouldProcess(offer, None)

    result.shouldProcess shouldEqual false
  }

  "Ignore offers from dealer" in new Fixture {
    val offerBuiler = TestUtils.createOffer(dealer = true)

    offerBuiler.getOfferAutoruBuilder
      .addServicesBuilder()
      .setIsActive(true)
      .setCreated(getNow - 20000)
      .setExpireDate(getNow - 10000)
      .setServiceType(ServiceType.TOP)
    val offer = offerBuiler.build()

    val result = worker.shouldProcess(offer, None).shouldProcess

    assert(!result)
  }

  "ReSchedule with correct timestamp" in new Fixture {
    val willExpire = getNow + 10000

    val offerBuiler = TestUtils.createOffer(dealer = false)
    offerBuiler.getOfferAutoruBuilder
      .addServicesBuilder()
      .setIsActive(true)
      .setCreated(getNow - 20000)
      .setExpireDate(willExpire)
      .setServiceType(ServiceType.TOP)
    val offer = offerBuiler.build()

    val result = worker.shouldProcess(offer, None)
    result.shouldProcess shouldEqual false

    result.shouldReschedule.get.toMillis shouldBe 10000L +- 1000L
  }
}
