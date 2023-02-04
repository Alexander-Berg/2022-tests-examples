package vertis.pushnoy.util

import org.scalatest.funsuite.AsyncFunSuite
import ru.yandex.pushnoy.push_request_model.{PushMessage, PushRequest, XivaInfo}
import vertis.pushnoy.util.PushRequestUtils._
import spray.json._

class PushRequestUtilsTest extends AsyncFunSuite {

  test("convert with title") {
    val xivaInfo = XivaInfo(pushName = "Дозаполни объявление")
    val pushMessage = PushMessage(
      title = "Ваше объявление почти готово",
      text = "Пара минут на заполнение — и покупатели начнут вам звонить",
      deeplink = "autoru://app/cars/used/add"
    )
    val pushRequest = PushRequest(
      xiva = Some(xivaInfo),
      androidMessage = Some(pushMessage),
      iosMessage = Some(pushMessage)
    )

    val result =
      """{"payload":{"body":"Пара минут на заполнение — и покупатели начнут вам звонить","url":"autoru://app/cars/used/add","title":"Ваше объявление почти готово","push_name":"Дозаполни объявление","action":"deeplink"},"event":"test","repack":{"apns":{"aps":{"alert":{"title":"Ваше объявление почти готово","body":"Пара минут на заполнение — и покупатели начнут вам звонить"},"mutable-content":1,"sound":"default","badge":1,"user_info":{"url":"autoru://app/cars/used/add","push_name":"Дозаполни объявление","action":"deeplink"}},"repack_payload":["push_name"]},"gcm":{"repack_payload":["url","action","body","push_name","title"]}},"meta":{}}"""

    assert(
      pushRequest.toPushMessage.toJson.asJsObject == JsonParser(result).asJsObject
    )
  }

  test("convert without title") {
    val xivaInfo = XivaInfo("Появились новые объявления")
    val androidMessage = PushMessage(
      title = "Acura TL",
      text = "Появились новые объявления",
      deeplink = "autoru://app/cars/acura/tl/all"
    )
    val iosMessage = PushMessage(
      text = "Пока вас не было, по Acura TL появились новые объявления",
      deeplink = "autoru://app/cars/acura/tl/all"
    )
    val pushRequest = PushRequest(
      xiva = Some(xivaInfo),
      androidMessage = Some(androidMessage),
      iosMessage = Some(iosMessage)
    )

    val result =
      """{"payload":{"body":"Появились новые объявления","url":"autoru://app/cars/acura/tl/all","title":"Acura TL","push_name":"Появились новые объявления","action":"deeplink"},"event":"test","repack":{"apns":{"aps":{"alert":{"body":"Пока вас не было, по Acura TL появились новые объявления"},"mutable-content":1,"sound":"default","badge":1,"user_info":{"url":"autoru://app/cars/acura/tl/all","push_name":"Появились новые объявления","action":"deeplink"}},"repack_payload":["push_name"]},"gcm":{"repack_payload":["url","action","body","push_name","title"]}},"meta":{}}"""

    assert(
      pushRequest.toPushMessage.toJson.asJsObject == JsonParser(result).asJsObject
    )
  }
}
