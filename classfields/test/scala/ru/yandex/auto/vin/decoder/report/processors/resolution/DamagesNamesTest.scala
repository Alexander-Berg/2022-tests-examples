package ru.yandex.auto.vin.decoder.report.processors.resolution

import org.scalatest.funsuite.AnyFunSuite

class DamagesNamesTest extends AnyFunSuite {

  test("duplicate damages") {
    val damages = DamagesNames.getJavaDamages(Seq(7, 8))
    assert(damages.size == 3)
  }
}
