package ru.yandex.vertis.subscriptions.backend.summarization

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpecLike}
import ru.yandex.vertis.subscriptions.storage.summarization.Summary

import scala.util.Random

/** Specs on [[Summary]]
  */
@RunWith(classOf[JUnitRunner])
class SummarySpec extends Matchers with WordSpecLike {

  "CollectedData" should {
    "preserve top N elements" in {
      var collected = Summary.emptySinceNow[Int](3)

      val shuffled = Random.shuffle((1 to 100).toList) ++
        Random.shuffle((1 to 100).toList)

      for (value <- shuffled)
        collected = collected.updated(value)
      collected.lastN.toSeq should be(Seq(100, 99, 98))
      collected should not be (empty)
    }
  }
}
