package ru.yandex.vertis.passport.util

import org.scalatest.{FreeSpec, Inspectors, Matchers, OptionValues}

class HideEntityUtilsSpec extends FreeSpec with Matchers with Inspectors with OptionValues {

  "mangleString" - {

    def doMangle(input: String, percent: Double, output: String, symbol: Char = '*') = {
      s"$input -> ($percent, $symbol) -> $output" in {
        import ru.yandex.vertis.passport.util.secured.SecuredEntityUtil._
        val res = mangleString(input, percent, symbol)
        res shouldBe output
      }
    }

    doMangle("123", 0.0, "1*3")
    doMangle("123", 1.0, "1*3")
    doMangle("1234", 0.5, "1**4")
    doMangle("12345", 0.5, "1***5")
    doMangle("123456", 0.5, "1***56")
    doMangle("1234567890", 0.0, "1234*67890")
    doMangle("1234567890", 0.1, "1234*67890")
    doMangle("1234567890", 0.2, "1234**7890")
    doMangle("1234567890", 0.3, "123***7890")
    doMangle("1234567890", 0.4, "123****890")
    doMangle("1234567890", 0.5, "12*****890")
    doMangle("1234567890", 0.6, "12******90")
    doMangle("1234567890", 0.7, "1*******90")
    doMangle("1234567890", 0.8, "1********0")
    doMangle("1234567890", 0.9, "1********0")
    doMangle("1234567890", 1.0, "1********0")
    doMangle("1234567890", 1.0, "1--------0", '-')
  }
}
