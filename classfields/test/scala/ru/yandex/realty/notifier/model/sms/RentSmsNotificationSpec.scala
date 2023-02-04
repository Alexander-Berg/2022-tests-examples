package ru.yandex.realty.notifier.model.sms

import org.joda.time.LocalDate
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{FlatSpec, Matchers}
import ru.yandex.realty.notifier.model.sms.RentSmsNotification._

@RunWith(classOf[JUnitRunner])
class RentSmsNotificationSpec extends FlatSpec with Matchers {

  private val december3rd = new LocalDate(2021, 12, 3).toDateTimeAtStartOfDay
  private val january2nd = new LocalDate(2022, 1, 2).toDateTimeAtStartOfDay

  classOf[RentPaymentTodaySmsNotification].getName should "render correct text" in {
    val sms = RentPaymentTodaySmsNotification(december3rd, january2nd)
    sms.text shouldBe
      """Пора оплатить аренду за период 3 декабря - 2 января.
        |Не забудьте внести деньги до конца дня.
        |Это можно сделать в приложении: https://clck.ru/Z7HSZ""".stripMargin
  }

  classOf[RentPaymentOverdueSmsNotification].getName should "render correct text" in {
    val sms = RentPaymentOverdueSmsNotification(december3rd, january2nd)
    sms.text shouldBe
      """Оплата за период 3 декабря - 2 января просрочена.
        |Пожалуйста, внесите деньги как можно скорее.
        |Это можно сделать в приложении: https://clck.ru/Z7HWo""".stripMargin
  }

}
