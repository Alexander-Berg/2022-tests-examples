package ru.yandex.vertis.subscriptions.storage

import org.scalacheck.Gen
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfter, Matchers, WordSpecLike}
import ru.yandex.vertis.subscriptions.util.test.CoreGenerators.emailSubscriptionGen
import ru.yandex.vertis.subscriptions.{Model, SlowAsyncSpec}

/**
  * Specs on [[ActiveSubscriptionsDao]]
  */
trait ActiveSubscriptionsDaoSpec
  extends Matchers
  with WordSpecLike
  with BeforeAndAfter
  with ScalaFutures
  with SlowAsyncSpec {

  val activeSubscriptionGen: Gen[ActiveSubscription] = for {
    subscription <- emailSubscriptionGen
    summary <- Generators.documentSummaryGen
    key = Notification.Key(subscription, Model.Delivery.Type.EMAIL)
  } yield ActiveSubscription(key, summary, System.currentTimeMillis())

  def dao: ActiveSubscriptionsDao

  def cleanData()

  before {
    cleanData()
  }

  def activeSubscriptions: Iterator[ActiveSubscription] =
    Iterator.continually(activeSubscriptionGen.sample).flatten

  def nextActiveSubscription =
    activeSubscriptions.take(1).toIterable.head

  def nextActiveSubscriptions(n: Int) =
    activeSubscriptions.take(n).toIndexedSeq

  "ActiveSubscriptionsDao" should {
    "get by token" in {
      val initial = nextActiveSubscriptions(5)
      dao.put(initial).futureValue
      val tokens = initial.map(dao.token).distinct

      val restored = for {
        token <- tokens
        as <- dao.withToken(token).futureValue.toArray
      } yield as

      restored.size shouldBe initial.size
      restored should contain theSameElementsAs initial
    }

    "put and get" in {
      val as = nextActiveSubscription
      dao.put(as).futureValue
      dao.get(as.key).futureValue should be(Some(as))
    }

    "get by non-existent token" in {
      dao.withToken("100500").futureValue shouldBe empty
    }

    "delete created ActiveSubscription" in {
      val as = nextActiveSubscription
      dao.put(as).futureValue
      dao.get(as.key).futureValue should be(Some(as))

      dao.delete(as.key).futureValue
      dao.get(as.key).futureValue should be(None)
    }
  }
}
