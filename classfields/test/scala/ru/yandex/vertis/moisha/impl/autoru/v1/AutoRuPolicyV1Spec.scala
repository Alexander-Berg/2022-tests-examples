package ru.yandex.vertis.moisha.impl.autoru.v1

import ru.yandex.vertis.moisha.impl.autoru.AutoRuPolicy
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

/**
  * Specs on [[AutoRuPolicyV1]]
  *
  * @author Alexander Kovalenko (alex-kovalenko@yandex-team.ru)
  */
@RunWith(classOf[JUnitRunner])
class AutoRuPolicyV1Spec
  extends AutoRuPlacementPolicySpec
  with AutoRuSpecialPolicySpec
  with AutoRuPremiumSpec
  with AutoRuBoostSpec
  with AutoRuCertificationSpec
  with AutoRuOldVasSpec {

  val policy: AutoRuPolicy = new AutoRuPolicyV1

}
