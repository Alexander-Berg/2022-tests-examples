package ru.auto.api.util

import ru.auto.api.BaseSpec

class IntOpsSpec extends BaseSpec {

  import java.math.RoundingMode
  import ru.auto.api.util.IntOps._

  "round" should {
    "round up" in {
      55.roundTo(accuracy = 10, roundingMode = RoundingMode.UP) shouldBe 60
      55.roundTo(accuracy = 5, roundingMode = RoundingMode.UP) shouldBe 55
      549000.roundTo(accuracy = 10000, roundingMode = RoundingMode.UP) shouldBe 550000
      339.roundTo(accuracy = 1, roundingMode = RoundingMode.UP) shouldBe 339
    }
    "round down" in {
      55.roundTo(accuracy = 10, roundingMode = RoundingMode.DOWN) shouldBe 50
      55.roundTo(accuracy = 5, roundingMode = RoundingMode.DOWN) shouldBe 55
      549000.roundTo(accuracy = 10000, roundingMode = RoundingMode.DOWN) shouldBe 540000
      339.roundTo(accuracy = 1, roundingMode = RoundingMode.DOWN) shouldBe 339
      339.roundTo(accuracy = 1, roundingMode = RoundingMode.DOWN) shouldBe 339
      9.roundTo(accuracy = 10, roundingMode = RoundingMode.DOWN) shouldBe 0
    }
    "require correct args" in {
      (the[IllegalArgumentException] thrownBy
        -1.roundTo(accuracy = 10, roundingMode = RoundingMode.DOWN) should have)
        .message("requirement failed: implemented for positive numbers only")

      (the[IllegalArgumentException] thrownBy
        1.roundTo(accuracy = -1, roundingMode = RoundingMode.DOWN) should have)
        .message("requirement failed: accuracy must be positive")

      (the[NotImplementedError] thrownBy
        9.roundTo(accuracy = 10, roundingMode = RoundingMode.HALF_EVEN) should have)
        .message("allowed rounding modes are UP and DOWN only")
    }
  }
}
