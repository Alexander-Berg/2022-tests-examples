package ru.yandex.vertis.general.gost.logic.test.validation.validators

import ru.yandex.vertis.general.common.model.delivery.{DeliveryInfo, SelfDelivery}
import ru.yandex.vertis.general.gost.logic.testkit.ValidatorTestkit.{odezhdaCategory, rabotaCategory, validatorTest}
import ru.yandex.vertis.general.gost.logic.validation.Validator
import ru.yandex.vertis.general.gost.logic.validation.validators.DeliveryValidator
import ru.yandex.vertis.general.gost.model.validation.fields.DeliveryNotSuitable
import zio.ZLayer
import zio.test.Assertion.{contains, isEmpty}
import zio.test.{DefaultRunnableSpec, ZSpec}

object DeliveryValidatorTest extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite("DeliveryValidator")(
      validatorTest("Все ок если доставка в категории одежды", odezhdaCategory)(
        _.copy(delivery =
          Some(DeliveryInfo(selfDelivery = Some(SelfDelivery(sendByCourier = true, sendWithinRussia = true))))
        )
      )(isEmpty),
      validatorTest("В категории Работы доставки быть не должно", rabotaCategory)(
        _.copy(delivery =
          Some(DeliveryInfo(selfDelivery = Some(SelfDelivery(sendByCourier = true, sendWithinRussia = true))))
        )
      )(contains(DeliveryNotSuitable))
    ).provideCustomLayerShared(ZLayer.succeed[Validator](DeliveryValidator))
  }
}
