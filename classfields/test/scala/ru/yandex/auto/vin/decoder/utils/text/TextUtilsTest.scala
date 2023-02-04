package ru.yandex.auto.vin.decoder.utils.text

import org.scalatest.wordspec.AnyWordSpecLike

class TextUtilsTest extends AnyWordSpecLike {

  "testCapitalizeIgnoringASCII" should {
    "Not fail on empty string" in {
      assert(TextUtils.capitalizeIgnoringASCII("") == "")
    }
    "Not fail on one element string" in {
      assert(TextUtils.capitalizeIgnoringASCII("и") == "И")
    }
    "Remain string with only ascii chars unchanged" in {
      val asciiStr = "do YOU Want to know how I got these SCARS?"
      assert(TextUtils.capitalizeIgnoringASCII(asciiStr) == asciiStr)
    }
    "Successfully convert Unicode/Ascii String" in {
      val mixedStr = "я помню Чудное МГНОВЕНИЕ"
      assert(TextUtils.capitalizeIgnoringASCII(mixedStr) == "Я помню чудное мгновение")
    }
    "Remain symbols in string untouched" in {
      val symbolStr = """а!\\\"#$%&\\'()*+,-.Б"""
      assert(TextUtils.capitalizeIgnoringASCII(symbolStr) == """А!\\\"#$%&\\'()*+,-.б""")
    }
  }

}
