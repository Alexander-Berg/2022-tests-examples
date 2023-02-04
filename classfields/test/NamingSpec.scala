package ru.yandex.vertis.general.bonsai.model.test

import ru.yandex.vertis.general.bonsai.model.Naming
import zio.test._
import zio.test.Assertion._

object NamingSpec extends DefaultRunnableSpec {

  def spec =
    suite("Naming")(
      test("translit names for uriPart") {
        assert(Naming.uriPart("Большой Лебовски"))(equalTo("bolshoy-lebovski"))
      },
      test("remove non letters") {
        assert(Naming.uriPart("#Ударная установка"))(equalTo("udarnaya-ustanovka"))
      }
    )
}
