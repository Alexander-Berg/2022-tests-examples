package ru.yandex.realty.util.iterators

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.SpecBase
import ru.yandex.realty.logging.Logging

/**
  * Created by Viacheslav Kukushkin <vykukushkin@yandex-team.ru> on 30.11.18
  */

@RunWith(classOf[JUnitRunner])
class TwoIteratorsSpec extends SpecBase with Logging {

  "TwoPointersIterator" should {
    "work correctly" in {
      val seq1 = Seq(1, 3, 5, 7, 9, 11, 13).map(_.toString)
      val seq2 = Seq(3, 6, 9, 12, 15).map(_.toString)

      val finalResult =
        Seq((1, -1), (3, 3), (5, -1), (-1, 6), (7, -1), (9, 9), (11, -1), (-1, 12), (13, -1), (-1, 15)).map {
          case (v1, v2) => (if (v1 > 0) Some(v1.toString) else None, if (v2 > 0) Some(v2.toString) else None)
        }.toList

      val realResult = new TwoPointersIterator[String, Long](seq1.iterator, seq2.iterator, v => v.toLong).toList
      val realResultReverted = new TwoPointersIterator[String, Long](seq2.iterator, seq1.iterator, v => v.toLong).toList
      val finalResultReverted = finalResult.map { case (v1, v2) => (v2, v1) }

      realResult shouldEqual finalResult

      realResultReverted shouldEqual finalResultReverted
    }

    "work with empty iterators" in {
      val seq1 = Seq(1, 3, 5, 7, 9, 11, 13).map(_.toString)
      val seq2 = Seq.empty[String]

      val finalResult = seq1.map { v =>
        (Some(v), None)
      }.toList

      val realResult = new TwoPointersIterator[String, Long](seq1.iterator, seq2.iterator, v => v.toLong).toList
      val realResultReverted = new TwoPointersIterator[String, Long](seq2.iterator, seq1.iterator, v => v.toLong).toList
      val finalResultReverted = finalResult.map { case (v1, v2) => (v2, v1) }

      realResult shouldEqual finalResult

      realResultReverted shouldEqual finalResultReverted

    }
  }

}
