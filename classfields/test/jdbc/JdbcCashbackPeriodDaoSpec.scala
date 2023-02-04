package auto.dealers.loyalty.storage.jdbc

import auto.dealers.loyalty.storage.CashbackPeriodDao.SQLExecutionError
import common.zio.doobie.schema.InitSchema
import common.zio.doobie.testkit.TestMySQL
import doobie.Transactor
import doobie.implicits._
import zio.{Task, ZIO}
import zio.interop.catz._
import zio.test.{DefaultRunnableSpec, ZSpec, _}
import zio.test.Assertion._
import zio.test.environment.TestEnvironment
import zio.test.TestAspect._

import java.time.LocalDate

object JdbcCashbackPeriodDaoSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[TestEnvironment, Any] = {
    (suite("JdbcCashbackPeriodDao")(
      testM("protect from duplication") {
        val start = LocalDate.now().minusDays(7)
        val finish = LocalDate.now().minusDays(6)

        for {
          xa <- ZIO.service[Transactor[Task]]
          client = new JdbcCashbackPeriodDao(xa)
          firstResult <- client.insert(start, finish)
          secondResult <- client.insert(start, finish).run
        } yield assert(firstResult)(isUnit) &&
          assert(secondResult)(fails(isSubtype[SQLExecutionError](anything)))
      },
      testM("get period by id") {
        val start = LocalDate.now().minusDays(10)
        val finish = LocalDate.now().minusDays(9)

        for {
          xa <- ZIO.service[Transactor[Task]]
          client = new JdbcCashbackPeriodDao(xa)
          _ <- client.insert(start, finish)
          periods <- client.getPeriods
          fetched <- ZIO
            .foreach(periods) { p =>
              client.getById(p.id).map(_.map(p.id -> _))
            }
            .map(_.flatten.toMap)
        } yield {
          assert(periods)(isNonEmpty) &&
          assert(periods)(equalTo(periods.map(period => fetched(period.id))))
        }
      },
      testM("get all periods") {
        val insertCount = 3
        val ids = (1 to insertCount).toList

        for {
          xa <- ZIO.service[Transactor[Task]]
          client = new JdbcCashbackPeriodDao(xa)

          _ <- ZIO.foreach_(ids) { i =>
            val start = LocalDate.now().minusDays(i * 2 + 1)
            val finish = LocalDate.now().minusDays(i * 2)
            client.insert(start, finish)
          }

          periods <- client.getPeriods
        } yield {
          assert(periods)(isNonEmpty) &&
          assert(periods.size)(equalTo(insertCount)) &&
          assert(periods.distinct)(hasSameElements(periods)) &&
          assert(periods.map(_.id))(hasSameElements(ids)) &&
          assert(periods.map(_.isActive))(forall(equalTo(true))) &&
          assert(periods.filter(_.id == ids.head).map(_.previousPeriod))(forall(isNone)) &&
          assert(periods.filter(_.id != ids.head).map(_.previousPeriod))(forall(isSome))
        }
      },
      testM("close period by id") {
        val startA = LocalDate.parse("2020-03-01")
        val finishA = LocalDate.parse("2020-03-31")

        val startB = LocalDate.parse("2020-04-01")
        val finishB = LocalDate.parse("2020-04-30")

        for {
          xa <- ZIO.service[Transactor[Task]]
          client = new JdbcCashbackPeriodDao(xa)
          _ <- client.insert(startA, finishA)
          _ <- client.insert(startB, finishB)
          old <- client.getPeriods
          _ <- ZIO.foreach_(old.tail) { p =>
            client.closeById(p.id)
          }
          updated <- client.getPeriods
        } yield {
          assert(old)(isNonEmpty) &&
          assert(old.map(_.isActive))(equalTo(old.map(_ => true))) &&
          assert(updated.size)(equalTo(old.size)) &&
          assert(updated.count(_.isActive))(equalTo(1))
        }
      },
      testM("close period twice") {
        val start = LocalDate.parse("2019-03-01")
        val finish = LocalDate.parse("2019-03-31")

        for {
          xa <- ZIO.service[Transactor[Task]]
          client = new JdbcCashbackPeriodDao(xa)

          _ <- client.insert(start, finish)
          periods <- client.getPeriods
          id <- ZIO.succeed(periods.head.id)
          firstUpdate <- client.closeById(id)
          secondUpdate <- client.closeById(id)
          updatedPeriod <- client.getById(id)
        } yield {
          assert(firstUpdate)(isUnit) &&
          assert(secondUpdate)(isUnit) &&
          assert(updatedPeriod.map(_.isActive))(equalTo(updatedPeriod.map(_ => false)))
        }
      },
      testM("send updates to buffer table") {
        val start = LocalDate.parse("2020-03-01")
        val finish = LocalDate.parse("2020-03-31")

        for {
          xa <- ZIO.service[Transactor[Task]]
          client = new JdbcCashbackPeriodDao(xa)

          _ <- client.insert(start, finish)
          fetched <- client.getPeriods

          firstPeriod = fr"${fetched.head.id}"
          _ <-
            sql"""
          INSERT INTO loyalty_report (`period_id`, `client_id`, `loyalty_level`, `cashback_amount`, `cashback_percent`, `extra_bonus`, `has_full_stock`, `resolution`, `activations_amount`, `status`, `negative_resolution_pushed`, `manager_name`, `vas_spend_percent`, `placement_spend_percent`, `placement_discount_percent`)
          VALUES ($firstPeriod, 20101, 3, 10, 1, '', 0, 1, 0, 'approved', 0, '', 100, 0, 0);
        """.update.run.transact(xa)

          _ <-
            sql"""
          INSERT INTO loyalty_report (`period_id`, `client_id`, `loyalty_level`, `cashback_amount`, `cashback_percent`, `extra_bonus`, `has_full_stock`, `resolution`, `activations_amount`, `status`, `negative_resolution_pushed`, `manager_name`, `vas_spend_percent`, `placement_spend_percent`, `placement_discount_percent`)
          VALUES ($firstPeriod, 68, 3, 10, 1, '', 0, 0, 0, 'in_progress_negative', 0, '', 100, 0, 0);
        """.update.run.transact(xa)

          otherPeriod = fr"${fetched.head.id + 1}"
          _ <- sql"""
          INSERT INTO loyalty_report (`period_id`, `client_id`, `loyalty_level`, `cashback_amount`, `cashback_percent`, `extra_bonus`, `has_full_stock`, `resolution`, `activations_amount`, `status`, `negative_resolution_pushed`, `manager_name`, `vas_spend_percent`, `placement_spend_percent`, `placement_discount_percent`)
          VALUES ($otherPeriod, 68, 3, 10, 1, '', 0, 0, 0, 'in_progress_negative', 0, '', 100, 0, 0);
        """.update.run
            .transact(xa)

          _ <- sql"""select client_id from loyalty_report"""
            .query[Int]
            .to[List]
            .transact(xa)

          _ <- ZIO.foreach_(fetched) { p =>
            client.closeById(p.id)
          }

          res <-
            sql"""
                 SELECT client_id, data_source from clients_changed_buffer
               """
              .query[(Int, String)]
              .to[List]
              .transact(xa)
        } yield {
          assert(fetched)(isNonEmpty) &&
          assert(res.map(_._1))(equalTo(List(68, 68))) &&
          assert(res.map(_._2))(equalTo(List("loyalty_report", "dealer_pony")))
        }
      },
      testM("close period for nonexistent id") {
        for {
          xa <- ZIO.service[Transactor[Task]]
          client = new JdbcCashbackPeriodDao(xa)

          countUpdate <- client.closeById(100L)
        } yield assert(countUpdate)(isUnit)
      }
    ) @@
      beforeAll(
        ZIO
          .service[Transactor[Task]]
          .flatMap { xa =>
            for {
              _ <- InitSchema("/schema.sql", xa)
            } yield ()
          }
      ) @@
      after(ZIO.service[Transactor[Task]].flatMap(sql"TRUNCATE TABLE cashback_periods".update.run.transact(_).unit)) @@
      sequential).provideCustomLayerShared(TestMySQL.managedTransactor)
  }
}
