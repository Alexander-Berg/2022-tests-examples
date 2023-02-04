package auto.dealers.multiposting.logic.test.warehouse

import cats.syntax.option._
import common.scalapb.ScalaProtobuf
import common.zio.clock.MoscowClock
import auto.dealers.multiposting.logic.warehouse.WarehouseService
import ru.auto.multiposting.filter_model.{PeriodFilter, WarehouseFilter}
import auto.dealers.multiposting.model.{ClientId, Filters, WarehouseDayState, WarehouseUniqueCounters}
import zio.random.Random
import zio.test.Assertion._
import zio.test.mock.Expectation._
import zio.test.{DefaultRunnableSpec, ZSpec, _}

import java.time.LocalDate
import common.zio.logging.Logging
import auto.dealers.multiposting.storage.testkit.WarehouseStateDaoMock

object WarehouseServiceSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[environment.TestEnvironment, Any] = suite("WarehouseService.live")(
    testM(
      """should fetch daily warehouse state"""
    ) {
      val givenClientId = 100L

      val givenFrom = LocalDate.now(MoscowClock.timeZone)
      val givenTo = LocalDate.now(MoscowClock.timeZone)
      val givenFilter = createWarehouseFilter(givenFrom, givenTo)
      val givenProtoFilter = createWarehouseProtoFilter(givenFrom, givenTo)

      checkM(Gen.listOf(Gen.fromRandomSample(warehouseStateSample))) { state =>
        val daoMock = WarehouseStateDaoMock.GetPerDayState(
          equalTo((ClientId(givenClientId), givenFilter)),
          value(state)
        )

        assertM(
          WarehouseService.getWarehouseDailyState(ClientId(givenClientId), givenProtoFilter)
        )(hasSize(equalTo(state.size)))
          .provideCustomLayer((Logging.live ++ daoMock) >>> WarehouseService.live)
      }
    },
    testM(
      """should fetch warehouse unique counters"""
    ) {
      val givenClientId = 100L

      val givenFrom = LocalDate.now(MoscowClock.timeZone)
      val givenTo = LocalDate.now(MoscowClock.timeZone)
      val givenFilter = createWarehouseFilter(givenFrom, givenTo)
      val givenProtoFilter = createWarehouseProtoFilter(givenFrom, givenTo)

      checkM(Gen.fromRandomSample(warehouseUniqueCountersSample)) { state =>
        val daoMock = WarehouseStateDaoMock.GetUniqueCounters(
          equalTo((ClientId(givenClientId), givenFilter)),
          value(state)
        )

        (for {
          counters <- WarehouseService.getWarehouseUniqueCounters(ClientId(givenClientId), givenProtoFilter)
        } yield {
          assert(counters.autoruActive)(equalTo(state.autoruActive)) &&
          assert(counters.autoruInactive)(equalTo(state.autoruInactive)) &&
          assert(counters.avitoActive)(equalTo(state.avitoActive)) &&
          assert(counters.avitoInactive)(equalTo(state.avitoInactive)) &&
          assert(counters.dromActive)(equalTo(state.dromActive)) &&
          assert(counters.dromInactive)(equalTo(state.dromInactive)) &&
          assert(counters.totalActive)(equalTo(state.totalActive)) &&
          assert(counters.totalInactive)(equalTo(state.totalInactive)) &&
          assert(counters.totalRemoved)(equalTo(state.totalRemoved))
        }).provideCustomLayer((Logging.live ++ daoMock) >>> WarehouseService.live)
      }
    }
  )

  private def createWarehouseProtoFilter(givenFrom: LocalDate, givenTo: LocalDate) =
    WarehouseFilter(period =
      PeriodFilter(ScalaProtobuf.toTimestamp(givenFrom).some, ScalaProtobuf.toTimestamp(givenTo).some).some
    )

  private def createWarehouseFilter(givenFrom: LocalDate, givenTo: LocalDate) =
    Filters.WarehouseFilter(
      from = givenFrom,
      to = givenTo,
      category = None,
      section = None
    )

  private def warehouseStateSample(rnd: Random.Service) =
    for {
      autoruActive <- rnd.nextInt
      autoruInactive <- rnd.nextInt
      autoruRemoved <- rnd.nextInt
      avitoActive <- rnd.nextInt
      avitoInactive <- rnd.nextInt
      avitoRemoved <- rnd.nextInt
      dromActive <- rnd.nextInt
      dromInactive <- rnd.nextInt
      dromRemoved <- rnd.nextInt
      totalActive <- rnd.nextInt
      totalInactive <- rnd.nextInt
      totalRemoved <- rnd.nextInt
    } yield Sample.noShrink(
      WarehouseDayState(
        day = LocalDate.now(),
        autoruActive = autoruActive,
        autoruInactive = autoruInactive,
        autoruRemoved = autoruRemoved,
        avitoActive = avitoActive,
        avitoInactive = avitoInactive,
        avitoRemoved = avitoRemoved,
        dromActive = dromActive,
        dromInactive = dromInactive,
        dromRemoved = dromRemoved,
        totalActive = totalActive,
        totalInactive = totalInactive,
        totalRemoved = totalRemoved
      )
    )

  private def warehouseUniqueCountersSample(rnd: Random.Service) =
    for {
      autoruActive <- rnd.nextInt
      autoruInactive <- rnd.nextInt
      avitoActive <- rnd.nextInt
      avitoInactive <- rnd.nextInt
      dromActive <- rnd.nextInt
      dromInactive <- rnd.nextInt
      totalActive <- rnd.nextInt
      totalInactive <- rnd.nextInt
      totalRemoved <- rnd.nextInt
    } yield Sample.noShrink(
      WarehouseUniqueCounters(
        autoruActive = autoruActive,
        autoruInactive = autoruInactive,
        avitoActive = avitoActive,
        avitoInactive = avitoInactive,
        dromActive = dromActive,
        dromInactive = dromInactive,
        totalActive = totalActive,
        totalInactive = totalInactive,
        totalRemoved = totalRemoved
      )
    )
}
