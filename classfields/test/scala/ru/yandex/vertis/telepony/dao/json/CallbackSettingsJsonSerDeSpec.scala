package ru.yandex.vertis.telepony.dao.json

import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.yandex.vertis.telepony.SpecBase
import ru.yandex.vertis.telepony.model.CallbackGenerator._

/**
  * @author neron
  */
class CallbackSettingsJsonSerDeSpec extends SpecBase with ScalaCheckPropertyChecks {

  implicit private val generatorConfig: PropertyCheckConfiguration = PropertyCheckConfiguration(1000)

  "CallbackOrderConversion" should {
    "convert callback settings to json and back" in {
      forAll(CallbackSettingsGen) { settings =>
        val json = CallbackSettingsJsonSerDe.to(settings)
        val modelActual = CallbackSettingsJsonSerDe.from(json)
        modelActual should ===(settings)
      }
    }
  }

}
