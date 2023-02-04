package ru.yandex.verba.core.preprocessor.validation.validator

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import ru.yandex.verba.core.TestUtils
import ru.yandex.verba.core.attributes.{Attributes, Str}
import ru.yandex.verba.core.util.VerbaUtils

/**
  * Author: Evgeny Vanslov (evans@yandex-team.ru)
  * Created: 25.06.14
  */
class IntValidatorTest extends AnyFlatSpec with Matchers with VerbaUtils {

  val attrsValid = Attributes.apply(Map("a" -> Str("123")))
  val attrsInvalid = Attributes.apply(Map("a" -> Str("123d")))

  "IntValidator " should " check is attr of type int" in {
    val validator = new IntValidator("a")
    val Seq() = validator.validate(TestUtils.asTerm(attrsValid))
    val Seq(x) = validator.validate(TestUtils.asTerm(attrsInvalid))
  }
}
