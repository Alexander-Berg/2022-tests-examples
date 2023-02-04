package ru.yandex.vertis.feedprocessor.autoru.scheduler.services.salesman

import org.scalatest.concurrent.Futures
import ru.yandex.vertis.feedprocessor.WordSpecBase
import ru.yandex.vertis.feedprocessor.autoru.scheduler.services.salesman.SalesmanClient.Schedule
import ru.yandex.vertis.feedprocessor.autoru.scheduler.util.HttpClientSuite
import ru.yandex.vertis.feedprocessor.http.HttpClientConfig

import java.time.LocalTime
import java.util.TimeZone

/**
  * @author pnaydenov
  */
class SalesmanClientIntTest extends WordSpecBase with HttpClientSuite with Futures {

  override protected def config: HttpClientConfig =
    HttpClientConfig("salesman-api-http-api.vrts-slb.test.vertis.yandex.net", 80)

  val client = new SalesmanClientImpl(http, "autoru")

  "SalesmanClientImpl" should {
    "handle schedules" in {
      val schedule1 =
        Schedule("1078322505-27229fc0", List(1, 2, 3), LocalTime.of(10, 35, 0), TimeZone.getTimeZone("GMT+03:00"))
      val schedule2 =
        Schedule("1080837394-dc5731e0", List(1, 2, 3, 4, 5), LocalTime.of(15, 45, 0), TimeZone.getTimeZone("GMT+03:00"))

      client.deleteSchedule(7808, "1078322505-27229fc0").futureValue
      client.deleteSchedule(7808, "1080837394-dc5731e0").futureValue

      client
        .getSchedules(7808, Seq("1078322505-27229fc0", "1080837394-dc5731e0", "1-foobar"))
        .futureValue shouldBe empty

      client.addSchedule(7808, schedule1).futureValue

      val schedules1 = client.getSchedules(7808, Seq("1078322505-27229fc0", "1080837394-dc5731e0")).futureValue
      schedules1 should have size (1)
      schedules1.head shouldEqual schedule1

      client.addSchedule(7808, schedule2).futureValue

      val schedules2 = client.getSchedules(7808, Seq("1078322505-27229fc0", "1080837394-dc5731e0")).futureValue
      schedules2 should have size (2)
      schedules2.toSet shouldEqual Set(schedule1, schedule2)

      client.deleteSchedule(7808, "1078322505-27229fc0").futureValue

      val schedules3 = client.getSchedules(7808, Seq("1078322505-27229fc0", "1080837394-dc5731e0")).futureValue
      schedules3 should have size (1)
      schedules1.head shouldEqual schedule1
    }
  }
}
