package ru.yandex.vertis.passport.proto

import org.joda.time.DateTime
import org.scalatest.{BeforeAndAfter, OptionValues, WordSpec}
import ru.yandex.vertis.passport.model.visits.{UserWasSeenCounter, UserWasSeenCounters}
import ru.yandex.vertis.passport.test.SpecBase

class NoContextApiProtoFormatsSpec extends WordSpec with SpecBase with BeforeAndAfter with OptionValues {
  "UserWasSeenCountersFormat.read" should {
    "convert proto to case class correctly" in {
      val moment = DateTime.now().withMillisOfDay(0)
      val data1 = UserWasSeenCounters(
        ios = UserWasSeenCounter(1, moment.plusHours(1).toInstant, moment.plusHours(2).toInstant),
        android = UserWasSeenCounter(2, moment.plusHours(2).toInstant, moment.plusHours(3).toInstant),
        desktop = UserWasSeenCounter(3, moment.plusHours(3).toInstant, moment.plusHours(4).toInstant),
        mobile = UserWasSeenCounter(4, moment.plusHours(4).toInstant, moment.plusHours(5).toInstant),
        partner = UserWasSeenCounter(5, moment.plusHours(5).toInstant, moment.plusHours(6).toInstant),
        unknown = UserWasSeenCounter(6, moment.plusHours(6).toInstant, moment.plusHours(7).toInstant)
      )
      val proto = NoContextApiProtoFormats.UserWasSeenCountersFormat.write(data1)
      val data2 = NoContextApiProtoFormats.UserWasSeenCountersFormat.read(proto)
      data1 shouldBe data2
    }
  }
}
