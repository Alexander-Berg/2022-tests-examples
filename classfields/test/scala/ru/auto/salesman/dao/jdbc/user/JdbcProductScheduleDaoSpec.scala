package ru.auto.salesman.dao.jdbc.user

import java.sql.{Timestamp, Types}

import cats.data.NonEmptySet
import org.joda.time.{DateTimeUtils, DateTimeZone, LocalDate, LocalTime}
import ru.auto.salesman.dao.impl.jdbc.database.Database
import ru.auto.salesman.dao.impl.jdbc.user.JdbcProductScheduleDao
import ru.auto.salesman.dao.slick.invariant
import ru.auto.salesman.dao.user.{
  ProductScheduleDao,
  ProductScheduleDaoSpec,
  ProductScheduleInsertDao
}
import ru.auto.salesman.model.DeprecatedDomain
import ru.auto.salesman.model.user.schedule._
import ru.auto.salesman.test.IntegrationPropertyCheckConfig
import ru.auto.salesman.test.template.SalesmanUserJdbcSpecTemplate
import ru.auto.salesman.util.JodaCatsInstances._

import scala.slick.jdbc.{PositionedParameters, StaticQuery}
import scala.util.Try

class JdbcProductScheduleDaoSpec
    extends ProductScheduleDaoSpec
    with SalesmanUserJdbcSpecTemplate
    with IntegrationPropertyCheckConfig {

  private lazy val dao = new JdbcProductScheduleDaoSpec.DaoForTests(database)

  def newDao(
      data: Iterable[ProductSchedule]
  ): ProductScheduleDao with ProductScheduleInsertDao = {
    dao.clean()
    if (data.nonEmpty) dao.insert(data).success
    dao
  }

  "JDBC product schedule dao" should {

    "save dates in yyyy-MM-dd format" in {
      forAll(
        productScheduleGen(
          parametersGen = scheduleOnceAtDatesGen(
            NonEmptySet.of(
              LocalDate.parse("2019-01-05"),
              LocalDate.parse("2018-12-30"),
              LocalDate.parse("2019-01-01"),
              LocalDate.parse("2019-01-13")
            )
          )
        )
      ) { schedule =>
        val dao = newDao(Nil)
        dao.insert(List(schedule)).success
        database.withSession { implicit session =>
          invariant.StaticQuery
            .queryNA[String]("SELECT dates FROM products_apply_schedule")
            .first shouldBe "2018-12-30,2019-01-01,2019-01-05,2019-01-13"
        }
      }
    }

    "fill in prev_schedule_id field on replace" in {
      forAll(
        productScheduleGen(isVisibleGen = IsVisible(true), isDeletedGen = false)
      ) { schedule =>
        val dao = newDao(Nil)

        val scheduleSource: ScheduleSource =
          scheduleToScheduleSource(schedule).copy(
            scheduleParameters = ScheduleOnceAtTimeGen.next
          )

        dao.insert(List(schedule)).success
        dao.replace(List(scheduleSource)).success

        database.withSession { implicit session =>
          // Table products_apply_schedule should contain 2 versions of the schedule...
          invariant.StaticQuery
            .queryNA[String]("SELECT COUNT(*) FROM products_apply_schedule")
            .first shouldBe "2"

          // ... and one of them should have a link to the previous version of schedule.
          invariant.StaticQuery
            .queryNA[String](
              s"SELECT COUNT(*) FROM products_apply_schedule WHERE prev_schedule_id = ${schedule.id}"
            )
            .first shouldBe "1"
        }
      }
    }

    "on replace() field allow_multiple_reschedule should be false even if schedule doesn't have predecessor" in {
      forAll(
        productScheduleGen(
          isVisibleGen = IsVisible(true),
          isDeletedGen = false,
          allowMultipleRescheduleGen = false
        )
      ) { schedule =>
        val dao = newDao(Nil)

        dao.replace(List(scheduleToScheduleSource(schedule))).success

        database.withSession { implicit session =>
          invariant.StaticQuery
            .queryNA[String]("SELECT COUNT(*) FROM products_apply_schedule")
            .first shouldBe "1"

          invariant.StaticQuery
            .queryNA[String](
              "SELECT COUNT(*) FROM products_apply_schedule WHERE allow_multiple_reschedule = 0"
            )
            .first shouldBe "1"
        }
      }
    }
  }
}

object JdbcProductScheduleDaoSpec {

  class DaoForTests(database: Database)(implicit domain: DeprecatedDomain)
      extends JdbcProductScheduleDao(database)
      with ProductScheduleInsertDao {

    def clean(): Unit =
      database.withSession { implicit session =>
        StaticQuery.queryNA[Int]("delete from products_apply_schedule").execute
      }

    def insert(schedules: Iterable[ProductSchedule]): Try[Unit] = Try {
      database.withTransaction { implicit session =>
        val stmt = session.conn.prepareStatement(
          "insert into products_apply_schedule" +
          "(id, offer_id, user, product, schedule_type, weekdays, dates, time," +
          "timezone, updated_at, is_deleted, epoch, is_visible, expire_date," +
          "custom_price, allow_multiple_reschedule, prev_schedule_id)" +
          s"values (${Iterator.continually("?").take(17).mkString(", ")})"
        )

        schedules.foreach { s =>
          val pp = new PositionedParameters(stmt)
          pp.setLong(s.id)
          pp.setString(s.offerId.value)
          pp.setString(s.user.toString)
          pp.setString(s.product.name)
          setParams(pp, s.scheduleParameters)
          pp.setString(timezoneAsOffset(s.scheduleParameters.timezone))
          pp.setTimestamp(new Timestamp(s.updatedAt.getMillis))
          pp.setBoolean(s.isDeleted)
          pp.setTimestamp(new Timestamp(s.epoch.getMillis))
          pp.setBoolean(s.isVisible.value)
          pp.setTimestampOption(
            s.expireDate.map(_.getMillis).map(new Timestamp(_))
          )
          pp.setLongOption(s.customPrice)
          pp.setBoolean(s.allowMultipleReschedule)
          pp.setLongOption(s.prevScheduleId)

          stmt.addBatch()
        }

        stmt.executeBatch()
      }
    }
  }

  private def timezoneAsOffset(timezone: DateTimeZone): String = {
    val offsetMillis = timezone.getOffset(DateTimeUtils.currentTimeMillis())
    DateTimeZone.forOffsetMillis(offsetMillis).toString
  }

  // scheduleType, weekdays, dates, time
  private def setParams(
      pp: PositionedParameters,
      sp: ScheduleParameters
  ): Unit =
    sp match {
      case s: ScheduleParameters.OnceAtTime =>
        pp.setString(ScheduleType.OnceAtTime.entryName)
        pp.setString(s.weekdays.toList.sorted.mkString(","))
        pp.setNull(Types.VARCHAR)
        pp.setTime(toTime(s.time))
      case s: ScheduleParameters.OnceAtDates =>
        pp.setString(ScheduleType.OnceAtDates.entryName)
        pp.setNull(Types.VARCHAR)
        pp.setString(s.dates.toSortedSet.mkString(","))
        pp.setTime(toTime(s.time))
    }

  def toTime(t: LocalTime): java.sql.Time = {
    val javaTime = java.time.LocalTime.ofSecondOfDay(t.getMillisOfDay / 1000)
    val result = java.sql.Time.valueOf(javaTime)
    result
  }
}
