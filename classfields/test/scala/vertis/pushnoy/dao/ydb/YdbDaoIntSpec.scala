package vertis.pushnoy.dao.ydb

import java.time.YearMonth
import org.scalatest.Assertion
import ru.yandex.vertis.generators.BasicGenerators.readableString
import vertis.pushnoy.gen.ModelGenerators._
import ru.yandex.vertis.generators.ProducerProvider
import vertis.pushnoy.model.ClientType.Auto
import vertis.pushnoy.model._
import vertis.pushnoy.model.request.DeviceInfo.IosDeviceCheckBits
import vertis.pushnoy.model.request.enums.ClientOS
import vertis.pushnoy.model.request.TokenInfo
import vertis.pushnoy.util.TestUtils
import vertis.zio.test.ZioSpecBase

/** @author kusaeva
  */
class YdbDaoIntSpec extends ZioSpecBase with PushnoyYdbTest with YdbSupport with ProducerProvider with TestUtils {

  "YdbDao" should {

    "add and get token information" in {
      val deviceId = deviceGen(Auto).next
      val deviceInfo = TokenInfoGen.next

      dao.addTokenInfo(deviceId, deviceInfo).futureValue
      val deviceInfoGet = dao.getTokenInfo(deviceId).futureValue

      deviceInfo shouldBe deviceInfoGet
    }

    "add and get device information" in {
      val deviceId = deviceGen(Auto).next
      val deviceInfo = DeviceInfoGen.next

      dao.addDeviceInfo(deviceId, deviceInfo).futureValue

      val deviceInfoGet = dao.getDeviceInfo(deviceId).futureValue
      deviceInfo shouldBe deviceInfoGet
    }

    "add device information should preserve ios device check bits" in {
      val deviceId = deviceGen(Auto).next
      val checkBits = IosDeviceCheckBits(true, true, Some(YearMonth.now))
      val deviceInfo = DeviceInfoGen.next.copy(clientOS = ClientOS.IOS, iosDeviceCheckBits = Some(checkBits))

      dao.addDeviceInfo(deviceId, deviceInfo).futureValue
      val deviceInfoGet = dao.getDeviceInfo(deviceId).futureValue
      deviceInfoGet shouldBe deviceInfo

      val newDeviceInfo = deviceInfo.copy(iosDeviceCheckBits = None)

      dao.addDeviceInfo(deviceId, newDeviceInfo).futureValue
      val newDeviceInfoGet = dao.getDeviceInfo(deviceId).futureValue
      newDeviceInfoGet.iosDeviceCheckBits shouldBe Some(checkBits)
    }

    "attach and detach device" in {

      val deviceId = deviceGen(Auto).next
      val tokenInfo = TokenInfoGen.next

      val deviceId2 = deviceGen(Auto).next
      val tokenInfo2 = TokenInfoGen.next

      val user = userGen(Auto).next

      dao.addDeviceInfo(deviceId, DeviceInfoGen.next).futureValue
      dao.addDeviceInfo(deviceId2, DeviceInfoGen.next).futureValue
      dao.addTokenInfo(deviceId, tokenInfo).futureValue
      dao.addTokenInfo(deviceId2, tokenInfo2).futureValue

      dao.attachDevice(user, deviceId).futureValue
      dao.attachDevice(user, deviceId2).futureValue

      checkUserDevicesTokens(user, Seq(tokenInfo, tokenInfo2))

      dao.detachDevice(user, deviceId).futureValue

      checkUserDevicesTokens(user, Seq(tokenInfo2))
    }

    "add and delete history" in {
      val deviceId = deviceGen(Auto).next
      val tokenInfo = TokenInfoGen.next
      val pushName = readableString.next

      dao.addTokenInfo(deviceId, tokenInfo).futureValue
      dao.savePushHistory(deviceId, pushName).futureValue

      val history = dao.getPushHistory(deviceId).futureValue.history

      assert(history.contains(pushName))
      history(pushName).length shouldBe 1

      dao.savePushHistory(deviceId, pushName).futureValue
      val history2 = dao.getPushHistory(deviceId).futureValue.history
      history2(pushName).length shouldBe 2

      val otherPushName = readableString.next

      dao.savePushHistory(deviceId, otherPushName).futureValue
      val history3 = dao.getPushHistory(deviceId).futureValue.history

      history3.keySet shouldBe Set(pushName, otherPushName)

      dao.deletePushHistory(deviceId, pushName).futureValue

      val history4 = dao.getPushHistory(deviceId).futureValue.history

      history4.keySet shouldBe Set(otherPushName)
    }

    "attach device also set owner" in {

      val device = Device(Auto, "device")
      val user = User(Auto, "user")

      dao.addDeviceInfo(device, DeviceInfoGen.next).futureValue

      dao.attachDevice(user, device).futureValue

      checkUserDevicesIds(user, Seq(device))
      dao.getDeviceOwner(device).futureValue shouldBe Some(user)
    }

    "return only owned devices" in {
      val client = Auto

      val deviceId = deviceGen(client).next
      val tokenInfo = TokenInfoGen.next

      val deviceId2 = deviceGen(client).next
      val tokenInfo2 = TokenInfoGen.next

      val user = userGen(client).next
      val user2 = userGen(client).next

      dao.addDeviceInfo(deviceId, DeviceInfoGen.next).futureValue
      dao.addDeviceInfo(deviceId2, DeviceInfoGen.next).futureValue
      dao.addTokenInfo(deviceId, tokenInfo).futureValue
      dao.addTokenInfo(deviceId2, tokenInfo2).futureValue

      dao.attachDevice(user, deviceId).futureValue
      dao.attachDevice(user, deviceId2).futureValue

      checkUserDevicesTokens(user, Seq(tokenInfo, tokenInfo2))
      checkUserDevicesIds(user, Seq(deviceId, deviceId2))

      dao.attachDevice(user2, deviceId2).futureValue

      checkUserDevicesTokens(user, Seq(tokenInfo))
      checkUserDevicesIds(user, Seq(deviceId))
      checkUserDevicesTokens(user2, Seq(tokenInfo2))
      checkUserDevicesIds(user2, Seq(deviceId2))
    }

    "return only owned devices even when data is inconsistent" in {
      val client = Auto

      val deviceId = deviceGen(client).next
      val tokenInfo = TokenInfoGen.next

      val deviceId2 = deviceGen(client).next
      val tokenInfo2 = TokenInfoGen.next

      val user = userGen(client).next
      val user2 = userGen(client).next

      dao.addDeviceInfo(deviceId, DeviceInfoGen.next).futureValue
      dao.addDeviceInfo(deviceId2, DeviceInfoGen.next).futureValue
      dao.addTokenInfo(deviceId, tokenInfo).futureValue
      dao.addTokenInfo(deviceId2, tokenInfo2).futureValue

      dao.attachDevice(user, deviceId).futureValue
      dao.attachDevice(user, deviceId2).futureValue

      checkUserDevicesTokens(user, Seq(tokenInfo, tokenInfo2))
      checkUserDevicesIds(user, Seq(deviceId, deviceId2))

      /** emulate inconsistency in database */
      runInTx(dao.devicesDao.removeOwner(deviceId2)).futureValue

      checkUserDevicesTokens(user, Seq(tokenInfo))
      checkUserDevicesIds(user, Seq(deviceId))

      /** emulate another inconsistency in database */
      runInTx(dao.devicesDao.setOwner(deviceId2, user2)).futureValue

      checkUserDevicesTokens(user, Seq(tokenInfo))
      checkUserDevicesIds(user, Seq(deviceId))

      val sub = "sub"
      dao.updateDeviceSubscription(deviceId2, sub, disable = false).futureValue

      checkUserDevicesIds(user, Seq(deviceId), Some(sub))
    }
  }

  private def checkUserDevicesTokens(user: User, tokens: Seq[TokenInfo]): Assertion = {
    val userTokens = dao.getUserDevicesFullInfo(user).map(_.flatMap(_.token)).futureValue
    userTokens should contain theSameElementsAs tokens
  }

  private def checkUserDevicesIds(user: User, devices: Seq[Device], subscription: Option[String] = None): Assertion = {
    val userDevices = dao.getUserDevicesId(user, subscription).futureValue
    userDevices should contain theSameElementsAs devices
  }
}
