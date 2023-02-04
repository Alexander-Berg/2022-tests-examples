package ru.yandex.auto.util

import org.scalatest.FlatSpec
import org.scalatest.Matchers
import Matchers._
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class RoundUtilsSpec extends FlatSpec {

  import RoundUtils._

  "displacementCm3ToCapacityInLitres" should "round volume up correctly" in {
    assert(displacementCm3ToCapacityInLitres(51399) == 51.4d)
    assert(displacementCm3ToCapacityInLitres(5139) == 5.1d)
    assert(displacementCm3ToCapacityInLitres(1596) == 1.6d)
    assert(displacementCm3ToCapacityInLitres(1621) == 1.6d)
    assert(displacementCm3ToCapacityInLitres(1600) == 1.6d)
    assert(displacementCm3ToCapacityInLitres(2000) == 2.0d)
    assert(displacementCm3ToCapacityInLitres(1999) == 2.0d)
    assert(displacementCm3ToCapacityInLitres(2010) == 2.0d)
    assert(displacementCm3ToCapacityInLitres(998) == 1d)
    assert(displacementCm3ToCapacityInLitres(499) == 0.5d)
    assert(displacementCm3ToCapacityInLitres(505) == 0.5d)
    assert(displacementCm3ToCapacityInLitres(500) == 0.5d)
    assert(displacementCm3ToCapacityInLitres(50) == 0.1d)
    assert(displacementCm3ToCapacityInLitres(1645) == 1.7d)
    assert(displacementCm3ToCapacityInLitres(1644) == 1.6d)

    assert(displacementCm3ToCapacityInLitresFormatted(1596) == "1.6")
    assert(displacementCm3ToCapacityInLitresFormatted(1621) == "1.6")
    assert(displacementCm3ToCapacityInLitresFormatted(1600) == "1.6")
    assert(displacementCm3ToCapacityInLitresFormatted(2000) == "2.0")
    assert(displacementCm3ToCapacityInLitresFormatted(1999) == "2.0")
    assert(displacementCm3ToCapacityInLitresFormatted(2010) == "2.0")
    assert(displacementCm3ToCapacityInLitresFormatted(499) == "0.5")
    assert(displacementCm3ToCapacityInLitresFormatted(505) == "0.5")
    assert(displacementCm3ToCapacityInLitresFormatted(500) == "0.5")
    assert(displacementCm3ToCapacityInLitresFormatted(50) == "0.1")
    assert(displacementCm3ToCapacityInLitresFormatted(1645) == "1.7")
    assert(displacementCm3ToCapacityInLitresFormatted(1644) == "1.6")
  }

  "equalDisplacementCm3" should "compare correctly" in {
    equalDisplacementCm3(1445, 1490) should be(true)
    equalDisplacementCm3(1600, 1550) should be(true)
    equalDisplacementCm3(1600, 1600) should be(true)
    equalDisplacementCm3(545, 644) should be(true)
    equalDisplacementCm3(1444, 1445) should be(false)
    equalDisplacementCm3(544, 644) should be(false)
    equalDisplacementCm3(545, 645) should be(false)
  }
}
