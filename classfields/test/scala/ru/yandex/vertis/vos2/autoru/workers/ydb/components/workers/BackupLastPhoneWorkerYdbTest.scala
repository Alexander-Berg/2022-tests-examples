package ru.yandex.vertis.vos2.autoru.workers.ydb.components.workers

import java.util.concurrent.atomic.AtomicReference
import ru.yandex.vertis.baker.components.workdistribution.WorkDistributionData
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.{EndpointConfig, LocalTracingSupport, Traced, TracingSupport}
import ru.yandex.vos2.autoru.model.TestUtils
import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class BackupLastPhoneWorkerYdbTest extends AnyWordSpec with MockitoSupport with Matchers with BeforeAndAfterAll {
  implicit val traced: Traced = Traced.empty

  abstract private class Fixture {

    val worker = new BackupLastPhoneWorkerYdb with YdbWorkerTestImpl
  }

  ("shouldProcess") in new Fixture {
    val offer1 = TestUtils.createOffer(dealer = false)
    assert(!worker.shouldProcess(offer1.build(), None).shouldProcess)

    val offer2 = TestUtils.createOffer(dealer = false)
    val phone: String = "79264445566"
    offer2.getOfferAutoruBuilder.getSellerBuilder.addPhoneBuilder().setNumber(phone)
    assert(worker.shouldProcess(offer2.build(), None).shouldProcess)
  }

  ("process") in new Fixture {
    val offer = TestUtils.createOffer(dealer = false)
    val phone: String = "79264445566"
    offer.getOfferAutoruBuilder.getSellerBuilder.addPhoneBuilder().setNumber(phone)

    val offer2 = worker.process(offer.build(), None).updateOfferFunc.get(offer.build())
    assert(offer2.getOfferAutoru.getLastPhone == phone)

    offer.getOfferAutoruBuilder.clearSeller().setLastPhone(phone)
    assert(worker.process(offer.build(), None).updateOfferFunc.isEmpty)
  }

}
