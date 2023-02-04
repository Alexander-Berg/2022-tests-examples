package ru.yandex.vertis.general.gost.model.test.attributes.validation

import general.bonsai.restriction_model.NumberRestriction.NumberRestrictionValue
import general.bonsai.restriction_model.Restriction.TypeRestriction
import general.bonsai.restriction_model.{NumberRange, NumberRestriction, Restriction => ApiRestriction}
import ru.yandex.vertis.general.gost.model.attributes.Attribute
import ru.yandex.vertis.general.gost.model.attributes.AttributeValue.NumberValue
import ru.yandex.vertis.general.gost.model.validation.attributes.Restriction
import zio.test.Assertion.isNone
import zio.test._

object NumberRestrictionSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("NumberRestriction")(
      testM("validate number by decimals count") {
        val restriction = ApiRestriction(
          "attr",
          TypeRestriction.NumberRestriction(
            NumberRestriction(
              NumberRestrictionValue.FractionalDigitsMustFit(
                NumberRange(0, 0, minInclusive = true, maxInclusive = true)
              )
            )
          )
        )

        for {
          compiled <- Restriction.compile(restriction)
          check <- compiled.check(Attribute("attr", 0, NumberValue(150)))
        } yield assert(check)(isNone)
      }
    )
}
