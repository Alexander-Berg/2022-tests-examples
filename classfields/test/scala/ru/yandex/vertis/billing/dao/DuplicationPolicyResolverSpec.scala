package ru.yandex.vertis.billing.dao

import org.scalatest.matchers.should.Matchers
import ru.yandex.vertis.billing.dao.EventDivisionDao.DuplicationDaoPolicies
import ru.yandex.vertis.billing.model_core.SupportedDivisions
import ru.yandex.vertis.billing.model_core.gens._
import DuplicationPolicyResolver.DuplicationPolicyDisableVSBILLING4143
import org.scalatest.wordspec.AnyWordSpec

/**
  * Spec on [[DuplicationPolicyResolver]]
  *
  * @author ruslansd
  */
class DuplicationPolicyResolverSpec extends AnyWordSpec with Matchers {

  import DuplicationPolicyResolver.WithSkipOnIndexing

  "DuplicationPolicyResolver" should {

    "provide skip policy on indexing division" in {
      SupportedDivisions.Values.foreach { d =>
        val policy = WithSkipOnIndexing.resolve(d).dao
        if (d.isIndexing)
          policy should be(DuplicationDaoPolicies.Skip)
        else
          policy should be(DuplicationDaoPolicies.Update)
      }
    }

    "modify only commercial realty indexing only before VSBILLING-4143 start" in {
      val event = EventGen.next
      SupportedDivisions.Values.foreach { d =>
        val modify = WithSkipOnIndexing.resolve(d).modify
        val before = {
          val p = event.payload.copy(timestamp = DuplicationPolicyDisableVSBILLING4143.minusMinutes(10))
          event.copy(division = d, payload = p)
        }

        val after = {
          val p = event.payload.copy(timestamp = DuplicationPolicyDisableVSBILLING4143.plusMinutes(10))
          event.copy(division = d, payload = p)
        }

        if (d == SupportedDivisions.RealtyCommercialRuIndexing) {
          val expected =
            before.copy(payload = before.payload.copy(timestamp = before.payload.timestamp.withTimeAtStartOfDay()))
          modify(before) should be(expected)
          modify(after) should be(after)
        } else
          modify(before) should be(before)
        modify(after) should be(after)
      }
    }
  }

}
