package ru.yandex.vertis.moisha.impl.parts_quota.msk.v1

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

import ru.yandex.vertis.moisha.impl.parts_quota.PartsQuotaPolicy

/**
  * Specs on [[PartsQuotaMskPolicyV1]]
  */
@RunWith(classOf[JUnitRunner])
class PartsQuotaMskPolicyV1Spec extends PrioritySpec {

  val policy: PartsQuotaPolicy = new PartsQuotaMskPolicyV1

}
