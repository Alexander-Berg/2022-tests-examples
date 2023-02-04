package vertis.pushnoy.dao.ydb

import ru.yandex.vertis.generators.ProducerProvider
import vertis.pushnoy.dao.ydb.YdbUtils.{DeviceNotFound, FieldNotFound}
import vertis.pushnoy.model.DeviceFullInfo
import vertis.pushnoy.gen.ModelGenerators._
import vertis.zio.test.ZioSpecBase

/** @author kusaeva
  */
class DevicesYdbStorageIntSpec extends ZioSpecBase with PushnoyYdbTest with ProducerProvider {

  "DevicesYdbStorage" should {
    "get" should {
      "token info" in ioTest {
        val device = DeviceGen.next
        val token = TokenInfoGen.next
        for {
          _ <- ydbWrapper.runTx(devicesStorage.addTokenInfo(device, token))
          res <- ydbWrapper.runTx(devicesStorage.getTokenInfo(device))
          _ <- check {
            res shouldBe token
          }
        } yield ()
      }
      "device info" in ioTest {
        val device = DeviceGen.next
        val deviceInfo = DeviceInfoGen.next
        for {
          _ <- ydbWrapper.runTx(devicesStorage.addDeviceInfo(device, deviceInfo))
          res <- ydbWrapper.runTx(devicesStorage.getDeviceInfo(device))
          _ <- check {
            res shouldBe deviceInfo
          }
        } yield ()
      }
      "subscriptions" in ioTest {
        val user = UserGen.next
        val device = DeviceGen.next
        val subscriptions = DisabledSubscriptionsGen.next
        for {
          _ <- ydbWrapper.runTx(usersStorage.attachDevice(user, device))
          _ <- ydbWrapper.runTx(devicesStorage.updateSubscriptions(device, subscriptions))
          res <- ydbWrapper.runTx(devicesStorage.getSubscriptions(device))
          _ <- check {
            res shouldBe subscriptions
          }
        } yield ()
      }
      "push history" in ioTest {
        val device = DeviceGen.next
        val history = PushHistoryGen.next
        for {
          _ <- ydbWrapper.runTx(devicesStorage.updatePushHistory(device, history))
          info <- ydbWrapper.runTx(devicesStorage.getPushHistory(device))
          _ <- check {
            info shouldBe history
          }
        } yield ()
      }
      "owner" in ioTest {
        val device = DeviceGen.next
        val user = UserGen.next.copy(client = device.client)
        for {
          _ <- ydbWrapper.runTx(usersStorage.attachDevice(user, device))
          res <- ydbWrapper.runTx(devicesStorage.getOwner(device))
          _ <- check {
            res shouldBe defined
            res.get shouldBe user
          }
        } yield ()
      }
      "full info" in ioTest {
        val user = UserGen.next
        val device = DeviceGen.next
        val info = DeviceInfoGen.next
        val token = TokenInfoGen.next
        val disabledSubscriptions = DisabledSubscriptionsGen.next
        for {
          _ <- ydbWrapper.runTx(usersStorage.attachDevice(user, device))
          _ <- ydbWrapper.runTx(devicesStorage.addDeviceInfo(device, info))
          _ <- ydbWrapper.runTx(devicesStorage.addTokenInfo(device, token))
          _ <- ydbWrapper.runTx(devicesStorage.updateSubscriptions(device, disabledSubscriptions))
          res <- ydbWrapper.runTx(devicesStorage.getFullInfo(device))
          _ <- check {
            res.device shouldBe device
            res.info shouldBe Some(info)
            res.token shouldBe Some(token)
            res.disabledSubscriptions shouldBe disabledSubscriptions
          }
        } yield ()
      }
    }

    "throw when no device on getting" should {
      "token info" in {
        intercept[DeviceNotFound.type] {
          ioTest {
            val device = DeviceGen.next
            for {
              _ <- ydbWrapper.runTx(devicesStorage.getTokenInfo(device)).mapError(_.squash)
            } yield ()
          }
        }
      }
      "device info" in {
        intercept[DeviceNotFound.type] {
          ioTest {
            val device = DeviceGen.next
            for {
              _ <- ydbWrapper.runTx(devicesStorage.getDeviceInfo(device)).mapError(_.squash)
            } yield ()
          }
        }
      }
      "subscriptions" in {
        intercept[DeviceNotFound.type] {
          ioTest {
            val device = DeviceGen.next
            for {
              _ <- ydbWrapper.runTx(devicesStorage.getSubscriptions(device)).mapError(_.squash)
            } yield ()
          }
        }
      }
      "owner" in {
        intercept[DeviceNotFound.type] {
          ioTest {
            val device = DeviceGen.next
            for {
              _ <- ydbWrapper.runTx(devicesStorage.getOwner(device)).mapError(_.squash)
            } yield ()
          }
        }
      }
      "push history" in {
        intercept[DeviceNotFound.type] {
          ioTest {
            val device = DeviceGen.next
            for {
              _ <- ydbWrapper.runTx(devicesStorage.getPushHistory(device)).mapError(_.squash)
            } yield ()
          }
        }
      }
      "full info" in {
        intercept[DeviceNotFound.type] {
          ioTest {
            val device = DeviceGen.next
            for {
              _ <- ydbWrapper.runTx(devicesStorage.getFullInfo(device)).mapError(_.squash)
            } yield ()
          }
        }
      }
    }

    "throw on getting field when no" should {
      "token info" in {
        intercept[FieldNotFound.type] {
          ioTest {
            val device = DeviceGen.next
            val user = UserGen.next
            for {
              _ <- ydbWrapper.runTx(usersStorage.attachDevice(user, device))
              _ <- ydbWrapper.runTx(devicesStorage.getTokenInfo(device)).mapError(_.squash)
            } yield ()
          }
        }
      }
      "device info" in {
        intercept[FieldNotFound.type] {
          ioTest {
            val device = DeviceGen.next
            val user = UserGen.next
            for {
              _ <- ydbWrapper.runTx(usersStorage.attachDevice(user, device))
              _ <- ydbWrapper.runTx(devicesStorage.getDeviceInfo(device)).mapError(_.squash)
            } yield ()
          }
        }
      }
      "push history" in {
        intercept[FieldNotFound.type] {
          ioTest {
            val device = DeviceGen.next
            val user = UserGen.next
            for {
              _ <- ydbWrapper.runTx(usersStorage.attachDevice(user, device))
              _ <- ydbWrapper.runTx(devicesStorage.getPushHistory(device)).mapError(_.squash)
            } yield ()
          }
        }
      }
    }

    "return none when" should {
      "owner not set" in ioTest {
        val device = DeviceGen.next
        val user = UserGen.next
        for {
          _ <- ydbWrapper.runTx(usersStorage.attachDevice(user, device))
          _ <- ydbWrapper.runTx(usersStorage.detachDevice(user, device))
          res <- ydbWrapper.runTx(devicesStorage.getOwner(device))
          _ <- check {
            res shouldBe empty
          }
        } yield ()
      }
    }

    "return empty when" should {
      "subscriptions not set" in ioTest {
        val user = UserGen.next
        val device = DeviceGen.next
        for {
          _ <- ydbWrapper.runTx(usersStorage.attachDevice(user, device))
          res <- ydbWrapper.runTx(devicesStorage.getSubscriptions(device))
          _ <- check {
            res shouldBe Set()
          }
        } yield ()
      }
    }

    "not throw on update subscriptions when" should {
      "no device" in ioTest {
        val device = DeviceGen.next
        val subscriptions = DisabledSubscriptionsGen.next
        for {
          _ <- ydbWrapper.runTx(devicesStorage.updateSubscriptions(device, subscriptions))
        } yield ()
      }
    }

    "return default values for empty fields when" should {
      "get full info" in ioTest {
        val user = UserGen.next
        val device = DeviceGen.next
        for {
          _ <- ydbWrapper.runTx(usersStorage.attachDevice(user, device))
          res <- ydbWrapper.runTx(devicesStorage.getFullInfo(device))
          _ <- check {
            res.device shouldBe device
            res.info shouldBe None
            res.token shouldBe None
            res.disabledSubscriptions shouldBe Set.empty
          }
        } yield ()
      }
    }
  }

  "set" should {
    "full info" in ioTest {
      val device = DeviceGen.next
      val info = DeviceInfoGen.next
      val token = TokenInfoGen.next
      val disabledSubscriptions = DisabledSubscriptionsGen.next
      val fullInfo = DeviceFullInfo(device, info = Some(info), token = Some(token), disabledSubscriptions)
      for {
        _ <- ydbWrapper.runTx(devicesStorage.setFullInfo(device, fullInfo))
        res <- ydbWrapper.runTx(devicesStorage.getFullInfo(device))
        _ <- check {
          res.device shouldBe device
          res.info shouldBe Some(info)
          res.token shouldBe Some(token)
          res.disabledSubscriptions shouldBe disabledSubscriptions
        }
      } yield ()
    }
    "full info when some fields are none" in ioTest {
      val device = DeviceGen.next
      val disabledSubscriptions = DisabledSubscriptionsGen.next
      val info = DeviceInfoGen.next
      val fullInfo = DeviceFullInfo(device, info = Some(info), token = None, disabledSubscriptions)
      for {
        _ <- ydbWrapper.runTx(devicesStorage.setFullInfo(device, fullInfo))
        res <- ydbWrapper.runTx(devicesStorage.getFullInfo(device))
        _ <- check {
          res.device shouldBe device
          res.info shouldBe Some(info)
          res.token shouldBe None
          res.disabledSubscriptions shouldBe disabledSubscriptions
        }
      } yield ()
    }
  }
}
