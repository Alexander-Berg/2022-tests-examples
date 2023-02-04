package test.scala.ru.yandex.auto.core.model

import org.scalatest.{Matchers, WordSpecLike}
import ru.yandex.auto.core.model.PriceRange

class PriceRangeSpec extends WordSpecLike with Matchers {
  def mean(from: Int, to: Int): Float = new PriceRange(from, to, "").getMean

  "should calculate common mean" in {
    mean(0, 0) shouldEqual (0)
    mean(5, 3) shouldEqual (4)
    mean(-3, -5) shouldEqual (-4)
    mean(10, 100) shouldEqual (55)
  }

  "should be overflow protected" in {
    mean(Int.MaxValue, Int.MaxValue) shouldEqual Int.MaxValue
    mean(0, Int.MaxValue) shouldEqual (Int.MaxValue / 2)
    mean(Int.MinValue, 0) shouldEqual (Int.MinValue / 2)
  }

}
