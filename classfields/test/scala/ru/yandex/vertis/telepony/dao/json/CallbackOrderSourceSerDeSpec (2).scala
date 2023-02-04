package ru.yandex.vertis.telepony.dao.json

import ru.yandex.vertis.telepony.SpecBase
import ru.yandex.vertis.telepony.model.CallbackGenerator.CallCallbackOrderSourceGen
import ru.yandex.vertis.telepony.generator.Producer._
import ru.yandex.vertis.telepony.model.CallbackOrder.CallbackOrderSource

class CallbackOrderSourceSerDeSpec extends SpecBase {

  "CallbackOrderSourceSerDe" should {
    "convert api callback order source" in {
      val expectedSource = CallbackOrderSource.Api
      val json = CallbackOrderSourceSerDe.to(expectedSource)
      val actualSource = CallbackOrderSourceSerDe.from(json)
      actualSource shouldBe expectedSource
    }
    "convert call callback order source" in {
      val expectedSource = CallCallbackOrderSourceGen.next
      val json = CallbackOrderSourceSerDe.to(expectedSource)
      val actualSource = CallbackOrderSourceSerDe.from(json)
      actualSource shouldBe expectedSource
    }
  }

}
