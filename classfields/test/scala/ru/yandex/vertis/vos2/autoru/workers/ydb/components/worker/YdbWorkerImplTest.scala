package ru.yandex.vertis.vos2.autoru.workers.ydb.components.worker

import ru.yandex.vertis.tracing.Traced
import ru.yandex.vertis.vos2.autoru.workers.ydb.components.workers.YdbWorkerTestImpl
import ru.yandex.vos2.OfferModel.{Offer, WorkerVersion}
import ru.yandex.vos2.autoru.model.TestUtils

import scala.jdk.CollectionConverters._
import org.junit.runner.RunWith
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class YdbWorkerImplTest extends AnyWordSpec with Matchers {

  object ydbWorker extends YdbWorkerImpl with YdbWorkerTestImpl {

    override def shouldProcess(offer: Offer, state: Option[String]): YdbShouldProcessResult =
      YdbShouldProcessResult(true)

    override def process(offer: Offer, state: Option[String])(implicit trace: Traced): YdbWorkerResult =
      YdbWorkerResult(None, None)
  }

  abstract private class Fixture {
    val offer: Offer
  }

  "YdbWorkerImpl" should {

    "increment test" in new Fixture {
      val offerBuilder = TestUtils.createOffer()
      val startVersion = 1
      val workerName2 = ydbWorker.token.name + "test"
      val workerVersion = WorkerVersion.newBuilder().setWorkerName(ydbWorker.token.name).setUpdateVersion(startVersion)
      val workerVersion2 =
        WorkerVersion.newBuilder().setWorkerName(workerName2).setUpdateVersion(startVersion + 2)
      (1 to 5).foreach(_ => offerBuilder.addWorkerVersion(workerVersion))
      (1 to 5).foreach(_ => offerBuilder.addWorkerVersion(workerVersion2))
      val offer = offerBuilder.build()
      assert(offer.getWorkerVersionCount == 10)
      val resOffer = ydbWorker.incrementVersion(offer)._1
      assert(resOffer.getWorkerVersionCount == 2)
      assert(
        resOffer.getWorkerVersionList.asScala
          .find(_.getWorkerName == ydbWorker.token.name)
          .get
          .getUpdateVersion == startVersion + 1
      )
      assert(
        resOffer.getWorkerVersionList.asScala
          .find(_.getWorkerName == workerName2)
          .get
          .getUpdateVersion == startVersion + 2
      )

    }
  }

}
