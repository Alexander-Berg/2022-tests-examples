package vertis.pushnoy.scheduler.task

import java.time.Instant
import java.time.temporal.ChronoUnit
import org.scalatest.concurrent.ScalaFutures
import vertis.pushnoy.dao.ydb.{PushnoyYdbTest, YdbSupport}
import vertis.pushnoy.gen.ModelGenerators._
import vertis.pushnoy.services.apple.{AppleDeviceCheckClient, TestAppleDeviceCheckClient}
import vertis.pushnoy.services.xiva.{DeviceChecker, TestXivaClient, XivaClient, XivaManagerSupport}
import ru.yandex.vertis.generators.ProducerProvider
import ru.yandex.vertis.ops.test.TestOperationalSupport
import ru.yandex.vertis.scheduler.model.Schedule
import ru.yandex.vertis.util.concurrent.Futures
import vertis.pushnoy.TestEnvironment
import vertis.pushnoy.conf.DeleteExpiredTaskConfig
import vertis.pushnoy.model.Device
import vertis.pushnoy.model.request.PushMessageV1
import vertis.pushnoy.model.template.Template
import vertis.pushnoy.services.{PushBanChecker, TestDeviceChecker}
import vertis.pushnoy.services.xiva.TestXivaClient.InvalidId
import vertis.zio.test.ZioSpecBase

import scala.concurrent.duration._

/** @author kusaeva
  */
class DeleteExpiredTaskIntTest
  extends ZioSpecBase
  with PushnoyYdbTest
  with ScalaFutures
  with ProducerProvider
  with YdbSupport
  with XivaManagerSupport
  with TestEnvironment {

  lazy val xivaClient: XivaClient = new TestXivaClient
  lazy val appleDeviceCheckClient: AppleDeviceCheckClient = new TestAppleDeviceCheckClient
  lazy val deviceChecker: DeviceChecker = new TestDeviceChecker

  lazy val pushBanChecker: PushBanChecker = new PushBanChecker {
    override def isPushBanned(push: PushMessageV1): Boolean = false

    override def isPushBanned(template: Template): Boolean = false
  }

  private val deleteExpiredConfig = DeleteExpiredTaskConfig(30.days, false)

  private lazy val task = new DeleteExpiredTask(
    dao,
    xivaManager,
    deleteExpiredConfig,
    Schedule.Manually,
    TestOperationalSupport.prometheusRegistry
  )

  private def getInstant = Instant.now().truncatedTo(ChronoUnit.MICROS)

  "DeleteExpiredTask" should {
    Seq(
      ("delete expired devices", false),
      ("not delete expired devices having a dry run", true)
    ).foreach { case (name, dryRun) =>
      name in {
        val now = getInstant
        val user = UserGen.next
        val count = 3
        val expiration = 5
        val devices = DeviceGen.next(count).toSeq.map(_.copy(client = user.client))
        val attaches = devices.zip(0 until count).map { case (device, i) =>
          dao.devicesDao.addDeviceInfo(device, DeviceInfoGen.next) *>
            dao.devicesDao.addTokenInfo(device, TokenInfoGen.next) *>
            dao.usersDao.attachDeviceWithTimestamp(user, device, now.minus((i + expiration).toLong, ChronoUnit.DAYS))

        }
        Futures.traverseSequential(attaches)(runInTx).futureValue

        dao.getUserDevicesFullInfo(user).map(_.map(_.device)).futureValue should contain theSameElementsAs devices

        task
          .deleteDevicesOlderThan(now.minus(expiration.toLong, ChronoUnit.DAYS), dryRun)
          .futureValue

        if (dryRun) {
          dao.getUserDevicesFullInfo(user).map(_.map(_.device)).futureValue should contain theSameElementsAs devices
        } else {
          dao.getUserDevicesFullInfo(user).map(_.map(_.device)).futureValue should contain theSameElementsAs Seq(
            devices.head
          )
        }
      }
    }

    "delete device if unsubscribing failed" in {
      val now = getInstant
      val user = UserGen.next
      val count = 3
      val devices = DeviceGen.next(count).toSeq.map(_.copy(client = user.client)) ++ Seq(Device(user.client, InvalidId))
      val attaches = devices.zipWithIndex.map { case (device, i) =>
        dao.devicesDao.addDeviceInfo(device, DeviceInfoGen.next) *>
          dao.devicesDao.addTokenInfo(device, TokenInfoGen.next) *>
          dao.usersDao.attachDeviceWithTimestamp(user, device, now.minus(i.toLong, ChronoUnit.DAYS))

      }

      Futures.traverseSequential(attaches)(runInTx).futureValue

      dao.getUserDevicesFullInfo(user).map(_.map(_.device)).futureValue should contain theSameElementsAs devices

      task
        .deleteDevicesOlderThan(now, dryRun = false)
        .futureValue

      dao.getUserDevicesFullInfo(user).map(_.map(_.device)).futureValue should contain theSameElementsAs Seq(
        devices.head
      )
    }
  }
}
