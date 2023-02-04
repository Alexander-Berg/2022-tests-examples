package ru.yandex.util.cassandra.cache

import ru.yandex.common.monitoring.error.AlwaysWarningErrorPercentileTimeWindowReservoir
import ru.yandex.util.cassandra.{StaticSessionFactory, TestCassandra}

import com.datastax.driver.core.Session
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Matchers, WordSpec}
import org.slf4j.LoggerFactory

import java.util

import scala.collection.JavaConverters._
import scala.concurrent.duration._

class TtlCassandraCacheIntSpec
  extends WordSpec
    with Matchers
    with BeforeAndAfterAll
    with BeforeAndAfterEach
    with TestCassandra {

  val sf =
    new StaticSessionFactory(
      cassandraHost,
      cassandraDc,
      Some(cassandraCredentials),
      Some(cassandraKeyspace),
      port = Some(cassandraPort))(new AlwaysWarningErrorPercentileTimeWindowReservoir(5, 1.hour))
  var session: Session = null

  override def beforeAll() {
    session = sf.createSession()
  }



  override protected def beforeEach(): Unit = {
    session.execute(s"drop table if exists ${TestTtlDeserializer.tableName}")
  }

  override protected def afterEach(): Unit = {
    session.execute(s"drop table if exists ${TestTtlDeserializer.tableName}")
  }

  override def afterAll(): Unit = {
    session.close()
    sf.close()
  }

  def getCache: TtlCassandraCache[TestKey, TestValue] = new
      TtlCassandraCache(DummyBuilder, session, TestTtlDeserializer, 100.seconds)

  val log = LoggerFactory.getLogger(classOf[TtlCassandraCacheIntSpec])

  val testSet = Set(
            TestKey("4748495241724191055_0"),
            TestKey("4748495241724191055_1"),
            TestKey("8413697910796827614_0")
              ).asJava

  "Cassandra cache" should {
    "build new values" in {
      val keyToValue: util.Map[TestKey, TestValue] = getCache.get(testSet, -1)
      keyToValue.asScala.foreach{
        case (cacheKey, cacheValue) =>
          val expected = DummyBuilder.build(Set(cacheKey).asJava).get(cacheKey)
          val actual = cacheValue
          assert(expected === actual)
      }
    }
    "build new values by cache" in {
      val keyToValue: util.Map[TestKey, TestValue] = getCache.get(testSet, -1)
      keyToValue.asScala.foreach {
        case (cacheKey, cacheValue) =>
          val expected = DummyBuilder.build(Set(cacheKey).asJava).get(cacheKey)
          val actual = cacheValue
          assert(expected === actual)
      }
    }

  }
}
