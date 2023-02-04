package ru.yandex.auto.traffic.model

import org.scalatest.{FlatSpec, Matchers}

class EngineSpec extends FlatSpec with Matchers {

  it should "correctly convert liters to cm3" in {
    val cases = List(
      (Engine.Cm3(50), Engine.L(0.1), Engine.Cm3(100)),
      (Engine.Cm3(499), Engine.L(0.5), Engine.Cm3(500)),
      (Engine.Cm3(500), Engine.L(0.5), Engine.Cm3(500)),
      (Engine.Cm3(505), Engine.L(0.5), Engine.Cm3(500)),
      (Engine.Cm3(998), Engine.L(1.0), Engine.Cm3(1000)),
      (Engine.Cm3(1596), Engine.L(1.6), Engine.Cm3(1600)),
      (Engine.Cm3(1600), Engine.L(1.6), Engine.Cm3(1600)),
      (Engine.Cm3(1621), Engine.L(1.6), Engine.Cm3(1600)),
      (Engine.Cm3(1644), Engine.L(1.6), Engine.Cm3(1600)),
      (Engine.Cm3(1645), Engine.L(1.7), Engine.Cm3(1700)),
      (Engine.Cm3(1999), Engine.L(2.0), Engine.Cm3(2000)),
      (Engine.Cm3(2000), Engine.L(2.0), Engine.Cm3(2000)),
      (Engine.Cm3(2143), Engine.L(2.1), Engine.Cm3(2100)),
      (Engine.Cm3(2163), Engine.L(2.2), Engine.Cm3(2200)),
      (Engine.Cm3(2250), Engine.L(2.3), Engine.Cm3(2300)),
      (Engine.Cm3(5139), Engine.L(5.1), Engine.Cm3(5100)),
      (Engine.Cm3(51399), Engine.L(51.4), Engine.Cm3(51400))
    )

    cases.foreach {
      case (cm3, cm3ToL, lToCm3) =>
        cm3.liters should be(cm3ToL)
        cm3ToL.cm3 should be(lToCm3)
    }
  }
}
