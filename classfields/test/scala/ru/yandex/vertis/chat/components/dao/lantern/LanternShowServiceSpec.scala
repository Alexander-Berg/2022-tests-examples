package ru.yandex.vertis.chat.components.dao.lantern

import java.util.concurrent.atomic.AtomicReference

import scala.concurrent.duration._

import org.joda.time.DateTime
import ru.yandex.vertis.chat.SpecBase
import ru.yandex.vertis.chat.model.ModelGenerators.userId
import ru.yandex.vertis.chat.util.test.RequestContextAware
import ru.yandex.vertis.generators.ProducerProvider

trait LanternShowServiceSpec extends SpecBase with RequestContextAware with ProducerProvider {

  def lanternShowService: LanternShowService

  "Lantern Show Service" should {

    val user = userId.next
    val time = new AtomicReference(DateTime.now())

    "show lantern if no record in table" in {
      withUserContext(user) { implicit rc =>
        lanternShowService.canShowLantern(user, time.get(), Overload, 10.seconds).futureValue shouldBe true

      }
    }

    "add record to table" in {
      withUserContext(user) { implicit rc =>
        lanternShowService.setLanternShowed(user, time.get(), Overload).futureValue
      }
    }

    "not show lantern if not enough time passed since previous" in {
      withUserContext(user) { implicit rc =>
        lanternShowService
          .canShowLantern(user, time.get().plusSeconds(1), Overload, 10.seconds)
          .futureValue shouldBe false
      }
    }

    "show lantern if enough time passed since previous" in {
      lanternShowService
        .canShowLantern(user, time.get().plusSeconds(11), Overload, 10.seconds)
        .futureValue shouldBe true
    }
  }

}
