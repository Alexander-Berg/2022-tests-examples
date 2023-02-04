package ru.yandex.hydra.profile.dao.limiter.dao

import com.codahale.metrics.MetricRegistry
import com.datastax.driver.core.Row
import org.scalatest.concurrent.ScalaFutures
import org.slf4j.LoggerFactory
import ru.yandex.common.monitoring.{InstrumentedBuilder, Metrics}
import ru.yandex.hydra.profile.dao.SpecBase
import ru.yandex.hydra.profile.dao.cassandra.TestCassandra
import ru.yandex.hydra.profile.dao.limiter.dao.CassandraLimiterDaoSpec._
import ru.yandex.hydra.profile.dao.limiter.dao.impl.CassandraLimiter

import scala.jdk.CollectionConverters._
import scala.concurrent.duration._

/** @author @logab
  */
class CassandraLimiterDaoSpec extends SpecBase with ScalaFutures with TestCassandra {

  val log = LoggerFactory.getLogger(classOf[CassandraLimiterDaoSpec])

  import scala.concurrent.ExecutionContext.Implicits.global

  private val thisNodeId: Long = 0L
  private val p: String = project
  private val l: String = locale
  private val c: String = component

  val cassandraDao: CassandraLimiter =
    new CassandraLimiter(
      session,
      p,
      l,
      c,
      Ttl,
      thisNodeId,
      createTable = true
    ) with MeteredCassandraLimiterDao with InstrumentedBuilder {
      override def metricRegistry: MetricRegistry = Metrics.defaultRegistry()
    }

  private val thatNodeId: Long = 1L

  val anotherNodeDao: CassandraLimiter =
    new CassandraLimiter(
      session,
      p,
      l,
      c,
      Ttl,
      thatNodeId,
      createTable = true
    ) with MeteredCassandraLimiterDao with InstrumentedBuilder {
      override def metricRegistry: MetricRegistry = Metrics.defaultRegistry()
    }

  implicit val cfg = PatienceConfig(700.millis, 10.millis)

  override def beforeEach(): Unit = {
    val _ = session.execute(s"TRUNCATE TABLE ${cassandraDao.table}")
  }

  override def afterEach(): Unit = {
    val _ = session.execute(s"TRUNCATE TABLE ${cassandraDao.table}")
  }

  override def afterAll(): Unit = {
    val _ = session.execute(s"DROP TABLE IF EXISTS ${cassandraDao.table}")
  }

  val User = "User"
  val Value = 1

  def check(rows: Iterable[Row], value: Int): Unit = {
    val _ = rows.map(_.getInt(CassandraLimiter.ValueField)).sum shouldEqual value
  }

  "CassandraLimiterDao" should {
    "store value" in {
      cassandraDao.dump(User, 0, Value).futureValue
      check(session.execute(s"SELECT * FROM ${cassandraDao.table}").all().asScala, Value)
    }
    "rewrite value" in {
      val newValue = Value * 2
      cassandraDao.dump(User, 0, Value).futureValue
      cassandraDao.dump(User, 0, newValue).futureValue
      check(session.execute(s"SELECT * FROM ${cassandraDao.table}").all().asScala, newValue)
    }
    "store values in several ticks" in {
      cassandraDao.dump(User, 0, Value).futureValue
      cassandraDao.dump(User, 1, Value).futureValue
      check(session.execute(s"SELECT * FROM ${cassandraDao.table}").all().asScala, Value * 2)
    }
    "expire value" in {
      cassandraDao.dump(User, 0, Value).futureValue
      Thread.sleep(Ttl.toMillis)
      check(session.execute(s"SELECT * FROM ${cassandraDao.table}").all().asScala, 0)
    }
    "get value" in {
      cassandraDao.dump(User, 0, Value).futureValue
      cassandraDao.get(User).futureValue shouldEqual Value
    }
    "get values from different ticks" in {
      cassandraDao.dump(User, 0, Value).futureValue
      cassandraDao.dump(User, 1, Value).futureValue
      cassandraDao.get(User).futureValue shouldEqual Value * 2
    }
  }

  "CassandraLimiters on different nodes" should {
    val thisValue = 1
    val thatValue = 31
    "store independently" in {
      cassandraDao.dump(User, 0, thisValue).futureValue
      anotherNodeDao.dump(User, 0, thatValue).futureValue
      check(session.execute(s"SELECT * FROM ${cassandraDao.table}").all().asScala, thisValue + thatValue)
    }
    "get only stored in this node" in {
      import CassandraLimiter._
      cassandraDao.dump(User, 0, thisValue).futureValue
      anotherNodeDao.dump(User, 0, thatValue).futureValue
      check(
        session
          .execute(s"""
           |SELECT $ValueField
           |  FROM ${cassandraDao.table}
           |    WHERE $UserIdField = '$User'
           |    AND $NodeIdField = $thisNodeId
         """.stripMargin)
          .asScala,
        thisValue
      )
      check(
        session
          .execute(s"""
           |SELECT $ValueField
           |  FROM ${cassandraDao.table}
           |    WHERE $UserIdField = '$User'
           |    AND $NodeIdField = $thatNodeId
         """.stripMargin)
          .asScala,
        thatValue
      )
      cassandraDao.get(User).futureValue shouldEqual thisValue + thatValue
      anotherNodeDao.get(User).futureValue shouldEqual thisValue + thatValue
    }
  }
}

object CassandraLimiterDaoSpec {
  val Ttl = 2.seconds
}
