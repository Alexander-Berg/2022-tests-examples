package ru.yandex.auto.util

import org.scalatest.FlatSpec

class RoundUtilsSpec extends FlatSpec {

  import RoundUtils._

  "displacementCm3ToCapacityInLitres" should "round volume up correctly" in {
    assert(displacementCm3ToCapacityInLitres(1596) == 1.6f)
    assert(displacementCm3ToCapacityInLitres(1621) == 1.6f)
    assert(displacementCm3ToCapacityInLitres(1600) == 1.6f)
    assert(displacementCm3ToCapacityInLitres(2000) == 2.0f)
    assert(displacementCm3ToCapacityInLitres(1999) == 2.0f)
    assert(displacementCm3ToCapacityInLitres(2010) == 2.0f)
    assert(displacementCm3ToCapacityInLitres(499) == 0.5f)
    assert(displacementCm3ToCapacityInLitres(505) == 0.5f)
    assert(displacementCm3ToCapacityInLitres(500) == 0.5f)
    assert(displacementCm3ToCapacityInLitres(50) == 0.1f)

    assert(displacementCm3ToCapacityInLitres(1596).toString == "1.6")
    assert(displacementCm3ToCapacityInLitres(1621).toString == "1.6")
    assert(displacementCm3ToCapacityInLitres(1600).toString == "1.6")
    assert(displacementCm3ToCapacityInLitres(2000).toString == "2.0")
    assert(displacementCm3ToCapacityInLitres(1999).toString == "2.0")
    assert(displacementCm3ToCapacityInLitres(2010).toString == "2.0")
    assert(displacementCm3ToCapacityInLitres(499).toString == "0.5")
    assert(displacementCm3ToCapacityInLitres(505).toString == "0.5")
    assert(displacementCm3ToCapacityInLitres(500).toString == "0.5")
    assert(displacementCm3ToCapacityInLitres(50).toString == "0.1")
  }
}
