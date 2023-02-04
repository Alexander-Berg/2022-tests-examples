package ru.yandex.vertis.subscriptions.storage.cassandra

import org.junit.runner.RunWith
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.junit.JUnitRunner
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, Matchers, WordSpecLike}
import ru.yandex.vertis.subscriptions.storage.KeyValueTemplateConverters

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util.{Failure, Random, Success}

/**
  * Specs on [[ru.yandex.vertis.subscriptions.storage.cassandra.KeyValueTemplate]]
  */
@RunWith(classOf[JUnitRunner])
class KeyValueTemplateIntSpec
  extends KeyValueTemplateConverters
  with Matchers
  with WordSpecLike
  with ScalaFutures
  with BeforeAndAfter
  with BeforeAndAfterAll
  with TestCassandra {

  override def spanScaleFactor: Double = 5

  // for prevent writes and reads from same table at concurrent test runs
  val instanceId = Some("_" + Math.abs(Random.nextLong()).toString)

  val keyValue = new KeyValueTemplate[Long](testSession, "key_value", 3, true, Duration.Inf, 1.day, instanceId)

  val keyValue2 = new KeyValueTemplate[String](testSession, "key_value", 3, true, Duration.Inf, 1.day, instanceId)

  "KeyValue" should {

    "put value by full key" in {
      val key = List("1", "2", "3")
      keyValue.put(key, 20).get
      keyValue.get(key) should be(Success(List(Success(20))))
      keyValue.getSingle(key) should be(Success(Some(Success(20))))
    }

    "not put by partial key" in {
      keyValue.put(List("1", "2"), 20) match {
        case Failure(e: IllegalArgumentException) =>
        case other => fail("Expect IllegalArgument exception, but got " + other)
      }
    }

    "replace value by key" in {
      val key = List("1", "2", "3")
      keyValue.put(key, 20).get
      keyValue.get(key) should be(Success(List(Success(20))))

      keyValue.put(key, 30).get
      keyValue.get(key) should be(Success(List(Success(30))))
    }

    "remove value by full key" in {
      val key = List("1", "2", "3")
      keyValue.put(key, 20).get
      keyValue.get(key) should be(Success(List(Success(20))))

      keyValue.delete(key).get
      keyValue.getSingle(key) should be(Success(None))
    }

    "indicates beaten value" in {
      val key = List("1", "2", "3")
      keyValue2.put(key, "abc").get

      keyValue.get(key) match {
        case Success(List(Failure(_))) =>
        case x => fail(s"Expected beaten value, but got $x")
      }
    }

    "perform batch put" in {
      val keyPart = "2" :: "1" :: Nil
      val batch = (for {
        i <- 1L to 100L
        key = (i.toString :: keyPart).reverse
        value = i
      } yield key -> value).toMap

      keyValue.put(batch).futureValue
      val expected = (1L to 100L).map(x => Success(x)).toSet
      keyValue.get(List("1")).map(_.toSet) should be(Success(expected))
    }
  }

  before {
    testSession.execute(s"TRUNCATE ${keyValue.table}")
  }
}
