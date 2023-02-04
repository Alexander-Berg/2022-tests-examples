package auto.dealers.dealer_pony.scheduler.tasks

import auto.dealers.dealer_pony.scheduler.tasks.DealersWhitelistMaintenanceTask
import auto.dealers.dealer_pony.scheduler.tasks.DealersWhitelistMaintenanceTask.DealersWhitelistConfig
import auto.dealers.dealer_pony.storage.dao.DealerStatusDao
import auto.dealers.dealer_pony.storage.dao.DealerPhonesDao
import auto.dealers.dealer_pony.storage.testkit.DealerPhonesDaoMock
import auto.dealers.dealer_pony.clients.telepony.TeleponyClient
import auto.dealers.dealer_pony.clients.testkit.TeleponyClientMock
import common.zio.logging.Logging
import auto.dealers.dealer_pony.model._
import common.zio.clock.MoscowClock
import scala.concurrent.duration._
import java.time.ZonedDateTime
import zio.ZLayer
import zio.test.Assertion._
import zio.test.mock.Expectation._
import zio.test.mock.{Expectation, MockClock}
import zio.test.{assertM, DefaultRunnableSpec, ZSpec}

object DealersWhitelistMaintenanceTaskSpec extends DefaultRunnableSpec {

  val config = DealersWhitelistConfig()

  val timePoint =
    ZonedDateTime
      .of(2020, 1, 8, 0, 0, 0, 0, MoscowClock.timeZone)
      .toOffsetDateTime()

  val task = new DealersWhitelistMaintenanceTask()

  val phone = PhoneNumber.fromString("+79111234567").toOption.get

  override val spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("DealersWhitelistMaintenanceTask")(
      updateLoyalTTLsTest,
      updateLoyalTTLsTeleponyFailedTest,
      updateLoyalTTLsPhonesDAOFailedTest,
      deleteMarkedPhonesTest,
      deleteMarkedPhonesTeleponyFailedTest,
      deleteMarkedPhonesPhonesDaoFailedTest
    )

  lazy val updateLoyalTTLsTest =
    testM("updateLoyalTTLs") {

      val request = WhitelistUpdateRequest(
        ownerId = 0L,
        sourcePhones = List(phone),
        domains = List("autoru_def"),
        allDomainsFlag = false,
        ttl = config.ttlDays.days
      )

      val conf = ZLayer.succeed(config)

      val clock = MockClock.CurrentDateTime {
        value(timePoint)
      }.optional

      val getExpiring =
        DealerPhonesDaoMock.GetExpiring(equalTo((timePoint, config.dealersPerBatch)), value(List((0, Seq(phone)))))

      val teleponyUpdate = TeleponyClientMock.AddOrUpdate(equalTo(request), unit)

      val updateTTL =
        DealerPhonesDaoMock.UpdateExpirationDates(equalTo((0L, Seq((phone, timePoint.plusDays(config.ttlDays))))), unit)

      val mocks = clock ++ getExpiring ++ teleponyUpdate ++ updateTTL

      val env = conf ++ mocks ++ Logging.live

      assertM(task.updateLoyalTTLs)(isUnit)
        .provideCustomLayer(env)
    }

  lazy val updateLoyalTTLsTeleponyFailedTest =
    testM("updateLoyalTTLs Telepony Failed") {

      val request = WhitelistUpdateRequest(
        ownerId = 0L,
        sourcePhones = List(phone),
        domains = List("autoru_def"),
        allDomainsFlag = false,
        ttl = config.ttlDays.days
      )

      val conf = ZLayer.succeed(config)

      val clock = MockClock.CurrentDateTime {
        value(timePoint)
      }.optional

      val getExpiring =
        DealerPhonesDaoMock.GetExpiring(equalTo((timePoint, config.dealersPerBatch)), value(List((0, Seq(phone)))))

      val teleponyUpdate = TeleponyClientMock.AddOrUpdate(
        equalTo(request),
        failure(TeleponyClient.UnknownError(new Exception("network error")))
      )

      val mocks = clock ++ getExpiring ++ teleponyUpdate

      val env = conf ++ mocks ++ Logging.live

      assertM(task.updateLoyalTTLs)(isUnit)
        .provideCustomLayer(env)
    }

  lazy val updateLoyalTTLsPhonesDAOFailedTest =
    testM("updateLoyalTTLs DealerPhonesDao Failed") {

      val conf = ZLayer.succeed(config)

      val clock = MockClock.CurrentDateTime {
        value(timePoint)
      }.optional

      val getExpiring =
        DealerPhonesDaoMock.GetExpiring(equalTo((timePoint, config.dealersPerBatch)), failure(new Throwable))

      val mocks = clock ++ getExpiring

      val env = conf ++ mocks ++ TeleponyClientMock.empty ++ Logging.live

      assertM(task.updateLoyalTTLs)(isUnit)
        .provideCustomLayer(env)
    }

  lazy val deleteMarkedPhonesTest =
    testM("deleteMarkedPhones") {

      val request = WhitelistDeleteRequest(
        ownerId = 0L,
        sourcePhones = List(phone),
        domains = List("autoru_def"),
        allDomainsFlag = false
      )

      val conf = ZLayer.succeed(config)

      val getToBeDeleted =
        DealerPhonesDaoMock.GetToBeDeleted(equalTo(config.dealersPerBatch), value(List((0, Seq(phone)))))

      val teleponyDelete = TeleponyClientMock.Delete(equalTo(request), unit)

      val deletePhones =
        DealerPhonesDaoMock.Delete(equalTo((0L, List(phone))), unit)

      val mocks = getToBeDeleted ++ teleponyDelete ++ deletePhones

      val env = conf ++ mocks ++ Logging.live

      assertM(task.deleteMarkedPhones)(isUnit)
        .provideCustomLayer(env)
    }

  lazy val deleteMarkedPhonesTeleponyFailedTest =
    testM("deleteMarkedPhones Telepony Failed") {

      val request = WhitelistDeleteRequest(
        ownerId = 0L,
        sourcePhones = List(phone),
        domains = List("autoru_def"),
        allDomainsFlag = false
      )

      val conf = ZLayer.succeed(config)

      val getToBeDeleted =
        DealerPhonesDaoMock.GetToBeDeleted(equalTo(config.dealersPerBatch), value(List((0, Seq(phone)))))

      val teleponyDelete = TeleponyClientMock.Delete(
        equalTo(request),
        failure(TeleponyClient.UnknownError(new Exception("network error")))
      )

      val mocks = getToBeDeleted ++ teleponyDelete

      val env = conf ++ mocks ++ Logging.live

      assertM(task.deleteMarkedPhones)(isUnit)
        .provideCustomLayer(env)
    }

  lazy val deleteMarkedPhonesPhonesDaoFailedTest =
    testM("deleteMarkedPhones DealerPhonesDao Failed") {

      val conf = ZLayer.succeed(config)

      val getToBeDeleted =
        DealerPhonesDaoMock.GetToBeDeleted(equalTo(config.dealersPerBatch), failure(new Throwable))

      val env = conf ++ getToBeDeleted ++ TeleponyClientMock.empty ++ Logging.live

      assertM(task.deleteMarkedPhones)(isUnit)
        .provideCustomLayer(env)
    }

}
