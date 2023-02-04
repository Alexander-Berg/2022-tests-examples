package vertis.spamalot.services.impl

import org.scalacheck.{Arbitrary, Gen}
import org.scalatestplus.mockito.MockitoSugar
import vertis.ArbitraryTestBase
import vertis.spamalot.model.TimeWindow
import vertis.spamalot.services.TimeWindowService
import vertis.zio.test.ZioSpecBase
import zio.Task

import java.time.{Duration, Instant, LocalDate, ZoneId}

class SendingTimeServiceImplTest extends ZioSpecBase with MockitoSugar with ArbitraryTestBase {

  private def timeWindowServiceMock(timeWindow: TimeWindow = globalTW) = new TimeWindowService.Service {
    override def get: Task[TimeWindow] = Task.succeed(timeWindow)
  }

  private val globalTW: TimeWindow = TimeWindow(
    Duration.ofHours(0),
    Duration.ofHours(24)
  )

  implicit private val twArbitrary: Arbitrary[TimeWindow] = Arbitrary.apply {
    for {
      h1 <- Gen.choose(0, 24)
      h2 <- Gen.choose(0, 24).filter(_ != h1)
      start = Math.min(h1, h2)
      end = Math.max(h1, h2)
    } yield TimeWindow(
      Duration.ofHours(start),
      Duration.ofHours(end)
    )
  }

  private val timezoneGen =
    Gen
      .oneOf(
        "America/Chicago",
        "Asia/Kashgar",
        "Chile/Continental",
        "Pacific/Yap",
        "CET",
        "Etc/GMT-1",
        "Etc/GMT-0",
        "Europe/Jersey",
        "America/Tegucigalpa",
        "Etc/GMT-5",
        "Europe/Istanbul",
        "America/Eirunepe",
        "Etc/GMT-4",
        "America/Miquelon",
        "Etc/GMT-3"
      )
      .map(ZoneId.of)

  private val secondsInADay = 60 * 60 * 24

  "SendingTimeService" should {
    "always allow sending for global time window" in ioTest {
      val service = new SendingTimeServiceImpl(timeWindowServiceMock())
      val tz = timezoneGen.sample.get
      val instant = random[Instant]
      for {
        isAllowed <- service.isAllowedNow(instant, tz)
        _ <- check(isAllowed should be(true))
      } yield ()
    }
    "never allow sending for overlapping time window" in ioTest {
      val overlappingWindow = TimeWindow(Duration.ofHours(24), Duration.ofHours(0))
      val service = new SendingTimeServiceImpl(timeWindowServiceMock(overlappingWindow))
      val tz = timezoneGen.sample.get
      val instant = random[Instant]
      for {
        isAllowed <- service.isAllowedNow(instant, tz)
        _ <- check(isAllowed should be(false))
      } yield ()
    }
    "allow sending inside window" in ioTest {
      val tw = random[TimeWindow]
      val service = new SendingTimeServiceImpl(timeWindowServiceMock(tw))
      val tz = timezoneGen.sample.get
      val twPlusSecondInstant = LocalDate
        .now()
        .atStartOfDay(tz)
        .plus(tw.start)
        .plusSeconds(1)
        .toInstant
      for {
        isAllowed <- service.isAllowedNow(twPlusSecondInstant, tz)
        _ <- check(isAllowed should be(true))
      } yield ()
    }
    "not allow sending outside window" in ioTest {
      val tw = random[TimeWindow]
      val service = new SendingTimeServiceImpl(timeWindowServiceMock(tw))
      val tz = timezoneGen.sample.get
      val twStartInstant =
        LocalDate
          .now()
          .atStartOfDay(tz)
          .plus(tw.start)
          .toInstant
      val twEndInstant =
        LocalDate
          .now()
          .atStartOfDay(tz)
          .plus(tw.end)
          .toInstant
      for {
        isAllowedBefore <- service.isAllowedNow(twStartInstant.minusSeconds(1), tz)
        isAllowedAfter <- service.isAllowedNow(twEndInstant.plusSeconds(1), tz)
        _ <- check(isAllowedBefore should be(false))
        _ <- check(isAllowedAfter should be(false))
      } yield ()
    }
    "return today start of the window if it's earlier than start of the window" in ioTest {
      val tw = random[TimeWindow]
      val adjustedTW = tw.copy(start = tw.start.plusSeconds(1))
      val service = new SendingTimeServiceImpl(timeWindowServiceMock(adjustedTW))
      val tz = timezoneGen.sample.get
      val beforeTWStartInstant =
        LocalDate
          .now()
          .atStartOfDay(tz)
          .plus(tw.start)
          .toInstant
      for {
        sendTime <- service.nextAllowedTime(beforeTWStartInstant, tz)
        _ <- check(sendTime should be(beforeTWStartInstant.plusSeconds(1)))
      } yield ()
    }
    "return tomorrows start of the window if it's later than start of the window" in ioTest {
      val tw = random[TimeWindow]
      val service = new SendingTimeServiceImpl(timeWindowServiceMock(tw))
      val tz = timezoneGen.sample.get
      val afterTWStartInstant =
        LocalDate
          .now()
          .atStartOfDay(tz)
          .plus(tw.start)
          .plusSeconds(1)
          .toInstant
      for {
        sendTime <- service.nextAllowedTime(afterTWStartInstant, tz)
        _ <- check(sendTime should be(afterTWStartInstant.minusSeconds(1).plusSeconds(secondsInADay)))
      } yield ()
    }
  }
}
