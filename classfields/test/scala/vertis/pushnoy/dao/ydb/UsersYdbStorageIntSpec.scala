package vertis.pushnoy.dao.ydb

import ru.yandex.vertis.generators.ProducerProvider
import vertis.pushnoy.dao.ydb.YdbUtils.UserNotFound
import vertis.pushnoy.model.ClientType._
import vertis.pushnoy.model._
import vertis.pushnoy.gen.ModelGenerators._
import vertis.zio.test.ZioSpecBase

/** @author kusaeva
  */
class UsersYdbStorageIntSpec extends ZioSpecBase with PushnoyYdbTest with ProducerProvider {

  "UsersYdbStorage" should {

    "attach device" should {
      "to user" in ioTest {
        val user1 = UserGen.next
        val device1 = DeviceGen.next.copy(client = user1.client)
        val device2 = DeviceGen.next.copy(client = user1.client)
        val device3 = DeviceGen.next.copy(client = if (user1.client == Auto) Realty else Auto)
        for {
          _ <- ydbWrapper.runTx(usersStorage.attachDevice(user1, device1))
          _ <- ydbWrapper.runTx(usersStorage.attachDevice(user1, device2))
          _ <- ydbWrapper.runTx(usersStorage.attachDevice(user1, device3))
          devices <- ydbWrapper.runTx(usersStorage.getDevices(user1))
          owner1 <- ydbWrapper.runTx(devicesStorage.getOwner(device1))
          owner2 <- ydbWrapper.runTx(devicesStorage.getOwner(device2))
          _ <- check {
            devices should contain theSameElementsAs Seq(device1, device2)
            owner1.get shouldBe user1
            owner2.get shouldBe user1
          }
        } yield ()
      }
    }

    "detach device" should {
      "from user" in ioTest {
        val user1 = UserGen.next
        val device1 = DeviceGen.next.copy(client = user1.client)
        val device2 = DeviceGen.next.copy(client = user1.client)
        for {
          _ <- ydbWrapper.runTx(usersStorage.attachDevice(user1, device1))
          _ <- ydbWrapper.runTx(usersStorage.attachDevice(user1, device2))
          devices <- ydbWrapper.runTx(usersStorage.getDevices(user1))
          _ <- ydbWrapper.runTx(usersStorage.detachDevice(user1, device1))
          devicesNew <- ydbWrapper.runTx(usersStorage.getDevices(user1))
          _ <- check {
            devices should contain theSameElementsAs Seq(device1, device2)
            devicesNew should contain theSameElementsAs Seq(device2)
          }
        } yield ()
      }
    }

    "get" should {
      "devices" in ioTest {
        val user1 = UserGen.next
        val device1 = DeviceGen.next.copy(client = user1.client)
        val device2 = DeviceGen.next.copy(client = user1.client)
        val device3 = DeviceGen.next.copy(client = user1.client)
        for {
          _ <- ydbWrapper.runTx(usersStorage.attachDevice(user1, device1))
          _ <- ydbWrapper.runTx(usersStorage.attachDevice(user1, device2))
          _ <- ydbWrapper.runTx(usersStorage.attachDevice(user1, device3))
          devices <- ydbWrapper.runTx(usersStorage.getDevices(user1))
          _ <- check {
            devices should contain theSameElementsAs Seq(
              device1,
              device2,
              device3
            )
          }
        } yield ()
      }
      "devices full info" in ioTest {
        val user1 = UserGen.next
        val device1 = DeviceGen.next.copy(client = user1.client)
        val device2 = DeviceGen.next.copy(client = user1.client)
        val token = TokenInfoGen.next
        val deviceInfo = DeviceInfoGen.next
        val subscriptions1 = DisabledSubscriptionsGen.next
        val subscriptions2 = DisabledSubscriptionsGen.next
        for {
          _ <- ydbWrapper.runTx(usersStorage.attachDevice(user1, device1))
          _ <- ydbWrapper.runTx(usersStorage.attachDevice(user1, device2))
          _ <- ydbWrapper.runTx(devicesStorage.addTokenInfo(device1, token))
          _ <- ydbWrapper.runTx(devicesStorage.addDeviceInfo(device1, deviceInfo))
          _ <- ydbWrapper.runTx(devicesStorage.updateSubscriptions(device1, subscriptions1))
          _ <- ydbWrapper.runTx(devicesStorage.updateSubscriptions(device2, subscriptions2))
          devices <- ydbWrapper.runTx(usersStorage.getDevicesFullInfo(user1))
          _ <- check {
            devices should contain theSameElementsAs Seq(
              DeviceFullInfo(device1, Some(deviceInfo), Some(token), subscriptions1),
              DeviceFullInfo(device2, None, None, subscriptions2)
            )
          }
        } yield ()
      }
      "subscriptions" in ioTest {
        val user1 = UserGen.next
        val device1 = DeviceGen.next.copy(client = user1.client)
        val device2 = DeviceGen.next.copy(client = user1.client)
        val device3 = DeviceGen.next.copy(client = user1.client)
        val subscriptions1 = DisabledSubscriptionsGen.next
        val subscriptions2 = DisabledSubscriptionsGen.next
        for {
          _ <- ydbWrapper.runTx(usersStorage.attachDevice(user1, device1))
          _ <- ydbWrapper.runTx(usersStorage.attachDevice(user1, device2))
          _ <- ydbWrapper.runTx(usersStorage.attachDevice(user1, device3))
          _ <- ydbWrapper.runTx(devicesStorage.updateSubscriptions(device1, subscriptions1))
          _ <- ydbWrapper.runTx(devicesStorage.updateSubscriptions(device2, subscriptions2))
          devices <- ydbWrapper.runTx(usersStorage.getDevicesSubscriptions(user1))
          _ <- check {
            devices should contain theSameElementsAs Seq(
              DeviceFullInfo(device1, None, None, subscriptions1),
              DeviceFullInfo(device2, None, None, subscriptions2),
              DeviceFullInfo(device3, None, None, Set.empty)
            )
          }
        } yield ()
      }
    }

    "throw UserNotFound when no user on get" should {
      "devices" in {
        intercept[UserNotFound.type] {
          ioTest {
            val user = UserGen.next
            for {
              _ <- ydbWrapper.runTx(usersStorage.getDevices(user)).mapError(_.squash)
            } yield ()
          }
        }
      }
      "devices full info" in {
        intercept[UserNotFound.type] {
          ioTest {
            val user = UserGen.next
            for {
              _ <- ydbWrapper.runTx(usersStorage.getDevicesFullInfo(user)).mapError(_.squash)
            } yield ()
          }
        }
      }
      "subscriptions" in {
        intercept[UserNotFound.type] {
          ioTest {
            val user = UserGen.next
            for {
              _ <- ydbWrapper.runTx(usersStorage.getDevicesSubscriptions(user)).mapError(_.squash)
            } yield ()
          }
        }
      }
    }

    "throw UserNotFound when no devices on get" should {
      "devices" in {
        intercept[UserNotFound.type] {
          ioTest {
            val user1 = UserGen.next
            val device1 = DeviceGen.next.copy(client = user1.client)
            for {
              _ <- ydbWrapper.runTx(usersStorage.attachDevice(user1, device1))
              _ <- ydbWrapper.runTx(usersStorage.detachDevice(user1, device1))
              _ <- ydbWrapper.runTx(usersStorage.getDevices(user1)).mapError(_.squash)
            } yield ()
          }
        }
      }
      "devices full info" in {
        intercept[UserNotFound.type] {
          ioTest {
            val user1 = UserGen.next
            val device1 = DeviceGen.next.copy(client = user1.client)
            for {
              _ <- ydbWrapper.runTx(usersStorage.attachDevice(user1, device1))
              _ <- ydbWrapper.runTx(usersStorage.detachDevice(user1, device1))
              _ <- ydbWrapper.runTx(usersStorage.getDevicesFullInfo(user1)).mapError(_.squash)
            } yield ()
          }
        }
      }
      "subscriptions" in {
        intercept[UserNotFound.type] {
          ioTest {
            val user1 = UserGen.next
            val device1 = DeviceGen.next.copy(client = user1.client)
            for {
              _ <- ydbWrapper.runTx(usersStorage.attachDevice(user1, device1))
              _ <- ydbWrapper.runTx(usersStorage.detachDevice(user1, device1))
              _ <- ydbWrapper.runTx(usersStorage.getDevicesSubscriptions(user1)).mapError(_.squash)
            } yield ()
          }
        }
      }
    }

    "return devices full info when" should {
      "not all fields set" in ioTest {
        val user1 = UserGen.next
        val device1 = DeviceGen.next.copy(client = user1.client)
        val device2 = DeviceGen.next.copy(client = user1.client)
        for {
          _ <- ydbWrapper.runTx(usersStorage.attachDevice(user1, device1))
          _ <- ydbWrapper.runTx(usersStorage.attachDevice(user1, device2))
          devices <- ydbWrapper.runTx(usersStorage.getDevicesFullInfo(user1))
          _ <- check {
            devices should contain theSameElementsAs Seq(
              DeviceFullInfo(device1, None, None, Set.empty),
              DeviceFullInfo(device2, None, None, Set.empty)
            )
          }
        } yield ()
      }
    }
  }
}
