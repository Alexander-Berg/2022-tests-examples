package ru.yandex.vertis.billing.dao.monitoring

import com.google.common.util.concurrent.ThreadFactoryBuilder
import org.joda.time.DateTime
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.dao.impl.jdbc._
import ru.yandex.vertis.billing.util.DateTimeUtils.now
import slick.jdbc.MySQLProfile.api._

import java.util.concurrent.ScheduledThreadPoolExecutor
import scala.annotation.nowarn
import scala.concurrent.duration._
import scala.util.Try

/**
  * Perf spec on [[DatabaseMonitor]]
  *
  * @author alesavin
  */
class DatabaseMonitorSpec extends AnyWordSpec with Matchers with JdbcSpecTemplate {

  private val PingQuery = sql"SELECT 1"

  "Database pinger" should {
    "run frequently queries if first is long" in {

      val TestDatabase = holdDatabase

      val Scheduler =
        new ScheduledThreadPoolExecutor(1, new ThreadFactoryBuilder().setNameFormat("test-database-monitor-%d").build())

      val Period = 50.millis
      val Sleep = 250.millis
      var IsFirst = true
      var Touches = List.empty[DateTime]

      val Pinger = new Runnable {

        def run(): Unit = {
          val r = ping
          r.get: @nowarn("msg=discarded non-Unit value")
        }

        def ping = Try {
          TestDatabase.runSync {
            Touches = now() :: Touches
            if (IsFirst) {
              IsFirst = false
              Thread.sleep(Sleep.toMillis)
            }
            PingQuery.as[Int].head
          }
        }
      }

      Scheduler.scheduleWithFixedDelay(
        Pinger,
        0,
        Period.length,
        Period.unit
      )

      Thread.sleep((Sleep + Period + Period).toMillis)
      Touches.size should be < 4
    }
  }
}
