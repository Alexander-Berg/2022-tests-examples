package ru.yandex.auto.vin.decoder.pushnoy

import auto.carfax.common.clients.pushnoy.PushnoyClient
import org.mockito.Mockito.{never, reset, times, verify}
import org.scalatest.BeforeAndAfter
import org.scalatest.funsuite.AsyncFunSuite
import ru.yandex.auto.vin.decoder.model.{AutoruUser, MockedFeatures}
import ru.yandex.auto.vin.decoder.pushnoy.model.OfferPushMessage
import auto.carfax.common.clients.pushnoy.model.UserDevicesResponse.{Device, DeviceFullInfo, DeviceInfo}
import auto.carfax.common.utils.tracing.Traced
import ru.yandex.auto.vin.decoder.ydb.YdbSentPushDao
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.ops.test.TestOperationalSupport

import java.time._
import java.time.format.DateTimeFormatter
import scala.concurrent.Future

class PushnoyManagerTest extends AsyncFunSuite with BeforeAndAfter with MockitoSupport with MockedFeatures {

  implicit val t: Traced = Traced.empty
  implicit val metrics = TestOperationalSupport
  implicit val clock = Clock.fixed(Instant.parse("2020-05-31T00:00:00Z"), ZoneOffset.UTC)

  val msk = "Europe/Moscow"
  val krsk = "Asia/Krasnoyarsk"

  // GMT: Sunday, 31 May 2020 г., 10:00:52
  val suitableMorning = 1590919252000L

  val pushMessage = OfferPushMessage("1043045004-977b3", "Mercedes", "Benz", 2002)

  val client = mock[PushnoyClient]
  val ydbSentPushDao = mock[YdbSentPushDao]
  val manager = new PushnoyManager(client, ydbSentPushDao, features)

  before {
    reset(client)
    reset(features)

    when(features.PushnoyManagerDryRun).thenReturn(disabledFeature)
  }

  def time(hour: Int, minute: Int = 0, second: Int = 0): Clock = {
    val time = LocalTime.of(hour, minute, second)
    val dtf = DateTimeFormatter.ofPattern("HH:mm:ss")
    Clock.fixed(Instant.parse(s"2020-05-31T${time.format(dtf)}Z"), ZoneOffset.UTC)
  }

  def checkSDF(zoneId: String, timestamp: Long)(implicit clock: Clock): Option[ZonedDateTime] = {
    PushnoyManager.checkSuitableDeliverFrom(zoneId, timestamp)(clock)
  }

  test("getSuitableDeliverFrom") {
    // около 7 утра GMT
    assert(checkSDF(msk, 1590908340000L).isEmpty)
    assert(checkSDF(msk, 1590908400001L).get.toString == "2020-05-31T10:00:00.001+03:00[Europe/Moscow]")
    assert(checkSDF(msk, 1590908400001L)(time(8)).isEmpty)
    // около 8 вечера GMT
    assert(checkSDF(msk, 1590944400001L).isEmpty)
    assert(checkSDF(msk, 1590944340000L).get.toString == "2020-05-31T19:59+03:00[Europe/Moscow]")
    // около 7 утра GMT
    assert(checkSDF(krsk, 1590894000000L).isEmpty)
    assert(checkSDF(krsk, 1590894000001L).get.toString == "2020-05-31T10:00:00.001+07:00[Asia/Krasnoyarsk]")
    assert(checkSDF(krsk, 1590894000001L)(time(3)).get.toString == "2020-05-31T10:00:00.001+07:00[Asia/Krasnoyarsk]")
  }

  test("send pushes") {
    when(client.getDevices(?)(?)).thenReturn(
      Future.successful(
        Seq(
          DeviceFullInfo(Device("autoru", "1"), Some(DeviceInfo(Some(msk)))),
          DeviceFullInfo(Device("autoru", "2"), Some(DeviceInfo(Some(krsk)))),
          DeviceFullInfo(Device("autoru", "3"), Some(DeviceInfo(Some(krsk))))
        )
      )
    )
    when(ydbSentPushDao.hasEntriesForOffer(?, ?)).thenReturn(Future.successful(false))
    when(client.sendPush(?, ?, ?, ?, ?)(?)).thenReturn(Future.successful(Some("uuid")))
    when(ydbSentPushDao.addEntry(?, ?, ?, ?, ?)).thenReturn(Future.unit)

    manager
      .sendPushes(AutoruUser(1), pushMessage, suitableMorning)
      .map { sentCount =>
        verify(client, times(3)).sendPush(?, ?, ?, ?, ?)(?)
        verify(ydbSentPushDao, times(3)).addEntry(?, ?, ?, ?, ?)
        assert(sentCount == 3)
      }
  }

  test("don't send pushes already sent") {
    when(ydbSentPushDao.hasEntriesForOffer(?, ?)).thenReturn(Future.successful(true))

    manager
      .sendPushes(AutoruUser(1), pushMessage, suitableMorning)
      .map { sentCount =>
        verify(client, never()).sendPush(?, ?, ?, ?, ?)(?)
        assert(sentCount == 0)
      }
  }

  test("dont'w send pushes without devices") {
    when(client.getDevices(?)(?)).thenReturn(Future.successful(Seq.empty))

    manager
      .sendPushes(AutoruUser(1), pushMessage, suitableMorning)
      .map { sentCount =>
        verify(client, never()).sendPush(?, ?, ?, ?, ?)(?)
        assert(sentCount == 0)
      }
  }
}
