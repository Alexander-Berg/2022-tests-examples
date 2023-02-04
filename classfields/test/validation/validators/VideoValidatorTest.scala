package ru.yandex.vertis.general.gost.logic.test.validation.validators

import zio.test._
import zio.test.Assertion._
import ru.yandex.vertis.general.gost.logic.testkit.ValidatorTestkit.validatorTest
import ru.yandex.vertis.general.gost.logic.validation.Validator
import ru.yandex.vertis.general.gost.logic.validation.validators.VideoValidator
import ru.yandex.vertis.general.gost.model.Offer.Video
import ru.yandex.vertis.general.gost.model.validation.fields.InvalidVideoSource
import zio.ZLayer

object VideoValidatorTest extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("VideoValidator")(
      validatorTest("Work fine with empty video url")(
        _.copy(video = Some(Video(url = "")))
      )(isEmpty),
      validatorTest("Fail when video is not from youtube")(
        _.copy(video = Some(Video(url = "http://verybadsite.com/why_you_cant_live_without_tagless_final")))
      )(contains(InvalidVideoSource)),
      validatorTest("Work fine with valid entity")(identity)(isEmpty)
    ).provideCustomLayerShared(ZLayer.succeed[Validator](VideoValidator))
}
