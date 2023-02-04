package ru.auto.salesman.dao

import com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException
import org.joda.time.DateTime
import org.scalatest.Inspectors
import ru.auto.salesman.dao.ClientsChangedBufferDao.DataSourceFilter
import ru.auto.salesman.dao.impl.jdbc.JdbcClientsChangedBufferDao.commonFilter
import ru.auto.salesman.dao.impl.jdbc.database.Database
import ru.auto.salesman.dao.slick.invariant.StaticQuery._
import ru.auto.salesman.model.PeriodId
import ru.auto.salesman.test.BaseSpec
import zio.ZIO

trait CashbackPeriodDaoSpec extends BaseSpec {
  def cashbackPeriodDao: CashbackPeriodDao
  def clientsChangedBufferDao: ClientsChangedBufferDao
  def database: Database

  "CashbackPeriodDao" should {

    "protect from duplication" in {
      val start = DateTime.now().minusDays(7)
      val finish = DateTime.now().minusDays(6)

      val zio = for {
        _ <- cashbackPeriodDao.insert(start, finish)
        _ <- cashbackPeriodDao.insert(start, finish)
      } yield ()

      zio.failure.cause.squash shouldBe a[
        MySQLIntegrityConstraintViolationException
      ]
    }

    "get period by id" in {
      val start = DateTime.now().minusDays(10)
      val finish = DateTime.now().minusDays(9)
      val z = for {
        _ <- cashbackPeriodDao.insert(start, finish)
        periods <- cashbackPeriodDao.getPeriods
        fetched <- ZIO
          .foreach(periods) { p =>
            cashbackPeriodDao.getById(p.id).map(_.map(p.id -> _))
          }
          .map(_.flatten.toMap)
      } yield {
        periods should not be empty
        periods.map { period =>
          period shouldBe fetched(period.id)
        }
      }

      z.success.value
    }

    "get all periods" in {
      val insertCount = 3
      val ids = (1 to insertCount).toList
      val periodIds = ids.map(id => PeriodId(id))

      val z = for {
        _ <- ZIO.foreach(ids) { i =>
          val start = DateTime.now().minusDays(i * 2 + 1)
          val finish = DateTime.now().minusDays(i * 2)
          cashbackPeriodDao.insert(start, finish)
        }
        periods <- cashbackPeriodDao.getPeriods
      } yield {
        periods should not be empty
        periods.size shouldBe insertCount

        periods.distinct should contain theSameElementsAs periods

        Inspectors.forEvery(periods) { p =>
          periodIds should contain(p.id)
          p.isActive shouldBe true
          if (p.id == periodIds.head)
            p.previousPeriod shouldBe empty
          else
            p.previousPeriod shouldBe 'nonEmpty
        }

      }
      z.success.value
    }
  }

  "close period by id" in {
    val startA = DateTime.parse("2020-03-01T00:00:00.000+03:00")
    val finishA = DateTime.parse("2020-03-31T00:00:00.000+03:00")

    val startB = DateTime.parse("2020-04-01T00:00:00.000+03:00")
    val finishB = DateTime.parse("2020-04-30T00:00:00.000+03:00")
    val z = for {
      _ <- cashbackPeriodDao.insert(startA, finishA)
      _ <- cashbackPeriodDao.insert(startB, finishB)
      old <- cashbackPeriodDao.getPeriods
      _ <- ZIO
        // don't close one period
        .foreach(old.tail) { p =>
          cashbackPeriodDao.closeById(p.id)
        }
      updated <- cashbackPeriodDao.getPeriods
    } yield {
      old should not be empty
      old.map { period =>
        period.isActive shouldBe true
      }
      updated.size shouldEqual old.size
      updated.count(_.isActive) shouldEqual 1
    }
    z.success.value
  }

  "close period twice " in {
    val start = DateTime.parse("2019-03-01T00:00:00.000+03:00")
    val finish = DateTime.parse("2019-03-31T00:00:00.000+03:00")
    val z = for {
      _ <- cashbackPeriodDao.insert(start, finish)
      periods <- cashbackPeriodDao.getPeriods
      id <- ZIO.succeed(periods.head.id)
      firstUpdate <- cashbackPeriodDao.closeById(id)
      secondUpdate <- cashbackPeriodDao.closeById(id)
      updatedPeriod <- cashbackPeriodDao.getById(id)
    } yield {
      firstUpdate should be(1)
      secondUpdate should be(1)
      updatedPeriod.map { p =>
        p.isActive shouldBe false
      }
    }
    z.success.value
  }

  "send updates to buffer table" in {
    val start = DateTime.parse("2020-03-01T00:00:00.000+03:00")
    val finish = DateTime.parse("2020-03-31T00:00:00.000+03:00")

    val z = for {
      _ <- cashbackPeriodDao.insert(start, finish)
      fetched <- cashbackPeriodDao.getPeriods
      _ = database.withTransaction { implicit session =>
        // period = fetched.head.id; resolution = 1;
        sqlu"""
          INSERT INTO loyalty_report (`period_id`, `client_id`, `loyalty_level`, `cashback_amount`, `cashback_percent`, `extra_bonus`, `has_full_stock`, `resolution`, `activations_amount`, `status`, `negative_resolution_pushed`, `manager_name`, `vas_spend_percent`, `placement_spend_percent`, `placement_discount_percent`)
          VALUES (${fetched.head.id}, 20101, 3, 10, 1, '', 0, 1, 0, 'approved', 0, '', 100, 0, 0);
        """.execute
        // period = fetched.head.id; resolution = 0; (SHOULD BE SENT)
        sqlu"""
          INSERT INTO loyalty_report (`period_id`, `client_id`, `loyalty_level`, `cashback_amount`, `cashback_percent`, `extra_bonus`, `has_full_stock`, `resolution`, `activations_amount`, `status`, `negative_resolution_pushed`, `manager_name`, `vas_spend_percent`, `placement_spend_percent`, `placement_discount_percent`)
          VALUES (${fetched.head.id}, 68, 3, 10, 1, '', 0, 0, 0, 'in_progress_negative', 0, '', 100, 0, 0);
        """.execute
        // period = fetched.head.id+1; resolution = 0;
        sqlu"""
          INSERT INTO loyalty_report (`period_id`, `client_id`, `loyalty_level`, `cashback_amount`, `cashback_percent`, `extra_bonus`, `has_full_stock`, `resolution`, `activations_amount`, `status`, `negative_resolution_pushed`, `manager_name`, `vas_spend_percent`, `placement_spend_percent`, `placement_discount_percent`)
          VALUES (${fetched.head.id + 1}, 68, 3, 10, 1, '', 0, 0, 0, 'in_progress_negative', 0, '', 100, 0, 0);
        """.execute
      }
      _ <- ZIO.foreach_(fetched) { p =>
        cashbackPeriodDao.closeById(p.id)
      }
      res <- clientsChangedBufferDao.get(
        DataSourceFilter(commonFilter.dataSources + "dealer_pony")
      )
    } yield {
      res.map(_.clientId) should contain theSameElementsAs (List(68, 68))
      res.map(_.dataSource) should contain theSameElementsAs (List(
        "loyalty_report",
        "dealer_pony"
      ))
    }
    z.success.value
  }

  "close period for nonexistent id " in {
    val z = for {
      countUpdate <- cashbackPeriodDao.closeById(PeriodId(100))
    } yield countUpdate should be(0)
    z.success.value
  }
}
