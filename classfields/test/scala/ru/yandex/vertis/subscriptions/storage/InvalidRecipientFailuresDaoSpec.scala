package ru.yandex.vertis.subscriptions.storage

import ru.yandex.vertis.subscriptions.{SlowAsyncSpec, SpecBase}

/**
  * Tests for [[InvalidRecipientFailuresDao]]
  *
  * @author zvez
  */
trait InvalidRecipientFailuresDaoSpec extends SpecBase with SlowAsyncSpec {

  import scala.concurrent.ExecutionContext.Implicits.global

  def dao: InvalidRecipientFailuresDao

  "DeliveryFailuresDao" should {
    "return nothing when there is no failures" in {
      dao.all().map(_.toList).futureValue should be(Nil)
    }

    "create row with first failure" in {
      dao.increaseFailed("1").futureValue
      dao.all().map(_.toList).futureValue should be(Seq("1" -> 1))
    }

    "increment failure count" in {
      dao.increaseFailed("1").futureValue
      dao.all().map(_.toList).futureValue should be(Seq("1" -> 2))
    }

    "work with multiple subscriptions" in {
      dao.increaseFailed("2").futureValue
      dao.all().map(_.toMap).futureValue should be(Map("1" -> 2, "2" -> 1))
    }

    "remove failure info" in {
      dao.remove("1").futureValue
      dao.all().map(_.toList).futureValue should be(Seq("2" -> 1))
    }
  }

}
