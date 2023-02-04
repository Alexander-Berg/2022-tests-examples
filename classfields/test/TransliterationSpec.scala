package common.text.translit.test

import common.text.translit.src.Transliteration
import zio.test._
import zio.test.Assertion._

object TransliterationSpec extends DefaultRunnableSpec {

  def spec =
    suite("Transliteration")(
      test("translit russian to english") {
        val translit = Transliteration.DefaultRusToEn
        assert(translit("абв"))(equalTo("abv")) &&
        assert(translit("СССР – Это страна победившего социализма"))(
          equalTo("SSSR – Eto strana pobedivshego socializma")
        ) &&
        assert(translit("электроника"))(equalTo("elektronika")) &&
        assert(translit("ёпрстъ"))(equalTo("eprst"))
      }
    )
}
