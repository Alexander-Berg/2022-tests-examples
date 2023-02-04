package ru.auto.salesman.dao.impl.jdbc.test

import org.joda.time.DateTime
import ru.auto.salesman.dao.impl.jdbc._
import ru.auto.salesman.dao.impl.jdbc.database.Database
import ru.auto.salesman.model.ScheduleInstance

import scala.slick.jdbc.{PositionedParameters, StaticQuery}

class JdbcScheduleInstanceDaoForTests(
    val database: Database,
    val table: String,
    val scheduleIdColumn: String
) extends JdbcScheduleInstanceDao {

  def clean(): Unit =
    database.withSession { implicit session =>
      StaticQuery.queryNA[Int](s"delete from $table").execute
    }

  def insertInstances(instances: Iterable[ScheduleInstance]): Unit =
    database.withTransaction { implicit session =>
      val stmt = session.conn.prepareStatement(s"""
             insert into $table
                    (id, $scheduleIdColumn, fire_time, create_time, schedule_update_time, status, epoch)
             values (?, ?, ?, ?, ?, ?, ?)
          """)

      instances.foreach { instance =>
        val pp = new PositionedParameters(stmt)
        pp.setLong(instance.id)
        pp.setLong(instance.scheduleId)
        pp.>>[DateTime](instance.fireTime)
        pp.>>[DateTime](instance.createTime)
        pp.>>[DateTime](instance.scheduleUpdateTime)
        pp.setString(instance.status.toString)
        pp.>>[DateTime](instance.epoch)
        stmt.addBatch()
      }

      stmt.executeBatch()
    }
}
