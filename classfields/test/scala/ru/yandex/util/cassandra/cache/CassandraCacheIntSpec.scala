package ru.yandex.util.cassandra.cache

import ru.yandex.common.monitoring.error.AlwaysWarningErrorPercentileTimeWindowReservoir
import ru.yandex.util.cassandra.{StaticSessionFactory, TestCassandra}

import com.datastax.driver.core.Session
import org.mockito.Mockito._
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Matchers, WordSpec}
import org.slf4j.LoggerFactory

import java.util

import scala.collection.JavaConverters._
import scala.concurrent.duration._

/**
  *
  * @author Anton Volokhov @literal{<logab@yandex-team.ru <mailto:logab@yandex-team.ru>>}
  */
class CassandraCacheIntSpec
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

  override def beforeEach(): Unit = {
    session.execute(s"drop table if exists ${TestDeserializer.tableName}")
  }
  override def afterEach(): Unit = {
    session.execute(s"drop table if exists ${TestDeserializer.tableName}")
  }

  override def afterAll(): Unit = {
    session.close()
    sf.close()
  }

  def getCache(
      expirationTimeout: FiniteDuration): CassandraCache[TestKey, TestValue] = new
          CassandraCache(DummyBuilder, session, TestDeserializer,
            AlwaysCheckFunction, CassandraCache.idleHandler[TestKey],
            expirationTimeout)

  val log = LoggerFactory.getLogger(classOf[CassandraCacheIntSpec])
  val testSet = Set(TestKey("first"), TestKey("second")).asJava
  val anotherTestSet = Set(TestKey("third"), TestKey("fourth")).asJava

  "Cassandra cache" should {
    "build new values" in {
      val cache = getCache(1.second)
      val spied = spy(cache)
      val keyToValue: util.Map[TestKey, TestValue] = spied.get(testSet, -1)
      verify(spied).submit(org.mockito.Matchers.any())(org.mockito.Matchers.anyLong())
      keyToValue.asScala.foreach {
        case (cacheKey, cacheValue) =>
          val expected = DummyBuilder.build(Set(cacheKey).asJava).get(cacheKey)
          val actual = cacheValue
          assert(expected === actual)
      }
    }
    "update timestamps only once" in {
      val cache = getCache(2000.millis)
      val spied = spy(cache)
      val secondSpy = spy(cache)
      cache.get(testSet, -1)
      Thread.sleep(1000)
      val keyToValue: util.Map[TestKey, TestValue] = spied.get(testSet, -1)

      verify(spied).updateTs(org.mockito.Matchers.any())(org.mockito.Matchers.anyLong())
      keyToValue.asScala.foreach {
        case (cacheKey, cacheValue) =>
          val expected = DummyBuilder.build(Set(cacheKey).asJava).get(cacheKey)
          val actual = cacheValue
          assert(expected === actual)
      }
      secondSpy.get(testSet, -1)
      verify(secondSpy, times(0))
          .updateTs(org.mockito.Matchers.any())(org.mockito.Matchers.anyLong())
    }
    "not update timestamps" in {
      val cache = getCache(1.hour)
      val spied = spy(cache)
      cache.get(testSet, -1)
      val keyToValue: util.Map[TestKey, TestValue] = spied.get(testSet, -1)

      verify(spied, times(0)).updateTs(org.mockito.Matchers.any())(org.mockito.Matchers.anyLong())
      keyToValue.asScala.foreach {
        case (cacheKey, cacheValue) =>
          val expected = DummyBuilder.build(Set(cacheKey).asJava).get(cacheKey)
          val actual = cacheValue
          assert(expected === actual)
      }
    }
    "delete obsolete" in {
      val cache = getCache(10.millis)
      val spied = spy(cache)
      cache.get(testSet, -1)
      Thread.sleep(1000)
      val keyToValue: util.Map[TestKey, TestValue] = spied.get(anotherTestSet, -1)

      verify(spied).removeObsolete(org.mockito.Matchers.any())(org.mockito.Matchers.anyLong())
      keyToValue.asScala.foreach {
        case (cacheKey, cacheValue) =>
          val expected = DummyBuilder.build(Set(cacheKey).asJava).get(cacheKey)
          val actual = cacheValue
          assert(expected === actual)
      }
    }

    "multi build" in {
      val cache = getCache(10.second)
      val spied = spy(cache)
      assert(
        cache.get(((1 to 10000).map(i => TestKey(i.toString)).toSet).asJava, -1).size() > 1000)
      assert(
        cache.get(((1 to 10000).map(i => TestKey(i.toString)).toSet).asJava, -1).size() > 1000)
    }

  }
}
