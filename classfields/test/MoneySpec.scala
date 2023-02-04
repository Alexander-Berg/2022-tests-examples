package common.models.finance.test

import common.models.finance.Money.{Kopecks, Rubles}
import zio.test._

object MoneySpec extends DefaultRunnableSpec {

  def spec = suite("Money")(
    test("Rubles should converts to Kopecks") {
      val rubles = Rubles(99L)
      assertTrue(rubles.asKopecks == Kopecks(9900L))
    },
    test("Rubles should be additive") {
      val sum = Rubles(22L) + Rubles(33L)
      assertTrue(sum == Rubles(55L))
    },
    test("Kopecks should be additive") {
      val sum = Kopecks(22L) + Kopecks(33L)
      assertTrue(sum == Kopecks(55L))
    }
  )
}
