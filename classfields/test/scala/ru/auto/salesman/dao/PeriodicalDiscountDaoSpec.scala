package ru.auto.salesman.dao

import org.joda.time.DateTime
import ru.auto.salesman.dao.impl.jdbc.jodaDateTimeAsSqlTimestamp
import ru.auto.salesman.dao.impl.jdbc.user.JdbcPeriodicalDiscountDao
import ru.auto.salesman.dao.user.PeriodicalDiscountDao
import ru.auto.salesman.dao.user.PeriodicalDiscountDao.StartedFilter._
import ru.auto.salesman.dao.user.PeriodicalDiscountDao.HaveExclusionsFilter._
import ru.auto.salesman.model.AutoruUser
import ru.auto.salesman.model.user.{PeriodicalDiscount, PeriodicalDiscountContext}
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.template.SalesmanUserJdbcSpecTemplate

import scala.slick.jdbc.{JdbcBackend, PositionedParameters, StaticQuery}

class PeriodicalDiscountDaoSpec
    extends BaseSpec
    with PeriodicalDiscountDaoSpec.JdbcTemplate
    with SalesmanUserJdbcSpecTemplate {

  import PeriodicalDiscountDaoSpec._

  def dao = new JdbcPeriodicalDiscountDao(transactor, transactor)

  "GetActive discounts" should {

    "return None if there is no active discount" in {
      clean()
      val discounts = List(
        PeriodicalDiscount(
          discountId = "1",
          start = currentTime.minusMinutes(2),
          deadline = currentTime.minusMinutes(1),
          discount = 10,
          context = None
        )
      )
      database.withSession { implicit session =>
        insertDiscounts(discounts)
      }
      dao
        .getActive(PeriodicalDiscountDao.ActiveFilter.All)
        .provideConstantClock(currentTime)
        .success
        .value shouldEqual None
    }

    "return discount if there is active discount" in {
      clean()
      val discounts = List(
        PeriodicalDiscount(
          discountId = "1",
          start = currentTime.minusMinutes(2),
          deadline = currentTime.minusMinutes(1),
          discount = 10,
          context = Some(PeriodicalDiscountContext(Some(List("turbo-package"))))
        ),
        availableDiscount,
        PeriodicalDiscount(
          discountId = "3",
          start = currentTime.plusMinutes(1),
          deadline = currentTime.plusMinutes(2),
          discount = 30,
          context = Some(PeriodicalDiscountContext(Some(List("turbo-package"))))
        )
      )
      database.withSession { implicit session =>
        insertDiscounts(discounts)
      }

      val result = dao
        .getActive(PeriodicalDiscountDao.ActiveFilter.All)
        .provideConstantClock(currentTime)
        .success
        .value
        .value

      result shouldEqual availableDiscount
    }

    "filter by user and return right discount if user not excluded" in {
      clean()
      val excludedUser = AutoruUser("user:1")
      val userForCheck = AutoruUser("user:2")
      prepareDataForExclusions(availableDiscount, excludedUser)

      val result = dao
        .getActive(PeriodicalDiscountDao.ActiveFilter.ForUser(userForCheck))
        .provideConstantClock(currentTime)
        .success
        .value
        .value

      result shouldEqual availableDiscount
    }

    "filter by user and return None if user excluded" in {
      clean()
      val excludedUser = AutoruUser("user:1")
      val userForCheck = excludedUser
      prepareDataForExclusions(availableDiscount, excludedUser)

      dao
        .getActive(PeriodicalDiscountDao.ActiveFilter.ForUser(userForCheck))
        .provideConstantClock(currentTime)
        .success
        .value shouldEqual None
    }
  }

  "GetAll discounts" should {
    "return only tomorrow discount if it exists with ForDate filter" in {
      clean()
      val tomorrow = currentTime.plusDays(1)
      val expectedResult = PeriodicalDiscount(
        discountId = "3",
        start = tomorrow.withTimeAtStartOfDay(),
        deadline = tomorrow.plusDays(1),
        discount = 30,
        context = Some(PeriodicalDiscountContext(Some(List("turbo-package"))))
      )
      val discounts = List(
        availableDiscount,
        expectedResult,
        PeriodicalDiscount(
          discountId = "4",
          start = tomorrow.plusDays(2),
          deadline = tomorrow.plusDays(3),
          discount = 40,
          context = Some(PeriodicalDiscountContext(Some(List("turbo-package"))))
        )
      )
      database.withSession { implicit session =>
        insertDiscounts(discounts)
      }

      val result = dao
        .getAll(PeriodicalDiscountDao.StartedFilter.ForDate(tomorrow), None)
        .provideConstantClock(currentTime)
        .success
        .value

      result should contain theSameElementsAs List(expectedResult)
    }

    "return empty if tomorrow discount not exists with ForDate filter" in {
      clean()
      val tomorrow = currentTime.plusDays(1)
      val nextWeek = currentTime.plusDays(7)

      val discounts = List(
        availableDiscount,
        PeriodicalDiscount(
          discountId = "3",
          start = nextWeek.withTimeAtStartOfDay(),
          deadline = nextWeek.plusDays(1),
          discount = 30,
          context = Some(PeriodicalDiscountContext(Some(List("turbo-package"))))
        )
      )
      database.withSession { implicit session =>
        insertDiscounts(discounts)
      }

      val result = dao
        .getAll(PeriodicalDiscountDao.StartedFilter.ForDate(tomorrow), None)
        .provideConstantClock(currentTime)
        .success
        .value
        .headOption

      result shouldEqual None
    }

    "return nextWeek discounts if it exists with ForDateRange filter" in {
      clean()
      val tomorrow = currentTime.plusDays(1)
      val nextWeek = currentTime.plusDays(7)
      val expectedResult = List(
        PeriodicalDiscount(
          discountId = "3",
          start = nextWeek.withTimeAtStartOfDay(),
          deadline = nextWeek.plusDays(1),
          discount = 30,
          context = Some(PeriodicalDiscountContext(Some(List("turbo-package"))))
        ),
        PeriodicalDiscount(
          discountId = "4",
          start = nextWeek.plusDays(1),
          deadline = nextWeek.plusDays(2),
          discount = 40,
          context = Some(PeriodicalDiscountContext(Some(List("turbo-package"))))
        )
      )

      val discounts = List(
        availableDiscount,
        PeriodicalDiscount(
          discountId = "5",
          start = nextWeek.plusDays(10),
          deadline = nextWeek.plusDays(11),
          discount = 50,
          context = Some(PeriodicalDiscountContext(Some(List("turbo-package"))))
        )
      ) ++ expectedResult

      database.withSession { implicit session =>
        insertDiscounts(discounts)
      }

      val result = dao
        .getAll(ForDateRange(tomorrow, nextWeek.plusDays(7)), None)
        .provideConstantClock(currentTime)
        .success
        .value

      result should contain theSameElementsAs expectedResult
    }

    "return all discounts if exclusions filter isn't set" in {
      clean()
      val tomorrow = new DateTime(currentTime.plusDays(1))
      val nextWeek = new DateTime(currentTime.plusDays(7))
      val expectedResult = List(
        PeriodicalDiscount(
          discountId = "3",
          start = nextWeek.withTimeAtStartOfDay(),
          deadline = nextWeek.plusDays(1),
          discount = 30,
          context = Some(PeriodicalDiscountContext(Some(List("turbo-package"))))
        ),
        PeriodicalDiscount(
          discountId = "4",
          start = nextWeek.plusDays(1),
          deadline = nextWeek.plusDays(2),
          discount = 40,
          context = Some(PeriodicalDiscountContext(Some(List("turbo-package"))))
        )
      )

      val result =
        for {
          _ <- dao.insertDiscountExclusions(
            expectedResult.head.discountId,
            List(AutoruUser("user:1"))
          )
          _ = database.withSession(insertDiscounts(expectedResult)(_))
          result <- dao.getAll(
            ForDateRange(tomorrow, nextWeek.plusDays(7)),
            None
          )
        } yield result

      result
        .provideConstantClock(currentTime)
        .success
        .value should contain theSameElementsAs expectedResult
    }

    "return discounts without ones with exclusions if filter set to WithoutExclusions" in {
      clean()
      val tomorrow = new DateTime(currentTime.plusDays(1))
      val nextWeek = new DateTime(currentTime.plusDays(7))
      val expectedResult =
        PeriodicalDiscount(
          discountId = "3",
          start = nextWeek.withTimeAtStartOfDay(),
          deadline = nextWeek.plusDays(1),
          discount = 30,
          context = Some(PeriodicalDiscountContext(Some(List("turbo-package"))))
        )
      val discountWithExclusions =
        PeriodicalDiscount(
          discountId = "4",
          start = nextWeek.plusDays(1),
          deadline = nextWeek.plusDays(2),
          discount = 40,
          context = Some(PeriodicalDiscountContext(Some(List("turbo-package"))))
        )

      val discounts = List(
        expectedResult,
        discountWithExclusions
      )

      val result =
        for {
          _ <- dao.insertDiscountExclusions(
            discountWithExclusions.discountId,
            List(AutoruUser("user:1"))
          )
          _ = database.withSession(insertDiscounts(discounts)(_))
          result <- dao.getAll(
            ForDateRange(tomorrow, nextWeek.plusDays(7)),
            Some(WithoutExclusions)
          )
        } yield result

      result
        .provideConstantClock(currentTime)
        .success
        .value should contain theSameElementsAs List(expectedResult)
    }

    "return discounts with exclusions loaded if filter set to WithExclusions" in {
      clean()
      val tomorrow = new DateTime(currentTime.plusDays(1))
      val nextWeek = new DateTime(currentTime.plusDays(7))
      val expectedResult =
        PeriodicalDiscount(
          discountId = "3",
          start = nextWeek.withTimeAtStartOfDay(),
          deadline = nextWeek.plusDays(1),
          discount = 30,
          context = Some(PeriodicalDiscountContext(Some(List("turbo-package"))))
        )
      val discountWithoutExclusions =
        PeriodicalDiscount(
          discountId = "4",
          start = nextWeek.plusDays(1),
          deadline = nextWeek.plusDays(2),
          discount = 40,
          context = Some(PeriodicalDiscountContext(Some(List("turbo-package"))))
        )

      val discounts = List(
        expectedResult,
        discountWithoutExclusions
      )

      val result =
        for {
          _ <- dao.insertDiscountExclusions(
            expectedResult.discountId,
            List(AutoruUser("user:1"))
          )
          _ = database.withSession(insertDiscounts(discounts)(_))
          result <- dao.getAll(
            ForDateRange(tomorrow, nextWeek.plusDays(7)),
            Some(WithExclusions)
          )
        } yield result

      result
        .provideConstantClock(currentTime)
        .success
        .value should contain theSameElementsAs List(expectedResult)
    }
  }

  "Exclusions" should {

    "return empty exclusions if there is no data for discount" in {
      clean()
      dao
        .getExcludedUsersByDiscount("1")
        .success
        .value shouldEqual List.empty
    }

    "return right exclusions" in {
      val testDao = dao
      clean()
      val discountId = "1"
      val expectedResult = List(AutoruUser("user:1"), AutoruUser("user:2"))

      val result = (for {
        _ <- testDao.insertDiscountExclusions(discountId, expectedResult)
        _ <- testDao.insertDiscountExclusions(
          "2",
          List(1, 3).map(AutoruUser(_))
        )
        res <- testDao.getExcludedUsersByDiscount(discountId)
      } yield res).success.value

      result shouldEqual expectedResult
    }

    "successful insert duplicates into exclusions" in {
      val testDao = dao
      clean()
      val discountId = "1"
      val users1 = List("user:1", "user:2").map(AutoruUser(_))
      val users2 = List("user:1", "user:3").map(AutoruUser(_))
      val expectedResult = (users1 ++ users2).toSet

      val result = (for {
        _ <- testDao.insertDiscountExclusions("2", users1)
        _ <- testDao.insertDiscountExclusions(discountId, users1)
        _ <- testDao.insertDiscountExclusions(discountId, users2)
        res <- testDao.getExcludedUsersByDiscount(discountId)
      } yield res).success.value

      result should contain theSameElementsAs expectedResult
    }

    "don't touch other records while inserting new exclusions" in {
      val testDao = dao
      clean()
      val discountId1 = "1"
      val discountId2 = "2"
      val users1 = List("user:1", "user:2").map(AutoruUser(_))
      val users2 = List("user:1", "user:3").map(AutoruUser(_))

      val result = (for {
        _ <- testDao.insertDiscountExclusions(discountId1, users1)
        _ <- testDao.insertDiscountExclusions(discountId2, users2)
        res <- testDao.getExcludedUsersByDiscount(discountId1)
      } yield res).success.value

      result should contain theSameElementsAs users1
    }
  }
}

object PeriodicalDiscountDaoSpec {

  private val currentTime = DateTime.parse("2020-10-14T10:00:00.000")

  private val availableDiscount = PeriodicalDiscount(
    discountId = "2",
    start = currentTime.minusMinutes(1),
    deadline = currentTime.plusMinutes(1),
    discount = 20,
    context = Some(PeriodicalDiscountContext(Some(List("turbo-package"))))
  )

  trait JdbcTemplate { _: SalesmanUserJdbcSpecTemplate =>

    protected def clean(): Unit = database.withTransaction { implicit session =>
      StaticQuery.queryNA[Int](s"delete from `periodical_discount`").execute
      StaticQuery
        .queryNA[Int](s"delete from `periodical_discount_user_exclusion`")
        .execute
    }

    protected def prepareDataForExclusions(
        discount: PeriodicalDiscount,
        excludedUser: AutoruUser
    ): Unit =
      database.withSession { implicit session =>
        StaticQuery
          .queryNA[Int](s"""
                           |insert into periodical_discount_user_exclusion (periodical_discount_id, user_id) values
                           |(${discount.discountId}, "${excludedUser.toString}"),
                           |(20, "user:2");
                           |""".stripMargin)
          .execute

        insertDiscounts(List(discount))
      }

    protected def insertDiscounts(
        discounts: Iterable[PeriodicalDiscount],
        contextJson: String = """{"products": ["turbo-package"]}"""
    )(implicit session: JdbcBackend.Session): Unit = {
      val stmt = session.conn.prepareStatement(
        s"""insert into periodical_discount (id, start, deadline, discount, context, epoch) values 
          |(?, ?, ?, ?, '$contextJson', now())""".stripMargin
      )

      discounts.foreach { d =>
        val pp = new PositionedParameters(stmt)
        pp.setInt(d.discountId.toInt)
        pp.setTimestamp(jodaDateTimeAsSqlTimestamp(d.start))
        pp.setTimestamp(jodaDateTimeAsSqlTimestamp(d.deadline))
        pp.setInt(d.discount)
        stmt.addBatch()
      }
      stmt.executeBatch()
    }

  }

}
