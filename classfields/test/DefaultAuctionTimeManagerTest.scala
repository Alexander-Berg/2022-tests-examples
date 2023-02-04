package auto.c2b.lotus.logic.test

import auto.c2b.lotus.logic.AuctionTimeManager.AuctionTimeManager
import auto.c2b.lotus.logic.{AuctionTimeManager, DefaultAuctionTimeManager}
import auto.c2b.lotus.model.StartSchedule
import zio.magic._
import zio.test.TestAspect.sequential
import zio.test.environment.{TestClock, TestEnvironment}
import zio.test.{assertTrue, checkNM, DefaultRunnableSpec, Gen, ZSpec}
import zio.{Has, ZLayer}

import java.time.temporal.ChronoUnit
import java.time.{DayOfWeek, LocalTime, MonthDay, OffsetDateTime, ZoneId, ZoneOffset}

object DefaultAuctionTimeManagerTest extends DefaultRunnableSpec {
  private val timeZoneId = ZoneId.of("Europe/Moscow")
  private val timeZoneOffset = ZoneOffset.ofHours(3)

  private def testTime(fakeNow: OffsetDateTime, expected: OffsetDateTime) = {
    for {
      _ <- TestClock.setDateTime(fakeNow)
      gotTime <- AuctionTimeManager.nearestStartTime
    } yield assertTrue(gotTime.atOffset(timeZoneOffset) == expected.truncatedTo(ChronoUnit.MILLIS))
  }

  private def genCorrectOffsetDateTime(min: OffsetDateTime, max: OffsetDateTime) = {
    Gen
      .offsetDateTime(min, max)
      .filter(odt => odt.isAfter(min) && odt.isBefore(max)) // for some reason this conditions are not met by default
  }

  private val checksPerCase = 10

  override def spec: ZSpec[TestEnvironment, Any] = {
    (suite("DefaultAuctionTimeManager")(
      testM("now is work hours") {
        checkNM(checksPerCase)(
          genCorrectOffsetDateTime(
            OffsetDateTime.of(2022, 2, 18, 9, 30, 0, 0, timeZoneOffset),
            OffsetDateTime.of(2022, 2, 18, 17, 30, 0, 0, timeZoneOffset)
          ).noShrink
        ) { fakeNow =>
          testTime(
            fakeNow = fakeNow,
            expected = fakeNow.plusHours(1)
          )
        }
      },
      testM("now is non-working hours between two workdays") {
        checkNM(checksPerCase)(
          genCorrectOffsetDateTime(
            OffsetDateTime.of(2022, 2, 16, 18, 30, 0, 0, timeZoneOffset),
            OffsetDateTime.of(2022, 2, 17, 9, 30, 0, 0, timeZoneOffset)
          ).noShrink
        ) { fakeNow =>
          testTime(
            fakeNow = fakeNow,
            expected = OffsetDateTime.of(2022, 2, 17, 10, 30, 0, 0, timeZoneOffset)
          )
        }
      },
      testM("now is friday evening or weekends") {
        checkNM(checksPerCase)(
          genCorrectOffsetDateTime(
            OffsetDateTime.of(2022, 2, 18, 19, 30, 0, 0, timeZoneOffset),
            OffsetDateTime.of(2022, 2, 20, 9, 30, 0, 0, timeZoneOffset)
          ).noShrink
        ) { fakeNow =>
          testTime(
            fakeNow = fakeNow,
            expected = OffsetDateTime.of(2022, 2, 21, 10, 30, 0, 0, timeZoneOffset)
          )
        }
      },
      testM("now is evening before holiday or holiday") {
        checkNM(checksPerCase)(
          genCorrectOffsetDateTime(
            OffsetDateTime.of(2022, 4, 30, 19, 30, 1, 0, timeZoneOffset),
            OffsetDateTime.of(2022, 5, 2, 9, 30, 0, 0, timeZoneOffset)
          ).noShrink
        ) { fakeNow =>
          testTime(
            fakeNow = fakeNow,
            expected = OffsetDateTime.of(2022, 5, 2, 10, 30, 0, 0, timeZoneOffset)
          )
        }
      },
      testM("now is weekend, but it's working day") {
        checkNM(checksPerCase)(
          genCorrectOffsetDateTime(
            OffsetDateTime.of(2022, 3, 5, 9, 30, 0, 0, timeZoneOffset),
            OffsetDateTime.of(2022, 3, 5, 17, 30, 0, 0, timeZoneOffset)
          ).noShrink
        ) { fakeNow =>
          testTime(
            fakeNow = fakeNow,
            expected = fakeNow.plusHours(1)
          )
        }
      },
      testM("now is evening or night before working weekend") {
        checkNM(checksPerCase)(
          genCorrectOffsetDateTime(
            OffsetDateTime.of(2022, 3, 4, 17, 30, 0, 0, timeZoneOffset),
            OffsetDateTime.of(2022, 3, 5, 9, 30, 0, 0, timeZoneOffset)
          ).noShrink
        ) { fakeNow =>
          testTime(
            fakeNow = fakeNow,
            expected = OffsetDateTime.of(2022, 3, 5, 10, 30, 0, 0, timeZoneOffset)
          )
        }
      }
    ) @@ sequential).provideCustomLayerShared {
      ZLayer
        .wireSome[
          TestEnvironment,
          AuctionTimeManager with TestClock with Has[DefaultAuctionTimeManager.Config]
        ](
          ZLayer.succeed(
            DefaultAuctionTimeManager.Config(
              StartSchedule(
                time = StartSchedule.StartTime(
                  from = LocalTime.of(10, 30),
                  to = LocalTime.of(18, 30)
                ),
                workingDaysOfWeek =
                  Set(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY),
                holidays = Set(MonthDay.of(5, 1)),
                auctionTimeZone = timeZoneId,
                workingWeekends = Set(MonthDay.of(3, 5)),
                confirmDays = 3
              )
            )
          ),
          DefaultAuctionTimeManager.live
        )
    }
  }
}
