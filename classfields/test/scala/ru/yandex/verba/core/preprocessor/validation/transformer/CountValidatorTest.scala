package ru.yandex.verba.core.preprocessor.validation.transformer

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import ru.yandex.verba.core.TestUtils.asTerm
import ru.yandex.verba.core.attributes.{Attributes, Str, Strings}
import ru.yandex.verba.core.preprocessor.validation.TransformResult
import ru.yandex.verba.core.preprocessor.validation.validator.CountValidator
import ru.yandex.verba.core.util.VerbaUtils

/**
  * Author: Evgeny Vanslov (evans@yandex-team.ru)
  * Created: 21.07.14
  */
class CountValidatorTest extends AnyFlatSpec with Matchers with VerbaUtils {

  val attrsValid = Attributes.apply(Map("a" -> Str("abc")))
  val attrsInvalid = Attributes.apply(Map("a" -> Strings(Seq("abc", "ade"))))

  "Count validator " should " validate count" in {
    val validator = new CountValidator("a", 1)
    val TransformResult(_, Seq(), Seq()) = validator.transform(asTerm(attrsValid))
    val TransformResult(_, Seq(), Seq(x)) = validator.transform(asTerm(attrsInvalid))
  }
}
