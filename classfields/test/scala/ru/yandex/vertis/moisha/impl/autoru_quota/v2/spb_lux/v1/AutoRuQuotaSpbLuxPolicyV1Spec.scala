package ru.yandex.vertis.moisha.impl.autoru_quota.v2.spb_lux.v1

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

import ru.yandex.vertis.moisha.impl.autoru_quota.AutoRuQuotaPolicy

/**
  * Specs on [[AutoRuQuotaSpbLuxPolicyV1]]
  */
@RunWith(classOf[JUnitRunner])
class AutoRuQuotaSpbLuxPolicyV1Spec
  extends PlacementCarsNewSpec
  with PlacementCarsNewPremiumSpec
  with PlacementMotoSpec {

  val policy: AutoRuQuotaPolicy = new AutoRuQuotaSpbLuxPolicyV1

}
