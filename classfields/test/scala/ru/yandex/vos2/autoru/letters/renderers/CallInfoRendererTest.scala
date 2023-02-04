package ru.yandex.vos2.autoru.letters.renderers

import com.google.protobuf.util.Timestamps
import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import ru.auto.api.ApiOfferModel.Category
import ru.yandex.vertis.chat.model.api.ApiModel.MessageProperties
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vos2.autoru.model.TestUtils
import ru.yandex.vos2.model.UserRef
import ru.yandex.vos2.notifications.{CallParams, CallSource}
import scala.concurrent.duration._

@RunWith(classOf[JUnitRunner])
class CallInfoRendererTest extends AnyFunSuite with Matchers {
  implicit val trace = Traced.empty
  test("create notification for user +79999999999") {
    val offer = TestUtils.createOffer().build()

    val seller: UserRef = UserRef.from(offer.getUserRef)

    val initParams = CallParams(
      callerId = "user:89",
      duration = 1.minute.+(10.seconds),
      startTime = new DateTime(1585145598487L),
      outgoingPhoneNumber = "+79999999999",
      CallSource.Phone,
      None
    )

    val expMessage = "Звонок в 2020-03-25 17:13 мск длительностью 1 мин от ********9999"

    val notification = CallInfoRenderer.render(offer, initParams.asList)
    notification.chatSupport shouldBe None
    notification.mail shouldBe None
    notification.sms shouldBe None
    notification.push shouldBe None
    notification.offerChat shouldNot be(None)
    notification.offerChat match {
      case Some(template) =>
        val resProps: MessageProperties = template.messageProperties
        template.offerId should be(offer.getOfferID)
        template.category shouldBe Category.CARS
        template.author shouldBe "user:89"
        template.recipient shouldBe CallInfoRenderer.transformPassportToAutoruUser(seller.asPassportUser.passportUid)
        resProps.getCallInfo.getDuration shouldBe 70
        resProps.getCallInfo.getOutcomingPhoneNumber shouldBe "********9999"
        resProps.getCallInfo.getStartTime shouldBe Timestamps.fromMillis(1585145598487L)
        template.text shouldBe expMessage
        template.isSilent shouldBe true
      case _ => fail
    }
  }

  test("create notification about missed call") {
    val offer = TestUtils.createOffer().build()

    val seller: UserRef = UserRef.from(offer.getUserRef)

    val initParams = CallParams(
      callerId = "user:89",
      duration = 0.seconds,
      startTime = new DateTime(1585145598487L),
      outgoingPhoneNumber = "+79999999999",
      CallSource.Phone,
      None
    )

    val expMessage = "Пропущенный вызов в 2020-03-25 17:13 мск. Договоритесь с покупателем в чате"

    val notification = CallInfoRenderer.render(offer, initParams.asList)
    notification.chatSupport shouldBe None
    notification.mail shouldBe None
    notification.sms shouldBe None
    notification.push shouldBe None
    notification.offerChat shouldNot be(None)
    notification.offerChat match {
      case Some(template) =>
        val resProps: MessageProperties = template.messageProperties
        template.offerId should be(offer.getOfferID)
        template.category shouldBe Category.CARS
        template.author shouldBe "user:89"
        template.recipient shouldBe CallInfoRenderer.transformPassportToAutoruUser(seller.asPassportUser.passportUid)
        resProps.getCallInfo.getDuration shouldBe 0
        resProps.getCallInfo.getOutcomingPhoneNumber shouldBe "********9999"
        resProps.getCallInfo.getStartTime shouldBe Timestamps.fromMillis(1585145598487L)
        template.text shouldBe expMessage
        template.isSilent shouldBe false
      case _ => fail
    }
  }

  test("calculate duration") {
    CallInfoRenderer.toHumanReadable(0.second) shouldBe "0 сек"

    CallInfoRenderer.toHumanReadable(1.second) shouldBe "1 сек"
    CallInfoRenderer.toHumanReadable(1.minute) shouldBe "1 мин"
    CallInfoRenderer.toHumanReadable(1.hour) shouldBe "1 ч"

    CallInfoRenderer.toHumanReadable(20.seconds) shouldBe "20 сек"
    CallInfoRenderer.toHumanReadable(10.minutes) shouldBe "10 мин"
    CallInfoRenderer.toHumanReadable(2.hours) shouldBe "2 ч"

    CallInfoRenderer.toHumanReadable(2.minute.+(30.seconds)) shouldBe "2 мин"
    CallInfoRenderer.toHumanReadable(1.hours.+(30.minutes).+(24.seconds)) shouldBe "1 ч"
  }

  test("hide phone number") {
    CallInfoRenderer.hideNumber("+79999999999", "") shouldBe "********9999"
    CallInfoRenderer.hideNumber("1234", "") shouldBe "1234"
    CallInfoRenderer.hideNumber("12345", "") shouldBe "*2345"
  }

  test("transform passport user id to autoru user id with prefix user:") {
    CallInfoRenderer.transformPassportToAutoruUser("12") shouldBe "user:12"
    assertThrows[Exception] {
      CallInfoRenderer.transformPassportToAutoruUser("a12")
    }
  }
}
