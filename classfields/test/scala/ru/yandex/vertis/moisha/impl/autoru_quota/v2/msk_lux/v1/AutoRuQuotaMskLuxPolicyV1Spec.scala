package ru.yandex.vertis.moisha.impl.autoru_quota.v2.msk_lux.v1

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

import ru.yandex.vertis.moisha.impl.autoru_quota.AutoRuQuotaPolicy

/**
  * Specs on [[AutoRuQuotaMskLuxPolicyV1]]
  */
@RunWith(classOf[JUnitRunner])
class AutoRuQuotaMskLuxPolicyV1Spec
  extends PlacementCarsNewSpec
  with PlacementCarsNewPremiumSpec
  with PlacementMotoSpec {

  val policy: AutoRuQuotaPolicy = new AutoRuQuotaMskLuxPolicyV1

}
