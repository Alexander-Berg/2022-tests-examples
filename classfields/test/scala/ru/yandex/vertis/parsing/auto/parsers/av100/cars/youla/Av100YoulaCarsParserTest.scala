package ru.yandex.vertis.parsing.auto.parsers.av100.cars.youla

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

/**
  * TODO
  *
  * @author aborunov
  */
//scalastyle:off line.size.limit
@RunWith(classOf[JUnitRunner])
class Av100YoulaCarsParserTest extends FunSuite {
  test("offerId") {
    assert(
      Av100YoulaCarsParser.offerId(
        "https://youla.io/moskva/avto-moto/avtomobili/khiendai-matriks-58ef7903132ca5942434a247"
      ) == "58ef7903132ca5942434a247"
    )
    assert(
      Av100YoulaCarsParser
        .offerId("https://youla.io/chernoe/avto-moto/avtomobili/audi-80-58fb0a0cd67750926d22af42") == "58fb0a0cd67750926d22af42"
    )
  }

  test("parseAddressFromUrl") {
    assert(
      Av100YoulaCarsParser
        .parseAddressFromUrl("https://youla.io/moskva/avto-moto/avtomobili/khiendai-matriks-58ef7903132ca5942434a247")
        .contains("MOSKVA")
    )
    assert(
      Av100YoulaCarsParser
        .parseAddressFromUrl("https://youla.io/chernoe/avto-moto/avtomobili/audi-80-58fb0a0cd67750926d22af42")
        .contains("CHERNOE")
    )
  }
}
