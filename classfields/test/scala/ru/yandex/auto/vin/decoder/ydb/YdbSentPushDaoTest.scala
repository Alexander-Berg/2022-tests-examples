package ru.yandex.auto.vin.decoder.ydb

import auto.carfax.pro_auto.core.src.testkit.YdbContainerKit
import com.dimafeng.testcontainers.ForAllTestContainer
import org.scalacheck.Gen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import ru.yandex.auto.vin.decoder.model.AutoruUser
import auto.carfax.common.clients.pushnoy.model.ZonedDevice
import auto.carfax.common.utils.tracing.Traced
import auto.carfax.common.utils.concurrent.CoreFutureUtils._
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.ydb.zio.YdbZioWrapper
import ru.yandex.vertis.ydb.QueryOptions
import zio.Runtime
import zio.blocking.Blocking
import zio.clock.Clock
import zio.random.Random

import java.time.{LocalDate, LocalTime, ZoneId, ZonedDateTime}
import scala.concurrent.duration._

class YdbSentPushDaoTest
  extends AnyWordSpecLike
  with YdbContainerKit
  with MockitoSupport
  with ForAllTestContainer
  with Matchers {

  implicit val t: Traced = Traced.empty

  private val zioRuntime: zio.Runtime[Blocking with Clock with Random] = Runtime.default

  val prefix = "/local"

  lazy val ydb = YdbZioWrapper.make(container.tableClient, prefix, 3.seconds, QueryOptions.Default.withV1Syntax)

  lazy val sentPushDao = new YdbSentPushDao(ydb, zioRuntime, None)

  def daoTest(action: YdbSentPushDao => Unit): Unit = {
    action(sentPushDao)
  }

  def OfferIdGen: Gen[String] = Gen.alphaNumStr.map(_.take(10))

  def getAboutNow(zoneId: ZoneId): Gen[ZonedDateTime] = {
    for {
      seconds <- Gen.chooseNum(-24.hour.toSeconds, 0)
    } yield ZonedDateTime.now(zoneId).minusSeconds(seconds)
  }

  def logEntry(
      objectId: Option[String] = None,
      user: Option[AutoruUser] = None,
      deliveryDate: Option[LocalDate] = None): Unit = {
    val userGen = user.map(Gen.const).getOrElse(Gen.chooseNum(1, 199).map(AutoruUser(_)))
    val zoneId = ZoneId.systemDefault()

    val entryGen = for {
      uuid <- Gen.uuid.map(_.toString)
      offerId <- objectId.map(Gen.const).getOrElse(OfferIdGen)
      user <- userGen
      deviceId <- Gen.alphaLowerStr.map(_.take(10))
      device = ZonedDevice(deviceId, zoneId.getId)
      deliverDt <- {
        deliveryDate.map(dd => Gen.const(ZonedDateTime.of(dd, LocalTime.now(), zoneId))).getOrElse {
          getAboutNow(zoneId).map(v => ZonedDateTime.ofInstant(v.toInstant, zoneId))
        }
      }
    } yield sentPushDao.addEntry(uuid, offerId, user, device, deliverDt)
    entryGen.sample.get.await
  }

  def logEntry(objectId: String, user: AutoruUser): Unit = {
    logEntry(Some(objectId), Some(user))
  }

  def logEntry(user: AutoruUser, deliverDate: LocalDate, times: Int): Unit = {
    (1 to times).foreach { _ =>
      logEntry(OfferIdGen.sample, Some(user), Some(deliverDate))
    }
  }

  def logEntry(objectId: String, user: AutoruUser, deliverDate: LocalDate, times: Int): Unit = {
    (1 to times).foreach { _ =>
      logEntry(Some(objectId), Some(user), Some(deliverDate))
    }
  }

  "YdbSentPushDao" should {
    "init" in {
      sentPushDao.init()

    }

    "hasEntriesForOffer" should {
      val offerId = "16147224-ec2e319a"
      val user = AutoruUser(1)

      "return false" in {
        sentPushDao.hasEntriesForOffer(offerId, user).await shouldBe false
      }

      "append data" in {
        logEntry(offerId, user)
      }

      "return true" in {
        sentPushDao.hasEntriesForOffer(offerId, user).await shouldBe true
      }
    }

    "drop" in daoTest { dao =>
      dao.drop()
    }
  }
}
