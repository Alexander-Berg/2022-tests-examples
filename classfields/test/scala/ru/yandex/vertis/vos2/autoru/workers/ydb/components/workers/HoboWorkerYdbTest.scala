package ru.yandex.vertis.vos2.autoru.workers.ydb.components.workers

import org.mockito.Mockito.verify
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.hobo.proto.Model.{QueueId, Task}
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.mockito.MockitoSupport.{eq => eqq}
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vertis.vos2.model.qas2.QASMode
import ru.yandex.vos2.OfferModel.{OfferFlag, QASCheck, QASCheckStatus}
import ru.yandex.vos2.autoru.model.TestUtils
import ru.yandex.vos2.getNow
import ru.yandex.vos2.services.hobo.HoboClient

import scala.concurrent.duration.DurationInt
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class HoboWorkerYdbTest extends AnyWordSpec with MockitoSupport with Matchers with BeforeAndAfterAll {
  implicit val traced: Traced = Traced.empty

  abstract private class Fixture {

    val hoboClient = mockStrict[HoboClient]

    val worker = new HoboWorkerYdb(
      hoboClient,
      "vos-host",
      9999
    ) with YdbWorkerTestImpl
  }
  ("create task source") in new Fixture {
    val offer = TestUtils.createOffer().build()

    val check = QASCheck
      .newBuilder()
      .setKey(123L)
      .setQASService("test")
      .setQASMode(QASMode.MODE_POST)
      .setTimestampCreated(getNow)
      .build()

    val taskSource = worker.newHoboTask(offer, check)

    val userId = offer.getUserRef.stripPrefix("a_")
    taskSource.getResponse.getUrl shouldBe s"http://vos-host:9999/api/v0/hobo/user:$userId/${offer.getOfferID}"
  }

  ("pass ttl from qas_check") in new Fixture {
    val offer = TestUtils.createOffer().build()

    val check = QASCheck
      .newBuilder()
      .setKey(123L)
      .setQASService("test")
      .setQASMode(QASMode.MODE_POST)
      .setTimestampCreated(getNow)
      .setCheckTtlSeconds(1.day.toSeconds)
      .build()

    val taskSource = worker.newHoboTask(offer, check)

    val willExpire = getNow + 1.day.toMillis
    taskSource.getExpireTime shouldBe willExpire +- 1000L
  }

  ("ignore inactive offer") in new Fixture {

    val offer = TestUtils
      .createOffer()
      .addFlag(OfferFlag.OF_INACTIVE)

    offer
      .addQASCheckBuilder()
      .setKey(1)
      .setQASService("TEST_QUEUE")
      .setQASMode(QASMode.MODE_POST)
      .setTimestampCreated(0L)
      .setStatus(QASCheckStatus.SCHEDULED)

    val processingResult = worker.process(offer.build(), None)
    processingResult.updateOfferFunc shouldBe None

  }

  ("send active offer to hobo") in new Fixture {
    val offer = TestUtils.createOffer()

    offer
      .addQASCheckBuilder()
      .setKey(1)
      .setQASService("TEST_QUEUE")
      .setQASMode(QASMode.MODE_POST)
      .setTimestampCreated(0L)
      .setStatus(QASCheckStatus.SCHEDULED)

    val task = Task
      .newBuilder()
      .setVersion(1)
      .setKey("abc123")
      .build()

    when(hoboClient.createTask(?, ?)(?)).thenReturn(task)

    val processingResult = worker.process(offer.build(), None)
    val newOffer = processingResult.updateOfferFunc.get(offer.build)

    val check = newOffer.getQASCheck(0)
    check.getStatus shouldBe QASCheckStatus.SENT
    check.getHoboKey shouldBe "abc123"

    processingResult.nextCheck shouldBe None

    verify(hoboClient).createTask(eqq(QueueId.TEST_QUEUE), ?)(?)
  }

}
