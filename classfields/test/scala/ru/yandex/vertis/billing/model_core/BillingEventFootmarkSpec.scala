package ru.yandex.vertis.billing.model_core

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import ru.yandex.vertis.billing.model_core.gens._

/**
  * Spec on [[BillingEventFootmark]]
  *
  * @author ruslansd
  */
class BillingEventFootmarkSpec extends AnyWordSpec with Matchers {

  "BillingEventFootmark" should {
    "have different id for different events" in {
      val events = BillingEventFootmarkGen.next(1000).toSet
      val hashes = events.map(_.id)
      events.size should be(hashes.size)
    }

    "different id for close events" in {
      val event1 = BillingEventFootmarkGen.next match {
        case e: BillingEventFootmarkImpl => e
      }
      val event2 = event1.copy(time = event1.time.plus(1))
      event1.id should not be event2.id
    }
  }

}
