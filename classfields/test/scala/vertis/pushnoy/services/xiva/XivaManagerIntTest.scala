package vertis.pushnoy.services.xiva

import org.scalatest.{Assertion, BeforeAndAfterAll}
import org.scalatest.RecoverMethods.recoverToSucceededIf
import ru.yandex.vertis.generators.BasicGenerators.readableString
import ru.yandex.vertis.generators.ProducerProvider
import ru.yandex.vertis.util.concurrent.Futures
import vertis.pushnoy.dao.ydb.PushnoyYdbTest
import vertis.pushnoy.gen.ModelGenerators._
import vertis.pushnoy.model.ClientType.Auto
import vertis.pushnoy.model._
import vertis.pushnoy.model.exception.DevicesNotFoundException
import vertis.pushnoy.model.request.params.Devices
import vertis.pushnoy.model.request.{PushMessageV1, TokenInfo}
import vertis.pushnoy.model.response.{PushCounterResponse, PushHistoryResponse}
import vertis.pushnoy.model.template.Template
import vertis.pushnoy.services.{PushBanChecker, SubscriptionsManager}
import vertis.pushnoy.services.xiva.TestXivaClient.InvalidId
import vertis.pushnoy.util.DeviceNameUtils
import vertis.pushnoy.util.log.event.PushSentEventLog
import vertis.pushnoy.{PushnoySpecBase, TestClients, TestEnvironment}
import vertis.zio.test.ZioSpecBase

/** Created by Karpenko Maksim (knkmx@yandex-team.ru) on 12/07/2017.
  */
class XivaManagerIntTest
  extends ZioSpecBase
  with PushnoySpecBase
  with PushnoyYdbTest
  with TestEnvironment
  with TestClients
  with XivaManagerSupport
  with ProducerProvider
  with BeforeAndAfterAll {

  override lazy val deviceChecker: DeviceChecker = new DeviceCheckerImpl

  lazy val subscriptionsManager = new SubscriptionsManager(dao)

  private val sendEvent = PushSentEventLog()

  override def pushBanChecker: PushBanChecker = new PushBanChecker {
    override def isPushBanned(push: PushMessageV1): Boolean = false

    override def isPushBanned(template: Template): Boolean = false
  }

  "XivaManager" should {
    "add and get token information" in {
      val deviceId = DeviceGen.next
      val tokenInfo = TokenInfoGen.next

      xivaManager.addToken(deviceId, tokenInfo).futureValue

      val tokenInfoGet = xivaManager.getTokenInfo(deviceId).futureValue
      tokenInfoGet shouldBe tokenInfo
    }

    "add and get device information" in {
      val deviceId = DeviceGen.next
      val deviceInfo = DeviceInfoGen.next

      xivaManager.addDevice(deviceId, deviceInfo).futureValue

      val deviceInfoGet = xivaManager.getDeviceInfo(deviceId).futureValue
      deviceInfoGet shouldBe deviceInfo
    }

    "add and get fixed device information" in {
      val deviceId = DeviceGen.next

      val badDeviceName =
        readableString(DeviceNameUtils.maxDeviceNameLength + 1, DeviceNameUtils.maxDeviceNameLength * 2).next
      val deviceInfo = DeviceInfoGen.next.copy(name = badDeviceName)
      val fixedDeviceName = badDeviceName.take(DeviceNameUtils.maxDeviceNameLength)

      xivaManager.addDevice(deviceId, deviceInfo).futureValue

      val deviceInfoGet = xivaManager.getDeviceInfo(deviceId).futureValue
      deviceInfoGet.name shouldBe fixedDeviceName
    }

    "add device to user" in {
      val client = Auto

      val deviceId = deviceGen(client).next
      val tokenInfo = TokenInfoGen.next

      val deviceId2 = deviceGen(client).next
      val tokenInfo2 = TokenInfoGen.next

      val user = userGen(client).next

      xivaManager.addToken(deviceId, tokenInfo).futureValue
      xivaManager.addToken(deviceId2, tokenInfo2).futureValue
      xivaManager.addDevice(deviceId, DeviceInfoGen.next).futureValue
      xivaManager.addDevice(deviceId2, DeviceInfoGen.next).futureValue
      xivaManager.addUser(user, deviceId).futureValue
      xivaManager.addUser(user, deviceId2).futureValue

      checkUserDevices(user, Seq(tokenInfo, tokenInfo2))
    }

    "add device to user if someone already owns it" in {
      val client = Auto

      val deviceId = deviceGen(client).next
      val tokenInfo = TokenInfoGen.next

      val user = userGen(Auto).next
      val newUser = userGen(Auto).next

      xivaManager.addUser(user, deviceId).futureValue
      xivaManager.addDevice(deviceId, DeviceInfoGen.next).futureValue
      xivaManager.addToken(deviceId, tokenInfo).futureValue

      checkUserDevices(user, Seq(tokenInfo))

      xivaManager.addUser(newUser, deviceId).futureValue

      checkUserDevicesFailed(user)
      checkUserDevices(newUser, Seq(tokenInfo))
    }

    "detach device" in {
      val client = Auto

      val deviceId = deviceGen(client).next
      val tokenInfo = TokenInfoGen.next

      val deviceId2 = deviceGen(client).next
      val tokenInfo2 = TokenInfoGen.next

      val deviceId3 = deviceGen(client).next
      val tokenInfo3 = TokenInfoGen.next

      val user = userGen(client).next
      val user2 = userGen(client).next

      xivaManager.addToken(deviceId, tokenInfo).futureValue
      xivaManager.addToken(deviceId2, tokenInfo2).futureValue
      xivaManager.addToken(deviceId3, tokenInfo3).futureValue
      xivaManager.addDevice(deviceId, DeviceInfoGen.next).futureValue
      xivaManager.addDevice(deviceId2, DeviceInfoGen.next).futureValue
      xivaManager.addDevice(deviceId3, DeviceInfoGen.next).futureValue
      xivaManager.addUser(user, deviceId).futureValue
      xivaManager.addUser(user, deviceId2).futureValue

      checkUserDevices(user, Seq(tokenInfo, tokenInfo2))

      xivaManager.detachDevice(deviceId).futureValue

      checkUserDevices(user, Seq(tokenInfo2))

      xivaManager.addUser(user, deviceId3).futureValue
      checkUserDevices(user, Seq(tokenInfo2, tokenInfo3))

      xivaManager.addUser(user2, deviceId2).futureValue
      checkUserDevices(user2, Seq(tokenInfo2))
    }

    "push to device" in {
      val testDevice1 = DeviceGen.next
      val testTokenInfo = TokenInfoGen.next
      val testDeviceInfo = DeviceInfoGen.next
      val testPushMessage = PushMessageGen.next
      val pushName = readableString.next

      xivaManager.addToken(testDevice1, testTokenInfo).futureValue
      xivaManager.addDevice(testDevice1, testDeviceInfo).futureValue

      val pushResult = xivaManager
        .pushToDevice(testDevice1, testPushMessage, pushName = Some(pushName), event = sendEvent)
        .futureValue
      pushResult shouldBe PushCounterResponse(1)

      succeed
    }

    "push to unsubscribed device" in {
      val testDevice = DeviceGen.next.copy(id = InvalidId)
      val testTokenInfo = TokenInfoGen.next
      val testPushMessage = PushMessageGen.next
      val subscription = readableString.next

      xivaManager.addToken(testDevice, testTokenInfo).futureValue
      xivaManager.addDevice(testDevice, DeviceInfoGen.next).futureValue
      dao.updateDeviceSubscription(testDevice, subscription, disable = true).futureValue

      val pushResult =
        xivaManager.pushToDevice(testDevice, testPushMessage, Some(subscription), event = sendEvent).futureValue
      pushResult shouldBe PushCounterResponse(0)
    }

    "push to user" in {
      val testUser = UserGen.next
      val testDevice1 = deviceGen(testUser.client).next
      val testTokenInfo = TokenInfoGen.next
      val testDeviceInfo = DeviceInfoGen.next
      val testPushMessage = PushMessageGen.next
      val pushName = readableString.next

      xivaManager.addUser(testUser, testDevice1).futureValue
      xivaManager.addToken(testDevice1, testTokenInfo).futureValue
      xivaManager.addDevice(testDevice1, testDeviceInfo).futureValue

      val pushResult = xivaManager.pushToUser(testUser, Devices, testPushMessage, pushName = Some(pushName)).futureValue
      pushResult shouldBe PushCounterResponse(1)

      succeed
    }

    "push to user with many devices" in {
      val testUser = UserGen.next
      val count = 3
      val testDevices = deviceGen(testUser.client).next(count).toSeq
      val testTokenInfo = TokenInfoGen.next
      val testDeviceInfo = DeviceInfoGen.next
      val testPushMessage = PushMessageGen.next
      val pushName = readableString.next

      Futures
        .traverseSequential(testDevices) { device =>
          for {
            _ <- xivaManager.addUser(testUser, device)
            _ <- xivaManager.addDevice(device, testDeviceInfo)
            _ <- xivaManager.addToken(device, testTokenInfo)
          } yield ()
        }
        .futureValue

      val pushResult = xivaManager.pushToUser(testUser, Devices, testPushMessage, pushName = Some(pushName)).futureValue
      pushResult shouldBe PushCounterResponse(count)

      succeed
    }

    "push to user with bad devices" in {
      val testUser = UserGen.next
      val count = 3
      val testDevices = deviceGen(testUser.client).next(count).toSeq
      val testTokenInfo = TokenInfoGen.next
      val testDeviceInfo = DeviceInfoGen.next
      val testPushMessage = PushMessageGen.next
      val badInfo = testDeviceInfo.copy(appVersion = None)
      val pushName = readableString.next

      Futures
        .traverseSequential(testDevices) { device =>
          for {
            _ <- xivaManager.addUser(testUser, device)
            _ <- xivaManager.addDevice(device, badInfo)
            _ <- xivaManager.addToken(device, testTokenInfo)
          } yield ()
        }
        .futureValue

      xivaManager.pushToUser(testUser, Devices, testPushMessage, pushName = Some(pushName)).futureValue shouldBe
        PushCounterResponse(0)

      succeed
    }

    "push to user with unsubscribed devices" in {
      val testUser = UserGen.next
      val testDevice = deviceGen(testUser.client).next
      val testTokenInfo = TokenInfoGen.next
      val testPushMessage = PushMessageGen.next
      val subscription = readableString.next

      xivaManager.addUser(testUser, testDevice).futureValue
      xivaManager.addToken(testDevice, testTokenInfo).futureValue
      xivaManager.addDevice(testDevice, DeviceInfoGen.next).futureValue

      dao.updateDeviceSubscription(testDevice, subscription, disable = true).futureValue
      xivaManager.pushToUser(testUser, Devices, testPushMessage, Some(subscription)).futureValue shouldBe
        PushCounterResponse(0)
    }

    "push to user with hidden pushes" in {
      val testUser = UserGen.next
      val testDevice = deviceGen(testUser.client).next
      val testTokenInfo = TokenInfoGen.next.copy(hidden = true)
      val testPushMessage = PushMessageGen.next

      xivaManager.addUser(testUser, testDevice).futureValue
      xivaManager.addToken(testDevice, testTokenInfo).futureValue
      xivaManager.addDevice(testDevice, DeviceInfoGen.next).futureValue
      xivaManager.pushToUser(testUser, Devices, testPushMessage, None).futureValue shouldBe
        PushCounterResponse(0)
    }

    "push to user who owns inconsistent devices" in {
      val testUser = UserGen.next
      val client = testUser.client
      val testUser2 = userGen(client).next
      val testDevice = deviceGen(client).next
      val testPushMessage = PushMessageGen.next

      xivaManager.addToken(testDevice, TokenInfoGen.next).futureValue
      xivaManager.addDevice(testDevice, DeviceInfoGen.next).futureValue

      xivaManager.addUser(testUser, testDevice).futureValue
      xivaManager.addUser(testUser2, testDevice).futureValue

      xivaManager.pushToUser(testUser, Devices, testPushMessage, None).futureValue shouldBe
        PushCounterResponse(0)
      xivaManager.pushToUser(testUser2, Devices, testPushMessage, None).futureValue shouldBe
        PushCounterResponse(1)
    }

    "get history of unexisting device" in {
      recoverToSucceededIf[DevicesNotFoundException] {
        xivaManager.getPushHistory(DeviceGen.next, "testPushName")
      }
    }

    "get history" in {
      val testDevice = DeviceGen.next
      val testDeviceInfo = DeviceInfoGen.next

      xivaManager.addDevice(testDevice, testDeviceInfo).futureValue
      xivaManager.getPushHistory(testDevice, "testPushName").futureValue shouldBe
        PushHistoryResponse("testPushName", Seq.empty)
    }
  }

  private def checkUserDevices(user: User, devices: Seq[TokenInfo]): Assertion = {
    val userDevices = xivaManager.getUserDevicesFullInfo(user).map(_.flatMap(_.token)).futureValue
    userDevices should contain theSameElementsAs devices
  }

  private def checkUserDevicesFailed(user: User): Assertion =
    xivaManager
      .getUserDevicesFullInfo(user)
      .map(_.flatMap(_.token))
      .failed
      .futureValue shouldBe a[DevicesNotFoundException]
}
