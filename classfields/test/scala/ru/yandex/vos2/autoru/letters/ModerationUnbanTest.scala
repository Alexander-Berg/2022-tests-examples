package ru.yandex.vos2.autoru.letters

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.OptionValues
import org.scalatest.funsuite.AnyFunSuite
import play.api.libs.json.{JsObject, Json}
import ru.yandex.vos2.model.UserRef
import ru.yandex.vos2.services.phone.{SimpleSmsTemplateWithParams, ToPhoneDelivery}
import ru.yandex.vos2.services.sender.{EmailDeliveryParams, SenderTemplateWithParams, ToEmailDelivery}

/**
  * TODO
  *
  * @author aborunov
  */
@RunWith(classOf[JUnitRunner])
class ModerationUnbanTest extends AnyFunSuite with OptionValues {
  test("notification") {
    val name: String = "moderation_unban"
    val phone = "79291112233"
    val email = "example@example.com"
    val notification = ModerationUnban(
      UserRef.from("ac_123"),
      mark = "Hyundai",
      model = "ix35",
      smsText = Some("Покупатели снова видят ваше объявление о продаже МАРКА / МОДЕЛЬ на сайте."),
      chatText = Some("Покупатели снова видят ваше объявление о продаже МАРКА / МОДЕЛЬ на сайте."),
      senderTemplate = "moderation.unblock_success",
      paramDelivery = ToEmailDelivery(email),
      paramSms = Some(ToPhoneDelivery(phone))
    )
    assert(notification.mail.contains(new SenderTemplateWithParams {
      override val deliveryParams: EmailDeliveryParams = notification.paramDelivery

      override def name: String = notification.senderTemplate

      override def payload: JsObject = Json.obj(
        "mark_model" -> "Hyundai ix35"
      )
    }))

    assert(
      notification.sms.contains(
        SimpleSmsTemplateWithParams(
          id = name,
          smsParams = notification.paramSms,
          smsText = "Покупатели снова видят ваше объявление о продаже Hyundai ix35 на сайте."
        )
      )
    )

    assert(
      notification.chatSupport.value.text == "Покупатели снова видят ваше объявление о продаже Hyundai ix35 на сайте."
    )
  }
}
