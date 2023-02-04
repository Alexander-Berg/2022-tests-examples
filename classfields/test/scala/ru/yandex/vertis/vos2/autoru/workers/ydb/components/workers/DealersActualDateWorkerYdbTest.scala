package ru.yandex.vertis.vos2.autoru.workers.ydb.components.workers

import java.util.concurrent.atomic.AtomicReference

import org.joda.time.DateTime
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.baker.components.workdistribution.WorkDistributionData
import ru.yandex.vertis.baker.components.workersfactory.workers.WorkersFactory
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.ops.OperationalSupport
import ru.yandex.vertis.tracing.{EndpointConfig, LocalTracingSupport, Traced, TracingSupport}
import ru.yandex.vertis.ydb.skypper.YdbWrapper
import ru.yandex.vos2.autoru.dao.offers.AutoruOfferDao
import ru.yandex.vos2.autoru.model.TestUtils
import ru.yandex.vos2.commonfeatures.FeaturesManager

import scala.concurrent.ExecutionContext
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class DealersActualDateWorkerYdbTest extends AnyWordSpec with MockitoSupport with Matchers with BeforeAndAfterAll {
  implicit val traced: Traced = Traced.empty

  abstract private class Fixture {

    val worker = new DealersActualDateWorkerYdb with YdbWorkerTestImpl
  }

  ("skip offers from private seller") in new Fixture {
    // убеждаемся, что объявление частного лица никак не меняется
    val offerFromUser = TestUtils.createOffer().build
    assert(!worker.shouldProcess(offerFromUser, None).shouldProcess)
  }

  ("actualize offers from dealers") in new Fixture {
    // убеждаемся, что в объявлении от салона меняется TimestampTtlStart и оно планируется к посещению через сутки
    val offerFromDealer = TestUtils.createOffer(dealer = true).build
    assert(worker.shouldProcess(offerFromDealer, None).shouldProcess)
    val result = worker.process(offerFromDealer, None)

    result.updateOfferFunc.get(offerFromDealer).getTimestampTtlStart shouldBe System.currentTimeMillis +- 1000
    (result.nextCheck.get.getMillis shouldBe (new DateTime().plusHours(24).getMillis +- 1000))
  }

}
