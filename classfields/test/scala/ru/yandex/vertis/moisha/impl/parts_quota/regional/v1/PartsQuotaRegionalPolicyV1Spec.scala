package ru.yandex.vertis.moisha.impl.parts_quota.regional.v1

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

import ru.yandex.vertis.moisha.impl.parts_quota.PartsQuotaPolicy

/**
  * Specs on [[PartsQuotaRegionalPolicyV1]]
  */
@RunWith(classOf[JUnitRunner])
class PartsQuotaRegionalPolicyV1Spec extends PrioritySpec {

  val policy: PartsQuotaPolicy = new PartsQuotaRegionalPolicyV1

}
