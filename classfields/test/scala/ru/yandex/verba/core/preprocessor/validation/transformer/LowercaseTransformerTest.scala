package ru.yandex.verba.core.preprocessor.validation.transformer

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import ru.yandex.verba.core.TestUtils._
import ru.yandex.verba.core.attributes._
import ru.yandex.verba.core.model.domain.Languages
import ru.yandex.verba.core.preprocessor.validation.TransformResult
import ru.yandex.verba.core.util.VerbaUtils

/**
  * Author: Evgeny Vanslov (evans@yandex-team.ru)
  * Created: 03.07.14
  */
class LowercaseTransformerTest extends AnyFlatSpec with Matchers with VerbaUtils {

  val attrsValid = Attributes.apply(Map("a" -> Str("abc")))

  val attrsvalid2 = Attributes.apply(
    Map(
      "acronym" -> Aliases(Map(Languages.En -> "1"), Set.empty),
      "generation-name" -> Aliases(Map(Languages.En -> "11"), Set.empty)
    )
  )
  val attrsInvalid2 = Attributes.apply(Map("a" -> Str("ABC")))

  "Lowercase Transformer " should " transform to lowercase" in {
    val validator = new LowercaseTransformer("a")
    val TransformResult(_, Seq(), Seq()) = validator.transform(asTerm(attrsValid))
    val TransformResult(_, Seq(x), Seq()) = validator.transform(asTerm(attrsInvalid2))
    val TransformResult(y, Seq(), Seq()) = new LowercaseTransformer("generation-name").transform(asTerm(attrsvalid2))
  }
}
