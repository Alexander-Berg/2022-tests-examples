package ru.yandex.verba.core.preprocessor.validation.validator

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import ru.yandex.verba.core.TestUtils.asTerm
import ru.yandex.verba.core.attributes._
import ru.yandex.verba.core.model.domain.Languages
import ru.yandex.verba.core.util.VerbaUtils

/**
  * Author: Evgeny Vanslov (evans@yandex-team.ru)
  * Created: 25.06.14
  */
class InseparablesValidatorTest extends AnyFlatSpec with Matchers with VerbaUtils {

  val attrsValid = Attributes.apply(Map("a" -> Empty, "b" -> Empty))

  val attrsValid2 = Attributes.apply(
    Map("a" -> Aliases(Map(Languages.En -> "1"), Set.empty), "b" -> Aliases(Map(Languages.En -> "1"), Set.empty))
  )

  val attrsValid3 = Attributes.apply(
    Map("a" -> Aliases(Map(Languages.En -> "11"), Set.empty), "b" -> Aliases(Map(Languages.En -> "1"), Set.empty))
  )
  val attrsInvalid = Attributes.apply(Map("a" -> Empty, "b" -> Str("d")))
  val attrsInvalid2 = Attributes.apply(Map("a" -> Aliases(Map.empty, Set.empty), "b" -> Str("d")))
  val attrsInvalid3 = Attributes.apply(Map("a" -> Aliases(Map.empty, Set(Alias("1", Set.empty, 0))), "b" -> Str("d")))

  "Inseparables Validator " should " check existance of all group of attrs'" in {
    val validator = new InseparablesValidator(Set("a", "b"))
    val Seq() = validator.validate(asTerm(attrsValid))
    val Seq() = validator.validate(asTerm(attrsValid2))
    val Seq() = validator.validate(asTerm(attrsValid3))
    val Seq(x) = validator.validate(asTerm(attrsInvalid))
    val Seq(y) = validator.validate(asTerm(attrsInvalid2))
    val Seq(z) = validator.validate(asTerm(attrsInvalid3))
  }
}
