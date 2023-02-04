package ru.auto.catalog.core.util.collections

import ru.auto.catalog.BaseSpec
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import common.collections.syntax._

class SyntaxSpec extends BaseSpec with ScalaCheckDrivenPropertyChecks {

  "RichRelation.inverse" should {
    "contain the same pairs as the original" in forAll { m: Map[Int, Set[Int]] =>
      val originalPairs: Set[(Int, Int)] = m.toSeq.flatMap { case (k, vs) => vs.toSeq.map((k, _)) }.toSet
      val inversePairs: Set[(Int, Int)] = m.inverse.toSeq.flatMap { case (v, ks) => ks.toSeq.map((_, v)) }.toSet
      inversePairs shouldBe originalPairs
    }
    "be an inverse of itself for inputs where sets are nonempty" in forAll { m: Map[Int, Set[Int]] =>
      val m0 = m.filter(_._2.nonEmpty)
      m0.inverse.inverse shouldBe m0
    }
  }
}
