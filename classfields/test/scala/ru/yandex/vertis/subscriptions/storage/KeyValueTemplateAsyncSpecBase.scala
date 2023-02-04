package ru.yandex.vertis.subscriptions.storage

import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, Matchers, WordSpecLike}
import org.scalatest.concurrent.ScalaFutures
import ru.yandex.vertis.subscriptions.storage

import scala.concurrent.ExecutionContext
import scala.util.Random

trait KeyValueTemplateAsyncSpecBase
  extends KeyValueTemplateConverters
  with Matchers
  with WordSpecLike
  with ScalaFutures
  with BeforeAndAfter
  with BeforeAndAfterAll {

  def keyValue[T: Format](): storage.KeyValueTemplateAsync[T]

  protected lazy val firstDao = keyValue[Long]()

  def cleanDatabase(): Unit

  implicit val ec = ExecutionContext.global

  protected val instanceId =
    Some("_" + Math.abs(Random.nextLong()).toString)

  "KeyValue" should {

    "put value by full key" in {
      val key = List("1", "2", "3")
      firstDao.put(key, 20).futureValue
      firstDao.get(key).futureValue should be(List(20))
      firstDao.getSingle(key).futureValue should be(Some(20))
    }

    "not put by partial key" in {
      firstDao.put(List("1", "2"), 20).failed.futureValue shouldBe an[IllegalArgumentException]
    }

    "replace value by key" in {
      val key = List("1", "2", "3")
      firstDao.put(key, 20).futureValue
      firstDao.get(key).futureValue should be(List(20))

      firstDao.put(key, 30).futureValue
      firstDao.get(key).futureValue should be(List(30))
    }

    "remove value by full key" in {
      val key = List("1", "2", "3")
      firstDao.put(key, 20).futureValue
      firstDao.get(key).futureValue should be(List(20))

      firstDao.delete(key).futureValue
      firstDao.getSingle(key).futureValue should be(None)
    }

    "perform batch put" in {
      val keyPart = "2" :: "1" :: Nil
      val expected = 1L to 100L
      val batch = (for {
        i <- expected
        key = (i.toString :: keyPart).reverse
        value = i
      } yield key -> value).toMap

      firstDao.put(batch).futureValue
      firstDao.get(List("1")).futureValue should contain theSameElementsAs expected
    }
  }

  before {
    cleanDatabase()
  }

}
