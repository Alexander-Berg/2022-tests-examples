package ru.yandex.vertis.moisha.impl.parts_quota.spb.v1

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

import ru.yandex.vertis.moisha.impl.parts_quota.PartsQuotaPolicy

/**
  * Specs on [[PartsQuotaSpbPolicyV1]]
  */
@RunWith(classOf[JUnitRunner])
class PartsQuotaSpbPolicyV1Spec extends PrioritySpec {

  val policy: PartsQuotaPolicy = new PartsQuotaSpbPolicyV1

}
