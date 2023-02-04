package ru.yandex.vertis.general.category_matcher.model.testkit

import ru.yandex.vertis.general.category_matcher.model.CategoryId
import zio.test.Gen

object CategoryIdGen {
  val any = Gen.alphaNumericStringBounded(10, 20).map(CategoryId.apply)
}
