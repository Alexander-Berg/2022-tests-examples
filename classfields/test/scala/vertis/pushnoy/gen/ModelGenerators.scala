package vertis.pushnoy.gen

import org.scalacheck.Gen
import org.scalacheck.Gen.Choose
import ru.yandex.vertis.generators.BasicGenerators.readableString
import vertis.pushnoy.model._
import vertis.pushnoy.model.request.enums.ClientOS.ClientOS
import vertis.pushnoy.model.request.enums.{ClientOS, Platform}
import vertis.pushnoy.model.request.{DeviceInfo, PushMessageV1, TokenInfo}
import spray.json.{JsObject, JsString}

object ModelGenerators {

  def deviceGen(clientType: ClientType): Gen[Device] =
    for {
      id <- readableString
    } yield Device(clientType, id)

  def deviceInfoGen(clientOS: ClientOS): Gen[DeviceInfo] =
    for {
      fingerprint <- readableString
      manufacturer <- readableString
      brand <- readableString
      model <- readableString
      device <- readableString
      product <- readableString
      name <- readableString
      appVersion <- Gen.listOfN(3, Gen.choose(4, 20))
    } yield DeviceInfo(
      fingerprint,
      manufacturer,
      brand,
      model,
      device,
      product,
      clientOS,
      name,
      Some(appVersion.mkString("."))
    )

  def userGen(clientType: ClientType): Gen[User] =
    for {
      id <- readableString
    } yield User(clientType, id)

  val ClientTypeGen: Gen[ClientType] =
    Gen.oneOf(ClientType.Auto, ClientType.Realty)

  val DeviceGen: Gen[Device] = for {
    clientType <- ClientTypeGen
    device <- deviceGen(clientType)
  } yield device

  val DeviceInfoGen: Gen[DeviceInfo] = for {
    clientOS <- Gen.oneOf(ClientOS.values.toSeq)
    deviceInfo <- deviceInfoGen(clientOS)
  } yield deviceInfo

  val JsObjectGen: Gen[JsObject] = for {
    key <- readableString
    value <- readableString
  } yield JsObject(key -> JsString(value))

  val JsStringGen: Gen[JsString] = for {
    value <- readableString
  } yield JsString(value)

  val PushMessagePayloadGen: Gen[JsObject] =
    JsObject(
      "title" -> JsString("title"),
      "body" -> JsString("body"),
      "url" -> JsString("url"),
      "action" -> JsString("deeplink"),
      "push_name" -> JsString("pushName")
    )

  val PushMessageRepackGen: Gen[JsObject] =
    JsObject(
      "apns" -> JsObject(
        "aps" -> JsObject(
          "alert" -> JsObject(
            "title" -> JsString("title"),
            "body" -> JsString("body")
          ),
          "user_info" -> JsObject(
            "url" -> JsString("url"),
            "action" -> JsString("deeplink"),
            "push_name" -> JsString("pushName")
          )
        )
      )
    )

  val PushMessageGen: Gen[PushMessageV1] = for {
    payload <- PushMessagePayloadGen
    event <- readableString
    ttl <- Gen.size
    repack <- PushMessageRepackGen
    tags <- JsObjectGen
    keys <- JsObjectGen
  } yield PushMessageV1(payload, event, Some(ttl), Some(repack), Some(tags), Some(keys))

  val TokenInfoGen: Gen[TokenInfo] = for {
    uuid <- readableString
    platform <- Gen.oneOf(Platform.values.toSeq)
    pushToken <- readableString
  } yield TokenInfo(uuid, platform, pushToken, hidden = false)

  val DeviceFullInfoGen: Gen[DeviceFullInfo] = for {
    device <- DeviceGen
    info <- Gen.some(DeviceInfoGen)
    token <- Gen.some(TokenInfoGen)
  } yield DeviceFullInfo(device, info, token)

  val UserGen: Gen[User] = for {
    clientType <- ClientTypeGen
    id <- readableString
  } yield User(clientType, id)

  val DisabledSubscriptionsGen: Gen[Set[String]] = for {
    length <- Gen.choose(0, 5)
    subscriptions <- Gen.listOfN(length, readableString)
  } yield subscriptions.toSet

  val HistoryElementGen: Gen[(String, Seq[Long])] = for {
    length <- Gen.choose(0, 5)
    str <- readableString
    timestamps <- Gen.listOfN[Long](length, Choose.chooseLong.choose(0L, Long.MaxValue))
  } yield str -> timestamps

  val PushHistoryGen: Gen[PushHistory] = for {
    length <- Gen.choose(0, 10)
    history <- Gen.listOfN(length, HistoryElementGen)
  } yield PushHistory(history.toMap)

}
