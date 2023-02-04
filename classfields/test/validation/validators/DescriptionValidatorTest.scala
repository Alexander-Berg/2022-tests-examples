package ru.yandex.vertis.general.gost.logic.test.validation.validators

import ru.yandex.vertis.general.gost.logic.testkit.ValidatorTestkit._
import ru.yandex.vertis.general.gost.logic.validation.Validator
import ru.yandex.vertis.general.gost.logic.validation.validators.DescriptionValidator
import ru.yandex.vertis.general.gost.model.validation.fields.DescriptionOutOfBounds
import zio.ZLayer
import zio.test.Assertion._
import zio.test._

object DescriptionValidatorTest extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("ContactsValidator")(
      validatorTest("Fail when description has more then 10000 symbols")(
        _.copy(description = "a" * (DescriptionValidator.MaxDescriptionLength + 1))
      )(contains(DescriptionOutOfBounds)),
      validatorTest("Work fine with default description")(identity)(isEmpty)
    ).provideCustomLayerShared(ZLayer.succeed[Validator](DescriptionValidator))

}
