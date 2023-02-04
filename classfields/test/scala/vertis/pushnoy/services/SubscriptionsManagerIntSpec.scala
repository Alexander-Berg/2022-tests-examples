package vertis.pushnoy.services

import org.scalatest.Assertion
import vertis.pushnoy.TestEnvironment
import vertis.pushnoy.dao.ydb.PushnoyYdbTest
import vertis.pushnoy.gen.ModelGenerators._
import vertis.pushnoy.model.exception.DevicesNotFoundException
import vertis.pushnoy.util.GeneratorUtils._
import vertis.zio.test.ZioSpecBase

import scala.concurrent.Future

class SubscriptionsManagerIntSpec extends ZioSpecBase with PushnoyYdbTest with TestEnvironment {

  private lazy val subscriptionsManager = new SubscriptionsManager(dao)

  def ftest(f: Future[Assertion]): Assertion =
    f.futureValue

  "SubscriptionsManager" should {
    "give disabled subscriptions" in ftest {
      val device = DeviceGen.next

      for {
        _ <- dao.addDeviceInfo(device, DeviceInfoGen.next)
        res <- subscriptionsManager.getDisabledSubscriptions(device)
      } yield res shouldBe empty
    }

    "raises exception" in {
      val unknownDevice = DeviceGen.next

      subscriptionsManager
        .getDisabledSubscriptions(unknownDevice)
        .failed
        .futureValue shouldBe a[DevicesNotFoundException]
      subscriptionsManager
        .enableSubscription(unknownDevice, "someSubscription")
        .failed
        .futureValue shouldBe a[DevicesNotFoundException]
    }

    "enable subscription" in ftest {
      val device = DeviceGen.next

      for {
        _ <- dao.addDeviceInfo(device, DeviceInfoGen.next)
        _ <- subscriptionsManager.enableSubscription(device, "offerPriceChanged")
        res <- subscriptionsManager.getDisabledSubscriptions(device)
      } yield res shouldBe empty

    }

    "disable subscription" in ftest {
      val device = DeviceGen.next

      for {
        _ <- dao.addDeviceInfo(device, DeviceInfoGen.next)
        _ <- subscriptionsManager.disableSubscription(device, "sub")
        res <- subscriptionsManager.getDisabledSubscriptions(device)
      } yield res should contain theSameElementsAs Set("sub")

    }

    "update moved subscription" in ftest {
      val device = DeviceGen.next

      for {
        _ <- dao.addDeviceInfo(device, DeviceInfoGen.next)
        _ <- subscriptionsManager.disableSubscription(device, "offerPriceChanged")
        disabled <- subscriptionsManager.getDisabledSubscriptions(device)
        _ <- subscriptionsManager.enableSubscription(device, "offerPriceChanged")
        empty <- subscriptionsManager.getDisabledSubscriptions(device)
        _ <- subscriptionsManager.disableSubscription(device, "offerPriceChanged")
        _ <- subscriptionsManager.enableSubscription(device, "offerChanged")
        newEmpty <- subscriptionsManager.getDisabledSubscriptions(device)
        _ <- subscriptionsManager.disableSubscription(device, "offerChanged")
        res <- subscriptionsManager.getDisabledSubscriptions(device)
      } yield {
        disabled should contain theSameElementsAs Set("offerChanged", "offerPriceChanged")
        empty shouldBe empty
        newEmpty shouldBe empty
        res should contain theSameElementsAs Set("offerChanged")
      }

    }

    "disable/enable subscriptions" in ftest {
      val unsubscribedDevice = DeviceGen.next

      for {
        _ <- dao.addDeviceInfo(unsubscribedDevice, DeviceInfoGen.next)
        empty <- subscriptionsManager.getDisabledSubscriptions(unsubscribedDevice)
        _ <- subscriptionsManager.disableSubscription(unsubscribedDevice, "disabledSubscription")
        disabled <- subscriptionsManager.getDisabledSubscriptions(unsubscribedDevice)
        _ <- subscriptionsManager.enableSubscription(unsubscribedDevice, "disabledSubscription")
        res <- subscriptionsManager.getDisabledSubscriptions(unsubscribedDevice)
      } yield {
        empty shouldBe empty
        disabled should contain theSameElementsAs Set("disabledSubscription")
        res shouldBe empty
      }
    }
  }
}
