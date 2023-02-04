package vertis.pushnoy.services

import vertis.pushnoy.gen.ModelGenerators._
import vertis.pushnoy.model.ClientType.Auto
import vertis.pushnoy.model.{Device, DeviceFullInfo}
import vertis.pushnoy.model.request.{DeviceInfo, PushMessageV1}
import vertis.pushnoy.model.request.enums.{AppVersionCompare, ClientOS}
import vertis.pushnoy.model.request.params.AppVersionLimit
import vertis.pushnoy.services.xiva.DeviceCheckerImpl
import vertis.pushnoy.services.xiva.DeclinePushReason._
import ru.yandex.vertis.generators.ProducerProvider
import vertis.pushnoy.PushnoySuiteBase

/** @author kusaeva
  */
class DeviceCheckerTest extends PushnoySuiteBase with ProducerProvider {
  private val testPushMessage: PushMessageV1 = PushMessageGen.next

  private val pushName = testPushMessage.payload.fields.get("push_name").map(_.toString)

  private val device = Device(client = Auto, id = "test_id")

  private val goodInfo =
    DeviceInfo("test", "test", "test", "test", "test", "test", ClientOS.ANDROID, "test", Some("5.2"))

  private val tokenInfo = TokenInfoGen.next

  private val defaultFullInfo = DeviceFullInfo(device, Some(goodInfo), Some(tokenInfo), Set.empty)

  private val subscription = Some("subscription")

  private val checker = new DeviceCheckerImpl

  test("pass good device") {
    checker.check(defaultFullInfo, testPushMessage, subscription).isRight shouldBe true
  }

  test("do not pass device with bad version") {
    val badInfo = goodInfo.copy(appVersion = Some(DeviceCheckerImpl.badVersion))
    val fullInfo = defaultFullInfo.copy(info = Some(badInfo))

    checker.check(fullInfo, testPushMessage, subscription).left.value shouldBe
      BadDevice(
        device,
        version = Some(DeviceCheckerImpl.badVersion),
        pushName = pushName
      )
  }

  test("do not pass device without version") {
    val noVersionInfo = goodInfo.copy(appVersion = None)
    val fullInfo = defaultFullInfo.copy(info = Some(noVersionInfo))

    checker.check(fullInfo, testPushMessage, subscription).left.value shouldBe
      NoVersion(device)
  }

  test("do not pass unsubscribed device") {
    val subscriptionName = subscription.get
    val fullInfo = defaultFullInfo.copy(disabledSubscriptions = Set(subscriptionName))

    checker.check(fullInfo, testPushMessage, subscription).left.value shouldBe
      Unsubscribed(device, subscriptionName)
  }

  test("do not pass when incompatible device and push versions") {
    val versionLimit = AppVersionLimit(Some("1.1"), Some("1.1"), AppVersionCompare.GT)
    val version = "1.0"
    val pushMessage = testPushMessage.copy(app_version = Some(versionLimit))
    val noVersionInfo = goodInfo.copy(appVersion = Some(version))
    val fullInfo = defaultFullInfo.copy(info = Some(noVersionInfo))

    checker.check(fullInfo, pushMessage, subscription).left.value shouldBe
      IncompatibleVersion(device, versionLimit, version)
  }

  test("pass device if hidden = false") {
    val fullInfo = defaultFullInfo.copy(token = Some(tokenInfo.copy(hidden = false)))

    checker.check(fullInfo, testPushMessage, subscription).isRight shouldBe true
  }

  test("do not pass device if hidden = true") {
    val fullInfo = defaultFullInfo.copy(token = Some(tokenInfo.copy(hidden = true)))

    checker.check(fullInfo, testPushMessage, subscription).left.value shouldBe
      PushesHidden(device)
  }

}
