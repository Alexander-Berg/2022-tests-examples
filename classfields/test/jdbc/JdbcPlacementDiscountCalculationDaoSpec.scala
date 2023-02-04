package auto.dealers.loyalty.storage.jdbc

import auto.dealers.loyalty.storage.PlacementDiscountCalculationDao.InvalidOperationError
import common.zio.doobie.schema.InitSchema
import common.zio.doobie.testkit.TestMySQL
import common.zio.testkit.failsWith
import doobie.Transactor
import doobie.implicits._
import zio.interop.catz._
import zio.test.Assertion._
import zio.test.TestAspect.{after, beforeAll, sequential}
import zio.test._
import zio.test.environment.TestEnvironment
import zio.{Task, ZIO}

object JdbcPlacementDiscountCalculationDaoSpec extends DefaultRunnableSpec {

  private val allSuits = suite("PlacementDiscountCalculationDao")(
    suite("Get stock")(
      testM("None if period+client not found") {
        val initialValues = sql"""
            insert into placement_discount_calculation (period_id, client_id, stock_avg)
            value (8, 8, 90)
             """.update.run
        for {
          xa <- transactor
          _ <- initialValues.transact(xa)
          stock <- JdbcPlacementDiscountCalculationDao(_.getStock(0, 0))
        } yield assert(stock)(isNone)
      },
      testM("Some stock if period+client record found") {
        val initialValues = sql"""
          insert into placement_discount_calculation (period_id, client_id, stock_avg)
          value (1, 1, 50)
           """.update.run
        for {
          xa <- transactor
          _ <- initialValues.transact(xa)
          stock <- JdbcPlacementDiscountCalculationDao(_.getStock(1, 1))
        } yield assert(stock)(isSome(equalTo(50)))
      }
    ),
    suite("Sum stock for company")(
      testM("None if no company stock") {
        val initialValues = sql"""
            insert into placement_discount_calculation (period_id, client_id, company_id, stock_avg)
            value (1, 1, null, 90)
             """.update.run
        for {
          xa <- transactor
          _ <- initialValues.transact(xa)
          stock <- JdbcPlacementDiscountCalculationDao(_.sumCompanyStock(1, 4))
        } yield assert(stock)(isNone)
      },
      testM("Zero for company with zero stock") {
        val initialValues = sql"""
            insert into placement_discount_calculation
            (period_id, client_id, company_id, stock_avg)
            values
            (1, 1, 4, 0),
            (1, 2, 4, 0)
             """.update.run
        for {
          xa <- transactor
          _ <- initialValues.transact(xa)
          sum <- JdbcPlacementDiscountCalculationDao(_.sumCompanyStock(1, 4))
        } yield assert(sum)(isSome(equalTo(0)))
      },
      testM("Sum for company stocks") {
        val initialValues = sql"""
            insert into placement_discount_calculation
            (period_id, client_id, company_id, stock_avg)
            values
            (1, 1, 4, 40),
            (1, 2, 4, 50)
             """.update.run
        for {
          xa <- transactor
          _ <- initialValues.transact(xa)
          sum <- JdbcPlacementDiscountCalculationDao(_.sumCompanyStock(1, 4))
        } yield assert(sum)(isSome(equalTo(90)))
      }
    ),
    suite("Write stock")(
      testM("Write stock for dealer") {
        for {
          _ <- JdbcPlacementDiscountCalculationDao(_.writeStock(1, 1, 60))
          stock <- JdbcPlacementDiscountCalculationDao(_.getStock(1, 1))
        } yield assert(stock)(isSome(equalTo(60)))
      },
      testM("Write stock for company") {
        for {
          _ <- JdbcPlacementDiscountCalculationDao(_.writeStock(1, 1, 50, Some(4)))
          _ <- JdbcPlacementDiscountCalculationDao(_.writeStock(1, 2, 40, Some(4)))
          sum <- JdbcPlacementDiscountCalculationDao(_.sumCompanyStock(1, 4))
        } yield assert(sum)(isSome(equalTo(90)))
      },
      testM("Cannot update stock") {
        assertM((for {
          _ <- JdbcPlacementDiscountCalculationDao(_.writeStock(1, 1, 30))
          _ <- JdbcPlacementDiscountCalculationDao(_.writeStock(1, 1, 60))
        } yield ()).run)(failsWith[InvalidOperationError])
      }
    ),
    suite("GetPlacementDiscount")(
      testM("None if period+client not found") {
        val initialValues =
          sql"""
            insert into placement_discount_calculation
            (period_id, client_id, stock_avg)
            value (8, 8, 30)
            """.update.run
        for {
          xa <- transactor
          _ <- initialValues.transact(xa)
          discount <- JdbcPlacementDiscountCalculationDao(_.getDiscount(0, 0))
        } yield assert(discount)(isNone)
      },
      testM("None if not calculated discount") {
        val initialValues = sql"""
            insert into placement_discount_calculation
            (period_id, client_id, stock_avg, placement_discount_percent)
            value (1, 1, 90, null)
            """.update.run
        for {
          xa <- transactor
          _ <- initialValues.transact(xa)
          discount <- JdbcPlacementDiscountCalculationDao(_.getDiscount(1, 1))
        } yield assert(discount)(isNone)
      },
      testM("Some discount if discount ready") {
        val initialValues = sql"""
            insert into placement_discount_calculation
            (period_id, client_id, stock_avg, placement_discount_percent)
            value (1, 1, 90, 4)
            """.update.run
        for {
          xa <- transactor
          _ <- initialValues.transact(xa)
          discount <- JdbcPlacementDiscountCalculationDao(_.getDiscount(1, 1))
        } yield assert(discount)(isSome(equalTo(4)))
      }
    ),
    suite("Set placement discount")(
      testM("Can set new discount") {
        for {
          _ <- JdbcPlacementDiscountCalculationDao(_.writeStock(1, 1, 90))
          _ <- JdbcPlacementDiscountCalculationDao(_.saveDiscount(1, 1, 3))
          discount <- JdbcPlacementDiscountCalculationDao(_.getDiscount(1, 1))
        } yield assert(discount)(isSome(equalTo(3)))
      },
      testM("Cannot save discount if no stock")(
        assertM(JdbcPlacementDiscountCalculationDao(_.saveDiscount(1, 1, 8)).run)(failsWith[InvalidOperationError])
      ),
      testM("Cannot update discount")(
        assertM((for {
          _ <- JdbcPlacementDiscountCalculationDao(_.writeStock(1, 1, 90))
          _ <- JdbcPlacementDiscountCalculationDao(_.saveDiscount(1, 1, 3))
          _ <- JdbcPlacementDiscountCalculationDao(_.saveDiscount(1, 1, 8))
        } yield ()).run)(failsWith[InvalidOperationError])
      )
    )
  )

  private def transactor = ZIO.service[Transactor[Task]]

  override def spec: ZSpec[TestEnvironment, Any] = (allSuits @@
    beforeAll(
      transactor
        .flatMap { xa =>
          for {
            _ <- InitSchema("/schema.sql", xa)
          } yield ()
        }
    ) @@
    after(
      transactor
        .flatMap(xa => sql"TRUNCATE TABLE placement_discount_calculation".update.run.transact(xa))
    )
    @@ sequential).provideCustomLayerShared(TestMySQL.managedTransactor >+> JdbcPlacementDiscountCalculationDao.layer)
}
