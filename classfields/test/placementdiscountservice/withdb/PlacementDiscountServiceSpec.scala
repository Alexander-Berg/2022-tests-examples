package auto.dealers.loyalty.logic.test.placementdiscountservice.withdb

import auto.common.clients.cabinet.testkit.CabinetTest
import auto.dealers.loyalty.logic.{PlacementDiscountService, PlacementDiscountServiceLive}
import auto.dealers.loyalty.model.Percent
import auto.dealers.loyalty.storage.PlacementDiscountCalculationDao
import auto.dealers.loyalty.storage.clients.{DealerWarehouse, DiscountSettings}
import auto.dealers.loyalty.storage.jdbc.JdbcPlacementDiscountCalculationDao
import common.zio.doobie.schema.InitSchema
import common.zio.doobie.testkit.TestMySQL
import common.zio.logging.Logging
import doobie.Transactor
import doobie.implicits._
import ru.auto.cabinet.api_model.DetailedClient
import ru.auto.loyalty.placement_discount_policies.PlacementDiscountPolicies
import ru.auto.loyalty.placement_discount_policies.PlacementDiscountPolicies.{ClientId, DiscountLevel}
import zio._
import zio.interop.catz._
import zio.test.Assertion._
import zio.test.TestAspect._
import zio.test._
import zio.test.environment.TestEnvironment
import zio.test.mock.Expectation._
import zio.test.mock.mockable

import java.time.LocalDate

object PlacementDiscountServiceSpec extends DefaultRunnableSpec {

  private val client = DetailedClient(id = 4L)
  private val periodId = 1L

  private val clientPolicy =
    PlacementDiscountPolicies(
      clients = List(ClientId(clientId = client.id)),
      discounts = List(DiscountLevel(40, 15), DiscountLevel(80, 32))
    )
  private val warehouseData = List(53, 37, 46)
  private val periodStartDate = LocalDate.ofEpochDay(40000)
  private val periodEndDate = periodStartDate.plusDays(30)

  private val singleClientCabinet = CabinetTest.GetDetailedClient(equalTo(client.id), value(client))
  private val singlePolicyStorage = DiscountPoliciesMock.GetAllPolicies(value(List(clientPolicy)))

  private val fixedWarehouseClient =
    DealerWarehouseMock.GetActiveStockByDay(equalTo((client.id, periodStartDate, periodEndDate)), value(warehouseData))

  private val allSuites = suite("PlacementDiscountService")(
    testM("Calculate discount")(
      assertM {
        val expectations = fixedWarehouseClient && singlePolicyStorage && singleClientCabinet
        PlacementDiscountService(_.getOrCalcPlacementDiscount(periodId, client.id, periodStartDate, periodEndDate))
          .provideLayer((daoLayer ++ expectations ++ Logging.live) >>> PlacementDiscountServiceLive.layer)
          .map(_.placementDiscountPercent)
          .zip(checkDb)
          .map(Result.tupled)
      }(equalTo(Result(discount = 15, DB(stock = 46, discount = 15))))
    ),
    testM("Doesn't request stock if data available")(
      assertM {
        val expectations = DealerWarehouseMock.empty ++ singlePolicyStorage ++ singleClientCabinet
        (
          PlacementDiscountCalculationDao(_.writeStock(periodId, client.id, 90)) *>
            PlacementDiscountService(_.getOrCalcPlacementDiscount(periodId, client.id, periodStartDate, periodEndDate))
        )
          .provideLayer(
            daoLayer >+> ((daoLayer ++ expectations ++ Logging.live) >>> PlacementDiscountServiceLive.layer)
          )
          .map(_.placementDiscountPercent)
          .zip(checkDb)
          .map(Result.tupled)
      }(equalTo(Result(discount = 32, DB(stock = 90, discount = 32))))
    ),
    testM("Returns discount if available")(
      assertM {
        val expectations = DealerWarehouseMock.empty ++ DiscountPoliciesMock.empty ++ CabinetTest.empty
        (
          PlacementDiscountCalculationDao(_.writeStock(periodId, client.id, -10)) *>
            PlacementDiscountCalculationDao(_.saveDiscount(periodId, client.id, 97)) *>
            PlacementDiscountService(_.getOrCalcPlacementDiscount(periodId, client.id, periodStartDate, periodEndDate))
        )
          .provideLayer(
            daoLayer >+> ((daoLayer ++ expectations ++ Logging.live) >>> PlacementDiscountServiceLive.layer)
          )
          .map(_.placementDiscountPercent)
          .zip(checkDb)
          .map(Result.tupled)
      }(equalTo(Result(discount = 97, DB(stock = -10, discount = 97))))
    )
  )

  case class Result(discount: Percent, db: DB)
  case class DB(stock: Int, discount: Int)

  private def checkDb = transactor.flatMap(xa =>
    sql"""select stock_avg, placement_discount_percent
          from placement_discount_calculation
          where client_id = ${client.id} and period_id = $periodId"""
      .query[(Int, Percent)]
      .map(DB.tupled)
      .unique
      .transact(xa)
  )

  @mockable[DealerWarehouse]
  object DealerWarehouseMock

  @mockable[DiscountSettings]
  object DiscountPoliciesMock

  private def transactor = ZIO.service[Transactor[Task]]
  private def daoLayer = ZLayer.requires[Has[PlacementDiscountCalculationDao]]

  override def spec: ZSpec[TestEnvironment, Any] = (allSuites @@
    beforeAll(
      transactor
        .flatMap { xa =>
          for {
            _ <- InitSchema("/schema.sql", xa)
          } yield ()
        }
    ) @@
    after(
      transactor.flatMap(xa => sql"TRUNCATE TABLE placement_discount_calculation".update.run.transact(xa))
    ) @@
    sequential).provideCustomLayerShared(TestMySQL.managedTransactor >+> JdbcPlacementDiscountCalculationDao.layer)
}
