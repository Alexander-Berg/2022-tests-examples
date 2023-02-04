package ru.yandex.vertis.subscriptions.storage

import org.scalacheck.Gen
import org.scalatest.{BeforeAndAfter, Matchers, WordSpecLike}
import ru.yandex.vertis.subscriptions.Model
import ru.yandex.vertis.subscriptions.Model.Delivery
import ru.yandex.vertis.subscriptions.backend.LatestDocument
import ru.yandex.vertis.subscriptions.util.test.CoreGenerators.idGen

import scala.util.Success

/**
  * Specs on [[ActiveSubscriptionsDao]]
  */
trait LatestDocumentDaoSpec extends Matchers with WordSpecLike with BeforeAndAfter {

  val latestDocumentGen: Gen[LatestDocument] = for {
    id <- idGen
    delivery <- Gen.oneOf(Model.Delivery.Type.values())
    key = Notification.KeyLite(id, delivery)
    now = System.currentTimeMillis()
    timestamp <- Gen.choose(now - 100000, now - 1000)
  } yield LatestDocument(key, timestamp)

  def dao: LatestDocumentDao

  def cleanData()

  before {
    cleanData()
  }

  def latestDocuments: Iterator[LatestDocument] =
    Iterator.continually(latestDocumentGen.sample).flatten

  def nextLatestDocument =
    latestDocuments.take(4).toIterable.head

  def nextLatestDocuments(n: Int) =
    latestDocuments.take(n).toIterable

  "LatestDocumentsDao" should {
    "put and get" in {
      val ld = nextLatestDocument
      dao.put(ld).get
      dao.get(ld.key) should be(Success(Some(ld)))
    }

    "get by token" in {
      val initial = nextLatestDocuments(50)
      for (ld <- initial)
        dao.put(ld)
      val tokens = initial.map(dao.token)

      val restored = for {
        token <- tokens
        ld <- dao.withToken(token).get
      } yield ld

      restored.toSet should be(initial.toSet)
    }

    "get by subscription ID" in {
      val ld1 = LatestDocument(Notification.KeyLite("foo", Delivery.Type.EMAIL), System.currentTimeMillis())
      val ld2 = LatestDocument(Notification.KeyLite("foo", Delivery.Type.SMS), System.currentTimeMillis())

      dao.put(ld1).get
      dao.getAll("foo").map(_.toSet) should be(Success(Set(ld1)))

      dao.put(ld2).get
      dao.getAll("foo").map(_.toSet) should be(Success(Set(ld1, ld2)))
    }
  }
}
