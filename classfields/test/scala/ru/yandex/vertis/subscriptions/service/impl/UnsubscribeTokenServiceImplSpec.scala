package ru.yandex.vertis.subscriptions.service.impl

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.prop.PropertyChecks
import ru.yandex.vertis.subscriptions.SpecBase
import ru.yandex.vertis.subscriptions.model.UnsubscribeGenerators

/**
  * Specs on [[UnsubscribeTokenServiceImpl]].
  *
  * @author dimas
  */
@RunWith(classOf[JUnitRunner])
class UnsubscribeTokenServiceImplSpec extends SpecBase with PropertyChecks {

  "UnsubscribeTokenServiceImpl" should {
    "round trip unsubscribe instances" in {
      forAll(UnsubscribeGenerators.unsubscribe) { initial =>
        val serialized = UnsubscribeTokenServiceImpl.serialize(initial)
        val deserialized = UnsubscribeTokenServiceImpl.deserialize(serialized)
        deserialized should be(initial)
      }
    }
  }

}
