package vertis.pushnoy.model

import ru.yandex.pushnoy.push_request_model.{PushMessage, PushRequest, XivaInfo}
import vertis.pushnoy.PushnoySuiteBase
import vertis.pushnoy.model.template.{ContactTheSellerTemplate, SeveralDaysInactivityTemplate}

/** Created by Karpenko Maksim (knkmx@yandex-team.ru) on 03/05/2018.
  */
class TemplateTest extends PushnoySuiteBase {

  test("severalDaysInactivity") {
    val template = new SeveralDaysInactivityTemplate("mark", "model", Some("markName"), Some("modelName"))

    val xivaInfo = XivaInfo("Появились новые объявления")
    val androidMessage = PushMessage(
      deeplink = "autoru://app/cars/mark/model/all",
      title = "markName modelName",
      text = "Появились новые объявления"
    )
    val iosMessage = PushMessage(
      deeplink = "autoru://app/cars/mark/model/all",
      text = "Пока вас не было, по markName modelName появились новые объявления"
    )
    val request = PushRequest(
      messageName = "severalDaysInactivity",
      xiva = Some(xivaInfo),
      androidMessage = Some(androidMessage),
      iosMessage = Some(iosMessage)
    )

    template.toPushRequest() shouldBe request
  }

  test("contactTheSeller") {
    val template = new ContactTheSellerTemplate("1", false, "mark", "model", Some("markName"), Some("modelName"))

    val deepLink = s"autoru://app/cars/used/sale/1"
    val xivaInfo = XivaInfo("Вернись в объяву и позвони")

    val message =
      PushMessage(title = "Заинтересовались markName modelName?", deeplink = deepLink, text = "Напишите продавцу")

    val request = PushRequest(
      messageName = "contactTheSeller",
      xiva = Some(xivaInfo),
      iosMessage = Some(message),
      androidMessage = Some(message)
    )

    template.toPushRequest() shouldBe request
  }

}
