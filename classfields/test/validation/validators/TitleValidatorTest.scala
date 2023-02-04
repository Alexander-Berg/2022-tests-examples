package ru.yandex.vertis.general.gost.logic.test.validation.validators

import zio.test._
import zio.test.Assertion._
import ru.yandex.vertis.general.gost.logic.testkit.ValidatorTestkit.validatorTest
import ru.yandex.vertis.general.gost.logic.validation.Validator
import ru.yandex.vertis.general.gost.logic.validation.validators.TitleValidator
import ru.yandex.vertis.general.gost.model.validation.fields.{TitleOverBounds, TitleRequired}
import zio.ZLayer

object TitleValidatorTest extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("TitleValidator")(
      validatorTest("Fail with empty title")(
        _.copy(title = "")
      )(contains(TitleRequired)),
      validatorTest("Fail with title over bounds")(
        _.copy(title = "t")
      )(contains(TitleOverBounds)),
      validatorTest("Work fine with valid entity")(identity)(isEmpty)
    ).provideCustomLayerShared(ZLayer.succeed[Validator](TitleValidator))
}
