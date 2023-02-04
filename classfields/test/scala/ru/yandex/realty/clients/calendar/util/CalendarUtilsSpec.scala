package ru.yandex.realty.clients.calendar.util

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpecLike}
import ru.yandex.realty.model.duration.{LocalDateInterval, LocalTimeInterval}
import java.time.LocalDate.{parse => ld}
import java.time.LocalDateTime.{parse => p}
import java.time.LocalTime.{of => t, parse => lt}
import java.time.temporal.ChronoUnit
import java.time.{Instant, LocalDate, LocalDateTime, LocalTime}

@RunWith(classOf[JUnitRunner])
class CalendarUtilsSpec extends WordSpecLike with Matchers with CalendarModelsGen {
  import CalendarUtils._

  "LocalDateTimeOps.truncateToStartDay" should {
    "return truncated value" in {
      p("2022-01-01T00:00:00").truncateToStartDay shouldEqual p("2022-01-01T00:00:00")
      p("2022-01-01T00:00:01").truncateToStartDay shouldEqual p("2022-01-01T00:00:00")
      p("2022-02-09T10:20:30").truncateToStartDay shouldEqual p("2022-02-09T00:00:00")
      p("2022-12-31T23:59:59").truncateToStartDay shouldEqual p("2022-12-31T00:00:00")
    }
  }

  "LocalDateTimeOps.truncateToEndDay" should {
    "return truncated value" in {
      p("2022-01-01T00:00:00").truncateToEndDay shouldEqual p("2022-01-01T23:59:59.999999999")
      p("2022-01-01T00:00:01").truncateToEndDay shouldEqual p("2022-01-01T23:59:59.999999999")
      p("2022-02-09T10:20:30").truncateToEndDay shouldEqual p("2022-02-09T23:59:59.999999999")
      p("2022-12-31T23:59:59").truncateToEndDay shouldEqual p("2022-12-31T23:59:59.999999999")
    }
  }

  "CalendarUtils.max" should {
    "return [max] time" in {
      max(t(10, 0), t(10, 30)) shouldEqual t(10, 30)
      max(t(10, 30), t(10, 0)) shouldEqual t(10, 30)
      max(t(10, 0), t(10, 0)) shouldEqual t(10, 0)
    }
  }

  "CalendarUtils.LocalTimeIntervalOps.merge" should {
    "return defined merged interval" in {
      //   |-------|
      //   |-------|             => |-------|
      LocalTimeInterval(t(10, 0), t(10, 30)) merge
        LocalTimeInterval(t(10, 0), t(10, 30)) shouldEqual Some(LocalTimeInterval(t(10, 0), t(10, 30)))

      //           |-------|
      //   |-------|             => |--------------|
      LocalTimeInterval(t(10, 0), t(10, 30)) merge
        LocalTimeInterval(t(10, 30), t(11, 0)) shouldEqual Some(LocalTimeInterval(t(10, 0), t(11, 0)))
      LocalTimeInterval(t(10, 30), t(11, 0)) merge
        LocalTimeInterval(t(10, 0), t(10, 30)) shouldEqual Some(LocalTimeInterval(t(10, 0), t(11, 0)))

      //   |----|
      //   |-------|             => |-------|
      LocalTimeInterval(t(10, 0), t(10, 30)) merge
        LocalTimeInterval(t(10, 0), t(10, 20)) shouldEqual Some(LocalTimeInterval(t(10, 0), t(10, 30)))
      LocalTimeInterval(t(10, 0), t(10, 20)) merge
        LocalTimeInterval(t(10, 0), t(10, 30)) shouldEqual Some(LocalTimeInterval(t(10, 0), t(10, 30)))

      //   |----|
      //      |-------|          => |---------|
      LocalTimeInterval(t(10, 0), t(10, 30)) merge
        LocalTimeInterval(t(10, 20), t(11, 30)) shouldEqual Some(LocalTimeInterval(t(10, 0), t(11, 30)))
      LocalTimeInterval(t(10, 20), t(11, 30)) merge
        LocalTimeInterval(t(10, 0), t(10, 30)) shouldEqual Some(LocalTimeInterval(t(10, 0), t(11, 30)))

      //     |---|
      //   |-------|
      LocalTimeInterval(t(10, 0), t(10, 30)) merge
        LocalTimeInterval(t(10, 5), t(10, 10)) shouldEqual Some(LocalTimeInterval(t(10, 0), t(10, 30)))
      LocalTimeInterval(t(10, 5), t(10, 10)) merge
        LocalTimeInterval(t(10, 0), t(10, 30)) shouldEqual Some(LocalTimeInterval(t(10, 0), t(10, 30)))
    }

    "return [None] for merging of non overlapping intervals" in {
      //             |-------|
      //   |-------|
      LocalTimeInterval(t(10, 0), t(10, 30)) merge
        LocalTimeInterval(t(11, 0), t(11, 30)) shouldEqual None
      LocalTimeInterval(t(11, 0), t(11, 30)) merge
        LocalTimeInterval(t(10, 0), t(10, 30)) shouldEqual None
    }
  }

  "CalendarUtils.LocalTimeIntervalOps.isOverlap" should {
    "return [true] for overlapping intervals" in {
      //   |-------|
      //   |-------|
      LocalTimeInterval(t(10, 0), t(10, 30)) isOverlap
        LocalTimeInterval(t(10, 0), t(10, 30)) shouldEqual true

      //   |-------|
      //           |-------|
      LocalTimeInterval(t(10, 0), t(10, 30)) isOverlap
        LocalTimeInterval(t(10, 30), t(11, 0)) shouldEqual true
      LocalTimeInterval(t(10, 30), t(11, 0)) isOverlap
        LocalTimeInterval(t(10, 0), t(10, 30)) shouldEqual true

      //   |----|
      //   |-------|
      LocalTimeInterval(t(10, 0), t(10, 30)) isOverlap
        LocalTimeInterval(t(10, 0), t(10, 20)) shouldEqual true
      LocalTimeInterval(t(10, 0), t(10, 20)) isOverlap
        LocalTimeInterval(t(10, 0), t(10, 30)) shouldEqual true

      //   |----|
      //      |-------|
      LocalTimeInterval(t(10, 0), t(10, 30)) isOverlap
        LocalTimeInterval(t(10, 20), t(11, 30)) shouldEqual true
      LocalTimeInterval(t(10, 20), t(11, 30)) isOverlap
        LocalTimeInterval(t(10, 0), t(10, 30)) shouldEqual true

      //     |---|
      //   |-------|
      LocalTimeInterval(t(10, 0), t(10, 30)) isOverlap
        LocalTimeInterval(t(10, 5), t(10, 10)) shouldEqual true
      LocalTimeInterval(t(10, 5), t(10, 10)) isOverlap
        LocalTimeInterval(t(10, 0), t(10, 30)) shouldEqual true
    }

    "return [false] for non-overlapping intervals" in {
      //             |-------|
      //   |-------|
      LocalTimeInterval(t(10, 0), t(10, 30)) isOverlap
        LocalTimeInterval(t(11, 0), t(11, 30)) shouldEqual false
      LocalTimeInterval(t(11, 0), t(11, 30)) isOverlap
        LocalTimeInterval(t(10, 0), t(10, 30)) shouldEqual false
    }
  }

  "CalendarUtils.LocalTimeIntervalOps.contains" should {
    "return [true] if time point is contained in interval" in {
      val interval = LocalTimeInterval(t(10, 0), t(10, 30))
      val points = Seq(
        interval.from,
        t(10, 5),
        interval.to
      )

      points foreach (p => interval.contains(p) shouldEqual true)
    }

    "return [false] if time point is not contained in interval" in {
      val interval = LocalTimeInterval(t(10, 0), t(10, 30))
      val points = Seq(
        interval.from.minusSeconds(1),
        interval.from.minusHours(1),
        interval.to.plusSeconds(1),
        interval.to.plusHours(1)
      )

      points foreach (p => interval.contains(p) shouldEqual false)
    }
  }

  "CalendarUtils.LocalTimeIntervalOps.reorderWith" should {
    "return sorted intervals by [from] for non-overlapping intervals" in {
      val i1 = LocalTimeInterval(t(10, 0), t(10, 30))
      val i2 = LocalTimeInterval(t(12, 0), t(12, 30))

      i1.reorderWith(i1) shouldEqual (i1, i1)
      i1.reorderWith(i2) shouldEqual (i1, i2)
      i2.reorderWith(i1) shouldEqual (i1, i2)
    }

    "return sorted intervals by [from] for overlapping intervals" in {
      val i1 = LocalTimeInterval(t(10, 0), t(10, 30))
      val i2 = LocalTimeInterval(t(10, 5), t(12, 30))

      i1.reorderWith(i1) shouldEqual (i1, i1)
      i1.reorderWith(i2) shouldEqual (i1, i2)
      i2.reorderWith(i1) shouldEqual (i1, i2)
    }
  }

  "CalendarUtils.EventOps.isOpaqueOneDay" should {
    "return [true] for events which opaque one day" in {
      import LocalDateTime.{parse => p}
      eventGen.next
        .copy(startTs = p("2022-02-09T00:00:00"), endTs = p("2022-02-09T10:20:30"))
        .isOpaqueOneDay shouldEqual true

      eventGen.next
        .copy(startTs = p("2022-02-09T10:20:20"), endTs = p("2022-02-09T10:20:30"))
        .isOpaqueOneDay shouldEqual true

      eventGen.next
        .copy(startTs = p("2022-02-09T00:00:00"), endTs = p("2022-02-09T23:59:59.999999999"))
        .isOpaqueOneDay shouldEqual true

      eventGen.next
        .copy(startTs = p("2022-02-09T23:59:00"), endTs = p("2022-02-09T23:59:59.999999999"))
        .isOpaqueOneDay shouldEqual true
    }

    "return [false] for events which have length more than one day" in {
      import LocalDateTime.{parse => p}
      eventGen.next
        .copy(startTs = p("2022-02-09T00:00:00"), endTs = p("2022-02-10T00:00:00"))
        .isOpaqueOneDay shouldEqual false

      eventGen.next
        .copy(startTs = p("2022-02-09T23:59:59.999999999"), endTs = p("2022-02-10T00:00:00"))
        .isOpaqueOneDay shouldEqual false

      eventGen.next
        .copy(startTs = p("2022-02-09T10:20:30"), endTs = p("2022-02-10T10:20:30"))
        .isOpaqueOneDay shouldEqual false

      eventGen.next
        .copy(startTs = p("2022-02-09T10:20:30"), endTs = p("2022-02-11T10:20:30"))
        .isOpaqueOneDay shouldEqual false
    }
  }

  "CalendarUtils.EventOps.splitByDays" should {
    "return seq of one event for event which opaques one day" in {
      import LocalDateTime.{parse => p}
      val sampleEvents = Seq(
        eventGen.next.copy(startTs = p("2022-02-09T00:00:00"), endTs = p("2022-02-09T10:20:30")),
        eventGen.next.copy(startTs = p("2022-02-09T10:20:30"), endTs = p("2022-02-09T10:20:40")),
        eventGen.next.copy(startTs = p("2022-02-09T00:00:00"), endTs = p("2022-02-09T23:59:59.999999999"))
      )

      sampleEvents foreach { e =>
        e.splitByDays should contain theSameElementsAs Seq(e)
      }
    }

    "return seq of single-day's events for event which opaques two days" in {
      import LocalDateTime.{parse => p}
      val longEvt = eventGen.next.copy(startTs = p("2022-02-09T10:20:30"), endTs = p("2022-02-10T11:30:00"))
      longEvt.splitByDays should contain theSameElementsAs Seq(
        longEvt.copy(startTs = p("2022-02-09T10:20:30"), endTs = p("2022-02-09T23:59:59.999999999")),
        longEvt.copy(startTs = p("2022-02-10T00:00:00"))
      )
    }

    "return seq of single-day's events for event which opaques more than one day" in {
      import LocalDateTime.{parse => p}
      val longEvt = eventGen.next.copy(startTs = p("2022-02-09T10:20:30"), endTs = p("2022-02-12T11:30:00"))
      longEvt.splitByDays should contain theSameElementsAs Seq(
        longEvt.copy(startTs = p("2022-02-09T10:20:30"), endTs = p("2022-02-09T23:59:59.999999999")),
        longEvt.copy(startTs = p("2022-02-10T00:00:00"), endTs = p("2022-02-10T23:59:59.999999999")),
        longEvt.copy(startTs = p("2022-02-11T00:00:00"), endTs = p("2022-02-11T23:59:59.999999999")),
        longEvt.copy(startTs = p("2022-02-12T00:00:00"))
      )
    }

    "return seq of single-day's events for event which opaques more than one day between two months" in {
      import LocalDateTime.{parse => p}
      val longEvt = eventGen.next.copy(startTs = p("2022-01-30T10:20:30"), endTs = p("2022-02-03T11:30:00"))
      longEvt.splitByDays should contain theSameElementsAs Seq(
        longEvt.copy(startTs = p("2022-01-30T10:20:30"), endTs = p("2022-01-30T23:59:59.999999999")),
        longEvt.copy(startTs = p("2022-01-31T00:00:00"), endTs = p("2022-01-31T23:59:59.999999999")),
        longEvt.copy(startTs = p("2022-02-01T00:00:00"), endTs = p("2022-02-01T23:59:59.999999999")),
        longEvt.copy(startTs = p("2022-02-02T00:00:00"), endTs = p("2022-02-02T23:59:59.999999999")),
        longEvt.copy(startTs = p("2022-02-03T00:00:00"))
      )
    }

    "return seq of single-day's events for event which opaques more than one day between two years" in {
      import LocalDateTime.{parse => p}
      val longEvt = eventGen.next.copy(startTs = p("2021-12-30T10:20:30"), endTs = p("2022-01-03T11:30:00"))
      longEvt.splitByDays should contain theSameElementsAs Seq(
        longEvt.copy(startTs = p("2021-12-30T10:20:30"), endTs = p("2021-12-30T23:59:59.999999999")),
        longEvt.copy(startTs = p("2021-12-31T00:00:00"), endTs = p("2021-12-31T23:59:59.999999999")),
        longEvt.copy(startTs = p("2022-01-01T00:00:00"), endTs = p("2022-01-01T23:59:59.999999999")),
        longEvt.copy(startTs = p("2022-01-02T00:00:00"), endTs = p("2022-01-02T23:59:59.999999999")),
        longEvt.copy(startTs = p("2022-01-03T00:00:00"))
      )
    }
  }

  "CalendarUtils.EventOps.durationInMinutes" should {
    "return correct duration=[30min]" in {
      val durationInMinutes = 30
      val from = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS).plusHours(10)
      val to = from.plusMinutes(durationInMinutes)

      val event = eventGen.next.copy(startTs = from, endTs = to)

      event.durationInMinutes shouldEqual durationInMinutes
    }
  }

  "CalendarUtils.EventOps.getLocalTimeInterval" should {
    "return correct interval" in {
      val durationInMinutes = 30
      val from = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS).plusHours(10)
      val to = from.plusMinutes(durationInMinutes)

      val event = eventGen.next.copy(
        startTs = from,
        endTs = to
      )

      val expectedInterval = LocalTimeInterval(t(10, 0), t(10, 30))

      event.getLocalTimeInterval shouldEqual expectedInterval
    }

    "throw exception for interval which > 1 day" in {
      val from = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS).plusHours(10)
      val to = from.plusDays(1).plusMinutes(30)

      val event = eventGen.next.copy(
        startTs = from,
        endTs = to
      )

      an[IllegalArgumentException] should be thrownBy event.getLocalTimeInterval
    }
  }

  "CalendarUtils.EventOps.belongsToTheSameDate" should {
    "return [true] for events which are belonged to the same date" in {
      val date = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS)
      val events = eventGen.next(5).zipWithIndex.toSeq.map {
        case (event, i) =>
          event.copy(
            startTs = date.plusHours(i),
            endTs = date.plusHours(i).plusMinutes(15)
          )
      }
      belongsToTheSameDate(events) shouldEqual true
    }

    "return [true] for events which are belonged to the same date including events which have length several days" in {
      val date = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS)
      val events = eventGen.next(5).zipWithIndex.toSeq.map {
        case (event, i) =>
          event.copy(
            startTs = date.plusHours(i),
            endTs = date.plusHours(i).plusMinutes(15)
          )
      }

      val longEvent = eventGen.next.copy(startTs = date.plusHours(1), endTs = date.plusDays(2))
      val extendedEvents = events :+ longEvent

      belongsToTheSameDate(extendedEvents) shouldEqual true
    }

    "return [false] for events which are belonged to the different dates" in {
      val date = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS)
      val events = eventGen.next(5).zipWithIndex.toSeq.map {
        case (event, i) =>
          event.copy(
            startTs = date.plusDays(i).plusHours(i),
            endTs = date.plusDays(i).plusHours(i).plusMinutes(15)
          )
      }
      belongsToTheSameDate(events) shouldEqual false
    }
  }

  "CalendarUtils.getSlots" should {
    "throw exception if [from] is after than [to]" in {
      val from = t(10, 0)
      val to = t(8, 0)
      val slotSizeInMins = 30

      getSlots(from, to, slotSizeInMins) shouldBe empty
    }

    "throw exception if [slotSize] is < 0" in {
      val from = t(10, 0)
      val to = t(11, 0)
      val slotSizeInMins = -10

      getSlots(from, to, slotSizeInMins) shouldBe empty
    }

    "return correct slots from interval with non-inclusive last opaque time" in {
      val from = t(10, 0)
      val to = t(12, 35)
      val slotSizeInMins = 30

      val slots = getSlots(from, to, slotSizeInMins)
      val expectedSlots = Seq(
        LocalTimeInterval(t(10, 0), t(10, 30)),
        LocalTimeInterval(t(10, 30), t(11, 0)),
        LocalTimeInterval(t(11, 0), t(11, 30)),
        LocalTimeInterval(t(11, 30), t(12, 0)),
        LocalTimeInterval(t(12, 0), t(12, 30))
      )

      slots shouldEqual expectedSlots
    }

    "return correct slots from interval with inclusive last opaque time" in {
      val from = LocalTime.of(10, 0)
      val to = LocalTime.of(15, 0)
      val slotSizeInMins = 30

      val slots = getSlots(from, to, slotSizeInMins)
      val expectedSlots = Seq(
        LocalTimeInterval(t(10, 0), t(10, 30)),
        LocalTimeInterval(t(10, 30), t(11, 0)),
        LocalTimeInterval(t(11, 0), t(11, 30)),
        LocalTimeInterval(t(11, 30), t(12, 0)),
        LocalTimeInterval(t(12, 0), t(12, 30)),
        LocalTimeInterval(t(12, 30), t(13, 0)),
        LocalTimeInterval(t(13, 0), t(13, 30)),
        LocalTimeInterval(t(13, 30), t(14, 0)),
        LocalTimeInterval(t(14, 0), t(14, 30)),
        LocalTimeInterval(t(14, 30), t(15, 0))
      )

      slots shouldEqual expectedSlots
    }
  }

  "CalendarUtils.getOverlappingIntervals" should {
    "return original intervals for non-overlapping intervals" in {
      //  |-----|
      //          |---|
      //                |--------|
      //                           |--|
      val intervals = Seq(
        LocalTimeInterval(t(10, 0), t(10, 30)),
        LocalTimeInterval(t(11, 0), t(11, 10)),
        LocalTimeInterval(t(12, 0), t(12, 45)),
        LocalTimeInterval(t(13, 0), t(13, 5))
      )
      val date = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS)
      val events = intervals.map { i =>
        val from = date.`with`(i.from)
        val to = date.`with`(i.to)
        eventGen.next.copy(startTs = from, endTs = to)
      }

      val result = getOverlappingIntervals(events)
      //  |-----| |---| |--------| |--|
      val expectedResult = intervals

      result shouldEqual expectedResult
    }

    "return original non-overlapping and merged overlapping intervals" in {
      //  |-----|
      //      |---|
      //                |--------|
      //                    |--|
      val intervals = Seq(
        LocalTimeInterval(t(10, 0), t(10, 30)),
        LocalTimeInterval(t(10, 20), t(10, 35)),
        LocalTimeInterval(t(12, 0), t(12, 45)),
        LocalTimeInterval(t(12, 20), t(12, 30))
      )
      val date = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS)
      val events = intervals.map { i =>
        val from = date.`with`(i.from)
        val to = date.`with`(i.to)
        eventGen.next.copy(startTs = from, endTs = to)
      }

      val result = getOverlappingIntervals(events)
      //  |-------|     |--------|
      val expectedResult = Seq(
        LocalTimeInterval(t(10, 0), t(10, 35)),
        LocalTimeInterval(t(12, 0), t(12, 45))
      )

      result shouldEqual expectedResult
    }
  }

  "CalendarUtils.getDayAvailableSlots" should {

    "return all available slots if there aren't opaque slots" in {
      val workFrom = LocalTime.of(10, 0)
      val workTo = LocalTime.of(15, 0)
      val slotDurationInMins = 30

      // events:
      // ____________________________________________________________________________________
      // opaque:
      // slots:       |......|......|......|......|......|......|......|......|......|......|
      //            10:00  10:30  11:00  11:30  12:00  12:30  13:00  13:30  14:00  14:30  15:00
      // available:   |......|......|......|......|......|......|......|......|......|......|
      val opaqueIntervals = Seq.empty[LocalTimeInterval]

      val date = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS)
      val opaqueEvents =
        opaqueIntervals.map(i => eventGen.next.copy(startTs = date.`with`(i.from), endTs = date.`with`(i.to)))

      val result = getDayAvailableSlots(opaqueEvents, workFrom, workTo, slotDurationInMins)
      val expectedResult = Seq(
        LocalTimeInterval(t(10, 0), t(10, 30)),
        LocalTimeInterval(t(10, 30), t(11, 0)),
        LocalTimeInterval(t(11, 0), t(11, 30)),
        LocalTimeInterval(t(11, 30), t(12, 0)),
        LocalTimeInterval(t(12, 0), t(12, 30)),
        LocalTimeInterval(t(12, 30), t(13, 0)),
        LocalTimeInterval(t(13, 0), t(13, 30)),
        LocalTimeInterval(t(13, 30), t(14, 0)),
        LocalTimeInterval(t(14, 0), t(14, 30)),
        LocalTimeInterval(t(14, 30), t(15, 0))
      )
      result shouldEqual expectedResult
    }

    "return all available slots if there aren't opaque slots which aren't overlapped with working interval" in {
      val workFrom = LocalTime.of(10, 0)
      val workTo = LocalTime.of(15, 0)
      val slotDurationInMins = 30

      // events:
      //           ▒▒▒▒▒▒▒
      //                                                                                             ▒▒▒▒▒▒▒
      // _______________________________________________________________________________________________________
      // opaque:
      // slots:            |......|......|......|......|......|......|......|......|......|......|
      //                 10:00  10:30  11:00  11:30  12:00  12:30  13:00  13:30  14:00  14:30  15:00
      // available:        |......|......|......|......|......|......|......|......|......|......|
      val opaqueIntervals = Seq(
        LocalTimeInterval(t(9, 0), t(9, 50)),
        LocalTimeInterval(t(15, 20), t(17, 0))
      )

      val date = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS)
      val opaqueEvents =
        opaqueIntervals.map(i => eventGen.next.copy(startTs = date.`with`(i.from), endTs = date.`with`(i.to)))

      val result = getDayAvailableSlots(opaqueEvents, workFrom, workTo, slotDurationInMins)
      val expectedResult = Seq(
        LocalTimeInterval(t(10, 0), t(10, 30)),
        LocalTimeInterval(t(10, 30), t(11, 0)),
        LocalTimeInterval(t(11, 0), t(11, 30)),
        LocalTimeInterval(t(11, 30), t(12, 0)),
        LocalTimeInterval(t(12, 0), t(12, 30)),
        LocalTimeInterval(t(12, 30), t(13, 0)),
        LocalTimeInterval(t(13, 0), t(13, 30)),
        LocalTimeInterval(t(13, 30), t(14, 0)),
        LocalTimeInterval(t(14, 0), t(14, 30)),
        LocalTimeInterval(t(14, 30), t(15, 0))
      )
      result shouldEqual expectedResult
    }

    "return [Nil] if there aren't available slots" in {
      val workFrom = LocalTime.of(10, 0)
      val workTo = LocalTime.of(15, 0)
      val slotDurationInMins = 30

      // events:
      //                           ▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒
      //              ▒▒▒▒▒▒▒▒▒▒▒                                ▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒
      //                      ▒▒▒▒▒▒▒▒     ▒
      // ____________________________________________________________________________________
      // opaque:      ▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒
      // slots:       |......|......|......|......|......|......|......|......|......|......|
      //            10:00  10:30  11:00  11:30  12:00  12:30  13:00  13:30  14:00  14:30  15:00
      // available:
      val opaqueIntervals = Seq(
        LocalTimeInterval(t(10, 50), t(13, 10)),
        //
        LocalTimeInterval(t(10, 0), t(10, 45)),
        LocalTimeInterval(t(13, 0), t(15, 0)),
        //
        LocalTimeInterval(t(10, 5), t(11, 10)),
        LocalTimeInterval(t(11, 29), t(11, 31))
      )

      val date = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS)
      val opaqueEvents =
        opaqueIntervals.map(i => eventGen.next.copy(startTs = date.`with`(i.from), endTs = date.`with`(i.to)))

      val result = getDayAvailableSlots(opaqueEvents, workFrom, workTo, slotDurationInMins)
      result shouldBe empty
    }

    "return available slots" in {
      val workFrom = LocalTime.of(10, 0)
      val workTo = LocalTime.of(15, 0)
      val slotDurationInMins = 30

      // events:
      //                      ▒▒                     ▒▒                  ▒▒▒▒▒▒▒
      //                    ▒▒▒                  ▒▒▒▒      ▒▒
      //                                        ▒▒       ▒▒▒▒▒              ▒▒▒▒▒▒▒▒
      // ____________________________________________________________________________________
      // opaque:            ▒▒▒▒                ▒▒▒▒▒▒▒  ▒▒▒▒▒           ▒▒▒▒▒▒▒▒▒▒▒
      // slots:       |......|......|......|......|......|......|......|......|......|......|
      //            10:00  10:30  11:00  11:30  12:00  12:30  13:00  13:30  14:00  14:30  15:00
      // available:             |......|......|               |......|              |......|
      val opaqueIntervals = Seq(
        LocalTimeInterval(t(10, 31), t(10, 35)),
        LocalTimeInterval(t(10, 20), t(10, 32)),
        //
        LocalTimeInterval(t(11, 45), t(11, 48)),
        LocalTimeInterval(t(11, 46), t(12, 15)),
        LocalTimeInterval(t(12, 15), t(12, 20)),
        //
        LocalTimeInterval(t(12, 29), t(12, 45)),
        LocalTimeInterval(t(12, 10), t(12, 20)),
        //
        LocalTimeInterval(t(13, 35), t(14, 5)),
        LocalTimeInterval(t(13, 45), t(14, 20))
      )
      val date = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS)
      val opaqueEvents =
        opaqueIntervals.map(i => eventGen.next.copy(startTs = date.`with`(i.from), endTs = date.`with`(i.to)))

      val result = getDayAvailableSlots(opaqueEvents, workFrom, workTo, slotDurationInMins)
      val expectedResult = Seq(
        LocalTimeInterval(t(10, 35), t(11, 5)),
        LocalTimeInterval(t(11, 5), t(11, 35)),
        LocalTimeInterval(t(12, 45), t(13, 15)),
        LocalTimeInterval(t(14, 20), t(14, 50))
      )

      result shouldEqual expectedResult
    }
  }

  "CalendarUtils.getAvailableSlots" should {
    "return slots by date range" in {
      // [2022-01-09]
      // opaque:            ▒▒▒▒                ▒▒▒▒▒▒▒  ▒▒▒▒▒           ▒▒▒▒▒▒▒▒▒▒▒
      // slots:       |......|......|......|......|......|......|......|......|......|......|
      //            10:00  10:30  11:00  11:30  12:00  12:30  13:00  13:30  14:00  14:30  15:00
      // available:             |......|......|               |......|              |......|
      // ----------------------------------------------------------------------------------------------
      //
      //
      // [2022-01-10]
      // opaque:      ▒▒▒▒                  ▒▒▒▒▒▒▒▒▒▒▒           ▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒
      // slots:       |......|......|......|......|......|......|......|......|......|......|
      //            10:00  10:30  11:00  11:30  12:00  12:30  13:00  13:30  14:00  14:30  15:00
      // available:       |......|......|              |......|                     |......|
      // ----------------------------------------------------------------------------------------------
      //
      //
      // [2022-01-11]
      // opaque:
      // slots:       |......|......|......|......|......|......|......|......|......|......|
      //            10:00  10:30  11:00  11:30  12:00  12:30  13:00  13:30  14:00  14:30  15:00
      // available:   |......|......|......|......|......|......|......|......|......|......|
      // ----------------------------------------------------------------------------------------------
      //
      //
      // [2022-01-12]
      // opaque:      ▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒            ▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒
      // slots:       |......|......|......|......|......|......|......|......|......|......|
      //            10:00  10:30  11:00  11:30  12:00  12:30  13:00  13:30  14:00  14:30  15:00
      // available:                           |......|
      // ----------------------------------------------------------------------------------------------

      val from = ld("2022-01-09")
      val to = ld("2022-01-12")
      val workTimeFrom = lt("10:00")
      val workTimeTo = lt("15:00")

      val e = eventGen.next
      val opaqueEvents = Seq(
        Seq(
          e.copy(startTs = p("2022-01-09T10:28:00"), endTs = p("2022-01-09T10:32:00")),
          e.copy(startTs = p("2022-01-09T11:45:00"), endTs = p("2022-01-09T12:25:00")),
          e.copy(startTs = p("2022-01-09T12:30:00"), endTs = p("2022-01-09T12:35:00")),
          e.copy(startTs = p("2022-01-09T13:32:00"), endTs = p("2022-01-09T14:25:00"))
        ),
        Seq(
          e.copy(startTs = p("2022-01-10T10:00:00"), endTs = p("2022-01-10T10:15:00")),
          e.copy(startTs = p("2022-01-10T11:31:00"), endTs = p("2022-01-10T12:25:00")),
          e.copy(startTs = p("2022-01-10T13:05:00"), endTs = p("2022-01-10T14:25:00"))
        ),
        Seq(
          // 2022-01-11 there are not events
        ),
        Seq(
          e.copy(startTs = p("2022-01-12T10:00:00"), endTs = p("2022-01-12T11:35:00")),
          e.copy(startTs = p("2022-01-12T12:31:00"), endTs = p("2022-01-12T15:00:00"))
        )
      ).flatten

      val result = CalendarUtils.getAvailableSlots(from, to, opaqueEvents, workTimeFrom, workTimeTo, 30)
      val expected = Map(
        ld("2022-01-09") -> Seq(
          LocalTimeInterval(lt("10:32"), lt("11:02")),
          LocalTimeInterval(lt("11:02"), lt("11:32")),
          LocalTimeInterval(lt("12:35"), lt("13:05")),
          LocalTimeInterval(lt("14:25"), lt("14:55"))
        ),
        ld("2022-01-10") -> Seq(
          LocalTimeInterval(lt("10:15"), lt("10:45")),
          LocalTimeInterval(lt("10:45"), lt("11:15")),
          LocalTimeInterval(lt("12:25"), lt("12:55")),
          LocalTimeInterval(lt("14:25"), lt("14:55"))
        ),
        ld("2022-01-11") -> Seq(
          LocalTimeInterval(lt("10:00"), lt("10:30")),
          LocalTimeInterval(lt("10:30"), lt("11:00")),
          LocalTimeInterval(lt("11:00"), lt("11:30")),
          LocalTimeInterval(lt("11:30"), lt("12:00")),
          LocalTimeInterval(lt("12:00"), lt("12:30")),
          LocalTimeInterval(lt("12:30"), lt("13:00")),
          LocalTimeInterval(lt("13:00"), lt("13:30")),
          LocalTimeInterval(lt("13:30"), lt("14:00")),
          LocalTimeInterval(lt("14:00"), lt("14:30")),
          LocalTimeInterval(lt("14:30"), lt("15:00"))
        ),
        ld("2022-01-12") -> Seq(
          LocalTimeInterval(lt("11:35"), lt("12:05"))
        )
      )

      result should contain theSameElementsAs expected
    }

    "return slots by date dange with long event which has length=[several days]" in {
      // Long event:
      //                                     14:00-11:00
      //                  ░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░
      // |....................|....................|....................|....................|
      //      2022-01-09           2022-01-10           2022-01-11            2022-01-12
      //
      // ----------------------------------------------------------------------------------------------
      // [2022-01-09]
      //                                                                      ░░░░░░░░░░░░░░░
      // opaque:            ▒▒▒▒                ▒▒▒▒▒▒▒  ▒▒▒▒▒           ▒▒▒▒▒▒▒▒▒▒▒
      // slots:       |......|......|......|......|......|......|......|......|......|......|
      //            10:00  10:30  11:00  11:30  12:00  12:30  13:00  13:30  14:00  14:30  15:00
      // available:             |......|......|               |......|
      // ----------------------------------------------------------------------------------------------
      //
      //
      // [2022-01-10]
      //              ░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░
      // opaque:      ▒▒▒▒                  ▒▒▒▒▒▒▒▒▒▒▒           ▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒
      // slots:       |......|......|......|......|......|......|......|......|......|......|
      //            10:00  10:30  11:00  11:30  12:00  12:30  13:00  13:30  14:00  14:30  15:00
      // available:
      // ----------------------------------------------------------------------------------------------
      //
      //
      // [2022-01-11]
      //              ░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░
      // opaque:
      // slots:       |......|......|......|......|......|......|......|......|......|......|
      //            10:00  10:30  11:00  11:30  12:00  12:30  13:00  13:30  14:00  14:30  15:00
      // available:
      // ----------------------------------------------------------------------------------------------
      //
      //
      // [2022-01-12]
      //              ░░░░░░░░░░░░░░░
      // opaque:      ▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒            ▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒
      // slots:       |......|......|......|......|......|......|......|......|......|......|
      //            10:00  10:30  11:00  11:30  12:00  12:30  13:00  13:30  14:00  14:30  15:00
      // available:                           |......|
      // ----------------------------------------------------------------------------------------------

      val from = ld("2022-01-09")
      val to = ld("2022-01-12")
      val workTimeFrom = lt("10:00")
      val workTimeTo = lt("15:00")

      val e = eventGen.next
      val longEvents = Seq(
        e.copy(startTs = p("2022-01-09T14:00:00"), endTs = p("2022-01-12T11:00:00"))
      )
      val opaqueEvents = Seq(
        Seq(
          e.copy(startTs = p("2022-01-09T10:28:00"), endTs = p("2022-01-09T10:32:00")),
          e.copy(startTs = p("2022-01-09T11:45:00"), endTs = p("2022-01-09T12:25:00")),
          e.copy(startTs = p("2022-01-09T12:30:00"), endTs = p("2022-01-09T12:35:00")),
          e.copy(startTs = p("2022-01-09T13:32:00"), endTs = p("2022-01-09T14:25:00"))
        ),
        Seq(
          e.copy(startTs = p("2022-01-10T10:00:00"), endTs = p("2022-01-10T10:15:00")),
          e.copy(startTs = p("2022-01-10T11:31:00"), endTs = p("2022-01-10T12:25:00")),
          e.copy(startTs = p("2022-01-10T13:05:00"), endTs = p("2022-01-10T14:25:00"))
        ),
        Seq(
          // 2022-01-11 there are not events
        ),
        Seq(
          e.copy(startTs = p("2022-01-12T10:00:00"), endTs = p("2022-01-12T11:35:00")),
          e.copy(startTs = p("2022-01-12T12:31:00"), endTs = p("2022-01-12T15:00:00"))
        )
      ).flatten ++ longEvents

      val result = CalendarUtils.getAvailableSlots(from, to, opaqueEvents, workTimeFrom, workTimeTo, 30)
      val expected = Map(
        ld("2022-01-09") -> Seq(
          LocalTimeInterval(lt("10:32"), lt("11:02")),
          LocalTimeInterval(lt("11:02"), lt("11:32")),
          LocalTimeInterval(lt("12:35"), lt("13:05"))
        ),
        ld("2022-01-10") -> Seq(),
        ld("2022-01-11") -> Seq(),
        ld("2022-01-12") -> Seq(
          LocalTimeInterval(lt("11:35"), lt("12:05"))
        )
      )

      result should contain theSameElementsAs expected
    }

    "return slots by date range with long events which have length more than one day" in {
      // Long events:
      //                  14:00-11:00          14:00-11:00          14:00-11:00
      //                  ░░░░░░░░░░           ░░░░░░░░░░           ░░░░░░░░░░
      // |....................|....................|....................|....................|
      //      2022-01-09           2022-01-10           2022-01-11            2022-01-12
      //
      // ----------------------------------------------------------------------------------------------
      // [2022-01-09]
      //                                                                      ░░░░░░░░░░░░░░░
      // opaque:            ▒▒▒▒                ▒▒▒▒▒▒▒  ▒▒▒▒▒           ▒▒▒▒▒▒▒▒▒▒▒
      // slots:       |......|......|......|......|......|......|......|......|......|......|
      //            10:00  10:30  11:00  11:30  12:00  12:30  13:00  13:30  14:00  14:30  15:00
      // available:             |......|......|               |......|
      // ----------------------------------------------------------------------------------------------
      //
      //
      // [2022-01-10]
      //              ░░░░░░░░░░░░░░░                                         ░░░░░░░░░░░░░░░
      // opaque:      ▒▒▒▒                  ▒▒▒▒▒▒▒▒▒▒▒           ▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒
      // slots:       |......|......|......|......|......|......|......|......|......|......|
      //            10:00  10:30  11:00  11:30  12:00  12:30  13:00  13:30  14:00  14:30  15:00
      // available:                 |......|           |......|
      // ----------------------------------------------------------------------------------------------
      //
      //
      // [2022-01-11]
      //              ░░░░░░░░░░░░░░░                                         ░░░░░░░░░░░░░░░
      // opaque:
      // slots:       |......|......|......|......|......|......|......|......|......|......|
      //            10:00  10:30  11:00  11:30  12:00  12:30  13:00  13:30  14:00  14:30  15:00
      // available:                 |......|......|......|......|......|......|
      // ----------------------------------------------------------------------------------------------
      //
      //
      // [2022-01-12]
      //              ░░░░░░░░░░░░░░░
      // opaque:      ▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒            ▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒
      // slots:       |......|......|......|......|......|......|......|......|......|......|
      //            10:00  10:30  11:00  11:30  12:00  12:30  13:00  13:30  14:00  14:30  15:00
      // available:                           |......|
      // ----------------------------------------------------------------------------------------------

      val from = ld("2022-01-09")
      val to = ld("2022-01-12")
      val workTimeFrom = lt("10:00")
      val workTimeTo = lt("15:00")

      val e = eventGen.next
      val longEvents = Seq(
        e.copy(startTs = p("2022-01-09T14:00:00"), endTs = p("2022-01-10T11:00:00")),
        e.copy(startTs = p("2022-01-10T14:00:00"), endTs = p("2022-01-11T11:00:00")),
        e.copy(startTs = p("2022-01-11T14:00:00"), endTs = p("2022-01-12T11:00:00"))
      )
      val opaqueEvents = Seq(
        Seq(
          e.copy(startTs = p("2022-01-09T10:28:00"), endTs = p("2022-01-09T10:32:00")),
          e.copy(startTs = p("2022-01-09T11:45:00"), endTs = p("2022-01-09T12:25:00")),
          e.copy(startTs = p("2022-01-09T12:30:00"), endTs = p("2022-01-09T12:35:00")),
          e.copy(startTs = p("2022-01-09T13:32:00"), endTs = p("2022-01-09T14:25:00"))
        ),
        Seq(
          e.copy(startTs = p("2022-01-10T10:00:00"), endTs = p("2022-01-10T10:15:00")),
          e.copy(startTs = p("2022-01-10T11:31:00"), endTs = p("2022-01-10T12:25:00")),
          e.copy(startTs = p("2022-01-10T13:05:00"), endTs = p("2022-01-10T14:25:00"))
        ),
        Seq(
          // 2022-01-11 there are not events
        ),
        Seq(
          e.copy(startTs = p("2022-01-12T10:00:00"), endTs = p("2022-01-12T11:35:00")),
          e.copy(startTs = p("2022-01-12T12:31:00"), endTs = p("2022-01-12T15:00:00"))
        )
      ).flatten ++ longEvents

      val result = CalendarUtils.getAvailableSlots(from, to, opaqueEvents, workTimeFrom, workTimeTo, 30)
      val expected = Map(
        ld("2022-01-09") -> Seq(
          LocalTimeInterval(lt("10:32"), lt("11:02")),
          LocalTimeInterval(lt("11:02"), lt("11:32")),
          LocalTimeInterval(lt("12:35"), lt("13:05"))
        ),
        ld("2022-01-10") -> Seq(
          LocalTimeInterval(lt("11:00"), lt("11:30")),
          LocalTimeInterval(lt("12:25"), lt("12:55"))
        ),
        ld("2022-01-11") -> Seq(
          LocalTimeInterval(lt("11:00"), lt("11:30")),
          LocalTimeInterval(lt("11:30"), lt("12:00")),
          LocalTimeInterval(lt("12:00"), lt("12:30")),
          LocalTimeInterval(lt("12:30"), lt("13:00")),
          LocalTimeInterval(lt("13:00"), lt("13:30")),
          LocalTimeInterval(lt("13:30"), lt("14:00"))
        ),
        ld("2022-01-12") -> Seq(
          LocalTimeInterval(lt("11:35"), lt("12:05"))
        )
      )

      result should contain theSameElementsAs expected
    }
  }

  "CalendarUtils.splitByDates" should {
    "return Seq.empty if start > end" in {
      val from = Instant.parse("2022-01-18T10:30:00.000Z")
      val to = from.minus(1, ChronoUnit.DAYS)
      CalendarUtils.splitByDates(from, to) shouldEqual Seq.empty
    }

    "return Seq.empty if start == end" in {
      val from = Instant.parse("2022-01-18T10:30:00.000Z")
      val to = from
      CalendarUtils.splitByDates(from, to) shouldEqual Seq(LocalDate.parse("2022-01-18"))
    }

    "return days sequence for correct input range: 1 day" in {
      val from = Instant.parse("2022-01-18T10:30:00.000Z")
      val to = from.plus(1, ChronoUnit.DAYS)

      CalendarUtils.splitByDates(from, to) shouldEqual Seq(
        LocalDate.parse("2022-01-18"),
        LocalDate.parse("2022-01-19")
      )
    }

    "return days sequence for correct input range: N days" in {
      val from = Instant.parse("2022-01-18T10:30:00.000Z")
      val to = from.plus(5, ChronoUnit.DAYS)

      CalendarUtils.splitByDates(from, to) shouldEqual Seq(
        LocalDate.parse("2022-01-18"),
        LocalDate.parse("2022-01-19"),
        LocalDate.parse("2022-01-20"),
        LocalDate.parse("2022-01-21"),
        LocalDate.parse("2022-01-22"),
        LocalDate.parse("2022-01-23")
      )
    }

    "return days sequence for correct input range: N days which contains transfer between months" in {
      val from = Instant.parse("2022-01-30T10:30:00.000Z")
      val to = from.plus(5, ChronoUnit.DAYS)

      CalendarUtils.splitByDates(from, to) shouldEqual Seq(
        LocalDate.parse("2022-01-30"),
        LocalDate.parse("2022-01-31"),
        LocalDate.parse("2022-02-01"),
        LocalDate.parse("2022-02-02"),
        LocalDate.parse("2022-02-03"),
        LocalDate.parse("2022-02-04")
      )
    }

    "return days sequence for correct input range: N days which contains transfer between years" in {
      val from = Instant.parse("2022-12-30T10:30:00.000Z")
      val to = from.plus(5, ChronoUnit.DAYS)

      CalendarUtils.splitByDates(from, to) shouldEqual Seq(
        LocalDate.parse("2022-12-30"),
        LocalDate.parse("2022-12-31"),
        LocalDate.parse("2023-01-01"),
        LocalDate.parse("2023-01-02"),
        LocalDate.parse("2023-01-03"),
        LocalDate.parse("2023-01-04")
      )
    }
  }

  "CalendarUtils.subtractIntervals" should {
    def interval(from: Int, to: Int): LocalDateInterval = {
      val fromStr = s"0$from".takeRight(2)
      val toStr = s"0$to".takeRight(2)
      LocalDateInterval(LocalDate.parse(s"2022-12-$fromStr"), LocalDate.parse(s"2022-12-$toStr"))
    }

    "process call without subtrahends" in {
      CalendarUtils.subtractIntervals(interval(1, 10), Nil) shouldEqual List(interval(1, 10))
      CalendarUtils.subtractIntervals(interval(1, 1), Nil) shouldEqual List(interval(1, 1))
    }

    "subtract the same interval" in {
      CalendarUtils.subtractIntervals(interval(1, 5), List(interval(1, 5))) shouldEqual Nil
      CalendarUtils.subtractIntervals(interval(1, 1), List(interval(1, 1))) shouldEqual Nil
    }

    "subtract bigger interval" in {
      CalendarUtils.subtractIntervals(interval(5, 10), List(interval(1, 20))) shouldEqual Nil
    }

    "subtract one interval from the middle" in {
      CalendarUtils.subtractIntervals(interval(5, 10), List(interval(7, 8))) shouldEqual
        List(interval(5, 6), interval(9, 10))
      CalendarUtils.subtractIntervals(interval(5, 10), List(interval(6, 9))) shouldEqual
        List(interval(5, 5), interval(10, 10))
    }

    "subtract one interval from the beginnig" in {
      CalendarUtils.subtractIntervals(interval(5, 10), List(interval(5, 8))) shouldEqual
        List(interval(9, 10))
      CalendarUtils.subtractIntervals(interval(5, 10), List(interval(3, 8))) shouldEqual
        List(interval(9, 10))
    }

    "subtract one interval from the end" in {
      CalendarUtils.subtractIntervals(interval(5, 10), List(interval(9, 10))) shouldEqual
        List(interval(5, 8))
      CalendarUtils.subtractIntervals(interval(5, 10), List(interval(9, 12))) shouldEqual
        List(interval(5, 8))
    }

    "subtract adjacent intervals" in {
      CalendarUtils.subtractIntervals(interval(5, 20), List(interval(10, 12), interval(13, 14))) shouldEqual
        List(interval(5, 9), interval(15, 20))
      CalendarUtils.subtractIntervals(interval(5, 20), List(interval(5, 12), interval(13, 20))) shouldEqual
        List()
    }

    "subtract overlapping intervals" in {
      CalendarUtils.subtractIntervals(interval(5, 20), List(interval(10, 14), interval(12, 16))) shouldEqual
        List(interval(5, 9), interval(17, 20))

      CalendarUtils.subtractIntervals(
        interval(5, 20),
        List(interval(10, 16), interval(12, 13), interval(12, 12), interval(13, 13))
      ) shouldEqual List(interval(5, 9), interval(17, 20))

      CalendarUtils.subtractIntervals(
        interval(5, 7),
        List(interval(5, 5), interval(5, 5), interval(6, 7), interval(6, 7))
      ) shouldEqual List()
    }

    "subtract out of bounds intervals" in {
      CalendarUtils.subtractIntervals(interval(5, 20), List(interval(1, 1), interval(2, 3), interval(22, 25))) shouldEqual
        List(interval(5, 20))
      CalendarUtils.subtractIntervals(interval(5, 20), List(interval(1, 4), interval(21, 22))) shouldEqual
        List(interval(5, 20))
    }
  }
}
