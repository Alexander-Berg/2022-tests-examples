package ru.yandex.realty.rent.util

import org.junit.runner.RunWith
import org.scalatest.{Matchers, WordSpecLike}
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.rent.util.Money._

@RunWith(classOf[JUnitRunner])
class MoneySpec extends WordSpecLike with Matchers {

  "Kopecks to rubles function" should {
    "correctly convert kopecks to rounded to rubles kopecks" in {
      assert(roundFloatKopecksToRubles(16570.0f) == 16600) // 165 rubles 70 kopecks should be rounded to 166 rubles
      assert(roundFloatKopecksToRubles(16550.0f) == 16600) // 165 rubles 50 kopecks should be rounded to 166 rubles
      assert(roundFloatKopecksToRubles(16549.0f) == 16500) // 165 rubles 49 kopecks should be rounded to 165 rubles
      assert(roundFloatKopecksToRubles(16540.0f) == 16500) // 165 rubles 40 kopecks should be rounded to 165 rubles

      assert(roundLongKopecksToRubles(16570) == 16600)
      assert(roundLongKopecksToRubles(16550) == 16600)
      assert(roundLongKopecksToRubles(16549) == 16500)
      assert(roundLongKopecksToRubles(16540) == 16500)
    }
  }

  "printKopecksAsFractionalRubles" should {
    "not add fractional part for whole ruble amount" in {
      printKopecksAsFractionalRubles(100) shouldBe "1"
    }

    "add fractional part for non-whole ruble amount" in {
      printKopecksAsFractionalRubles(102) shouldBe "1.02"
    }
  }
}
